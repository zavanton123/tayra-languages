import AVFoundation
import Foundation
import SherpaOnnxC

/// Errors from the on-device speech code, carried to Kotlin as their message.
struct SpeechError: LocalizedError {
    let message: String
    init(_ message: String) { self.message = message }
    var errorDescription: String? { message }
}

/// The files and settings of one sherpa-onnx model configuration. Empty strings mean "not used".
struct TtsSpec {
    var key: String
    var kokoro: Bool
    var model: String
    var tokens: String
    var dataDir: String
    var voices = ""
    var lexicon = ""
    var lang = ""
    var ruleFsts = ""
}

/// sherpa-onnx text-to-speech through its C API. One model stays loaded and is reused while the
/// key is the same; not thread-safe, so callers keep it on one queue.
final class SherpaSynthesizer {
    private var loadedKey: String?
    private var loadedModel: String?
    private var tts: OpaquePointer?

    deinit { unload() }

    /// Writes the speech for `text` as a WAV file at `output`.
    func synthesize(_ spec: TtsSpec, text: String, speaker: Int32, speed: Float, output: String) throws {
        if loadedKey != spec.key {
            unload()
            tts = try create(spec)
            loadedKey = spec.key
            loadedModel = spec.model
        }
        var generation = SherpaOnnxGenerationConfig()
        generation.speed = speed
        generation.sid = speaker
        generation.silence_scale = 0.2
        guard let audio = SherpaOnnxOfflineTtsGenerateWithConfig(tts, text, &generation, nil, nil) else {
            throw SpeechError("sherpa-onnx produced no audio")
        }
        defer { SherpaOnnxDestroyOfflineTtsGeneratedAudio(audio) }
        let result = audio.pointee
        guard result.n > 0, let samples = result.samples else { throw SpeechError("sherpa-onnx produced no audio") }
        guard SherpaOnnxWriteWave(samples, result.n, result.sample_rate, output) == 1 else {
            throw SpeechError("could not write \(output)")
        }
    }

    /// Releases the loaded model when its files live under `path`, or any model when `path` is nil.
    func unload(under path: String? = nil) {
        if let path, let model = loadedModel, !model.hasPrefix(path) { return }
        if let tts { SherpaOnnxDestroyOfflineTts(tts) }
        tts = nil
        loadedKey = nil
        loadedModel = nil
    }

    private func create(_ spec: TtsSpec) throws -> OpaquePointer {
        // sherpa-onnx copies every string while it builds the engine, so they are freed right after.
        var strings: [UnsafeMutablePointer<CChar>] = []
        defer { strings.forEach { free($0) } }
        func c(_ value: String) -> UnsafePointer<CChar>? {
            guard !value.isEmpty, let copy = strdup(value) else { return nil }
            strings.append(copy)
            return UnsafePointer(copy)
        }
        var config = SherpaOnnxOfflineTtsConfig()
        config.model.debug = 0
        config.model.provider = c("cpu")
        if spec.kokoro {
            config.model.num_threads = 4
            config.model.kokoro.model = c(spec.model)
            config.model.kokoro.voices = c(spec.voices)
            config.model.kokoro.tokens = c(spec.tokens)
            config.model.kokoro.data_dir = c(spec.dataDir)
            config.model.kokoro.lexicon = c(spec.lexicon)
            config.model.kokoro.lang = c(spec.lang)
            config.model.kokoro.length_scale = 1
        } else {
            config.model.num_threads = 2
            config.model.vits.model = c(spec.model)
            config.model.vits.tokens = c(spec.tokens)
            config.model.vits.data_dir = c(spec.dataDir)
            config.model.vits.noise_scale = 0.667
            config.model.vits.noise_scale_w = 0.8
            config.model.vits.length_scale = 1
        }
        config.rule_fsts = c(spec.ruleFsts)
        config.max_num_sentences = 1
        config.silence_scale = 0.2
        guard let created = SherpaOnnxCreateOfflineTts(&config) else {
            throw SpeechError("sherpa-onnx could not load \(spec.model)")
        }
        return created
    }
}

