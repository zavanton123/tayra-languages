package com.tayra.languages.core.domain.flashcards

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class FlashcardSchedulerTest {
    private val scheduler = FlashcardScheduler(timeZone = { TimeZone.UTC })
    private val config = SchedulerConfig()
    private val noon = Instant.parse("2026-10-05T12:00:00Z")
    private val fresh = CardSchedule(due = noon)

    @Test
    fun aNewCardGoesThroughTheLearningSteps() {
        val first = scheduler.outcomes(fresh, noon, config)
        assertEquals(CardState.LEARNING, first.getValue(Rating.AGAIN).state)
        assertEquals(noon + 1.minutes, first.getValue(Rating.AGAIN).due)
        // Hard waits half-way between the first two steps.
        assertEquals(noon + 5.5.minutes, first.getValue(Rating.HARD).due)
        val good = first.getValue(Rating.GOOD)
        assertEquals(CardState.LEARNING, good.state)
        assertEquals(noon + 10.minutes, good.due)
        assertEquals(1, good.remainingSteps)
        assertEquals(1, good.reps)
        assertNotNull(good.memory)

        // Good on the last step graduates the card to a review at least a day away.
        val later = noon + 10.minutes
        val graduated = scheduler.answer(good, Rating.GOOD, later, config)
        assertEquals(CardState.REVIEW, graduated.state)
        assertTrue(graduated.intervalDays >= 1)
        assertEquals(scheduler.dayStart(scheduler.day(later, config) + graduated.intervalDays, config), graduated.due)
    }

    @Test
    fun easySkipsTheStepsAndWaitsLongerThanGood() {
        val outcomes = scheduler.outcomes(fresh, noon, config)
        val easy = outcomes.getValue(Rating.EASY)
        assertEquals(CardState.REVIEW, easy.state)
        // The model's first interval after Easy is its fourth parameter, about eight days.
        assertEquals(8, easy.intervalDays)
        assertEquals(Instant.parse("2026-10-13T04:00:00Z"), easy.due)
    }

    @Test
    fun aPassedReviewGrowsAndAHarderAnswerNeverWaitsLonger() {
        var card = scheduler.answer(fresh, Rating.EASY, noon, config)
        var now = card.due + 8.hours
        repeat(4) {
            val outcomes = scheduler.outcomes(card, now, config)
            val hard = outcomes.getValue(Rating.HARD).intervalDays
            val good = outcomes.getValue(Rating.GOOD).intervalDays
            val easy = outcomes.getValue(Rating.EASY).intervalDays
            assertTrue(hard < good && good < easy, "$hard < $good < $easy")
            assertTrue(good > card.intervalDays, "a remembered card waits longer: $good after ${card.intervalDays}")
            card = outcomes.getValue(Rating.GOOD)
            now = card.due + 8.hours
        }
        assertEquals(0, card.lapses)
        assertEquals(5, card.reps)
    }

    @Test
    fun aForgottenReviewRelearnsAndComesBackSooner() {
        var card = scheduler.answer(fresh, Rating.EASY, noon, config)
        card = scheduler.answer(card, Rating.GOOD, card.due + 8.hours, config)
        val before = card.intervalDays
        val failedAt = card.due + 8.hours
        val lapsed = scheduler.answer(card, Rating.AGAIN, failedAt, config)
        assertEquals(CardState.RELEARNING, lapsed.state)
        assertEquals(1, lapsed.lapses)
        assertEquals(failedAt + 10.minutes, lapsed.due)
        assertTrue(lapsed.intervalDays in 1 until before)

        val back = scheduler.answer(lapsed, Rating.GOOD, failedAt + 10.minutes, config)
        assertEquals(CardState.REVIEW, back.state)
        assertTrue(back.intervalDays in 1 until before, "${back.intervalDays} days after forgetting, $before before")
    }

    @Test
    fun theStudyDayStartsAtFour() {
        val lateNight = Instant.parse("2026-10-06T03:30:00Z")
        assertEquals(scheduler.day(noon, config), scheduler.day(lateNight, config))
        assertEquals(scheduler.day(noon, config) + 1, scheduler.day(Instant.parse("2026-10-06T04:00:00Z"), config))
    }

    @Test
    fun withoutStepsTheModelDecidesFromTheFirstAnswer() {
        val stepless = config.copy(learnSteps = emptyList(), relearnSteps = emptyList())
        val outcomes = scheduler.outcomes(fresh, noon, stepless)
        // Again's first stability is about a fifth of a day: another look later today.
        assertEquals(CardState.LEARNING, outcomes.getValue(Rating.AGAIN).state)
        assertTrue(outcomes.getValue(Rating.AGAIN).due < noon + 1.days)
        assertEquals(CardState.REVIEW, outcomes.getValue(Rating.GOOD).state)
        assertEquals(2, outcomes.getValue(Rating.GOOD).intervalDays)
    }

    @Test
    fun fuzzKeepsIntervalsNearTheModelsAndOrdered() {
        var card = scheduler.answer(fresh, Rating.EASY, noon, config)
        card = scheduler.answer(card, Rating.GOOD, card.due + 8.hours, config)
        val now = card.due + 8.hours
        val exact = scheduler.outcomes(card, now, config).getValue(Rating.GOOD).intervalDays
        for (fuzz in listOf(0.0, 0.3, 0.7, 0.999)) {
            val outcomes = scheduler.outcomes(card, now, config, fuzz)
            val good = outcomes.getValue(Rating.GOOD).intervalDays
            assertTrue(kotlin.math.abs(good - exact) <= exact * 0.2 + 2, "$good is near $exact")
            assertTrue(outcomes.getValue(Rating.HARD).intervalDays < good && good < outcomes.getValue(Rating.EASY).intervalDays)
        }
    }
}
