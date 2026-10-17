package com.tayra.languages.feature.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.books.discover.Difficulty
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.news.NewsArticle
import com.tayra.languages.core.domain.news.NewsBook
import com.tayra.languages.core.domain.news.NewsLevel
import com.tayra.languages.core.domain.news.NewsService
import com.tayra.languages.core.domain.news.NewsSource
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.CoverageCheck
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.state.UiEvents
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class NewsItem(
    val article: NewsArticle,
    val check: CoverageCheck = CoverageCheck.NotChecked,
    val opening: Boolean = false,
    val error: String? = null,
) {
    val difficulty: Difficulty? get() = (check as? CoverageCheck.Checked)?.coverage?.difficulty
}

/** How recent the articles listed are. */
enum class NewsPeriod { ANY, TODAY, WEEK }

data class NewsUiState(
    val languageName: String = "",
    val sources: List<NewsSource> = emptyList(),
    /** Null shows every source's articles. */
    val source: NewsSource? = null,
    /** Null shows the articles whatever their difficulty. */
    val difficulty: Difficulty? = null,
    /** Words the title or summary must contain; blank shows every article. */
    val query: String = "",
    val period: NewsPeriod = NewsPeriod.ANY,
    /** Only the outlets that write in simple language. */
    val easyOnly: Boolean = false,
    /** Leaves out the articles already opened. */
    val hideRead: Boolean = false,
    /** A topic the article is filed under; null shows every topic. */
    val topic: String? = null,
    val articles: List<NewsItem> = emptyList(),
    /** How many of the articles are listed; more come as the list is scrolled. */
    val listed: Int = PAGE,
    /** The articles already opened, by address. */
    val opened: Map<String, NewsBook> = emptyMap(),
    val loading: Boolean = true,
    /** The sources whose feeds could not be read this time. */
    val failed: List<NewsSource> = emptyList(),
    val loadedAt: Instant? = null,
) {
    /** The articles every filter but difficulty lets through, which is what is judged to find the difficulty. */
    val filtered: List<NewsItem>
        get() {
            val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val since = loadedAt?.let { at ->
                when (period) {
                    NewsPeriod.ANY -> null
                    NewsPeriod.TODAY -> at - 1.days
                    NewsPeriod.WEEK -> at - 7.days
                }
            }
            return articles.filter { item ->
                val article = item.article
                (source == null || article.source == source) &&
                    (!easyOnly || article.source.level == NewsLevel.EASY) &&
                    (!hideRead || article.url !in opened) &&
                    (topic == null || article.topics.any { it.equals(topic, ignoreCase = true) }) &&
                    (since == null || article.published?.let { it >= since } == true) &&
                    words.all { word -> article.title.lowercase().contains(word) || article.summary.lowercase().contains(word) }
            }
        }

    /** The articles of every filter, as many as are listed. */
    val shown: List<NewsItem> get() = matching.take(listed)

    val hasMore: Boolean get() = matching.size > listed

    private val matching: List<NewsItem> get() = filtered.filter { difficulty == null || it.difficulty == difficulty }

    /** How many of the filtered articles still wait to be judged while a difficulty is chosen. */
    val stillChecking: Int
        get() = if (difficulty == null) 0 else filtered.take(MAX_CHECKED).count { it.check == CoverageCheck.NotChecked || it.check == CoverageCheck.Checking }

    /** The topics most articles share, the most shared first; a topic of one article is no filter. */
    val topics: List<String>
        get() = articles.flatMap { item -> item.article.topics.map { it.trim() }.distinctBy { it.lowercase() } }
            .groupBy { it.lowercase() }.values.filter { it.size >= 2 }
            .sortedByDescending { it.size }.take(MAX_TOPICS).map { it.first() }

    val hasEasySources: Boolean get() = sources.any { it.level == NewsLevel.EASY }

    val filtering: Boolean
        get() = source != null || difficulty != null || query.isNotBlank() || period != NewsPeriod.ANY || easyOnly || hideRead || topic != null

    companion object {
        const val PAGE = 20

        /** With a difficulty chosen, up to this many articles are judged to find the ones at it. */
        const val MAX_CHECKED = 60
        const val MAX_TOPICS = 12
    }
}

sealed interface NewsEvent {
    data class Read(val bookId: Long) : NewsEvent
}

/**
 * The latest news of the language being learned. The articles listed are judged one after another
 * by the words the reader knows, which means reading each from its site; an article read becomes a
 * text of its own, kept with the news rather than among the books.
 */
