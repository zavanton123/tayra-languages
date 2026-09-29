import Foundation
import MLKitTranslate
import Shared

/// Google ML Kit's on-device translation, handed to the shared code as `OnDeviceTranslatorBridge`.
final class MlKitTranslatorBridge: NSObject, OnDeviceTranslatorBridge {
    private var translators: [String: Translator] = [:]

    func supportedLanguages() -> [String] {
        TranslateLanguage.allLanguages().map { $0.rawValue }.sorted()
    }

    func downloadedLanguages(callback: @escaping ([String]?, String?) -> Void) {
        let models = ModelManager.modelManager().downloadedTranslateModels
        callback(models.map { $0.language.rawValue }, nil)
    }

    func download(language: String, callback: @escaping (String?) -> Void) {
        let model = TranslateRemoteModel.translateRemoteModel(language: TranslateLanguage(rawValue: language))
        let conditions = ModelDownloadConditions(allowsCellularAccess: true, allowsBackgroundDownloading: false)
        var observers: [NSObjectProtocol] = []
        func finish(_ error: String?) {
            observers.forEach { NotificationCenter.default.removeObserver($0) }
            observers.removeAll()
            callback(error)
        }
        observers.append(NotificationCenter.default.addObserver(forName: .mlkitModelDownloadDidSucceed, object: nil, queue: .main) { note in
            guard let done = note.userInfo?[ModelDownloadUserInfoKey.remoteModel.rawValue] as? TranslateRemoteModel, done == model else { return }
            finish(nil)
        })
        observers.append(NotificationCenter.default.addObserver(forName: .mlkitModelDownloadDidFail, object: nil, queue: .main) { note in
            guard let failed = note.userInfo?[ModelDownloadUserInfoKey.remoteModel.rawValue] as? TranslateRemoteModel, failed == model else { return }
            let error = note.userInfo?[ModelDownloadUserInfoKey.error.rawValue] as? Error
            finish(error?.localizedDescription ?? "download failed")
        })
        _ = ModelManager.modelManager().download(model, conditions: conditions)
    }

    func delete(language: String, callback: @escaping (String?) -> Void) {
        let model = TranslateRemoteModel.translateRemoteModel(language: TranslateLanguage(rawValue: language))
        ModelManager.modelManager().deleteDownloadedModel(model) { error in
            callback(error?.localizedDescription)
        }
    }

    func translate(text: String, from: String, to: String, callback: @escaping (String?, String?) -> Void) {
        let key = "\(from)-\(to)"
        let translator = translators[key] ?? {
            let options = TranslatorOptions(sourceLanguage: TranslateLanguage(rawValue: from), targetLanguage: TranslateLanguage(rawValue: to))
            let t = Translator.translator(options: options)
            translators[key] = t
            return t
        }()
        translator.translate(text) { result, error in
            if let error = error { callback(nil, error.localizedDescription) } else { callback(result, nil) }
        }
    }

    func languageName(language: String) -> String {
        Locale(identifier: "en").localizedString(forLanguageCode: language) ?? language
    }
}
