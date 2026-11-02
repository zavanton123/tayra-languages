package com.tayra.languages.core.domain.flashcards

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/** One answer of a card's history as training reads it: its rating, and the study days since the answer before, 0 the same day. */
data class FsrsReview(val rating: Int, val deltaT: Int)

/** A card's answers since it was last new, in order, with when each was given, in epoch milliseconds. */
data class FsrsCard(val id: Long, val reviews: List<FsrsReview>, val times: List<Long>)

/**
 * Fits the 21 FSRS-6 parameters to a learner's own answers, as fsrs-rs 6.6 (the crate Anki trains
 * with) does: every answer a day or more after the one before is a prediction to learn from; odd
 * first intervals are left out; the four first stabilities are fitted to the first reviews; then
 * Adam, with a cosine learning rate, trains all 21 over the cards a batch at a time, the recent
 * answers weighing more and every parameter held near where it started and within its bounds.
 * Gradients are exact, carried forward through the memory model with each value.
 */
object FsrsOptimizer {
    private const val EPOCHS = 8
    private const val BATCH = 512
    private const val LEARNING_RATE = 4e-2
    private const val MAX_SEQUENCE = 256
    private const val GAMMA = 1.0
    private const val SEED = 2023

    private val DEFAULT = Fsrs.DEFAULT_PARAMETERS

    /** How far each parameter may stray, as the pull towards the starting point measures it. */
    private val SPREAD = doubleArrayOf(
        6.43, 9.66, 17.58, 27.85, 0.57, 0.28, 0.6, 0.12, 0.39, 0.18, 0.33, 0.3, 0.09, 0.16, 0.57, 0.25,
        1.03, 0.31, 0.32, 0.14, 0.27,
    )

    /** A prediction: [card]'s answer at [position], judged from those before it. */
    internal class Item(val card: FsrsCard, val position: Int) {
        val current: FsrsReview get() = card.reviews[position]
        val time: Long get() = card.times[position]
        val longTermCount: Int get() = (0..position).count { card.reviews[it].deltaT > 0 }
        val firstLongTerm: FsrsReview get() = card.reviews.take(position + 1).first { it.deltaT > 0 }
    }

    /** Every prediction [cards] hold, the earliest first. */
    internal fun items(cards: List<FsrsCard>): List<Item> =
        cards.flatMap { card -> (1 until card.reviews.size).filter { card.reviews[it].deltaT > 0 }.map { Item(card, it) } }
            .sortedWith(compareBy({ it.time }, { it.card.id }))

    /**
     * The parameters [cards] call for, or null when they are too few to say: below 8 predictions
     * the defaults stand, and below 64 only the first stabilities are fitted. [relearningSteps]
     * bounds the short-term parameters, as the relearning steps in use do; [onProgress] hears the
     * share done, and training stops early, with null, once [keepGoing] says so.
     */
    fun optimize(
        cards: List<FsrsCard>,
        relearningSteps: Int = 1,
        onProgress: (Double) -> Unit = {},
        keepGoing: () -> Boolean = { true },
    ): DoubleArray? {
        val all = items(cards)
        val (initialization, trainSet) = withoutOutliers(all)
        if (trainSet.size < 8) return null
        val averageRecall = trainSet.count { it.current.rating > 1 }.toDouble() / trainSet.size
        val (firstStabilities, ratingCounts) = initialStabilities(initialization, averageRecall) ?: return null
        val initialized = firstStabilities + DEFAULT.copyOfRange(4, 21)
        if (trainSet.size == initialization.size || trainSet.size < 64) return initialized
        val last = (trainSet.size - 1).coerceAtLeast(1).toDouble()
        val weighted = trainSet.mapIndexed { index, item -> item to 0.25 + 0.75 * (index / last).pow(3) }
            .filter { (item, _) -> item.position + 1 <= MAX_SEQUENCE }
        val trained = train(weighted, initialized, relearningSteps, onProgress, keepGoing) ?: return null
        if (trained.any { !it.isFinite() }) return null
        val stabilities = smoothAndFill(mutableMapOf(1 to trained[0], 2 to trained[1], 3 to trained[2], 4 to trained[3]), ratingCounts) ?: return null
        return stabilities + trained.copyOfRange(4, 21)
    }

