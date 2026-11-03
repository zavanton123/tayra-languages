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
import kotlin.math.pow
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
    /** The memory model's parameters trained for the card's language; null for the defaults. */
    val parameters: List<Double>? = null,
    /** A review's day within its fuzz is chosen to even out the load, as Anki's load balancer chooses it. */
    val loadBalancing: Boolean = false,
    /** How much each weekday, Monday first, may be given. */
    val easyDays: List<EasyDay> = List(7) { EasyDay.NORMAL },
    /** The reviews already due on each study day ahead, by day number, which load balancing evens out. */
    val load: Map<Long, Int> = emptyMap(),
) {
    /** A review's day is weighed: for the load, or, without balancing, to keep the light weekdays light. */
    val weighsDays: Boolean get() = loadBalancing || easyDays.any { it != EasyDay.NORMAL }
}

/** How much a weekday may be given: as much as any other, about half of that, or as little as can be. */
enum class EasyDay(val share: Double) {
    NORMAL(1.0),
    REDUCED(0.5),
    MINIMUM(0.0001);

    companion object {
        /** The seven days, Monday first, as kept in settings: a letter a day, N, R or M. */
        fun parse(text: String): List<EasyDay> =
            List(7) { i -> when (text.getOrNull(i)) { 'R' -> REDUCED; 'M' -> MINIMUM; else -> NORMAL } }

        fun format(days: List<EasyDay>): String = days.joinToString("") { it.name.take(1) }
    }
}

/**
 * Anki's card states around the FSRS memory model: new cards go through the learning steps,
 * graduate to reviews days apart, and a forgotten review goes through the relearning steps.
 * Every answer updates the memory state; intervals in days come from it.
 */
class FlashcardScheduler(private val fsrs: Fsrs = Fsrs(), private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }) {
    /** The model of the trained parameters last asked for, kept for the next card of the language. */
    private var trained: Pair<List<Double>, Fsrs>? = null

    private fun model(config: SchedulerConfig): Fsrs = config.parameters?.let { parameters ->
        trained?.takeIf { it.first == parameters }?.second ?: Fsrs(parameters.toDoubleArray()).also { trained = parameters to it }
    } ?: fsrs

    /** The study day [instant] falls in, as a day number. */
    fun day(instant: Instant, config: SchedulerConfig): Long =
        (instant - config.dayStartHour.hours).toLocalDateTime(timeZone()).date.toEpochDays().toLong()

    /** When study day [day] starts. */
    fun dayStart(day: Long, config: SchedulerConfig): Instant =
        LocalDate.fromEpochDays(day.toInt()).atTime(LocalTime(config.dayStartHour, 0)).toInstant(timeZone())

    /** The chance, from 0 to 1, of recalling the card at [now]; null for a card never answered. */
    fun retrievability(card: CardSchedule, now: Instant, config: SchedulerConfig): Double? {
        val memory = card.memory ?: return null
        val elapsed = card.lastReview?.let { (day(now, config) - day(it, config)).coerceAtLeast(0) } ?: 0
        return model(config).retrievability(elapsed.toDouble(), memory.stability)
    }

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
        val fsrs = model(config)
        val memory = Rating.entries.associateWith { fsrs.next(card.memory, elapsed, it) }
        val interval = memory.mapValues { fsrs.interval(it.value.stability, config.desiredRetention) }
        val maximum = config.maximumIntervalDays

        fun answered(rating: Rating) = card.copy(memory = memory.getValue(rating), reps = card.reps + 1, lastReview = now)

        fun review(rating: Rating, days: Int) =
            answered(rating).copy(state = CardState.REVIEW, intervalDays = days, remainingSteps = 0, due = dayStart(today + days, config))

