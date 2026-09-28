package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Language
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** Translates whole sentences into the native language, e.g. through an online service. */
interface SentenceTranslator {
    /** The translation, or null when the service has none or is unavailable. Never throws. */
    suspend fun translate(text: String, language: Language): String?
}

/** State of one sentence's translation on the reading page. */
sealed interface SentenceTranslation {
    data object Loading : SentenceTranslation
    data class Done(val text: String) : SentenceTranslation
    data object Unavailable : SentenceTranslation
}

/**
 * Serialises requests to the wrapped translator, spaces them out, and remembers results so a
 * page re-render or a return to the page costs no network calls. Failures are remembered only
 * briefly so a flaky connection gets another chance.
 */
class CachedSentenceTranslator(
    private val inner: SentenceTranslator,
    private val settings: () -> String,
    private val minIntervalMs: Long = 250,
    private val maxEntries: Int = 2_000,
    private val failureTtlMs: Long = 60_000,
) : SentenceTranslator {
    private val cache = LinkedHashMap<String, Entry>()
    private val lock = Mutex()
    private var lastRequestAt = 0L

    private class Entry(val text: String?, val at: Long)

    override suspend fun translate(text: String, language: Language): String? {
        val key = "${language.id}|${settings()}|$text"
        val now = Clock.System.now().toEpochMilliseconds()
        lock.withLock {
            val cached = cache[key]
            if (cached != null && (cached.text != null || now - cached.at < failureTtlMs)) {
                // Refresh recency for the LRU.
                cache.remove(key); cache[key] = cached
                return cached.text
            }
            val wait = minIntervalMs - (now - lastRequestAt)
            if (wait > 0) delay(wait)
            val result = inner.translate(text, language)
            lastRequestAt = Clock.System.now().toEpochMilliseconds()
            cache[key] = Entry(result, lastRequestAt)
            while (cache.size > maxEntries) cache.remove(cache.keys.first())
            return result
        }
    }
}
