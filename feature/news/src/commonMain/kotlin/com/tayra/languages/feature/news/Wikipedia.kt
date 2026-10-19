package com.tayra.languages.feature.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.books.discover.Difficulty
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.news.NewsArticle
import com.tayra.languages.core.domain.news.NewsBook
import com.tayra.languages.core.domain.news.NewsService
import com.tayra.languages.core.domain.news.Wiki
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.AppFilterChip
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.CoverageCheck
import com.tayra.languages.core.ui.components.CoverageLine
import com.tayra.languages.core.ui.components.DifficultyFilter
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.state.UiEvents
import com.tayra.languages.core.ui.theme.AppShapes
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

/** How long a whole article is, by its size on the wiki. */
enum class ArticleLength(val below: Int) {
    ANY(Int.MAX_VALUE), SHORT(10_000), MEDIUM(50_000), LONG(Int.MAX_VALUE);

    companion object {
        fun of(size: Int?): ArticleLength? = when {
            size == null -> null
            size < SHORT.below -> SHORT
            size < MEDIUM.below -> MEDIUM
            else -> LONG
        }
    }
}

data class WikiUiState(
    val languageName: String = "",
    /** The wikis with an edition in the language, Wikipedia first. */
    val wikis: List<Wiki> = emptyList(),
    val wiki: Wiki = Wiki.WIKIPEDIA,
    /** Words to look for; blank lists the most read articles. */
    val query: String = "",
    val length: ArticleLength = ArticleLength.ANY,
    val difficulty: Difficulty? = null,
    val hideRead: Boolean = false,
    val articles: List<NewsItem> = emptyList(),
    val opened: Map<String, NewsBook> = emptyMap(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
) {
    val shown: List<NewsItem>
        get() = articles.filter { item ->
            (length == ArticleLength.ANY || ArticleLength.of(item.article.length) == length) &&
                (difficulty == null || item.difficulty == difficulty) &&
                (!hideRead || item.article.url !in opened)
        }
}

sealed interface WikiEvent {
    data class Read(val bookId: Long) : WikiEvent
}

/**
 * The language's wikis, one at a time: the articles read most yesterday (or some at random, where
 * views are not counted), or those a search finds, each judged by its introduction, which is also
 * what is read unless the whole article is asked for.
 */
class WikipediaViewModel(
    private val news: NewsService,
    languages: LanguageRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WikiUiState())
    val state: StateFlow<WikiUiState> = _state.asStateFlow()
    val events = UiEvents<WikiEvent>()

    private var language: Language? = null
    private var page = 1
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            settings.settings.map { it.currentLanguageId }.distinctUntilChanged().collect { id ->
                language = languages.getById(id)
                val wikis = language?.let(news::wikis).orEmpty()
                _state.update { WikiUiState(languageName = language?.name.orEmpty(), wikis = wikis, wiki = it.wiki.takeIf { wiki -> wiki in wikis } ?: Wiki.WIKIPEDIA, opened = it.opened) }
                reload()
            }
        }
        viewModelScope.launch { news.observeOpened().collect { opened -> _state.update { it.copy(opened = opened) } } }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        // Typing waits for a pause before asking the wiki.
        reload(delayMillis = SEARCH_DELAY)
    }

    fun setWiki(wiki: Wiki) {
        if (wiki == _state.value.wiki) return
        _state.update { it.copy(wiki = wiki) }
        reload()
    }

    fun setLength(length: ArticleLength) = _state.update { it.copy(length = length) }
    fun setDifficulty(difficulty: Difficulty?) = _state.update { it.copy(difficulty = difficulty) }
    fun setHideRead(on: Boolean) = _state.update { it.copy(hideRead = on) }

    fun refresh() = reload()

    fun loadMore() {
        val s = _state.value
        if (!s.hasMore || s.loading || s.loadingMore) return
        loadJob = viewModelScope.launch { load(page + 1) }
    }

    /** Opens the article's introduction to read. */
    fun open(item: NewsItem) = opening(item) { lang -> news.open(item.article, lang, wikiAttribution(item.article)) }

    /** Opens the whole article to read. */
    fun openWhole(item: NewsItem) = opening(item) { lang ->
        news.openWhole(item.article, lang, tr("{0} (whole article)", item.article.title), wikiAttribution(item.article))
    }

    private fun opening(item: NewsItem, open: suspend (Language) -> Long) {
        val lang = language ?: return
        if (item.opening) return
        updateItem(item.article) { it.copy(opening = true, error = null) }
        viewModelScope.launch {
            try {
                val bookId = open(lang)
                updateItem(item.article) { it.copy(opening = false) }
                events.send(WikiEvent.Read(bookId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Opening ${item.article.url} failed" }
                updateItem(item.article) { it.copy(opening = false, error = e.message?.let(::tr) ?: tr("Could not open the article")) }
            }
        }
    }

    private fun reload(delayMillis: Long = 0) {
        loadJob?.cancel()
        _state.update { it.copy(articles = emptyList(), loading = true, error = null, hasMore = false) }
        loadJob = viewModelScope.launch {
            if (delayMillis > 0) delay(delayMillis)
            load(1)
        }
    }

    private suspend fun load(number: Int) {
        val lang = language ?: run { _state.update { it.copy(loading = false) }; return }
        if (number > 1) _state.update { it.copy(loadingMore = true) }
        try {
            val result = _state.value.let { news.wiki(lang, it.wiki, it.query, number) }
            page = number
            val items = result.articles.map { article -> NewsItem(article, judged(article, lang)) }
            _state.update { s ->
                s.copy(articles = s.articles + items.filter { new -> s.articles.none { it.article.url == new.article.url } }, loading = false, loadingMore = false, hasMore = result.hasMore)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Reading ${_state.value.wiki.siteName} failed" }
            _state.update { it.copy(loading = false, loadingMore = false, error = e.message?.let(::tr) ?: tr("Could not reach the wiki")) }
        }
    }

    /** The introduction comes with the article, so judging it takes no download. */
    private suspend fun judged(article: NewsArticle, language: Language): CoverageCheck =
        try {
            news.coverage(article, language)?.let { CoverageCheck.Checked(it) } ?: CoverageCheck.Failed(tr("This language cannot be checked"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CoverageCheck.Failed(tr("Could not read the article"))
        }

    private fun updateItem(article: NewsArticle, change: (NewsItem) -> NewsItem) =
        _state.update { s -> s.copy(articles = s.articles.map { if (it.article.url == article.url) change(it) else it }) }

    private companion object {
        const val SEARCH_DELAY = 500L
    }
}

/** The line that ends a wiki's article: the wiki, the title and the licence all of them share. */
internal fun wikiAttribution(article: NewsArticle): String {
    val wiki = Wiki.entries.firstOrNull { it.siteName == article.source.name }
    return tr("Source: {0}, «{1}», CC BY-SA 4.0.", wiki?.label ?: article.source.name, article.title)
}

/** The wiki's name as the reader's language has it. */
internal val Wiki.label: String
    get() = when (this) {
        Wiki.WIKIPEDIA -> tr("Wikipedia")
        Wiki.SIMPLE_WIKIPEDIA -> tr("Simple English Wikipedia")
        Wiki.KLEXIKON -> "Klexikon"
        Wiki.MINIKLEXIKON -> "MiniKlexikon"
        Wiki.WIKIVOYAGE -> tr("Wikivoyage")
        Wiki.WIKIBOOKS -> tr("Wikibooks")
    }

/** The wiki's name on its chip, short enough for four to a phone's row. */
private val Wiki.chipLabel: String
    get() = if (this == Wiki.SIMPLE_WIKIPEDIA) tr("Simple English") else label

/** What sets the wiki apart, for all but Wikipedia. */
private val Wiki.about: String?
    get() = when (this) {
        Wiki.WIKIPEDIA -> null
        Wiki.SIMPLE_WIKIPEDIA -> tr("Wikipedia written in simple English, for learners and children.")
        Wiki.KLEXIKON -> tr("The German encyclopedia for children of 8 to 13.")
        Wiki.MINIKLEXIKON -> tr("The Klexikon in easy German, a short sentence to a line, for children starting to read.")
        Wiki.WIKIVOYAGE -> tr("Travel guides written by travellers: places, getting around, food and phrases.")
        Wiki.WIKIBOOKS -> tr("Textbooks and guides, a chapter to a page.")
    }

internal class WikiActions(
    val onWiki: (Wiki) -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onLength: (ArticleLength) -> Unit = {},
    val onDifficulty: (Difficulty?) -> Unit = {},
    val onHideRead: (Boolean) -> Unit = {},
    val onMore: () -> Unit = {},
    val onOpen: (NewsItem) -> Unit = {},
    val onOpenWhole: (NewsItem) -> Unit = {},
    val onRetry: () -> Unit = {},
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WikipediaContent(state: WikiUiState, actions: WikiActions, modifier: Modifier = Modifier, tabs: @Composable () -> Unit = {}) {
    val compact = LocalWindowWidth.current.isCompact
    val list = rememberLazyListState()
    LaunchedEffect(list, state.hasMore) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index to list.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 3) actions.onMore() }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        ScrollList(
            Modifier.widthIn(max = 1100.dp).fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 24.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val site = state.wiki.label
                    ScreenHeader(
                        tr("News"),
                        if (state.languageName.isEmpty()) null
                        else {
                            val language = languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)
                            when {
                                state.query.isNotBlank() -> tr("{0}: articles in {1} found by your search.", site, language)
                                state.wiki.countsViews -> tr("{0}: the articles read most yesterday in {1}, with how much of each introduction you already know.", site, language)
                                else -> tr("{0}: articles picked at random, with how much of each introduction you already know.", site)
                            }
                        },
                    )
                    tabs()
                    if (state.wikis.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.wikis.forEach { wiki ->
                                AppFilterChip(selected = state.wiki == wiki, onClick = { actions.onWiki(wiki) }, label = { Text(wiki.chipLabel) }, modifier = Modifier.testTag("wiki-site-${wiki.name}"))
                            }
                        }
                    }
                    if (state.languageName.isNotEmpty()) {
                        state.wiki.about?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    SearchField(state.query, actions.onQuery, tr("Search: {0}", site), Modifier.fillMaxWidth(), fieldModifier = Modifier.testTag("wiki-search"))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArticleLength.entries.forEach { length ->
                            AppFilterChip(selected = state.length == length, onClick = { actions.onLength(length) }, label = { Text(length.label) }, modifier = Modifier.testTag("length-${length.name}"))
                        }
                        AppFilterChip(selected = state.hideRead, onClick = { actions.onHideRead(!state.hideRead) }, label = { Text(tr("Hide read")) }, modifier = Modifier.testTag("wiki-hide-read"))
                    }
                    DifficultyFilter(state.difficulty, actions.onDifficulty)
                }
            }
            when {
                state.languageName.isEmpty() -> item { EmptyState(AppIcons.Globe, tr("Choose a language to learn to see its news.")) }
                state.loading -> item { Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Large) } }
                state.error != null -> item {
                    EmptyState(
                        Icons.Default.Warning,
                        tr("{0} could not be read: {1}", state.wiki.label, state.error),
                        action = { SecondaryButton(tr("Try again"), onClick = actions.onRetry) },
                    )
                }
                state.shown.isEmpty() -> item { EmptyState(AppIcons.Globe, if (state.articles.isEmpty()) tr("No articles found.") else tr("No articles match these filters.")) }
                else -> items(state.shown.size, key = { state.shown[it].article.url }) { i ->
                    WikiCard(state.shown[i], state, actions, compact, Modifier.padding(bottom = 12.dp))
                }
            }
            if (state.loadingMore) item { Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Medium) } }
            item {
                Text(
                    tr("{0}: articles free to share under CC BY-SA 4.0. Each is judged by its introduction.", state.wiki.label),
                    Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WikiCard(item: NewsItem, state: WikiUiState, actions: WikiActions, compact: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val article = item.article
    val introOpened = article.url in state.opened
    val wholeOpened = NewsService.wholeKey(article) in state.opened
    Column(
        modifier.fillMaxWidth().cardSurface().padding(16.dp).testTag("wiki-${article.title}"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Wiki.entries.firstOrNull { it.siteName == article.source.name }?.label ?: article.source.name, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            ArticleLength.of(article.length)?.let { length ->
                Text(
                    length.tag,
                    Modifier.clip(AppShapes.pill).background(colors.primary.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary,
                )
            }
        }
        Text(article.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        CoverageLine(item.difficulty, item.check, tag = article.url)
        item.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
        Spacer(Modifier.height(2.dp))
        val buttons: @Composable (Modifier) -> Unit = { m ->
            val label = if (introOpened) tr("Continue") else tr("Read the introduction")
            PrimaryButton(label, onClick = { actions.onOpen(item) }, modifier = m.testTag("read-${article.url}"), loading = item.opening)
            QuietButton(if (wholeOpened) tr("Continue the whole article") else tr("Whole article"), onClick = { actions.onOpenWhole(item) }, enabled = !item.opening, modifier = Modifier.testTag("whole-${article.url}"))
        }
        if (compact) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { buttons(Modifier.fillMaxWidth()) }
        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { buttons(Modifier) }
    }
}

private val ArticleLength.label: String
    get() = when (this) {
        ArticleLength.ANY -> tr("Any length")
        ArticleLength.SHORT -> tr("Short")
        ArticleLength.MEDIUM -> tr("Medium length")
        ArticleLength.LONG -> tr("Long")
    }

private val ArticleLength.tag: String
    get() = when (this) {
        ArticleLength.ANY -> ""
        ArticleLength.SHORT -> tr("Short article")
        ArticleLength.MEDIUM -> tr("Medium-length article")
        ArticleLength.LONG -> tr("Long article")
    }
