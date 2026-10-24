package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.practice.PracticeExampleRepository
import com.tayra.languages.core.domain.practice.StoredExample
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class PracticeExampleRepositoryImpl(private val provider: DatabaseProvider) : PracticeExampleRepository {

    override suspend fun examples(termIds: Collection<Long>): Map<Long, List<StoredExample>> = withContext(databaseDispatcher) {
        if (termIds.isEmpty()) return@withContext emptyMap()
        termIds.chunked(CHUNK).flatMap { ids -> provider.database().practiceQueries.selectExamples(ids).awaitAsList() }
            .map { StoredExample(it.term_id, it.sentence, it.translation, it.form, it.form_start.toInt(), it.source, it.source_id) }
            .groupBy { it.termId }
    }

    override suspend fun counts(termIds: Collection<Long>): Map<Long, Int> = withContext(databaseDispatcher) {
        if (termIds.isEmpty()) return@withContext emptyMap()
        termIds.chunked(CHUNK).flatMap { ids -> provider.database().practiceQueries.countExamples(ids).awaitAsList() }
            .associate { it.term_id to it.count.toInt() }
    }

    override suspend fun add(examples: List<StoredExample>, fetchedAt: Instant) = withContext(databaseDispatcher) {
        val queries = provider.database().practiceQueries
        for (e in examples) {
            queries.insertExample(e.termId, e.sentence, e.translation, e.form, e.formStart.toLong(), e.source, e.sourceId, fetchedAt.toEpochMilliseconds())
        }
    }

    override suspend fun clear(termId: Long) = withContext(databaseDispatcher) {
        provider.database().practiceQueries.deleteExamples(termId)
        Unit
    }

    private companion object {
        /** SQLite's limit on bound values is 999 on older versions. */
        const val CHUNK = 500
    }
}