        fun fuzzed(days: Double, minimum: Int) = fuzzedDay(fuzz, days, minimum, maximum, today, config)

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
        val easy = review(Rating.EASY, fuzzed(max(interval.getValue(Rating.EASY).roundToInt(), 1).toDouble(), goodDays + 1))
        return mapOf(Rating.AGAIN to again, Rating.HARD to hard, Rating.GOOD to good, Rating.EASY to easy)
    }

    /**
     * [card] coming back at most [days] after [now] when it is a review due later: on the last of
     * those days, or, balancing the load, on whichever of the last [CAP_SPREAD] + 1 is lighter.
     */
    fun within(card: CardSchedule, days: Int, now: Instant, config: SchedulerConfig, fuzz: Double? = null): CardSchedule {
        if (!card.state.isReview || card.intervalDays <= days) return card
        val today = day(now, config)
        val chosen = if (fuzz != null && config.weighsDays) balanced((days - CAP_SPREAD).coerceAtLeast(1)..days, fuzz, today, config) else days
        return card.copy(intervalDays = chosen, due = dayStart(today + chosen, config))
    }

    /** The interval [fuzz] picks for [days] within its fuzz range: by load where it is balanced, else evenly. */
    private fun fuzzedDay(fuzz: Double?, days: Double, minimum: Int, maximum: Int, today: Long, config: SchedulerConfig): Int {
        if (fuzz == null || !config.weighsDays || days > MAX_BALANCED || minimum > MAX_BALANCED) return withFuzz(fuzz, days, minimum, maximum)
        val (lower, upper) = fuzzBounds(days, minimum, maximum)
        return balanced(lower..upper, fuzz, today, config)
    }

    /**
     * One of the intervals in [range], drawn with [fuzz] by the weights of Anki's load balancer: a
     * day with nothing due weighs 1, any other (1 / reviews due)^2.15 × (1 / interval)^3, so the
     * emptiest and nearest days are likeliest; each weighed again by how much its weekday may be given.
     */
    internal fun balanced(range: IntRange, fuzz: Double, today: Long, config: SchedulerConfig): Int {
        if (range.first >= range.last) return range.first
        val counts = range.map { config.load[today + it] ?: 0 }
        val weekdays = range.map { LocalDate.fromEpochDays((today + it).toInt()).dayOfWeek.ordinal }
        val easy = easyDayShares(config.easyDays, weekdays, counts)
        val weights = range.mapIndexed { i, days ->
            // Unlike Anki, an empty day is weighed by its weekday too, so a day kept light stays light while there is little due.
            (if (counts[i] == 0) 1.0 else (1.0 / counts[i]).pow(2.15) * (1.0 / days).pow(3)) * easy[i]
        }
        var left = fuzz.coerceIn(0.0, 1.0) * weights.sum()
        weights.forEachIndexed { i, weight ->
            left -= weight
            if (left < 0) return range.first + i
        }
        return range.last
    }

    /**
     * What each day of a range may be given, as Anki works it out: a reduced day is open only while
     * it holds no more than half a day's share of the reviews due over the range.
     */
    private fun easyDayShares(easyDays: List<EasyDay>, weekdays: List<Int>, counts: List<Int>): List<Double> {
        val total = counts.sum()
        val shares = weekdays.sumOf { easyDays[it].share }
        return weekdays.mapIndexed { i, weekday ->
            when (val day = easyDays[weekday]) {
                EasyDay.REDUCED -> {
                    val otherShares = shares - EasyDay.REDUCED.share
                    val threshold = if (otherShares > 0) (total - counts[i]) / otherShares else Double.MAX_VALUE
                    if (counts[i] / EasyDay.REDUCED.share > threshold) EasyDay.MINIMUM.share else EasyDay.NORMAL.share
                }
                else -> day.share
            }
        }
    }

    /** Hard repeats the current step; on the first step it waits half-way to the second. */
    private fun hardDelay(steps: List<Duration>, index: Int): Duration? {
        val current = steps.getOrNull(index) ?: return null
        if (index > 0) return current
        val next = steps.getOrNull(1)
        return if (next != null) (current + next) / 2 else minOf(current * 1.5, current + 1.days)
    }

    private companion object {
        /** Past this many days a review's day is left to fuzz alone: so far out, an even load matters little. */
        const val MAX_BALANCED = 90

        /** How many days before the cap a capped review may come, for a lighter day. */
        const val CAP_SPREAD = 2

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
