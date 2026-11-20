package com.idiomiko.feature.news

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.news.NewsLevel
import com.idiomiko.core.domain.news.NewsSource
import com.idiomiko.core.domain.news.NewsTopic
import com.idiomiko.core.domain.news.NewsTopics
import com.idiomiko.core.ui.components.AppBottomSheet
import com.idiomiko.core.ui.components.AppCheckbox
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenu
import com.idiomiko.core.ui.components.AppSwitch
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.EmptyState
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.ScrollList
import com.idiomiko.core.ui.components.SearchField
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.Segment
import com.idiomiko.core.ui.components.SegmentStyle
import com.idiomiko.core.ui.components.SegmentedToggle
import com.idiomiko.core.ui.components.SelectButton
import com.idiomiko.core.ui.components.SelectMenu
import com.idiomiko.core.ui.components.Tag
import com.idiomiko.core.ui.components.TagSize
import com.idiomiko.core.ui.components.ToggleButton
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.components.relativeTo
import com.idiomiko.core.ui.i18n.LanguageCase
import com.idiomiko.core.ui.i18n.languageInSentence
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.state.CollectEvents
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlin.time.Clock
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel

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
    val compact = LocalWindowWidth.current.isCompact
    val refresh = if (tab == NewsTab.NEWS) viewModel::refresh else wiki::refresh
    val loading = if (tab == NewsTab.NEWS) state.loading else wikiState.loading
    Scaffold(
        topBar = {
            AppTopBar(
                title = tr("News"),
                onNavigate = onNavigate,
                section = NavSection.NEWS,
                // A wide window's page has its own Refresh by the title.
                actions = { if (compact) AppIconButton(Icons.Default.Refresh, contentDescription = tr("Refresh"), onClick = refresh, enabled = !loading) },
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
                onQuery = viewModel::setQuery,
                onPeriod = viewModel::setPeriod,
                onSort = viewModel::setSort,
                onSource = viewModel::toggleSource,
                onAllSources = viewModel::showAllSources,
                onHideRead = viewModel::setHideRead,
                onTopic = viewModel::setTopic,
                onClear = viewModel::clearFilters,
                onShowAll = viewModel::showAll,
                onOpen = viewModel::open,
                onSeen = viewModel::seen,
                onRefresh = viewModel::refresh,
            ),
            Modifier.padding(padding),
            tabs = tabs,
        )
    }
}

/** The choice between the news sites' feeds and the wikis, at the start of the controls. */
@Composable
internal fun NewsTabs(selected: NewsTab, onSelect: (NewsTab) -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    SegmentedToggle(
        NewsTab.entries.map { tab -> Segment(tab, label = if (tab == NewsTab.NEWS) tr("News sites") else tr("Wikis"), testTag = "tab-${tab.name}") },
        selected,
        onSelect,
        style = SegmentStyle.Raised,
        fill = compact,
    )
}

internal class NewsActions(
    val onQuery: (String) -> Unit = {},
    val onPeriod: (NewsPeriod) -> Unit = {},
    val onSort: (NewsSort) -> Unit = {},
    val onSource: (NewsSource) -> Unit = {},
    val onAllSources: () -> Unit = {},
    val onHideRead: (Boolean) -> Unit = {},
    val onTopic: (NewsTopic?) -> Unit = {},
    val onClear: () -> Unit = {},
    val onShowAll: () -> Unit = {},
    val onOpen: (NewsItem) -> Unit = {},
    val onSeen: (NewsItem) -> Unit = {},
    val onRefresh: () -> Unit = {},
)

/**
 * The news as a front page: the title with when it was read and Refresh, one row of controls, the topics, then the
 * lead beside four cards and the rest as rows. A phone has one column: search and a Filters sheet, topics that scroll
 * sideways, the lead, then cards with their picture at the left.
 */
