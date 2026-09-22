package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.repository.WordsReadRepository
import com.tayra.languages.core.domain.stats.ChartPoint
import com.tayra.languages.core.domain.stats.LanguageReadCounts
import com.tayra.languages.core.domain.stats.ReadingStats
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class ReadingStatsSummary(
    val table: List<LanguageReadCounts>,
    val chart: Map<String, List<ChartPoint>>,
    val streak: Int,
)

class StatsService(
    private val wordsRead: WordsReadRepository,
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

    suspend fun streak(): Int = ReadingStats.streak(wordsRead.dailyCounts().mapTo(HashSet()) { it.date }, today())
}
