package com.tayra.languages.feature.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.media.AutoPause
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.CuePlayback
import com.tayra.languages.core.domain.media.StoredCue
import com.tayra.languages.core.domain.media.Video
import com.tayra.languages.core.domain.media.VideoRepository
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.render.TextItem
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.ui.media.VideoController
import com.tayra.languages.feature.terms.form.TermFormKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** A subtitle line of the transcript: its cue, its words as the reader renders them, and its translation. */
data class TranscriptLine(
    val index: Int,
    val cue: Cue,
    val pageOrder: Int,
    val items: List<TextItem>,
    /** The translation that came with the video, or one made by the translation engine. */
    val translation: String?,
) {
    val text: String get() = cue.text

    /** Whether the line has a word not known yet, for pausing only on such lines. */
    val hasUnknownWords: Boolean get() = items.any { it.isWord && (it.status == TermStatus.UNKNOWN || it.status == TermStatus.NEW_1) }
}

/** The word picked in the transcript, by its line and its place there. */
data class PickedWord(val line: Int, val item: Int)

data class WatchUiState(
    val loading: Boolean = true,
    val video: Video? = null,
    val language: Language? = null,
    val lines: List<TranscriptLine> = emptyList(),
    val picked: PickedWord? = null,
    /** The word open in the term pane, and a string naming it for the pane's view model. */
    val term: TermFormKey? = null,
    val termKey: String = "",
    val showTranslations: Boolean = true,
    val pauseOnLookup: Boolean = true,
    val error: String? = null,
)

