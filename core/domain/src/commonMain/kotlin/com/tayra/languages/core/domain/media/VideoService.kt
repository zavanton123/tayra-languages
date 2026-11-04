package com.tayra.languages.core.domain.media

import com.tayra.languages.core.domain.book.SentenceBuilder
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.NewPage
import kotlin.time.Clock

class VideoImportException(message: String) : Exception(message)

/** What a video is made from: where it plays, its subtitles and, optionally, a translation of them. */
data class VideoImport(
    val languageId: Long,
    val title: String,
    val kind: VideoKind,
    val media: String,
    val subtitles: SubtitleTrack,
    val translation: List<Cue>? = null,
    val translationSource: TranslationSource? = null,
    val channel: String? = null,
    val thumbnail: String? = null,
    val durationMs: Long = 0,
    val dropSoundTags: Boolean = true,
)

/** Makes videos from subtitles and keeps their cues and transcript in step. */
class VideoService(
    private val videos: VideoRepository,
    private val languages: LanguageRepository,
    private val clock: Clock = Clock.System,
) {

    suspend fun import(request: VideoImport): Long {
        if (request.title.isBlank()) throw VideoImportException("Title is required")
        val language = languages.getById(request.languageId) ?: throw VideoImportException("No such language")
        val cues = prepare(request.subtitles, request.translation, request.dropSoundTags)
        if (cues.isEmpty()) throw VideoImportException("The subtitles have no lines")
        val video = NewVideo(
            languageId = language.id,
            title = request.title.trim(),
            kind = request.kind,
            media = request.media,
            channel = request.channel?.trim()?.ifEmpty { null },
            thumbnail = request.thumbnail,
            durationMs = request.durationMs,
            subtitleKind = request.subtitles.kind,
            createdAt = clock.now(),
        )
        return videos.insert(video, transcript(cues, language, request.translationSource))
    }

    /** Puts new subtitles (and their translation) in place of a video's. */
    suspend fun replaceSubtitles(bookId: Long, subtitles: SubtitleTrack, translation: List<Cue>? = null, translationSource: TranslationSource? = null, dropSoundTags: Boolean = true) {
        val video = videos.video(bookId) ?: throw VideoImportException("No such video")
        val language = languages.getById(video.languageId) ?: throw VideoImportException("No such language")
        val cues = prepare(subtitles, translation, dropSoundTags)
        if (cues.isEmpty()) throw VideoImportException("The subtitles have no lines")
        videos.replaceTranscript(bookId, transcript(cues, language, translationSource), subtitles.kind)
    }

    /** Gives the lines of a video their translations from a translation track, matched by time. */
    suspend fun attachTranslation(bookId: Long, translation: List<Cue>, source: TranslationSource) {
        val stored = videos.cues(bookId)
        val aligned = CueAlignment.align(stored.map { it.cue.copy(translation = null) }, CueNormalizer.normalize(translation))
        videos.setTranslations(bookId, stored.indices.associate { stored[it].order to aligned[it].translation }, source)
    }

    suspend fun removeTranslations(bookId: Long) {
        val stored = videos.cues(bookId)
        videos.setTranslations(bookId, stored.associate { it.order to null }, null)
    }

    /** Moves every line of a video [deltaMs] later (earlier when negative), for subtitles out of step with the sound. */
    suspend fun shift(bookId: Long, deltaMs: Long) = videos.shiftCues(bookId, deltaMs)

    companion object {
        /** Lines on a page of a video's transcript; pages only bound statistics and sync, the transcript scrolls whole. */
        const val LINES_PER_PAGE = 150

        /**
         * Clean cues from a track: markup and repeats gone, automatic captions joined into
         * sentence-sized lines, and the translation (if any) matched to them by time.
         */
        fun prepare(subtitles: SubtitleTrack, translation: List<Cue>?, dropSoundTags: Boolean = true): List<Cue> {
            var cues = CueNormalizer.normalize(subtitles.cues, dropSoundTags)
            if (subtitles.kind == SubtitleKind.YOUTUBE_AUTO || CueNormalizer.looksUnpunctuated(cues)) {
                cues = CueNormalizer.mergeFragments(cues)
            }
            if (!translation.isNullOrEmpty()) {
                cues = CueAlignment.align(cues, CueNormalizer.normalize(translation, dropSoundTags))
            }
            return cues
        }

        /** The transcript of [cues]: a line per cue, [LINES_PER_PAGE] lines a page. */
        fun transcript(cues: List<Cue>, language: Language, translationSource: TranslationSource?): VideoTranscript {
            val pages = mutableListOf<NewPage>()
            val stored = mutableListOf<StoredCue>()
            cues.chunked(LINES_PER_PAGE).forEachIndexed { pageIndex, chunk ->
                val text = chunk.joinToString("\n") { it.text }
                pages += NewPage(text, SentenceBuilder.wordCount(text, language))
                chunk.forEachIndexed { line, cue ->
                    stored += StoredCue(
                        order = stored.size,
                        pageOrder = pageIndex + 1,
                        lineIndex = line,
                        cue = cue,
                        translationSource = translationSource.takeIf { cue.translation != null },
                    )
                }
            }
            return VideoTranscript(pages, stored)
        }
    }
}
