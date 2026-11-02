package com.tayra.languages.core.domain.flashcards

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FsrsOptimizerTest {

    /** fsrs-rs's own check of its loss and gradient (training.rs, test_loss_and_grad), with the default parameters. */
    @Test
    fun lossAndGradientMatchFsrsRs() {
        val intervals = listOf(listOf(0, 0, 0, 0, 1, 3), listOf(0, 0, 0, 1, 3, 6), listOf(0, 0, 0, 1, 3, 6), listOf(0, 0, 1, 3, 5, 12))
        val ratings = listOf(listOf(1, 3, 1, 4, 3, 2), listOf(2, 4, 4, 3, 1, 3), listOf(3, 2, 4, 3, 3, 3), listOf(4, 4, 3, 3, 3, 4))
        val asked = listOf(4, 11, 12, 23)
        val recalled = listOf(true, true, true, false)
        val sequences = (0 until 4).map { column ->
            val history = intervals[column].zip(ratings[column]) { t, r -> FsrsReview(r, t) }
            FsrsOptimizer.Sequence(history + FsrsReview(if (recalled[column]) 3 else 1, asked[column]), mapOf(6 to ((if (recalled[column]) 1.0 else 0.0) to 1.0)))
        }
        val (loss, gradient) = FsrsOptimizer.lossAndGradient(Fsrs.DEFAULT_PARAMETERS, sequences)
        assertEquals(4.0466027, loss, 1e-5)
        val expected = doubleArrayOf(
            -0.095688485, -0.0051607806, -0.0012249565, 0.007462064, 0.03650761, -0.082112335, 0.0593964, -2.1474836,
            0.57626534, -2.8751316, 0.7154875, -0.028993709, 0.0099172965, -0.2189217, -0.0017800558, -0.089381434,
            0.299141, 0.068104014, -0.011605468, -0.25398168, 0.27700496,
        )
        for (i in 0 until 21) assertTrue(abs(gradient[i] - expected[i]) <= 1e-4 + 1e-4 * abs(expected[i]), "w$i: ${gradient[i]} against ${expected[i]}")
    }

    /** A learner whose memory works by [truth], answering [cards] cards over [days] days, each review on its day. */
    private fun learner(truth: DoubleArray, cards: Int, days: Int, seed: Int): List<FsrsCard> {
        val random = Random(seed)
        val fsrs = Fsrs(truth)
        return (0 until cards).map { id ->
            val reviews = mutableListOf<FsrsReview>()
            val times = mutableListOf<Long>()
            var day = random.nextInt(days / 3)
            // Learned on its first day: a first answer, and Good on the step ten minutes on.
            val first = Rating.of(random.nextInt(1, 5))
            var state = fsrs.next(null, 0, first)
            reviews += FsrsReview(first.value, 0)
            times += day * 86_400_000L
            state = fsrs.next(state, 0, Rating.GOOD)
            reviews += FsrsReview(3, 0)
            times += day * 86_400_000L + 600_000
            while (true) {
                val interval = fsrs.interval(state.stability, 0.9).toInt().coerceAtLeast(1)
                if (day + interval >= days) break
                day += interval
                val recalled = random.nextDouble() < fsrs.retrievability(interval.toDouble(), state.stability)
                val rating = if (!recalled) Rating.AGAIN else if (random.nextDouble() < 0.15) Rating.HARD else if (random.nextDouble() < 0.1) Rating.EASY else Rating.GOOD
                state = fsrs.next(state, interval, rating)
                reviews += FsrsReview(rating.value, interval)
                times += day * 86_400_000L
            }
            FsrsCard(id.toLong(), reviews, times)
        }
    }

    @Test
    fun trainingFitsALearnerBetterThanTheDefaults() {
        // A learner who forgets faster than the defaults expect, and gains less from each review.
        val truth = Fsrs.DEFAULT_PARAMETERS.copyOf().also {
            it[0] = 0.1; it[1] = 0.6; it[2] = 1.2; it[3] = 4.0; it[8] = 1.4; it[10] = 0.6; it[20] = 0.3
        }
        val cards = learner(truth, cards = 600, days = 240, seed = 3)
        val trained = assertNotNull(FsrsOptimizer.optimize(cards))
        val before = FsrsOptimizer.logLoss(Fsrs.DEFAULT_PARAMETERS, cards)!!
        val after = FsrsOptimizer.logLoss(trained, cards)!!
        val truthLoss = FsrsOptimizer.logLoss(truth, cards)!!
        assertTrue(after < before, "trained $after against default $before")
        assertTrue(after < truthLoss * 1.02, "trained $after against the truth's $truthLoss")
        assertTrue(trained[2] < Fsrs.DEFAULT_PARAMETERS[2], "a learner who forgets sooner starts lower: ${trained[2]}")
        assertEquals(trained.toList(), FsrsOptimizer.optimize(cards)!!.toList(), "the same answers train the same parameters")
    }

    @Test
    fun tooFewAnswersTrainNothing() {
        val cards = learner(Fsrs.DEFAULT_PARAMETERS, cards = 3, days = 30, seed = 1)
        assertNull(FsrsOptimizer.optimize(cards))
        assertNull(FsrsOptimizer.optimize(emptyList()))
    }

    @Test
    fun aFewAnswersFitTheFirstStabilitiesAlone() {
        val cards = learner(Fsrs.DEFAULT_PARAMETERS, cards = 15, days = 10, seed = 5)
        val trained = FsrsOptimizer.optimize(cards) ?: return
        assertEquals(Fsrs.DEFAULT_PARAMETERS.drop(4), trained.drop(4), "below 64 predictions only the first four move")
        assertEquals(trained.take(4).sorted(), trained.take(4), "rising with the rating")
    }

    @Test
    fun trainingStopsWhenAsked() {
        val cards = learner(Fsrs.DEFAULT_PARAMETERS, cards = 300, days = 120, seed = 2)
        var progress = 0.0
        assertNull(FsrsOptimizer.optimize(cards, onProgress = { progress = it }, keepGoing = { progress < 0.3 }))
        assertTrue(progress in 0.3..0.5)
    }
}