    /**
     * How badly [parameters] predict [cards]' answers: the cross-entropy of every prediction, the
     * recent ones weighing more; null without a prediction to make.
     */
    fun logLoss(parameters: DoubleArray, cards: List<FsrsCard>): Double? {
        val all = items(cards)
        if (all.isEmpty()) return null
        val fsrs = Fsrs(parameters)
        val last = (all.size - 1).coerceAtLeast(1).toDouble()
        var loss = 0.0
        var weights = 0.0
        all.forEachIndexed { index, item ->
            var state: MemoryState? = null
            for (i in 0 until item.position) state = fsrs.next(state, item.card.reviews[i].deltaT, Rating.of(item.card.reviews[i].rating))
            val r = fsrs.retrievability(item.current.deltaT.toDouble(), state!!.stability).coerceIn(MIN_PROBABILITY, MAX_PROBABILITY)
            val label = if (item.current.rating > 1) 1.0 else 0.0
            val weight = 0.25 + 0.75 * (index / last).pow(3)
            loss += (label * ln(r) + (1 - label) * ln(1 - r)) * weight
            weights += weight
        }
        return -loss / weights
    }

    /**
     * The predictions to fit the first stabilities with, and those to train on: a first review's
     * interval seen too rarely, or too long, is left out of both, as fsrs-rs leaves outliers out.
     */
    internal fun withoutOutliers(items: List<Item>): Pair<List<Item>, List<Item>> {
        val firsts = items.filter { it.longTermCount == 1 }
        val removed = Array(5) { mutableSetOf<Int>() }
        val kept = mutableListOf<Item>()
        firsts.groupBy { it.card.reviews[0].rating }.toList().sortedBy { it.first }.forEach { (rating, group) ->
            // The commonest intervals first, the longer of two as common first.
            val byInterval = group.groupBy { it.current.deltaT }.toList()
                .sortedWith { a, b -> if (a.second.size != b.second.size) b.second.size - a.second.size else b.first - a.first }
            val total = group.size
            var removedSoFar = 0
            for ((deltaT, sub) in byInterval.reversed()) {
                if (removedSoFar + sub.size >= max(20, total / 20)) {
                    if (sub.size >= 6 && deltaT <= if (rating != 4) 100 else 365) kept += sub else removed[rating] += deltaT
                } else {
                    removedSoFar += sub.size
                    removed[rating] += deltaT
                }
            }
        }
        return kept to items.filter { it.firstLongTerm.deltaT !in removed[it.card.reviews[0].rating] }
    }

    /** The four first stabilities fitted to the first reviews, with how many first reviews each rating had; null with none. */
    internal fun initialStabilities(items: List<Item>, averageRecall: Double): Pair<DoubleArray, Map<Int, Int>>? {
        val groups = items.filter { it.longTermCount == 1 }.groupBy { it.card.reviews[0].rating }
        val counts = groups.mapValues { it.value.size }
        val fitted = groups.mapValues { (rating, group) ->
            val points = group.groupBy { it.firstLongTerm.deltaT }.map { (deltaT, answers) ->
                Triple(deltaT.toDouble(), answers.count { it.firstLongTerm.rating > 1 }.toDouble() / answers.size, answers.size.toDouble())
            }
            fitFirstStability(points, averageRecall, DEFAULT[rating - 1])
        }.toMutableMap()
        return smoothAndFill(fitted, counts)?.let { it to counts }
    }

    /** The stability that best fits [points] (interval, share recalled, count), the defaults' curve assumed, a little drawn to [default]. */
    private fun fitFirstStability(points: List<Triple<Double, Double, Double>>, averageRecall: Double, default: Double): Double {
        val decay = -DEFAULT[20]
        val factor = 0.9.pow(1.0 / decay) - 1.0
        fun loss(s: Double): Double = points.sumOf { (t, recall, count) ->
            val smoothed = (recall * count + averageRecall) / (count + 1.0)
            val y = (t / s * factor + 1.0).pow(decay)
            -(smoothed * ln(y) + (1 - smoothed) * ln(1 - y)) * count
        } + abs(s - default) / 16.0
        var low = S_MIN
        var high = INIT_S_MAX
        var best = default
        var iterations = 0
        while (high - low > EPSILON && iterations < 1000) {
            iterations++
            val first = low + (high - low) / 3.0
            val second = high - (high - low) / 3.0
            if (loss(first) < loss(second)) high = second else low = first
            best = (high + low) / 2.0
        }
        return best
    }

