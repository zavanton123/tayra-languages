package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.practice.PracticeProgressRepository
import com.tayra.languages.core.domain.practice.WordProgress
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class PracticeProgressRepositoryImpl(private val provider: DatabaseProvider) : PracticeProgressRepository {

    override suspend fun progress(termIds: Collection<Long>): Map<Long, WordProgress> = withContext(databaseDispatcher) {
        if (termIds.isEmpty()) return@withContext emptyMap()
        termIds.chunked(CHUNK).flatMap { ids -> provider.database().practiceQueries.selectProgress(ids).awaitAsList() }.associate {
            it.term_id to WordProgress(
                it.term_id, it.level.toInt().coerceIn(0, WordProgress.TOP), it.streak.toInt(), it.asked.toInt(), it.right_answers.toInt(),
                it.last_practised?.let(Instant::fromEpochMilliseconds),
            )
        }
    }

    override suspend fun save(progress: WordProgress) = withContext(databaseDispatcher) {
        provider.database().practiceQueries.upsertProgress(
            progress.termId, progress.level.toLong(), progress.streak.toLong(), progress.asked.toLong(), progress.right.toLong(),
            progress.lastPractised?.toEpochMilliseconds(),
        )
        Unit
    }

    private companion object {
        const val CHUNK = 500
    }
}
