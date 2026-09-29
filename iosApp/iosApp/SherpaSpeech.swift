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
    var dictDir = ""
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
            config.model.kokoro.dict_dir = c(spec.dictDir)
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
        config.max_num_sentences = 1
        config.silence_scale = 0.2
        guard let created = SherpaOnnxCreateOfflineTts(&config) else {
            throw SpeechError("sherpa-onnx could not load \(spec.model)")
        }
        return created
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
    static func start(url: String, destination: String, progress: @escaping (Int64, Int64) -> Void, completion: @escaping (String?) -> Void) {
        guard let address = URL(string: url) else { completion("bad address \(url)"); return }
        let delegate = FileDownload(destination: destination, progress: progress, completion: completion)
        let session = URLSession(configuration: .default, delegate: delegate, delegateQueue: nil)
        session.downloadTask(with: address).resume()
        session.finishTasksAndInvalidate()
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
