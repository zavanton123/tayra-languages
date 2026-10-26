package com.tayra.languages.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * One record as it travels between a device and the account: what it is ([kind]), its [key] there,
 * the same on every device, when it last changed ([updatedAt], epoch milliseconds), whether it was
 * deleted, and its fields ([data], absent for a deletion). [deviceId] is the device that made the
 * change, which also breaks ties between changes made in the same millisecond.
 */
@Serializable
data class SyncRecord(
    val kind: String,
    val key: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val data: JsonObject? = null,
    val deviceId: String = "",
)

/** A device's changes since it last sent them. */
@Serializable
data class PushRequest(val deviceId: String, val records: List<SyncRecord>)

/** The account's change number after the push, and the server's clock, which the device compares its own with. */
@Serializable
data class PushResponse(val seq: Long, val serverTime: Long)

/** The account's records changed after a device's last change number, oldest first; [more] when a further page follows. */
@Serializable
data class PullResponse(val records: List<SyncRecord>, val seq: Long, val more: Boolean)

/** The kinds of record, as they are named on the wire. */
object SyncKinds {
    const val TERM = "term"
    const val FLASHCARD = "flashcard"
    const val REVIEW = "review"
    const val PRACTICE_PROGRESS = "practice_progress"
    const val PRACTICE_EXAMPLE = "practice_example"
    const val LANGUAGE = "language"
    const val VOCABULARY_LEVEL = "vocabulary_level"
    const val BOOK = "book"
    const val PAGE = "page"
    const val BOOKMARK = "bookmark"
    const val WORDS_READ = "words_read"
    const val PLAYLIST = "playlist"
    const val SETTING = "setting"

    /** Kinds whose records are only ever added: two devices never disagree on one, so they are never merged. */
    val appendOnly = setOf(REVIEW, WORDS_READ)
}

/**
 * The keys records go by on the wire. A language is named by its ISO code, never by a device's own
 * id for it; a word by its language and lower-case text, so the same word saved on two devices is one
 * record; a text by the random id it was given where it was made.
 */
object SyncKeys {
    private const val SEPARATOR = '/'

    fun term(languageCode: String, textLc: String): String = "$languageCode$SEPARATOR$textLc"

    /** The language code and the lower-case text of a word's [key]; codes have no separator, texts may. */
    fun termParts(key: String): Pair<String, String>? {
        val at = key.indexOf(SEPARATOR)
        return if (at <= 0) null else key.substring(0, at) to key.substring(at + 1)
    }

    /** A record that belongs to one word: its card, its practice progress. */
    fun ofTerm(languageCode: String, textLc: String): String = term(languageCode, textLc)

    /** One review of a word's card, at [reviewedAt]. */
    fun review(languageCode: String, textLc: String, reviewedAt: Long): String = "$reviewedAt$SEPARATOR${term(languageCode, textLc)}"

    /** An example sentence kept for a word. */
    fun practiceExample(languageCode: String, textLc: String, sentence: String): String = "${term(languageCode, textLc)}\n$sentence"

    fun language(languageCode: String): String = languageCode

    fun page(bookSyncId: String, pageOrder: Long): String = "$bookSyncId$SEPARATOR$pageOrder"

    fun bookmark(bookSyncId: String, pageOrder: Long, made: String): String = "${page(bookSyncId, pageOrder)}$SEPARATOR$made"

    fun setting(name: String): String = name
}

/** How records are written as JSON on the wire: fields a device does not know yet are passed over, not refused. */
val SyncJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
