package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.repository.SentenceTranslationCache
import kotlinx.coroutines.withContext

class SentenceTranslationCacheImpl(private val provider: DatabaseProvider) : SentenceTranslationCache {

    override suspend fun get(sentence: String, targetLanguage: String, notBefore: Long): String? = withContext(databaseDispatcher) {
        provider.database().sentenceTranslationsQueries.select(sentence, targetLanguage, notBefore).awaitAsOneOrNull()?.translation
    }

    override suspend fun put(sentence: String, targetLanguage: String, translation: String, createdAt: Long) {
        withContext(databaseDispatcher) {
            provider.database().sentenceTranslationsQueries.upsert(sentence, targetLanguage, translation, createdAt)
        }
    }

    override suspend fun prune(cutoff: Long) {
        withContext(databaseDispatcher) { provider.database().sentenceTranslationsQueries.deleteOlderThan(cutoff) }
    }
}
