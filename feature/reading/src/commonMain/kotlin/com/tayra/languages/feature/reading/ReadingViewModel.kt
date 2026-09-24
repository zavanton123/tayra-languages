package com.tayra.languages.feature.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.render.RenderedPage
import com.tayra.languages.core.domain.render.TextItem
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.BulkTermUpdate
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.TermPopup
import com.tayra.languages.core.domain.service.TermPopupBuilder
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.state.UiEvents
import com.tayra.languages.core.ui.theme.AppThemes
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What is shown in the side panel / bottom sheet. */
sealed interface ReadingPanel {
    data object None : ReadingPanel
    data class EditTerm(val termId: Long, val version: Int = 0) : ReadingPanel
    data class NewTerm(val languageId: Long, val text: String) : ReadingPanel
    data class BulkEdit(val termIds: List<Long>) : ReadingPanel
}

enum class TextScope { SENTENCE, PARAGRAPH, PAGE }

enum class CursorTarget { WORD, UNKNOWN_WORD, SENTENCE_START }

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
) : ViewModel() {

    private val _state = MutableStateFlow(ReadingUiState())
    val state: StateFlow<ReadingUiState> = combine(_state, settingsRepository.settings) { s, prefs -> s.copy(settings = prefs) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReadingUiState())
    val events = UiEvents<ReadingEvent>()

    private var popupJob: Job? = null
    private var lastTranslation: Pair<String, Int>? = null

    init {
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
                )
            }
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
        if (item.status == TermStatus.UNKNOWN && s.settings.tapSetsStatus) {
            setStatus(TermStatus.NEW_1)
        } else {
            openTerm(item)
        }
    }

    private fun openTerm(item: TextItem) {
        val termId = item.termId
        val panel = if (termId != null) ReadingPanel.EditTerm(termId) else ReadingPanel.NewTerm(item.term?.languageId ?: return, item.text)
        _state.update { it.copy(panel = panel) }
    }

    private fun showBulkPanel(marked: Set<Int>) {
        val ids = marked.mapNotNull { _state.value.items.getOrNull(it)?.termId }.distinct()
        _state.update { it.copy(panel = ReadingPanel.BulkEdit(ids)) }
    }

    fun onHover(itemIndex: Int?) {
        val s = _state.value
        if (s.marked.isNotEmpty() || s.selection != null) {
            if (itemIndex == null) hidePopup()
            return
        }
        val item = itemIndex?.let { s.items.getOrNull(it) }?.takeIf { it.isWord }
        val index = item?.let { itemIndex }
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
        val termId = _state.value.items.getOrNull(itemIndex)?.termId ?: return
        popupJob = viewModelScope.launch {
            delay(350)
            val popup = popupBuilder.build(termId)
            _state.update { s ->
                if (s.hovered == itemIndex && popup != null) s.copy(popup = PopupState(itemIndex, popup)) else s.copy(popup = null)
            }
        }
    }

    fun showPopupFor(itemIndex: Int) {
        val termId = _state.value.items.getOrNull(itemIndex)?.termId ?: return
        viewModelScope.launch {
            val popup = popupBuilder.build(termId)
            _state.update { it.copy(popup = popup?.let { p -> PopupState(itemIndex, p) }) }
        }
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
        _state.update { it.copy(selection = range, selecting = false, marked = emptySet(), panel = ReadingPanel.NewTerm(language.id, text)) }
    }

    fun cancelSelection() = _state.update { it.copy(selection = null, selecting = false) }

    fun startHoverMode() {
        hidePopup()
        _state.update { it.copy(marked = emptySet(), selection = null, selecting = false, panel = ReadingPanel.None) }
    }

    fun closePanel() = _state.update { it.copy(panel = ReadingPanel.None, selection = null, selecting = false) }

    fun moveCursor(direction: Int, target: CursorTarget) {
        val s = _state.value
        val words = s.items.withIndex().filter { (_, item) ->
            item.isWord && when (target) {
                CursorTarget.WORD -> true
                CursorTarget.UNKNOWN_WORD -> item.status == TermStatus.UNKNOWN
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
        viewModelScope.launch {
            termService.setStatus(ids, status)
            afterTermChange()
        }
    }

    fun shiftStatus(delta: Int) {
        val ids = _state.value.activeTermIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            termService.shiftStatus(ids, delta)
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

    fun openParentTerm(languageId: Long, text: String) {
        _state.update { it.copy(panel = ReadingPanel.NewTerm(languageId, text)) }
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

    // ---- settings

    fun toggleHighlights() = updateSettings { it.copy(showHighlights = !it.showHighlights) }
    fun toggleFocusMode() = updateSettings { it.copy(focusMode = !it.focusMode) }
    fun toggleTapSetsStatus() = updateSettings { it.copy(tapSetsStatus = !it.tapSetsStatus) }
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

    fun pageTermIds(): List<Long> = _state.value.page.words.mapNotNull { it.termId }.distinct()

    fun clearFlash() = _state.update { it.copy(flash = null) }
}
