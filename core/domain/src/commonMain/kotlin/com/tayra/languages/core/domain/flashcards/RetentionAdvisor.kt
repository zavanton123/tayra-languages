package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.repository.FlashcardRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/**
 * Helps choose the desired retention, as Anki's simulator does: a language's cards are simulated
 * ahead at every retention from 70% to 99%, answered as the learner's own answers show, and each
 * retention's daily minutes set against the words remembered at the end.
 */
class RetentionAdvisor(
    private val cards: FlashcardRepository,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val scheduler = FlashcardScheduler(timeZone = timeZone)

    /** One retention's outcome: minutes and reviews a day on average, and the words remembered at the end. */
    data class Point(val retention: Int, val minutesPerDay: Double, val reviewsPerDay: Double, val remembered: Double)

    /**
     * The outcome of every retention over [days]; [suggested] remembers the most words for the
     * minutes spent, kept within 70% and 95% as Anki keeps it. [timedAnswers] counts the answers
     * whose time is known, which the minutes are worked out from.
     */
    data class Advice(val points: List<Point>, val suggested: Int, val current: Int, val days: Int, val timedAnswers: Int, val cards: Int)

    suspend fun advise(languageId: Long, days: Int): Advice {
        val now = clock.now()
        cards.reconcile(now)
        val s = settings.current
        val config = FlashcardService.schedulerConfig(s)
        val today = scheduler.day(now, config)
        val w = cards.parameters(languageId)?.parameters?.toDoubleArray() ?: Fsrs.DEFAULT_PARAMETERS
        val all = cards.list(languageId).filter { !it.suspended }
        val existing = all.mapNotNull { card ->
            val memory = card.schedule.memory ?: return@mapNotNull null
            when (card.schedule.state) {
                CardState.NEW -> null
                CardState.REVIEW -> SimulatedCard(
                    memory.stability,
                    memory.difficulty,
                    lastDay = ((card.schedule.lastReview?.let { scheduler.day(it, config) } ?: today) - today).coerceAtMost(0).toDouble(),
                    due = (scheduler.day(card.schedule.due, config) - today).toDouble(),
                    lapses = card.schedule.lapses,
                )
                // A card on its steps comes up again today.
                else -> SimulatedCard(memory.stability, memory.difficulty, 0.0, 0.0, card.schedule.lapses)
            }
        }
        // The new cards waiting, and as many more as the last month brought, for the days ahead.
        val waiting = all.count { it.schedule.state == CardState.NEW }
        val lately = all.count { card -> card.createdAt?.let { now - it < RECENT.days } == true }
        val newCards = waiting + (lately.toDouble() / RECENT * days).roundToInt()
        val reviews = cards.reviewsOf(languageId)
        val simulator = settingsFrom(reviews, config).copy(
            learningStepCount = config.learnSteps.size,
            relearningStepCount = config.relearnSteps.size,
            newPerDay = s.flashcardNewPerDay,
            reviewsPerDay = s.flashcardReviewsPerDay,
            maximumInterval = config.maximumIntervalDays,
        )
        val points = withContext(Dispatchers.Default) {
            (MIN_RETENTION..MAX_RETENTION).map { retention ->
                // A few runs each, as one run's chances make the curves jagged.
                val runs = (1..RUNS).map { seed -> FsrsSimulator.simulate(w, simulator, retention / 100.0, days, existing, newCards, seed) }
                Point(
                    retention,
                    runs.sumOf { it.seconds.sum() } / RUNS / 60 / days,
                    runs.sumOf { it.reviews.sum() + it.learned.sum() }.toDouble() / RUNS / days,
                    runs.sumOf { it.remembered.last() } / RUNS,
                )
            }
        }
        val suggested = points.filter { it.retention in MIN_RETENTION..MAX_SUGGESTED && it.minutesPerDay > 0 }
            .maxByOrNull { it.remembered / it.minutesPerDay }?.retention ?: s.flashcardRetention
        val timed = reviews.count { it.answerTime?.let { t -> t > 0.seconds } == true }
        return Advice(points, suggested, s.flashcardRetention, days, timed, existing.size + waiting)
    }

    /**
     * How the learner answers, read from [reviews] as fsrs-rs reads Anki's review log: the first
     * answer of each day to each card tells the first rating of a new card and the rating of a
     * remembered review; the day's answers after it, how the steps go; the timed answers, the
     * seconds each takes. Each is drawn towards Anki's defaults while it rests on few answers.
     * Unlike fsrs-rs, answers kept before their time was are read for all but the seconds.
     */
    fun settingsFrom(reviews: List<FlashcardReview>, config: SchedulerConfig): SimulatorSettings {
        val defaults = SimulatorSettings()
        if (reviews.isEmpty()) return defaults
        fun state(review: FlashcardReview) = when (review.stateBefore) {
            CardState.NEW, CardState.LEARNING -> 0
            CardState.REVIEW -> 1
            CardState.RELEARNING -> 2
        }
        val costs = defaults.costs.mapIndexed { state, row ->
            row.mapIndexed { rating, default ->
                val times = reviews.filter { state(it) == state && it.rating.value == rating + 1 }
                    .mapNotNull { it.answerTime?.takeIf { t -> t > 0.seconds }?.inWholeMilliseconds?.div(1000.0) }
                towards(times.average().takeIf { times.isNotEmpty() } ?: 0.0, default, times.size)
            }
        }
        // Each card's answers of each study day, in order.
        val days = reviews.groupBy { it.termId to scheduler.day(it.at, config) }.values.map { day -> day.sortedWith(compareBy({ it.at }, { it.id })) }
        val learnFirst = IntArray(4)
        val reviewFirst = IntArray(4)
        val learning = mutableListOf<List<Int>>()
        val relearning = mutableListOf<List<Int>>()
        for (day in days) {
            val first = day.first()
            val ratings = day.map { it.rating.value }
            when (state(first)) {
                0 -> {
                    learnFirst[first.rating.value - 1]++
                    learning += ratings
                }
                1 -> {
                    reviewFirst[first.rating.value - 1]++
                    if (first.rating == Rating.AGAIN) relearning += ratings
                }
            }
        }
        val learnCounts = if (learnFirst.all { it == 0 }) IntArray(4) { 1 } else learnFirst
        val reviewCounts = if (reviewFirst.drop(1).all { it == 0 }) intArrayOf(reviewFirst[0], 1, 1, 1) else reviewFirst
        val firstRating = learnCounts.map { it.toDouble() / learnCounts.sum() }
            .zip(defaults.firstRating) { mine, default -> towards(mine, default, learnCounts.sum()) }
        val reviewRating = reviewCounts.drop(1).map { it.toDouble() / reviewCounts.drop(1).sum() }
            .zip(defaults.reviewRating) { mine, default -> towards(mine, default, reviewCounts.drop(1).sum()) }
        return defaults.copy(
            firstRating = firstRating,
            reviewRating = reviewRating,
            learningSteps = transitions(learning, defaults.learningSteps),
            relearningSteps = transitions(relearning, defaults.relearningSteps),
            costs = costs,
        )
    }

    /** How the steps go from each of Again, Hard and Good, counted over [sequences] with one of each added, drawn towards [defaults]. */
    private fun transitions(sequences: List<List<Int>>, defaults: List<List<Double>>): List<List<Double>> {
        val counts = Array(4) { DoubleArray(4) { 1.0 } }
        for (sequence in sequences) for (i in 0 until sequence.size - 1) counts[sequence[i] - 1][sequence[i + 1] - 1] += 1.0
        return (0 until 3).map { from ->
            val total = counts[from].sum()
            counts[from].map { it / total }.zip(defaults[from]) { mine, default -> towards(mine, default, total) }
        }
    }

    /** [mine] as far as [count] answers bear it out, the rest [default]: fsrs-rs's weight of n / (50 + n). */
    private fun towards(mine: Double, default: Double, count: Number): Double {
        val n = count.toDouble()
        val weight = n / (50.0 + n)
        return weight * mine + (1 - weight) * default
    }

    companion object {
        const val MIN_RETENTION = 70
        const val MAX_RETENTION = 99
        private const val MAX_SUGGESTED = 95

        /** The runs each retention's figures are the average of. */
        private const val RUNS = 3

        /** The days whose new cards set how many more are taken to come. */
        private const val RECENT = 30
    }
}
