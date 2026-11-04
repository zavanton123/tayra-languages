package com.tayra.languages.core.domain.media

/**
 * One subtitle line of a video: shown from [beginMs] to [endMs]. [translation] is the matching line of
 * a translation track, when one was given.
 */
data class Cue(
    val beginMs: Long,
    val endMs: Long,
    val text: String,
    val translation: String? = null,
) {
    val durationMs: Long get() = (endMs - beginMs).coerceAtLeast(0)

    /** How long this cue and [other] are on screen together. */
    fun overlapMs(other: Cue): Long = (minOf(endMs, other.endMs) - maxOf(beginMs, other.beginMs)).coerceAtLeast(0)
}

/** Where a video's subtitles came from. */
enum class SubtitleKind {
    /** A subtitle file the user chose. */
    FILE,

    /** Subtitles a person made for a YouTube video. */
    YOUTUBE,

    /** YouTube's automatic speech recognition. */
    YOUTUBE_AUTO,

    /** Transcribed on this device. */
    TRANSCRIBED,
}

/** Where the translations of a video's lines came from. */
enum class TranslationSource {
    FILE,
    YOUTUBE,
}

/** A whole subtitle track, in the order of time. */
data class SubtitleTrack(
    val cues: List<Cue>,
    val kind: SubtitleKind = SubtitleKind.FILE,
    val languageCode: String? = null,
)
