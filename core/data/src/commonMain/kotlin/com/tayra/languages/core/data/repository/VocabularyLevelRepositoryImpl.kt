package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.frequency.KnownWord
import com.tayra.languages.core.domain.frequency.LevelChange
import com.tayra.languages.core.domain.frequency.VocabularyLevelRepository
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class VocabularyLevelRepositoryImpl(
    private val provider: DatabaseProvider,
    private val clock: Clock = Clock.System,
) : VocabularyLevelRepository {

    override fun observeLevel(languageId: Long): Flow<Int?> = flow {
        val query = provider.database().vocabularyLevelsQueries.selectLevel(languageId)
        emitAll(query.asFlow().mapToOneOrNull(databaseDispatcher).map { it?.toInt() })
    }

    override suspend fun level(languageId: Long): Int? = withContext(databaseDispatcher) {
        provider.database().vocabularyLevelsQueries.selectLevel(languageId).awaitAsOneOrNull()?.toInt()
    }

    override suspend fun setLevel(languageId: Long, level: Int, known: List<KnownWord>): LevelChange = withContext(databaseDispatcher) {
        val database = provider.database()
        val levels = database.vocabularyLevelsQueries
        val terms = database.termsQueries
        val knownStatus = TermStatus.WELL_KNOWN.value.toLong()
        val now = clock.now().toEpochMilliseconds()
        database.transactionWithResult {
            val from = levels.selectLevel(languageId).awaitAsOneOrNull()?.toInt() ?: 0
            val keep = known.flatMapTo(HashSet()) { it.forms + it.textLc }

            // Terms a higher level saved go, unless the reader has changed them since.
            val dropped = levels.selectLevelTerms(languageId).awaitAsList().filter { it.text_lc !in keep }
            val removed = dropped.filter { it.status == knownStatus }.map { it.id }
            removed.chunked(CHUNK).forEach { terms.deleteByIds(it) }
            dropped.map { it.id }.chunked(CHUNK).forEach { levels.deleteLevelTerms(it) }

            // By lowercase text: the terms there were, and the ones saved here, since words share forms ("foi").
            val existing = levels.selectWordTerms(languageId).awaitAsList().associateTo(HashMap()) { it.text_lc to (it.id to it.status) }
            var added = 0
            suspend fun knownTerm(text: String, textLc: String): Long {
                existing[textLc]?.let { (id, status) ->
                    // An unknown term is one the reader has not judged yet; anything else is left alone.
                    if (status == TermStatus.UNKNOWN.value.toLong()) {
                        terms.updateStatus(status = knownStatus, changedAt = now, ids = listOf(id))
                        levels.insertLevelTerm(id)
                        existing[textLc] = id to knownStatus
                        added++
                    }
                    return id
                }
                terms.insert(
                    languageId = languageId, text = text, textLc = textLc, status = knownStatus, translation = null, romanization = null,
                    tokenCount = 1, syncStatus = false, flashMessage = null, createdAt = now, statusChangedAt = now,
                    translationLanguage = null, sentence = null,
                )
                val id = terms.lastInsertId().awaitAsOne()
                levels.insertLevelTerm(id)
                existing[textLc] = id to knownStatus
                added++
                return id
            }
            for (word in known) {
                val wordId = knownTerm(word.text, word.textLc)
                for (form in word.forms) {
                    // A form the reader saved, or another word's, keeps the links it has.
                    if (form in existing) continue
                    terms.insertParent(knownTerm(form, form), wordId)
                }
            }
            levels.upsertLevel(languageId, level.toLong())
            LevelChange(from, level, added, removed.size)
        }
    }

    private companion object {
        const val CHUNK = 500
    }
}
