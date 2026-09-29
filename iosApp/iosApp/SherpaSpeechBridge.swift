import Foundation
import Shared

/// sherpa-onnx speech for the shared code: downloads, unpacking and synthesis on a background queue.
final class SherpaSpeechBridge: NSObject, OnDeviceSpeechBridge {
    private let queue = DispatchQueue(label: "com.tayra.languages.speech", qos: .userInitiated)
    private let synthesizer = SherpaSynthesizer()

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
            voices: request.voices, lexicon: request.lexicon, lang: request.lang, dictDir: request.dictDir
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
}
