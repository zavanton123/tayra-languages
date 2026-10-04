package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.FlashcardRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The flashcards of the terms being learned. Every term at status 1 to 4 has a card; answering
 * cards schedules them with FSRS and moves the term's status with what the reviews show:
 *
 * - up, as the wait until the next review grows: a day or more is status 2, a week 3, three
 *   weeks 4 and ninety days Known;
 * - down one level when a review is forgotten.
 *
 * A status changed anywhere else wins: marking a term known, ignored or unknown retires its
 * card, and moving a known term back to learning starts its card over.
 */
class FlashcardService(
    private val cards: FlashcardRepository,
    private val terms: TermRepository,
    private val settings: SettingsRepository,
    private val scheduler: FlashcardScheduler = FlashcardScheduler(),
    private val clock: Clock = Clock.System,
) {
    /** The card to show next with its term and what each answer would do, or no card when the day's work is done. */
    data class Session(
        val card: Flashcard?,
        val term: Term?,
        val outcomes: Map<Rating, CardSchedule>,
        val counts: DueCounts,
        /** When the next card being learned comes back, if one does later today. */
        val nextLearningAt: Instant?,
        val shownAt: Instant,
    )

    /** What an answer changed, enough to take it back. */
    data class Answered(val before: Flashcard, val statusBefore: TermStatus, val statusAfter: TermStatus, val reviewId: Long)

    private fun config(s: UserSettings = settings.current) = SchedulerConfig(
        desiredRetention = s.flashcardRetention / 100.0,
        learnSteps = LearningSteps.parseOr(s.flashcardLearnSteps, UserSettings().flashcardLearnSteps),
        relearnSteps = LearningSteps.parseOr(s.flashcardRelearnSteps, UserSettings().flashcardRelearnSteps),
    )

    private data class Limits(val newLeft: Int, val reviewsLeft: Int)

    private suspend fun limits(languageId: Long?, now: Instant, config: SchedulerConfig): Limits {
        val s = settings.current
        val since = scheduler.dayStart(scheduler.day(now, config), config)
        val newLeft = (s.flashcardNewPerDay - cards.reviewsSince(languageId, CardState.NEW, since)).coerceAtLeast(0)
        val reviewsLeft = if (s.flashcardReviewsPerDay == 0) Int.MAX_VALUE
        else (s.flashcardReviewsPerDay - cards.reviewsSince(languageId, CardState.REVIEW, since)).coerceAtLeast(0)
        return Limits(newLeft, reviewsLeft)
    }

    /** The cards of [languageId] (null for every language) waiting today, within the daily limits. */
    suspend fun counts(languageId: Long?): DueCounts {
        val now = clock.now()
        cards.reconcile(now)
        val config = config()
        return counts(languageId, now, config, limits(languageId, now, config))
    }

    private suspend fun counts(languageId: Long?, now: Instant, config: SchedulerConfig, limits: Limits): DueCounts {
        val endOfDay = scheduler.dayStart(scheduler.day(now, config) + 1, config)
        return DueCounts(
            new = minOf(cards.count(languageId, setOf(CardState.NEW), Instant.DISTANT_FUTURE), limits.newLeft),
            learning = cards.count(languageId, STEPPING, endOfDay),
            review = minOf(cards.count(languageId, setOf(CardState.REVIEW), now), limits.reviewsLeft),
        )
    }

    /**
     * The next card: one being learned whose wait is over, then a review that is due, then a new
     * card, and when only cards being learned are left, one whose wait ends within twenty minutes.
     * [prefer] names a term whose card is shown first if it has one.
     */
    suspend fun next(languageId: Long?, prefer: Long? = null): Session {
        val now = clock.now()
        cards.reconcile(now)
        val config = config()
        val limits = limits(languageId, now, config)
        // [prefer] puts a card first whatever its turn: the one an answer was just taken back from.
        val card = prefer?.let { cards.get(it) }?.takeIf { !it.suspended }
            ?: cards.next(languageId, STEPPING, now)
            ?: (if (limits.reviewsLeft > 0) cards.next(languageId, setOf(CardState.REVIEW), now) else null)
            ?: (if (limits.newLeft > 0) cards.next(languageId, setOf(CardState.NEW), Instant.DISTANT_FUTURE) else null)
            ?: cards.next(languageId, STEPPING, now + LEARN_AHEAD)
        val term = card?.let { terms.getById(it.termId) }
        val counts = counts(languageId, now, config, limits)
        if (card == null || term == null) {
            val endOfDay = scheduler.dayStart(scheduler.day(now, config) + 1, config)
            val later = cards.earliestDue(languageId, STEPPING)?.takeIf { it < endOfDay }
            return Session(null, null, emptyMap(), counts, later, now)
        }
        return Session(card, term, scheduler.outcomes(card.schedule, now, config, fuzz(card)), counts, null, now)
    }

    /** Answers [card] and moves its term's status with it. */
    suspend fun answer(card: Flashcard, rating: Rating): Answered? {
        val now = clock.now()
        val term = terms.getById(card.termId) ?: return null
        val config = config()
        val before = card.schedule
        val after = scheduler.answer(before, rating, now, config, fuzz(card))
        val status = statusAfter(term.status, before, after, rating)
        val elapsed = before.lastReview?.let { (scheduler.day(now, config) - scheduler.day(it, config)).coerceAtLeast(0).toInt() } ?: 0
        val reviewId = cards.addReview(
            FlashcardReview(
                termId = card.termId,
                at = now,
                rating = rating,
                stateBefore = before.state,
                intervalDays = if (after.state.isReview) after.intervalDays else 0,
                elapsedDays = elapsed,
            ),
        )
        // The card records the status it gives the term, so the change is not taken for one made elsewhere.
        cards.save(card.copy(schedule = after, seenStatus = status))
        if (status != term.status) terms.updateStatus(listOf(term.id), status)
        return Answered(card, term.status, status, reviewId)
    }

    /** Takes [answered] back: the card, its term's status and the history are as before. */
    suspend fun undo(answered: Answered) {
        cards.deleteReview(answered.reviewId)
        cards.save(answered.before)
        if (answered.statusAfter != answered.statusBefore) terms.updateStatus(listOf(answered.before.termId), answered.statusBefore)
    }

    /** The term's card, if it has one. */
    suspend fun cardFor(termId: Long): Flashcard? {
        cards.reconcile(clock.now())
        return cards.get(termId)
    }

    suspend fun setSuspended(termId: Long, suspended: Boolean) {
        cards.reconcile(clock.now())
        cards.setSuspended(termId, suspended)
    }

    /** Forgets the card's history: it is a new card again. */
    suspend fun restart(termId: Long) {
        val now = clock.now()
        cards.reconcile(now)
        cards.restart(termId, now)
    }

    /** How many cards of the language being learned wait today; follows changes and the clock. */
    fun observeDueCount(): Flow<Int> {
        val everyMinute = flow {
            while (true) {
                emit(Unit)
                delay(1.minutes)
            }
        }
        return combine(cards.observeChanges(), settings.settings, everyMinute) { _, s, _ -> counts(s.currentLanguageId.takeIf { it != 0L }).total }
            .distinctUntilChanged()
    }

    /** The same fuzz for a card until it is answered, so the buttons show the intervals the answers give. */
    private fun fuzz(card: Flashcard): Double = Random(card.termId * 31 + card.schedule.reps).nextDouble()

    companion object {
        private val STEPPING = setOf(CardState.LEARNING, CardState.RELEARNING)

        /** A card being learned is shown this much early when nothing else is left, as in Anki. */
        private val LEARN_AHEAD = 20.minutes

        /** The status a review interval of [days] has earned. */
        fun statusFor(days: Int): TermStatus = when {
            days >= 90 -> TermStatus.WELL_KNOWN
            days >= 21 -> TermStatus.LEARNING_4
            days >= 7 -> TermStatus.LEARNING_3
            days >= 1 -> TermStatus.NEW_2
            else -> TermStatus.NEW_1
        }

        private fun oneLower(status: TermStatus): TermStatus = when (status) {
            TermStatus.WELL_KNOWN -> TermStatus.LEARNING_4
            TermStatus.LEARNING_4 -> TermStatus.LEARNING_3
            TermStatus.LEARNING_3 -> TermStatus.NEW_2
            else -> TermStatus.NEW_1
        }

        /** The term's status after an answer: one lower when a review was forgotten, otherwise never lower than it was. */
        fun statusAfter(current: TermStatus, before: CardSchedule, after: CardSchedule, rating: Rating): TermStatus {
            if (rating == Rating.AGAIN && before.state.isReview) return oneLower(current)
            if (!after.state.isReview) return current
            val earned = statusFor(after.intervalDays)
            return if (earned.value > current.value) earned else current
        }
    }
}
