package com.tayra.languages.core.domain.export

import kotlin.time.Instant

/** A note of an Anki package: its field values in the note type's order, its deck and tags. */
data class AnkiNote(val guid: String, val deck: String, val fields: List<String>, val tags: List<String>)

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

data class AnkiCollection(val noteType: AnkiNoteType, val notes: List<AnkiNote>, val media: List<AnkiMedia>)

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
