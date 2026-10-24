package com.tayra.languages.core.domain.practice

import com.tayra.languages.core.domain.practice.WordProgress.Answer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** A word climbs from recognising to producing by runs of right answers, and slides back by wrong ones. */
class WordProgressTest {
    private val now = Instant.fromEpochMilliseconds(1)

    private fun WordProgress.answers(vararg answers: Answer) = answers.fold(this) { p, a -> p.after(a, now) }

    @Test
    fun runsOfAnswersMoveTheLevel() {
        val start = WordProgress(1)
        assertEquals(1, start.answers(Answer.RIGHT, Answer.RIGHT).level, "two right in a row at level 0")
        assertEquals(0, start.answers(Answer.RIGHT, Answer.WRONG, Answer.RIGHT).level, "the run was broken")
        assertEquals(0, start.answers(Answer.WRONG, Answer.WRONG, Answer.WRONG).level, "nothing below recognising")
        val one = WordProgress(1, level = 1)
        assertEquals(0, one.answers(Answer.WRONG, Answer.WRONG).level, "two wrong at level 1")
        assertEquals(2, one.answers(Answer.RIGHT, Answer.RIGHT).level)
        val two = WordProgress(1, level = 2)
        assertEquals(1, two.answers(Answer.WRONG).level, "one wrong at level 2")
        assertEquals(3, two.answers(Answer.RIGHT, Answer.RIGHT, Answer.RIGHT).level)
        assertEquals(3, WordProgress(1, level = 3).answers(Answer.RIGHT, Answer.RIGHT, Answer.RIGHT, Answer.RIGHT).level, "the top stays")
        assertEquals(2, WordProgress(1, level = 3).answers(Answer.WRONG).level)
    }

    @Test
    fun aSlipCountsAsRightButMovesNothingUp() {
        val slipped = WordProgress(1).answers(Answer.RIGHT, Answer.SLIP)
        assertEquals(0, slipped.level)
        assertEquals(2, slipped.streak)
        assertEquals(1, slipped.answers(Answer.RIGHT).level, "the next right answer moves it")
        assertEquals(2, slipped.asked)
        assertEquals(2, slipped.right)
        assertEquals(now, slipped.lastPractised)
    }

    /** The words chosen by their progress: never practised, the weakest half, or not practised for a week. */
    @Test
    fun wordsAreChosenByTheirProgress() {
        val words = (1L..6L).map { PracticeWord("w$it", null, "w$it.", 0..1, it) }
        val day = 24 * 60 * 60 * 1000L
        val today = Instant.fromEpochMilliseconds(30 * day)
        val progress = mapOf(
            2L to WordProgress(2, level = 3, asked = 9, right = 9, lastPractised = Instant.fromEpochMilliseconds(29 * day)),
            3L to WordProgress(3, level = 1, asked = 4, right = 1, lastPractised = Instant.fromEpochMilliseconds(10 * day)),
            4L to WordProgress(4, level = 1, asked = 4, right = 3, lastPractised = Instant.fromEpochMilliseconds(29 * day)),
            5L to WordProgress(5, level = 2, asked = 5, right = 5, lastPractised = Instant.fromEpochMilliseconds(29 * day)),
            6L to WordProgress(6, level = 0, asked = 2, right = 0, lastPractised = Instant.fromEpochMilliseconds(29 * day)),
        )
        assertEquals(words, WordChoice.ALL.of(words, progress, today))
        assertEquals(listOf("w1"), WordChoice.NEW.of(words, progress, today).map { it.text })
        assertEquals(listOf("w1", "w3"), WordChoice.STALE.of(words, progress, today).map { it.text })
        assertEquals(listOf("w1", "w6", "w3", "w4", "w5"), WordChoice.WEAKEST.of(words, progress, today).map { it.text }, "the lowest levels, the least often right first; five at least")
    }
}
