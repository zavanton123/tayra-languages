package com.tayra.languages.core.domain.flashcards

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Where a card is in its life, as in Anki. */
enum class CardState(val value: Int) {
    /** Never answered. */
    NEW(0),

    /** Being learned through the learning steps, minutes apart. */
    LEARNING(1),

    /** Learned; reviewed at growing intervals of days. */
    REVIEW(2),

    /** Forgotten in a review; back on the relearning steps. */
    RELEARNING(3);

    val isReview: Boolean get() = this == REVIEW

    companion object {
        fun of(value: Int): CardState = entries.firstOrNull { it.value == value } ?: NEW
    }
}

/** A card's schedule: its state, when it is due and what the memory model knows about it. */
data class CardSchedule(
    val state: CardState = CardState.NEW,
    val due: Instant,
    val memory: MemoryState? = null,
    /** The interval in days of a review card; a relearning card keeps the one it returns to. */
    val intervalDays: Int = 0,
    /** Learning or relearning steps still ahead, the current one included. */
    val remainingSteps: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReview: Instant? = null,
)

data class SchedulerConfig(
    /** The chance of recall reviews are timed for. */
    val desiredRetention: Double = 0.9,
    val learnSteps: List<Duration> = listOf(1.minutes, 10.minutes),
    val relearnSteps: List<Duration> = listOf(10.minutes),
    val maximumIntervalDays: Int = 36500,
    /** The hour a new study day starts, so late-night reviews count for the day before. */
    val dayStartHour: Int = 4,
)

/**
 * Anki's card states around the FSRS memory model: new cards go through the learning steps,
 * graduate to reviews days apart, and a forgotten review goes through the relearning steps.
 * Every answer updates the memory state; intervals in days come from it.
 */
class FlashcardScheduler(private val fsrs: Fsrs = Fsrs(), private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }) {

    /** The study day [instant] falls in, as a day number. */
    fun day(instant: Instant, config: SchedulerConfig): Long =
        (instant - config.dayStartHour.hours).toLocalDateTime(timeZone()).date.toEpochDays().toLong()

    /** When study day [day] starts. */
    fun dayStart(day: Long, config: SchedulerConfig): Instant =
        LocalDate.fromEpochDays(day.toInt()).atTime(LocalTime(config.dayStartHour, 0)).toInstant(timeZone())

    /** The card after answering [rating] at [now]. */
    fun answer(card: CardSchedule, rating: Rating, now: Instant, config: SchedulerConfig, fuzz: Double? = null): CardSchedule =
        outcomes(card, now, config, fuzz).getValue(rating)

