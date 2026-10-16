package com.tayra.languages.core.domain.stats

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StudyActivityTest {
    private val today = LocalDate(2026, 10, 16)
    private fun daysAgo(n: Int) = today.minus(n, DateTimeUnit.DAY)

    @Test
    fun everyKindOfStudyKeepsTheStreak() {
        // Read two days ago, only answered flashcards yesterday, only saved a word today.
        val activity = StudyActivity.of(
            wordsRead = mapOf(daysAgo(2) to 300, daysAgo(5) to 100),
            reviews = mapOf(daysAgo(1) to 20),
            wordsSaved = mapOf(today to 1),
            today = today,
        )
        assertEquals(3, activity.streak)
        assertEquals(3, activity.longestStreak)
        assertTrue(activity.studiedToday)
        assertEquals(1, activity.savedToday)
        assertEquals(StudyDay(reviews = 20), activity.days[daysAgo(1)])
    }

    @Test
    fun daysWithNothingDoNotCount() {
        val activity = StudyActivity.of(wordsRead = mapOf(today to 0), reviews = emptyMap(), wordsSaved = emptyMap(), today = today)
        assertFalse(activity.studiedToday)
        assertEquals(0, activity.streak)
        assertEquals(emptyMap(), activity.levels(daysAgo(7)))
    }

    @Test
    fun aDayIsAsBusyAsItsBusiestKindOfStudy() {
        val activity = StudyActivity.of(
            wordsRead = mapOf(daysAgo(1) to 1000, daysAgo(2) to 100, daysAgo(30) to 5000),
            reviews = mapOf(daysAgo(2) to 40, today to 10),
            wordsSaved = mapOf(today to 2),
            today = today,
        )
        // Only the last week counts, so the busy day a month ago does not make the others look quiet.
        val levels = activity.levels(daysAgo(6))
        assertEquals(mapOf(daysAgo(1) to 4, daysAgo(2) to 4, today to 4), levels)
        assertEquals(mapOf(daysAgo(30) to 4, daysAgo(1) to 1, daysAgo(2) to 4, today to 4), activity.levels(daysAgo(40)))
    }

    @Test
    fun heatLevelsAreQuartersOfTheBusiestDay() {
        assertEquals(listOf(0, 1, 1, 2, 3, 4, 4), listOf(0, 1, 25, 26, 51, 76, 100).map { heatLevel(it, 100) })
        assertEquals(0, heatLevel(5, 0))
    }
}