    /**
     * The four first stabilities in rising order, those of ratings never seen filled in from the
     * others as fsrs-rs fills them; null with none seen.
     */
    internal fun smoothAndFill(stabilities: MutableMap<Int, Double>, counts: Map<Int, Int>): DoubleArray? {
        stabilities.keys.retainAll(counts.keys)
        for ((small, big) in listOf(1 to 2, 2 to 3, 3 to 4, 1 to 3, 2 to 4, 1 to 4)) {
            val smallValue = stabilities[small] ?: continue
            val bigValue = stabilities[big] ?: continue
            if (smallValue > bigValue) {
                if (counts.getValue(small) > counts.getValue(big)) stabilities[big] = smallValue else stabilities[small] = bigValue
            }
        }
        val w1 = 0.41
        val w2 = 0.54
        val s = arrayOfNulls<Double>(5)
        for (rating in 1..4) s[rating] = stabilities[rating]
        val filled: List<Double> = when (stabilities.size) {
            0 -> return null
            1 -> {
                val (rating, value) = stabilities.entries.single()
                val factor = value / DEFAULT[rating - 1]
                (0..3).map { DEFAULT[it] * factor }.sorted()
            }
            2 -> {
                val (r1, r2, r3, r4) = listOf(s[1], s[2], s[3], s[4])
                when {
                    r1 == null && r2 == null && r3 != null && r4 != null -> {
                        val two = r3.pow(1.0 / (1.0 - w2)) * r4.pow(1.0 - 1.0 / (1.0 - w2))
                        s[2] = two
                        s[1] = two.pow(1.0 / w1) * r3.pow(1.0 - 1.0 / w1)
                    }
                    r1 == null && r2 != null && r3 == null && r4 != null -> {
                        val three = r2.pow(1.0 - w2) * r4.pow(w2)
                        s[3] = three
                        s[1] = r2.pow(1.0 / w1) * three.pow(1.0 - 1.0 / w1)
                    }
                    r1 == null && r2 != null && r3 != null && r4 == null -> {
                        s[4] = r2.pow(1.0 - 1.0 / w2) * r3.pow(1.0 / w2)
                        s[1] = r2.pow(1.0 / w1) * r3.pow(1.0 - 1.0 / w1)
                    }
                    r1 != null && r2 == null && r3 == null && r4 != null -> {
                        val share = w1 / (w1 + w2 - w1 * w2)
                        val shareTwo = w2 / (w1 + w2 - w1 * w2)
                        s[2] = r1.pow(share) * r4.pow(1.0 - share)
                        s[3] = r1.pow(1.0 - shareTwo) * r4.pow(shareTwo)
                    }
                    r1 != null && r2 == null && r3 != null && r4 == null -> {
                        val two = r1.pow(w1) * r3.pow(1.0 - w1)
                        s[2] = two
                        s[4] = two.pow(1.0 - 1.0 / w2) * r3.pow(1.0 / w2)
                    }
                    r1 != null && r2 != null && r3 == null && r4 == null -> {
                        val three = r1.pow(1.0 - 1.0 / (1.0 - w1)) * r2.pow(1.0 / (1.0 - w1))
                        s[3] = three
                        s[4] = r2.pow(1.0 - 1.0 / w2) * three.pow(1.0 / w2)
                    }
                }
                s.filterNotNull()
            }
            3 -> {
                val (r1, r2, r3, r4) = listOf(s[1], s[2], s[3], s[4])
                when {
                    r1 == null && r2 != null && r3 != null -> s[1] = r2.pow(1.0 / w1) * r3.pow(1.0 - 1.0 / w1)
                    r1 != null && r2 == null && r3 != null -> s[2] = r1.pow(w1) * r3.pow(1.0 - w1)
                    r2 != null && r3 == null && r4 != null -> s[3] = r2.pow(1.0 - w2) * r4.pow(w2)
                    r2 != null && r3 != null && r4 == null -> s[4] = r2.pow(1.0 - 1.0 / w2) * r3.pow(1.0 / w2)
                }
                s.filterNotNull()
            }
            else -> s.filterNotNull()
        }
        if (filled.size < 4) return null
        return DoubleArray(4) { filled[it].coerceIn(S_MIN, INIT_S_MAX) }
    }

