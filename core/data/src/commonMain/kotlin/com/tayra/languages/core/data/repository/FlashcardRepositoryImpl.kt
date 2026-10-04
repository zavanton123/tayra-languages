package com.tayra.languages.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.Flashcards
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.flashcards.CardSchedule
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.Flashcard
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.domain.flashcards.MemoryState
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class FlashcardRepositoryImpl(private val provider: DatabaseProvider) : FlashcardRepository {

    private suspend fun queries() = provider.database().flashcardsQueries

    override suspend fun reconcile(now: Instant) = withContext(databaseDispatcher) {
        val database = provider.database()
        val q = database.flashcardsQueries
        if (q.outOfLine().awaitAsOne() == 0L) return@withContext
        database.transaction {
            q.deleteOrphanCards()
            q.deleteOrphanReviews()
            q.deleteRetired()
            q.restartReturned(now.toEpochMillis())
            q.followStatus()
            q.createMissing()
        }
    }

    override suspend fun get(termId: Long): Flashcard? = withContext(databaseDispatcher) {
        queries().selectByTerm(termId).awaitAsOneOrNull()?.toFlashcard()
    }

    override suspend fun next(languageId: Long?, states: Set<CardState>, dueBy: Instant): Flashcard? = withContext(databaseDispatcher) {
        queries().nextInStates(states.map { it.value.toLong() }, dueBy.toEpochMillis(), languageId).awaitAsOneOrNull()?.toFlashcard()
    }

    override suspend fun count(languageId: Long?, states: Set<CardState>, dueBy: Instant): Int = withContext(databaseDispatcher) {
        queries().countInStates(states.map { it.value.toLong() }, dueBy.toEpochMillis(), languageId).awaitAsOne().toInt()
    }

    override suspend fun earliestDue(languageId: Long?, states: Set<CardState>): Instant? = withContext(databaseDispatcher) {
        queries().earliestInStates(states.map { it.value.toLong() }, languageId).awaitAsOneOrNull()?.min?.toInstant()
    }

    override suspend fun reviewsSince(languageId: Long?, stateBefore: CardState, since: Instant): Int = withContext(databaseDispatcher) {
        queries().countReviewsSince(since.toEpochMillis(), stateBefore.value.toLong(), languageId).awaitAsOne().toInt()
    }

    override suspend fun save(card: Flashcard) {
        withContext(databaseDispatcher) {
            val s = card.schedule
            queries().save(
                termId = card.termId,
                state = s.state.value.toLong(),
                due = s.due.toEpochMillis(),
                stability = s.memory?.stability,
                difficulty = s.memory?.difficulty,
                intervalDays = s.intervalDays.toLong(),
                remainingSteps = s.remainingSteps.toLong(),
                reps = s.reps.toLong(),
                lapses = s.lapses.toLong(),
                lastReview = s.lastReview?.toEpochMillis(),
                suspended = card.suspended,
                seenStatus = card.seenStatus.value.toLong(),
            )
        }
    }

    override suspend fun addReview(review: FlashcardReview): Long = withContext(databaseDispatcher) {
        val database = provider.database()
        database.transactionWithResult {
            database.flashcardsQueries.insertReview(
                termId = review.termId,
                reviewedAt = review.at.toEpochMillis(),
                rating = review.rating.value.toLong(),
                stateBefore = review.stateBefore.value.toLong(),
                intervalDays = review.intervalDays.toLong(),
                elapsedDays = review.elapsedDays.toLong(),
            )
            database.flashcardsQueries.lastReviewId().awaitAsOne()
        }
    }

    override suspend fun deleteReview(id: Long) {
        withContext(databaseDispatcher) { queries().deleteReview(id) }
    }

    override suspend fun setSuspended(termId: Long, suspended: Boolean) {
        withContext(databaseDispatcher) { queries().setSuspended(suspended, termId) }
    }

    override suspend fun restart(termId: Long, now: Instant) {
        withContext(databaseDispatcher) { queries().restart(now.toEpochMillis(), termId) }
    }

    override fun observeChanges(): Flow<Unit> = flow {
        queries().changeMarker().asFlow().collect { emit(Unit) }
    }

    private fun Flashcards.toFlashcard() = Flashcard(
        termId = term_id,
        schedule = CardSchedule(
            state = CardState.of(state.toInt()),
            due = due.toInstant(),
            memory = stability?.let { s -> difficulty?.let { d -> MemoryState(s, d) } },
            intervalDays = interval_days.toInt(),
            remainingSteps = remaining_steps.toInt(),
            reps = reps.toInt(),
            lapses = lapses.toInt(),
            lastReview = last_review?.toInstant(),
        ),
        suspended = suspended,
        seenStatus = TermStatus.entries.firstOrNull { it.value == seen_status.toInt() } ?: TermStatus.WELL_KNOWN,
    )
}
