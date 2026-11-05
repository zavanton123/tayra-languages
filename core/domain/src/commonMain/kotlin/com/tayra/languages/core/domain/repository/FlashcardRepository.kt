package com.tayra.languages.core.domain.repository

import com.tayra.languages.core.domain.flashcards.MemoryState
import com.tayra.languages.core.domain.flashcards.PersonalParameters
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.Flashcard
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface FlashcardRepository {
    /**
     * Brings the cards in line with the terms: every term at status 1 to 4 with a sentence has a card, which then
     * stays whatever status is set by hand and goes only with its term. Writes only when something
     * is out of line.
     */
    suspend fun reconcile(now: Instant)

    /** The terms whose cards have no sentence to ask their word in. */
    suspend fun wordOnly(): Set<Long>

    suspend fun get(termId: Long): Flashcard?

    /** Every card of [languageId] (null for every language), suspended ones included. */
    suspend fun list(languageId: Long?): List<Flashcard>

    /** The answers given to the term's card, the latest first. */
    suspend fun reviews(termId: Long): List<FlashcardReview>

    /** The card in [states] due soonest, by [dueBy] at the latest, among those not suspended; the terms in [except] are left out. */
    suspend fun next(languageId: Long?, states: Set<CardState>, dueBy: Instant, except: Set<Long> = emptySet()): Flashcard?
    suspend fun count(languageId: Long?, states: Set<CardState>, dueBy: Instant, except: Set<Long> = emptySet()): Int

    /** When the cards in [states] not suspended fall due, those due from [from] until [until]. */
    suspend fun dueTimes(languageId: Long?, states: Set<CardState>, from: Instant, until: Instant): List<Instant>

    /** Every card in [states] due by [dueBy], the soonest first, among those not suspended. */
    suspend fun dueInStates(languageId: Long?, states: Set<CardState>, dueBy: Instant, except: Set<Long> = emptySet()): List<Flashcard>
    suspend fun earliestDue(languageId: Long?, states: Set<CardState>, except: Set<Long> = emptySet()): Instant?

    /** The terms of [languageId] whose cards were answered Again since [since]. */
    suspend fun forgottenSince(languageId: Long?, since: Instant): Set<Long>

    /** Answers given since [since] to cards that were in [stateBefore]. */
    suspend fun reviewsSince(languageId: Long?, stateBefore: CardState, since: Instant): Int

    suspend fun save(card: Flashcard)
    suspend fun addReview(review: FlashcardReview): Long

    /** Adds [reviews] together, as one write. */
    suspend fun addReviews(reviews: List<FlashcardReview>)
    suspend fun deleteReview(id: Long)
    suspend fun setSuspended(termId: Long, suspended: Boolean)

    /** Makes the card new again, at the end of the new cards. */
    suspend fun restart(termId: Long, now: Instant)

    /** The FSRS parameters trained on [languageId]'s answers, once it has been trained. */
    suspend fun parameters(languageId: Long): PersonalParameters?
    suspend fun saveParameters(parameters: PersonalParameters)

    /** Goes back to the default parameters for [languageId]. */
    suspend fun clearParameters(languageId: Long)

    /** Every answer to [languageId]'s cards, card by card in the order given. */
    suspend fun reviewsOf(languageId: Long): List<FlashcardReview>
    suspend fun reviewCount(languageId: Long): Int

    /** Sets the memory of cards, as worked out again from their answers, in one write. */
    suspend fun setMemories(memories: Map<Long, MemoryState>)

    /** Emits whenever terms, cards or reviews change. */
    fun observeChanges(): Flow<Unit>
}
