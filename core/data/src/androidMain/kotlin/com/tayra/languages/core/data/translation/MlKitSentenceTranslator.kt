package com.tayra.languages.core.data.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Google ML Kit on-device translation; models are fetched by ML Kit and kept by Android. */
class MlKitSentenceTranslator(settings: SettingsRepository) : LanguageModelTranslator(settings) {

    override val displayName: String = "Google ML Kit"
    override val description: String = "Google ML Kit translates on this phone with no network once a language's model is downloaded. Models come from Google and stay on the device."

    private val manager = RemoteModelManager.getInstance()
    private val translators = mutableMapOf<String, Translator>()

    override suspend fun supportedModels(): List<String> = TranslateLanguage.getAllLanguages()

    override suspend fun downloadedModels(): Set<String> =
        manager.getDownloadedModels(TranslateRemoteModel::class.java).await().map { it.language }.toSet()

    override suspend fun downloadModel(model: String) {
        manager.download(TranslateRemoteModel.Builder(model).build(), DownloadConditions.Builder().build()).await()
    }

    override suspend fun deleteModel(model: String) {
        manager.deleteDownloadedModel(TranslateRemoteModel.Builder(model).build()).await()
        translators.keys.filter { it.startsWith("$model-") || it.endsWith("-$model") }.forEach { translators.remove(it)?.close() }
    }

    override suspend fun translateWithModels(text: String, fromModel: String, toModel: String): String {
        val translator = translators.getOrPut("$fromModel-$toModel") {
            Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(fromModel).setTargetLanguage(toModel).build())
        }
        return translator.translate(text).await()
    }

    override fun modelFor(code: String): String? = TranslateLanguage.fromLanguageTag(if (code == "nb") "no" else code)

    override fun codeFor(model: String): String = if (model == "no") "nb" else model

    override fun nameFor(model: String): String = Locale(model).getDisplayLanguage(Locale.ENGLISH).ifEmpty { model }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}
