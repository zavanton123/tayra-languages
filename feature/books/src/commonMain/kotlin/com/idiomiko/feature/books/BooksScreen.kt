package com.idiomiko.feature.books

import com.idiomiko.core.ui.components.ArchivedBooksIllustration
import com.idiomiko.core.ui.components.AppDialog
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import com.idiomiko.core.ui.components.inLearnedScript
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.dictionary.PackState
import com.idiomiko.core.domain.model.BookListItem
import com.idiomiko.core.domain.stats.StudyTime
import com.idiomiko.core.ui.components.AppCheckbox
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.AppTriStateCheckbox
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.ButtonTone
import com.idiomiko.core.ui.components.ConfirmDialog
import com.idiomiko.core.ui.components.EmptyState
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LoadingIndicator
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.OverflowMenu
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.ScrollList
import com.idiomiko.core.ui.components.SearchField
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.Segment
import com.idiomiko.core.ui.components.SegmentStyle
import com.idiomiko.core.ui.components.SegmentedToggle
import com.idiomiko.core.ui.components.SelectMenu
import com.idiomiko.core.ui.components.SelectionBar
import com.idiomiko.core.ui.components.TagFilterButton
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.components.pressScale
import com.idiomiko.core.ui.i18n.LanguageCase
import com.idiomiko.core.ui.i18n.formatCount
import com.idiomiko.core.ui.i18n.languageInSentence
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.navigation.rememberPracticeOpener
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The library: the book being read, the books to filter and sort, as a table or a grid. Also used for the archive. */
@Composable
fun BooksScreen(
    archived: Boolean,
    onNavigate: (Route) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: BooksViewModel = koinViewModel(key = "books-$archived") { parametersOf(archived) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<BookListItem?>(null) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }
    val compact = LocalWindowWidth.current.isCompact
    // On a phone, ticking books starts from the list menu in the bar, so the choice lives here.
    var selecting by rememberSaveable { mutableStateOf(false) }
    val practise = rememberPracticeOpener(onNavigate)

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (archived) tr("Archived books") else if (compact) tr("Books") else "Idiomiko",
                onNavigate = onNavigate,
                onBack = onBack,
                section = NavSection.BOOKS,
                actions = {
                    if (compact) PhoneBooksMenu(archived, selecting, onSelecting = { selecting = it; if (!it) viewModel.selectAll(false) }, onNewBook = { onNavigate(Route.NewBook()) }, onArchive = { onNavigate(Route.ArchivedBooks) }, onFindBooks = { onNavigate(Route.FindBooks) })
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        val callbacks = BooksCallbacks(
                onOpen = { onNavigate(Route.Read(it.id)) },
                onBackToBooks = { onBack?.invoke() ?: onNavigate(Route.Books) },
                onPractice = { practise(Route.Practice(it.id)) },
                onEdit = { onNavigate(Route.EditBook(it.id)) },
                onArchive = { if (it.isArchived) viewModel.unarchive(it.id) else viewModel.archive(it.id) },
                onDelete = { pendingDelete = it },
                onBookmarks = { onNavigate(Route.Bookmarks(it.id)) },
                onNewBook = { onNavigate(Route.NewBook()) },
                onSearch = viewModel::setSearch,
                onSort = viewModel::setSort,
                onTags = viewModel::setTags,
                onProgress = viewModel::setProgress,
                onView = viewModel::setView,
                onToggle = { viewModel.toggleSelected(it.id) },
                onSelectAll = viewModel::selectAll,
                onArchiveSelected = { viewModel.archiveSelected() },
                onDeleteSelected = { confirmDeleteSelected = true },
                onDownloadBooks = viewModel::downloadBooks,
                onFindBooks = { onNavigate(Route.FindBooks) },
            )
        if (compact) PhoneBooksContent(state, callbacks, selecting = selecting, onDone = { selecting = false; viewModel.selectAll(false) }, modifier = Modifier.padding(padding))
        else BooksContent(state = state, callbacks = callbacks, modifier = Modifier.padding(padding))
    }

    pendingDelete?.let { book ->
        ConfirmDialog(
            title = tr("Delete \"{0}\"?", book.title),
            text = tr("The book and its pages will be deleted. Terms are kept."),
            confirmLabel = tr("Delete"),
            destructive = true,
            onConfirm = { viewModel.delete(book.id); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
    if (confirmDeleteSelected) {
        val count = state.selectedBooks.size
        ConfirmDialog(
            title = trPlural(count, "Delete {0} book?", "Delete {0} books?"),
            text = tr("The books and their pages will be deleted. Terms are kept."),
            confirmLabel = tr("Delete"),
            destructive = true,
            onConfirm = { viewModel.deleteSelected(); confirmDeleteSelected = false },
            onDismiss = { confirmDeleteSelected = false },
        )
    }
}

internal class BooksCallbacks(
    val onOpen: (BookListItem) -> Unit = {},
    /** Practises the words being learned in the whole book. */
    val onPractice: (BookListItem) -> Unit = {},
    val onEdit: (BookListItem) -> Unit = {},
    val onArchive: (BookListItem) -> Unit = {},
    val onDelete: (BookListItem) -> Unit = {},
    val onBookmarks: (BookListItem) -> Unit = {},
    val onNewBook: () -> Unit = {},
    /** From the archive back to the library. */
    val onBackToBooks: () -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val onSort: (BookSort) -> Unit = {},
    val onTags: (tags: Set<String>, matchAll: Boolean) -> Unit = { _, _ -> },
    val onProgress: (ProgressFilter) -> Unit = {},
    val onView: (BooksView) -> Unit = {},
    val onToggle: (BookListItem) -> Unit = {},
    val onSelectAll: (Boolean) -> Unit = {},
    val onArchiveSelected: () -> Unit = {},
    val onDeleteSelected: () -> Unit = {},
    /** Downloads the language's pack of free books. */
    val onDownloadBooks: () -> Unit = {},
    /** Opens the free books of online libraries. */
    val onFindBooks: () -> Unit = {},
) {
    /** Back to every book: no search, no status, no tags. */
    fun clearFilters() {
        onSearch("")
        onProgress(ProgressFilter.ALL)
        onTags(emptySet(), false)
    }
}

/** The page below the top bar: the title, the book being read, the toolbar, and the books as a table or a grid. */
@Composable
internal fun BooksContent(state: BooksUiState, callbacks: BooksCallbacks, modifier: Modifier = Modifier) {
    // The table needs a wide window; narrower ones list the books as cards.
    val wide = LocalWindowWidth.current.isExpanded
    // Ticking books for the bulk actions starts from Select, so the list stays plain otherwise.
    var selecting by rememberSaveable { mutableStateOf(false) }
    val showChecks = selecting || state.selectedBooks.isNotEmpty()
    val books = state.filteredBooks
    val onSelect = { selecting = true }.takeIf { !showChecks && books.isNotEmpty() }
    ScrollList(
        modifier.fillMaxSize(),
        // The same gap on either side of the page, measured to the scrollbar on the right.
        contentPadding = PaddingValues(start = AppSpacing.xxxl, end = AppSpacing.xxxl + AppSizes.scrollbarRoom, top = AppSpacing.xxl, bottom = AppSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        item {
            ScreenHeader(
                title = if (state.archived) tr("Archived books") else tr("Books"),
                subtitle = if (state.archived) tr("Books you set aside appear here.") else null,
            ) {
                // An empty library offers the same ways in itself.
                if (!state.archived && state.books.isNotEmpty()) {
                    SecondaryButton(tr("Find books online"), onClick = callbacks.onFindBooks, modifier = Modifier.testTag("find-books"), icon = Icons.Default.Search)
                    PrimaryButton(tr("Add a book"), onClick = callbacks.onNewBook, modifier = Modifier.testTag("new-book"), icon = Icons.Default.Add)
                }
            }
        }
        // An empty library: the way in is a pack of free books, or a first book of one's own.
        if (!state.archived && state.books.isEmpty()) {
            item { EmptyLibrary(state, callbacks.onNewBook, callbacks.onDownloadBooks, callbacks.onFindBooks) }
            return@ScrollList
        }
        // An empty archive has nothing to sort or filter, so it says only how books get there.
        if (state.archived && state.books.isEmpty()) {
            item { EmptyArchive(callbacks.onBackToBooks) }
            return@ScrollList
        }
        if (!state.archived) state.continueWith?.let { book -> item { ContinueCard(book) { callbacks.onOpen(book) } } }
        item { Toolbar(state, wide, callbacks) }
        if (showChecks) item { BooksSelectionBar(state, callbacks, onDone = { selecting = false; callbacks.onSelectAll(false) }) }
        item {
            when {
                books.isEmpty() -> NoBooks(state.archived, callbacks.onNewBook, onClearFilters = callbacks::clearFilters, onBackToBooks = callbacks.onBackToBooks)
                wide && state.view == BooksView.LIST -> BookTable(state, books, showChecks, callbacks, onSelect)
                else -> Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    BookGrid(books, state.selected, showChecks, callbacks)
                    LibraryFooter(state, onSelect, Modifier.padding(horizontal = AppSpacing.xs))
                }
            }
        }
    }
}

/** The book being read most recently: its cover, where the reader is and the minutes left, the two shares, and the way back in. */
@Composable
private fun ContinueCard(book: BookListItem, onContinue: () -> Unit) = BoxWithConstraints(Modifier.fillMaxWidth()) {
    // A window too narrow for the shares beside the title stacks the card as a phone does.
    if (maxWidth < CONTINUE_ROW) {
        PhoneContinueCard(book, onContinue)
        return@BoxWithConstraints
    }
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.xl).testTag("continue-card"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl),
    ) {
        BookCover(book, CONTINUE_COVER)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(tr("Continue reading").uppercase(), style = AppText.overline, color = colors.primary)
            Text(book.title.inLearnedScript(), style = AppText.headline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(placeLine(book), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        ContinueBars(book, Modifier.weight(1f), spacing = AppSpacing.m)
        PrimaryButton(tr("Continue"), onClick = onContinue, modifier = Modifier.testTag("continue-reading"), size = ButtonSize.Large)
    }
}

/** "Page 3 of 5 · ~13 min left": the page under way and the minutes the rest of the book takes. */
internal fun placeLine(book: BookListItem): String =
    tr("Page {0} of {1} · ~{2} min left", book.currentPage, book.pageCount, StudyTime.readingMinutes(book.wordsLeft))

/** The Continue card's two shares: the pages read and the words known. */
@Composable
internal fun ContinueBars(book: BookListItem, modifier: Modifier, spacing: Dp, vertical: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val read: @Composable (Modifier) -> Unit = { LabelledBar(tr("Pages read"), book.progressPercent, colors.primary, colors.onSurface, it) }
    val known: @Composable (Modifier) -> Unit = { LabelledBar(tr("Words you know"), book.masteryPercent, AppAccents.green, AppAccents.green, it) }
    if (vertical) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) { read(Modifier.fillMaxWidth()); known(Modifier.fillMaxWidth()) }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing)) { read(Modifier.weight(1f)); known(Modifier.weight(1f)) }
    }
}

