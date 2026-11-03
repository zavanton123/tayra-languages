package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.model.TermStatus
import kotlin.time.Duration
import kotlin.time.Instant

/** A term's flashcard. */
data class Flashcard(
    val termId: Long,
    val schedule: CardSchedule,
    /** A suspended card is kept but never shown. */
    val suspended: Boolean = false,
    /** The term's status as the card last noted it. */
    val seenStatus: TermStatus,
    /** When the word became one being learned and the card was made; null for a card not yet saved. */
    val createdAt: Instant? = null,
)

/** How a card is answered: its answer shown, or the word typed or said first. */
enum class AnswerMode(val value: Int) {
    SHOW(0),
    TYPE(1),
    SPEAK(2);

    companion object {
        fun of(value: Int): AnswerMode = entries.firstOrNull { it.value == value } ?: SHOW
    }
}

/** The order of the reviews due when there are far more of them than usual. */
enum class BacklogOrder {
    /**
     * The cards slipping furthest past where they were meant to be reviewed first, measured against
     * their memory; with one desired retention, the cards least likely to be recalled now first.
     */
    RELATIVE_OVERDUENESS,

    /** The cards still most likely to be recalled first, to keep what can still be kept. */
    RECALL_HIGH_FIRST,

    /** The cards of words ahead in the text read last first, the rest by relative overdueness. */
    READING_FIRST,

    /** The order of every other day: the cards due longest first. */
    DUE_DATE,
}

/** The order new cards are learned in. */
enum class NewCardOrder {
    /** As the words were saved, the first saved first. */
    SAVED,

    /** The word saved last first, while its text is fresh. */
    NEWEST,

    /** In no order, shuffled again each study day, so words saved together come apart. */
    RANDOM,
}

/** Where a day's new cards come among its reviews. */
enum class NewCardMix {
    /** Spread evenly among the reviews, as Anki spreads them by default. */
    MIXED,
    AFTER_REVIEWS,
    BEFORE_REVIEWS,
}

/** What is done with a card left with its answer showing, as Anki's auto-advance does. */
enum class AutoAnswer {
    AGAIN,
    HARD,
    GOOD,

    /** Skipped until tomorrow. */
    SKIP,
}

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
    /** Whether the word was typed or said before the answer was shown. */
    val answeredBy: AnswerMode = AnswerMode.SHOW,
    /** How long the answer took, from the card shown to its rating; null for answers from before it was kept. */
    val answerTime: Duration? = null,
)

/** The cards waiting today. */
data class DueCounts(val new: Int = 0, val learning: Int = 0, val review: Int = 0) {
    val total: Int get() = new + learning + review
}

/**
 * A language's FSRS parameters trained on its own answers; null [parameters] while the defaults fit
 * them best. [reviews] counts the answers the last training read, at [trainedAt]; [gain] is how
 * much better than the defaults the parameters predict them, a share of the defaults' log loss.
 */
data class PersonalParameters(val languageId: Long, val parameters: List<Double>?, val reviews: Int, val trainedAt: Instant, val gain: Double)
