package com.idiomiko.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.idiomiko.core.domain.books.discover.BookDiscoveryService
import com.idiomiko.core.domain.books.discover.BookSource
import com.idiomiko.core.domain.books.discover.Difficulty
import com.idiomiko.core.domain.books.discover.Discovery
import com.idiomiko.core.domain.books.discover.FoundBook
import com.idiomiko.core.domain.books.discover.JudgedBook
import com.idiomiko.core.domain.books.discover.LibraryResult
import com.idiomiko.core.domain.books.discover.LibraryStatus
import com.idiomiko.core.domain.books.discover.Ranked
import com.idiomiko.core.domain.books.discover.Shelf
import com.idiomiko.core.domain.books.discover.ShelfBooks
import com.idiomiko.core.domain.books.discover.TextCoverage
import com.idiomiko.core.domain.books.discover.Topic
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.ui.components.CoverageCheck
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.state.UiEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

data class FoundBookItem(
    override val book: FoundBook,
    val check: CoverageCheck = CoverageCheck.NotChecked,
    /** The book's id once it is in the reader's library. */
    val libraryId: Long? = null,
    val adding: Boolean = false,
    val error: String? = null,
) : JudgedBook {
    override val coverage: TextCoverage? get() = (check as? CoverageCheck.Checked)?.coverage

    /** How hard the book is: by the words the reader knows once checked, otherwise by the library's own level. */
    val difficulty: Difficulty? get() = coverage?.difficulty ?: book.levelDifficulty
}

/** Where the page is: the shelves, or one shelf in full. */
sealed interface FindView {
    data object Discover : FindView
    data class AllOf(val shelf: Shelf) : FindView
}

/**
 * How a library answered when it was last asked, when that was, in its words why it failed if it did, and whether it
 * has more books than its first page.
 */
data class LibraryInfo(val status: LibraryStatus = LibraryStatus.CHECKING, val checkedAt: Instant? = null, val message: String? = null, val hasMore: Boolean = false)

/** The pages after the first of the library the page is narrowed to, loaded as the reader comes to the end of its books. */
data class MorePages(
    val books: List<FoundBookItem> = emptyList(),
    /** The last page loaded; the first comes with every other library's. */
    val page: Int = 1,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    /** Why the next page could not be loaded, if it could not. */
    val error: String? = null,
)