class WatchViewModel(
    private val bookId: Long,
    private val videos: VideoRepository,
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val reading: ReadingService,
    private val bookStats: BookStatsService,
    private val translator: SentenceTranslator,
) : ViewModel() {
    private val _state = MutableStateFlow(WatchUiState())
    val state: StateFlow<WatchUiState> = _state.asStateFlow()

    private val _playback = MutableStateFlow<CuePlayback?>(null)

    /** Line-by-line playback, once the player is there. */
    val playback: StateFlow<CuePlayback?> = _playback.asStateFlow()

    private var controller: VideoController? = null
    private var controllerJobs: List<Job> = emptyList()
    private var translationJob: Job? = null
    private var pagesRead = mutableSetOf<Int>()
    private var autoPause = AutoPause.OFF

    init {
        viewModelScope.launch { load(firstTime = true) }
    }

    private suspend fun load(firstTime: Boolean) {
        try {
            val video = videos.video(bookId) ?: run {
                _state.update { it.copy(loading = false, error = "This video is no longer in the library.") }
                return
            }
            val language = languages.getById(video.languageId)
            val stored = videos.cues(bookId)
            val lines = render(stored, firstTime)
            _state.update { s ->
                s.copy(
                    loading = false,
                    video = video,
                    language = language,
                    lines = lines.map { line -> line.copy(translation = line.translation ?: s.lines.getOrNull(line.index)?.translation) },
                )
            }
            _playback.value?.setCues(lines.map { it.cue })
            if (firstTime) translateAround(0)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = "The video could not be opened.") }
        }
    }

    /** The lines with their words, a page of the transcript at a time; a page whose lines don't match its cues is rendered line by line. */
    private suspend fun render(stored: List<StoredCue>, firstTime: Boolean): List<TranscriptLine> {
        val language = state.value.language ?: videos.video(bookId)?.let { languages.getById(it.languageId) } ?: return emptyList()
        val out = ArrayList<TranscriptLine>(stored.size)
        for ((page, cues) in stored.groupBy { it.pageOrder }.entries.sortedBy { it.key }) {
            val opened = reading.openPage(bookId, page, trackOpen = firstTime && page == 1)
            val paragraphs = opened.rendered.paragraphs.map { p -> p.sentences.flatMap { it.items }.filter { !it.isParagraphMark } }
                .filter { items -> items.any { it.renderText.isNotBlank() } }
            for (cue in cues) {
                val items = paragraphs.getOrNull(cue.lineIndex)?.takeIf { paragraphs.size == cues.size }
                    ?: reading.renderPage(cue.cue.text, language).paragraphs.flatMap { p -> p.sentences.flatMap { it.items } }.filter { !it.isParagraphMark }
                out += TranscriptLine(cue.order, cue.cue, page, items, cue.cue.translation)
            }
        }
        return out
    }

    /** The player is there: playback follows its clock, from where watching last got to. */
    fun attach(player: VideoController) {
        if (controller === player) return
        detach(save = false)
        controller = player
        val playback = CuePlayback(player, viewModelScope)
        playback.hasUnknownWords = { state.value.lines.getOrNull(it)?.hasUnknownWords ?: false }
        playback.setAutoPause(autoPause)
        playback.setCues(state.value.lines.map { it.cue })
        _playback.value = playback
        val resume = state.value.video?.positionMs ?: 0
        if (resume > 0) player.seekTo(resume)
        controllerJobs = listOf(
            viewModelScope.launch { player.durationMs.filter { it > 0 }.distinctUntilChanged().collect { videos.setDuration(bookId, it) } },
            viewModelScope.launch {
                var saved = resume
                player.position.collect { ms ->
                    if (kotlin.math.abs(ms - saved) >= SAVE_EVERY_MS) {
                        saved = ms
                        videos.setPosition(bookId, ms, final = false)
                    }
                }
            },
            viewModelScope.launch {
                playback.current.collect { index -> if (index != null) onLine(index) }
            },
        )
    }

    /** Lets go of the player, keeping where watching got to for next time and for other devices. */
    fun detach(save: Boolean = true) {
        val player = controller ?: return
        controllerJobs.forEach { it.cancel() }
        controllerJobs = emptyList()
        if (save) {
            val at = player.position.value
            // The screen may be going for good, its scope with it; the place is kept all the same.
            leaving.launch { videos.setPosition(bookId, at, final = true) }
        }
        controller = null
        _playback.value = null
    }

    private fun onLine(index: Int) {
        translateAround(index)
        val page = state.value.lines.getOrNull(index)?.pageOrder ?: return
        // A page counts as read once watching has gone past all its lines.
        val passed = page - 1
        if (passed >= 1 && pagesRead.add(passed)) {
            viewModelScope.launch { runCatching { reading.markPageRead(bookId, passed, markRestAsKnown = false) } }
        }
    }

    fun setAutoPause(mode: AutoPause) {
        autoPause = mode
        _playback.value?.setAutoPause(mode)
    }

    fun setShowTranslations(show: Boolean) {
        _state.update { it.copy(showTranslations = show) }
        if (show) translateAround(_playback.value?.current?.value ?: 0)
    }

    fun setPauseOnLookup(pause: Boolean) = _state.update { it.copy(pauseOnLookup = pause) }

    /** Translates the lines near [index] that have no translation yet, the current one first. */
    fun translateAround(index: Int) {
        if (!state.value.showTranslations) return
        val language = state.value.language ?: return
        val wanted = (index - 1..index + TRANSLATE_AHEAD).filter { i -> state.value.lines.getOrNull(i)?.translation == null && i >= 0 && i < state.value.lines.size }
        if (wanted.isEmpty()) return
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            for (i in wanted.sortedBy { kotlin.math.abs(it - index) }) {
                val line = state.value.lines.getOrNull(i) ?: continue
                if (line.translation != null) continue
                val text = try {
                    translator.translate(line.text, language)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                } ?: continue
                _state.update { s -> s.copy(lines = s.lines.map { if (it.index == i) it.copy(translation = text) else it }) }
            }
        }
    }

    /** A word of the transcript was clicked: it is picked and opened in the term pane. */
    fun pickWord(line: Int, item: Int) {
        val l = state.value.lines.getOrNull(line) ?: return
        val word = l.items.getOrNull(item)?.takeIf { it.isWord } ?: return
        val language = state.value.language ?: return
        val key = word.termId?.let { TermFormKey.ById(it, l.text) } ?: TermFormKey.ByText(language.id, word.renderText, l.text)
        _state.update { it.copy(picked = PickedWord(line, item), term = key, termKey = "$line-$item-${word.renderText}") }
        if (state.value.pauseOnLookup) _playback.value?.pause()
    }

    fun openTerm(languageId: Long, text: String, sentence: String?) {
        _state.update { it.copy(term = TermFormKey.ByText(languageId, text, sentence), termKey = "text-$text") }
    }

    fun closeTerm() = _state.update { it.copy(picked = null, term = null) }

    /** A term was saved or deleted in the pane: the words take their new colours. */
    fun onTermChanged(close: Boolean) {
        if (close) closeTerm()
        viewModelScope.launch {
            bookStats.markStale(bookId)
            load(firstTime = false)
        }
    }

    override fun onCleared() {
        detach()
        super.onCleared()
    }

    companion object {
        private val leaving = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private const val SAVE_EVERY_MS = 5_000L
        private const val TRANSLATE_AHEAD = 6
    }
}
