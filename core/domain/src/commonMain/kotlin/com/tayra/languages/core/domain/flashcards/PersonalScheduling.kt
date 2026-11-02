package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.repository.FlashcardRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Each language's own scheduling: FSRS parameters trained on the answers to its cards, kept only
 * when they predict those answers better than the ones in use. Once a language has new ones, its
 * cards' memory is worked out again from their answers; their due dates stay as they are.
 */
class PersonalScheduling(
    private val cards: FlashcardRepository,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.System,
) {
    /** What a training came to. */
    sealed interface Outcome {
        /** New parameters, predicting the answers better than the defaults by [gain], a share of their log loss. */
        data class Trained(val gain: Double) : Outcome

        /** The parameters in use, the defaults or those trained before, still predict the answers best. */
        data object Kept : Outcome

        /** Too few answers to learn from yet. */
        data object TooFew : Outcome
    }

    /** One training at a time, whether asked for or of its own accord. */
    private val training = Mutex()
    private val progressState = MutableStateFlow<Map<Long, Double>>(emptyMap())

    /** The languages being trained now, with the share of each done. */
    val progress: StateFlow<Map<Long, Double>> = progressState.asStateFlow()

    suspend fun current(languageId: Long): PersonalParameters? = cards.parameters(languageId)

    suspend fun reviewCount(languageId: Long): Int = cards.reviewCount(languageId)

    /** Trains [languageId]'s parameters on its answers, its progress shown in [progress]. */
    suspend fun optimize(languageId: Long): Outcome = training.withLock {
        progressState.update { it + (languageId to 0.0) }
        try {
            train(languageId) { done -> progressState.update { it + (languageId to done) } }
        } finally {
            progressState.update { it - languageId }
        }
    }

    /** Trains each of [languageIds] that [isDue] says is due, one after another. */
    suspend fun optimizeWhereDue(languageIds: List<Long>) {
        for (id in languageIds) if (isDue(id)) optimize(id)
    }

    private suspend fun train(languageId: Long, onProgress: (Double) -> Unit): Outcome {
        val reviews = cards.reviewsOf(languageId)
        val histories = histories(reviews)
        val previous = cards.parameters(languageId)
        val inUse = previous?.parameters?.toDoubleArray() ?: Fsrs.DEFAULT_PARAMETERS
        val relearningSteps = LearningSteps.parseOr(settings.current.flashcardRelearnSteps, UserSettings().flashcardRelearnSteps).size.coerceAtLeast(1)
        val result = withContext(Dispatchers.Default) {
            val trained = FsrsOptimizer.optimize(histories, relearningSteps, onProgress) ?: return@withContext null
            Triple(trained, FsrsOptimizer.logLoss(trained, histories), FsrsOptimizer.logLoss(inUse, histories) to FsrsOptimizer.logLoss(Fsrs.DEFAULT_PARAMETERS, histories))
        }
        val now = clock.now()
        // Even a training that changes nothing is noted, so the next one waits for more answers.
        val (trained, loss, losses) = result ?: run {
            cards.saveParameters(PersonalParameters(languageId, previous?.parameters, reviews.size, now, previous?.gain ?: 0.0))
            return Outcome.TooFew
        }
        val (inUseLoss, defaultLoss) = losses
        if (loss == null || inUseLoss == null || defaultLoss == null || loss >= inUseLoss) {
            cards.saveParameters(PersonalParameters(languageId, previous?.parameters, reviews.size, now, previous?.gain ?: 0.0))
            return Outcome.Kept
        }
        val gain = ((defaultLoss - loss) / defaultLoss).coerceAtLeast(0.0)
        cards.saveParameters(PersonalParameters(languageId, trained.toList(), reviews.size, now, gain))
        rememberAgain(languageId, trained, reviews)
        return Outcome.Trained(gain)
    }

    /** Goes back to the default parameters for [languageId], its cards' memory worked out again with them. */
    suspend fun useDefaults(languageId: Long) {
        cards.clearParameters(languageId)
        rememberAgain(languageId, Fsrs.DEFAULT_PARAMETERS, cards.reviewsOf(languageId))
    }

    /**
     * Whether [languageId] is due a training of its own accord: [AUTO_REVIEWS] answers since the
     * last, and [AUTO_INTERVAL] since it ran.
     */
    suspend fun isDue(languageId: Long): Boolean {
        val count = cards.reviewCount(languageId)
        val last = cards.parameters(languageId) ?: return count >= AUTO_REVIEWS
        return count - last.reviews >= AUTO_REVIEWS && clock.now() - last.trainedAt >= AUTO_INTERVAL
    }

    /** Works out again the memory of [languageId]'s answered cards from their [reviews], under [parameters]. */
    private suspend fun rememberAgain(languageId: Long, parameters: DoubleArray, reviews: List<FlashcardReview>) {
        val fsrs = Fsrs(parameters)
        val answered = cards.list(languageId).filter { it.schedule.state != CardState.NEW }.map { it.termId }.toSet()
        val memories = withContext(Dispatchers.Default) {
            histories(reviews).filter { it.id in answered }.associate { history ->
                var state: MemoryState? = null
                history.reviews.forEach { state = fsrs.next(state, it.deltaT, Rating.of(it.rating)) }
                history.id to state!!
            }
        }
        cards.setMemories(memories)
    }

    companion object {
        /** New answers that make a training worth running of its own accord. */
        const val AUTO_REVIEWS = 500
        val AUTO_INTERVAL = 30.days

        /**
         * Each card's answers as training reads them, from its last answer as a new card on: the
         * history before a reset no longer says how it is remembered.
         */
        fun histories(reviews: List<FlashcardReview>): List<FsrsCard> = reviews.groupBy { it.termId }.mapNotNull { (termId, answers) ->
            val ordered = answers.sortedWith(compareBy({ it.at }, { it.id }))
            val start = ordered.indexOfLast { it.stateBefore == CardState.NEW }.takeIf { it >= 0 } ?: return@mapNotNull null
            val kept = ordered.drop(start)
            FsrsCard(
                termId,
                kept.mapIndexed { index, review -> FsrsReview(review.rating.value, if (index == 0) 0 else review.elapsedDays) },
                kept.map { it.at.toEpochMilliseconds() },
            )
        }
    }
}
