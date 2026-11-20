package com.idiomiko.feature.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.idiomiko.core.domain.books.discover.Difficulty
import com.idiomiko.core.domain.books.discover.TextCoverage
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.news.NewsArticle
import com.idiomiko.core.domain.news.NewsBook
import com.idiomiko.core.domain.news.NewsService
import com.idiomiko.core.domain.news.NewsSource
import com.idiomiko.core.domain.news.NewsTopic
import com.idiomiko.core.domain.news.NewsTopics
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.ui.components.CoverageCheck
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.state.UiEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    /** The picture the article's page names, found while judging it, for an article whose feed gives none. */
    val pagePicture: String? = null,
) {
    val coverage: TextCoverage? get() = (check as? CoverageCheck.Checked)?.coverage
    val difficulty: Difficulty? get() = coverage?.difficulty
    val picture: String? get() = article.imageUrl ?: pagePicture

    /** The topics of the article, from the sections its feed files it under. */
    val topics: List<NewsTopic> = NewsTopics.of(article)
}

/** How recent the articles shown are; a day is the last 24 hours. */
enum class NewsPeriod { TODAY, WEEK, ANY }

/** The order of the articles: the share of their words known, or how recent they are. */
enum class NewsSort { EASIEST, NEWEST }

/**
 * The front page of the articles the filters leave: the lead, the best of the newest by the order chosen, the
 * cards beside it, then the rest as rows, [hidden] of them waiting behind "See all".
 */
data class FrontPage(val lead: NewsItem?, val cards: List<NewsItem>, val rows: List<NewsItem>, val hidden: Int)

data class NewsUiState(
    val languageName: String = "",
    val sources: List<NewsSource> = emptyList(),
    /** The names of the sources left out; none shows every source's articles. */
    val hiddenSources: Set<String> = emptySet(),
    /** Words the title or summary must contain; blank shows every article. */
    val query: String = "",
    val period: NewsPeriod = NewsPeriod.TODAY,
    /** Whether the reader chose the period; until then a day with no news shows the week's. */
    val periodChosen: Boolean = false,
    val sort: NewsSort = NewsSort.EASIEST,
    /** Leaves out the articles already opened. */
    val hideRead: Boolean = false,
    /** A topic the article is filed under; null shows every topic. */
    val topic: NewsTopic? = null,
    val articles: List<NewsItem> = emptyList(),
    /** How many of the rows after the cards are shown; "See all" shows the rest. */
    val rows: Int = ROWS,
    /** The articles already opened, by address. */
    val opened: Map<String, NewsBook> = emptyMap(),
    val loading: Boolean = true,
    /** The sources whose feeds could not be read this time. */
    val failed: List<NewsSource> = emptyList(),
    val loadedAt: Instant? = null,
) {
    /** The language of the sources, for the topics' names. */
    val languageCode: String? get() = sources.firstOrNull()?.languageCode

    /** The period shown: the one chosen, or the week while the day has no news yet. */
    val shownPeriod: NewsPeriod
        get() = if (!periodChosen && period == NewsPeriod.TODAY && loadedAt != null && articles.isNotEmpty() && articles.none { it.isFrom(NewsPeriod.TODAY, loadedAt) }) NewsPeriod.WEEK else period

    /** Every filter but the topic, so the topics offered are those the other filters leave articles in. */
    private val beforeTopic: List<NewsItem>
        get() {
            val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val period = shownPeriod
            return articles.filter { item ->
                val article = item.article
                article.source.name !in hiddenSources &&
                    (!hideRead || article.url !in opened) &&
                    (loadedAt == null || item.isFrom(period, loadedAt)) &&
                    words.all { word -> article.title.lowercase().contains(word) || article.summary.lowercase().contains(word) }
            }
        }

    /** The articles every filter lets through, in the order chosen. */
    val filtered: List<NewsItem> get() = sorted(beforeTopic.filter { topic == null || topic in it.topics })

    private fun sorted(items: List<NewsItem>): List<NewsItem> {
        val newest = items.sortedByDescending { it.article.published ?: Instant.DISTANT_PAST }
        if (sort == NewsSort.NEWEST) return newest
        // The judged first, the best known first; then those still to judge, newest first, so the top settles as they are judged.
        val (judged, waiting) = newest.partition { it.coverage != null }
        return judged.sortedByDescending { item -> item.coverage!!.let { it.known * 1000L / it.words.coerceAtLeast(1) } } + waiting
    }

    /** The front page: the lead from the newest few, preferring one not read yet, then the cards and the rows in order. */
    val front: FrontPage
        get() {
            val all = filtered
            val pool = all.sortedByDescending { it.article.published ?: Instant.DISTANT_PAST }.take(LEAD_POOL).toSet()
            val lead = all.firstOrNull { it in pool && it.article.url !in opened } ?: all.firstOrNull()
            val rest = all - setOfNotNull(lead)
            val rows = rest.drop(CARDS)
            return FrontPage(lead, rest.take(CARDS), rows.take(this.rows), (rows.size - this.rows).coerceAtLeast(0))
        }

    /** The topics the articles left by the other filters are filed under, in a fixed order, and the one chosen. */
    val topics: List<NewsTopic>
        get() {
            val code = languageCode ?: return emptyList()
            val present = beforeTopic.flatMapTo(HashSet()) { it.topics } + listOfNotNull(topic)
            return NewsTopic.entries.filter { it in present && NewsTopics.label(it, code) != null }
        }

    /** How many filters of the phone's sheet are set, for its Filters button. */
    val activeFilters: Int
        get() = listOf(hiddenSources.isNotEmpty(), periodChosen && period != NewsPeriod.TODAY, hideRead).count { it }

    val filtering: Boolean
        get() = hiddenSources.isNotEmpty() || query.isNotBlank() || hideRead || topic != null || (periodChosen && period != NewsPeriod.TODAY)

    /** Every source failed, so there is nothing to show but the way to try again. */
    val allFailed: Boolean get() = !loading && articles.isEmpty() && failed.isNotEmpty()

    companion object {
        /** The lead is the best of this many newest articles. */
        const val LEAD_POOL = 12
        const val CARDS = 4
        const val ROWS = 10
    }
}

