package com.tayra.languages.core.domain.flashcards

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The expected values are those the fsrs crate (6.6.2), which Anki schedules with, tests itself against. */
class FsrsTest {
    private val fsrs = Fsrs()

    private fun assertClose(expected: Double, actual: Double, tolerance: Double = 1e-3) =
        assertTrue(abs(expected - actual) <= tolerance * maxOf(1.0, abs(expected)), "expected $expected, got $actual")

    @Test
    fun forgettingCurve() {
        val recall = listOf(0.0 to 1.0, 1.0 to 2.0, 2.0 to 3.0, 3.0 to 4.0, 4.0 to 4.0, 5.0 to 2.0).map { (days, stability) -> fsrs.retrievability(days, stability) }
        listOf(1.0, 0.9403443, 0.9253786, 0.9185229, 0.9, 0.8261359).forEachIndexed { i, expected -> assertClose(expected, recall[i]) }
    }

    @Test
    fun intervalForAWantedRetention() {
        val days = (3..10).map { fsrs.interval(1.0, it / 10.0).roundToInt().coerceAtLeast(1) }
        assertEquals(listOf(2508, 387, 90, 27, 9, 3, 1, 1), days)
        assertClose(121.01551, fsrs.interval(121.01552, 0.9))
    }

    @Test
    fun difficultyMovesWithTheAnswerAndDriftsBack() {
        val next = (1..4).map { fsrs.nextDifficulty(5.0, it) }
        listOf(8.354889, 6.6774445, 5.0, 3.3225555).forEachIndexed { i, expected -> assertClose(expected, next[i]) }
        listOf(8.341763, 6.6659956, 4.990228, 3.3144615).forEachIndexed { i, expected -> assertClose(expected, fsrs.meanReversion(next[i])) }
    }

    @Test
    fun stabilityAfterRecallForgettingAndSameDayReviews() {
        val recalled = listOf(0.9, 0.8, 0.7, 0.6).mapIndexed { i, r -> fsrs.stabilityAfterSuccess(5.0, (i + 1).toDouble(), r, i + 1) }
        listOf(25.602541, 28.226582, 58.656002, 127.226685).forEachIndexed { i, expected -> assertClose(expected, recalled[i]) }
        val forgotten = listOf(0.9, 0.8, 0.7, 0.6).mapIndexed { i, r -> fsrs.stabilityAfterFailure(5.0, (i + 1).toDouble(), r) }
        listOf(1.0525396, 1.1894329, 1.3680838, 1.584989).forEachIndexed { i, expected -> assertClose(expected, forgotten[i]) }
        val sameDay = (1..4).map { fsrs.stabilityShortTerm(5.0, it) }
        listOf(1.596818, 5.0, 5.0, 8.12961).forEachIndexed { i, expected -> assertClose(expected, sameDay[i]) }
    }

    @Test
    fun aFirstAnswerStartsFromTheParameters() {
        Rating.entries.forEachIndexed { i, rating ->
            assertClose(Fsrs.DEFAULT_PARAMETERS[i], fsrs.next(null, 0, rating).stability)
        }
        assertClose(Fsrs.DEFAULT_PARAMETERS[4], fsrs.next(null, 0, Rating.AGAIN).difficulty)
    }

    /** Again, then Good five times, 0, 0, 1, 3, 8 and 21 days apart. */
    @Test
    fun aHistoryOfAnswersGivesTheReferenceMemoryState() {
        val ratings = listOf(Rating.AGAIN, Rating.GOOD, Rating.GOOD, Rating.GOOD, Rating.GOOD, Rating.GOOD)
        val gaps = listOf(0, 0, 1, 3, 8, 21)
        var state: MemoryState? = null
        ratings.zip(gaps).forEach { (rating, gap) -> state = fsrs.next(state, gap, rating) }
        assertClose(53.62691, state!!.stability)
        assertClose(6.3574867, state!!.difficulty)
    }
}
