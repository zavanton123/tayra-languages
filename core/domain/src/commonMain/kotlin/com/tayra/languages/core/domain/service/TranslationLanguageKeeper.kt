package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.backup.BackupRepository
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.language.Scripts
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.SavedTranslation
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Keeps saved term translations in the native language: whenever it changes, and at start-up,
 * each translation written in another language is replaced with the offline dictionary's gloss
 * or the translation engine's answer. One that cannot be translated yet is kept and retried on
 * the next start. Translations saved before their language was recorded count as native when
 * they are in the native language's script.
 *
 * A translation replaced is lost, so the data is backed up just before the first one is; when the
 * backup cannot be made, none is replaced, and they are tried again on the next start.
 */
class TranslationLanguageKeeper(
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val dictionary: OfflineDictionary,
    private val suggestions: TermTranslationProvider,
    private val settings: SettingsRepository,
    /** Where the backup goes; null replaces without one, as in tests. */
    private val backups: BackupRepository? = null,
) {
    private val _backedUp = MutableStateFlow<Int?>(null)

    /** How many translations were replaced after a backup was made for them, until [noticed]. */
    val backedUp: StateFlow<Int?> = _backedUp.asStateFlow()

    fun noticed() {
        _backedUp.value = null
    }

    fun start(scope: CoroutineScope): Job = scope.launch {
        settings.settings.map { native(it.nativeLanguage) }.distinctUntilChanged().collectLatest { update(it) }
    }

    /** Returns how many translations were replaced. */
    suspend fun update(native: String): Int {
        val stale = terms.translationsNotIn(native)
        if (stale.isEmpty()) return 0
        val (legacyNative, foreign) = stale.partition { it.language == null && inScriptOf(native, it.translation) }
        terms.markTranslationLanguage(legacyNative.map { it.termId }, native)
        var backedUp: Boolean? = null
        var replaced = 0
        for ((languageId, rows) in foreign.groupBy { it.languageId }) {
            val language = languages.getById(languageId) ?: continue
            for (row in rows) {
                val clean = translationFor(row, language, native) ?: continue
                if (backedUp == null) backedUp = backUp()
                if (backedUp != true) return replaced
                if (terms.replaceTranslation(row.termId, expected = row.translation, translation = clean, language = native)) replaced++
            }
        }
        if (replaced > 0 && backups != null) _backedUp.update { (it ?: 0) + replaced }
        return replaced
    }

    /** Whether the data was backed up, or there is nowhere to back it up to. */
    private suspend fun backUp(): Boolean {
        val backups = backups ?: return true
        return try {
            backups.create()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /** The translation to put in place of [row]'s, in [native]; null when there is none. */
    private suspend fun translationFor(row: SavedTranslation, language: Language, native: String): String? {
        val text = row.text.replace(ZWS_STRING, "").trim()
        val translation = attempt { offline(language, native, text) } ?: attempt { suggestions.suggestTranslation(text, language) }
        return translation?.trim()?.takeIf { it.isNotEmpty() && !it.equals(text, ignoreCase = true) }
    }

    private suspend fun offline(language: Language, native: String, text: String): String? {
        val pack = DictionaryPacks.find(LanguageCodes.codeFor(language.name), native)?.id ?: return null
        if (!dictionary.isAvailable(pack)) return null
        return dictionary.lookup(pack, text).suggestedTranslation
    }

    private fun inScriptOf(native: String, translation: String): Boolean {
        val script = Scripts.dominant(translation) ?: return true
        return script in Scripts.of(native)
    }

    private inline fun <T> attempt(block: () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun native(value: String): String = value.trim().lowercase().ifEmpty { "en" }
}
