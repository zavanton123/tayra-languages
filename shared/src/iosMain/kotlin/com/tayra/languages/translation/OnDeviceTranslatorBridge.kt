package com.tayra.languages.translation

import com.tayra.languages.core.data.translation.LanguageModelTranslator
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.i18n.tr
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * What the Swift side implements with Google ML Kit. Plain callbacks rather than suspend
 * functions, because Kotlin cannot call a suspend function that Swift implements. Language
 * codes are BCP-47 tags as ML Kit spells them ("no" for Norwegian).
 */
interface OnDeviceTranslatorBridge {
    /** Every language ML Kit can translate, as tags. */
    fun supportedLanguages(): List<String>

    /** Tags of the models on the device; the callback gets an error message instead when the lookup fails. */
    fun downloadedLanguages(callback: (languages: List<String>?, error: String?) -> Unit)

    fun download(language: String, callback: (error: String?) -> Unit)

    fun delete(language: String, callback: (error: String?) -> Unit)

    fun translate(text: String, from: String, to: String, callback: (result: String?, error: String?) -> Unit)

    /** An English display name for a tag. */
    fun languageName(language: String): String
}

/** Google ML Kit on iOS, reached through [OnDeviceTranslatorBridge]. */
class BridgedSentenceTranslator(private val bridge: OnDeviceTranslatorBridge, settings: SettingsRepository) : LanguageModelTranslator(settings) {

    override val displayName: String = "Google ML Kit"
    override val description: String get() = tr("Google ML Kit translates on this device with no network once a language's model is downloaded. Models come from Google and stay on the device.")

    override suspend fun supportedModels(): List<String> = bridge.supportedLanguages()

    override suspend fun downloadedModels(): Set<String> = suspendCancellableCoroutine { cont ->
        bridge.downloadedLanguages { languages, error ->
            if (languages != null) cont.resume(languages.toSet()) else cont.resumeWithException(IllegalStateException(error ?: "unknown error"))
        }
    }

    override suspend fun downloadModel(model: String) = suspendCancellableCoroutine { cont ->
        bridge.download(model) { error -> if (error == null) cont.resume(Unit) else cont.resumeWithException(IllegalStateException(error)) }
    }

    override suspend fun deleteModel(model: String) = suspendCancellableCoroutine { cont ->
        bridge.delete(model) { error -> if (error == null) cont.resume(Unit) else cont.resumeWithException(IllegalStateException(error)) }
    }

    override suspend fun translateWithModels(text: String, fromModel: String, toModel: String): String = suspendCancellableCoroutine { cont ->
        bridge.translate(text, fromModel, toModel) { result, error ->
            if (result != null) cont.resume(result) else cont.resumeWithException(IllegalStateException(error ?: "unknown error"))
        }
    }

    override fun modelFor(code: String): String? {
        val tag = if (code == "nb") "no" else code
        return tag.takeIf { it in bridge.supportedLanguages() }
    }

    override fun codeFor(model: String): String = if (model == "no") "nb" else model

    override fun nameFor(model: String): String = bridge.languageName(model)
}