class NewsViewModel(
    private val news: NewsService,
    languages: LanguageRepository,
    settings: SettingsRepository,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private val _state = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = _state.asStateFlow()
    val events = UiEvents<NewsEvent>()

    private var language: Language? = null
    private var loadJob: Job? = null
    private var checkJob: Job? = null

    init {
        viewModelScope.launch {
            settings.settings.map { it.currentLanguageId }.distinctUntilChanged().collect { id ->
                language = languages.getById(id)
                _state.update { NewsUiState(languageName = language?.name.orEmpty(), sources = language?.let(news::sources).orEmpty(), opened = it.opened) }
                refresh()
            }
        }
        viewModelScope.launch { news.observeOpened().collect { opened -> _state.update { it.copy(opened = opened) } } }
    }

    fun refresh() {
        val lang = language ?: run { _state.update { it.copy(loading = false) }; return }
        loadJob?.cancel()
        checkJob?.cancel()
        _state.update { it.copy(loading = true) }
        loadJob = viewModelScope.launch {
            val list = news.latest(lang)
            val checked = _state.value.articles.associateBy { it.article.url }
            _state.update { s ->
                s.copy(
                    // An article already judged keeps its judgement.
                    articles = list.articles.map { article -> checked[article.url]?.copy(article = article) ?: NewsItem(article) },
                    failed = list.failed,
                    loading = false,
                    loadedAt = clock.now(),
                    listed = NewsUiState.PAGE,
                )
            }
            checkShown()
        }
    }

    /** Reads the news again unless it was read in the last few minutes. */
    fun refreshIfOld() {
        val at = _state.value.loadedAt ?: return
        if (clock.now() - at > STALE_AFTER) refresh()
    }

    fun setSource(source: NewsSource?) = filter { it.copy(source = source) }
    fun setDifficulty(difficulty: Difficulty?) = filter { it.copy(difficulty = difficulty) }
    fun setQuery(query: String) = filter { it.copy(query = query) }
    fun setPeriod(period: NewsPeriod) = filter { it.copy(period = period) }
    fun setEasyOnly(on: Boolean) = filter { it.copy(easyOnly = on) }
    fun setHideRead(on: Boolean) = filter { it.copy(hideRead = on) }
    fun setTopic(topic: String?) = filter { it.copy(topic = topic) }

    fun clearFilters() = filter {
        it.copy(source = null, difficulty = null, query = "", period = NewsPeriod.ANY, easyOnly = false, hideRead = false, topic = null)
    }

    /** Applies a change of filter, starting again from the top of the list. */
    private fun filter(change: (NewsUiState) -> NewsUiState) {
        _state.update { change(it).copy(listed = NewsUiState.PAGE) }
        checkShown()
    }

    fun showMore() {
        if (!_state.value.hasMore) return
        _state.update { it.copy(listed = it.listed + NewsUiState.PAGE) }
        checkShown()
    }

    /** Opens the article to read, made into a text the first time. */
    fun open(item: NewsItem) {
        val lang = language ?: return
        if (item.opening) return
        updateItem(item.article) { it.copy(opening = true, error = null) }
        viewModelScope.launch {
            try {
                val bookId = news.open(item.article, lang, attribution(item.article))
                updateItem(item.article) { it.copy(opening = false) }
                events.send(NewsEvent.Read(bookId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Opening ${item.article.url} failed" }
                updateItem(item.article) { it.copy(opening = false, error = e.message?.let(::tr) ?: tr("Could not open the article")) }
            }
        }
    }

    /** Judges the listed articles not judged yet, one at a time. */
    private fun checkShown() {
        val lang = language ?: return
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            // The filtered ones whatever their difficulty, as the difficulty is what is being found; with a
            // difficulty chosen, beyond the ones listed, so the list fills with articles at it.
            val waiting = _state.value.let { s -> s.filtered.take(if (s.difficulty != null) NewsUiState.MAX_CHECKED else s.listed) }
                .filter { it.check == CoverageCheck.NotChecked }
            for (item in waiting) {
                updateItem(item.article) { it.copy(check = CoverageCheck.Checking) }
                val check = try {
                    news.coverage(item.article, lang)?.let { CoverageCheck.Checked(it) } ?: CoverageCheck.Failed(tr("This language cannot be checked"))
                } catch (e: CancellationException) {
                    updateItem(item.article) { it.copy(check = CoverageCheck.NotChecked) }
                    throw e
                } catch (e: Exception) {
                    Logger.w(e) { "Checking ${item.article.url} failed" }
                    CoverageCheck.Failed(e.message?.let(::tr) ?: tr("Could not read the article"))
                }
                updateItem(item.article) { it.copy(check = check) }
            }
        }
    }

    private fun updateItem(article: NewsArticle, change: (NewsItem) -> NewsItem) =
        _state.update { s -> s.copy(articles = s.articles.map { if (it.article.url == article.url) change(it) else it }) }

    private companion object {
        val STALE_AFTER = 15.minutes
    }
}

/** The line that ends an article: where it is from. The address stays out of the text, where its parts would read as words; the text keeps it as its source. */
internal fun attribution(article: NewsArticle): String = tr("Source: {0}.", article.source.name)
