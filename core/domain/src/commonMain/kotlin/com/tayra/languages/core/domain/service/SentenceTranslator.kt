package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.SentenceTranslationCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** Translates whole sentences into the native language, e.g. through an online service. */
interface SentenceTranslator {
    /** The translation, or null when the service has none or is unavailable. Never throws. */
    suspend fun translate(text: String, language: Language): String?

    /** Forgets stored translations, if this translator keeps any. */
    suspend fun clearCache() {}
}

/** State of one sentence's translation on the reading page. */
sealed interface SentenceTranslation {
    data object Loading : SentenceTranslation
    /** [engine] is the service that produced [text], so the reader can say where translations come from. */
    data class Done(val text: String, val engine: TranslationEngine? = null) : SentenceTranslation
    data object Unavailable : SentenceTranslation
}

/**
 * Serialises requests to the wrapped translator, spaces them out, and stores results keyed by
 * sentence and target language: in memory for the session and in [store] for a day. Failures
 * are remembered only briefly so a flaky connection gets another chance.
 */
class CachedSentenceTranslator(
    private val inner: SentenceTranslator,
    private val store: SentenceTranslationCache,
    private val targetLanguage: () -> String,
    private val minIntervalMs: Long = 250,
    private val maxEntries: Int = 2_000,
    private val failureTtlMs: Long = 60_000,
    private val storeTtlMs: Long = 24L * 60 * 60 * 1000,
) : SentenceTranslator {
    private val memory = LinkedHashMap<String, Entry>()
    private val lock = Mutex()
    private var lastRequestAt = 0L
    private var lastPruneAt = 0L

    private class Entry(val text: String?, val at: Long)

    override suspend fun translate(text: String, language: Language): String? {
        val target = targetLanguage().trim().lowercase().ifEmpty { "en" }
        val key = "$target|$text"
        lock.withLock {
            val now = now()
            memory[key]?.let { cached ->
                if (cached.text != null || now - cached.at < failureTtlMs) {
                    memory.remove(key); memory[key] = cached // refresh recency
                    return cached.text
                }
            }
            if (now - lastPruneAt > PRUNE_INTERVAL_MS) {
                runCatching { store.prune(now - storeTtlMs) }
                lastPruneAt = now
            }
            runCatching { store.get(text, target, now - storeTtlMs) }.getOrNull()?.let { stored ->
                remember(key, Entry(stored, now))
                return stored
            }
            val wait = minIntervalMs - (now - lastRequestAt)
            if (wait > 0) delay(wait)
            val result = inner.translate(text, language)
            lastRequestAt = now()
            remember(key, Entry(result, lastRequestAt))
            if (result != null) runCatching { store.put(text, target, result, lastRequestAt) }
            return result
        }
    }

    override suspend fun clearCache() {
        lock.withLock {
            memory.clear()
            runCatching { store.clear() }
        }
    }

    private fun remember(key: String, entry: Entry) {
        memory[key] = entry
        while (memory.size > maxEntries) memory.remove(memory.keys.first())
    }

    private fun now() = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val PRUNE_INTERVAL_MS = 60L * 60 * 1000
    }
}

/** Sends sentences to the engine chosen in settings, falling back to the online one when no local engine exists. */
class RoutingSentenceTranslator(
    private val myMemory: SentenceTranslator,
    private val google: SentenceTranslator,
    private val azure: SentenceTranslator,
    private val alibaba: SentenceTranslator,
    private val baidu: SentenceTranslator,
    private val local: SentenceTranslator?,
    /** The effective engine, see [effectiveEngine]. */
    private val engine: () -> TranslationEngine,
) : SentenceTranslator {
    override suspend fun translate(text: String, language: Language): String? = when (engine()) {
        TranslationEngine.ARGOS -> (local ?: myMemory).translate(text, language)
        TranslationEngine.GOOGLE -> google.translate(text, language)
        TranslationEngine.AZURE -> azure.translate(text, language)
        TranslationEngine.ALIBABA -> alibaba.translate(text, language)
        TranslationEngine.BAIDU -> baidu.translate(text, language)
        TranslationEngine.MYMEMORY -> myMemory.translate(text, language)
    }
}