data class FindBooksUiState(
    val languageName: String = "",
    /** The libraries with books in the language being learned. */
    val sources: List<BookSource> = emptyList(),
    /** What is in the search box. */
    val query: String = "",
    /** What every library was last asked for; blank for their own picks. */
    val searched: String = "",
    val topic: Topic? = null,
    /** The least share of a book's words the reader asks to know, from the How hard? scale; 0 for any. */
    val minKnown: Int = Discovery.DEFAULT_MIN_KNOWN,
    /** Each library's first page for [searched], as it arrived. */
    val found: Map<BookSource, List<FoundBookItem>> = emptyMap(),
    val libraries: Map<BookSource, LibraryInfo> = emptyMap(),
    /** Whether books found are still being judged. */
    val judging: Boolean = false,
    val view: FindView = FindView.Discover,
    /** The library the page is narrowed to, its card picked; null for every library. */
    val selected: BookSource? = null,
    val more: MorePages = MorePages(),
) {
    /** The topics the language's libraries can tell apart. */
    val topics: List<Topic> get() = Discovery.topics(sources)

    /** The books on the page: the pages of the library picked, or the first page of every library as one list, each book once. */
    val books: List<FoundBookItem> get() = selected?.let { found[it].orEmpty() + more.books } ?: Discovery.merge(sources.mapNotNull { found[it] })

    /** The books of [topic], those the reader knows enough of first. */
    val results: Ranked<FoundBookItem> get() = Discovery.rank(books.filter { item -> topic.let { it == null || Discovery.matches(it, item.book, item.coverage) } }, minKnown)

    fun shelf(shelf: Shelf): ShelfBooks<FoundBookItem> = Discovery.shelf(shelf, books, minKnown)

    /** A search or a topic narrows the page to the books matching it, in place of the shelves. */
    val narrowed: Boolean get() = searched.isNotBlank() || topic != null

    /**
     * The library picked holds books the shelves would mostly leave out: short children's books, which are never
     * classics, or books judged only on request, which no shelf takes until then. Its books show as one grid instead.
     */
    val oneGrid: Boolean get() = selected?.let { it in Discovery.CHILDREN_SOURCES || !it.judgedOnSight } == true

    /** The libraries whose books are on the page. */
    private val shown: List<BookSource> get() = selected?.let(::listOf) ?: sources

    /** None of the libraries on the page has answered yet. */
    val loading: Boolean get() = shown.isNotEmpty() && shown.none { libraries[it]?.status?.let { s -> s != LibraryStatus.CHECKING } == true }

    /** Some library on the page has still to answer. */
    val waiting: Boolean get() = shown.any { (libraries[it]?.status ?: LibraryStatus.CHECKING) == LibraryStatus.CHECKING }

    /** The library picked failed when last asked. */
    val selectedDown: Boolean get() = selected?.let { libraries[it]?.status?.isDown } == true

    /** The library picked has a page of books not loaded yet. */
    val hasMore: Boolean get() = selected != null && if (more.page > 1) more.hasMore else libraries[selected]?.hasMore == true

    /** Books of the library picked are still being judged, which comes before asking it for its next page. */
    val judgingPage: Boolean get() = selected != null && books.any { it.check == CoverageCheck.Checking || (it.book.source.judgedOnSight && it.check == CoverageCheck.NotChecked) }

    /** The next page of the library picked can be asked for now. */
    val canLoadMore: Boolean get() = hasMore && !more.loading && more.error == null && !judgingPage

    /** Every library failed: the reader is offline, or all of them are down. */
    val allDown: Boolean get() = sources.isNotEmpty() && sources.all { libraries[it]?.status?.isDown == true }

    /** When the libraries were last asked. */
    val checkedAt: Instant? get() = libraries.values.mapNotNull { it.checkedAt }.maxOrNull()
}

sealed interface FindBooksEvent {
    data class Read(val bookId: Long) : FindBooksEvent
}

/**
 * Free books for the language being learned, from every online library at once. Each library's first page shows
 * as it arrives, so a slow or broken one holds up none of the others, and its books are judged by the words the
 * reader knows: all of them for a library with a cheap sample of each, the first few for the others. A library's card
 * narrows the page to its books, whose further pages load as the reader comes to their end.
 */
