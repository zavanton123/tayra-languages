package com.tayra.languages.feature.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.data.media.SubtitleFiles
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.SubtitleKind
import com.tayra.languages.core.domain.media.SubtitleTrack
import com.tayra.languages.core.domain.media.TranslationSource
import com.tayra.languages.core.domain.media.VideoImport
import com.tayra.languages.core.domain.media.VideoImportException
import com.tayra.languages.core.domain.media.VideoKind
import com.tayra.languages.core.domain.media.VideoRepository
import com.tayra.languages.core.domain.media.VideoService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.media.SubtitleFormat
import com.tayra.languages.core.domain.media.SubtitleFormats
import com.tayra.languages.core.domain.media.YouTubeIds
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.ui.media.VideoController
import com.tayra.languages.core.ui.media.YouTubeCaptions
import com.tayra.languages.core.ui.media.YouTubeDetails
import com.tayra.languages.core.ui.media.YouTubeTrack
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A subtitle file read for a new video. */
data class PickedSubtitles(val name: String, val cues: List<Cue>) {
    val lastMs: Long get() = cues.maxOfOrNull { it.endMs } ?: 0
}

/** Where a new video comes from. */
enum class VideoOrigin { FILE, YOUTUBE }

/** A YouTube video being added: its link, what its player says about it, and the subtitles chosen. */
data class YouTubePick(
    val input: String = "",
    val id: String? = null,
    /** Asking the player for the video's details. */
    val loading: Boolean = false,
    val details: YouTubeDetails? = null,
    val track: YouTubeTrack? = null,
    val translate: Boolean = true,
    /** The player here can't hand over subtitles (no YouTube support on this device yet). */
    val unsupported: Boolean = false,
)

data class AddVideoUiState(
    val origin: VideoOrigin = VideoOrigin.FILE,
    val youtube: YouTubePick = YouTubePick(),
    val languageCode: String? = null,
    val nativeCode: String = "en",
    val mediaName: String? = null,
    val mediaAddress: String? = null,
    val title: String = "",
    val subtitles: PickedSubtitles? = null,
    val translation: PickedSubtitles? = null,
    /** Subtitle files found beside the video, to choose from. */
    val sidecars: List<String> = emptyList(),
    /** What went wrong, in English, to show through `tr`. */
    val error: String? = null,
    val saving: Boolean = false,
) {
    val canAdd: Boolean
        get() = !saving && title.isNotBlank() && when (origin) {
            VideoOrigin.FILE -> mediaAddress != null && subtitles != null
            VideoOrigin.YOUTUBE -> youtube.id != null && youtube.track != null
        }
}

sealed interface AddVideoEvent {
    data class Added(val bookId: Long) : AddVideoEvent
}

