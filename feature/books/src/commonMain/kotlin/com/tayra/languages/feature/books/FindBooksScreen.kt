package com.tayra.languages.feature.books

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.Difficulty
import com.tayra.languages.core.ui.components.AppFilterChip
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.CoverageLine
import com.tayra.languages.core.ui.components.DifficultyFilter
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PlaceholderCards
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.Segment
import com.tayra.languages.core.ui.components.SegmentedToggle
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.color
import com.tayra.languages.core.ui.components.itemMotion
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel

/** Free books to add to the library, found in online libraries and judged by the words the reader knows. */
@Composable
fun FindBooksScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: FindBooksViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is FindBooksEvent.Read -> onNavigate(Route.Read(event.bookId))
        }
    }
    Scaffold(
        topBar = { AppTopBar(title = tr("Find books"), onNavigate = onNavigate, onBack = onBack, section = NavSection.BOOKS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        FindBooksContent(
            state,
            FindBooksActions(
                onSource = viewModel::setSource,
                onQuery = viewModel::setQuery,
                onDifficulty = viewModel::setDifficulty,
                onLoadMore = viewModel::loadMore,
                onRetry = viewModel::retry,
                onCheck = { viewModel.check(it.book) },
                onAdd = { viewModel.add(it.book) },
                onRead = viewModel::read,
            ),
            Modifier.padding(padding),
        )
    }
}

internal class FindBooksActions(
    val onSource: (BookSource) -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onDifficulty: (Difficulty?) -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onCheck: (FoundBookItem) -> Unit = {},
    val onAdd: (FoundBookItem) -> Unit = {},
    val onRead: (FoundBookItem) -> Unit = {},
)

@Composable
internal fun FindBooksContent(state: FindBooksUiState, actions: FindBooksActions, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    val list = rememberLazyListState()
    // The next page comes when the end of the list is near.
    LaunchedEffect(list, state.hasMore) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index to list.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 3) actions.onLoadMore() }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        ScrollList(
            Modifier.widthIn(max = AppSizes.contentWidth).fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
        ) {
            item {
                Column(Modifier.padding(bottom = AppSpacing.xl)) {
                    ScreenHeader(
                        tr("Find books"),
                        if (state.languageName.isEmpty()) tr("Free books from online libraries, with how much of each you already know.")
                        else tr("Free books in {0} from online libraries, with how much of each you already know.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
                    )
                }
            }
            item { Controls(state, actions, compact) }
            when {
                state.loading -> item { PlaceholderCards(4, thumbnail = true) }
                state.error != null -> item { ErrorBox(state, actions) }
                state.shown.isEmpty() -> item { EmptyBox(state, actions) }
                else -> items(state.shown.size, key = { "${state.shown[it].book.source}-${state.shown[it].book.id}" }) { i -> Column(itemMotion()) {
                    FoundBookCard(state.shown[i], actions, compact, Modifier.padding(bottom = AppSpacing.m))
                }}
            }
            if (state.loadingMore) item { Centered { AppSpinner(size = SpinnerSize.Medium) } }
            if (!state.loading && state.error == null) item { SourceNote(state.source) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Controls(state: FindBooksUiState, actions: FindBooksActions, compact: Boolean) {
    Column(Modifier.fillMaxWidth().padding(bottom = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        val sources = state.sources.ifEmpty { BookSource.entries.filter { it.languages == null } }
        // Three libraries fit side by side on a phone; a language with libraries of its own lists them as chips.
        if (sources.size <= 3) {
            SegmentedToggle(
                sources.map { Segment(it, label = it.label, testTag = "source-${it.name}") },
                state.source,
                actions.onSource,
                if (compact) Modifier else Modifier.width(SOURCE_TOGGLE_WIDTH),
                fill = true,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                sources.forEach { source ->
                    AppFilterChip(selected = state.source == source, onClick = { actions.onSource(source) }, label = { Text(source.label) }, modifier = Modifier.testTag("source-${source.name}"))
                }
            }
        }
        SearchField(state.query, actions.onQuery, tr("Search titles and authors"), Modifier.fillMaxWidth())
        DifficultyFilter(state.difficulty, actions.onDifficulty)
    }
}

@Composable
private fun FoundBookCard(item: FoundBookItem, actions: FindBooksActions, compact: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val book = item.book
    Row(
        modifier.fillMaxWidth().cardSurface().padding(AppSpacing.l).testTag("found-${book.id}"),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        if (!compact) {
            Box(Modifier.size(COVER_WIDTH, COVER_HEIGHT).clip(AppShapes.small).background((item.difficulty?.color ?: colors.primary).copy(alpha = AppAlpha.soft)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.MenuBook, contentDescription = null, tint = item.difficulty?.color ?: colors.primary, modifier = Modifier.size(AppSizes.iconXLarge))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(book.title, style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val byline = listOfNotNull(book.authors.joinToString(", ").takeIf { it.isNotEmpty() }, book.level).joinToString(" · ")
            if (byline.isNotEmpty()) Text(byline, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (book.description.isNotEmpty()) Text(book.description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            CoverageLine(item.difficulty, item.check, tag = book.id, onCheck = if (book.source.judgedOnSight) null else ({ actions.onCheck(item) }))
            item.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
            if (compact) {
                Spacer(Modifier.height(AppSpacing.xs))
                BookAction(item, actions, Modifier.fillMaxWidth())
            }
        }
        if (!compact) BookAction(item, actions, Modifier.align(Alignment.CenterVertically).widthIn(min = ACTION_WIDTH))
    }
}

@Composable
private fun BookAction(item: FoundBookItem, actions: FindBooksActions, modifier: Modifier) {
    when {
        item.libraryId != null -> SecondaryButton(tr("Read"), onClick = { actions.onRead(item) }, modifier = modifier.testTag("read-${item.book.id}"))
        else -> PrimaryButton(
            if (item.adding) tr("Adding…") else tr("Add to library"),
            onClick = { actions.onAdd(item) },
            modifier = modifier.testTag("add-${item.book.id}"),
            loading = item.adding,
        )
    }
}

@Composable
private fun ErrorBox(state: FindBooksUiState, actions: FindBooksActions) {
    EmptyState(
        Icons.Default.Warning,
        tr("Could not reach {0}.", state.source.libraryName),
        message = state.error,
        action = { SecondaryButton(tr("Try again"), onClick = actions.onRetry) },
    )
}

@Composable
private fun EmptyBox(state: FindBooksUiState, actions: FindBooksActions) {
    val other = state.sources.ifEmpty { BookSource.entries.toList() }.first { it != state.source }
    EmptyState(
        AppIcons.MenuBook,
        when {
            state.languageName.isEmpty() -> tr("Choose a language to learn to see its books.")
            state.books.isNotEmpty() -> tr("None of these books is at this level yet.")
            state.query.isNotBlank() -> tr("No books match your search.")
            else -> tr("{0} has no books in {1}.", state.source.libraryName, languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL))
        },
        action = if (state.books.isEmpty() && state.languageName.isNotEmpty()) ({ SecondaryButton(tr("Look in {0}", other.libraryName), onClick = { actions.onSource(other) }) }) else null,
    )
}

@Composable
private fun SourceNote(source: BookSource) {
    Text(
        when (source) {
            BookSource.GUTENBERG -> tr("Classics from Project Gutenberg, free of copyright in the United States. Each book is judged by its first pages.")
            BookSource.WIKISOURCE -> tr("Texts transcribed by Wikisource's volunteers, free to share under CC BY-SA. Without a search, the most read works of last month. Each is judged by its first chapter.")
            BookSource.GDL -> tr("Children's books from the Global Digital Library, graded by level and free to share under open licences.")
            BookSource.STORYBOOKS -> tr("Short illustrated stories for children from Global Storybooks, a few sentences a page, free to share under CC BY. The easiest reading to start with.")
            BookSource.STORYWEAVER -> tr("Picture books from Pratham Books' StoryWeaver, graded from level 1 to 4 and free to share under CC BY 4.0. Each book is judged when you ask, as its pages are downloaded.")
            BookSource.WOLNE_LEKTURY -> tr("Polish school readings and classics from Wolne Lektury, most in the public domain. Without a search, its books for children and young people.")
            BookSource.AOZORA -> tr("Japanese works out of copyright from Aozora Bunko, without their furigana. Without a search, the most read works for children.")
            BookSource.LITTERATURBANKEN -> tr("Swedish classics from Litteraturbanken, free to share under CC0, most in the spelling of their time. Without a search, the most read.")
            BookSource.BEN_YEHUDA -> tr("Hebrew works in the public domain from Project Ben-Yehuda, without vowel points. Without a search, its fables, then its stories.")
            BookSource.ADL -> tr("Older Danish literature from the Royal Danish Library's Arkiv for Dansk Litteratur, free to share non-commercially, in the spelling of its time. Without a search, H. C. Andersen's fairy tales.")
            BookSource.GANJOOR -> tr("Classical Persian poems and stories from Ganjoor, each one text, without short vowel marks. Without a search, the stories of Saadi's Golestan.")
            BookSource.ELG -> tr("French classics from Ebooks libres et gratuits, free for non-commercial use. Without a search, its books for the young.")
            BookSource.TEXTGRID -> tr("German literature from the TextGrid Digitale Bibliothek, each tale, poem or novel a text of its own, free to share under CC BY 3.0. Without a search, the Grimms' tales and legends.")
            BookSource.LIBER_LIBER -> tr("Italian classics from Liber Liber, most free to share non-commercially. Without a search, its books for young people.")
            BookSource.ELEJANDRIA -> tr("Spanish books from Elejandría, in the public domain or under open licences. Without a search, its children's stories.")
            BookSource.ADAMASTOR -> tr("Portuguese classics from Projecto Adamastor, revised into today's spelling and free to share under CC BY-SA 4.0.")
        },
        Modifier.fillMaxWidth().padding(top = AppSpacing.s, bottom = AppSpacing.l),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = AppSpacing.xxl), contentAlignment = Alignment.Center) { content() }
}

private val BookSource.label: String
    get() = if (this == BookSource.GDL) tr("Easy readers") else shortName

private val SOURCE_TOGGLE_WIDTH = 460.dp
private val COVER_WIDTH = 56.dp
private val COVER_HEIGHT = 76.dp
private val ACTION_WIDTH = 160.dp