/// The running transcription, for stopping it from another thread.
final class TranscriptionJob {
    private let lock = NSLock()
    private var stopped = false
    private var task: URLSessionTask?

    var cancelled: Bool {
        lock.lock()
        defer { lock.unlock() }
        return stopped
    }

    func begin() {
        lock.lock()
        defer { lock.unlock() }
        stopped = false
        task = nil
    }

    func track(_ task: URLSessionTask?) {
        lock.lock()
        defer { lock.unlock() }
        self.task = task
    }

    func cancel() {
        lock.lock()
        defer { lock.unlock() }
        stopped = true
        task?.cancel()
    }
}

/// The sound of a video as samples for Whisper. The texts are the keys of the app's translations.
enum VideoSound {
    static let unreadable = "The video could not be read from its address."
    static let unsupported = "This kind of video can't be transcribed on an iPhone."
    static let silent = "The video has no sound."
    static let failed = "The transcription failed."
    /// How much of the progress bar the download takes; the decoding of the file takes the rest.
    static let downloadShare: Float = 0.8

    /// Writes the sound of the media `file` into `output` as 16 kHz mono 16-bit little-endian samples.
    static func decode(file: String, output: String, job: TranscriptionJob, progress: (Float) -> Void) async throws {
        let asset = AVURLAsset(url: URL(fileURLWithPath: file))
        // A container AVFoundation does not know, such as WebM or Ogg, fails here or reports itself unreadable.
        guard let readable = try? await asset.load(.isReadable), readable, let tracks = try? await asset.loadTracks(withMediaType: .audio) else {
            throw SpeechError(unsupported)
        }
        guard !tracks.isEmpty else { throw SpeechError(silent) }
        let seconds = (try? await asset.load(.duration)).map(CMTimeGetSeconds) ?? 0
        guard let reader = try? AVAssetReader(asset: asset) else { throw SpeechError(unsupported) }
        let samples = AVAssetReaderAudioMixOutput(audioTracks: tracks, audioSettings: [
            AVFormatIDKey: kAudioFormatLinearPCM,
            AVSampleRateKey: 16_000,
            AVNumberOfChannelsKey: 1,
            AVLinearPCMBitDepthKey: 16,
            AVLinearPCMIsFloatKey: false,
            AVLinearPCMIsBigEndianKey: false,
            AVLinearPCMIsNonInterleaved: false,
        ])
        guard reader.canAdd(samples) else { throw SpeechError(unsupported) }
        reader.add(samples)
        guard reader.startReading() else { throw SpeechError(unsupported) }
        defer { if reader.status == .reading { reader.cancelReading() } }

        guard FileManager.default.createFile(atPath: output, contents: nil), let out = FileHandle(forWritingAtPath: output) else {
            throw SpeechError(failed)
        }
        defer { try? out.close() }
        var written = 0
        while !job.cancelled, let buffer = samples.copyNextSampleBuffer() {
            guard let block = CMSampleBufferGetDataBuffer(buffer) else { continue }
            let length = CMBlockBufferGetDataLength(block)
            var data = Data(count: length)
            let status = data.withUnsafeMutableBytes { CMBlockBufferCopyDataBytes(block, atOffset: 0, dataLength: length, destination: $0.baseAddress!) }
            guard status == kCMBlockBufferNoErr else { throw SpeechError(failed) }
            try out.write(contentsOf: data)
            written += length
            if seconds > 0 { progress(min(1, Float(Double(written) / 2 / 16_000 / seconds))) }
        }
        if job.cancelled { return }
        if reader.status == .failed { throw SpeechError(unsupported) }
        if written == 0 { throw SpeechError(silent) }
    }
}

