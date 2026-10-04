package com.tayra.languages.core.data.translation

import kotlin.coroutines.cancellation.CancellationException
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.LocalPackage
import com.tayra.languages.core.domain.service.LocalSentenceTranslator
import com.tayra.languages.core.domain.service.LocalTranslationProblem
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A local translator built on one downloadable model per language, every pair going through
 * English (Google ML Kit on Android and iOS). Subclasses supply the platform calls; this class
 * turns them into the package list, the readiness checks and the reader's problem reports.
 */
abstract class LanguageModelTranslator(private val settings: SettingsRepository) : LocalSentenceTranslator {

    override val packagesDescription: String =
        "One model per language, about 30 MB each, downloaded from Google once. Reading a language needs its model; a native language other than English needs its own model too."
    override val hasRuntimeSetup: Boolean = false

    private val _lastError = MutableStateFlow<LocalTranslationProblem?>(null)
    override val lastError: StateFlow<LocalTranslationProblem?> = _lastError
    private val _progress = MutableStateFlow<String?>(null)
    override val progress: StateFlow<String?> = _progress

    /** The translator's own codes for every language it supports. */
    protected abstract suspend fun supportedModels(): List<String>

    /** The translator's own codes of the models on the device. English is built in and never listed. */
    protected abstract suspend fun downloadedModels(): Set<String>

    protected abstract suspend fun downloadModel(model: String)

    protected abstract suspend fun deleteModel(model: String)

    protected abstract suspend fun translateWithModels(text: String, fromModel: String, toModel: String): String

    /** The translator's code for an app language code, null when unsupported. */
    protected abstract fun modelFor(code: String): String?

    /** The app language code for a translator code. */
    protected abstract fun codeFor(model: String): String

    /** An English display name for a translator code. */
    protected abstract fun nameFor(model: String): String

    override fun requiredPackages(fromCode: String, toCode: String, catalog: List<LocalPackage>): Set<String> =
        setOfNotNull("$fromCode-en".takeIf { fromCode != "en" }, "$toCode-en".takeIf { toCode != "en" })

    override suspend fun translate(text: String, language: Language): String? {
        val source = LanguageCodes.codeFor(language.name) ?: return null
        val target = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        if (source == target) return null
        val from = modelFor(source) ?: return null
        val to = modelFor(target) ?: return null
        return try {
            translateWithModels(text, from, to).trim().takeIf { it.isNotEmpty() }.also { _lastError.value = null }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w { "$displayName failed: ${e.message}" }
            _lastError.value = LocalTranslationProblem.Failed(e.message ?: "unknown error")
            null
        }
    }

    override suspend fun status(): String = try {
        val models = downloadedModels().map { nameFor(it) }.sorted()
        if (models.isEmpty()) "$displayName · no language models yet"
        else "$displayName · ${models.size} language model${if (models.size == 1) "" else "s"}"
    } catch (e: Exception) {
        "$displayName is not available: ${e.message}"
    }

    override suspend fun prepare(fromCode: String, toCode: String, fromName: String, toName: String) {
        val problem: LocalTranslationProblem? = try {
            val from = modelFor(fromCode)
            val to = modelFor(toCode)
            if (from == null || to == null) LocalTranslationProblem.NoModel(fromName, toName, displayName)
            else {
                val have = downloadedModels()
                val missing = listOf(from, to).filter { it != ENGLISH && it !in have }.distinct()
                if (missing.isEmpty()) null
                else LocalTranslationProblem.ModelMissing(fromCode, toCode, missing.joinToString(" and ") { nameFor(it) } + if (missing.size == 1) " model" else " models")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LocalTranslationProblem.Failed(e.message ?: "unknown error")
        }
        _lastError.value = problem
        if (problem != null) error(problem.message)
    }

    override suspend fun canTranslate(fromCode: String, toCode: String): Boolean {
        val from = modelFor(fromCode) ?: return false
        val to = modelFor(toCode) ?: return false
        val have = runCatching { downloadedModels() }.getOrDefault(emptySet())
        return listOf(from, to).all { it == ENGLISH || it in have }
    }

    override suspend fun installModels(fromCode: String, toCode: String) {
        try {
            val have = downloadedModels()
            for (model in listOfNotNull(modelFor(fromCode), modelFor(toCode)).distinct()) {
                if (model == ENGLISH || model in have) continue
                _progress.value = "Downloading the ${nameFor(model)} model\u2026"
                downloadModel(model)
            }
            _lastError.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _lastError.value = LocalTranslationProblem.Failed(e.message ?: "unknown error")
            throw e
        } finally {
            _progress.value = null
        }
    }

    override suspend fun setUp(): String = status()

    override suspend fun packages(): List<LocalPackage> {
        val have = downloadedModels()
        return supportedModels().filter { it != ENGLISH }.map { model ->
            LocalPackage(fromCode = codeFor(model), toCode = "en", fromName = nameFor(model), toName = "English", installed = model in have, label = nameFor(model))
        }.sortedBy { it.title }
    }

    override suspend fun installPackage(fromCode: String, toCode: String) {
        val model = modelFor(fromCode) ?: error("no model for $fromCode")
        _progress.value = "Downloading the ${nameFor(model)} model\u2026"
        try { downloadModel(model) } finally { _progress.value = null }
    }

    override suspend fun removePackage(fromCode: String, toCode: String) {
        val model = modelFor(fromCode) ?: error("no model for $fromCode")
        deleteModel(model)
    }

    protected companion object {
        const val ENGLISH = "en"
    }
}
