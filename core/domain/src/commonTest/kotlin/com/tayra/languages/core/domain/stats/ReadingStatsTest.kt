package com.tayra.languages.core.domain.stats

import com.tayra.languages.core.domain.render.TextItem
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingStatsTest {
    private val today = LocalDate(2026, 9, 22)

    @Test
    fun streakCountsConsecutiveDays() {
        val dates = setOf(LocalDate(2026, 9, 22), LocalDate(2026, 9, 21), LocalDate(2026, 9, 20), LocalDate(2026, 9, 10))
        assertEquals(3, ReadingStats.streak(dates, today))
        assertEquals(2, ReadingStats.streak(dates - LocalDate(2026, 9, 22), today))
        assertEquals(0, ReadingStats.streak(setOf(LocalDate(2026, 9, 10)), today))
    }

    @Test
    fun countsPerPeriod() {
        val entries = listOf(
            DailyWordCount("English", today, 10),
            DailyWordCount("English", LocalDate(2026, 9, 18), 20),
            DailyWordCount("English", LocalDate(2026, 8, 1), 30),
            DailyWordCount("Spanish", LocalDate(2025, 1, 1), 5),
        )
        val table = ReadingStats.tableData(entries, today)
        assertEquals(ReadCounts(day = 10, week = 30, month = 30, year = 60, total = 60), table[0].counts)
        assertEquals(ReadCounts(day = 0, week = 0, month = 0, year = 0, total = 5), table[1].counts)
        val chart = ReadingStats.chartData(entries).getValue("English")
        assertEquals(LocalDate(2026, 7, 31), chart.first().date)
        assertEquals(60, chart.last().runningTotal)
    }

    @Test
    fun bookStatsDistribution() {
        fun item(text: String, status: TermStatus?) = TextItem(0, text, text, 1, 0,
            status?.let { Term(id = 1, languageId = 1, text = text, textLc = text, status = it) })
        val items = listOf(item("a", TermStatus.UNKNOWN), item("a", TermStatus.UNKNOWN), item("b", TermStatus.NEW_1), item(" ", null))
        val stats = BookStatsCalculator.calculate(items)
        assertEquals(2, stats.distinctTerms)
        assertEquals(1, stats.distinctUnknowns)
        assertEquals(50, stats.unknownPercent)
    }
}
