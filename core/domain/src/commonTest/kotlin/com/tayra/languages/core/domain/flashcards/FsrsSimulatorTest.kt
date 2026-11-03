package com.tayra.languages.core.domain.flashcards

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FsrsSimulatorTest {
    private val w = Fsrs.DEFAULT_PARAMETERS
    private val settings = SimulatorSettings(newPerDay = 10)

    /** Cards learned a while ago, some due, some not yet. */
    private val existing = (0 until 300).map { i ->
        val due = (i % 30 - 10).toDouble()
        SimulatedCard(stability = 2.0 + i % 40, difficulty = 3.0 + i % 6, lastDay = minOf(due - 1 - i % 20, 0.0), due = due)
    }

    @Test
    fun newCardsAreLearnedAtTheDailyLimit() {
        val result = FsrsSimulator.simulate(w, settings, 0.9, 60, emptyList(), newCards = 100)
        assertEquals(List(10) { 10 } + List(50) { 0 }, result.learned.toList())
        assertTrue(result.reviews.sum() > 0)
        assertTrue(result.remembered.last() in 50.0..100.0, "${result.remembered.last()}")
    }

    @Test
    fun aHigherRetentionCostsMoreAndKeepsMore() {
        val low = FsrsSimulator.simulate(w, settings, 0.75, 365, existing, newCards = 500)
        val high = FsrsSimulator.simulate(w, settings, 0.95, 365, existing, newCards = 500)
        assertTrue(high.reviews.sum() > low.reviews.sum() * 1.5, "${low.reviews.sum()} against ${high.reviews.sum()}")
        assertTrue(high.seconds.sum() > low.seconds.sum())
        assertTrue(high.remembered.last() > low.remembered.last())
    }

    @Test
    fun theSameSeedSimulatesTheSame() {
        val a = FsrsSimulator.simulate(w, settings, 0.9, 120, existing, newCards = 200, seed = 7)
        val b = FsrsSimulator.simulate(w, settings, 0.9, 120, existing, newCards = 200, seed = 7)
        assertEquals(a.reviews.toList(), b.reviews.toList())
        assertEquals(a.remembered.toList(), b.remembered.toList())
    }

    @Test
    fun aReviewLimitPutsTheRestOff() {
        val limited = FsrsSimulator.simulate(w, settings.copy(reviewsPerDay = 5), 0.9, 30, existing, newCards = 0)
        assertTrue(limited.reviews.all { it <= 5 })
    }
}