/// The files of one Whisper model and the voice detector, and the language to hear.
struct WhisperSpec {
    var encoder: String
    var decoder: String
    var tokens: String
    var vad: String
    var language: String
}

/// Whisper through sherpa-onnx's C API: Silero's voice detector cuts the samples into stretches of speech, Whisper hears each.
enum WhisperListener {
    private static let sampleRate: Int32 = 16_000
    private static let chunkSamples = 16_000

    /// Hears the samples in the file `pcm`, calling `heard` with begin and end in milliseconds and the text of each stretch.
    static func listen(_ spec: WhisperSpec, pcm: String, job: TranscriptionJob, progress: (Float) -> Void, heard: (Int64, Int64, String) -> Void) throws {
        var strings: [UnsafeMutablePointer<CChar>] = []
        defer { strings.forEach { free($0) } }
        func c(_ value: String) -> UnsafePointer<CChar>? {
            guard let copy = strdup(value) else { return nil }
            strings.append(copy)
            return UnsafePointer(copy)
        }

        var detector = SherpaOnnxVadModelConfig()
        detector.silero_vad.model = c(spec.vad)
        detector.silero_vad.threshold = 0.5
        detector.silero_vad.min_silence_duration = 0.4
        detector.silero_vad.min_speech_duration = 0.25
        detector.silero_vad.window_size = 512
        detector.silero_vad.max_speech_duration = 12
        detector.sample_rate = sampleRate
        detector.num_threads = 1
        detector.provider = c("cpu")
        guard let vad = SherpaOnnxCreateVoiceActivityDetector(&detector, 60) else { throw SpeechError(VideoSound.failed) }
        defer { SherpaOnnxDestroyVoiceActivityDetector(vad) }

        var config = SherpaOnnxOfflineRecognizerConfig()
        config.feat_config.sample_rate = sampleRate
        config.feat_config.feature_dim = 80
        config.model_config.whisper.encoder = c(spec.encoder)
        config.model_config.whisper.decoder = c(spec.decoder)
        config.model_config.whisper.language = c(spec.language)
        config.model_config.whisper.task = c("transcribe")
        config.model_config.tokens = c(spec.tokens)
        config.model_config.num_threads = 4
        config.model_config.provider = c("cpu")
        config.model_config.model_type = c("whisper")
        config.decoding_method = c("greedy_search")
        guard let recognizer = SherpaOnnxCreateOfflineRecognizer(&config) else { throw SpeechError(VideoSound.failed) }
        defer { SherpaOnnxDestroyOfflineRecognizer(recognizer) }

        func drain() {
            while !job.cancelled, SherpaOnnxVoiceActivityDetectorEmpty(vad) == 0 {
                guard let segment = SherpaOnnxVoiceActivityDetectorFront(vad) else { break }
                defer {
                    SherpaOnnxDestroySpeechSegment(segment)
                    SherpaOnnxVoiceActivityDetectorPop(vad)
                }
                guard let stream = SherpaOnnxCreateOfflineStream(recognizer) else { continue }
                defer { SherpaOnnxDestroyOfflineStream(stream) }
                SherpaOnnxAcceptWaveformOffline(stream, sampleRate, segment.pointee.samples, segment.pointee.n)
                SherpaOnnxDecodeOfflineStream(recognizer, stream)
                guard let result = SherpaOnnxGetOfflineStreamResult(stream) else { continue }
                defer { SherpaOnnxDestroyOfflineRecognizerResult(result) }
                let text = result.pointee.text.map { String(cString: $0) } ?? ""
                let begin = Int64(segment.pointee.start) * 1000 / Int64(sampleRate)
                heard(begin, begin + Int64(segment.pointee.n) * 1000 / Int64(sampleRate), text.trimmingCharacters(in: .whitespacesAndNewlines))
            }
        }

        guard let input = FileHandle(forReadingAtPath: pcm) else { throw SpeechError(VideoSound.failed) }
        defer { try? input.close() }
        let total = max(1, (try? FileManager.default.attributesOfItem(atPath: pcm)[.size] as? Int) ?? 1)
        var read = 0
        var floats = [Float](repeating: 0, count: chunkSamples)
        while !job.cancelled, let data = try input.read(upToCount: chunkSamples * 2), !data.isEmpty {
            read += data.count
            let count = data.count / 2
            data.withUnsafeBytes { raw in
                for i in 0..<count { floats[i] = Float(Int16(littleEndian: raw.loadUnaligned(fromByteOffset: i * 2, as: Int16.self))) / 32768 }
            }
            SherpaOnnxVoiceActivityDetectorAcceptWaveform(vad, floats, Int32(count))
            drain()
            progress(Float(read) / Float(total))
        }
        if job.cancelled { return }
        SherpaOnnxVoiceActivityDetectorFlush(vad)
        drain()
    }
}

