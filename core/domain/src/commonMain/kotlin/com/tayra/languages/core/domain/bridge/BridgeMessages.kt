package com.tayra.languages.core.domain.bridge

import kotlinx.serialization.Serializable

/** The version of the calls the browser extension makes; it goes up when one changes in a way old extensions can't follow. */
const val BRIDGE_API = 1

/** What the app answers anyone who asks, so the extension knows it found Tayra. */
@Serializable
data class BridgeHello(val app: String = "tayra", val api: Int = BRIDGE_API)

@Serializable
data class PairRequest(val code: String, val browser: String = "")

@Serializable
data class PairResponse(val token: String)

@Serializable
data class BridgeLanguage(val id: Long, val name: String, val code: String?)

/** The language being learned, the native one, and the app's version. */
@Serializable
data class BridgeStatus(val language: BridgeLanguage?, val native: String, val version: String?)

/** A YouTube video's subtitle track, and the track YouTube translated it into, both as YouTube sends them (json3). */
@Serializable
data class TranscriptRequest(
    val videoId: String,
    val trackLanguage: String,
    val automatic: Boolean,
    val original: String,
    val translation: String? = null,
)

@Serializable
data class TranscriptResponse(
    /** Whether the track is in the language being learned; when not, the lines come without words to look up. */
    val languageMatches: Boolean,
    val language: BridgeLanguage?,
    val lines: List<BridgeLine>,
    /** Each word's place in the language's frequency list, 1 for the most common, by what its status is kept under; words the list lacks are left out. */
    val ranks: Map<String, Int> = emptyMap(),
)

/** A line of the cleaned subtitles, cut into what the reader shows: words and phrases, and the text between them. */
@Serializable
data class BridgeLine(
    val beginMs: Long,
    val endMs: Long,
    val text: String,
    val translation: String? = null,
    val segments: List<BridgeSegment> = emptyList(),
)

/**
 * A piece of a line. A word or saved phrase has [term], the text to look it up and save it by,
 * [lc], what its status is kept under, and its [status]; the text between words has neither.
 */
@Serializable
data class BridgeSegment(val text: String, val term: String? = null, val lc: String? = null, val status: Int? = null)

@Serializable
data class LookupResponse(
    val term: String,
    val lc: String,
    val status: Int,
    val translation: String,
    val parents: List<String>,
    val entries: List<BridgeEntry>,
)

@Serializable
data class BridgeEntry(
    val word: String,
    val pos: String,
    val ipa: String? = null,
    val reading: String? = null,
    /** Each sense's glosses. */
    val senses: List<List<String>>,
    /** The imported dictionary the entry comes from; null for the app's own pack. */
    val dictionary: String? = null,
)

/** Saves a word with [status]; [sentence] is the subtitle line it was met in. */
@Serializable
data class SaveTermRequest(val term: String, val status: Int, val translation: String? = null, val sentence: String? = null)

@Serializable
data class SaveTermResponse(val lc: String, val status: Int)

@Serializable
data class TranslateRequest(val lines: List<String>)

@Serializable
data class TranslateResponse(val translations: List<String?>)

/**
 * The statuses that changed after the extension's last [seq], by [BridgeSegment.lc]. [reset] says
 * the changes asked for are no longer kept, so the extension should ask for its lines again.
 */
@Serializable
data class ChangesResponse(val seq: Long, val statuses: Map<String, Int> = emptyMap(), val reset: Boolean = false)

/** A YouTube video watched with subtitles in the language being learned, to keep in the Videos library. */
@Serializable
data class AddVideoRequest(
    val videoId: String,
    val title: String,
    val channel: String? = null,
    val durationMs: Long = 0,
    val trackLanguage: String,
    val automatic: Boolean,
    /** The subtitles and their translation as YouTube sends them (json3). */
    val original: String,
    val translation: String? = null,
)

/** The video's book in the library; [added] is false when it was there already. */
@Serializable
data class AddVideoResponse(val bookId: Long, val added: Boolean)

/** Bookmarks the line of video [bookId] playing at [atMs]. */
@Serializable
data class BookmarkRequest(val bookId: Long, val atMs: Long)

/** The bookmark, titled with its line's time and text; [added] is false when the line had one already. */
@Serializable
data class BookmarkResponse(val id: Long, val title: String, val added: Boolean)

/** Opens video [bookId] in the app at [positionMs]. */
@Serializable
data class OpenVideoRequest(val bookId: Long, val positionMs: Long)

@Serializable
data class BridgeError(val error: String)
