package com.tayra.languages.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.books.discover.BookDiscoveryService
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.Difficulty
import com.tayra.languages.core.domain.books.discover.FoundBook
import com.tayra.languages.core.domain.books.discover.TextCoverage
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.CoverageCheck
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoundBookItem(
    val book: FoundBook,
    val check: CoverageCheck = CoverageCheck.NotChecked,
    /** The book's id once it is in the reader's library. */
    val libraryId: Long? = null,
    val adding: Boolean = false,
    val error: String? = null,
) {
    /** How hard the book is: by the words the reader knows once checked, otherwise by the library's own level. */
    val difficulty: Difficulty? get() = (check as? CoverageCheck.Checked)?.coverage?.difficulty ?: book.levelDifficulty
}

data class FindBooksUiState(
    val languageName: String = "",
    val source: BookSource = BookSource.GUTENBERG,
    /** The libraries with books in the language being learned. */
    val sources: List<BookSource> = emptyList(),
    val query: String = "",
    /** Null shows the books whatever their difficulty. */
    val difficulty: Difficulty? = null,
    val books: List<FoundBookItem> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    /** Why the library could not be searched, if it could not. */
    val error: String? = null,
) {
    val shown: List<FoundBookItem> get() = if (difficulty == null) books else books.filter { it.difficulty == difficulty }
}

sealed interface FindBooksEvent {
    data class Read(val bookId: Long) : FindBooksEvent
}

/**
 * Free books for the language being learned, from Project Gutenberg, Wikisource or the Global
 * Digital Library's graded readers. Gutenberg and Wikisource books are judged as they are listed,
 * one after another, from a small sample each; a Global Digital Library book is judged when asked,
 * since that means downloading it whole.
 */