@Composable
internal fun NewsContent(
    state: NewsUiState,
    actions: NewsActions,
    modifier: Modifier = Modifier,
    now: Instant = Clock.System.now(),
    tabs: @Composable () -> Unit = {},
    filtersOpen: Boolean = false,
) {
    val compact = LocalWindowWidth.current.isCompact
    var sheet by remember { mutableStateOf(filtersOpen) }
    val context = CardContext(state.sources, state.languageCode, state.opened.keys, now, actions.onOpen, actions.onSeen)
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val layout = when {
            compact -> FrontLayout.PHONE
            maxWidth >= WIDE_FROM -> FrontLayout.WIDE
            else -> FrontLayout.MEDIUM
        }
        NewsPage(compact) {
            if (!compact) part(gap = false) { NewsHeader(state.loadedAt, state.loading, now, actions.onRefresh) }
            part(gap = !compact) { if (compact) PhoneControls(state, actions, tabs) { sheet = true } else DesktopControls(state, actions, tabs) }
            when {
                state.languageName.isEmpty() -> part { EmptyState(AppIcons.Newspaper, tr("Choose a language to learn to see its news.")) }
                state.sources.isEmpty() -> part {
                    EmptyState(AppIcons.Newspaper, tr("There is no news feed in {0} yet.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)))
                }
                state.loading && state.articles.isEmpty() -> part { FrontSkeleton(layout) }
                state.allFailed -> part { OutageCard(actions.onRefresh, compact) }
                state.articles.isEmpty() -> part { EmptyState(AppIcons.Newspaper, tr("No articles found.")) }
                else -> frontPage(state, actions, context, layout)
            }
            if (state.sources.isNotEmpty()) part {
                Text(
                    tr("Articles belong to their publishers and are read from their sites for your own learning."),
                    Modifier.fillMaxWidth().padding(top = AppSpacing.s),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    if (sheet) FiltersSheet(state, actions) { sheet = false }
}

/** The topics, a notice of the sources that failed, the lead and the cards, then the rest under their heading. */
private fun LazyListScope.frontPage(state: NewsUiState, actions: NewsActions, context: CardContext, layout: FrontLayout) {
    val phone = layout == FrontLayout.PHONE
    val topics = state.topics
    if (topics.size > 1) part(key = "topics") { TopicRow(state, topics, actions.onTopic, scrolls = phone) }
    if (state.failed.isNotEmpty()) part(key = "failed") { FailedSourcesNotice(state.failed, actions.onRefresh) }
    val front = state.front
    val lead = front.lead
    if (lead == null) {
        part(key = "no-match") { NoMatchCard(state, actions, compact = phone) }
        return
    }
    when (layout) {
        FrontLayout.WIDE -> part(key = "front") { WideFront(lead, front.cards, context) }
        FrontLayout.MEDIUM -> part(key = "front") { MediumFront(lead, front.cards, context) }
        FrontLayout.PHONE -> {
            part(key = lead.article.url) { LeadCard(lead, context, PHONE_LEAD_PICTURE, Modifier.fillMaxWidth(), compact = true) }
            front.cards.forEach { card -> part(key = card.article.url) { ThumbCard(card, context) } }
        }
    }
    if (front.rows.isEmpty()) return
    part(key = "more") { MoreHeader(state, front, actions.onShowAll, phone) }
    val rows = front.rows
    // Each row is an item of its own, so the rows are judged as they come into view; on a wide window they join into one card.
    rows.forEachIndexed { i, row ->
        part(key = row.article.url, gap = phone || i == 0) {
            if (phone) ThumbCard(row, context) else ArticleRow(row, context, first = i == 0, last = i == rows.size - 1)
        }
    }
}

/** A part of the page, after the gap that parts it from the one above unless [gap] is false. */
internal fun LazyListScope.part(key: Any? = null, gap: Boolean = true, content: @Composable () -> Unit) = item(key) {
    val size = if (LocalWindowWidth.current.isCompact) AppSpacing.m else PAGE_GAP
    Box(Modifier.fillMaxWidth().padding(top = if (gap) size else 0.dp)) { content() }
}

/** The lead at the left over two rows of two cards. */
@Composable
private fun WideFront(lead: NewsItem, cards: List<NewsItem>, context: CardContext) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag("front-page"), horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) {
        LeadCard(lead, context, LEAD_PICTURE, Modifier.weight(LEAD_WEIGHT).fillMaxHeight())
        if (cards.isEmpty()) {
            Spacer(Modifier.weight(2f))
            return@Row
        }
        val stretch = cards.size > 2
        Column(Modifier.weight(2f).then(if (stretch) Modifier.fillMaxHeight() else Modifier), verticalArrangement = Arrangement.spacedBy(GRID_GAP)) {
            cards.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().then(if (stretch) Modifier.weight(1f) else Modifier.height(IntrinsicSize.Min)), horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) {
                    pair.forEach { ArticleCard(it, context, Modifier.weight(1f).fillMaxHeight()) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** A window too narrow for the cards beside the lead: the lead across, then the cards two to a row. */
@Composable
private fun MediumFront(lead: NewsItem, cards: List<NewsItem>, context: CardContext) {
    Column(Modifier.fillMaxWidth().testTag("front-page"), verticalArrangement = Arrangement.spacedBy(GRID_GAP)) {
        LeadCard(lead, context, MEDIUM_LEAD_PICTURE, Modifier.fillMaxWidth())
        cards.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(GRID_GAP)) {
                pair.forEach { ArticleCard(it, context, Modifier.weight(1f).fillMaxHeight()) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** "More from today", the order they are in, and See all while some wait behind it. */
@Composable
private fun MoreHeader(state: NewsUiState, front: FrontPage, onShowAll: () -> Unit, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val title = when (state.shownPeriod) {
        NewsPeriod.TODAY -> tr("More from today")
        NewsPeriod.WEEK -> tr("More from this week")
        NewsPeriod.ANY -> tr("More news")
    }
    Row(Modifier.fillMaxWidth().padding(top = if (compact) AppSpacing.xs else AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Text(title, Modifier.alignByBaseline(), style = if (compact) AppText.sectionTitle else AppText.headline)
            Text(state.sort.label, Modifier.alignByBaseline(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (front.hidden > 0) {
            LinkText(tr("See all {0}", state.filtered.size), onClick = onShowAll, modifier = Modifier.testTag("see-all"), style = AppText.labelStrong)
        }
    }
}

/** The page's column: centred on a wide window, with the same gap on either side measured to the scrollbar. */
@Composable
internal fun NewsPage(compact: Boolean, state: LazyListState = rememberLazyListState(), content: LazyListScope.() -> Unit) {
    ScrollList(
        Modifier.widthIn(max = AppSizes.pageWidth + AppSpacing.xxxl * 2 + AppSizes.scrollbarRoom).fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            start = if (compact) AppSpacing.l else AppSpacing.xxxl,
            end = if (compact) AppSpacing.l else AppSpacing.xxxl + AppSizes.scrollbarRoom,
            top = if (compact) AppSpacing.s else AppSpacing.xxl,
            bottom = AppSpacing.xxxl,
        ),
        content = content,
    )
}

/** "News", and at the right when the news was read and Refresh; a phone's bar has the title and Refresh, so it shows neither. */
@Composable
internal fun NewsHeader(loadedAt: Instant?, loading: Boolean, now: Instant, onRefresh: () -> Unit) {
    if (LocalWindowWidth.current.isCompact) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Text(tr("News"), Modifier.weight(1f), style = AppText.pageTitle)
        if (loadedAt != null) {
            Text(tr("Updated {0}", loadedAt.relativeTo(now)), Modifier.testTag("news-updated"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SecondaryButton(tr("Refresh"), onClick = onRefresh, modifier = Modifier.testTag("news-refresh"), icon = Icons.Default.Refresh, loading = loading)
    }
}

/**
 * The controls in one row: news or wikis, the search, the time, the sources, the order and Hide read. A window too
 * narrow for that puts the search on a row of its own.
 */
@Composable
private fun DesktopControls(state: NewsUiState, actions: NewsActions, tabs: @Composable () -> Unit) {
    val search: @Composable (Modifier) -> Unit = { modifier ->
        SearchField(state.query, actions.onQuery, tr("Search the news"), modifier, fieldModifier = Modifier.testTag("news-search"))
    }
    val rest: @Composable () -> Unit = {
        PeriodToggle(state.shownPeriod, actions.onPeriod)
        SourcesMenu(state, actions)
        SelectMenu(NewsSort.entries, state.sort, actions.onSort, { it.label }, buttonModifier = Modifier.testTag("news-sort"))
        HideReadSwitch(state.hideRead, actions.onHideRead, Modifier.testTag("hide-read"))
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= ONE_ROW_FROM) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                tabs()
                search(Modifier.weight(1f))
                rest()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    tabs()
                    search(Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) { rest() }
            }
        }
    }
}

/** A phone's controls: news or wikis, then the search beside Filters, which opens the sheet of the rest. */
@Composable
private fun PhoneControls(state: NewsUiState, actions: NewsActions, tabs: @Composable () -> Unit, onFilters: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        tabs()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            SearchField(state.query, actions.onQuery, tr("Search the news"), Modifier.weight(1f), fieldModifier = Modifier.testTag("news-search"))
            val active = state.activeFilters
            ToggleButton(
                tr("Filters"),
                active = active > 0,
                onClick = onFilters,
                modifier = Modifier.testTag("news-filters"),
                icon = AppIcons.Tune,
                badge = active,
                contentDescription = if (active > 0) trPlural(active, "{0} filter on", "{0} filters on") else tr("Filters"),
            )
        }
    }
}

/** A phone's sheet of the filters: the time, the order, the sources and Hide read, and the way back to the articles. */
@Composable
private fun FiltersSheet(state: NewsUiState, actions: NewsActions, onDismiss: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        ScrollColumn(
            Modifier.fillMaxWidth(),
            contentModifier = Modifier.padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m).testTag("news-filters-sheet"),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            Text(tr("Filters"), style = AppText.sectionTitle)
            SheetSection(tr("When")) { PeriodToggle(state.shownPeriod, actions.onPeriod, fill = true) }
            SheetSection(tr("Sort")) {
                SegmentedToggle(NewsSort.entries.map { Segment(it, label = it.label, testTag = "sort-${it.name}") }, state.sort, actions.onSort, style = SegmentStyle.Raised, fill = true)
            }
            if (state.sources.size > 1) {
                SheetSection(tr("Sources")) {
                    Column(Modifier.fillMaxWidth().clip(AppShapes.panel).border(AppBorders.hairline(), AppShapes.panel)) {
                        state.sources.forEachIndexed { i, source ->
                            if (i > 0) AppDivider()
                            SourceChoice(source, state, actions, Modifier.padding(horizontal = AppSpacing.m, vertical = AppSpacing.s))
                        }
                    }
                }
            }
            HideReadSwitch(state.hideRead, actions.onHideRead, Modifier.fillMaxWidth().testTag("sheet-hide-read"))
            val count = state.filtered.size
            PrimaryButton(trPlural(count, "Show {0} article", "Show {0} articles"), onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("news-filters-done"))
        }
    }
}

@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(title, style = AppText.labelStrong, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/** Today, This week or Any time, as one segmented control. */
@Composable
private fun PeriodToggle(selected: NewsPeriod, onSelect: (NewsPeriod) -> Unit, fill: Boolean = false) {
    SegmentedToggle(NewsPeriod.entries.map { Segment(it, label = it.label, testTag = "period-${it.name}") }, selected, onSelect, style = SegmentStyle.Raised, fill = fill)
}

/** "Sources: all 6 ▾", and its menu: every source with a tick while its articles show, and a way back to all. */
@Composable
private fun SourcesMenu(state: NewsUiState, actions: NewsActions) {
    var open by remember { mutableStateOf(false) }
    val total = state.sources.size
    val shown = total - state.hiddenSources.size
    Box {
        SelectButton(
            if (shown == total) tr("Sources: all {0}", total) else tr("Sources: {0} of {1}", shown, total),
            onClick = { open = true },
            modifier = Modifier.testTag("news-sources"),
            active = shown < total,
        )
        AppMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.testTag("news-sources-menu")) {
            Row(
                Modifier.fillMaxWidth().toggleable(value = shown == total, role = Role.Checkbox, onValueChange = { actions.onAllSources() })
                    .padding(horizontal = AppSpacing.m, vertical = AppSpacing.s).testTag("source-all"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
            ) {
                AppCheckbox(checked = shown == total, onCheckedChange = null)
                Text(tr("All sources"), style = AppText.labelStrong)
            }
            AppDivider()
            state.sources.forEach { source -> SourceChoice(source, state, actions, Modifier.padding(horizontal = AppSpacing.m, vertical = AppSpacing.xs)) }
        }
    }
}

/** A source to show or leave out: its tick, its colour, its name, and whether it writes in easy language. */
@Composable
private fun SourceChoice(source: NewsSource, state: NewsUiState, actions: NewsActions, modifier: Modifier = Modifier) {
    val on = source.name !in state.hiddenSources
    val color = sourceColor(state.sources, source)
    Row(
        Modifier.fillMaxWidth().toggleable(value = on, role = Role.Checkbox, onValueChange = { actions.onSource(source) }).then(modifier).testTag("source-${source.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        AppCheckbox(checked = on, onCheckedChange = null)
        SourceDot(color)
        Text(source.name, Modifier.weight(1f, fill = false), style = AppText.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (source.level == NewsLevel.EASY) Tag(tr("Easy language"), size = TagSize.Small)
    }
}

/** A switch with its label that turns the whole row: Hide read. */
@Composable
internal fun HideReadSwitch(on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.height(AppSizes.control).clip(AppShapes.control).toggleable(value = on, role = Role.Switch, onValueChange = onChange).padding(horizontal = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        AppSwitch(checked = on, onCheckedChange = null)
        Text(tr("Hide read"), style = AppText.labelStrong, maxLines = 1, softWrap = false)
    }
}

/** All, then the topics the articles are filed under, in the names the language's news uses; a phone's row scrolls sideways. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicRow(state: NewsUiState, topics: List<NewsTopic>, onTopic: (NewsTopic?) -> Unit, scrolls: Boolean) {
    val code = state.languageCode ?: return
    val chips: @Composable () -> Unit = {
        TopicChip(tr("All"), selected = state.topic == null, tag = "topic-all") { onTopic(null) }
        topics.forEach { topic ->
            TopicChip(NewsTopics.label(topic, code).orEmpty().inLearnedScript(), selected = state.topic == topic, tag = "topic-${topic.name}") { onTopic(if (state.topic == topic) null else topic) }
        }
    }
    if (scrolls) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) { chips() }
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) { chips() }
    }
}

/** A topic as a pill: outlined on the surface, or filled with the text colour while chosen. */
@Composable
private fun TopicChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.clip(AppShapes.pill).background(if (selected) colors.onSurface else colors.surface)
            .border(AppBorders.hairline(if (selected) colors.onSurface else colors.outlineVariant), AppShapes.pill)
            .clickable(onClick = onClick).padding(horizontal = AppSpacing.m + AppSpacing.xxs, vertical = AppSpacing.s - AppSpacing.xxs).testTag(tag),
    ) {
        Text(label, style = AppText.labelStrong, color = if (selected) colors.surface else colors.onSurface, maxLines = 1, softWrap = false)
    }
}

internal val NewsPeriod.label: String
    get() = when (this) {
        NewsPeriod.TODAY -> tr("Today")
        NewsPeriod.WEEK -> tr("This week")
        NewsPeriod.ANY -> tr("Any time")
    }

internal val NewsSort.label: String
    get() = when (this) {
        NewsSort.EASIEST -> tr("Easiest first")
        NewsSort.NEWEST -> tr("Newest first")
    }

/** The gap between the page's parts. */
private val PAGE_GAP = 20.dp

/** From this width the lead has the cards beside it. */
private val WIDE_FROM = 1000.dp

/** From this width the controls fit in one row. */
private val ONE_ROW_FROM = 1180.dp
