package com.tayra.languages.core.domain.export

import kotlin.time.Instant

/**
 * A note of an Anki package: its field values in the note type's order, its deck and tags, and
 * its card's progress when it has some to carry; without, the card comes in new.
 */
data class AnkiNote(val guid: String, val deck: String, val fields: List<String>, val tags: List<String>, val card: AnkiCard? = null)

/**
 * A card's scheduling as Anki keeps it. [type] is 0 new, 1 learning, 2 review, 3 relearning;
 * [queue] the same but 1 for a step later today, 3 for one on a later day and -1 suspended. [due]
 * is a new card's position, the second a step is due today, and otherwise a day counted from
 * [AnkiDays.dayZero]. [left] holds the steps still to go; [data] the FSRS memory, as JSON.
 */
data class AnkiCard(
    val type: Int,
    val queue: Int,
    val due: Long,
    val intervalDays: Int,
    val factor: Int,
    val reps: Int,
    val lapses: Int,
    val left: Int,
    val data: String,
    val reviews: List<AnkiRevlog>,
)

/**
 * An answer of Anki's review log: [ease] 1 to 4, [interval] the one it gave (days, or minus the
 * seconds of a step), [factor] the ease, or FSRS's difficulty shifted to 100–1100, [millis] the
 * time it took, and [type] 0 learn, 1 review, 2 relearn.
 */
data class AnkiRevlog(val at: Instant, val ease: Int, val interval: Int, val lastInterval: Int, val factor: Int, val millis: Int, val type: Int)

/** When the collection's day 0 began, and the hour and UTC offset (in minutes) its days start by. */
data class AnkiDays(val dayZero: Instant, val rolloverHour: Int, val utcOffsetMinutes: Int)

/**
 * A deck preset: Anki's deck options. Steps are in minutes; [easyDays] Monday first, 1 a normal
 * day, 0.5 a reduced one, 0 the least; [newMix], [newGather] and [newSort] Anki's own values for
 * where new cards go among reviews and in what order; [answerAction] what an answer shown too long
 * becomes, also Anki's own value.
 */
data class AnkiPreset(
    val name: String,
    val newPerDay: Int,
    val reviewsPerDay: Int,
    val learnSteps: List<Double>,
    val relearnSteps: List<Double>,
    val maximumInterval: Int,
    val leechThreshold: Int,
    val leechSuspend: Boolean,
    val desiredRetention: Double,
    val parameters: List<Double>,
    val easyDays: List<Double>,
    val newMix: Int,
    val newGather: Int,
    val newSort: Int,
    val autoplay: Boolean,
    val timer: Boolean,
    val secondsToShowQuestion: Int,
    val secondsToShowAnswer: Int,
    val answerAction: Int,
) {
    /** The same in every export, so that Anki keeps one preset for the language. */
    val id: Long get() = stableAnkiId("preset|$name")
}

/** A media file of the package, referred to from a field as `[sound:name]`. */
class AnkiMedia(val name: String, val bytes: ByteArray)

/** A cloze note type: field names, the card's templates and its styling. */
data class AnkiNoteType(val name: String, val fields: List<String>, val front: String, val back: String, val css: String) {
    /**
     * The id an .apkg gives the note type, the same in every export, so that Anki keeps one note
     * type for Tayra's cards rather than adding another each time.
     */
    val id: Long get() = stableAnkiId(name)
}

/** An id made from [name], the same in every export: of a note type or a deck. */
internal fun stableAnkiId(name: String): Long = 1_600_000_000_000L + Sha1.hex(name.encodeToByteArray()).take(8).toLong(16) % 100_000_000_000L

/** What a package holds; with [days] its cards' due days count from there, with [preset] its decks use it. */
data class AnkiCollection(
    val noteType: AnkiNoteType,
    val notes: List<AnkiNote>,
    val media: List<AnkiMedia>,
    val days: AnkiDays? = null,
    val preset: AnkiPreset? = null,
)

/** Writes an Anki package (.apkg): a zip holding the collection database and its media. */
interface AnkiPackager {
    suspend fun pack(collection: AnkiCollection, now: Instant): ByteArray
}

/**
 * A running Anki that takes notes directly, through the AnkiConnect add-on on desktop, the way
 * Yomitan adds its cards.
 */
interface AnkiConnection {
    /** AnkiConnect's API version, or null when Anki is not running or has no AnkiConnect. */
    suspend fun version(): Int?

    /**
     * Adds the notes of [collection] that Anki does not have yet, creating its note type and decks
     * when they are missing and storing its media. Throws when Anki refuses the request.
     */
    suspend fun add(collection: AnkiCollection): AnkiAddResult
}

/** What Anki did with each note, by its index in the collection. */
class AnkiAddResult(val added: Set<Int>, val duplicates: Set<Int>, val failed: Map<Int, String>)
