package com.idiomiko.core.domain.service

import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.repository.WordsReadRepository
import com.idiomiko.core.domain.stats.LanguageActivity
import com.idiomiko.core.domain.stats.LanguageOverview
import com.idiomiko.core.domain.stats.LearningStatsRepository
import com.idiomiko.core.domain.stats.ChartPoint
import com.idiomiko.core.domain.stats.LanguageReadCounts
import com.idiomiko.core.domain.stats.ReadingStats
import com.idiomiko.core.domain.stats.SavedWord
import com.idiomiko.core.domain.stats.StudyActivity
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock

data class ReadingStatsSummary(
    val table: List<LanguageReadCounts>,
    val chart: Map<String, List<ChartPoint>>,
)

class StatsService(
    private val wordsRead: WordsReadRepository,
    private val learning: LearningStatsRepository = LearningStatsRepository { LanguageActivity() },
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    fun today(): LocalDate = clock.now().toLocalDateTime(timeZone).date

    suspend fun summary(): ReadingStatsSummary {
        val entries = wordsRead.dailyCounts()
        val today = today()
        return ReadingStatsSummary(
            table = ReadingStats.tableData(entries, today),
            chart = ReadingStats.chartData(entries),
        )
    }

    /** The progress in the language with [languageId], named [languageName] as the words-read records name it. */
    suspend fun overview(languageId: Long, languageName: String): LanguageOverview {
        val today = today()
        val byDate = ReadingStats.byLanguage(wordsRead.dailyCounts())[languageName].orEmpty().filterValues { it > 0 }
        val activity = learning.activity(languageId)
        // A day of flashcards or saved words keeps the streak as a day of reading does, as on the activity squares.
        val study = study(byDate, activity, today)
        val statuses = activity.termsByStatus
        val reviewsTotal = activity.reviews.values.sum()
        return LanguageOverview(
            languageName = languageName,
            wordsRead = byDate.values.sum(),
            wordsReadThisWeek = ReadingStats.counts(byDate, today).week,
            daysRead = byDate.size,
            wordsPerReadingDay = if (byDate.isEmpty()) 0 else byDate.values.sum() / byDate.size,
            streak = study.streak,
            longestStreak = study.longestStreak,
            study = study,
            readingDays = byDate,
            known = statuses[TermStatus.WELL_KNOWN] ?: 0,
            learning = statuses.filterKeys { it.isLearning }.values.sum(),
            ignored = statuses[TermStatus.IGNORED] ?: 0,
            termsByStatus = statuses,
            savedByWeek = ReadingStats.weekly(activity.termsSaved, today, SAVED_WEEKS),
            savedThisWeek = ReadingStats.weekly(activity.termsSaved, today, 1).single(),
            savedToday = activity.termsSaved[today] ?: 0,
            reviewsByDay = activity.reviews,
            reviewsTotal = reviewsTotal,
            rememberedPercent = if (reviewsTotal == 0) null else (100.0 * activity.reviewsRemembered / reviewsTotal).roundToInt(),
            pagesRead = activity.pagesRead,
            booksFinished = activity.booksFinished,
            today = today,
        )
    }

    /** The study in the language with [languageId], named [languageName] as the words-read records name it. */
    suspend fun studyActivity(languageId: Long, languageName: String): StudyActivity {
        val read = ReadingStats.byLanguage(wordsRead.dailyCounts())[languageName].orEmpty()
        return study(read, learning.activity(languageId), today())
    }

    /** The one rule for a study day, behind the streaks and the activity squares alike. */
    private fun study(read: Map<LocalDate, Int>, activity: LanguageActivity, today: LocalDate): StudyActivity =
        StudyActivity.of(read, activity.reviews, activity.termsSaved, today, activity.reviewTime, activity.averageAnswer)

    /** The words counted as saved today in the language with [languageId], the latest first. */
    suspend fun savedToday(languageId: Long): List<SavedWord> = learning.savedOn(languageId, today())

    /** The last [limit] words saved to learn in the language with [languageId], the latest first. */
    suspend fun recentlySaved(languageId: Long, limit: Int): List<SavedWord> = learning.recentlySaved(languageId, limit)

    private companion object {
        const val SAVED_WEEKS = 12
    }
}
