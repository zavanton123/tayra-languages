package com.idiomiko.core.domain.stats

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class StudyTimeTest {
    private val today = LocalDate(2026, 11, 20)

    @Test
    fun wordsReadAndAnswersAddUpToMinutes() {
        // 600 words at the learner's pace are ten minutes, and the answers took two and a half more.
        assertEquals(12, StudyTime.minutes(600, 150.seconds))
        assertEquals(0, StudyTime.minutes(30, 20.seconds))
        assertEquals(0, StudyTime.minutes(-5, (-1).minutes))
    }

    @Test
    fun aTextAndTheCardsDueAreGivenTheMinutesTheyWillTake() {
        // A lesson of 443 words takes about eight minutes, at the same pace as the words read are counted.
        assertEquals(8, StudyTime.readingMinutes(443))
        assertEquals(1, StudyTime.readingMinutes(0))
        // Cards take as long as the reader's answers do, or a usual answer's time before any was timed.
        assertEquals(31, StudyTime.reviewMinutes(310, null))
        assertEquals(26, StudyTime.reviewMinutes(310, 5.seconds))
        assertEquals(1, StudyTime.reviewMinutes(1, 5.seconds))
    }

    @Test
    fun todaysMinutesComeFromTodaysReadingAndAnswers() {
        val activity = StudyActivity.of(
            wordsRead = mapOf(today to 300, today.minus(1, DateTimeUnit.DAY) to 6000),
            reviews = mapOf(today to 40),
            wordsSaved = emptyMap(),
            today = today,
            reviewTime = mapOf(today to 7.minutes),
        )
        assertEquals(12, activity.minutesToday)
        assertEquals(100, activity.days.getValue(today.minus(1, DateTimeUnit.DAY)).minutes)
    }

    @Test
    fun theDailyGoalIsReachedOnceTheMinutesAreDone() {
        val goal = DailyGoal(goal = 20, done = 12)
        assertFalse(goal.reached)
        assertEquals(0.6f, goal.fraction)
        assertEquals(8, goal.left)
        assertTrue(DailyGoal(20, 25).reached)
        assertEquals(1f, DailyGoal(20, 25).fraction)
        assertEquals(0, DailyGoal(20, 25).left)
    }
}
