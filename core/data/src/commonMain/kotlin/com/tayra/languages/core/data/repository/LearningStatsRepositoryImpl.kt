package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.stats.LanguageActivity
import com.tayra.languages.core.domain.stats.LearningStatsRepository
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class LearningStatsRepositoryImpl(
    private val provider: DatabaseProvider,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : LearningStatsRepository {

    override suspend fun activity(languageId: Long): LanguageActivity = withContext(databaseDispatcher) {
        val queries = provider.database().statsQueries
        val reviews = queries.reviewTimes(languageId).awaitAsList()
        LanguageActivity(
            termsByStatus = queries.termStatusCounts(languageId).awaitAsList()
                .mapNotNull { row -> TermStatus.fromValueOrNull(row.status.toInt())?.let { it to row.term_count.toInt() } }
                .groupBy({ it.first }, { it.second }).mapValues { (_, counts) -> counts.sum() },
            termsSaved = queries.termSavedTimes(languageId).awaitAsList().map { it.day() }.groupingBy { it }.eachCount(),
            reviews = reviews.map { it.reviewed_at.day() }.groupingBy { it }.eachCount(),
            reviewsRemembered = reviews.count { it.rating.toInt() != Rating.AGAIN.value },
            pagesRead = queries.pagesRead(languageId).awaitAsOne().toInt(),
            booksFinished = queries.booksFinished(languageId).awaitAsOne().toInt(),
        )
    }

    private fun Long.day(): LocalDate = toInstant().toLocalDateTime(timeZone).date
}
