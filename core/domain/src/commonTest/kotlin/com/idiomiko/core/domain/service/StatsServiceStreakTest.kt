package com.idiomiko.core.domain.service

import com.idiomiko.core.domain.model.WordsReadEntry
import com.idiomiko.core.domain.repository.WordsReadRepository
import com.idiomiko.core.domain.stats.DailyWordCount
import com.idiomiko.core.domain.stats.LanguageActivity
import com.idiomiko.core.domain.stats.LearningStatsRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** The streak on Statistics and on Home, and the activity squares, all count a day with any study. */
class StatsServiceStreakTest {
    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 11, 20)
    private val clock = object : Clock { override fun now(): Instant = today.atStartOfDayIn(zone) + 15.hours }
    private fun daysAgo(n: Int) = today.minus(n, DateTimeUnit.DAY)

    private fun service(read: Map<LocalDate, Int>, activity: LanguageActivity): StatsService {
        val words = object : WordsReadRepository {
            override suspend fun add(entry: WordsReadEntry) = Unit
            override suspend fun dailyCounts() = read.map { (date, count) -> DailyWordCount("Portuguese", date, count) }
        }
        return StatsService(words, LearningStatsRepository { activity }, clock, zone)
    }

    @Test
    fun aDayOfFlashcardsOnlyKeepsTheStreak() = runTest {
        // Read three days ago, only reviewed the two days after, and studied nothing today yet.
        val stats = service(
            read = mapOf(daysAgo(3) to 400, daysAgo(10) to 100),
            activity = LanguageActivity(reviews = mapOf(daysAgo(2) to 30, daysAgo(1) to 12)),
        )
        val overview = stats.overview(1, "Portuguese")
        val activity = stats.studyActivity(1, "Portuguese")
        assertEquals(3, overview.streak)
        assertEquals(3, overview.longestStreak)
        assertEquals(activity.streak, overview.streak)
        assertEquals(activity.days.keys, overview.study.days.keys)
        // Reading days alone are still counted as such.
        assertEquals(2, overview.daysRead)
    }

    @Test
    fun aDayWithOnlyAWordSavedCountsAndAnEmptyOneDoesNot() = runTest {
        val stats = service(
            read = mapOf(daysAgo(1) to 0),
            activity = LanguageActivity(termsSaved = mapOf(today to 1), reviews = mapOf(daysAgo(1) to 0)),
        )
        val overview = stats.overview(1, "Portuguese")
        assertEquals(1, overview.streak)
        assertEquals(setOf(today), overview.study.days.keys)
    }
}
