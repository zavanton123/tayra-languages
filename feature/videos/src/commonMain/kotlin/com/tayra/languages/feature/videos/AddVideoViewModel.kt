package com.tayra.languages.feature.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.data.media.SubtitleDownloader
import com.tayra.languages.core.data.media.SubtitleFiles
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.SubtitleFormat
import com.tayra.languages.core.domain.media.SubtitleFormats
import com.tayra.languages.core.domain.media.SubtitleKind
import com.tayra.languages.core.domain.media.SubtitleTrack
import com.tayra.languages.core.domain.media.TranscriptionException
import com.tayra.languages.core.domain.media.TranscriptionModel
import com.tayra.languages.core.domain.media.TranscriptionStage
import com.tayra.languages.core.domain.media.TranslationSource
import com.tayra.languages.core.domain.media.VideoImport
import com.tayra.languages.core.domain.media.VideoImportException
import com.tayra.languages.core.domain.media.VideoKind
import com.tayra.languages.core.domain.media.VideoRepository
import com.tayra.languages.core.domain.media.VideoService
import com.tayra.languages.core.domain.media.VideoTranscriber
import com.tayra.languages.core.domain.media.YouTubeIds
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.media.VideoController
import com.tayra.languages.core.ui.media.YouTubeCaptions
import com.tayra.languages.core.ui.media.YouTubeDetails
import com.tayra.languages.core.ui.media.YouTubeTrack
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Subtitles read for a new video, from a file or written down by Whisper. */
data class PickedSubtitles(val name: String, val cues: List<Cue>, val kind: SubtitleKind = SubtitleKind.FILE) {
    val lastMs: Long get() = cues.maxOfOrNull { it.endMs } ?: 0
}

/** Where a new video comes from. */
enum class VideoOrigin { YOUTUBE, LINK }

/** A YouTube video being added: its link, what its player says about it, and the subtitles chosen. */
data class YouTubePick(
    val input: String = "",
    val id: String? = null,
    /** Asking the player for the video's details. */
    val loading: Boolean = false,
    val details: YouTubeDetails? = null,
    val track: YouTubeTrack? = null,
    val translate: Boolean = true,
    /** The player here can't hand over subtitles (no YouTube player on this device yet). */
    val unsupported: Boolean = false,
)

/** A video from its own address on the internet, with subtitles from a file or from their own address. */
data class LinkPick(
    val input: String = "",
    val url: String? = null,
    val subtitles: PickedSubtitles? = null,
    val translation: PickedSubtitles? = null,
    /** The subtitles' and translation's addresses, as typed. */
    val subtitlesUrl: String = "",
    val translationUrl: String = "",
    /** Fetching subtitles from an address. */
    val fetching: Boolean = false,
)

/**
 * Whisper writing down a video link's subtitles: offered where this device has a transcriber for the
 * language; its [models], the one being downloaded, and the stage of a transcription under way.
 */
data class TranscriptionPick(
    val offered: Boolean = false,
    val models: List<TranscriptionModel> = emptyList(),
    val installing: String? = null,
    val installProgress: String? = null,
    val running: Boolean = false,
    val stage: TranscriptionStage? = null,
    val fraction: Float? = null,
) {
    val ready: Boolean get() = models.any { it.installed }
}

