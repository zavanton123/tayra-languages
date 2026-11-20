package com.idiomiko.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.db.databaseDispatcher
import com.idiomiko.core.domain.flashcards.Rating
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.stats.LanguageActivity
import com.idiomiko.core.domain.stats.LearningStatsRepository
import com.idiomiko.core.domain.stats.SavedWord
import com.idiomiko.core.domain.stats.StudyTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
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
            reviewTime = reviews.groupBy({ it.reviewed_at.day() }) { it.taken_ms?.milliseconds ?: StudyTime.UNTIMED_ANSWER }
                .mapValues { (_, times) -> times.fold(Duration.ZERO, Duration::plus) },
            averageAnswer = reviews.mapNotNull { it.taken_ms }.takeIf { it.isNotEmpty() }?.average()?.milliseconds,
            reviewsRemembered = reviews.count { it.rating.toInt() != Rating.AGAIN.value },
            pagesRead = queries.pagesRead(languageId).awaitAsOne().toInt(),
            booksFinished = queries.booksFinished(languageId).awaitAsOne().toInt(),
        )
    }

    override suspend fun savedOn(languageId: Long, day: LocalDate): List<SavedWord> = withContext(databaseDispatcher) {
        val since = day.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val until = day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
        provider.database().statsQueries.termsSavedBetween(languageId, since, until).awaitAsList().mapNotNull { row ->
            val status = TermStatus.fromValueOrNull(row.status.toInt()) ?: return@mapNotNull null
            SavedWord(row.id, row.text, row.translation?.takeIf { it.isNotBlank() }, status, row.created_at.toInstant())
        }
    }

    private fun Long.day(): LocalDate = toInstant().toLocalDateTime(timeZone).date
}