private fun NewsItem.isFrom(period: NewsPeriod, now: Instant): Boolean = when (period) {
    NewsPeriod.ANY -> true
    NewsPeriod.TODAY -> article.published?.let { it >= now - 1.days } == true
    NewsPeriod.WEEK -> article.published?.let { it >= now - 7.days } == true
}

sealed interface NewsEvent {
    data class Read(val bookId: Long) : NewsEvent
}

/**
 * The latest news of the language being learned, as a front page. Each article is judged by the words the reader
 * knows, which for most sources means reading it from its site, so articles are judged a few at a time: the newest,
 * from which the lead is picked, and the cards at once, and the others as they come into view. An article read
 * becomes a text of its own, kept with the news rather than among the books.
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

    /** The articles waiting to be judged, the most wanted first, and the judging under way. */
    private val waiting = ArrayDeque<String>()
    private var judging: CompletableJob = SupervisorJob(viewModelScope.coroutineContext[Job])
    private var workers = 0

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
        stopJudging()
        _state.update { it.copy(loading = true) }
        loadJob = viewModelScope.launch {
            val list = news.latest(lang)
            val known = _state.value.articles.associateBy { it.article.url }
            _state.update { s ->
                s.copy(
                    // An article already judged keeps its judgement and the picture found, and one cut short is judged again.
                    articles = list.articles.map { article ->
                        known[article.url]?.let { if (it.check == CoverageCheck.Checking) it.copy(check = CoverageCheck.NotChecked) else it }?.copy(article = article) ?: NewsItem(article)
                    },
                    failed = list.failed,
                    loading = false,
                    loadedAt = clock.now(),
                    rows = NewsUiState.ROWS,
                )
            }
            judgeFront()
        }
    }

    /** Reads the news again unless it was read in the last few minutes. */
    fun refreshIfOld() {
        val at = _state.value.loadedAt ?: return
        if (clock.now() - at > STALE_AFTER) refresh()
    }

    fun setQuery(query: String) = filter { it.copy(query = query) }
    fun setPeriod(period: NewsPeriod) = filter { it.copy(period = period, periodChosen = true) }
    fun setSort(sort: NewsSort) = filter { it.copy(sort = sort) }
    fun setHideRead(on: Boolean) = filter { it.copy(hideRead = on) }
    fun setTopic(topic: NewsTopic?) = filter { it.copy(topic = topic) }

    /** Shows or leaves out a source's articles; the last source shown stays. */
    fun toggleSource(source: NewsSource) = filter { s ->
        val hidden = if (source.name in s.hiddenSources) s.hiddenSources - source.name else s.hiddenSources + source.name
        if (s.sources.all { it.name in hidden }) s else s.copy(hiddenSources = hidden)
    }

    fun showAllSources() = filter { it.copy(hiddenSources = emptySet()) }

    fun clearFilters() = filter {
        it.copy(hiddenSources = emptySet(), query = "", period = NewsPeriod.TODAY, periodChosen = false, hideRead = false, topic = null)
    }

    /** Shows every row of the front page. */
    fun showAll() = _state.update { it.copy(rows = Int.MAX_VALUE) }

    /** Applies a change of filter, starting again from the top of the page. */
    private fun filter(change: (NewsUiState) -> NewsUiState) {
        _state.update { change(it).copy(rows = NewsUiState.ROWS) }
        judgeFront()
    }

    /** An article came into view: it is judged next, if it was not yet. */
    fun seen(item: NewsItem) = judge(listOf(item.article.url), first = true)

    /** Opens the article to read, made into a text the first time. */
    fun open(item: NewsItem) {
        val lang = language ?: return
        if (item.opening) return
        updateItem(item.article.url) { it.copy(opening = true, error = null) }
        viewModelScope.launch {
            try {
                val bookId = news.open(item.article, lang, attribution(item.article))
                updateItem(item.article.url) { it.copy(opening = false) }
                events.send(NewsEvent.Read(bookId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Opening ${item.article.url} failed" }
                updateItem(item.article.url) { it.copy(opening = false, error = e.message?.let(::tr) ?: tr("Could not open the article")) }
            }
        }
    }

    /** The newest articles the filters leave, from which the lead is picked, and the cards beside it, are judged first. */
    private fun judgeFront() {
        val all = _state.value.filtered
        val newest = all.sortedByDescending { it.article.published ?: Instant.DISTANT_PAST }.take(NewsUiState.LEAD_POOL)
        waiting.clear()
        judge((newest + all.take(NewsUiState.CARDS + 1)).map { it.article.url }.distinct(), first = false)
    }

    private fun judge(urls: List<String>, first: Boolean) {
        val items = _state.value.articles.associateBy { it.article.url }
        val wanted = urls.filter { url -> items[url]?.check == CoverageCheck.NotChecked && url !in waiting }
        if (first) wanted.asReversed().forEach(waiting::addFirst) else wanted.forEach(waiting::addLast)
        while (workers < WORKERS && waiting.isNotEmpty()) {
            workers++
            val job = judging
            viewModelScope.launch(job) {
                try {
                    work()
                } finally {
                    if (job === judging) workers--
                }
            }
        }
    }

    /** Judges the articles waiting, one after another, until none waits. */
    private suspend fun work() {
        val lang = language ?: return
        while (true) {
            val url = waiting.removeFirstOrNull() ?: return
            val item = _state.value.articles.firstOrNull { it.article.url == url } ?: continue
            if (item.check != CoverageCheck.NotChecked) continue
            updateItem(url) { it.copy(check = CoverageCheck.Checking) }
            val (check, picture) = try {
                news.judge(item.article, lang)?.let { CoverageCheck.Checked(it.coverage) to it.imageUrl } ?: (CoverageCheck.Failed(tr("This language cannot be checked")) to null)
            } catch (e: CancellationException) {
                updateItem(url) { it.copy(check = CoverageCheck.NotChecked) }
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Checking $url failed" }
                CoverageCheck.Failed(e.message?.let(::tr) ?: tr("Could not read the article")) to null
            }
            updateItem(url) { it.copy(check = check, pagePicture = picture) }
        }
    }

    private fun stopJudging() {
        judging.cancel()
        judging = SupervisorJob(viewModelScope.coroutineContext[Job])
        workers = 0
        waiting.clear()
    }

    private fun updateItem(url: String, change: (NewsItem) -> NewsItem) =
        _state.update { s -> s.copy(articles = s.articles.map { if (it.article.url == url) change(it) else it }) }

    private companion object {
        val STALE_AFTER = 15.minutes

        /** How many articles are read and judged at once. */
        const val WORKERS = 3
    }
}

/** The line that ends an article: where it is from. The address stays out of the text, where its parts would read as words; the text keeps it as its source. */
internal fun attribution(article: NewsArticle): String = tr("Source: {0}.", article.source.name)