    /** A card's answers up to its last prediction, and the label and weight of each answer predicted. */
    internal class Sequence(val reviews: List<FsrsReview>, val labels: Map<Int, Pair<Double, Double>>)

    /** Adam over [weighted] predictions, a card's predictions in one batch, the batches in a new order each pass. */
    private fun train(
        weighted: List<Pair<Item, Double>>,
        start: DoubleArray,
        relearningSteps: Int,
        onProgress: (Double) -> Unit,
        keepGoing: () -> Boolean,
    ): DoubleArray? {
        val total = weighted.size
        val batches = cardBatches(weighted)
        val iterations = (total / BATCH + 1) * EPOCHS
        val schedule = CosineAnnealing(iterations.toDouble(), LEARNING_RATE)
        val parameters = start.copyOf()
        val adam = Adam()
        val random = Random(SEED)
        val steps = batches.size * EPOCHS
        var done = 0
        repeat(EPOCHS) {
            for ((sequences, predictions) in batches.shuffled(random)) {
                if (!keepGoing()) return null
                val rate = schedule.step()
                val gradient = lossAndGradient(parameters, sequences).second
                val scale = GAMMA * predictions / total
                for (i in 0 until 21) gradient[i] += 2.0 * (parameters[i] - start[i]) / (SPREAD[i] * SPREAD[i]) * scale
                adam.step(parameters, gradient, rate)
                clip(parameters, relearningSteps)
                done++
                onProgress(done.toDouble() / steps)
            }
        }
        return parameters
    }

    /** The cards of [weighted], each with all its predictions, gathered into batches of about [BATCH] predictions. */
    private fun cardBatches(weighted: List<Pair<Item, Double>>): List<Pair<List<Sequence>, Int>> {
        val cards = weighted.groupBy { it.first.card.id }.toList().sortedBy { it.first }.map { it.second }
            .map { group -> group.sortedBy { it.first.position } }
            .sortedBy { it.last().first.position }
        val batches = mutableListOf<Pair<List<Sequence>, Int>>()
        val current = mutableListOf<Sequence>()
        var predictions = 0
        for (card in cards) {
            if (current.isNotEmpty() && predictions + card.size > BATCH) {
                batches += current.toList() to predictions
                current.clear()
                predictions = 0
            }
            val reviews = card.last().first.card.reviews.take(card.last().first.position + 1)
            current += Sequence(reviews, card.associate { (item, weight) -> item.position to ((if (item.current.rating > 1) 1.0 else 0.0) to weight) })
            predictions += card.size
        }
        if (current.isNotEmpty()) batches += current.toList() to predictions
        return batches
    }

    /** The summed loss of [sequences]' predictions under [parameters], and its gradient by each parameter. */
    internal fun lossAndGradient(parameters: DoubleArray, sequences: List<Sequence>): Pair<Double, DoubleArray> {
        val w = Array(21) { Dual.parameter(it, parameters[it]) }
        val model = DualModel(w)
        var loss = 0.0
        val gradient = DoubleArray(21)
        for (sequence in sequences) {
            var s: Dual? = null
            var d: Dual? = null
            sequence.reviews.forEachIndexed { t, review ->
                if (s == null || d == null) {
                    val rating = review.rating.coerceIn(1, 4)
                    s = w[rating - 1].clamp(S_MIN, S_MAX)
                    d = model.initialDifficulty(rating).clamp(D_MIN, D_MAX)
                    return@forEachIndexed
                }
                val lastS = s!!.clamp(S_MIN, S_MAX)
                val lastD = d!!.clamp(D_MIN, D_MAX)
                val r = model.retrievability(review.deltaT.toDouble(), lastS)
                sequence.labels[t]?.let { (label, weight) ->
                    if (weight != 0.0) {
                        val step = crossEntropy(r, label, weight)
                        loss += step.v
                        for (i in 0 until 21) gradient[i] += step.d[i]
                    }
                }
                if (t + 1 < sequence.reviews.size) {
                    val (nextS, nextD) = model.step(lastS, lastD, r, review.deltaT, review.rating)
                    s = nextS
                    d = nextD
                }
            }
        }
        return loss to gradient
    }

