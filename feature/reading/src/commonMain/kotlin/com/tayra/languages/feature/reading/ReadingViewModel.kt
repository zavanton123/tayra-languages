package com.tayra.languages.feature.reading

import kotlin.coroutines.cancellation.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.render.RenderedPage
import com.tayra.languages.core.domain.render.TextItem
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.BulkTermUpdate
import com.tayra.languages.core.domain.service.LocalTranslationProblem
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceTranslation
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermPopup
import com.tayra.languages.core.domain.service.TermPopupBuilder
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.service.effectiveEngine
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.state.UiEvents
import com.tayra.languages.core.ui.theme.AppThemes
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.tayra.languages.core.domain.service.SentenceAudioState
import com.tayra.languages.core.domain.service.SentenceAudio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** What is shown in the side panel / bottom sheet. */
sealed interface ReadingPanel {
    data object None : ReadingPanel
    data class EditTerm(val termId: Long, val version: Int = 0, val sentence: String? = null) : ReadingPanel
    data class NewTerm(val languageId: Long, val text: String, val sentence: String? = null) : ReadingPanel
    data class BulkEdit(val termIds: List<Long>) : ReadingPanel
}

enum class TextScope { SENTENCE, PARAGRAPH, PAGE }

/** [COLORED_WORD] is a word shown with a colour: unknown or being learned, not known or ignored. */
enum class CursorTarget { WORD, UNKNOWN_WORD, COLORED_WORD, SENTENCE_START }

data class PopupState(val itemIndex: Int, val popup: TermPopup)

data class ReadingUiState(
    val loading: Boolean = true,
    val book: Book? = null,
    val language: Language? = null,
    val pageNumber: Int = 1,
    val pageCount: Int = 1,
    val page: RenderedPage = RenderedPage.EMPTY,
    /** Indexes (into [RenderedPage.items]) of clicked words. */
    val marked: Set<Int> = emptySet(),
    val hovered: Int? = null,
    /** Token index range of a multi-word selection; kept highlighted while its term form is open. */
    val selection: IntRange? = null,
    /** True while the user is still extending the selection (dragging, or between long-presses). */
    val selecting: Boolean = false,
    val panel: ReadingPanel = ReadingPanel.None,
    val popup: PopupState? = null,
    val settings: UserSettings = UserSettings(),
    val error: String? = null,
    val flash: String? = null,
    /** Sentence translations for the page, keyed by the sentence's display text. */
    val translations: Map<String, SentenceTranslation> = emptyMap(),
    /** Why offline translations are failing right now, shown above the text; null when they work or are off. */
    val translationError: LocalTranslationProblem? = null,
    /** What the offline translator is installing right now, shown above the text. */
    val translationProgress: String? = null,
) {
    val items: List<TextItem> get() = page.items
    val isLastPage: Boolean get() = pageNumber >= pageCount
    val isFirstPage: Boolean get() = pageNumber <= 1

    /** The words that status hotkeys act on: clicked words, or the hovered word. */
    val activeWordIndexes: List<Int>
        get() = if (marked.isNotEmpty()) marked.sorted() else listOfNotNull(hovered)

    val activeTermIds: List<Long> get() = activeWordIndexes.mapNotNull { items.getOrNull(it)?.termId }.distinct()

    fun itemsInSelection(): List<TextItem> {
        val range = selection ?: return emptyList()
        return items.filter { it.index in range }
    }
}

sealed interface ReadingEvent {
    data class CopyText(val text: String) : ReadingEvent
    data class OpenUrl(val url: String) : ReadingEvent
    data class Navigate(val bookId: Long, val page: Int) : ReadingEvent
    data object BookArchived : ReadingEvent
}

