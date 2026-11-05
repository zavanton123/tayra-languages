package com.tayra.languages.core.domain.media

import com.tayra.languages.core.domain.repository.NewPage
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

enum class VideoKind {
    /** A video file or stream on the internet (MP4, WebM, HLS…), by its address. */
    LINK,

    /** A YouTube video, played in YouTube's own player. */
    YOUTUBE,
}

/** A video in the library. Its transcript is the book [bookId]. */
data class Video(
    val bookId: Long,
    val languageId: Long,
    val title: String,
    val kind: VideoKind,
    /** The video's internet address, or the YouTube video id. */
    val media: String,
    val channel: String? = null,
    val thumbnail: String? = null,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val subtitleKind: SubtitleKind = SubtitleKind.FILE,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0),
) {
    val youtubeId: String? get() = media.takeIf { kind == VideoKind.YOUTUBE }
}

data class VideoListItem(
    val video: Video,
    val languageName: String,
    val lastOpened: Instant?,
    val wordCount: Int,
    val distinctTerms: Int?,
    val distinctUnknowns: Int?,
    val unknownPercent: Int?,
) {
    /** The share of the video's words already known or learning, once its statistics are counted. */
    val knownPercent: Int? get() = unknownPercent?.let { 100 - it }
}

/** A subtitle line as stored: its place in the video and on its line of the transcript. */
data class StoredCue(
    val order: Int,
    val pageOrder: Int,
    val lineIndex: Int,
    val cue: Cue,
    val translationSource: TranslationSource? = null,
)

/** The transcript of a video: its pages of text and the cues placed on their lines. */
data class VideoTranscript(val pages: List<NewPage>, val cues: List<StoredCue>)

data class NewVideo(
    val languageId: Long,
    val title: String,
    val kind: VideoKind,
    val media: String,
    val channel: String? = null,
    val thumbnail: String? = null,
    val durationMs: Long = 0,
    val subtitleKind: SubtitleKind,
    val createdAt: Instant,
)

interface VideoRepository {
    fun observeVideos(): Flow<List<VideoListItem>>
    suspend fun video(bookId: Long): Video?
    suspend fun isVideo(bookId: Long): Boolean
    suspend fun findByMedia(kind: VideoKind, media: String, languageId: Long): Long?
    suspend fun cues(bookId: Long): List<StoredCue>
    suspend fun insert(video: NewVideo, transcript: VideoTranscript): Long

    /** Puts [transcript] in place of the video's text and cues. */
    suspend fun replaceTranscript(bookId: Long, transcript: VideoTranscript, subtitleKind: SubtitleKind)

    /** Sets the translations of the cues by their order; a null translation removes one. */
    suspend fun setTranslations(bookId: Long, translations: Map<Int, String?>, source: TranslationSource?)

    suspend fun shiftCues(bookId: Long, deltaMs: Long)

    /** Keeps where watching got to; with [final] (on leaving the video) it also goes to other devices. */
    suspend fun setPosition(bookId: Long, positionMs: Long, final: Boolean)
    suspend fun setDuration(bookId: Long, durationMs: Long)
    suspend fun setMedia(bookId: Long, media: String)
}