class AddVideoViewModel(
    private val service: VideoService,
    private val videos: VideoRepository,
    private val settings: SettingsRepository,
    private val languages: LanguageRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AddVideoUiState())
    val state: StateFlow<AddVideoUiState> = _state.asStateFlow()
    val events = UiEvents<AddVideoEvent>()

    /** The player of the YouTube video being added, which hands over its subtitles. */
    private var player: YouTubeCaptions? = null

    init {
        viewModelScope.launch {
            val language = languages.getById(settings.current.currentLanguageId)
            _state.update { it.copy(languageCode = language?.let { l -> LanguageCodes.codeFor(l.name) }, nativeCode = settings.current.nativeLanguage) }
        }
    }

    fun setOrigin(origin: VideoOrigin) = _state.update { it.copy(origin = origin, error = null) }

    /** A YouTube link was typed or pasted; a new video's player starts once its id is clear. */
    fun setYouTubeInput(text: String) {
        val id = YouTubeIds.parse(text)
        _state.update {
            val same = id != null && id == it.youtube.id
            it.copy(youtube = if (same) it.youtube.copy(input = text) else YouTubePick(input = text, id = id, loading = id != null), error = null)
        }
        if (_state.value.youtube.id != id || id == null) player = null
    }

    /** The preview player of the video is there: it is asked for the title and the subtitle tracks. */
    fun onYouTubePlayer(controller: VideoController) {
        val id = state.value.youtube.id ?: return
        val captions = controller as? YouTubeCaptions
        if (captions == null) {
            _state.update { it.copy(youtube = it.youtube.copy(loading = false, unsupported = true)) }
            return
        }
        if (player === captions) return
        player = captions
        viewModelScope.launch {
            val details = captions.details()
            if (state.value.youtube.id != id) return@launch
            _state.update { s ->
                if (details == null) {
                    s.copy(youtube = s.youtube.copy(loading = false), error = "YouTube did not give this video's details.")
                } else {
                    val track = bestTrack(details.tracks, s.languageCode)
                    s.copy(
                        youtube = s.youtube.copy(loading = false, details = details, track = track),
                        title = s.title.ifBlank { details.title.orEmpty() },
                        error = if (details.tracks.isEmpty()) "This video has no subtitles." else null,
                    )
                }
            }
        }
    }

    fun chooseTrack(track: YouTubeTrack) = _state.update { it.copy(youtube = it.youtube.copy(track = track)) }

    fun setTranslate(on: Boolean) = _state.update { it.copy(youtube = it.youtube.copy(translate = on)) }

    /** The video file picked: its name and where it is kept, with the subtitle files found beside it as (name, bytes). */
    fun pickMedia(name: String, address: String, sidecars: List<Pair<String, ByteArray>>) {
        val stem = name.substringBeforeLast('.')
        _state.update { it.copy(mediaName = name, mediaAddress = address, title = it.title.ifBlank { stem }, error = null, sidecars = sidecars.map { s -> s.first }) }
        // The subtitles beside the video are the likely ones: those named like it, else the first.
        val first = sidecars.firstOrNull { it.first.substringBeforeLast('.') == stem } ?: sidecars.firstOrNull()
        if (first != null && _state.value.subtitles == null) pickSubtitles(first.first, first.second)
        sidecarBytes = sidecars.toMap()
    }

    private var sidecarBytes: Map<String, ByteArray> = emptyMap()

    fun pickSidecar(name: String, asTranslation: Boolean) {
        val bytes = sidecarBytes[name] ?: return
        if (asTranslation) pickTranslation(name, bytes) else pickSubtitles(name, bytes)
    }

    fun pickSubtitles(name: String, bytes: ByteArray) {
        val cues = SubtitleFiles.read(name, bytes)
        _state.update {
            if (cues.isEmpty()) it.copy(error = "No subtitles were found in this file.")
            else it.copy(subtitles = PickedSubtitles(name, cues), error = null)
        }
    }

    fun pickTranslation(name: String, bytes: ByteArray) {
        val cues = SubtitleFiles.read(name, bytes)
        _state.update {
            if (cues.isEmpty()) it.copy(error = "No subtitles were found in this file.")
            else it.copy(translation = PickedSubtitles(name, cues), error = null)
        }
    }

    fun clearTranslation() = _state.update { it.copy(translation = null) }

    fun setTitle(title: String) = _state.update { it.copy(title = title) }

    fun add() {
        if (state.value.origin == VideoOrigin.YOUTUBE) return addYouTube()
        val s = _state.value
        val address = s.mediaAddress ?: return
        val subtitles = s.subtitles ?: return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val language = settings.current.currentLanguageId
                val existing = videos.findByMedia(VideoKind.FILE, address, language)
                val id = existing ?: service.import(
                    VideoImport(
                        languageId = language,
                        title = s.title,
                        kind = VideoKind.FILE,
                        media = address,
                        subtitles = SubtitleTrack(subtitles.cues, SubtitleKind.FILE),
                        translation = s.translation?.cues,
                        translationSource = s.translation?.let { TranslationSource.FILE },
                    ),
                )
                _state.update { it.copy(saving = false) }
                events.send(AddVideoEvent.Added(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: VideoImportException) {
                _state.update { it.copy(saving = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = "The video could not be added.") }
            }
        }
    }

    private fun addYouTube() {
        val s = _state.value
        val id = s.youtube.id ?: return
        val track = s.youtube.track ?: return
        val captions = player ?: return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val language = settings.current.currentLanguageId
                val existing = videos.findByMedia(VideoKind.YOUTUBE, id, language)
                if (existing != null) {
                    _state.update { it.copy(saving = false) }
                    events.send(AddVideoEvent.Added(existing))
                    return@launch
                }
                val translateTo = s.nativeCode.takeIf { s.youtube.translate && it != track.languageCode.substringBefore('-') }
                val (original, translated) = captions.captions(track, translateTo)
                val cues = original?.let { SubtitleFormats.parse(it, SubtitleFormat.YOUTUBE_JSON) }.orEmpty()
                if (cues.isEmpty()) {
                    _state.update { it.copy(saving = false, error = "YouTube did not give the subtitles.") }
                    return@launch
                }
                val translation = translated?.let { SubtitleFormats.parse(it, SubtitleFormat.YOUTUBE_JSON) }?.takeIf { it.isNotEmpty() }
                val details = s.youtube.details
                val bookId = service.import(
                    VideoImport(
                        languageId = language,
                        title = s.title,
                        kind = VideoKind.YOUTUBE,
                        media = id,
                        subtitles = SubtitleTrack(cues, if (track.automatic) SubtitleKind.YOUTUBE_AUTO else SubtitleKind.YOUTUBE, track.languageCode),
                        translation = translation,
                        translationSource = translation?.let { TranslationSource.YOUTUBE },
                        channel = details?.channel,
                        thumbnail = YouTubeIds.thumbnailUrl(id),
                        durationMs = details?.lengthMs ?: 0,
                    ),
                )
                _state.update { it.copy(saving = false) }
                events.send(AddVideoEvent.Added(bookId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: VideoImportException) {
                _state.update { it.copy(saving = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = "The video could not be added.") }
            }
        }
    }

    companion object {
        /** Subtitles a person made in the language learned, else YouTube's automatic ones in it, else the first. */
        fun bestTrack(tracks: List<YouTubeTrack>, languageCode: String?): YouTubeTrack? {
            fun matches(track: YouTubeTrack) = languageCode != null && track.languageCode.substringBefore('-').equals(languageCode, ignoreCase = true)
            return tracks.firstOrNull { matches(it) && !it.automatic } ?: tracks.firstOrNull { matches(it) } ?: tracks.firstOrNull()
        }
    }
}
