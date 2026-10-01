package com.tayra.languages.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermListFilter
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.DemoDataService
import com.tayra.languages.core.domain.service.StatsService
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Instant

enum class BookSort(val label: String) { RECENT("Recently read"), TITLE("Title"), LANGUAGE("Language"), MASTERY("Mastery") }

/** Buckets of [masteryPercent]; books without stats only match [ALL]. */
enum class MasteryFilter(val label: String, val range: IntRange) {
    ALL("All mastery levels", 0..100),
    NEW("Mostly new", 0..29),
    LEARNING("In progress", 30..69),
    KNOWN("Mostly known", 70..100),
}

enum class BooksView { LIST, GRID }

/** Share of the book's distinct terms that are no longer unknown, or null before stats exist. */
val BookListItem.masteryPercent: Int?
    get() = stats?.takeIf { it.distinctTerms > 0 }?.let { 100 - it.unknownPercent }

data class BooksUiState(
    val loading: Boolean = true,
    val archived: Boolean = false,
    val books: List<BookListItem> = emptyList(),
    val languages: List<Language> = emptyList(),
    val currentLanguageId: Long = 0,
    val search: String = "",
    val sort: BookSort = BookSort.RECENT,
    val mastery: MasteryFilter = MasteryFilter.ALL,
    val view: BooksView = BooksView.LIST,
    val isDemo: Boolean = false,
    val tutorialBookId: Long? = null,
    val streak: Int = 0,
    val showStreak: Boolean = false,
    val wordsLearned: Int = 0,
) {
    val filteredBooks: List<BookListItem>
        get() {
            val matching = books.filter { book ->
                (currentLanguageId == 0L || book.languageId == currentLanguageId) &&
                    (search.isBlank() || book.title.contains(search, ignoreCase = true) || book.tags.any { it.contains(search, ignoreCase = true) }) &&
                    (mastery == MasteryFilter.ALL || book.masteryPercent?.let { it in mastery.range } == true)
            }
            return when (sort) {
                BookSort.RECENT -> matching.sortedWith(compareByDescending<BookListItem> { it.lastOpened ?: Instant.DISTANT_PAST }.thenBy { it.title.lowercase() })
                BookSort.TITLE -> matching.sortedBy { it.title.lowercase() }
                BookSort.LANGUAGE -> matching.sortedWith(compareBy<BookListItem> { it.languageName }.thenBy { it.title.lowercase() })
                BookSort.MASTERY -> matching.sortedWith(compareByDescending<BookListItem> { it.masteryPercent ?: -1 }.thenBy { it.title.lowercase() })
            }
        }

    val languageCount: Int get() = books.distinctBy { it.languageId }.size
}

class BooksViewModel(
    private val archived: Boolean,
    books: BookRepository,
    languages: LanguageRepository,
    terms: TermRepository,
    private val settings: SettingsRepository,
    private val bookService: BookService,
    private val bookStats: BookStatsService,
    private val demoData: DemoDataService,
    private val statsService: StatsService,
) : ViewModel() {

    private val search = MutableStateFlow("")
    private val sort = MutableStateFlow(BookSort.RECENT)
    private val mastery = MutableStateFlow(MasteryFilter.ALL)
    private val view = MutableStateFlow(BooksView.LIST)
    private val extras = MutableStateFlow(Extras())
    private val statsInFlight = HashSet<Long>()

    private data class Extras(val tutorialBookId: Long? = null, val streak: Int = 0)

    private data class Options(val sort: BookSort, val mastery: MasteryFilter, val view: BooksView, val extras: Extras, val wordsLearned: Int)

    private val wordsLearned = terms
        .observeList(TermListFilter(minStatus = TermStatus.WELL_KNOWN, maxStatus = TermStatus.WELL_KNOWN), TermListSort(), 0, 1)
        .map { it.totalCount }
        .catch { e -> Logger.w(e) { "Counting learned terms failed" }; emit(0) }

    private val options = combine(sort, mastery, view, extras, wordsLearned) { s, m, v, e, w -> Options(s, m, v, e, w) }

    val state: StateFlow<BooksUiState> = combine(
        books.observeBooks(archived).onEach(::computeMissingStats),
        languages.observeAll(),
        settings.settings,
        search,
        options,
    ) { bookList, languageList, prefs, query, opts ->
        BooksUiState(
            loading = false,
            archived = archived,
            books = bookList,
            languages = languageList,
            currentLanguageId = if (languageList.any { it.id == prefs.currentLanguageId }) prefs.currentLanguageId else 0,
            search = query,
            sort = opts.sort,
            mastery = opts.mastery,
            view = opts.view,
            isDemo = prefs.demoDataLoaded,
            tutorialBookId = opts.extras.tutorialBookId,
            streak = opts.extras.streak,
            showStreak = prefs.showStreakOnHome,
            wordsLearned = opts.wordsLearned,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BooksUiState(archived = archived))

    init {
        refreshExtras()
    }

    private fun refreshExtras() {
        viewModelScope.launch {
            extras.value = Extras(tutorialBookId = demoData.tutorialBookId(), streak = statsService.streak())
        }
    }

    private fun computeMissingStats(list: List<BookListItem>) {
        val missing = list.filter { it.stats == null && it.id !in statsInFlight }
        if (missing.isEmpty()) return
        statsInFlight.addAll(missing.map { it.id })
        viewModelScope.launch {
            for (book in missing) {
                try {
                    bookStats.stats(book.id)
                } catch (e: Exception) {
                    Logger.w(e) { "Stats failed for book ${book.id}" }
                } finally {
                    statsInFlight.remove(book.id)
                }
            }
        }
    }

    fun setSearch(query: String) {
        search.value = query
    }

    fun setSort(value: BookSort) {
        sort.value = value
    }

    fun setMastery(value: MasteryFilter) {
        mastery.value = value
    }

    fun setView(value: BooksView) {
        view.value = value
    }

    fun setLanguageFilter(languageId: Long) {
        viewModelScope.launch { settings.update { it.copy(currentLanguageId = languageId) } }
    }

    fun archive(bookId: Long) = viewModelScope.launch { bookService.archive(bookId) }
    fun unarchive(bookId: Long) = viewModelScope.launch { bookService.unarchive(bookId) }
    fun delete(bookId: Long) = viewModelScope.launch { bookService.delete(bookId) }

    fun refreshAllStats() = viewModelScope.launch {
        for (book in state.value.books) bookStats.markStale(book.id)
        bookStats.refreshAll()
    }

    fun dismissDemoNotice() = viewModelScope.launch { demoData.dismissDemoFlag() }

    fun wipeDatabase() = viewModelScope.launch {
        demoData.wipeDatabase()
        refreshExtras()
    }
}
