package com.tayra.languages.core.domain.flashcards

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** How well a card is remembered: [stability] is the days until recall drops to 90%, [difficulty] runs from 1 to 10. */
data class MemoryState(val stability: Double, val difficulty: Double)

/** The answer to a card, as in Anki. */
enum class Rating(val value: Int, val label: String) {
    AGAIN(1, "Again"),
    HARD(2, "Hard"),
    GOOD(3, "Good"),
    EASY(4, "Easy");

    companion object {
        fun of(value: Int): Rating = entries.firstOrNull { it.value == value } ?: GOOD
    }
}

/**
 * The FSRS-6 memory model, as in the fsrs crate (6.6) that Anki schedules with, with its default
 * parameters: how a memory state changes with each answer, and how long to wait for a wanted
 * chance of recall.
 */
class Fsrs(private val w: DoubleArray = DEFAULT_PARAMETERS) {

    private val decay = -w[20]
    private val factor = exp(ln(0.9) / decay) - 1.0

    /** The chance of recalling a card [elapsedDays] after its last review. */
    fun retrievability(elapsedDays: Double, stability: Double): Double = (elapsedDays / stability * factor + 1.0).pow(decay)

    /** The days after which the chance of recall falls to [desiredRetention]. */
    fun interval(stability: Double, desiredRetention: Double): Double = stability / factor * (desiredRetention.pow(1.0 / decay) - 1.0)

    /** The memory state after answering [rating], [elapsedDays] after the last review; [state] is null for a first answer. */
    fun next(state: MemoryState?, elapsedDays: Int, rating: Rating): MemoryState {
        val g = rating.value
        if (state == null) {
            return MemoryState(initStability(g).coerceIn(S_MIN, S_MAX), initDifficulty(g).coerceIn(D_MIN, D_MAX))
        }
        val lastS = state.stability.coerceIn(S_MIN, S_MAX)
        val lastD = state.difficulty.coerceIn(D_MIN, D_MAX)
        val r = retrievability(elapsedDays.toDouble(), lastS)
        val stability = when {
            // Reviews on the same day follow the short-term curve.
            elapsedDays == 0 -> stabilityShortTerm(lastS, g)
            rating == Rating.AGAIN -> stabilityAfterFailure(lastS, lastD, r)
            else -> stabilityAfterSuccess(lastS, lastD, r, g)
        }
        val difficulty = meanReversion(nextDifficulty(lastD, g)).coerceIn(D_MIN, D_MAX)
        return MemoryState(stability.coerceIn(S_MIN, S_MAX), difficulty)
    }

    internal fun initStability(rating: Int): Double = w[(rating - 1).coerceIn(0, 3)]

    internal fun initDifficulty(rating: Int): Double = w[4] - exp(w[5] * (rating - 1).coerceAtLeast(0)) + 1.0

    internal fun nextDifficulty(difficulty: Double, rating: Int): Double {
        val delta = -w[6] * (rating - 3.0)
        // The closer to the hardest, the less an answer moves it.
        return difficulty + (10.0 - difficulty) * delta / 9.0
    }

    internal fun meanReversion(difficulty: Double): Double = w[7] * (initDifficulty(4) - difficulty) + difficulty

    internal fun stabilityAfterSuccess(lastS: Double, lastD: Double, r: Double, rating: Int): Double {
        val hardPenalty = if (rating == 2) w[15] else 1.0
        val easyBonus = if (rating == 4) w[16] else 1.0
        return lastS * (exp(w[8]) * (11.0 - lastD) * lastS.pow(-w[9]) * (exp((1.0 - r) * w[10]) - 1.0) * hardPenalty * easyBonus + 1.0)
    }

    internal fun stabilityAfterFailure(lastS: Double, lastD: Double, r: Double): Double {
        val fresh = w[11] * lastD.pow(-w[12]) * ((lastS + 1.0).pow(w[13]) - 1.0) * exp((1.0 - r) * w[14])
        return min(fresh, lastS / exp(w[17] * w[18]))
    }

    internal fun stabilityShortTerm(lastS: Double, rating: Int): Double {
        val increase = exp(w[17] * (rating - 3.0 + w[18])) * lastS.pow(-w[19])
        return lastS * if (rating >= 2) max(increase, 1.0) else increase
    }

    companion object {
        val DEFAULT_PARAMETERS = doubleArrayOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796,
            1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542,
        )
        private const val S_MIN = 0.001
        private const val S_MAX = 36500.0
        private const val D_MIN = 1.0
        private const val D_MAX = 10.0
    }
}
