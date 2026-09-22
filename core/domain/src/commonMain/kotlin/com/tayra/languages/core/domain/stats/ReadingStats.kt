package com.tayra.languages.core.domain.stats

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Words read on a day, for one language. */
data class DailyWordCount(val languageName: String, val date: LocalDate, val wordCount: Int)

data class ReadCounts(val day: Int, val week: Int, val month: Int, val year: Int, val total: Int)

data class LanguageReadCounts(val languageName: String, val counts: ReadCounts)

data class ChartPoint(val date: LocalDate, val wordCount: Int, val runningTotal: Int)

object ReadingStats {

    fun byLanguage(entries: List<DailyWordCount>): Map<String, Map<LocalDate, Int>> =
        entries.groupBy { it.languageName }
            .mapValues { (_, days) -> days.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.wordCount } } }

    fun tableData(entries: List<DailyWordCount>, today: LocalDate): List<LanguageReadCounts> =
        byLanguage(entries).map { (language, byDate) -> LanguageReadCounts(language, counts(byDate, today)) }
            .sortedBy { it.languageName }

    fun counts(byDate: Map<LocalDate, Int>, today: LocalDate): ReadCounts {
        fun inLastDays(days: Int): Int {
            val start = today.minus(days, DateTimeUnit.DAY)
            return byDate.entries.filter { it.key >= start && it.key <= today }.sumOf { it.value }
        }
        return ReadCounts(
            day = inLastDays(0),
            week = inLastDays(6),
            month = inLastDays(29),
            year = inLastDays(364),
            total = inLastDays(3650),
        )
    }

    /** Chart data per language, with a zero point the day before the first read date. */
    fun chartData(entries: List<DailyWordCount>): Map<String, List<ChartPoint>> =
        byLanguage(entries).mapValues { (_, byDate) ->
            val dates = byDate.keys.sorted()
            if (dates.isEmpty()) return@mapValues emptyList()
            val points = mutableListOf(ChartPoint(dates.first().minus(1, DateTimeUnit.DAY), 0, 0))
            var total = 0
            for (date in dates) {
                val count = byDate.getValue(date)
                total += count
                points.add(ChartPoint(date, count, total))
            }
            points
        }

    /** Consecutive days with reading, ending today or yesterday. */
    fun streak(readDates: Set<LocalDate>, today: LocalDate): Int {
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        if (today !in readDates && yesterday !in readDates) return 0
        var streak = 0
        var current = if (today in readDates) today else yesterday
        while (current in readDates) {
            streak++
            current = current.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

}
