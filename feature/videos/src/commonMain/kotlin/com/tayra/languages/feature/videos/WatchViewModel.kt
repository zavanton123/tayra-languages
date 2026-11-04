package com.tayra.languages.feature.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.media.AutoPause
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.CuePlayback
import com.tayra.languages.core.domain.media.StoredCue
import com.tayra.languages.core.domain.media.Video
import com.tayra.languages.core.domain.media.VideoBlur
import com.tayra.languages.core.domain.media.VideoRepository
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.render.TextItem
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.media.VideoController
import com.tayra.languages.core.ui.media.VideoProblem
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

/** A word of a video: how often it comes, its status and meaning, and where it first comes. */
data class VideoWord(
    val text: String,
    val textLc: String,
    val status: TermStatus,
    val translation: String?,
    val count: Int,
    val firstLine: Int,
    val firstItem: Int,
)

/** How the words of a video are listed. */
enum class WordOrder { NEW_FIRST, MOST_FREQUENT, ALPHABETICAL }

/** The words of [lines], each once (a phrase counts as its own word), ordered by [order]. */
fun videoWords(lines: List<TranscriptLine>, order: WordOrder): List<VideoWord> {
    val found = LinkedHashMap<String, VideoWord>()
    for (line in lines) {
        line.items.forEachIndexed { i, item ->
            if (!item.isWord) return@forEachIndexed
            val key = item.textLc
            val known = found[key]
            found[key] = known?.copy(count = known.count + 1)
                ?: VideoWord(item.renderText, key, item.status, item.term?.translation, 1, line.index, i)
        }
    }
    val words = found.values.toList()
    return when (order) {
        WordOrder.NEW_FIRST -> words.sortedWith(compareBy<VideoWord> { statusRank(it.status) }.thenByDescending { it.count }.thenBy { it.textLc })
        WordOrder.MOST_FREQUENT -> words.sortedWith(compareByDescending<VideoWord> { it.count }.thenBy { it.textLc })
        WordOrder.ALPHABETICAL -> words.sortedBy { it.textLc }
    }
}

/** New words first, then those being learned (the least known first), then known and ignored ones. */
private fun statusRank(status: TermStatus): Int = when (status) {
    TermStatus.UNKNOWN -> 0
    TermStatus.NEW_1 -> 1
    TermStatus.NEW_2 -> 2
    TermStatus.LEARNING_3 -> 3
    TermStatus.LEARNING_4 -> 4
    TermStatus.WELL_KNOWN -> 5
    TermStatus.IGNORED -> 6
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
    val blur: VideoBlur = VideoBlur.OFF,
    /** Lines shown on purpose while their kind is hidden. */
    val revealed: Set<Int> = emptySet(),
    val error: String? = null,
) {
    /** Whether [line]'s own text is hidden: with lines hidden, until it has been heard or revealed. */
    fun hidesLine(line: Int, current: Int?): Boolean = blur == VideoBlur.LINE && line !in revealed && (current == null || line >= current)

    /** Whether [line]'s translation is hidden. */
    fun hidesTranslation(line: Int, current: Int?): Boolean = when (blur) {
        VideoBlur.OFF -> false
        VideoBlur.TRANSLATION -> line !in revealed
        VideoBlur.LINE -> hidesLine(line, current)
    }
}

class WatchViewModel(
    private val bookId: Long,
    private val videos: VideoRepository,
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val reading: ReadingService,
    private val bookStats: BookStatsService,
    private val translator: SentenceTranslator,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(
        settings.current.let { WatchUiState(showTranslations = it.videoTranslations, pauseOnLookup = it.videoPauseOnLookup, blur = it.videoBlur) },
    )
    val state: StateFlow<WatchUiState> = _state.asStateFlow()

    private val _playback = MutableStateFlow<CuePlayback?>(null)

    /** Line-by-line playback, once the player is there. */
    val playback: StateFlow<CuePlayback?> = _playback.asStateFlow()

    private val _problem = MutableStateFlow<VideoProblem?>(null)

    /** Why the video does not play, as the player tells it. */
    val problem: StateFlow<VideoProblem?> = _problem.asStateFlow()

    private var controller: VideoController? = null
    private var controllerJobs: List<Job> = emptyList()
    private var translationJob: Job? = null
    private var pagesRead = mutableSetOf<Int>()
    private var autoPause = settings.current.videoAutoPause

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
            viewModelScope.launch { player.problem.collect { _problem.value = it } },
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
            // The Q key changes it on the playback itself; it is kept for next time all the same.
            viewModelScope.launch {
                playback.autoPause.collect { mode ->
                    autoPause = mode
                    if (settings.current.videoAutoPause != mode) settings.update { it.copy(videoAutoPause = mode) }
                }
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

    /** The video file was chosen again, where it now is; the player opens it from there. */
    fun relink(address: String) {
        viewModelScope.launch {
            videos.setMedia(bookId, address)
            _problem.value = null
            _state.update { it.copy(video = it.video?.copy(media = address)) }
        }
    }

    fun setAutoPause(mode: AutoPause) {
        autoPause = mode
        _playback.value?.setAutoPause(mode)
    }

    fun setShowTranslations(show: Boolean) {
        _state.update { it.copy(showTranslations = show) }
        viewModelScope.launch { settings.update { it.copy(videoTranslations = show) } }
        if (show) translateAround(_playback.value?.current?.value ?: 0)
    }

    fun setPauseOnLookup(pause: Boolean) {
        _state.update { it.copy(pauseOnLookup = pause) }
        viewModelScope.launch { settings.update { it.copy(videoPauseOnLookup = pause) } }
    }

    fun setBlur(blur: VideoBlur) {
        _state.update { it.copy(blur = blur, revealed = emptySet()) }
        viewModelScope.launch { settings.update { it.copy(videoBlur = blur) } }
    }

    fun cycleBlur() = setBlur(VideoBlur.entries[(state.value.blur.ordinal + 1) % VideoBlur.entries.size])

    /** Shows the hidden part of [line]. */
    fun reveal(line: Int) = _state.update { it.copy(revealed = it.revealed + line) }

    /** Shows the hidden part of the line being spoken, as the E key does. */
    fun revealCurrent() {
        _playback.value?.current?.value?.let(::reveal)
    }

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
