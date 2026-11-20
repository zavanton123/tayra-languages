package com.idiomiko.feature.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.books.discover.BookSource
import com.idiomiko.core.domain.books.discover.Shelf
import com.idiomiko.core.domain.books.discover.Topic
import com.idiomiko.core.ui.components.AppDialog
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.CoverageLine
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.inLearnedScript
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.state.CollectEvents
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel

/**
 * Free books to add to the library, from every online library at once: shelves picked by the words the reader
 * knows, a search across them all, and a library's card to narrow them to its own books.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FindBooksScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: FindBooksViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is FindBooksEvent.Read -> onNavigate(Route.Read(event.bookId))
        }
    }
    // A shelf in full goes back to the shelves before the screen is left.
    BackHandler(enabled = state.view != FindView.Discover) { viewModel.back() }
    Scaffold(
        topBar = { AppTopBar(title = tr("Find books"), onNavigate = onNavigate, onBack = { if (!viewModel.back()) onBack() }, section = NavSection.BOOKS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        FindBooksContent(
            state,
            FindBooksActions(
                onQuery = viewModel::setQuery,
                onSearch = viewModel::search,
                onTopic = viewModel::setTopic,
                onMinKnown = viewModel::setMinKnown,
                onClearSearch = viewModel::clearSearch,
                onShowAll = viewModel::showAll,
                onLibrary = viewModel::toggleLibrary,
                onClearLibrary = viewModel::clearLibrary,
                onBack = { viewModel.back() },
                onRetry = viewModel::retry,
                onLoadMore = viewModel::loadMore,
                onRetryLibrary = viewModel::retryLibrary,
                onCheck = { viewModel.check(it.book) },
                onAdd = { viewModel.add(it.book) },
                onRead = viewModel::read,
            ),
            Modifier.padding(padding),
            knownWords = LocalLearningLanguage.current?.knownWords,
        )
    }
}

internal class FindBooksActions(
    val onQuery: (String) -> Unit = {},
    val onSearch: () -> Unit = {},
    val onTopic: (Topic) -> Unit = {},
    val onMinKnown: (Int) -> Unit = {},
    val onClearSearch: () -> Unit = {},
    val onShowAll: (Shelf) -> Unit = {},
    /** Picks a library's card, or lets it go when it is picked already. */
    val onLibrary: (BookSource) -> Unit = {},
    val onClearLibrary: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onRetryLibrary: () -> Unit = {},
    val onCheck: (FoundBookItem) -> Unit = {},
    val onAdd: (FoundBookItem) -> Unit = {},
    val onRead: (FoundBookItem) -> Unit = {},
)

/** The page below the top bar: the shelves, or a shelf in full; a book tapped opens its card. */
@Composable
internal fun FindBooksContent(state: FindBooksUiState, actions: FindBooksActions, modifier: Modifier = Modifier, knownWords: Int? = null) {
    // The book whose card is open, found again in the state so it shows its adding and judging as they go.
    var opened by remember { mutableStateOf<Pair<BookSource, String>?>(null) }
    val open: (FoundBookItem) -> Unit = { opened = it.book.source to it.book.id }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when (val view = state.view) {
            FindView.Discover -> DiscoverPage(state, actions, open, knownWords)
            is FindView.AllOf -> ShelfPage(state, view.shelf, actions, open)
        }
    }
    opened?.let { (source, id) ->
        val item = (state.found[source].orEmpty() + state.more.books).firstOrNull { it.book.source == source && it.book.id == id }
        if (item == null) opened = null else FoundBookDialog(item, actions, onDismiss = { opened = null })
    }
}

/** A book's card: its cover, who wrote it, how much of it the reader knows, where it is from and under what licence, and the way to add or read it. */
@Composable
private fun FoundBookDialog(item: FoundBookItem, actions: FindBooksActions, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val book = item.book
    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = DIALOG_WIDTH).testTag("found-book-dialog"),
        title = { Text(book.title.inLearnedScript(), maxLines = 3, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    BookCover(book.title, DIALOG_COVER, code = null)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                        val byline = listOfNotNull(book.authors.joinToString(", ").inLearnedScript().takeIf { it.isNotEmpty() }, book.level).joinToString(" · ")
                        if (byline.isNotEmpty()) Text(byline, style = AppText.emphasis)
                        item.coverage?.minutes?.let { Text(tr("~{0} min", it), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
                        CoverageLine(item.difficulty, item.check, tag = book.id, onCheck = if (book.source.judgedOnSight) null else ({ actions.onCheck(item) }))
                        Text(tr("From {0}, {1}.", book.source.libraryName, licence(book.license)), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                if (book.description.isNotEmpty()) Text(book.description.inLearnedScript(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 6, overflow = TextOverflow.Ellipsis)
                item.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
            }
        },
        confirmButton = {
            if (item.libraryId != null) {
                PrimaryButton(tr("Read"), onClick = { actions.onRead(item) }, modifier = Modifier.testTag("dialog-read"), trailingIcon = Icons.AutoMirrored.Filled.ArrowForward)
            } else {
                PrimaryButton(
                    when { item.adding -> tr("Adding…"); item.error != null -> tr("Try again"); else -> tr("Add to library") },
                    onClick = { actions.onAdd(item) },
                    modifier = Modifier.testTag("dialog-add"),
                    loading = item.adding,
                    icon = Icons.Default.Add,
                )
            }
        },
        dismissButton = { QuietButton(tr("Close"), onClick = onDismiss) },
    )
}

/** What a library is called on the page: the Global Digital Library by what it holds. */
internal val BookSource.label: String
    get() = if (this == BookSource.GDL) tr("Easy readers") else shortName

private val DIALOG_WIDTH = 560.dp
private val DIALOG_COVER = 112.dp
