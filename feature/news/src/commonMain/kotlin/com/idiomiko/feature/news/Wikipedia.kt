package com.idiomiko.feature.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.news.NewsArticle
import com.idiomiko.core.domain.news.NewsBook
import com.idiomiko.core.domain.news.NewsService
import com.idiomiko.core.domain.news.NewsTopic
import com.idiomiko.core.domain.news.Wiki
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.ui.components.AppBottomSheet
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppSpinner
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.CoverageCheck
import com.idiomiko.core.ui.components.EmptyState
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.PlaceholderLine
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.SearchField
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.Segment
import com.idiomiko.core.ui.components.SegmentStyle
import com.idiomiko.core.ui.components.SegmentedToggle
import com.idiomiko.core.ui.components.SelectMenu
import com.idiomiko.core.ui.components.SelectableChip
import com.idiomiko.core.ui.components.SpinnerSize
import com.idiomiko.core.ui.components.ToggleButton
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.components.placeholder
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.state.UiEvents
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
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
import kotlin.time.Clock

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
                (!hideRead || item.article.url !in opened)
        }

    /** How many filters of the phone's sheet are set, for its Filters button. */
    val activeFilters: Int get() = listOf(length != ArticleLength.ANY, hideRead).count { it }
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

/** The wiki's picture on its articles' tiles. */
private val Wiki.icon
    get() = when (this) {
        Wiki.WIKIVOYAGE -> AppIcons.Explore
        Wiki.WIKIBOOKS -> AppIcons.MenuBook
        Wiki.KLEXIKON, Wiki.MINIKLEXIKON -> AppIcons.School
        else -> AppIcons.Globe
    }

/** The topic whose colours the wiki's tiles take. */
private val Wiki.colors: NewsTopic
    get() = when (this) {
        Wiki.WIKIVOYAGE -> NewsTopic.LIFESTYLE
        Wiki.WIKIBOOKS -> NewsTopic.SCIENCE
        Wiki.KLEXIKON, Wiki.MINIKLEXIKON -> NewsTopic.CULTURE
        else -> NewsTopic.WORLD
    }