class ReadingViewModel(
    private val bookId: Long,
    initialPage: Int?,
    private val readingService: ReadingService,
    private val bookService: BookService,
    private val books: BookRepository,
    private val termService: TermService,
    private val popupBuilder: TermPopupBuilder,
    private val bookStats: BookStatsService,
    private val settingsRepository: SettingsRepository,
    private val translator: SentenceTranslator,
    localTranslation: LocalTranslation,
    private val localSpeech: LocalSpeech,
    private val wordTranslations: WordTranslationService,
    private val sentenceAudio: SentenceAudio,
) : ViewModel() {

    private val local = localTranslation.translator

    /** Whether the engine switch in the drawer has anything to switch to (Argos on desktop, ML Kit on phones). */
    val hasLocalTranslator: Boolean = local != null

    /** The on-device translator's name for the page caption. */
    val localTranslatorName: String? = local?.displayName

    private val _state = MutableStateFlow(ReadingUiState())
    val state: StateFlow<ReadingUiState> = combine(
        _state,
        settingsRepository.settings,
        local?.lastError ?: MutableStateFlow(null),
        local?.progress ?: MutableStateFlow(null),
    ) { s, prefs, localError, localProgress ->
        val offline = prefs.showTranslations && prefs.effectiveEngine(local != null) == TranslationEngine.ARGOS
        s.copy(settings = prefs, translationError = localError.takeIf { offline }, translationProgress = localProgress.takeIf { offline })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ReadingUiState())
    val events = UiEvents<ReadingEvent>()

    private var popupJob: Job? = null
    private var translationJob: Job? = null
    private var speechJob: Job? = null
    private var lastTranslation: Pair<String, Int>? = null

    /** The page's sentences by how far their audio has got, for the play buttons. */
    val sentenceAudioStates: StateFlow<Map<String, SentenceAudioState>> = sentenceAudio.states

    init {
        // A new engine, voice or speed makes different audio, so the page is prepared again.
        viewModelScope.launch {
            settingsRepository.settings
                .map { listOf(it.showSentencePlay, it.speechEngine, it.speechVoices, it.speechSpeed) }
                .distinctUntilChanged()
                .drop(1)
                .collect { prepareSpeech() }
        }
        viewModelScope.launch {
            val book = books.getBook(bookId)
            if (book == null) {
                _state.update { it.copy(loading = false, error = "Book not found") }
                return@launch
            }
            val page = initialPage ?: readingService.currentPageNumber(book)
            load(page, trackOpen = true)
        }
    }

    private suspend fun load(pageNumber: Int, trackOpen: Boolean, keepMarked: Boolean = false) {
        try {
            val reading = readingService.openPage(bookId, pageNumber, trackOpen)
            _state.update {
                it.copy(
                    loading = false,
                    book = reading.book,
                    language = reading.language,
                    pageNumber = reading.page.order,
                    pageCount = reading.pageCount,
                    page = reading.rendered,
                    marked = if (keepMarked) it.marked.filter { i -> i < reading.rendered.items.size }.toSet() else emptySet(),
                    hovered = if (keepMarked) it.hovered else null,
                    selection = null,
                    selecting = false,
                    popup = null,
                    panel = if (keepMarked) it.panel else ReadingPanel.None,
                    error = null,
                    translations = if (keepMarked) it.translations else emptyMap(),
                )
            }
            translateSentences()
            prepareSpeech()
        } catch (e: Exception) {
            Logger.e(e) { "Could not load page" }
            _state.update { it.copy(loading = false, error = e.message ?: "Could not load page") }
        }
    }

    fun goToPage(pageNumber: Int) {
        viewModelScope.launch { load(pageNumber.coerceIn(1, _state.value.pageCount), trackOpen = true) }
    }

    fun goToRelativePage(delta: Int) = goToPage(_state.value.pageNumber + delta)

    /** Re-renders the current page after term changes, keeping the cursor. */
    fun refresh() {
        viewModelScope.launch { load(_state.value.pageNumber, trackOpen = false, keepMarked = true) }
    }

    fun markPageRead(markRestAsKnown: Boolean, thenGoToRelative: Int) {
        viewModelScope.launch {
            val s = _state.value
            readingService.markPageRead(bookId, s.pageNumber, markRestAsKnown)
            bookStats.markStale(bookId)
            load(s.pageNumber + thenGoToRelative, trackOpen = true)
        }
    }

    // ---- cursor / selection

    fun onWordClick(itemIndex: Int, shift: Boolean) {
        val s = _state.value
        val item = s.items.getOrNull(itemIndex) ?: return
        if (!item.isWord) return
        hidePopup()
        if (!shift) {
            val nowMarked = itemIndex !in s.marked || s.marked.size > 1
            _state.update { it.copy(marked = if (nowMarked) setOf(itemIndex) else emptySet(), hovered = null, selection = null) }
            if (nowMarked) openTerm(item) else closePanel()
            return
        }
        val marked = if (itemIndex in s.marked) s.marked - itemIndex else s.marked + itemIndex
        _state.update { it.copy(marked = marked, hovered = if (marked.isEmpty()) itemIndex else null, selection = null) }
        if (marked.isEmpty()) closePanel() else showBulkPanel(marked)
    }

    /** Mobile: tap on a word. */
    fun onWordTap(itemIndex: Int) {
        val s = _state.value
        val item = s.items.getOrNull(itemIndex) ?: return
        if (!item.isWord) return
        if (s.selecting) {
            endSelection(item.index, copy = false)
            return
        }
        hidePopup()
        _state.update { it.copy(marked = setOf(itemIndex), hovered = null, selection = null) }
        openTerm(item)
    }

    /**
     * Sets one word's status. Unknown words have no stored term yet (their id is 0), so those
     * are created through the reading service instead of updated by id.
     */
    private fun setStatusForItem(item: TextItem, status: TermStatus) {
        val language = _state.value.language ?: return
        val sentence = sentenceOf(item)
        viewModelScope.launch {
            val id = item.termId
            if (id != null) termService.setStatus(listOf(id), status, sentence?.let { mapOf(id to it) }.orEmpty()) else readingService.setStatusForTexts(language, listOf(item.text), status, sentence)
            afterTermChange()
        }
    }

    /** The sentence [item] is read in, as shown on screen; stored with a word that starts being learned. */
    private fun sentenceOf(item: TextItem): String? =
        _state.value.page.paragraphs.asSequence().flatMap { it.sentences }.firstOrNull { s -> s.items.any { it === item } }?.displayText?.takeIf { it.isNotBlank() }

    /** The sentences the active words are read in, by term id. */
    private fun activeSentences(): Map<Long, String> {
        val s = _state.value
        return s.activeWordIndexes.mapNotNull { s.items.getOrNull(it) }
            .mapNotNull { item -> item.termId?.let { id -> sentenceOf(item)?.let { id to it } } }.toMap()
    }

    /**
     * Right click, without opening the panel: an unknown word starts at status 1, a word being
     * learned (1 to 5) becomes well known, and a well-known or ignored word goes back to 1.
     */
    fun markToLearn(itemIndex: Int) {
        val s = _state.value
        val item = s.items.getOrNull(itemIndex) ?: return
        if (!item.isWord) return
        val next = if (item.status.isLearning) TermStatus.WELL_KNOWN else TermStatus.NEW_1
        hidePopup()
        _state.update { it.copy(marked = setOf(itemIndex), hovered = null, selection = null, selecting = false) }
        setStatusForItem(item, next)
    }

    private fun openTerm(item: TextItem) {
        val termId = item.termId
        val sentence = sentenceOf(item)
        val panel = if (termId != null) ReadingPanel.EditTerm(termId, sentence = sentence) else ReadingPanel.NewTerm(item.term?.languageId ?: return, item.text, sentence)
        _state.update { it.copy(panel = panel) }
    }

    private fun showBulkPanel(marked: Set<Int>) {
        val ids = marked.mapNotNull { _state.value.items.getOrNull(it)?.termId }.distinct()
        _state.update { it.copy(panel = ReadingPanel.BulkEdit(ids)) }
    }

    /**
     * Mouse over a word: its translation card follows after a short pause, also while another
     * word's pane is open. Nothing shows during a drag selection, or for a word whose pane is open.
     */
    fun onHover(itemIndex: Int?) {
        val s = _state.value
        if (s.selecting) {
            if (itemIndex == null) hidePopup()
            return
        }
        val item = itemIndex?.let { s.items.getOrNull(it) }?.takeIf { it.isWord }
        val index = item?.let { itemIndex }?.takeIf { it !in s.marked }
        if (index == s.hovered) return
        _state.update { it.copy(hovered = index) }
        schedulePopup(index)
    }

    private fun schedulePopup(itemIndex: Int?) {
        popupJob?.cancel()
        if (itemIndex == null) {
            _state.update { it.copy(popup = null) }
            return
        }
        val item = _state.value.items.getOrNull(itemIndex) ?: return
        popupJob = viewModelScope.launch {
            delay(350)
            val popup = popupFor(item)
            _state.update { s ->
                if (s.hovered == itemIndex && popup != null) s.copy(popup = PopupState(itemIndex, popup)) else s.copy(popup = null)
            }
        }
    }

    fun showPopupFor(itemIndex: Int) {
        val item = _state.value.items.getOrNull(itemIndex) ?: return
        viewModelScope.launch {
            val popup = popupFor(item)
            _state.update { it.copy(popup = popup?.let { p -> PopupState(itemIndex, p) }) }
        }
    }

    /** The saved term's card, or, for a word nobody has translated yet, a card with a looked-up translation. */
    private suspend fun popupFor(item: TextItem): TermPopup? {
        item.termId?.let { id -> popupBuilder.build(id) }?.let { return it }
        val language = _state.value.language ?: return null
        val translation = wordTranslations.translate(language, item.renderText) ?: return null
        return TermPopup(termText = item.renderText, parentsText = "", translation = translation, romanization = "", flashMessage = "")
    }

    fun hidePopup() {
        popupJob?.cancel()
        if (_state.value.popup != null) _state.update { it.copy(popup = null) }
    }

    fun startSelection(tokenIndex: Int) {
        hidePopup()
        _state.update { it.copy(selection = tokenIndex..tokenIndex, selecting = true, hovered = null, marked = emptySet()) }
    }

    fun updateSelection(tokenIndex: Int) {
        if (!_state.value.selecting) return
        val start = _state.value.selection?.first ?: return
        val range = if (tokenIndex >= start) start..tokenIndex else tokenIndex..start
        _state.update { it.copy(selection = range) }
    }

    /**
     * Ends a multi-word selection: copies the text, or opens a new term form. The selection
     * stays highlighted while the form is open.
     */
    fun endSelection(tokenIndex: Int, copy: Boolean) {
        val s = _state.value
        val start = s.selection?.first ?: return
        val range = if (tokenIndex >= start) start..tokenIndex else tokenIndex..start
        val items = s.items.filter { it.index in range }
        val text = items.joinToString("") { it.renderText }.trim()
        val language = s.language
        if (text.isEmpty() || language == null || copy) {
            _state.update { it.copy(selection = null, selecting = false, marked = emptySet()) }
            if (copy && text.isNotEmpty()) copyText(text)
            return
        }
        _state.update { it.copy(selection = range, selecting = false, marked = emptySet(), panel = ReadingPanel.NewTerm(language.id, text, items.firstOrNull()?.let(::sentenceOf))) }
    }

    fun cancelSelection() = _state.update { it.copy(selection = null, selecting = false) }

    fun startHoverMode() {
        hidePopup()
        _state.update { it.copy(marked = emptySet(), selection = null, selecting = false, panel = ReadingPanel.None) }
    }

    fun closePanel() = _state.update { it.copy(panel = ReadingPanel.None, selection = null, selecting = false) }

    /**
     * Hides the term pane, keeping the word selected, or shows it again for the selected word (or
     * words). With nothing selected it opens empty, to look a word up.
     */
    fun toggleTermPane() {
        val s = _state.value
        if (s.panel != ReadingPanel.None) {
            closePanel()
            return
        }
        when {
            s.marked.size > 1 -> showBulkPanel(s.marked)
            s.marked.size == 1 -> s.items.getOrNull(s.marked.single())?.takeIf { it.isWord }?.let(::openTerm)
            else -> s.language?.let { language -> _state.update { it.copy(panel = ReadingPanel.NewTerm(language.id, "")) } }
        }
    }

    fun moveCursor(direction: Int, target: CursorTarget) {
        val s = _state.value
        val words = s.items.withIndex().filter { (_, item) ->
            item.isWord && when (target) {
                CursorTarget.WORD -> true
                CursorTarget.UNKNOWN_WORD -> item.status == TermStatus.UNKNOWN
                CursorTarget.COLORED_WORD -> item.status != TermStatus.WELL_KNOWN && item.status != TermStatus.IGNORED
                CursorTarget.SENTENCE_START -> item.isSentenceStart
            }
        }
        if (words.isEmpty()) return
        val current = s.activeWordIndexes.firstOrNull()
        val next = if (direction > 0) {
            words.firstOrNull { current == null || it.index > current } ?: return
        } else {
            words.lastOrNull { current == null || it.index < current } ?: return
        }
        hidePopup()
        _state.update { it.copy(marked = setOf(next.index), hovered = null, selection = null) }
        openTerm(next.value)
    }

    // ---- term changes

    fun setStatus(status: TermStatus) {
        val ids = _state.value.activeTermIds
        if (ids.isEmpty()) return
        val sentences = activeSentences()
        viewModelScope.launch {
            termService.setStatus(ids, status, sentences)
            afterTermChange()
        }
    }

    fun shiftStatus(delta: Int) {
        val ids = _state.value.activeTermIds
        if (ids.isEmpty()) return
        val sentences = activeSentences()
        viewModelScope.launch {
            termService.shiftStatus(ids, delta, sentences)
            afterTermChange()
        }
    }

    fun applyBulkUpdate(update: BulkTermUpdate) {
        val panel = _state.value.panel as? ReadingPanel.BulkEdit ?: return
        viewModelScope.launch {
            try {
                termService.applyBulkUpdate(update.copy(termIds = panel.termIds))
                _state.update { it.copy(panel = ReadingPanel.None, marked = emptySet()) }
                afterTermChange()
            } catch (e: Exception) {
                _state.update { it.copy(flash = e.message) }
            }
        }
    }

    /** Called when the embedded term form saved a term but stays open: re-render, keep the panel. */
    fun onTermChanged() {
        viewModelScope.launch {
            bookStats.markStale(bookId)
            load(_state.value.pageNumber, trackOpen = false, keepMarked = true)
        }
    }

    /** Called when the embedded term form saved or deleted a term. */
    fun onTermFormDone() {
        _state.update { it.copy(panel = ReadingPanel.None, selection = null, selecting = false) }
        viewModelScope.launch { afterTermChange() }
    }

    /** Opens a parent from the term pane; the family was met in the pane's sentence, so it goes along. */
    fun openParentTerm(languageId: Long, text: String) {
        _state.update {
            val sentence = when (val panel = it.panel) {
                is ReadingPanel.EditTerm -> panel.sentence
                is ReadingPanel.NewTerm -> panel.sentence
                else -> null
            }
            it.copy(panel = ReadingPanel.NewTerm(languageId, text, sentence))
        }
    }

    private suspend fun afterTermChange() {
        bookStats.markStale(bookId)
        load(_state.value.pageNumber, trackOpen = false, keepMarked = true)
        val panel = _state.value.panel
        if (panel is ReadingPanel.EditTerm) _state.update { it.copy(panel = panel.copy(version = panel.version + 1)) }
    }

    // ---- text of the current scope

    fun textFor(scope: TextScope): String {
        val s = _state.value
        val anchor = s.activeWordIndexes.firstOrNull()?.let { s.items.getOrNull(it) }
        val items = when (scope) {
            TextScope.PAGE -> s.items
            TextScope.SENTENCE -> anchor?.let { a -> s.items.filter { it.sentenceNumber == a.sentenceNumber } } ?: emptyList()
            TextScope.PARAGRAPH -> anchor?.let { a -> s.items.filter { it.paragraphNumber == a.paragraphNumber } } ?: emptyList()
        }
        return items.groupBy { it.paragraphNumber }.values
            .joinToString("\n") { para -> para.filterNot { it.isParagraphMark }.joinToString("") { it.renderText } }
            .trim()
    }

    fun copy(scope: TextScope) {
        val text = textFor(scope)
        if (text.isNotEmpty()) copyText(text)
    }

    private fun copyText(text: String) {
        viewModelScope.launch {
            events.send(ReadingEvent.CopyText(text))
            _state.update { it.copy(flash = "Copied to clipboard") }
            delay(1500)
            _state.update { if (it.flash == "Copied to clipboard") it.copy(flash = null) else it }
        }
    }

    /** Opens the next sentence dictionary for the text; repeated calls on the same text cycle dictionaries. */
    fun translate(scope: TextScope) {
        val text = textFor(scope)
        val dictionaries = _state.value.language?.sentenceDictionaries ?: return
        if (text.isEmpty() || dictionaries.isEmpty()) return
        val last = lastTranslation
        val index = if (last?.first == text) (last.second + 1) % dictionaries.size else 0
        lastTranslation = text to index
        val dictionary = dictionaries[index]
        viewModelScope.launch { events.send(ReadingEvent.OpenUrl(dictionary.lookupUrl(text.encodeURLParameter()))) }
    }

    /**
     * Fetches a translation for every sentence on the page, one request at a time, skipping
     * sentences already translated. Restarted whenever the page or the setting changes.
     */
    private fun translateSentences(enabled: Boolean = state.value.settings.showTranslations) {
        translationJob?.cancel()
        val s = _state.value
        if (!enabled) return
        val language = s.language ?: return
        val sentences = s.page.paragraphs.flatMap { it.sentences }.map { it.displayText }
            .filter { it.any { c -> c.isLetter() } }.distinct()
        val pending = sentences.filter { _state.value.translations[it] !is SentenceTranslation.Done }
        if (pending.isEmpty()) return
        _state.update { it.copy(translations = it.translations + pending.associateWith { SentenceTranslation.Loading }) }
        // _state carries default settings; the live ones come from the repository.
        val prefs = settingsRepository.current
        val engine = prefs.effectiveEngine(hasLocalTranslator)
        translationJob = viewModelScope.launch {
            if (engine == TranslationEngine.ARGOS && local != null) {
                val source = LanguageCodes.codeFor(language.name)
                val target = LanguageCatalog.nativeOption(prefs.nativeLanguage)
                val ready = source != null && try {
                    local.prepare(source, target.code, language.name, target.name); true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    false
                }
                if (!ready) {
                    _state.update { it.copy(translations = it.translations + pending.associateWith { SentenceTranslation.Unavailable }) }
                    return@launch
                }
            }
            for (sentence in pending) {
                val result = translator.translate(sentence, language)
                _state.update {
                    it.copy(translations = it.translations + (sentence to (result?.let { t -> SentenceTranslation.Done(t, engine) } ?: SentenceTranslation.Unavailable)))
                }
            }
        }
    }

    /**
     * Makes the audio of every sentence on the page ahead of time when the play buttons are on and
     * a local speech engine reads them, then the next page's, so turning to it is instant too;
     * restarted whenever the page or the speech settings change.
     */
    private fun prepareSpeech() {
        speechJob?.cancel()
        val s = _state.value
        val prefs = settingsRepository.current
        val code = s.language?.let { LanguageCodes.codeFor(it.name) }
        val sentences = if (prefs.showSentencePlay) s.page.paragraphs.flatMap { it.sentences }.map { it.displayText } else emptyList()
        speechJob = viewModelScope.launch {
            sentenceAudio.prepare(sentences, code, bookId)
            if (prefs.showSentencePlay) readingService.sentenceTexts(bookId, s.pageNumber + 1)?.let { sentenceAudio.prepare(it, code, bookId) }
        }
    }

    /** Drops every stored sentence translation and fetches the current page's again. */
    fun clearTranslationCache() {
        viewModelScope.launch {
            translationJob?.cancel()
            translator.clearCache()
            _state.update { it.copy(translations = emptyMap()) }
            translateSentences()
        }
    }

    // ---- settings

    fun toggleHighlights() = updateSettings { it.copy(showHighlights = !it.showHighlights) }
    fun toggleFocusMode() = updateSettings { it.copy(focusMode = !it.focusMode) }
    fun toggleSplitSentences() = updateSettings { it.copy(splitSentences = !it.splitSentences) }
    fun toggleSideBySideTranslations() = updateSettings { it.copy(sideBySideTranslations = !it.sideBySideTranslations) }
    fun toggleSentencePlay() = updateSettings { it.copy(showSentencePlay = !it.showSentencePlay) }
    fun toggleAutoPause() = updateSettings { it.copy(autoPause = !it.autoPause) }
    fun toggleSpeakWordOnClick() = updateSettings { it.copy(speakWordOnClick = !it.speakWordOnClick) }

    /** Steps the text size by [steps] tenths, within the range the reader's slider allows. */
    fun stepFontSize(steps: Int) = updateSettings {
        it.copy(readingFontScale = ((it.readingFontScale * 10).roundToInt() + steps).coerceIn(6, 25) / 10f)
    }

    fun resetFontSize() = updateSettings { it.copy(readingFontScale = UserSettings().readingFontScale) }

    fun resetLineHeight() = updateSettings { it.copy(readingLineHeight = UserSettings().readingLineHeight) }

    /** Steps the line height by [steps] tenths, within the range the reader's slider allows. */
    fun stepLineHeight(steps: Int) = updateSettings {
        it.copy(readingLineHeight = ((it.readingLineHeight * 10).roundToInt() + steps).coerceIn(10, 30) / 10f)
    }

    /** The word at [itemIndex] when a click on it should read it aloud. */
    fun wordToSpeak(itemIndex: Int): String? {
        val s = _state.value
        val item = s.items.getOrNull(itemIndex)?.takeIf { it.isWord } ?: return null
        if (s.selecting || !settingsRepository.current.speakWordOnClick) return null
        return item.text.replace(ZWS_STRING, "").takeIf { it.isNotBlank() }
    }

    /** Speech engines the drawer can offer here. */
    val speechEngines: List<SpeechEngine> = localSpeech.available

    private val _speechVoices = MutableStateFlow<List<SpeechVoice>>(emptyList())

    /** The chosen engine's usable voices for the book's language. */
    val speechVoices: StateFlow<List<SpeechVoice>> = _speechVoices.asStateFlow()

    /** The book's language code, as the speech engines name languages. */
    val speechLanguage: String? get() = state.value.language?.name?.let { LanguageCodes.codeFor(it) }

    fun loadSpeechVoices() {
        val code = speechLanguage
        val engine = localSpeech.find(settingsRepository.current.speechEngine)
        viewModelScope.launch {
            _speechVoices.value = if (code == null || engine == null) emptyList() else runCatching { engine.voices(code) }.getOrDefault(emptyList())
        }
    }

    fun setSpeechEngine(engine: SpeechEngine) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(speechEngine = engine) }
            loadSpeechVoices()
        }
    }

    fun setSpeechVoice(voiceId: String) {
        val code = speechLanguage ?: return
        updateSettings { it.copy(speechVoices = it.speechVoices + ("${it.speechEngine.name}:$code" to voiceId)) }
    }

    /** Steps the local engines' speed by [delta], kept on the Speech screen's 5 % grid between 50 % and 150 %. */
    fun adjustSpeechSpeed(delta: Float) =
        updateSettings { it.copy(speechSpeed = ((it.speechSpeed + delta).coerceIn(0.5f, 1.5f) * 20).roundToInt() / 20f) }
    fun toggleShowTranslations() {
        val enabling = !state.value.settings.showTranslations
        viewModelScope.launch {
            settingsRepository.update { it.copy(showTranslations = enabling) }
            // The combined state may not carry the new value yet, so pass it along.
            if (enabling) translateSentences(enabled = true) else translationJob?.cancel()
        }
    }
    /** Downloads the models the offline translator reported missing, then translates the page. */
    fun installOfflineModels() {
        val problem = state.value.translationError as? LocalTranslationProblem.ModelMissing ?: return
        val translator = local ?: return
        viewModelScope.launch {
            if (runCatching { translator.installModels(problem.fromCode, problem.toCode) }.isSuccess) retryOfflineTranslation()
        }
    }

    /** Asks the offline translator again after a failure, for example once the network is back. */
    fun retryOfflineTranslation() {
        _state.update { it.copy(translations = it.translations.filterValues { t -> t !is SentenceTranslation.Unavailable }) }
        translateSentences()
    }

    /** Switches this reader back to MyMemory, for languages Argos has no model for. */
    fun useOnlineEngine() {
        viewModelScope.launch {
            translationJob?.cancel()
            settingsRepository.update { it.copy(translationEngine = TranslationEngine.MYMEMORY) }
            _state.update { it.copy(translations = emptyMap()) }
            translateSentences()
        }
    }

    /** Engines the drawer can offer here: Argos only where it exists. */
    val availableEngines: List<TranslationEngine> =
        TranslationEngine.entries.filter { it != TranslationEngine.ARGOS || hasLocalTranslator }

    /** Switches engines; the page is translated again because each engine is cached apart. */
    fun setTranslationEngine(next: TranslationEngine) {
        if (next == state.value.settings.translationEngine) return
        viewModelScope.launch {
            translationJob?.cancel()
            settingsRepository.update { it.copy(translationEngine = next) }
            _state.update { it.copy(translations = emptyMap()) }
            translateSentences()
        }
    }
    fun nextTheme() = updateSettings { it.copy(themeId = AppThemes.next(it.themeId).id) }
    fun adjustFontScale(delta: Float) = updateSettings { it.copy(readingFontScale = (it.readingFontScale + delta).coerceIn(0.6f, 2.5f)) }
    fun adjustLineHeight(delta: Float) = updateSettings { it.copy(readingLineHeight = (it.readingLineHeight + delta).coerceIn(1.0f, 3.0f)) }
    fun adjustColumnWidth(delta: Int) = updateSettings { it.copy(readingColumnWidth = (it.readingColumnWidth + delta).coerceIn(320, 2000)) }

    private fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    // ---- pages, bookmarks, book

    fun addBookmark(title: String) {
        viewModelScope.launch {
            val page = books.getPage(bookId, _state.value.pageNumber) ?: return@launch
            books.addBookmark(page.id, title)
            _state.update { it.copy(flash = "Bookmark \"$title\" added") }
            delay(1500)
            _state.update { it.copy(flash = null) }
        }
    }

    fun deleteCurrentPage() {
        viewModelScope.launch {
            val s = _state.value
            if (!bookService.deletePage(bookId, s.pageNumber)) {
                _state.update { it.copy(flash = "Cannot delete the only page in the book") }
                return@launch
            }
            load(s.pageNumber, trackOpen = true)
        }
    }

    fun archiveBook() {
        viewModelScope.launch {
            bookService.archive(bookId)
            events.send(ReadingEvent.BookArchived)
        }
    }

    /** Brings an archived book back to the library and keeps reading it. */
    fun unarchiveBook() {
        viewModelScope.launch {
            bookService.unarchive(bookId)
            load(_state.value.pageNumber, trackOpen = false, keepMarked = true)
        }
    }

    fun pageTermIds(): List<Long> = _state.value.page.words.mapNotNull { it.termId }.distinct()

    fun clearFlash() = _state.update { it.copy(flash = null) }
}
