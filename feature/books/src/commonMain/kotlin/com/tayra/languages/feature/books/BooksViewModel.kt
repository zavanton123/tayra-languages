package com.tayra.languages.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.DemoDataService
import com.tayra.languages.core.domain.service.StatsService
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BooksUiState(
    val loading: Boolean = true,
    val archived: Boolean = false,
    val books: List<BookListItem> = emptyList(),
    val languages: List<Language> = emptyList(),
    val currentLanguageId: Long = 0,
    val search: String = "",
    val isDemo: Boolean = false,
    val tutorialBookId: Long? = null,
    val streak: Int = 0,
    val showStreak: Boolean = false,
) {
    val hasLanguages: Boolean get() = languages.isNotEmpty()
    val filteredBooks: List<BookListItem>
        get() = books.filter { book ->
            (currentLanguageId == 0L || book.languageId == currentLanguageId) &&
                (search.isBlank() || book.title.contains(search, ignoreCase = true) || book.tags.any { it.contains(search, ignoreCase = true) })
        }
}

class BooksViewModel(
    private val archived: Boolean,
    books: BookRepository,
    languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val bookService: BookService,
    private val bookStats: BookStatsService,
    private val demoData: DemoDataService,
    private val statsService: StatsService,
) : ViewModel() {

    private val search = MutableStateFlow("")
    private val extras = MutableStateFlow(Extras())
    private val statsInFlight = HashSet<Long>()

    private data class Extras(val tutorialBookId: Long? = null, val streak: Int = 0)

    val state: StateFlow<BooksUiState> = combine(
        books.observeBooks(archived).onEach(::computeMissingStats),
        languages.observeAll(),
        settings.settings,
        search,
        extras,
    ) { bookList, languageList, prefs, query, extra ->
        BooksUiState(
            loading = false,
            archived = archived,
            books = bookList,
            languages = languageList,
            currentLanguageId = if (languageList.any { it.id == prefs.currentLanguageId }) prefs.currentLanguageId else 0,
            search = query,
            isDemo = prefs.demoDataLoaded,
            tutorialBookId = extra.tutorialBookId,
            streak = extra.streak,
            showStreak = prefs.showStreakOnHome,
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
