package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.WordsReadRepository
import com.tayra.languages.core.domain.stats.LanguageActivity
import com.tayra.languages.core.domain.stats.LanguageOverview
import com.tayra.languages.core.domain.stats.LearningStatsRepository
import com.tayra.languages.core.domain.stats.ChartPoint
import com.tayra.languages.core.domain.stats.LanguageReadCounts
import com.tayra.languages.core.domain.stats.ReadingStats
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock

data class ReadingStatsSummary(
    val table: List<LanguageReadCounts>,
    val chart: Map<String, List<ChartPoint>>,
    val streak: Int,
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
            streak = ReadingStats.streak(entries.mapTo(HashSet()) { it.date }, today),
        )
    }

    /** The progress in the language with [languageId], named [languageName] as the words-read records name it. */
    suspend fun overview(languageId: Long, languageName: String): LanguageOverview {
        val today = today()
        val byDate = ReadingStats.byLanguage(wordsRead.dailyCounts())[languageName].orEmpty().filterValues { it > 0 }
        val activity = learning.activity(languageId)
        val statuses = activity.termsByStatus
        val reviewsTotal = activity.reviews.values.sum()
        return LanguageOverview(
            languageName = languageName,
            wordsRead = byDate.values.sum(),
            wordsReadThisWeek = ReadingStats.counts(byDate, today).week,
            daysRead = byDate.size,
            wordsPerReadingDay = if (byDate.isEmpty()) 0 else byDate.values.sum() / byDate.size,
            streak = ReadingStats.streak(byDate.keys, today),
            longestStreak = ReadingStats.longestStreak(byDate.keys),
            readingDays = byDate,
            known = statuses[TermStatus.WELL_KNOWN] ?: 0,
            learning = statuses.filterKeys { it.isLearning }.values.sum(),
            ignored = statuses[TermStatus.IGNORED] ?: 0,
            termsByStatus = statuses,
            savedByWeek = ReadingStats.weekly(activity.termsSaved, today, SAVED_WEEKS),
            savedThisWeek = ReadingStats.weekly(activity.termsSaved, today, 1).single(),
            reviewsByDay = activity.reviews,
            reviewsTotal = reviewsTotal,
            rememberedPercent = if (reviewsTotal == 0) null else (100.0 * activity.reviewsRemembered / reviewsTotal).roundToInt(),
            pagesRead = activity.pagesRead,
            booksFinished = activity.booksFinished,
            today = today,
        )
    }

    suspend fun streak(): Int = ReadingStats.streak(wordsRead.dailyCounts().mapTo(HashSet()) { it.date }, today())

    private companion object {
        const val SAVED_WEEKS = 12
    }
}
