import Foundation
import Shared

/// sherpa-onnx speech for the shared code: downloads, unpacking and synthesis on a background queue.
final class SherpaSpeechBridge: NSObject, OnDeviceSpeechBridge {
    private let queue = DispatchQueue(label: "com.tayra.languages.speech", qos: .userInitiated)
    private let synthesizer = SherpaSynthesizer()
    private let transcription = DispatchQueue(label: "com.tayra.languages.transcription", qos: .userInitiated)
    private let job = TranscriptionJob()

    func download(url: String, destination: String, progress: @escaping (KotlinLong, KotlinLong) -> Void, completion: @escaping (String?) -> Void) {
        FileDownload.start(url: url, destination: destination, progress: { done, total in
            progress(KotlinLong(value: done), KotlinLong(value: total))
        }, completion: completion)
    }

    func extractTarBz2(archive: String, destination: String, completion: @escaping (String?) -> Void) {
        DispatchQueue.global(qos: .utility).async {
            do {
                try TarBz2.extract(archive: archive, to: destination)
                completion(nil)
            } catch {
                completion(error.localizedDescription)
            }
        }
    }

    func synthesize(request: SpeechSynthesisRequest, output: String, completion: @escaping (String?) -> Void) {
        let spec = TtsSpec(
            key: request.key, kokoro: request.kokoro, model: request.model, tokens: request.tokens, dataDir: request.dataDir,
            voices: request.voices, lexicon: request.lexicon, lang: request.lang, ruleFsts: request.ruleFsts
        )
        let text = request.text, speaker = request.speaker, speed = request.speed
        queue.async { [synthesizer] in
            do {
                try synthesizer.synthesize(spec, text: text, speaker: speaker, speed: speed, output: output)
                completion(nil)
            } catch {
                completion(error.localizedDescription)
            }
        }
    }

    func unload(path: String) {
        queue.sync { synthesizer.unload(under: path) }
    }

    func decodeAudio(url: String, output: String, progress: @escaping (KotlinFloat?) -> Void, completion: @escaping (String?) -> Void) {
        let job = self.job
        job.begin()
        guard let address = URL(string: url) else {
            completion(VideoSound.unreadable)
            return
        }
        // AVFoundation cannot read a playlist of segments as one file.
        guard !address.pathExtension.lowercased().hasPrefix("m3u") else {
            completion(VideoSound.unsupported)
            return
        }
        // AVFoundation reads only local files here, and goes by the extension to know what it is reading.
        let source = NSTemporaryDirectory() + "tayra-transcription-source." + (address.pathExtension.isEmpty ? "mp4" : address.pathExtension)
        let task = FileDownload.start(url: url, destination: source, progress: { done, total in
            progress(total > done ? KotlinFloat(value: VideoSound.downloadShare * Float(done) / Float(total)) : nil)
        }, completion: { error in
            guard error == nil, !job.cancelled else {
                try? FileManager.default.removeItem(atPath: source)
                completion(job.cancelled ? nil : VideoSound.unreadable)
                return
            }
            Task.detached(priority: .userInitiated) {
                defer { try? FileManager.default.removeItem(atPath: source) }
                do {
                    try await VideoSound.decode(file: source, output: output, job: job) { share in
                        progress(KotlinFloat(value: VideoSound.downloadShare + (1 - VideoSound.downloadShare) * share))
                    }
                    completion(nil)
                } catch {
                    completion((error as? SpeechError)?.message ?? VideoSound.failed)
                }
            }
        })
        job.track(task)
    }

    func transcribe(request: WhisperRequest, progress: @escaping (KotlinFloat) -> Void, heard: @escaping (KotlinLong, KotlinLong, String) -> Void, completion: @escaping (String?) -> Void) {
        let job = self.job
        job.begin()
        let spec = WhisperSpec(encoder: request.encoder, decoder: request.decoder, tokens: request.tokens, vad: request.vad, language: request.language)
        let pcm = request.pcm
        transcription.async {
            do {
                try WhisperListener.listen(spec, pcm: pcm, job: job, progress: { progress(KotlinFloat(value: $0)) }, heard: { begin, end, text in
                    heard(KotlinLong(value: begin), KotlinLong(value: end), text)
                })
                completion(nil)
            } catch {
                completion((error as? SpeechError)?.message ?? VideoSound.failed)
            }
        }
    }

    func cancelTranscription() {
        job.cancel()
    }
}