/// Unpacks `.tar.bz2` archives: bzip2 from the system library, the tar format read here.
enum TarBz2 {
    /// Unpacks `archive` into `destination`, dropping the first path component of every entry.
    static func extract(archive: String, to destination: String) throws {
        guard let file = fopen(archive, "rb") else { throw SpeechError("cannot open \(archive)") }
        defer { fclose(file) }
        var status: Int32 = BZ_OK
        guard let bz = BZ2_bzReadOpen(&status, file, 0, 0, nil, 0), status == BZ_OK else {
            throw SpeechError("\(archive) is not a bzip2 archive")
        }
        defer { BZ2_bzReadClose(&status, bz) }
        var ended = false

        /// Fills `buffer` completely; returns false at the clean end of the stream.
        func read(_ buffer: UnsafeMutableRawPointer, _ count: Int) throws -> Bool {
            var done = 0
            while done < count {
                if ended { if done == 0 { return false }; throw SpeechError("the archive is truncated") }
                var error: Int32 = BZ_OK
                let n = BZ2_bzRead(&error, bz, buffer + done, Int32(min(count - done, Int(Int32.max))))
                if error == BZ_STREAM_END { ended = true } else if error != BZ_OK { throw SpeechError("the archive is damaged (\(error))") }
                done += Int(n)
            }
            return true
        }

        let fm = FileManager.default
        try fm.createDirectory(atPath: destination, withIntermediateDirectories: true)
        var header = [UInt8](repeating: 0, count: 512)
        var chunk = [UInt8](repeating: 0, count: 1 << 16)
        var longName: String?
        var paxPath: String?

        func field(_ bytes: [UInt8], _ offset: Int, _ length: Int) -> String {
            let slice = bytes[offset..<(offset + length)]
            let end = slice.firstIndex(of: 0) ?? slice.endIndex
            return String(decoding: bytes[offset..<end], as: UTF8.self)
        }
        func readData(_ size: Int) throws -> [UInt8] {
            var data = [UInt8](repeating: 0, count: size)
            if size > 0 { _ = try data.withUnsafeMutableBytes { try read($0.baseAddress!, size) } }
            try skip(padding(size))
            return data
        }
        func skip(_ count: Int) throws {
            var left = count
            while left > 0 {
                let n = min(left, chunk.count)
                _ = try chunk.withUnsafeMutableBytes { try read($0.baseAddress!, n) }
                left -= n
            }
        }
        func padding(_ size: Int) -> Int { (512 - size % 512) % 512 }

        while try header.withUnsafeMutableBytes({ try read($0.baseAddress!, 512) }) {
            if header.allSatisfy({ $0 == 0 }) { break }
            let sizeField = field(header, 124, 12).trimmingCharacters(in: .whitespaces)
            let size = Int(sizeField, radix: 8) ?? 0
            let type = header[156]
            switch type {
            case UInt8(ascii: "L"):
                longName = String(decoding: try readData(size).prefix { $0 != 0 }, as: UTF8.self)
                continue
            case UInt8(ascii: "x"):
                let records = String(decoding: try readData(size), as: UTF8.self)
                paxPath = records.split(separator: "\n").compactMap { line -> String? in
                    guard let range = line.range(of: " path=") else { return nil }
                    return String(line[range.upperBound...])
                }.first
                continue
            case UInt8(ascii: "g"):
                try skip(size + padding(size))
                continue
            default:
                break
            }
            let prefix = field(header, 345, 155)
            let plain = prefix.isEmpty ? field(header, 0, 100) : prefix + "/" + field(header, 0, 100)
            let name = paxPath ?? longName ?? plain
            longName = nil
            paxPath = nil
            let parts = name.split(separator: "/", omittingEmptySubsequences: true)
            let relative = parts.dropFirst().joined(separator: "/")
            let usable = !relative.isEmpty && !parts.contains("..")
            let target = destination + "/" + relative

            if type == UInt8(ascii: "5") {
                if usable { try fm.createDirectory(atPath: target, withIntermediateDirectories: true) }
                try skip(size + padding(size))
            } else if (type == UInt8(ascii: "0") || type == 0) && usable {
                try fm.createDirectory(atPath: (target as NSString).deletingLastPathComponent, withIntermediateDirectories: true)
                guard let out = fopen(target, "wb") else { throw SpeechError("cannot write \(target)") }
                var left = size
                while left > 0 {
                    let n = min(left, chunk.count)
                    _ = try chunk.withUnsafeMutableBytes { try read($0.baseAddress!, n) }
                    _ = chunk.withUnsafeBytes { fwrite($0.baseAddress!, 1, n, out) }
                    left -= n
                }
                fclose(out)
                try skip(padding(size))
            } else {
                // Links, devices and anything else the models do not need.
                try skip(size + padding(size))
            }
        }
    }
}

