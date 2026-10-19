package com.tayra.languages.feature.news

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.books.discover.Difficulty
import com.tayra.languages.core.domain.news.NewsLevel
import com.tayra.languages.core.domain.news.NewsSource
import com.tayra.languages.core.ui.components.AppFilterChip
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.CoverageLine
import com.tayra.languages.core.ui.components.DifficultyFilter
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.Segment
import com.tayra.languages.core.ui.components.SegmentedToggle
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.relativeTo
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppText

/** What the News section shows: the news sites' feeds, or the articles of Wikipedia and the other wikis. */
enum class NewsTab { NEWS, WIKIPEDIA }

@Composable
fun NewsScreen(onNavigate: (Route) -> Unit, viewModel: NewsViewModel = koinViewModel(), wiki: WikipediaViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val wikiState by wiki.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is NewsEvent.Read -> onNavigate(Route.Read(event.bookId))
        }
    }
    CollectEvents(wiki.events) { event ->
        when (event) {
            is WikiEvent.Read -> onNavigate(Route.Read(event.bookId))
        }
    }
    // Coming back after a while, the news has moved on.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshIfOld() }
    var chosen by rememberSaveable { mutableStateOf<NewsTab?>(null) }
    // A language without news feeds still has its wikis.
    val tab = chosen ?: if (state.languageName.isNotEmpty() && state.sources.isEmpty()) NewsTab.WIKIPEDIA else NewsTab.NEWS
    val tabs: @Composable () -> Unit = { NewsTabs(tab) { chosen = it } }
    Scaffold(
        topBar = {
            AppTopBar(
                title = tr("News"),
                onNavigate = onNavigate,
                section = NavSection.NEWS,
                actions = {
                    val loading = if (tab == NewsTab.NEWS) state.loading else wikiState.loading
                    AppIconButton(Icons.Default.Refresh, contentDescription = tr("Refresh"), onClick = if (tab == NewsTab.NEWS) viewModel::refresh else wiki::refresh, enabled = !loading)
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (tab == NewsTab.WIKIPEDIA) {
            WikipediaContent(
                wikiState,
                WikiActions(
                    onWiki = wiki::setWiki,
                    onQuery = wiki::setQuery,
                    onLength = wiki::setLength,
                    onDifficulty = wiki::setDifficulty,
                    onHideRead = wiki::setHideRead,
                    onMore = wiki::loadMore,
                    onOpen = wiki::open,
                    onOpenWhole = wiki::openWhole,
                    onRetry = wiki::refresh,
                ),
                Modifier.padding(padding),
                tabs,
            )
            return@Scaffold
        }
        NewsContent(
            state,
            NewsActions(
                onSource = viewModel::setSource,
                onDifficulty = viewModel::setDifficulty,
                onQuery = viewModel::setQuery,
                onPeriod = viewModel::setPeriod,
                onEasyOnly = viewModel::setEasyOnly,
                onHideRead = viewModel::setHideRead,
                onTopic = viewModel::setTopic,
                onClear = viewModel::clearFilters,
                onMore = viewModel::showMore,
                onOpen = viewModel::open,
                onRefresh = viewModel::refresh,
            ),
            Modifier.padding(padding),
            tabs = tabs,
        )
    }
}

/** The choice between the news sites' feeds and the wikis. */
@Composable
internal fun NewsTabs(selected: NewsTab, onSelect: (NewsTab) -> Unit) {
    SegmentedToggle(
        NewsTab.entries.map { tab -> Segment(tab, label = if (tab == NewsTab.NEWS) tr("News sites") else tr("Wikis"), testTag = "tab-${tab.name}") },
        selected,
        onSelect,
    )
}

internal class NewsActions(
    val onSource: (NewsSource?) -> Unit = {},
    val onDifficulty: (Difficulty?) -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onPeriod: (NewsPeriod) -> Unit = {},
    val onEasyOnly: (Boolean) -> Unit = {},
    val onHideRead: (Boolean) -> Unit = {},
    val onTopic: (String?) -> Unit = {},
    val onClear: () -> Unit = {},
    val onMore: () -> Unit = {},
    val onOpen: (NewsItem) -> Unit = {},
    val onRefresh: () -> Unit = {},
)

@Composable
internal fun NewsContent(state: NewsUiState, actions: NewsActions, modifier: Modifier = Modifier, now: Instant = Clock.System.now(), tabs: @Composable () -> Unit = {}) {
    val compact = LocalWindowWidth.current.isCompact
    val list = rememberLazyListState()
    // More articles are listed as the end of the list comes near.
    LaunchedEffect(list, state.hasMore) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index to list.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 3) actions.onMore() }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        ScrollList(
            Modifier.widthIn(max = AppSizes.contentWidth).fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    ScreenHeader(
                        tr("News"),
                        if (state.languageName.isEmpty()) null
                        else tr("The latest news in {0}, with how much of each article you already know.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
                    )
                    tabs()
                }
            }
            when {
                state.languageName.isEmpty() -> item { EmptyState(AppIcons.Newspaper, tr("Choose a language to learn to see its news.")) }
                state.sources.isEmpty() -> item {
                    EmptyState(AppIcons.Newspaper, tr("There is no news feed in {0} yet.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)))
                }
                else -> {
                    item { Filters(state, actions) }
                    if (state.failed.isNotEmpty()) item {
                        Text(
                            tr("Could not read: {0}", state.failed.joinToString(", ") { it.name }),
                            Modifier.padding(bottom = AppSpacing.m),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    when {
                        state.loading && state.articles.isEmpty() -> item { Box(Modifier.fillMaxWidth().padding(vertical = AppSpacing.xxxl), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Large) } }
                        state.shown.isEmpty() -> item {
                            EmptyState(
                                AppIcons.Newspaper,
                                when {
                                    state.articles.isEmpty() -> tr("The news could not be read. Check the connection and try again.")
                                    state.stillChecking > 0 -> tr("None of the articles checked so far is at this level.")
                                    else -> tr("No articles match these filters.")
                                },
                                action = {
                                    if (state.articles.isNotEmpty()) QuietButton(tr("Clear filters"), onClick = actions.onClear)
                                    else SecondaryButton(tr("Try again"), onClick = actions.onRefresh)
                                },
                            )
                        }
                        else -> items(state.shown.size, key = { state.shown[it].article.url }) { i ->
                            NewsCard(state.shown[i], state, actions, compact, now, Modifier.padding(bottom = AppSpacing.m))
                        }
                    }
                    item {
                        Text(
                            tr("Articles belong to their publishers and are read from their sites for your own learning."),
                            Modifier.fillMaxWidth().padding(top = AppSpacing.s, bottom = AppSpacing.l),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Filters(state: NewsUiState, actions: NewsActions) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(bottom = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        SearchField(state.query, actions.onQuery, tr("Search the news"), Modifier.fillMaxWidth(), fieldModifier = Modifier.testTag("news-search"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            NewsPeriod.entries.forEach { period ->
                AppFilterChip(selected = state.period == period, onClick = { actions.onPeriod(period) }, label = { Text(period.label) }, modifier = Modifier.testTag("period-${period.name}"))
            }
            if (state.hasEasySources) {
                AppFilterChip(selected = state.easyOnly, onClick = { actions.onEasyOnly(!state.easyOnly) }, label = { Text(tr("Easy language only")) }, modifier = Modifier.testTag("easy-only"))
            }
            AppFilterChip(selected = state.hideRead, onClick = { actions.onHideRead(!state.hideRead) }, label = { Text(tr("Hide read")) }, modifier = Modifier.testTag("hide-read"))
        }
        if (state.sources.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                AppFilterChip(selected = state.source == null, onClick = { actions.onSource(null) }, label = { Text(tr("All sources")) }, modifier = Modifier.testTag("source-all"))
                state.sources.forEach { source ->
                    AppFilterChip(
                        selected = state.source == source,
                        onClick = { actions.onSource(source) },
                        label = { Text(if (source.level == NewsLevel.EASY) tr("{0} · easy language", source.name) else source.name) },
                        modifier = Modifier.testTag("source-${source.name}"),
                    )
                }
            }
        }
        DifficultyFilter(state.difficulty, actions.onDifficulty)
        val topics = state.topics
        if (topics.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                AppFilterChip(selected = state.topic == null, onClick = { actions.onTopic(null) }, label = { Text(tr("All topics")) }, modifier = Modifier.testTag("topic-all"))
                topics.forEach { topic ->
                    AppFilterChip(selected = state.topic == topic, onClick = { actions.onTopic(topic) }, label = { Text(topic) }, modifier = Modifier.testTag("topic-$topic"))
                }
            }
        }
        if (state.filtering && !state.loading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                val count = state.shown.size + if (state.hasMore) 1 else 0
                Text(
                    if (state.hasMore) tr("More than {0} articles", state.shown.size) else trPlural(count, "{0} article", "{0} articles"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                QuietButton(tr("Clear filters"), onClick = actions.onClear, modifier = Modifier.testTag("clear-filters"))
            }
        }
        if (state.stillChecking > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                AppSpinner(size = SpinnerSize.Small)
                Text(
                    trPlural(state.stillChecking, "Checking the words of {0} more article…", "Checking the words of {0} more articles…"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

private val NewsPeriod.label: String
    get() = when (this) {
        NewsPeriod.ANY -> tr("Any time")
        NewsPeriod.TODAY -> tr("Today")
        NewsPeriod.WEEK -> tr("This week")
    }

@Composable
private fun NewsCard(item: NewsItem, state: NewsUiState, actions: NewsActions, compact: Boolean, now: Instant, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val article = item.article
    val opened = state.opened[article.url]
    Row(
        modifier.fillMaxWidth().cardSurface().padding(AppSpacing.l).testTag("article-${article.url}"),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Text(
                    listOfNotNull(article.source.name, article.published?.relativeTo(now)).joinToString(" · "),
                    Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.source.level == NewsLevel.EASY) {
                    Tag(tr("Easy language"), size = TagSize.Small)
                }
            }
            Text(article.title, style = AppText.itemTitle, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (article.summary.isNotEmpty()) Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            CoverageLine(item.difficulty, item.check, tag = article.url)
            item.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
            if (compact) {
                Spacer(Modifier.height(AppSpacing.xs))
                ReadButton(item, opened != null, actions, Modifier.fillMaxWidth())
            }
        }
        if (!compact) ReadButton(item, opened != null, actions, Modifier.align(Alignment.CenterVertically).widthIn(min = READ_BUTTON_WIDTH))
    }
}

@Composable
private fun ReadButton(item: NewsItem, opened: Boolean, actions: NewsActions, modifier: Modifier) {
    val text = if (opened) tr("Continue") else tr("Read")
    val tagged = modifier.testTag("read-${item.article.url}")
    if (opened) SecondaryButton(text, onClick = { actions.onOpen(item) }, modifier = tagged, loading = item.opening)
    else PrimaryButton(text, onClick = { actions.onOpen(item) }, modifier = tagged, loading = item.opening)
}

private val READ_BUTTON_WIDTH = 140.dp