    private fun crossEntropy(r: Dual, label: Double, weight: Double): Dual {
        // At the bounds the probability is held, and teaches nothing.
        val held = if (r.v > MIN_PROBABILITY && r.v < MAX_PROBABILITY) r else Dual.constant(r.v.coerceIn(MIN_PROBABILITY, MAX_PROBABILITY))
        return ((held.ln() * label) + ((1.0 - held).ln() * (1 - label))) * -weight
    }

    /** Keeps each parameter within its bounds, the short-term ones tighter with more relearning steps, as fsrs-rs does. */
    internal fun clip(w: DoubleArray, relearningSteps: Int) {
        val shortTermCeiling = if (relearningSteps > 1) {
            min(sqrt(max(-(ln(w[11]) + ln(2.0.pow(w[13]) - 1.0) + w[14] * 0.3) / relearningSteps, 0.01)), 2.0)
        } else {
            2.0
        }
        val bounds = arrayOf(
            S_MIN to INIT_S_MAX, S_MIN to INIT_S_MAX, S_MIN to INIT_S_MAX, S_MIN to INIT_S_MAX,
            D_MIN to D_MAX, 0.001 to 4.0, 0.001 to 4.0, 0.001 to 0.75, 0.0 to 4.5, 0.0 to 0.8, 0.001 to 3.5,
            0.001 to 5.0, 0.001 to 0.25, 0.001 to 0.9, 0.0 to 4.0, 0.0 to 1.0, 1.0 to 6.0,
            0.0 to shortTermCeiling, 0.0 to shortTermCeiling, 0.01 to 0.8, 0.1 to 0.8,
        )
        for (i in 0 until 21) w[i] = w[i].coerceIn(bounds[i].first, bounds[i].second)
    }

    private class Adam {
        private val m = DoubleArray(21)
        private val v = DoubleArray(21)
        private var t = 0

        fun step(parameters: DoubleArray, gradient: DoubleArray, rate: Double) {
            t++
            val bias1 = 1.0 - 0.9.pow(t)
            val bias2 = 1.0 - 0.999.pow(t)
            for (i in 0 until 21) {
                m[i] = 0.9 * m[i] + 0.1 * gradient[i]
                v[i] = 0.999 * v[i] + 0.001 * gradient[i] * gradient[i]
                parameters[i] -= rate * (m[i] / bias1) / (sqrt(v[i] / bias2) + 1e-8)
            }
        }
    }

    /** The learning rate falling along half a cosine over [steps] steps, as PyTorch's CosineAnnealingLR, which fsrs-rs follows. */
    private class CosineAnnealing(private val steps: Double, private val initial: Double) {
        private var count = -1.0
        private var rate = initial

        fun step(): Double {
            count++
            rate = when {
                count == 0.0 -> initial
                (count - 1.0 - steps) % (2.0 * steps) == 0.0 -> initial * (1.0 - cos(PI / steps)) / 2.0
                else -> (1.0 + cos(PI * count / steps)) / (1.0 + cos(PI * (count - 1.0) / steps)) * rate
            }
            return rate
        }
    }

    private const val S_MIN = 0.001
    private const val S_MAX = 36500.0
    private const val D_MIN = 1.0
    private const val D_MAX = 10.0
    private const val INIT_S_MAX = 100.0
    private const val MIN_PROBABILITY = 1e-7
    private const val MAX_PROBABILITY = 1.0 - 1e-7
    private const val EPSILON = 2.220446049250313e-16
}

