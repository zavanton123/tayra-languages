package com.tayra.languages.core.domain.repository

/** Stored sentence translations, keyed by the sentence and the language it was translated into. */
interface SentenceTranslationCache {
    /** The stored translation made after [notBefore] (epoch millis), or null. */
    suspend fun get(sentence: String, targetLanguage: String, notBefore: Long): String?
    suspend fun put(sentence: String, targetLanguage: String, translation: String, createdAt: Long)
    /** Drops entries older than [cutoff] (epoch millis). */
    suspend fun prune(cutoff: Long)
    suspend fun clear()
}
