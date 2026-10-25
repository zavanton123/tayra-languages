package com.tayra.languages.core.domain.service

import kotlin.coroutines.cancellation.CancellationException
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.MeaningChooser
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.repository.SentenceTranslationCache
import com.tayra.languages.core.domain.repository.TermRepository
import kotlin.time.Clock
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * A short translation for a single word, for hover tooltips. Asks, in order: the saved term,
 * its parent, the installed offline dictionary, then the chosen translation engine. The engine's
 * answers are kept in [store] for a week, so they outlast a restart; all answers not from a saved
 * term are kept in memory too, and a miss is retried after a minute.
 */
class WordTranslationService(
    private val terms: TermRepository,
    private val dictionary: OfflineDictionary,
    private val suggestions: TermTranslationProvider,
    private val settings: SettingsRepository,
    private val maxEntries: Int = 1000,
    /** Translates a word's sentence and the word, for the dictionary meaning the sentence uses; without it, the first meaning. */
    private val sentences: SentenceTranslator? = null,
    /** Where the engine's word translations are kept for a week; only in memory without it. */
    private val store: SentenceTranslationCache? = null,
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
        val found = offline(language, native, clean) ?: engine(language, native, clean)
        val translation = found?.trim()?.takeIf { it.isNotEmpty() && !it.equals(clean, ignoreCase = true) }
        lock.withLock {
            cache.remove(key)
            cache[key] = Entry(translation, TimeSource.Monotonic.markNow())
            while (cache.size > maxEntries) cache.remove(cache.keys.first())
        }
        return translation
    }

    /**
     * The translation of [word] as [sentence] uses it: the saved one, else the offline dictionary's
     * meaning the sentence's translation points to, else what [translate] gives.
     */
    suspend fun translateIn(language: Language, word: String, sentence: String?): String? {
        val clean = word.replace(ZWS_STRING, "").trim()
        if (sentence.isNullOrBlank() || clean.isEmpty()) return translate(language, word)
        saved(language, language.lowercase(clean))?.let { return it }
        val native = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        val code = LanguageCodes.codeFor(language.name)
        val pack = DictionaryPacks.find(code, native)?.id?.takeIf { dictionary.isAvailable(it) } ?: return translate(language, word)
        val lookup = attempt { dictionary.lookup(pack, clean) } ?: return translate(language, word)
        val translator = sentences
        val choice = MeaningChooser.choose(
            lookup, clean, sentence, code,
            translator?.let { attempt { it.translate(sentence, language) } }, translator?.let { attempt { it.translate(clean, language) } },
        )
        return choice?.gloss ?: translate(language, word)
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

    /** The engine's translation of [word], from [store] when it was asked within the week. */
    private suspend fun engine(language: Language, native: String, word: String): String? {
        // Keyed apart from sentences, and by engine, so switching engines never mixes their answers.
        val target = "word:${settings.current.translationEngine.name}:$native"
        val at = Clock.System.now().toEpochMilliseconds()
        attempt { store?.get(word, target, at - TRANSLATION_TTL_MS) }?.let { return it }
        return attempt { suggestions.suggestTranslation(word, language) }?.also { found ->
            attempt { store?.put(word, target, found, at) }
        }
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
