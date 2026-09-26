package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.db.SqlDriver
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionarySense
import com.tayra.languages.core.domain.repository.DictionaryForm
import com.tayra.languages.core.domain.repository.DictionaryRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class DictionaryRepositoryImpl(private val provider: DatabaseProvider) : DictionaryRepository {

    override suspend fun importedFormat(dictionary: DictionaryId): Int? = withContext(databaseDispatcher) {
        provider.database().dictionaryQueries.selectImport(dictionary.name).awaitAsOneOrNull()?.format?.toInt()
    }

    override suspend fun entries(dictionary: DictionaryId, wordLc: String): List<DictionaryEntry> = withContext(databaseDispatcher) {
        provider.database().dictionaryQueries.selectEntries(dictionary.name, wordLc).awaitAsList().map { row ->
            DictionaryEntry(row.word, row.pos, row.ipa, parseSenses(row.senses))
        }
    }

    override suspend fun lemmas(dictionary: DictionaryId, formLc: String): List<String> = withContext(databaseDispatcher) {
        provider.database().dictionaryQueries.selectLemmas(dictionary.name, formLc).awaitAsList()
    }

    override suspend fun replace(dictionary: DictionaryId, format: Int, entries: List<DictionaryEntry>, forms: List<DictionaryForm>) {
        withContext(databaseDispatcher) {
            val db = provider.database()
            val driver = provider.driver()
            db.transaction {
                db.dictionaryQueries.deleteImport(dictionary.name)
                db.dictionaryQueries.deleteEntries(dictionary.name)
                db.dictionaryQueries.deleteForms(dictionary.name)
                // Multi-row inserts keep the import to a few hundred statements, which matters on the
                // web worker driver where every statement is a message round trip.
                entries.chunked(ROWS_PER_STATEMENT).forEach { chunk -> insertEntries(driver, dictionary, chunk) }
                forms.chunked(ROWS_PER_STATEMENT).forEach { chunk -> insertForms(driver, dictionary, chunk) }
                db.dictionaryQueries.upsertImport(dictionary.name, format.toLong(), entries.size.toLong(), Clock.System.now().toEpochMilliseconds())
            }
        }
    }

    private suspend fun insertEntries(driver: SqlDriver, dictionary: DictionaryId, chunk: List<DictionaryEntry>) {
        val sql = "INSERT INTO dictionary_entries(dictionary, word, word_lc, pos, ipa, senses) VALUES " +
            chunk.joinToString(",") { "(?,?,?,?,?,?)" }
        driver.execute(null, sql, chunk.size * 6) {
            chunk.forEachIndexed { i, e ->
                val base = i * 6
                bindString(base, dictionary.name)
                bindString(base + 1, e.word)
                bindString(base + 2, e.word.lowercase())
                bindString(base + 3, e.pos)
                bindString(base + 4, e.ipa)
                bindString(base + 5, storeSenses(e.senses))
            }
        }.await()
    }

    private suspend fun insertForms(driver: SqlDriver, dictionary: DictionaryId, chunk: List<DictionaryForm>) {
        val sql = "INSERT INTO dictionary_forms(dictionary, form_lc, lemma, generated) VALUES " + chunk.joinToString(",") { "(?,?,?,?)" }
        driver.execute(null, sql, chunk.size * 4) {
            chunk.forEachIndexed { i, f ->
                val base = i * 4
                bindString(base, dictionary.name)
                bindString(base + 1, f.form.lowercase())
                bindString(base + 2, f.lemma)
                bindLong(base + 3, if (f.generated) 1 else 0)
            }
        }.await()
    }

    private companion object {
        /** Keeps each statement under SQLite's conservative 999-variable limit. */
        const val ROWS_PER_STATEMENT = 150
        val senseListSerializer = ListSerializer(StoredSense.serializer())

        fun parseSenses(value: String): List<DictionarySense> =
            Json.decodeFromString(senseListSerializer, value).map { DictionarySense(it.glosses, it.tags) }

        fun storeSenses(senses: List<DictionarySense>): String =
            Json.encodeToString(senseListSerializer, senses.map { StoredSense(it.glosses, it.tags) })
    }
}

@Serializable
private data class StoredSense(val glosses: List<String>, val tags: List<String> = emptyList())