class FindBooksViewModel(
    private val discovery: BookDiscoveryService,
    languages: LanguageRepository,
    settings: SettingsRepository,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private val _state = MutableStateFlow(FindBooksUiState())
    val state: StateFlow<FindBooksUiState> = _state.asStateFlow()
    val events = UiEvents<FindBooksEvent>()

    private var language: Language? = null
    private var library: Map<String, Long> = emptyMap()
    private var discoverJob: Job? = null
    private val checkJobs = mutableListOf<Job>()
    private var checking = 0
    private var retryJob: Job? = null
    private var pagesJob: Job? = null
    private var pagesCheckJob: Job? = null

    init {
        viewModelScope.launch {
            settings.settings.map { it.currentLanguageId }.distinctUntilChanged().collect { id ->
                language = languages.getById(id)
                val sources = language?.let(discovery::sources).orEmpty()
                _state.update { s ->
                    // A library of another language gives way to every library of this one.
                    s.copy(languageName = language?.name.orEmpty(), sources = sources, selected = s.selected?.takeIf { it in sources }, topic = s.topic?.takeIf { it in Discovery.topics(sources) })
                }
                discover()
            }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        // Clearing the box goes back to the libraries' own picks.
        if (query.isBlank() && _state.value.searched.isNotBlank()) search()
    }

    /** Asks every library for what is in the search box. */
    fun search() {
        val query = _state.value.query.trim()
        _state.update { it.copy(searched = query, view = FindView.Discover) }
        discover()
    }

    fun setTopic(topic: Topic) = _state.update { it.copy(topic = if (it.topic == topic) null else topic, view = FindView.Discover) }

    fun setMinKnown(percent: Int) = _state.update { it.copy(minKnown = percent) }

    /** Back to the libraries' own picks: no search and no topic. */
    fun clearSearch() {
        val searched = _state.value.searched.isNotBlank()
        _state.update { it.copy(query = "", searched = "", topic = null, view = FindView.Discover) }
        if (searched) discover()
    }

    fun showAll(shelf: Shelf) = _state.update { it.copy(view = FindView.AllOf(shelf)) }

    /** Back from a shelf in full to the shelves; false when already there. */
    fun back(): Boolean {
        if (_state.value.view == FindView.Discover) return false
        _state.update { it.copy(view = FindView.Discover) }
        return true
    }

    /** Narrows the page to [source]'s books, or back to every library's when it is the one picked already. */
    fun toggleLibrary(source: BookSource) = pick(source.takeIf { it != _state.value.selected })

    /** Back to the books of every library. */
    fun clearLibrary() = pick(null)

    private fun pick(source: BookSource?) {
        pagesJob?.cancel()
        pagesCheckJob?.cancel()
        _state.update { it.copy(selected = source, more = MorePages()) }
    }

    /** Asks every library again; those that answered before answer at once from the session's pages. */
    fun retry() = discover()

    /** Asks the library picked again, on its own. */
    fun retryLibrary() {
        val source = _state.value.selected ?: return
        val lang = language ?: return
        val query = _state.value.searched
        retryJob?.cancel()
        _state.update { s -> s.copy(found = s.found - source, libraries = s.libraries + (source to LibraryInfo(checkedAt = s.libraries[source]?.checkedAt))) }
        retryJob = viewModelScope.launch {
            val result = try {
                LibraryResult(source, discovery.search(source, lang, query, 1), null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LibraryResult(source, null, e)
            }
            arrived(result, lang)
        }
    }

    /** Asks the library picked for its next page of books, once those already on the page are judged. */
    fun loadMore() {
        val current = _state.value
        val source = current.selected ?: return
        val lang = language ?: return
        // A page that failed is asked for again.
        if (current.more.error == null && !current.canLoadMore) return
        if (current.more.loading) return
        val number = current.more.page + 1
        _state.update { it.copy(more = it.more.copy(loading = true, error = null)) }
        pagesJob = viewModelScope.launch {
            try {
                val result = discovery.search(source, lang, current.searched, number)
                val items = result.books.map { book -> FoundBookItem(book, discovery.cachedCoverage(book, lang)?.let { CoverageCheck.Checked(it) } ?: CoverageCheck.NotChecked, libraryId = library[book.pageUrl]) }
                _state.update { s ->
                    val known = s.books.mapTo(HashSet()) { it.book.id }
                    s.copy(more = s.more.copy(books = s.more.books + items.filter { it.book.id !in known }, page = number, hasMore = result.hasMore, loading = false))
                }
                if (source.judgedOnSight) checkInTurn(items.filter { it.check == CoverageCheck.NotChecked }.map { it.book })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Loading page $number of $source failed" }
                _state.update { it.copy(more = it.more.copy(loading = false, error = message(e))) }
            }
        }
    }

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

    private fun discover() {
        discoverJob?.cancel()
        retryJob?.cancel()
        pagesJob?.cancel()
        pagesCheckJob?.cancel()
        checkJobs.forEach { it.cancel() }
        checkJobs.clear()
        checking = 0
        val lang = language
        _state.update { s -> s.copy(found = emptyMap(), judging = false, more = MorePages(), libraries = s.sources.associateWith { LibraryInfo(checkedAt = s.libraries[it]?.checkedAt) }) }
        if (lang == null) return
        val query = _state.value.searched
        discoverJob = viewModelScope.launch {
            library = runCatching { discovery.libraryBooks() }.getOrDefault(emptyMap())
            discovery.searchAll(lang, query).collect { arrived(it, lang) }
        }
    }

    /** A library's answer: its books on the page at once, then judged, every one or the first few. */
    private suspend fun arrived(result: LibraryResult, lang: Language) {
        result.error?.let { Logger.w(it) { "Searching ${result.source} failed" } }
        val items = result.page?.books.orEmpty().map { book ->
            FoundBookItem(book, check = discovery.cachedCoverage(book, lang)?.let { CoverageCheck.Checked(it) } ?: CoverageCheck.NotChecked, libraryId = library[book.pageUrl])
        }
        val info = LibraryInfo(result.status, clock.now(), result.error?.let(::message), hasMore = result.page?.hasMore == true)
        _state.update { s -> s.copy(found = s.found + (result.source to items), libraries = s.libraries + (result.source to info)) }
        val waiting = items.filter { it.check == CoverageCheck.NotChecked }.let { if (result.source.judgedOnSight) it else it.take(AUTO_CHECKS) }
        if (waiting.isEmpty()) return
        checking++
        _state.update { it.copy(judging = true) }
        checkJobs += viewModelScope.launch {
            try {
                for (item in waiting) checkOne(item.book)
            } finally {
                checking--
                if (checking <= 0) _state.update { it.copy(judging = false) }
            }
        }
    }

    /** Judges a library's books one at a time, after the ones already waiting. */
    private fun checkInTurn(books: List<FoundBook>) {
        val previous = pagesCheckJob
        pagesCheckJob = viewModelScope.launch {
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

    private fun item(book: FoundBook): FoundBookItem? = _state.value.let { s ->
        (s.found[book.source].orEmpty() + s.more.books).firstOrNull { it.book.id == book.id && it.book.source == book.source }
    }

    /** Changes the book wherever it shows: in its library's first page and in the pages after it. */
    private fun updateItem(book: FoundBook, change: (FoundBookItem) -> FoundBookItem) = _state.update { s ->
        fun List<FoundBookItem>.changed() = map { if (it.book.id == book.id && it.book.source == book.source) change(it) else it }
        s.copy(
            found = s.found[book.source]?.let { s.found + (book.source to it.changed()) } ?: s.found,
            more = s.more.copy(books = s.more.books.changed()),
        )
    }

    private companion object {
        /** How many books of a library without a cheap sample are judged on the shelves, each downloaded whole. */
        const val AUTO_CHECKS = 3

        fun message(e: Throwable): String = e.message?.let(::tr) ?: tr("Could not reach the library")
    }
}

/**
 * The line that ends a book from a library: where it is from and under what licence. The address
 * stays out of the text, where its parts would read as words; the book keeps it as its source.
 */
internal fun attribution(book: FoundBook): String = tr("Source: {0}, «{1}», {2}.", book.source.libraryName, book.title, licence(book.license))

/** A licence as people write it: "public domain", and "CC BY-SA 4.0" for the tag "CC-BY-SA-4.0". */
internal fun licence(license: String): String {
    if (license.equals("Public domain", ignoreCase = true)) return tr("public domain")
    if (license.equals("Non-commercial use", ignoreCase = true)) return tr("free for non-commercial use")
    if (license.equals("Public domain or open licence", ignoreCase = true)) return tr("public domain or an open licence")
    val parts = license.split('-')
    if (parts.size < 2 || !parts.first().equals("CC", ignoreCase = true)) return license
    val version = parts.last().takeIf { it.firstOrNull()?.isDigit() == true }
    val terms = parts.drop(1).dropLast(if (version != null) 1 else 0).joinToString("-")
    return listOfNotNull("CC", terms, version).joinToString(" ")
}