data class AddVideoUiState(
    val origin: VideoOrigin = VideoOrigin.YOUTUBE,
    val youtube: YouTubePick = YouTubePick(),
    val link: LinkPick = LinkPick(),
    val transcription: TranscriptionPick = TranscriptionPick(),
    val languageCode: String? = null,
    val nativeCode: String = "en",
    val title: String = "",
    /** What went wrong, in English, to show through `tr`. */
    val error: String? = null,
    val saving: Boolean = false,
) {
    val canAdd: Boolean
        get() = !saving && !transcription.running && title.isNotBlank() && when (origin) {
            VideoOrigin.YOUTUBE -> youtube.id != null && youtube.track != null
            VideoOrigin.LINK -> link.url != null && link.subtitles != null
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
    private val downloader: SubtitleDownloader,
    private val transcriber: VideoTranscriber? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(AddVideoUiState())
    val state: StateFlow<AddVideoUiState> = _state.asStateFlow()
    val events = UiEvents<AddVideoEvent>()

    /** The player of the YouTube video being added, which hands over its subtitles. */
    private var player: YouTubeCaptions? = null

    private var transcription: Job? = null

    init {
        viewModelScope.launch {
            val language = languages.getById(settings.current.currentLanguageId)
            val code = language?.let { l -> LanguageCodes.codeFor(l.name) }
            _state.update { it.copy(languageCode = code, nativeCode = settings.current.nativeLanguage) }
            if (transcriber != null && code != null && transcriber.supports(code)) {
                _state.update { it.copy(transcription = it.transcription.copy(offered = true)) }
                loadModels()
            }
        }
        transcriber?.let { t ->
            viewModelScope.launch { t.installProgress.collect { text -> _state.update { it.copy(transcription = it.transcription.copy(installProgress = text)) } } }
        }
    }

    fun setOrigin(origin: VideoOrigin) = _state.update { it.copy(origin = origin, error = null) }

    fun setTitle(title: String) = _state.update { it.copy(title = title) }

    // ---- YouTube

    /** A YouTube link was typed or pasted; a new video's player starts once its id is clear. */
    fun setYouTubeInput(text: String) {
        val id = YouTubeIds.parse(text)
        val changed = id != state.value.youtube.id
        _state.update { it.copy(youtube = if (!changed) it.youtube.copy(input = text) else YouTubePick(input = text, id = id, loading = id != null), error = null) }
        if (changed) player = null
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
                    s.copy(
                        youtube = s.youtube.copy(loading = false, details = details, track = bestTrack(details.tracks, s.languageCode)),
                        title = s.title.ifBlank { details.title.orEmpty() },
                        error = if (details.tracks.isEmpty()) "This video has no subtitles." else null,
                    )
                }
            }
        }
    }

    fun chooseTrack(track: YouTubeTrack) = _state.update { it.copy(youtube = it.youtube.copy(track = track)) }

    fun setTranslate(on: Boolean) = _state.update { it.copy(youtube = it.youtube.copy(translate = on)) }

    // ---- A video link

    /** The video's address was typed or pasted: kept when it is a web address. */
    fun setLinkInput(text: String) {
        val url = webAddress(text)
        _state.update { s ->
            s.copy(
                link = s.link.copy(input = text, url = url),
                title = if (url != null && (s.title.isBlank() || s.title == titleOf(s.link.url))) titleOf(url).orEmpty() else s.title,
                error = null,
            )
        }
    }

    fun setSubtitlesUrl(text: String) = _state.update { it.copy(link = it.link.copy(subtitlesUrl = text)) }

    fun setTranslationUrl(text: String) = _state.update { it.copy(link = it.link.copy(translationUrl = text)) }

    fun pickSubtitles(name: String, bytes: ByteArray) = read(name, bytes, asTranslation = false)

    fun pickTranslation(name: String, bytes: ByteArray) = read(name, bytes, asTranslation = true)

    fun clearTranslation() = _state.update { it.copy(link = it.link.copy(translation = null)) }

    /** Fetches the subtitles (or their translation) from the address typed for them. */
    fun fetchSubtitles(asTranslation: Boolean) {
        val url = webAddress(if (asTranslation) state.value.link.translationUrl else state.value.link.subtitlesUrl)
        if (url == null) {
            _state.update { it.copy(error = "This is not a web address.") }
            return
        }
        _state.update { it.copy(link = it.link.copy(fetching = true), error = null) }
        viewModelScope.launch {
            val file = downloader.download(url)
            _state.update { it.copy(link = it.link.copy(fetching = false)) }
            if (file == null) _state.update { it.copy(error = "The subtitles could not be fetched.") } else read(file.first, file.second, asTranslation)
        }
    }

    private fun read(name: String, bytes: ByteArray, asTranslation: Boolean) {
        val cues = SubtitleFiles.read(name, bytes)
        _state.update {
            when {
                cues.isEmpty() -> it.copy(error = "No subtitles were found in this file.")
                asTranslation -> it.copy(link = it.link.copy(translation = PickedSubtitles(name, cues)), error = null)
                else -> it.copy(link = it.link.copy(subtitles = PickedSubtitles(name, cues)), error = null)
            }
        }
    }

    // ---- Whisper

    private suspend fun loadModels() {
        val models = transcriber?.models() ?: return
        _state.update { it.copy(transcription = it.transcription.copy(models = models)) }
    }

    fun installModel(model: TranscriptionModel) {
        val t = transcriber ?: return
        _state.update { it.copy(transcription = it.transcription.copy(installing = model.id), error = null) }
        viewModelScope.launch {
            try {
                t.install(model)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "The model could not be downloaded.") }
            } finally {
                _state.update { it.copy(transcription = it.transcription.copy(installing = null)) }
                loadModels()
            }
        }
    }

    fun removeModel(model: TranscriptionModel) {
        val t = transcriber ?: return
        viewModelScope.launch {
            t.remove(model)
            loadModels()
        }
    }

    /** Whisper writes down what is said in the linked video; its lines become the subtitles. */
    fun transcribe() {
        val t = transcriber ?: return
        val url = state.value.link.url ?: return
        val language = state.value.languageCode ?: return
        if (transcription?.isActive == true) return
        _state.update { it.copy(transcription = it.transcription.copy(running = true, stage = TranscriptionStage.AUDIO, fraction = null), error = null) }
        transcription = viewModelScope.launch {
            try {
                val cues = t.transcribe(url, language) { stage, fraction ->
                    _state.update { it.copy(transcription = it.transcription.copy(stage = stage, fraction = fraction)) }
                }
                _state.update {
                    if (cues.isEmpty()) it.copy(error = "Whisper heard no speech in this video.")
                    else it.copy(link = it.link.copy(subtitles = PickedSubtitles("Whisper", cues, SubtitleKind.TRANSCRIBED)))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: TranscriptionException) {
                _state.update { it.copy(error = e.message) }
            } catch (e: Exception) {
                Logger.w(e) { "transcription failed" }
                _state.update { it.copy(error = "The transcription failed.") }
            } finally {
                _state.update { it.copy(transcription = it.transcription.copy(running = false, stage = null, fraction = null)) }
            }
        }
    }

    fun cancelTranscription() {
        transcription?.cancel()
    }

    // ---- Adding

    fun add() {
        val s = state.value
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val id = when (s.origin) {
                    VideoOrigin.YOUTUBE -> addYouTube(s)
                    VideoOrigin.LINK -> addLink(s)
                }
                _state.update { it.copy(saving = false) }
                if (id != null) events.send(AddVideoEvent.Added(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: VideoImportException) {
                _state.update { it.copy(saving = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = "The video could not be added.") }
            }
        }
    }

    private suspend fun addLink(s: AddVideoUiState): Long? {
        val url = s.link.url ?: return null
        val subtitles = s.link.subtitles ?: return null
        val language = settings.current.currentLanguageId
        videos.findByMedia(VideoKind.LINK, url, language)?.let { return it }
        return service.import(
            VideoImport(
                languageId = language,
                title = s.title,
                kind = VideoKind.LINK,
                media = url,
                subtitles = SubtitleTrack(subtitles.cues, subtitles.kind, s.languageCode.takeIf { subtitles.kind == SubtitleKind.TRANSCRIBED }),
                translation = s.link.translation?.cues,
                translationSource = s.link.translation?.let { TranslationSource.FILE },
            ),
        )
    }

    private suspend fun addYouTube(s: AddVideoUiState): Long? {
        val id = s.youtube.id ?: return null
        val track = s.youtube.track ?: return null
        val captions = player ?: return null
        val language = settings.current.currentLanguageId
        videos.findByMedia(VideoKind.YOUTUBE, id, language)?.let { return it }
        val translateTo = s.nativeCode.takeIf { s.youtube.translate && it != track.languageCode.substringBefore('-') }
        val (original, translated) = captions.captions(track, translateTo)
        val cues = original?.let { SubtitleFormats.parse(it, SubtitleFormat.YOUTUBE_JSON) }.orEmpty()
        if (cues.isEmpty()) {
            _state.update { it.copy(error = "YouTube did not give the subtitles.") }
            return null
        }
        val translation = translated?.let { SubtitleFormats.parse(it, SubtitleFormat.YOUTUBE_JSON) }?.takeIf { it.isNotEmpty() }
        val details = s.youtube.details
        return service.import(
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
    }

    companion object {
        /** Subtitles a person made in the language learned, else YouTube's automatic ones in it, else the first. */
        fun bestTrack(tracks: List<YouTubeTrack>, languageCode: String?): YouTubeTrack? {
            fun matches(track: YouTubeTrack) = languageCode != null && track.languageCode.substringBefore('-').equals(languageCode, ignoreCase = true)
            return tracks.firstOrNull { matches(it) && !it.automatic } ?: tracks.firstOrNull { matches(it) } ?: tracks.firstOrNull()
        }

        /** [text] as an http(s) address, or null. */
        fun webAddress(text: String): String? = text.trim().takeIf { Regex("""^https?://[^\s/]+\.[^\s]+$""", RegexOption.IGNORE_CASE).matches(it) }

        /** A title from a video's address: its file's name, without the extension. */
        fun titleOf(url: String?): String? = url?.substringBefore('?')?.substringBefore('#')?.trimEnd('/')?.substringAfterLast('/')
            ?.substringBeforeLast('.')?.replace('_', ' ')?.replace('-', ' ')?.trim()?.takeIf { it.isNotEmpty() && '.' !in it }
    }
}