/// Downloads one file with progress, following redirects.
final class FileDownload: NSObject, URLSessionDownloadDelegate {
    private let destination: String
    private let progress: (Int64, Int64) -> Void
    private let completion: (String?) -> Void
    private var failure: String?
    private var lastReport = Date.distantPast

    private init(destination: String, progress: @escaping (Int64, Int64) -> Void, completion: @escaping (String?) -> Void) {
        self.destination = destination
        self.progress = progress
        self.completion = completion
    }

    /// Starts the download; `completion` gets nil on success or an error message.
    @discardableResult
    static func start(url: String, destination: String, progress: @escaping (Int64, Int64) -> Void, completion: @escaping (String?) -> Void) -> URLSessionTask? {
        guard let address = URL(string: url) else { completion("bad address \(url)"); return nil }
        let delegate = FileDownload(destination: destination, progress: progress, completion: completion)
        let session = URLSession(configuration: .default, delegate: delegate, delegateQueue: nil)
        let task = session.downloadTask(with: address)
        task.resume()
        session.finishTasksAndInvalidate()
        return task
    }

    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didWriteData bytesWritten: Int64, totalBytesWritten: Int64, totalBytesExpectedToWrite: Int64) {
        let now = Date()
        guard now.timeIntervalSince(lastReport) > 0.25 else { return }
        lastReport = now
        progress(totalBytesWritten, totalBytesExpectedToWrite > 0 ? totalBytesExpectedToWrite : totalBytesWritten)
    }

    func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        if let http = downloadTask.response as? HTTPURLResponse, http.statusCode != 200 {
            failure = "download failed with HTTP \(http.statusCode)"
            return
        }
        // The temporary file disappears when this method returns, so it is moved now.
        let fm = FileManager.default
        try? fm.removeItem(atPath: destination)
        do {
            try fm.createDirectory(atPath: (destination as NSString).deletingLastPathComponent, withIntermediateDirectories: true)
            try fm.moveItem(at: location, to: URL(fileURLWithPath: destination))
        } catch {
            failure = "could not save the download: \(error.localizedDescription)"
        }
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        completion(error?.localizedDescription ?? failure)
    }
}