/** The FSRS-6 memory model over [Dual]s, as fsrs-rs's analytic trainer runs it. */
private class DualModel(private val w: Array<Dual>) {
    private val decay = -w[20]
    private val factor = (Dual.constant(ln(0.9)) / decay).exp() - 1.0

    fun retrievability(t: Double, s: Dual): Dual = ((Dual.constant(max(t, 0.0)) / s) * factor + 1.0).pow(decay)

    fun initialDifficulty(rating: Int): Dual = w[4] - (w[5] * (rating - 1).toDouble()).exp() + 1.0

    /** The memory after answering [rating] [deltaT] days on, from [s] and [d] already held in bounds, [r] its recall then. */
    fun step(s: Dual, d: Dual, r: Dual, deltaT: Int, rating: Int): Pair<Dual, Dual> {
        val newS = when {
            deltaT == 0 -> {
                val raw = (w[17] * (w[18] + (rating - 3.0))).exp() * s.pow(-w[19])
                s * if (rating >= 2 && raw.v < 1.0) Dual.constant(1.0) else raw
            }
            rating == 1 -> {
                val raw = w[11] * d.pow(-w[12]) * ((s + 1.0).pow(w[13]) - 1.0) * ((1.0 - r) * w[14]).exp()
                val floor = s / (w[17] * w[18]).exp()
                if (floor.v < raw.v) floor else raw
            }
            else -> {
                var increase = w[8].exp() * (11.0 - d) * s.pow(-w[9]) * (((1.0 - r) * w[10]).exp() - 1.0)
                if (rating == 2) increase *= w[15]
                if (rating == 4) increase *= w[16]
                s * (increase + 1.0)
            }
        }
        val next = d + (10.0 - d) * (w[6] * -(rating - 3.0)) / 9.0
        val reverted = w[7] * (initialDifficulty(4) - next) + next
        return newS.clamp(0.001, 36500.0) to reverted.clamp(1.0, 10.0)
    }
}

/** A number with its derivative by each of the 21 parameters, carried through every operation. */
internal class Dual(val v: Double, val d: DoubleArray) {
    operator fun plus(o: Dual) = Dual(v + o.v, DoubleArray(SIZE) { d[it] + o.d[it] })
    operator fun plus(c: Double) = Dual(v + c, d)
    operator fun minus(o: Dual) = Dual(v - o.v, DoubleArray(SIZE) { d[it] - o.d[it] })
    operator fun minus(c: Double) = Dual(v - c, d)
    operator fun unaryMinus() = Dual(-v, DoubleArray(SIZE) { -d[it] })
    operator fun times(o: Dual) = Dual(v * o.v, DoubleArray(SIZE) { d[it] * o.v + o.d[it] * v })
    operator fun times(c: Double) = Dual(v * c, DoubleArray(SIZE) { d[it] * c })
    operator fun div(o: Dual) = Dual(v / o.v, DoubleArray(SIZE) { (d[it] * o.v - v * o.d[it]) / (o.v * o.v) })
    operator fun div(c: Double) = Dual(v / c, DoubleArray(SIZE) { d[it] / c })

    fun exp(): Dual {
        val e = exp(v)
        return Dual(e, DoubleArray(SIZE) { d[it] * e })
    }

    fun ln(): Dual = Dual(ln(v), DoubleArray(SIZE) { d[it] / v })

    fun pow(e: Dual): Dual {
        val r = v.pow(e.v)
        val logBase = ln(v)
        return Dual(r, DoubleArray(SIZE) { r * (e.d[it] * logBase + e.v * d[it] / v) })
    }

    /** Held within [low] and [high]; a value pressed against either no longer moves with the parameters. */
    fun clamp(low: Double, high: Double): Dual = if (v in low..high) this else constant(v.coerceIn(low, high))

    companion object {
        const val SIZE = 21
        private val NONE = DoubleArray(SIZE)

        fun constant(value: Double) = Dual(value, NONE)

        fun parameter(index: Int, value: Double) = Dual(value, DoubleArray(SIZE).also { it[index] = 1.0 })
    }
}

private operator fun Double.minus(o: Dual) = Dual(this - o.v, DoubleArray(Dual.SIZE) { -o.d[it] })
