package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.model.TermStatus
import kotlin.time.Instant

/** A term's flashcard. */
data class Flashcard(
    val termId: Long,
    val schedule: CardSchedule,
    /** A suspended card is kept but never shown. */
    val suspended: Boolean = false,
    /** The term's status when the card last saw it; a different status now is a change made elsewhere. */
    val seenStatus: TermStatus,
)

/** One answer, kept as history. */
data class FlashcardReview(
    val id: Long = 0,
    val termId: Long,
    val at: Instant,
    val rating: Rating,
    val stateBefore: CardState,
    /** The interval the answer gave the card, in days; 0 while it is on steps. */
    val intervalDays: Int,
    val elapsedDays: Int,
)

/** The cards waiting today. */
data class DueCounts(val new: Int = 0, val learning: Int = 0, val review: Int = 0) {
    val total: Int get() = new + learning + review
}