class FindBooksViewModel(
    private val discovery: BookDiscoveryService,
    languages: LanguageRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(FindBooksUiState())
    val state: StateFlow<FindBooksUiState> = _state.asStateFlow()
    val events = UiEvents<FindBooksEvent>()

    private var language: Language? = null
    private var page = 1
    private var loadJob: Job? = null
    private var checkJob: Job? = null
    private var library: Map<String, Long> = emptyMap()

    init {
        viewModelScope.launch {
            settings.settings.map { it.currentLanguageId }.distinctUntilChanged().collect { id ->
                language = languages.getById(id)
                val sources = language?.let(discovery::sources).orEmpty()
                // A library of another language gives way to the first of this one's.
                _state.update { s -> s.copy(languageName = language?.name.orEmpty(), sources = sources, source = s.source.takeIf { it in sources } ?: sources.firstOrNull() ?: BookSource.GUTENBERG) }
                reload()
            }
        }
    }

    fun setSource(source: BookSource) {
        if (source == _state.value.source) return
        _state.update { it.copy(source = source, difficulty = null) }
        reload()
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        // Typing waits for a pause before asking the library.
        reload(delayMillis = SEARCH_DELAY)
    }

    fun setDifficulty(difficulty: Difficulty?) = _state.update { it.copy(difficulty = difficulty) }

    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.loadingMore || current.loading) return
        loadJob = viewModelScope.launch { load(page + 1) }
    }

    fun retry() = reload()

    /** Judges a book by the words the reader knows. */
    fun check(book: FoundBook) {
        viewModelScope.launch { checkOne(book) }
    }

    fun add(book: FoundBook) {
        val lang = language ?: return
        if (item(book)?.adding == true) return
        updateItem(book) { it.copy(adding = true, error = null) }
        viewModelScope.launch {
            try {
                val id = discovery.addToLibrary(book, lang, ::attribution)
                library = library + (book.pageUrl to id)
                updateItem(book) { it.copy(adding = false, libraryId = id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Adding ${book.pageUrl} failed" }
                updateItem(book) { it.copy(adding = false, error = e.message?.let(::tr) ?: tr("Could not download the book")) }
            }
        }
    }

    fun read(item: FoundBookItem) {
        item.libraryId?.let { id -> viewModelScope.launch { events.send(FindBooksEvent.Read(id)) } }
    }

    private fun reload(delayMillis: Long = 0) {
        loadJob?.cancel()
        checkJob?.cancel()
        _state.update { it.copy(books = emptyList(), loading = true, error = null, hasMore = false) }
        loadJob = viewModelScope.launch {
            if (delayMillis > 0) delay(delayMillis)
            library = runCatching { discovery.libraryBooks() }.getOrDefault(emptyMap())
            load(1)
        }
    }

    private suspend fun load(number: Int) {
        val lang = language ?: run { _state.update { it.copy(loading = false) }; return }
        val current = _state.value
        if (number > 1) _state.update { it.copy(loadingMore = true) }
        try {
            val result = discovery.search(current.source, lang, current.query, number)
            page = number
            val items = result.books.map { FoundBookItem(it, libraryId = library[it.pageUrl]) }
            _state.update { s -> s.copy(books = s.books + items.filter { new -> s.books.none { it.book.id == new.book.id } }, loading = false, loadingMore = false, hasMore = result.hasMore, error = null) }
            if (current.source.judgedOnSight) checkInTurn(items.map { it.book })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Searching ${current.source} failed" }
            _state.update { it.copy(loading = false, loadingMore = false, error = e.message?.let(::tr) ?: tr("Could not reach the library")) }
        }
    }

    /** Judges the books one at a time, after the ones already waiting. */
    private fun checkInTurn(books: List<FoundBook>) {
        val previous = checkJob
        checkJob = viewModelScope.launch {
            previous?.join()
            for (book in books) checkOne(book)
        }
    }

    private suspend fun checkOne(book: FoundBook) {
        val lang = language ?: return
        val current = item(book) ?: return
        if (current.check is CoverageCheck.Checked || current.check is CoverageCheck.Checking) return
        updateItem(book) { it.copy(check = CoverageCheck.Checking) }
        val check = try {
            discovery.coverage(book, lang)?.let { CoverageCheck.Checked(it) } ?: CoverageCheck.Failed(tr("This language cannot be checked"))
        } catch (e: CancellationException) {
            updateItem(book) { it.copy(check = CoverageCheck.NotChecked) }
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Checking ${book.pageUrl} failed" }
            CoverageCheck.Failed(e.message?.let(::tr) ?: tr("Could not download the book"))
        }
        updateItem(book) { it.copy(check = check) }
    }

    private fun item(book: FoundBook): FoundBookItem? = _state.value.books.firstOrNull { it.book.id == book.id && it.book.source == book.source }

    private fun updateItem(book: FoundBook, change: (FoundBookItem) -> FoundBookItem) =
        _state.update { s -> s.copy(books = s.books.map { if (it.book.id == book.id && it.book.source == book.source) change(it) else it }) }

    private companion object {
        const val SEARCH_DELAY = 500L
    }
}

/**
 * The line that ends a book from a library: where it is from and under what licence. The address
 * stays out of the text, where its parts would read as words; the book keeps it as its source.
 */
internal fun attribution(book: FoundBook): String = tr("Source: {0}, «{1}», {2}.", book.source.libraryName, book.title, licence(book.license))

/** A licence as people write it: "public domain", and "CC BY-SA 4.0" for the tag "CC-BY-SA-4.0". */
private fun licence(license: String): String {
    if (license.equals("Public domain", ignoreCase = true)) return tr("public domain")
    if (license.equals("Non-commercial use", ignoreCase = true)) return tr("free for non-commercial use")
    val parts = license.split('-')
    if (parts.size < 2 || !parts.first().equals("CC", ignoreCase = true)) return license
    val version = parts.last().takeIf { it.firstOrNull()?.isDigit() == true }
    val terms = parts.drop(1).dropLast(if (version != null) 1 else 0).joinToString("-")
    return listOfNotNull("CC", terms, version).joinToString(" ")
}
