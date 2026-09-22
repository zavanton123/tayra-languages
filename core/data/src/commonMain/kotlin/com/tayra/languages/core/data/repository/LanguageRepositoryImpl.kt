package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.Languages
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.LanguageSummary
import com.tayra.languages.core.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class LanguageRepositoryImpl(private val provider: DatabaseProvider) : LanguageRepository {

    private suspend fun db(): TayraDatabase = provider.database()

    override fun observeAll(): Flow<List<Language>> = flow {
        val database = db()
        database.languagesQueries.selectAll().asFlow().mapToList(databaseDispatcher).collect { rows ->
            emit(withDictionaries(database, rows))
        }
    }

    override fun observeSummaries(): Flow<List<LanguageSummary>> = flow {
        val database = db()
        database.languagesQueries.summaries().asFlow().mapToList(databaseDispatcher).collect { rows ->
            emit(rows.map { LanguageSummary(it.id, it.name, it.book_count.toInt(), it.term_count.toInt()) })
        }
    }

    override suspend fun getAll(): List<Language> = withContext(databaseDispatcher) {
        val database = db()
        withDictionaries(database, database.languagesQueries.selectAll().awaitAsList())
    }

    override suspend fun getById(id: Long): Language? = withContext(databaseDispatcher) {
        val database = db()
        val row = database.languagesQueries.selectById(id).awaitAsOneOrNull() ?: return@withContext null
        withDictionaries(database, listOf(row)).single()
    }

    override suspend fun findByName(name: String): Language? = withContext(databaseDispatcher) {
        val database = db()
        val row = database.languagesQueries.selectByName(name).awaitAsOneOrNull() ?: return@withContext null
        withDictionaries(database, listOf(row)).single()
    }

    override suspend fun save(language: Language): Long = withContext(databaseDispatcher) {
        val database = db()
        val q = database.languagesQueries
        database.transactionWithResult {
            val id = if (language.id == 0L) {
                q.insert(
                    name = language.name,
                    characterSubstitutions = language.characterSubstitutions,
                    regexpSplitSentences = language.regexpSplitSentences,
                    exceptionsSplitSentences = language.exceptionsSplitSentences,
                    wordCharacters = language.wordCharacters,
                    rightToLeft = language.rightToLeft,
                    showRomanization = language.showRomanization,
                    parserType = language.parserType,
                )
                q.lastInsertId().awaitAsOne()
            } else {
                q.update(
                    id = language.id,
                    name = language.name,
                    characterSubstitutions = language.characterSubstitutions,
                    regexpSplitSentences = language.regexpSplitSentences,
                    exceptionsSplitSentences = language.exceptionsSplitSentences,
                    wordCharacters = language.wordCharacters,
                    rightToLeft = language.rightToLeft,
                    showRomanization = language.showRomanization,
                    parserType = language.parserType,
                )
                language.id
            }
            q.deleteDictionaries(id)
            language.dictionaries.forEachIndexed { index, dict ->
                q.insertDictionary(
                    languageId = id,
                    useFor = dict.useFor.key,
                    dictType = dict.type.key,
                    url = dict.url,
                    isActive = dict.isActive,
                    sortOrder = (index + 1).toLong(),
                )
            }
            id
        }
    }

    override suspend fun delete(id: Long) {
        withContext(databaseDispatcher) {
            db().languagesQueries.delete(id)
        }
    }

    private suspend fun withDictionaries(database: TayraDatabase, rows: List<Languages>): List<Language> {
        if (rows.isEmpty()) return emptyList()
        val dictionaries = database.languagesQueries.selectDictionaries(rows.map { it.id }).awaitAsList()
            .groupBy { it.language_id }
        return rows.map { it.toDomain(dictionaries[it.id] ?: emptyList()) }
    }
}
