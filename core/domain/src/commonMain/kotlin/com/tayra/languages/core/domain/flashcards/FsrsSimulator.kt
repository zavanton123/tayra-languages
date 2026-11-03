package com.tayra.languages.core.domain.flashcards

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * How a learner answers, as the simulator draws it: the first answer to a new card, the answer to a
 * review remembered (Hard, Good or Easy), how the learning and relearning steps go from each answer,
 * and the seconds each answer takes in each state (learning, review, relearning) for each rating.
 */
data class SimulatorSettings(
    val firstRating: List<Double> = listOf(0.24, 0.094, 0.495, 0.171),
    val reviewRating: List<Double> = listOf(0.224, 0.631, 0.145),
    val learningSteps: List<List<Double>> = listOf(
        listOf(0.3686, 0.0628, 0.5108, 0.0577),
        listOf(0.0442, 0.4553, 0.4457, 0.0549),
        listOf(0.0519, 0.047, 0.8462, 0.055),
    ),
    val relearningSteps: List<List<Double>> = listOf(
        listOf(0.2157, 0.0643, 0.6595, 0.0604),
        listOf(0.05, 0.4638, 0.4475, 0.0387),
        listOf(0.1057, 0.1434, 0.7266, 0.0244),
    ),
    val costs: List<List<Double>> = listOf(
        listOf(19.58, 18.79, 13.78, 10.71),
        listOf(19.38, 17.59, 12.38, 8.94),
        listOf(16.44, 15.25, 12.32, 8.03),
    ),
    val learningStepCount: Int = 2,
    val relearningStepCount: Int = 1,
    val newPerDay: Int = 20,
    /** 0 for no limit. */
    val reviewsPerDay: Int = 0,
    val maximumInterval: Int = 36500,
)

/**
 * A card as the simulator follows it, days counted from today: when it was last answered (null for
 * a card never answered), when it is due, and its memory.
 */
class SimulatedCard(
    var stability: Double,
    var difficulty: Double,
    var lastDay: Double?,
    var due: Double,
    var lapses: Int = 0,
)

/** What a simulation came to, day by day: the cards remembered, the reviews and new cards, the seconds spent. */
class SimulationResult(
    val remembered: DoubleArray,
    val reviews: IntArray,
    val learned: IntArray,
    val seconds: DoubleArray,
)

/**
 * Anki's workload simulator (fsrs-rs 6.6, simulate) in Kotlin: day by day, each card due is
 * remembered with the chance its memory gives, answered as the learner answers, rescheduled for
 * [SimulatorSettings]' desired retention, and the new cards learned at the daily limit.
 */
object FsrsSimulator {
    private const val S_MIN = 0.001
    private const val S_MAX = 36500.0
    private const val D_MIN = 1.0
    private const val D_MAX = 10.0
    private const val MAX_STEPS = 5

    fun simulate(
        w: DoubleArray,
        settings: SimulatorSettings,
        desiredRetention: Double,
        days: Int,
        existing: List<SimulatedCard>,
        newCards: Int,
        seed: Int = 42,
    ): SimulationResult {
        val random = Random(seed)
        val remembered = DoubleArray(days)
        val reviews = IntArray(days)
        val learned = IntArray(days)
        val seconds = DoubleArray(days)
        val decay = -w[20]
        val factor = 0.9.pow(1.0 / decay) - 1.0
        val retentionFactor = desiredRetention.pow(1.0 / decay) - 1.0

        fun recall(card: SimulatedCard, day: Double): Double = (max(day - card.lastDay!!, 0.0) / card.stability * factor + 1.0).pow(decay)

        // Cards by the day they come up; a card answered goes to a later day, never back to one passed.
        val cards = existing.filter { it.stability > 1e-9 }.map { SimulatedCard(it.stability, it.difficulty, it.lastDay, it.due, it.lapses) }.toMutableList()
        if (settings.newPerDay > 0) repeat(newCards) { i -> cards += SimulatedCard(0.0, 0.0, null, (i / settings.newPerDay).toDouble()) }
        val byDay = Array(days) { mutableListOf<Int>() }
        cards.forEachIndexed { i, card -> dayOf(card.due)?.takeIf { it < days }?.let { byDay[it] += i } }

        for (day in 0 until days) {
            // Reviews before new cards, and of reviews due the same day, the easier first, as fsrs-rs takes them.
            val today = byDay[day].sortedWith(compareBy({ cards[it].lastDay == null }, { if (cards[it].lastDay == null) Int.MIN_VALUE else (cards[it].difficulty * 100).toInt() }, { it }))
            for (index in today) {
                val card = cards[index]
                val learn = card.lastDay == null
                val overLimit = if (learn) learned[day] + 1 > settings.newPerDay else settings.reviewsPerDay > 0 && reviews[day] + 1 > settings.reviewsPerDay
                if (overLimit) {
                    card.due = day + 1.0
                    if (day + 1 < days) byDay[day + 1] += index
                    continue
                }
                if (learn) {
                    val rating = pick(settings.firstRating, random) + 1
                    val (s, d, cost) = shortTerm(w, w[rating - 1], initialDifficulty(w, rating).coerceIn(D_MIN, D_MAX), rating, settings.costs[0], settings.learningSteps, settings.learningStepCount, random)
                    card.stability = s
                    card.difficulty = d
                    learned[day]++
                    seconds[day] += cost
                } else {
                    val last = card.lastDay!!
                    val r = recall(card, card.due)
                    val forgot = random.nextDouble() >= r
                    if (forgot) card.lapses++
                    val rating = if (forgot) 1 else pick(settings.reviewRating, random) + 2
                    val (s, d, cost) = if (forgot) {
                        val lapsed = afterFailure(w, card.stability, r, card.difficulty)
                        val (s, d, steps) = shortTerm(w, lapsed, nextDifficulty(w, card.difficulty, rating), null, settings.costs[2], settings.relearningSteps, settings.relearningStepCount, random)
                        Triple(s, d, settings.costs[1][0] + steps)
                    } else {
                        Triple(afterSuccess(w, card.stability, r, card.difficulty, rating), nextDifficulty(w, card.difficulty, rating), settings.costs[1][rating - 1])
                    }
                    reviews[day]++
                    seconds[day] += cost
                    for (i in max(last, 0.0).toInt() until day) remembered[i] += recall(card, i.toDouble())
                    card.stability = s
                    card.difficulty = d
                }
                val interval = (card.stability / factor * retentionFactor).roundToInt().coerceIn(1, settings.maximumInterval)
                card.lastDay = day.toDouble()
                card.due = (day + interval).toDouble()
                if (day + interval < days) byDay[day + interval] += index
            }
        }
        // What each card answered holds until the end, from its last answer.
        for (card in cards) {
            val last = card.lastDay ?: continue
            for (i in max(last, 0.0).toInt() until days) remembered[i] += recall(card, i.toDouble())
        }
        return SimulationResult(remembered, reviews, learned, seconds)
    }

