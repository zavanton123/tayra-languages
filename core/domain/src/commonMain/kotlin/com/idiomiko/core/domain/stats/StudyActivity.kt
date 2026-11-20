package com.idiomiko.core.domain.stats

import kotlinx.datetime.LocalDate
import kotlin.math.ceil
import kotlin.time.Duration

/** What the reader did in a language on one day, and the time its answers took. */
data class StudyDay(val wordsRead: Int = 0, val reviews: Int = 0, val wordsSaved: Int = 0, val reviewTime: Duration = Duration.ZERO) {
    /** A day with no reading, no answers and no words saved is no study day. */
    val studied: Boolean get() = wordsRead > 0 || reviews > 0 || wordsSaved > 0

    /** The minutes of study the day's reading and answers stand for, as [StudyTime] reckons them. */
    val minutes: Int get() = StudyTime.minutes(wordsRead, reviewTime)
}

/**
 * The days the reader studied a language, whether by reading, answering flashcards or saving words to
 * learn: the one rule for a study day, which the streaks and the activity squares both count.
 */
data class StudyActivity(
    val days: Map<LocalDate, StudyDay>,
    val today: LocalDate,
    /** How long a flashcard answer takes on average; null before any was timed. */
    val averageAnswer: Duration? = null,
) {
    val streak: Int get() = ReadingStats.streak(days.keys, today)
    val longestStreak: Int get() = ReadingStats.longestStreak(days.keys)
    val studiedToday: Boolean get() = today in days

    /** Words saved to learn today. */
    val savedToday: Int get() = days[today]?.wordsSaved ?: 0

    /** Today's study in minutes, toward the daily goal. */
    val minutesToday: Int get() = days[today]?.minutes ?: 0

    /**
     * How busy each day studied from [from] to [to] was, from 1 to 4. Words read, answers and
     * words saved are not alike, so each is measured against its own busiest day in the span and
     * a day takes its busiest kind.
     */
    fun levels(from: LocalDate, to: LocalDate = today): Map<LocalDate, Int> {
        val span = days.filterKeys { it in from..to }
        val read = span.values.maxOfOrNull { it.wordsRead } ?: 0
        val reviews = span.values.maxOfOrNull { it.reviews } ?: 0
        val saved = span.values.maxOfOrNull { it.wordsSaved } ?: 0
        return span.mapValues { (_, day) ->
            maxOf(heatLevel(day.wordsRead, read), heatLevel(day.reviews, reviews), heatLevel(day.wordsSaved, saved))
        }
    }

    companion object {
        /** The days with any study, from the words read, the answers and the words saved by day, and the answers' time. */
        fun of(
            wordsRead: Map<LocalDate, Int>,
            reviews: Map<LocalDate, Int>,
            wordsSaved: Map<LocalDate, Int>,
            today: LocalDate,
            reviewTime: Map<LocalDate, Duration> = emptyMap(),
            averageAnswer: Duration? = null,
        ): StudyActivity {
            val dates = (wordsRead.keys + reviews.keys + wordsSaved.keys)
            val days = dates.associateWith { StudyDay(wordsRead[it] ?: 0, reviews[it] ?: 0, wordsSaved[it] ?: 0, reviewTime[it] ?: Duration.ZERO) }
                .filterValues { it.studied }
            return StudyActivity(days, today, averageAnswer)
        }
    }
}

/** 1 to 4: which quarter of [max] [value] reaches; 0 for nothing. */
fun heatLevel(value: Int, max: Int): Int = if (value <= 0 || max <= 0) 0 else ceil(4.0 * value / max).toInt().coerceIn(1, 4)