    /**
     * What each answer would make of the card at [now]. [fuzz], from 0 up to 1, spreads review
     * intervals of a few days or more over neighbouring days, so cards learned together do not
     * keep coming back together; null leaves intervals exact.
     */
    fun outcomes(card: CardSchedule, now: Instant, config: SchedulerConfig, fuzz: Double? = null): Map<Rating, CardSchedule> {
        val today = day(now, config)
        val elapsed = card.lastReview?.let { (today - day(it, config)).coerceAtLeast(0).toInt() } ?: 0
        val memory = Rating.entries.associateWith { fsrs.next(card.memory, elapsed, it) }
        val interval = memory.mapValues { fsrs.interval(it.value.stability, config.desiredRetention) }
        val maximum = config.maximumIntervalDays

        fun answered(rating: Rating) = card.copy(memory = memory.getValue(rating), reps = card.reps + 1, lastReview = now)

        fun review(rating: Rating, days: Int) =
            answered(rating).copy(state = CardState.REVIEW, intervalDays = days, remainingSteps = 0, due = dayStart(today + days, config))

        fun fuzzed(days: Double, minimum: Int) = withFuzz(fuzz, days, minimum, maximum)

        if (card.state.isReview) {
            val previous = card.intervalDays
            val hard = fuzzed(interval.getValue(Rating.HARD), max(minimumAfter(interval.getValue(Rating.HARD), previous, maximum), 1))
            val good = fuzzed(interval.getValue(Rating.GOOD), max(minimumAfter(interval.getValue(Rating.GOOD), previous, maximum), hard + 1))
            val easy = fuzzed(interval.getValue(Rating.EASY), max(minimumAfter(interval.getValue(Rating.EASY), previous, maximum), good + 1))
            // A forgotten card relearns first; the interval it returns to is fuzzed when it leaves relearning.
            val lapsedDays = interval.getValue(Rating.AGAIN)
            val returning = lapsedDays.roundToInt().coerceIn(1, maximum)
            val lapsed = answered(Rating.AGAIN).copy(lapses = card.lapses + 1, intervalDays = returning)
            val again = when {
                config.relearnSteps.isNotEmpty() -> lapsed.copy(state = CardState.RELEARNING, remainingSteps = config.relearnSteps.size, due = now + config.relearnSteps.first())
                lapsedDays < 0.5 -> lapsed.copy(state = CardState.RELEARNING, remainingSteps = 0, due = now + (lapsedDays * 86_400).toInt().seconds)
                else -> lapsed.copy(state = CardState.REVIEW, remainingSteps = 0, due = dayStart(today + returning, config))
            }
            return mapOf(Rating.AGAIN to again, Rating.HARD to review(Rating.HARD, hard), Rating.GOOD to review(Rating.GOOD, good), Rating.EASY to review(Rating.EASY, easy))
        }

        val relearning = card.state == CardState.RELEARNING
        val steps = if (relearning) config.relearnSteps else config.learnSteps
        val stepState = if (relearning) CardState.RELEARNING else CardState.LEARNING
        val remaining = if (card.state == CardState.NEW) steps.size else card.remainingSteps
        val index = (steps.size - remaining).coerceIn(0, max(steps.size - 1, 0))

        fun step(rating: Rating, remainingSteps: Int, delay: Duration) =
            answered(rating).copy(state = stepState, remainingSteps = remainingSteps, due = now + delay)

        /** Out of steps: a review days away, or, with no steps set, another look later today when the model says so. */
        fun graduate(rating: Rating): CardSchedule {
            val days = interval.getValue(rating)
            return if (steps.isEmpty() && days < 0.5) step(rating, remaining, (days * 86_400).toInt().seconds)
            else review(rating, fuzzed(max(days.roundToInt(), 1).toDouble(), 1))
        }

        val again = steps.firstOrNull()?.let { step(Rating.AGAIN, steps.size, it) } ?: graduate(Rating.AGAIN)
        val hard = hardDelay(steps, index)?.let { step(Rating.HARD, remaining, it) } ?: graduate(Rating.HARD)
        val good = steps.getOrNull(index + 1)?.let { step(Rating.GOOD, steps.size - (index + 1), it) } ?: graduate(Rating.GOOD)
        val goodDays = fuzzed(interval.getValue(Rating.GOOD), 1)
        val easy = review(Rating.EASY, withFuzz(fuzz, max(interval.getValue(Rating.EASY).roundToInt(), 1).toDouble(), goodDays + 1, maximum))
        return mapOf(Rating.AGAIN to again, Rating.HARD to hard, Rating.GOOD to good, Rating.EASY to easy)
    }

    /** Hard repeats the current step; on the first step it waits half-way to the second. */
    private fun hardDelay(steps: List<Duration>, index: Int): Duration? {
        val current = steps.getOrNull(index) ?: return null
        if (index > 0) return current
        val next = steps.getOrNull(1)
        return if (next != null) (current + next) / 2 else minOf(current * 1.5, current + 1.days)
    }

    private companion object {
        /** From an interval of days up, and the share of the days in that range an interval may move by. */
        val FUZZ_RANGES = listOf(Triple(2.5, 7.0, 0.15), Triple(7.0, 20.0, 0.1), Triple(20.0, Double.MAX_VALUE, 0.05))

        fun fuzzDelta(days: Double): Double =
            if (days < 2.5) 0.0 else FUZZ_RANGES.fold(1.0) { delta, (start, end, share) -> delta + share * max(min(days, end) - start, 0.0) }

        fun fuzzBounds(days: Double, minimum: Int, maximum: Int): Pair<Int, Int> {
            val lowest = min(minimum, maximum)
            val clamped = days.coerceIn(lowest.toDouble(), maximum.toDouble())
            val delta = fuzzDelta(clamped)
            val lower = (clamped - delta).roundToInt().coerceIn(lowest, maximum)
            var upper = (clamped + delta).roundToInt().coerceIn(lowest, maximum)
            if (upper == lower && upper > 2 && upper < maximum) upper = lower + 1
            return lower to upper
        }

        fun withFuzz(fuzz: Double?, days: Double, minimum: Int, maximum: Int): Int {
            if (fuzz == null) return days.roundToInt().coerceIn(min(minimum, maximum), maximum)
            val (lower, upper) = fuzzBounds(days, minimum, maximum)
            return floor(lower + fuzz * (1 + upper - lower)).toInt().coerceIn(lower, upper)
        }

        /** The least a passed review may be given: longer than before when the model says longer, never shorter than fuzz could make it. */
        fun minimumAfter(days: Double, previous: Int, maximum: Int): Int {
            val (_, upper) = fuzzBounds(days, 1, maximum)
            return when {
                days.roundToInt() > previous -> previous + 1
                previous <= upper -> previous
                else -> 0
            }
        }
    }
}
