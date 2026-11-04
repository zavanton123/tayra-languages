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

data class AddVideoUiState(
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
    val canAdd: Boolean get() = mediaAddress != null && subtitles != null && title.isNotBlank() && !saving
}

sealed interface AddVideoEvent {
    data class Added(val bookId: Long) : AddVideoEvent
}

class AddVideoViewModel(
    private val service: VideoService,
    private val videos: VideoRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AddVideoUiState())
    val state: StateFlow<AddVideoUiState> = _state.asStateFlow()
    val events = UiEvents<AddVideoEvent>()

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
}
