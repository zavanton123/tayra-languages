package com.idiomiko.core.domain.stats

import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Study time reckoned from what the app records: the words of the pages read, at a learner's pace with
 * the lookups, and the time each flashcard answer took. Nothing records how long a page was open, so
 * the same pace turns the words of a text into the minutes it will take and the words read into the
 * minutes they took, and the two agree.
 */
object StudyTime {
    /** Words a learner reads a minute, looking words up as they go. */
    const val READING_WORDS_PER_MINUTE = 60

    /** An answer from before answers were timed. */
    val UNTIMED_ANSWER: Duration = 8.seconds

    /** An answer of a language whose answers were never timed, for the time cards will take. */
    val USUAL_ANSWER: Duration = 6.seconds

    /** The whole minutes [wordsRead] and answers taking [reviewTime] stand for. */
    fun minutes(wordsRead: Int, reviewTime: Duration): Int =
        ((wordsRead.coerceAtLeast(0) * 60L / READING_WORDS_PER_MINUTE + reviewTime.inWholeSeconds.coerceAtLeast(0)) / 60).toInt()

    /** The minutes reading [words] will take, at least one. */
    fun readingMinutes(words: Int): Int = ceil(words.coerceAtLeast(0).toDouble() / READING_WORDS_PER_MINUTE).toInt().coerceAtLeast(1)

    /** The minutes answering [cards] will take at [average] an answer, at least one. */
    fun reviewMinutes(cards: Int, average: Duration?): Int =
        ceil(cards.coerceAtLeast(0) * (average ?: USUAL_ANSWER).inWholeMilliseconds / 60_000.0).toInt().coerceAtLeast(1)
}

/** The minutes of study a day the reader aims for, and how many of them today has had. */
data class DailyGoal(val goal: Int, val done: Int) {
    val reached: Boolean get() = goal > 0 && done >= goal
    /** From 0 to 1. */
    val fraction: Float get() = if (goal <= 0) 0f else (done.toFloat() / goal).coerceIn(0f, 1f)
    val left: Int get() = (goal - done).coerceAtLeast(0)
}
