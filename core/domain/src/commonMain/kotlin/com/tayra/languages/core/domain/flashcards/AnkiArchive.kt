package com.tayra.languages.core.domain.flashcards

import kotlin.time.Instant

/**
 * What an Anki package holds, as Anki keeps it: its notes by field name, each with its cards'
 * scheduling and answers. [dayZero] is when the collection's day 0 began; review cards fall due
 * on days counted from it.
 */
class AnkiArchive(val dayZero: Instant, val notes: List<AnkiArchiveNote>)

/** A note: [fields] by name, as its note type names them, and its cards. */
class AnkiArchiveNote(val id: Long, val fields: Map<String, String>, val tags: List<String>, val cards: List<AnkiArchiveCard>)

/**
 * A card as Anki schedules it. [type] is 0 new, 1 learning, 2 review, 3 relearning; [queue] -1 is
 * suspended. [due] is a position for a new card, seconds since 1970 for a card learning today, and
 * otherwise a day from [AnkiArchive.dayZero]; a card in a filtered deck has its own due in
 * [originalDue]. [left] holds the learning steps still to go in its last three digits.
 */
class AnkiArchiveCard(
    val deck: String,
    val type: Int,
    val queue: Int,
    val due: Long,
    val originalDue: Long?,
    val intervalDays: Int,
    val reps: Int,
    val lapses: Int,
    val left: Int,
    /** FSRS's memory state, when Anki keeps one. */
    val stability: Double?,
    val difficulty: Double?,
    val lastReview: Instant?,
    val reviews: List<AnkiArchiveReview>,
)

/** An answer from Anki's review log. [ease] 1 to 4 is the rating, 0 a reschedule by hand; [type] 0 learn, 1 review, 2 relearn, 3 filtered, 4 manual, 5 rescheduled. */
class AnkiArchiveReview(val at: Instant, val ease: Int, val intervalDays: Int, val type: Int)

/** Reads an Anki package (.apkg or .colpkg). */
interface AnkiArchiveReader {
    /** Whether packages can be read here. */
    val available: Boolean

    /** The package's notes; throws [AnkiImportException] when [bytes] are not one Anki can have made. */
    suspend fun read(bytes: ByteArray): AnkiArchive
}

class AnkiImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
