package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.data.dictionary.DictionaryDatabaseProvider
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionarySense
import com.tayra.languages.core.domain.repository.DictionaryRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class DictionaryRepositoryImpl(private val databases: DictionaryDatabaseProvider) : DictionaryRepository {

    override suspend fun isAvailable(dictionary: DictionaryId): Boolean = withContext(databaseDispatcher) {
        databases.database(dictionary) != null
    }

    override suspend fun entries(dictionary: DictionaryId, wordLc: String): List<DictionaryEntry> = withContext(databaseDispatcher) {
        val db = databases.database(dictionary) ?: return@withContext emptyList()
        db.dictionaryQueries.selectEntries(wordLc).awaitAsList().map { row ->
            DictionaryEntry(row.word, row.pos, row.ipa, parseSenses(row.senses))
        }
    }

    override suspend fun lemmas(dictionary: DictionaryId, formLc: String): List<String> = withContext(databaseDispatcher) {
        databases.database(dictionary)?.dictionaryQueries?.selectLemmas(formLc)?.awaitAsList().orEmpty()
    }

    override suspend fun close(dictionary: DictionaryId) {
        withContext(databaseDispatcher) { databases.close(dictionary) }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
        val senseListSerializer = ListSerializer(StoredSense.serializer())

        fun parseSenses(value: String): List<DictionarySense> =
            json.decodeFromString(senseListSerializer, value).map { DictionarySense(it.glosses, it.tags) }
    }
}

/** The JSON stored in the senses column. */
@Serializable
private data class StoredSense(val glosses: List<String>, val tags: List<String> = emptyList())
