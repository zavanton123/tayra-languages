package com.tayra.languages.core.domain.export

import kotlin.time.Instant

/** A note of an Anki package: its field values in the note type's order, its deck and tags. */
data class AnkiNote(val guid: String, val deck: String, val fields: List<String>, val tags: List<String>)

/** A media file of the package, referred to from a field as `[sound:name]`. */
class AnkiMedia(val name: String, val bytes: ByteArray)

/** A cloze note type: field names, the card's templates and its styling. */
data class AnkiNoteType(val name: String, val fields: List<String>, val front: String, val back: String, val css: String)

data class AnkiCollection(val noteType: AnkiNoteType, val notes: List<AnkiNote>, val media: List<AnkiMedia>)

/** Writes an Anki package (.apkg): a zip holding the collection database and its media. */
interface AnkiPackager {
    suspend fun pack(collection: AnkiCollection, now: Instant): ByteArray
}
