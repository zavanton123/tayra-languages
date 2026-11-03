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

    private val today = scheduler.day(noon, config)
    private val balancing = SchedulerConfig(loadBalancing = true)

    /** A review card [interval] days old, answered again on its day with Good. */
    private fun review(interval: Int, stability: Double = interval.toDouble()) =
        CardSchedule(CardState.REVIEW, noon, MemoryState(stability, 5.0), interval, 0, 5, 0, noon - interval.days)

    @Test
    fun loadBalancingWeighsTheDaysAsAnkiDoes() {
        // Days 10 and 11 hold one review and two: Anki's weights are (1/1)^2.15 (1/10)^3 and (1/2)^2.15 (1/11)^3.
        val loaded = balancing.copy(load = mapOf(today + 10 to 1, today + 11 to 2))
        val first = 1.0 / 1000
        val second = Math.pow(0.5, 2.15) / 1331
        val split = first / (first + second)
        assertEquals(10, scheduler.balanced(10..11, split - 1e-6, today, loaded))
        assertEquals(11, scheduler.balanced(10..11, split + 1e-6, today, loaded))
        // A day with nothing due outweighs any with reviews.
        val busy = balancing.copy(load = (8..12).associate { today + it to 5 } - (today + 11))
        assertEquals(11, scheduler.balanced(8..12, 0.5, today, busy))
    }

    @Test
    fun theDayShownIsTheDayGiven() {
        val loaded = balancing.copy(load = (1..60).associate { today + it to it % 7 })
        val card = review(20, stability = 25.0)
        for (fuzz in listOf(0.0, 0.3, 0.7, 0.99)) {
            val shown = scheduler.outcomes(card, noon, loaded, fuzz)
            assertEquals(shown.getValue(Rating.GOOD), scheduler.answer(card, Rating.GOOD, noon, loaded, fuzz))
        }
    }

    @Test
    fun withoutBalancingOrFarOutTheFuzzAloneDecides() {
        val card = review(20, stability = 25.0)
        val loaded = balancing.copy(load = (1..60).associate { today + it to if (it % 2 == 0) 50 else 0 })
        assertEquals(scheduler.outcomes(card, noon, config, 0.4), scheduler.outcomes(card, noon, loaded.copy(loadBalancing = false), 0.4))
        val far = review(200, stability = 300.0)
        assertEquals(scheduler.outcomes(far, noon, config, 0.4), scheduler.outcomes(far, noon, loaded, 0.4), "past 90 days nothing is balanced")
    }

    @Test
    fun easyDaysKeepTheirWeekdaysLight() {
        // Today, 5 October 2026, is a Monday: days 5 to 7 on are Saturday, Sunday and Monday.
        val weekend = balancing.copy(easyDays = List(5) { EasyDay.NORMAL } + listOf(EasyDay.MINIMUM, EasyDay.MINIMUM))
        // A day kept light weighs a ten-thousandth of another, so only the barest draw lands on it.
        for (fuzz in listOf(0.01, 0.5, 0.99)) assertEquals(7, scheduler.balanced(5..7, fuzz, today, weekend), "nothing on the weekend, though it is empty")
        // A reduced Saturday takes reviews only while it holds less than half a day's share.
        val reduced = balancing.copy(easyDays = List(5) { EasyDay.NORMAL } + listOf(EasyDay.REDUCED, EasyDay.NORMAL))
        val light = reduced.copy(load = mapOf(today + 5 to 1, today + 6 to 10, today + 7 to 10))
        assertEquals(5, scheduler.balanced(5..7, 0.0, today, light))
        val heavy = reduced.copy(load = mapOf(today + 5 to 8, today + 6 to 10, today + 7 to 10))
        for (fuzz in listOf(0.01, 0.5, 0.99)) assertTrue(scheduler.balanced(5..7, fuzz, today, heavy) != 5)
        assertEquals("NNNNNRM", EasyDay.format(EasyDay.parse("NNNNNRM")))
    }

    @Test
    fun aCappedReviewComesOnTheLightestOfItsLastDays() {
        val card = review(30, stability = 40.0)
        val loaded = balancing.copy(load = mapOf(today + 5 to 9, today + 6 to 0, today + 7 to 9))
        val capped = scheduler.within(card.copy(intervalDays = 30), 7, noon, loaded, fuzz = 0.9)
        assertEquals(6, capped.intervalDays)
        assertEquals(scheduler.dayStart(today + 6, config), capped.due)
        assertEquals(7, scheduler.within(card.copy(intervalDays = 30), 7, noon, config, fuzz = 0.9).intervalDays, "without balancing, the cap itself")
    }

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
