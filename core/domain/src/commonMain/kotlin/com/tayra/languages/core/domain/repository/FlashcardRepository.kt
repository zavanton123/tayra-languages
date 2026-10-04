package com.tayra.languages.core.domain.repository

import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.Flashcard
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface FlashcardRepository {
    /**
     * Brings the cards in line with the terms' statuses: every term at status 1 to 4 has a card;
     * a term marked known, ignored or unknown outside the flashcards loses its card; a term the
     * flashcards made known and that was moved back to learning starts over. Writes only when
     * something is out of line.
     */
    suspend fun reconcile(now: Instant)

    suspend fun get(termId: Long): Flashcard?

    /** The card in [states] due soonest, by [dueBy] at the latest, among those not suspended. */
    suspend fun next(languageId: Long?, states: Set<CardState>, dueBy: Instant): Flashcard?
    suspend fun count(languageId: Long?, states: Set<CardState>, dueBy: Instant): Int
    suspend fun earliestDue(languageId: Long?, states: Set<CardState>): Instant?

    /** Answers given since [since] to cards that were in [stateBefore]. */
    suspend fun reviewsSince(languageId: Long?, stateBefore: CardState, since: Instant): Int

    suspend fun save(card: Flashcard)
    suspend fun addReview(review: FlashcardReview): Long
    suspend fun deleteReview(id: Long)
    suspend fun setSuspended(termId: Long, suspended: Boolean)

    /** Makes the card new again, at the end of the new cards. */
    suspend fun restart(termId: Long, now: Instant)

    /** Emits whenever terms, cards or reviews change. */
    fun observeChanges(): Flow<Unit>
}
