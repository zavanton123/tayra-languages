package com.tayra.languages.core.domain.bridge

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.media.SubtitleFormat
import com.tayra.languages.core.domain.media.SubtitleFormats
import com.tayra.languages.core.domain.media.SubtitleKind
import com.tayra.languages.core.domain.media.SubtitleTrack
import com.tayra.languages.core.domain.media.TranslationSource
import com.tayra.languages.core.domain.media.VideoBookmarks
import com.tayra.languages.core.domain.media.VideoImport
import com.tayra.languages.core.domain.media.VideoImportException
import com.tayra.languages.core.domain.media.VideoKind
import com.tayra.languages.core.domain.media.VideoOpenRequests
import com.tayra.languages.core.domain.media.VideoRepository
import com.tayra.languages.core.domain.media.VideoService
import com.tayra.languages.core.domain.media.YouTubeIds
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * The Videos library as the browser extension uses it: the YouTube videos watched with it are
 * kept, a line can be bookmarked, and a video opened in the app where it was left.
 */
class BridgeVideos(
    private val settings: SettingsRepository,
    private val languages: LanguageRepository,
    private val videos: VideoRepository,
    private val service: VideoService,
    private val books: BookRepository,
    private val opens: VideoOpenRequests,
) {
    suspend fun add(request: AddVideoRequest): AddVideoResponse {
        val language = settings.current.currentLanguageId.takeIf { it != 0L }?.let { languages.getById(it) }
            ?: throw BridgeException("No language is being learned")
        if (YouTubeIds.ofUri(YouTubeIds.uri(request.videoId)) != request.videoId) throw BridgeException("Not a YouTube video")
        val code = LanguageCodes.codeFor(language.name)
        if (code == null || !code.equals(request.trackLanguage.substringBefore('-'), ignoreCase = true)) {
            throw BridgeException("The subtitles are not in ${language.name}")
        }
        videos.findByMedia(VideoKind.YOUTUBE, request.videoId, language.id)?.let { return AddVideoResponse(it, added = false) }
        val cues = parse(request.original)
        val translation = request.translation?.let(::parse)?.takeIf { it.isNotEmpty() }
        val bookId = try {
            service.import(
                VideoImport(
                    languageId = language.id,
                    title = request.title.ifBlank { "YouTube ${request.videoId}" },
                    kind = VideoKind.YOUTUBE,
                    media = request.videoId,
                    subtitles = SubtitleTrack(cues, if (request.automatic) SubtitleKind.YOUTUBE_AUTO else SubtitleKind.YOUTUBE, request.trackLanguage),
                    translation = translation,
                    translationSource = translation?.let { TranslationSource.YOUTUBE },
                    channel = request.channel,
                    thumbnail = YouTubeIds.thumbnailUrl(request.videoId),
                    durationMs = request.durationMs.coerceAtLeast(0),
                ),
            )
        } catch (e: VideoImportException) {
            throw BridgeException(e.message ?: "The video could not be added")
        }
        return AddVideoResponse(bookId, added = true)
    }

    suspend fun bookmark(request: BookmarkRequest): BookmarkResponse {
        if (videos.video(request.bookId) == null) throw BridgeException("No such video")
        val cues = videos.cues(request.bookId).sortedBy { it.cue.beginMs }
        val stored = cues.lastOrNull { it.cue.beginMs <= request.atMs } ?: cues.firstOrNull() ?: throw BridgeException("The video has no lines")
        val page = books.getPage(request.bookId, stored.pageOrder) ?: throw BridgeException("The video has no lines")
        val title = VideoBookmarks.title(stored.cue)
        books.observeBookmarks(request.bookId).first().firstOrNull { it.pageId == page.id && it.title == title }?.let {
            return BookmarkResponse(it.id, title, added = false)
        }
        return BookmarkResponse(books.addBookmark(page.id, title), title, added = true)
    }

    suspend fun open(request: OpenVideoRequest) {
        if (videos.video(request.bookId) == null) throw BridgeException("No such video")
        videos.setPosition(request.bookId, request.positionMs.coerceAtLeast(0), final = false)
        opens.open(request.bookId)
    }

    private fun parse(json3: String) = runCatching { SubtitleFormats.parse(json3, SubtitleFormat.YOUTUBE_JSON) }.getOrDefault(emptyList())
}