internal class WikiActions(
    val onWiki: (Wiki) -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onLength: (ArticleLength) -> Unit = {},
    val onHideRead: (Boolean) -> Unit = {},
    val onMore: () -> Unit = {},
    val onOpen: (NewsItem) -> Unit = {},
    val onOpenWhole: (NewsItem) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/**
 * The wikis' articles in the news page's frame: the same title and row of controls (the wiki, the length and Hide
 * read in place of the news's filters), what the list holds, then the articles as cards, more loading at the end.
 */
@Composable
internal fun WikipediaContent(state: WikiUiState, actions: WikiActions, modifier: Modifier = Modifier, tabs: @Composable () -> Unit = {}, filtersOpen: Boolean = false) {
    val compact = LocalWindowWidth.current.isCompact
    val list = rememberLazyListState()
    var sheet by remember { mutableStateOf(filtersOpen) }
    LaunchedEffect(list, state.hasMore) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index to list.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 3) actions.onMore() }
    }
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val columns = when {
            compact -> 1
            maxWidth >= WIDE_WIKI -> 3
            else -> 2
        }
        NewsPage(compact, list) {
            if (!compact) part(gap = false) { NewsHeader(null, state.loading, Clock.System.now(), actions.onRetry) }
            part(gap = !compact) { if (compact) WikiPhoneControls(state, actions, tabs) { sheet = true } else WikiControls(state, actions, tabs) }
            when {
                state.languageName.isEmpty() -> part { EmptyState(tr("Choose a language to learn to see its news.")) }
                state.loading -> part { WikiSkeleton(columns) }
                state.error != null -> part {
                    EmptyState(
                        tr("{0} could not be read: {1}", state.wiki.label, state.error),
                        action = { SecondaryButton(tr("Try again"), onClick = actions.onRetry) },
                    )
                }
                state.shown.isEmpty() -> part { EmptyState(if (state.articles.isEmpty()) tr("No articles found.") else tr("No articles match these filters.")) }
                else -> {
                    part(key = "wiki-heading") { WikiHeading(state, compact) }
                    state.shown.chunked(columns).forEach { row ->
                        part(key = row.first().article.url) {
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) {
                                row.forEach { WikiCard(it, state, actions, Modifier.weight(1f).fillMaxHeight()) }
                                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
            if (state.loadingMore) part { Box(Modifier.fillMaxWidth().padding(vertical = AppSpacing.l), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Medium) } }
            part {
                Text(
                    tr("{0}: articles free to share under CC BY-SA 4.0. Each is judged by its introduction.", state.wiki.label),
                    Modifier.fillMaxWidth().padding(top = AppSpacing.s),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    if (sheet) WikiFiltersSheet(state, actions) { sheet = false }
}

/** What the list holds: the articles read most yesterday, picked at random, or found; and what sets the wiki apart. */
@Composable
private fun WikiHeading(state: WikiUiState, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val title = when {
        state.query.isNotBlank() -> tr("Results for “{0}”", state.query.trim())
        state.wiki.countsViews -> tr("Read most yesterday")
        else -> tr("Picked at random")
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Text(title, Modifier.alignByBaseline(), style = if (compact) AppText.sectionTitle else AppText.headline)
            Text(state.wiki.label, Modifier.alignByBaseline(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
        }
        state.wiki.about?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
    }
}

/** The wiki's controls in one row: news or wikis, the search, the wiki, the length and Hide read. */
@Composable
private fun WikiControls(state: WikiUiState, actions: WikiActions, tabs: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        tabs()
        SearchField(state.query, actions.onQuery, tr("Search: {0}", state.wiki.label), Modifier.weight(1f), fieldModifier = Modifier.testTag("wiki-search"))
        if (state.wikis.size > 1) SelectMenu(state.wikis, state.wiki, actions.onWiki, { it.chipLabel }, buttonModifier = Modifier.testTag("wiki-site"))
        SelectMenu(ArticleLength.entries, state.length, actions.onLength, { it.label }, buttonModifier = Modifier.testTag("wiki-length"), active = state.length != ArticleLength.ANY)
        HideReadSwitch(state.hideRead, actions.onHideRead, Modifier.testTag("wiki-hide-read"))
    }
}

/** A phone's: news or wikis, then the search beside Filters, which opens the sheet of the wiki, the length and Hide read. */
@Composable
private fun WikiPhoneControls(state: WikiUiState, actions: WikiActions, tabs: @Composable () -> Unit, onFilters: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        tabs()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            SearchField(state.query, actions.onQuery, tr("Search: {0}", state.wiki.label), Modifier.weight(1f), fieldModifier = Modifier.testTag("wiki-search"))
            val active = state.activeFilters
            ToggleButton(
                tr("Filters"),
                active = active > 0,
                onClick = onFilters,
                modifier = Modifier.testTag("wiki-filters"),
                icon = AppIcons.Tune,
                badge = active,
                contentDescription = if (active > 0) trPlural(active, "{0} filter on", "{0} filters on") else tr("Filters"),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WikiFiltersSheet(state: WikiUiState, actions: WikiActions, onDismiss: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        ScrollColumn(
            Modifier.fillMaxWidth(),
            contentModifier = Modifier.padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            Text(tr("Filters"), style = AppText.sectionTitle)
            if (state.wikis.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    state.wikis.forEach { wiki -> SelectableChip(wiki.chipLabel, selected = state.wiki == wiki, onClick = { actions.onWiki(wiki) }, modifier = Modifier.testTag("wiki-site-${wiki.name}")) }
                }
            }
            SegmentedToggle(ArticleLength.entries.map { Segment(it, label = it.short, testTag = "length-${it.name}") }, state.length, actions.onLength, style = SegmentStyle.Raised, fill = true)
            HideReadSwitch(state.hideRead, actions.onHideRead, Modifier.fillMaxWidth().testTag("wiki-sheet-hide-read"))
            PrimaryButton(
                if (state.hasMore) tr("Show articles") else trPlural(state.shown.size, "Show {0} article", "Show {0} articles"),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().testTag("wiki-filters-done"),
            )
        }
    }
}

/**
 * A wiki's article: a strip in the wiki's colours, the wiki and the article's length, the title, the introduction,
 * the share of it known and its minutes, then the two ways in: the introduction, which the whole card opens too, or
 * the whole article.
 */
@Composable
private fun WikiCard(item: NewsItem, state: WikiUiState, actions: WikiActions, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val article = item.article
    val introOpened = article.url in state.opened
    val wholeOpened = NewsService.wholeKey(article) in state.opened
    val readLabel = if (introOpened) tr("Continue") else tr("Read the introduction")
    val wiki = Wiki.entries.firstOrNull { it.siteName == article.source.name }
    Column(
        modifier.cardSurface(AppShapes.large).clickable(enabled = !item.opening, onClickLabel = readLabel) { actions.onOpen(item) }.testTag("wiki-${article.title}"),
    ) {
        TopicTile(wiki?.colors, 32.dp, Modifier.fillMaxWidth().height(WIKI_STRIP), icon = wiki?.icon ?: AppIcons.Globe)
        Column(Modifier.weight(1f).padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(
                listOfNotNull(wiki?.label ?: article.source.name, ArticleLength.of(article.length)?.tag).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(article.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(article.summary.inLearnedScript(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                KnownBadge(item)
                item.coverage?.minutes?.let { Text("· " + tr("~{0} min", it), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
            }
            item.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                QuietButton(readLabel, onClick = { actions.onOpen(item) }, modifier = Modifier.testTag("read-${article.url}"), loading = item.opening, size = ButtonSize.Compact, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward)
                QuietButton(if (wholeOpened) tr("Continue the whole article") else tr("Whole article"), onClick = { actions.onOpenWhole(item) }, enabled = !item.opening, modifier = Modifier.testTag("whole-${article.url}"), size = ButtonSize.Compact)
            }
        }
    }
}

@Composable
private fun WikiSkeleton(columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(GRID_GAP)) {
        repeat(2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) {
                repeat(columns) {
                    Column(Modifier.weight(1f).cardSurface(AppShapes.large)) {
                        Box(Modifier.fillMaxWidth().height(WIKI_STRIP).placeholder(RectangleShape))
                        Column(Modifier.padding(AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                            PlaceholderLine(0.4f)
                            PlaceholderLine(0.8f, height = AppSizes.titleLine)
                            PlaceholderLine(1f)
                            PlaceholderLine(0.6f)
                        }
                    }
                }
            }
        }
    }
}

private val ArticleLength.label: String
    get() = when (this) {
        ArticleLength.ANY -> tr("Any length")
        ArticleLength.SHORT -> tr("Short")
        ArticleLength.MEDIUM -> tr("Medium length")
        ArticleLength.LONG -> tr("Long")
    }

/** The length on a phone's segmented control, where four must fit. */
private val ArticleLength.short: String
    get() = when (this) {
        ArticleLength.ANY -> tr("Any")
        ArticleLength.SHORT -> tr("Short")
        ArticleLength.MEDIUM -> tr("Medium")
        ArticleLength.LONG -> tr("Long")
    }

private val ArticleLength.tag: String?
    get() = when (this) {
        ArticleLength.ANY -> null
        ArticleLength.SHORT -> tr("Short article")
        ArticleLength.MEDIUM -> tr("Medium-length article")
        ArticleLength.LONG -> tr("Long article")
    }

private val WIKI_STRIP = 84.dp

/** From this width three wiki cards fit to a row. */
private val WIDE_WIKI = 1000.dp