/** The status tabs, each with how many books it holds, as one segmented control. */
@Composable
internal fun StatusSegments(state: BooksUiState, onSelect: (ProgressFilter) -> Unit) {
    SegmentedToggle(
        ProgressFilter.entries.map { Segment(it, label = tr(it.label), count = state.counts[it] ?: 0, testTag = "progress-${it.name}") },
        state.progress,
        onSelect,
        style = SegmentStyle.Raised,
    )
}

/**
 * The status tabs, the search, the tags, the order and the view in one row; when the search would be too
 * narrow, the tabs take a row of their own above the rest.
 */
@Composable
private fun Toolbar(state: BooksUiState, wide: Boolean, callbacks: BooksCallbacks) {
    val hasTags = state.availableTags.isNotEmpty()
    Layout(
        content = {
            StatusSegments(state, callbacks.onProgress)
            SearchField(state.search, callbacks.onSearch, tr("Search titles or tags"))
            if (hasTags) TagFilterButton(state.availableTags, state.tags, state.matchAllTags, callbacks.onTags) else Spacer(Modifier)
            SelectMenu(BookSort.entries, state.sort, callbacks.onSort, { tr(it.label) }, buttonModifier = Modifier.testTag("book-sort"))
            // Only wide windows have the table to switch to.
            if (wide) ViewToggle(state.view, callbacks.onView) else Spacer(Modifier)
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val gap = AppSpacing.m.roundToPx()
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val tabs = measurables[0].measure(loose)
        val controls = measurables.drop(2).map { it.measure(loose) }.filter { it.width > 0 }
        val controlsWidth = controls.sumOf { it.width + gap }
        val inline = width - tabs.width - gap - controlsWidth
        val ownRow = inline < MIN_SEARCH.roundToPx()
        val field = measurables[1].measure(Constraints.fixedWidth((if (ownRow) width - controlsWidth else inline).coerceAtLeast(0)))
        val rowHeight = maxOf(field.height, controls.maxOfOrNull { it.height } ?: 0, if (ownRow) 0 else tabs.height)
        val top = if (ownRow) tabs.height + gap else 0
        layout(width, top + rowHeight) {
            fun centred(p: Placeable, x: Int) = p.place(x, top + (rowHeight - p.height) / 2)
            var x = 0
            if (ownRow) tabs.place(0, 0) else { centred(tabs, 0); x = tabs.width + gap }
            centred(field, x)
            x += field.width
            controls.forEach { centred(it, x + gap); x += gap + it.width }
        }
    }
}

@Composable
private fun ViewToggle(selected: BooksView, onSelect: (BooksView) -> Unit) {
    SegmentedToggle(
        listOf(
            Segment(BooksView.LIST, AppIcons.ViewList, description = tr("list view")),
            Segment(BooksView.GRID, AppIcons.GridView, description = tr("grid view")),
        ),
        selected,
        onSelect,
        style = SegmentStyle.Raised,
        dense = true,
    )
}

@Composable
private fun BookGrid(books: List<BookListItem>, selected: Set<Long>, selecting: Boolean, callbacks: BooksCallbacks) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // As many cards of at least 330 wide as fit, sharing the row.
        val gap = AppSpacing.l
        val columns = ((maxWidth + gap) / (GRID_CARD + gap)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            books.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { book -> BookCard(book, book.id in selected, selecting, callbacks, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** A book as a card of the grid: its cover, title, length and tags, then its progress and the share of its words known. */
@Composable
private fun BookCard(book: BookListItem, selected: Boolean, selecting: Boolean, callbacks: BooksCallbacks, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.large
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier.pressScale(interaction).clip(shape).border(AppBorders.hairline(if (selected || hovered) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), shape)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
            .hoverable(interaction).clickable(interaction, LocalIndication.current) { if (selecting) callbacks.onToggle(book) else callbacks.onOpen(book) }
            .testTag("book-card-${book.id}").padding(AppSpacing.l),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (selecting) {
                Box(Modifier.offset(x = (-8).dp, y = (-8).dp)) { BookCheckbox(book, selected, callbacks) }
            }
            BookCover(book, GRID_COVER)
            Spacer(Modifier.width(AppSpacing.l))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(book.title.inLearnedScript(), style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                BookLength(book)
            }
            Box(Modifier.offset(x = 8.dp, y = (-8).dp)) { BookMenu(book, callbacks, selecting) }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(AppSpacing.l))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            ProgressRing(book, SMALL_RING)
            StatusPill(book)
            Spacer(Modifier.weight(1f))
            KnownShare(book, labelled = true)
        }
    }
}

@Composable
internal fun BookCheckbox(book: BookListItem, selected: Boolean, callbacks: BooksCallbacks) {
    AppCheckbox(checked = selected, onCheckedChange = { callbacks.onToggle(book) }, modifier = Modifier.semantics { contentDescription = tr("Select {0}", book.title) })
}

/** While books are being ticked: how many, all of them at once, the bulk actions, and Done. */
@Composable
internal fun BooksSelectionBar(state: BooksUiState, callbacks: BooksCallbacks, onDone: () -> Unit) {
    val count = state.selectedBooks.size
    val all = count > 0 && count == state.filteredBooks.size
    SelectionBar(
        tr("{0} of {1} selected", count, state.filteredBooks.size),
        leading = {
            // Half ticked while some are chosen; a click then chooses them all.
            AppTriStateCheckbox(
                state = when { all -> ToggleableState.On; count > 0 -> ToggleableState.Indeterminate; else -> ToggleableState.Off },
                onClick = { callbacks.onSelectAll(!all) },
                modifier = Modifier.semantics { contentDescription = tr("Select all books") },
            )
        },
    ) {
        QuietButton(if (state.archived) tr("Unarchive") else tr("Archive"), onClick = callbacks.onArchiveSelected, enabled = count > 0)
        QuietButton(tr("Delete"), onClick = callbacks.onDeleteSelected, enabled = count > 0, tone = ButtonTone.Danger)
        QuietButton(tr("Done"), onClick = onDone, modifier = Modifier.testTag("done-selecting"))
    }
}

/** Below the books: how many are listed and their words, and, given [onSelect], the way into ticking several. */
@Composable
internal fun LibraryFooter(state: BooksUiState, onSelect: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val books = state.filteredBooks
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Text(
            trPlural(books.size, "{1} book", "{1} books", formatCount(books.size)) + " · " + words(books.sumOf { it.wordCount }),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        if (onSelect != null) {
            LinkText(
                if (state.archived) tr("Select to unarchive or delete several") else tr("Select to archive or delete several"),
                onSelect,
                Modifier.testTag("select-books"),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** A book's actions behind its ⋮ button; Select ticks it, unless books are being ticked already. */
@Composable
internal fun BookMenu(book: BookListItem, callbacks: BooksCallbacks, selecting: Boolean = false) {
    OverflowMenu(tr("Actions"), buttonModifier = Modifier.testTag("book-menu-${book.id}")) { close ->
        AppMenuItem(text = { Text(tr("Read")) }, onClick = { close(); callbacks.onOpen(book) })
        AppMenuItem(text = { Text(tr("Edit")) }, onClick = { close(); callbacks.onEdit(book) })
        AppMenuItem(text = { Text(tr("Bookmarks")) }, onClick = { close(); callbacks.onBookmarks(book) })
        if (!selecting) AppMenuItem(text = { Text(tr("Select")) }, onClick = { close(); callbacks.onToggle(book) })
        AppMenuItem(text = { Text(if (book.isArchived) tr("Unarchive") else tr("Archive")) }, onClick = { close(); callbacks.onArchive(book) })
        AppMenuItem(text = { Text(tr("Delete")) }, onClick = { close(); callbacks.onDelete(book) })
    }
}

/** No books yet in the language: the free book pack, when there is one, and a book of one's own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EmptyLibrary(state: BooksUiState, onNewBook: () -> Unit, onDownloadBooks: () -> Unit, onFindBooks: () -> Unit = {}) {
    val language = state.languageName
    val downloading = state.packState is PackState.Downloading
    EmptyState(
        if (language.isEmpty()) tr("Choose a language to learn") else tr("Your library is empty"),
        Modifier.testTag("empty-library"),
        message = when {
            language.isEmpty() -> tr("Choose a language to learn to see its books.")
            state.packAvailable -> tr("Download the free {0} book pack, graded from A2 to C2, or add a text of your own.", languageInSentence(language, LanguageCase.PREPOSITIONAL))
            else -> tr("Add a text you want to read: paste it, or open a file.")
        },
        action = if (language.isEmpty()) null else ({
            (state.packState as? PackState.Failed)?.let { failed ->
                Text(tr(failed.message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(AppSpacing.l))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                if (state.packAvailable) {
                    PrimaryButton(
                        if (downloading) tr("Downloading…") else tr("Download the book pack"),
                        onClick = onDownloadBooks,
                        modifier = Modifier.testTag("download-books"),
                        icon = AppIcons.Download,
                        loading = downloading,
                    )
                }
                SecondaryButton(tr("Find free books"), onClick = onFindBooks, modifier = Modifier.testTag("empty-find-books"), icon = Icons.Default.Search)
                SecondaryButton(tr("Add a book"), onClick = onNewBook, icon = Icons.Default.Add)
            }
        }),
    )
}

/**
 * The archive with nothing in it: the mascot at an empty box beside what archiving does, the way back to the
 * library, and how archiving works.
 */
@Composable
internal fun EmptyArchive(onBackToBooks: () -> Unit) {
    var explaining by remember { mutableStateOf(false) }
    ArchiveEmptyCard(
        title = tr("No archived books yet"),
        message = tr("When you archive a book, it will appear here. You can restore it to your library whenever you're ready."),
        action = tr("Back to my books") to onBackToBooks,
        link = tr("How archiving works") to { explaining = true },
        tags = "archive-back" to "archive-how",
    )
    if (explaining) {
        AppDialog(
            onDismissRequest = { explaining = false },
            confirmButton = { QuietButton(tr("Got it"), onClick = { explaining = false }) },
            title = { Text(tr("How archiving works")) },
            text = {
                Text(tr("Archive a book from its ⋮ menu to take it out of your library without deleting it. Its pages, your progress and the words you saved from it stay. To read it again, open Archived books and choose Unarchive."))
            },
            modifier = Modifier.testTag("archive-how-dialog"),
        )
    }
}

/** The archive holds books, but none match the search or filters: the way to see them all, or back to the library. */
@Composable
internal fun NoArchivedMatches(onClearFilters: () -> Unit, onBackToBooks: () -> Unit) {
    ArchiveEmptyCard(
        title = tr("No archived books match"),
        message = tr("None of your archived books match this search or these filters. Clear them to see all your archived books."),
        action = tr("Clear filters") to onClearFilters,
        link = tr("Back to my books") to onBackToBooks,
        tags = "archive-clear" to "archive-back",
    )
}

/**
 * The archive's empty card: the mascot at an empty box beside a title, a line, one button and one link. Side by
 * side on a wide window, the picture above on a narrow one. [tags] name the button and the link for tests.
 */
@Composable
private fun ArchiveEmptyCard(title: String, message: String, action: Pair<String, () -> Unit>, link: Pair<String, () -> Unit>, tags: Pair<String, String>) {
    val wide = !LocalWindowWidth.current.isCompact
    val words = @Composable {
        Text(title, style = AppText.pageTitle)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton(action.first, onClick = action.second, modifier = Modifier.padding(top = AppSpacing.s).testTag(tags.first))
        LinkText(link.first, onClick = link.second, modifier = Modifier.testTag(tags.second))
    }
    val card = Modifier.fillMaxWidth().widthIn(max = EMPTY_ARCHIVE_MAX).cardSurface(AppShapes.large).padding(AppSpacing.xxxl).testTag("empty-archive")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        if (wide) {
            Row(card, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxxl)) {
                ArchivedBooksIllustration(Modifier.weight(1f).aspectRatio(ARCHIVE_PICTURE_RATIO))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) { words() }
            }
        } else {
            Column(card, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                ArchivedBooksIllustration(Modifier.fillMaxWidth().aspectRatio(ARCHIVE_PICTURE_RATIO))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) { words() }
            }
        }
    }
}

private val EMPTY_ARCHIVE_MAX = 1040.dp
/** The picture's own width to height. */
private const val ARCHIVE_PICTURE_RATIO = 1.5f

/**
 * No books in the archive, or none matching the filters. [onlyContinued] means the one match is the
 * book in the continue card above: the library holds nothing else, so the way on is another book.
 */
@Composable
internal fun NoBooks(
    archived: Boolean,
    onNewBook: () -> Unit,
    onlyContinued: Boolean = false,
    onFindBooks: () -> Unit = {},
    onClearFilters: () -> Unit = {},
    onBackToBooks: () -> Unit = {},
) {
    when {
        archived -> NoArchivedMatches(onClearFilters, onBackToBooks)
        onlyContinued -> EmptyState(tr("No other books yet"), action = { QuietButton(tr("Find books"), onClick = onFindBooks) })
        else -> EmptyState(tr("No books match these filters."), action = { QuietButton(tr("Create a book"), onClick = onNewBook) })
    }
}

internal fun words(count: Int) = trPlural(count, "{1} word", "{1} words", formatCount(count))

private val CONTINUE_COVER = 74.dp
private val CONTINUE_ROW = 760.dp
private val GRID_COVER = 72.dp
private val GRID_CARD = 330.dp
internal val SMALL_RING = 36.dp

/** The least the search keeps beside the status tabs before the tabs move to a row of their own. */
private val MIN_SEARCH = 220.dp