    /** The day a card due at [due] comes up: an overdue card today. */
    private fun dayOf(due: Double): Int? = if (due.isNaN()) null else max(due, 0.0).toInt()

    private fun pick(weights: List<Double>, random: Random): Int {
        var left = random.nextDouble() * weights.sum()
        weights.forEachIndexed { i, weight ->
            left -= weight
            if (left < 0) return i
        }
        return weights.lastIndex
    }

    private fun initialDifficulty(w: DoubleArray, rating: Int) = w[4] - exp(w[5] * (rating - 1)) + 1.0

    private fun nextDifficulty(w: DoubleArray, d: Double, rating: Int): Double {
        val next = d + (10.0 - d) / 9.0 * (-w[6] * (rating - 3.0))
        return (w[7] * initialDifficulty(w, 4) + (1 - w[7]) * next).coerceIn(D_MIN, D_MAX)
    }

    private fun afterSuccess(w: DoubleArray, s: Double, r: Double, d: Double, rating: Int): Double {
        val hard = if (rating == 2) w[15] else 1.0
        val easy = if (rating == 4) w[16] else 1.0
        return (s * (exp(w[8]) * (11.0 - d) * s.pow(-w[9]) * (exp((1.0 - r) * w[10]) - 1.0) * hard * easy + 1.0)).coerceIn(S_MIN, S_MAX)
    }

    private fun afterFailure(w: DoubleArray, s: Double, r: Double, d: Double): Double {
        val floor = s / exp(w[17] * w[18])
        return min(w[11] * d.pow(-w[12]) * ((s + 1.0).pow(w[13]) - 1.0) * exp((1.0 - r) * w[14]), floor).coerceIn(S_MIN, S_MAX)
    }

    private fun shortTermStability(w: DoubleArray, s: Double, rating: Int): Double {
        val change = exp(w[17] * (rating - 3.0 + w[18])) * s.pow(-w[19])
        return (s * if (rating >= 3) max(change, 1.0) else change).coerceIn(S_MIN, S_MAX)
    }

    /**
     * The steps of a card being learned ([first] its first answer) or relearned: answers drawn by how
     * the steps go from each, until enough are passed in a row or Easy ends them; with their seconds.
     */
    private fun shortTerm(
        w: DoubleArray,
        stability: Double,
        difficulty: Double,
        first: Int?,
        costs: List<Double>,
        steps: List<List<Double>>,
        stepCount: Int,
        random: Random,
    ): Triple<Double, Double, Double> {
        var rating = first ?: 1
        var cost = if (first == null) 0.0 else costs[rating - 1]
        var s = stability
        var d = difficulty
        var passed = 0
        val needed = if (rating > 2) (stepCount - 1).coerceAtLeast(0) else stepCount
        repeat(MAX_STEPS) {
            if (passed >= needed || rating >= 4) return Triple(s.coerceIn(S_MIN, S_MAX), d.coerceIn(D_MIN, D_MAX), cost)
            rating = pick(steps[rating - 1], random) + 1
            s = shortTermStability(w, s, rating)
            d = nextDifficulty(w, d, rating)
            cost += costs[rating - 1]
            if (rating > 2) passed++ else if (rating == 1) passed = 0
        }
        return Triple(s.coerceIn(S_MIN, S_MAX), d.coerceIn(D_MIN, D_MAX), cost)
    }
}
