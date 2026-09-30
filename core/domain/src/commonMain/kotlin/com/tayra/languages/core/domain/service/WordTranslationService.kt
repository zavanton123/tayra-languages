package com.tayra.languages.core.domain.service

import kotlin.coroutines.cancellation.CancellationException
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * A short translation for a single word, for hover tooltips. Asks, in order: the saved term,
 * its parent, the installed offline dictionary, then the chosen translation engine. Answers
 * that did not come from a saved term are cached, and a miss is retried after a minute.
 */
class WordTranslationService(
    private val terms: TermRepository,
    private val dictionary: OfflineDictionary,
    private val suggestions: TermTranslationProvider,
    private val settings: SettingsRepository,
    private val maxEntries: Int = 1000,
) {
    private class Entry(val translation: String?, val at: TimeMark)

    private val lock = Mutex()
    private val cache = LinkedHashMap<String, Entry>()

    suspend fun translate(language: Language, word: String): String? {
        val clean = word.replace(ZWS_STRING, "").trim()
        if (clean.isEmpty() || clean.none { it.isLetter() }) return null
        val lower = language.lowercase(clean)
        saved(language, lower)?.let { return it }

        val native = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        val key = "${language.id}|$native|${settings.current.translationEngine.name}|$lower"
        lock.withLock { cache[key] }?.let { entry ->
            if (entry.translation != null || entry.at.elapsedNow().inWholeMilliseconds < MISS_TTL_MS) return entry.translation
        }
        val found = offline(language, native, clean) ?: attempt { suggestions.suggestTranslation(clean, language) }
        val translation = found?.trim()?.takeIf { it.isNotEmpty() && !it.equals(clean, ignoreCase = true) }
        lock.withLock {
            cache.remove(key)
            cache[key] = Entry(translation, TimeSource.Monotonic.markNow())
            while (cache.size > maxEntries) cache.remove(cache.keys.first())
        }
        return translation
    }

    /** The word's own saved translation, or its parent's; read every time since the user edits them. */
    private suspend fun saved(language: Language, lower: String): String? {
        if (language.id == 0L) return null
        val term = terms.findByTextLc(language.id, lower) ?: return null
        term.translation?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val id = term.id.takeIf { it != 0L } ?: return null
        return terms.parents(id).firstNotNullOfOrNull { parent -> parent.translation?.trim()?.takeIf { it.isNotEmpty() } }
    }

    private suspend fun offline(language: Language, native: String, word: String): String? {
        val pack = DictionaryPacks.find(LanguageCodes.codeFor(language.name), native)?.id ?: return null
        if (!dictionary.isAvailable(pack)) return null
        return attempt { dictionary.lookup(pack, word).suggestedTranslation }
    }

    /** Null when [block] fails; a cancelled caller stays cancelled, so nothing is cached for it. */
    private inline fun <T> attempt(block: () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val MISS_TTL_MS = 60_000L
    }
}
