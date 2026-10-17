package com.tayra.languages.feature.news

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.books.discover.Difficulty
import com.tayra.languages.core.domain.news.NewsLevel
import com.tayra.languages.core.domain.news.NewsSource
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.CoverageLine
import com.tayra.languages.core.ui.components.DifficultyFilter
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ScrollList
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

/** What the News section shows: the news sites' feeds, or Wikipedia's articles. */
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
    // A language without news feeds still has its Wikipedia.
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
                    IconButton(onClick = if (tab == NewsTab.NEWS) viewModel::refresh else wiki::refresh, enabled = !loading) { Icon(Icons.Default.Refresh, contentDescription = tr("Refresh")) }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (tab == NewsTab.WIKIPEDIA) {
            WikipediaContent(
                wikiState,
                WikiActions(
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

/** The choice between the news sites' feeds and Wikipedia. */
@Composable
internal fun NewsTabs(selected: NewsTab, onSelect: (NewsTab) -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    SingleChoiceSegmentedButtonRow(if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(min = 360.dp)) {
        NewsTab.entries.forEachIndexed { i, tab ->
            SegmentedButton(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(i, NewsTab.entries.size),
                modifier = Modifier.testTag("tab-${tab.name}"),
            ) { Text(if (tab == NewsTab.NEWS) tr("News sites") else "Wikipedia") }
        }
    }
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
            Modifier.widthIn(max = 1100.dp).fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 24.dp),
        ) {
            item { Header(state, compact, tabs) }
            when {
                state.languageName.isEmpty() -> item { Message(tr("Choose a language to learn to see its news.")) }
                state.sources.isEmpty() -> item {
                    Message(tr("There is no news feed in {0} yet.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)))
                }
                else -> {
                    item { Filters(state, actions) }
                    if (state.failed.isNotEmpty()) item {
                        Text(
                            tr("Could not read: {0}", state.failed.joinToString(", ") { it.name }),
                            Modifier.padding(bottom = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    when {
                        state.loading && state.articles.isEmpty() -> item { Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                        state.shown.isEmpty() -> item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Message(
                                    when {
                                        state.articles.isEmpty() -> tr("The news could not be read. Check the connection and try again.")
                                        state.stillChecking > 0 -> tr("None of the articles checked so far is at this level.")
                                        else -> tr("No articles match these filters.")
                                    },
                                )
                                if (state.articles.isNotEmpty()) TextButton(onClick = actions.onClear) { Text(tr("Clear filters")) }
                                if (state.articles.isEmpty()) OutlinedButton(onClick = actions.onRefresh, shape = RoundedCornerShape(10.dp)) { Text(tr("Try again")) }
                            }
                        }
                        else -> items(state.shown.size, key = { state.shown[it].article.url }) { i ->
                            NewsCard(state.shown[i], state, actions, compact, now, Modifier.padding(bottom = 12.dp))
                        }
                    }
                    item {
                        Text(
                            tr("Articles belong to their publishers and are read from their sites for your own learning."),
                            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
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

@Composable
private fun Header(state: NewsUiState, compact: Boolean, tabs: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!compact) Text(tr("News"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        tabs()
        if (state.languageName.isNotEmpty()) {
            Text(
                tr("The latest news in {0}, with how much of each article you already know.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Filters(state: NewsUiState, actions: NewsActions) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = actions.onQuery,
            modifier = Modifier.fillMaxWidth().testTag("news-search"),
            placeholder = { Text(tr("Search the news")) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (state.query.isNotEmpty()) ({ IconButton(onClick = { actions.onQuery("") }) { Icon(Icons.Default.Close, contentDescription = tr("Clear")) } }) else null,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NewsPeriod.entries.forEach { period ->
                FilterChip(selected = state.period == period, onClick = { actions.onPeriod(period) }, label = { Text(period.label) }, modifier = Modifier.testTag("period-${period.name}"))
            }
            if (state.hasEasySources) {
                FilterChip(selected = state.easyOnly, onClick = { actions.onEasyOnly(!state.easyOnly) }, label = { Text(tr("Easy language only")) }, modifier = Modifier.testTag("easy-only"))
            }
            FilterChip(selected = state.hideRead, onClick = { actions.onHideRead(!state.hideRead) }, label = { Text(tr("Hide read")) }, modifier = Modifier.testTag("hide-read"))
        }
        if (state.sources.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.source == null, onClick = { actions.onSource(null) }, label = { Text(tr("All sources")) }, modifier = Modifier.testTag("source-all"))
                state.sources.forEach { source ->
                    FilterChip(
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.topic == null, onClick = { actions.onTopic(null) }, label = { Text(tr("All topics")) }, modifier = Modifier.testTag("topic-all"))
                topics.forEach { topic ->
                    FilterChip(selected = state.topic == topic, onClick = { actions.onTopic(topic) }, label = { Text(topic) }, modifier = Modifier.testTag("topic-$topic"))
                }
            }
        }
        if (state.filtering && !state.loading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val count = state.shown.size + if (state.hasMore) 1 else 0
                Text(
                    if (state.hasMore) tr("More than {0} articles", state.shown.size) else trPlural(count, "{0} article", "{0} articles"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                TextButton(onClick = actions.onClear, modifier = Modifier.testTag("clear-filters")) { Text(tr("Clear filters")) }
            }
        }
        if (state.stillChecking > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
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
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape).padding(16.dp).testTag("article-${article.url}"),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    listOfNotNull(article.source.name, article.published?.relativeTo(now)).joinToString(" · "),
                    Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.source.level == NewsLevel.EASY) {
                    Text(
                        tr("Easy language"),
                        Modifier.clip(RoundedCornerShape(50)).background(colors.primary.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                    )
                }
            }
            Text(article.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (article.summary.isNotEmpty()) Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            CoverageLine(item.difficulty, item.check, tag = article.url)
            item.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
            if (compact) {
                Spacer(Modifier.height(4.dp))
                ReadButton(item, opened != null, actions, Modifier.fillMaxWidth())
            }
        }
        if (!compact) ReadButton(item, opened != null, actions, Modifier.align(Alignment.CenterVertically).widthIn(min = 140.dp))
    }
}

@Composable
private fun ReadButton(item: NewsItem, opened: Boolean, actions: NewsActions, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val content: @Composable () -> Unit = {
        if (item.opening) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
            Spacer(Modifier.width(8.dp))
        }
        Text(if (opened) tr("Continue") else tr("Read"), fontWeight = FontWeight.SemiBold)
    }
    val tagged = modifier.testTag("read-${item.article.url}")
    if (opened) OutlinedButton(onClick = { actions.onOpen(item) }, enabled = !item.opening, shape = shape, modifier = tagged) { content() }
    else Button(onClick = { actions.onOpen(item) }, enabled = !item.opening, shape = shape, modifier = tagged) { content() }
}

@Composable
private fun Message(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(vertical = 24.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
}
