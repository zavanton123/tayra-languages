package com.tayra.languages.feature.books

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
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
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.AppTriStateCheckbox
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.CountTabs
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ProgressTrack
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SectionHeader
import com.tayra.languages.core.ui.components.Segment
import com.tayra.languages.core.ui.components.SegmentStyle
import com.tayra.languages.core.ui.components.SegmentedToggle
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.SelectionBar
import com.tayra.languages.core.ui.components.Tab
import com.tayra.languages.core.ui.components.TableHeader
import com.tayra.languages.core.ui.components.TableHeaderCell
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.TagFilterButton
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.TagStyle
import com.tayra.languages.core.ui.components.TintedSurface
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.pressScale
import com.tayra.languages.core.ui.components.relativeTo
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Home screen: the book listing. Also used for the archive. */
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

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (archived) tr("Archived books") else if (compact) tr("Books") else "Tayra Languages",
                onNavigate = onNavigate,
                onBack = onBack,
                section = NavSection.BOOKS,
                actions = {
                    if (compact) PhoneBooksMenu(archived, selecting, onSelecting = { selecting = it; if (!it) viewModel.selectAll(false) }, onNewBook = { onNavigate(Route.NewBook) }, onArchive = { onNavigate(Route.ArchivedBooks) }, onFindBooks = { onNavigate(Route.FindBooks) })
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        val callbacks = BooksCallbacks(
                onOpen = { onNavigate(Route.Read(it.id)) },
                onEdit = { onNavigate(Route.EditBook(it.id)) },
                onArchive = { if (it.isArchived) viewModel.unarchive(it.id) else viewModel.archive(it.id) },
                onDelete = { pendingDelete = it },
                onBookmarks = { onNavigate(Route.Bookmarks(it.id)) },
                onNewBook = { onNavigate(Route.NewBook) },
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
    val onEdit: (BookListItem) -> Unit = {},
    val onArchive: (BookListItem) -> Unit = {},
    val onDelete: (BookListItem) -> Unit = {},
    val onBookmarks: (BookListItem) -> Unit = {},
    val onNewBook: () -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val onSort: (BookSort) -> Unit = {},
    val onTags: (tags: Set<String>, matchAll: Boolean) -> Unit = { _, _ -> },
    val onProgress: (ProgressFilter) -> Unit = {},
    val onView: (BooksView) -> Unit = {},
    val onToggle: (BookListItem) -> Unit = {},
    val onSelectAll: (Boolean) -> Unit = {},
    val onArchiveSelected: () -> Unit = {},
    val onDeleteSelected: () -> Unit = {},
    /** Downloads the language's bundle of free books. */
    val onDownloadBooks: () -> Unit = {},
    /** Opens the free books of online libraries. */
    val onFindBooks: () -> Unit = {},
)

/** The page below the top bar, from the header to the list of books. */
@Composable
internal fun BooksContent(state: BooksUiState, callbacks: BooksCallbacks, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    // The table needs a wide window; narrower ones list the books as cards.
    val wide = LocalWindowWidth.current.isExpanded
    val books = state.filteredBooks
    // Ticking books for the bulk actions starts with Select, so the list stays plain otherwise.
    var selecting by rememberSaveable { mutableStateOf(false) }
    val showChecks = selecting || state.selectedBooks.isNotEmpty()
    ScrollList(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
    ) {
        item {
            Column(Modifier.padding(bottom = AppSpacing.xxl)) {
                ScreenHeader(
                    title = if (state.archived) tr("Archived books") else tr("Books"),
                    subtitle = if (state.archived) tr("Books you have set aside. Unarchive one to keep reading it.") else tr("Read, learn, and build your vocabulary with every text."),
                ) {
                    if (!state.archived) {
                        SecondaryButton(tr("Find books"), onClick = callbacks.onFindBooks, modifier = Modifier.testTag("find-books"), icon = Icons.Default.Search)
                        PrimaryButton(tr("New book"), onClick = callbacks.onNewBook, icon = Icons.Default.Add)
                    }
                }
            }
        }
        // An empty library: the way in is a bundle of free books, or a first book of one's own.
        if (!state.archived && state.books.isEmpty()) {
            item { EmptyLibrary(state, callbacks.onNewBook, callbacks.onDownloadBooks, callbacks.onFindBooks) }
            return@ScrollList
        }
        if (!state.archived) state.continueWith?.let { book -> item { ContinueCard(book) { callbacks.onOpen(book) } } }
        item { ProgressTabs(state.counts, state.progress, callbacks.onProgress) }
        item { Toolbar(state, compact, wide, books.size, callbacks) }
        item {
            LibraryHeader(
                state,
                showChecks,
                onSelect = { selecting = true },
                onDone = { selecting = false; callbacks.onSelectAll(false) },
                callbacks,
            )
        }
        item {
            when {
                books.isEmpty() -> NoBooks(state.archived, callbacks.onNewBook)
                !wide || state.view == BooksView.GRID -> BookGrid(books, state.selected, showChecks, compact, callbacks)
                else -> BookTable(state, showChecks, callbacks)
            }
        }
        item { Footer(state) }
    }
}

/** The book being read most recently: where the reader is, what they know of it, and the way back in. */
@Composable
private fun ContinueCard(book: BookListItem, onContinue: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val title: @Composable (Modifier) -> Unit = { m ->
        Row(m, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
            Box(Modifier.size(CONTINUE_ICON_TILE).clip(AppShapes.circle).background(colors.primary.copy(alpha = AppAlpha.tint)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.MenuBook, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconXLarge))
            }
            Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(tr("Continue reading"), style = AppText.smallTitle, color = colors.primary)
                Text(book.title, style = AppText.sectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    tr("Page {0} of {1} · {2}", book.currentPage, book.pageCount, words(book.wordCount)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
    val bar: @Composable (String, Int?, Color, Modifier) -> Unit = { label, percent, tint, m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            PercentBar(percent, tint)
        }
    }
    val button: @Composable (Modifier) -> Unit = { m ->
        PrimaryButton(tr("Continue"), onClick = onContinue, modifier = m.testTag("continue-reading"), size = ButtonSize.Large)
    }
    TintedSurface(colors.primary, Modifier.padding(bottom = AppSpacing.xxl), shape = AppShapes.large, padding = PaddingValues(horizontal = AppSpacing.xxl, vertical = AppSpacing.xl)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxxl)) {
            title(Modifier.weight(1.2f))
            bar(tr("Reading progress"), book.progressPercent, AppAccents.blue, Modifier.weight(1f))
            AppVerticalDivider()
            bar(tr("Vocabulary known"), book.masteryPercent, AppAccents.green, Modifier.weight(1f))
            button(Modifier.width(CONTINUE_BUTTON_WIDTH))
        }
    }
}

internal val ProgressFilter.tabLabel: String
    get() = when (this) {
        ProgressFilter.ALL -> tr("All")
        else -> tr(label)
    }

/** All books, then those in progress, not started and finished, each with how many it holds; [compact] is the phone's smaller row. */
@Composable
internal fun ProgressTabs(counts: Map<ProgressFilter, Int>, selected: ProgressFilter, onSelect: (ProgressFilter) -> Unit, compact: Boolean = false) {
    CountTabs(
        listOf(ProgressFilter.ALL, ProgressFilter.READING, ProgressFilter.NOT_STARTED, ProgressFilter.FINISHED).map { Tab(it, it.tabLabel, counts[it] ?: 0, testTag = "progress-${it.name}") },
        selected,
        onSelect,
        Modifier.padding(bottom = if (compact) AppSpacing.l else AppSpacing.xl),
        style = SegmentStyle.Soft,
        compact = compact,
    )
}

@Composable
private fun Toolbar(state: BooksUiState, compact: Boolean, wide: Boolean, count: Int, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val sortMenu: @Composable (Modifier) -> Unit = { SelectMenu(BookSort.entries, state.sort, callbacks.onSort, { tr(it.label) }, it, icon = AppIcons.SwapVert) }
    val hasTags = state.availableTags.isNotEmpty()
    val tagFilter: @Composable (Modifier) -> Unit = { TagFilterButton(state.availableTags, state.tags, state.matchAllTags, callbacks.onTags, it) }
    if (compact) {
        Column(Modifier.fillMaxWidth().padding(bottom = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            SearchField(state.search, callbacks.onSearch, tr("Search titles or tags"), Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                if (hasTags) tagFilter(Modifier)
                sortMenu(Modifier.weight(1f))
            }
        }
        return
    }
    Layout(
        content = {
            SearchField(state.search, callbacks.onSearch, tr("Search titles or tags"))
            if (hasTags) tagFilter(Modifier) else Spacer(Modifier)
            sortMenu(Modifier)
            // Only wide windows have the table to switch to.
            if (wide) ViewToggle(state.view, callbacks.onView) else Spacer(Modifier)
            Text(trPlural(count, "{0} book", "{0} books"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, softWrap = false)
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = AppSpacing.xxl),
    ) { measurables, constraints ->
        // The search takes what the other controls leave, up to a comfortable width; when too
        // little is left it gets a row of its own above them.
        val gap = 14.dp.roundToPx()
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val search = measurables[0]
        // Tags and sort after the search; the view toggle and the count at the far end.
        val controls = measurables.drop(1).map { it.measure(loose) }
        val controlsWidth = controls.sumOf { it.width } + gap * 3 + 16.dp.roundToPx()
        val searchWidth = (width - controlsWidth - gap).coerceAtMost(520.dp.roundToPx())
        val ownRow = searchWidth < 200.dp.roundToPx()
        val field = search.measure(Constraints.fixedWidth(if (ownRow) width else searchWidth))
        val rowHeight = maxOf(field.height, controls.maxOf { it.height })
        val top = if (ownRow) field.height + 12.dp.roundToPx() else 0
        layout(width, top + rowHeight) {
            fun centred(p: androidx.compose.ui.layout.Placeable, x: Int) = p.place(x, top + (rowHeight - p.height) / 2)
            var x = 0
            if (ownRow) field.place(0, 0) else { field.place(0, (rowHeight - field.height) / 2); x = field.width + gap }
            centred(controls[0], x)
            centred(controls[1], x + controls[0].width + (if (controls[0].width > 0) gap else 0))
            centred(controls[3], width - controls[3].width)
            centred(controls[2], width - controls[3].width - 20.dp.roundToPx() - controls[2].width)
        }
    }
}

/** "Your library", with the way into ticking books, or the bulk actions on the ticked ones. */
@Composable
private fun LibraryHeader(state: BooksUiState, selecting: Boolean, onSelect: () -> Unit, onDone: () -> Unit, callbacks: BooksCallbacks) {
    if (selecting) {
        BooksSelectionBar(state, callbacks, onDone)
        return
    }
    SectionHeader(if (state.archived) tr("Archive") else tr("Your library"), Modifier.padding(bottom = AppSpacing.l)) {
        if (state.filteredBooks.isNotEmpty()) QuietButton(tr("Select"), onClick = onSelect, modifier = Modifier.testTag("select-books"))
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
    )
}

// Column weights shared by the table header and rows.
private const val BOOK_WEIGHT = 3.4f
private const val STATUS_WIDTH = 140
private const val PROGRESS_WEIGHT = 2.2f
private const val KNOWN_WEIGHT = 2.2f
private const val OPENED_WEIGHT = 1.3f
private val MENU_WIDTH = 72.dp
private val CONTINUE_ICON_TILE = 76.dp
private val CONTINUE_BUTTON_WIDTH = 180.dp
private val PERCENT_WIDTH = 40.dp

@Composable
private fun BookTable(state: BooksUiState, selecting: Boolean, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().cardSurface()) {
        BookTableHeader(state, selecting, callbacks)
        state.filteredBooks.forEach { book ->
            AppDivider(color = colors.outlineVariant.copy(alpha = AppAlpha.muted))
            BookTableRow(book, book.id in state.selected, selecting, callbacks)
        }
    }
}

@Composable
private fun BookTableHeader(state: BooksUiState, selecting: Boolean, callbacks: BooksCallbacks) {
    val sort = state.sort
    val onSort = callbacks.onSort
    TableHeader(padding = PaddingValues(start = if (selecting) AppSpacing.s else AppSpacing.xxl, end = AppSpacing.m, top = AppSpacing.s, bottom = AppSpacing.s)) {
        // Ticking all is in the selection bar above; this keeps the columns over the rows' checkboxes.
        if (selecting) Spacer(Modifier.width(CHECKBOX_WIDTH + CHECKBOX_GAP))
        TableHeaderCell(tr("Book"), Modifier.weight(BOOK_WEIGHT), active = sort == BookSort.TITLE) { onSort(BookSort.TITLE) }
        TableHeaderCell(tr("Status"), Modifier.width(STATUS_WIDTH.dp))
        TableHeaderCell(tr("Reading"), Modifier.weight(PROGRESS_WEIGHT))
        TableHeaderCell(tr("Vocabulary"), Modifier.weight(KNOWN_WEIGHT), active = sort == BookSort.MASTERY) { onSort(BookSort.MASTERY) }
        TableHeaderCell(tr("Last opened"), Modifier.weight(OPENED_WEIGHT), active = sort == BookSort.RECENT) { onSort(BookSort.RECENT) }
        Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { TableHeaderCell(tr("Actions")) }
    }
}

@Composable
private fun BookTableRow(book: BookListItem, selected: Boolean, selecting: Boolean, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background = when {
        selected -> colors.primary.copy(alpha = AppAlpha.soft)
        hovered -> colors.primary.copy(alpha = AppAlpha.faint)
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().background(background)
            .hoverable(interaction).clickable { callbacks.onOpen(book) }.padding(start = if (selecting) AppSpacing.s else AppSpacing.xxl, end = AppSpacing.m, top = AppSpacing.m, bottom = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            BookCheckbox(book, selected, callbacks)
            Spacer(Modifier.width(CHECKBOX_GAP))
        }
        Row(Modifier.weight(BOOK_WEIGHT).padding(end = AppSpacing.xl), verticalAlignment = Alignment.CenterVertically) {
            BookCover(book, 52.dp)
            Spacer(Modifier.width(AppSpacing.l))
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(book.title, style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(words(book.wordCount), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                BookTags(book.tags)
            }
        }
        Box(Modifier.width(STATUS_WIDTH.dp)) { StatusTag(book.readingStatus) }
        Box(Modifier.weight(PROGRESS_WEIGHT).padding(end = AppSpacing.xxxl)) { PercentBar(book.progressPercent, AppAccents.blue) }
        Box(Modifier.weight(KNOWN_WEIGHT).padding(end = AppSpacing.xxxl)) { PercentBar(book.masteryPercent, AppAccents.green) }
        Text(book.lastOpened?.relativeTo() ?: tr("Not opened yet"), Modifier.weight(OPENED_WEIGHT).padding(end = AppSpacing.l), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { BookMenu(book, callbacks) }
    }
}

@Composable
internal fun StatusTag(status: ReadingStatus, size: TagSize = TagSize.Large) {
    val (label, tint) = when (status) {
        ReadingStatus.READING -> tr("In progress") to AppAccents.blue
        ReadingStatus.FINISHED -> tr("Finished") to AppAccents.green
        ReadingStatus.NOT_STARTED -> tr("Not started") to MaterialTheme.colorScheme.outline
    }
    Tag(label, tint, size = size)
}

/** A book's tags as small grey chips, the first few of them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BookTags(tags: List<String>, max: Int = 3) {
    if (tags.isEmpty()) return
    FlowRow(Modifier.padding(top = AppSpacing.xxs), horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        (tags.take(max) + listOfNotNull(if (tags.size > max) "+${tags.size - max}" else null)).forEach { tag ->
            Tag(tag, size = TagSize.Small, style = TagStyle.Neutral)
        }
    }
}

/** A thin bar filled to [percent] with the figure beside it; "…" while the figure is not known yet. */
@Composable
internal fun PercentBar(percent: Int?, color: Color) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        ProgressTrack((percent ?: 0) / 100f, color, Modifier.weight(1f), height = AppSizes.trackThick)
        Text(percent?.let { "$it%" } ?: "…", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.width(PERCENT_WIDTH), softWrap = false)
    }
}

@Composable
private fun BookGrid(books: List<BookListItem>, selected: Set<Long>, selecting: Boolean, compact: Boolean, callbacks: BooksCallbacks) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // As many cards of at least 330 wide as fit, sharing the row.
        val gap = 16.dp
        val columns = if (compact) 1 else ((maxWidth + gap) / (330.dp + gap)).toInt().coerceAtLeast(1)
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

@Composable
private fun BookCard(book: BookListItem, selected: Boolean, selecting: Boolean, callbacks: BooksCallbacks, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShapes.card
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier.pressScale(interaction).clip(shape).border(AppBorders.hairline(if (selected || hovered) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), shape)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
            .hoverable(interaction).clickable(interaction, LocalIndication.current) { callbacks.onOpen(book) }.padding(AppSpacing.xl),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (selecting) {
                Box(Modifier.offset(x = (-12).dp, y = (-8).dp)) { BookCheckbox(book, selected, callbacks) }
            }
            BookCover(book, 88.dp)
            Spacer(Modifier.width(AppSpacing.xl))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(book.title, style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(words(book.wordCount), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(AppSpacing.xs))
                BookTags(book.tags)
            }
            Box(Modifier.offset(x = 12.dp, y = (-8).dp)) { BookMenu(book, callbacks) }
        }
        Spacer(Modifier.height(AppSpacing.xl))
        Spacer(Modifier.weight(1f))
        Text(tr("Reading"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(AppSpacing.s))
        PercentBar(book.progressPercent, AppAccents.blue)
        Spacer(Modifier.height(AppSpacing.l))
        Text(tr("Vocabulary"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(AppSpacing.s))
        PercentBar(book.masteryPercent, AppAccents.green)
        Spacer(Modifier.height(AppSpacing.xl))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { StatusTag(book.readingStatus) }
            Text(
                book.lastOpened?.let { tr("Opened {0}", it.relativeTo()) } ?: tr("Not opened yet"),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/**
 * A book's cover: a tinted page with the language's code and a simple figure, both picked from
 * the book's id, so each book keeps its look and books side by side differ.
 */
@Composable
internal fun BookCover(book: BookListItem, width: Dp) {
    val covers = AppAccents.covers
    val tint = covers[(book.id % covers.size).toInt().let { if (it < 0) -it else it }]
    val figure = ((book.id + book.id / covers.size) % 4).toInt().let { if (it < 0) -it else it }
    val code = LanguageCodes.codeFor(book.languageName)?.uppercase() ?: book.languageName.take(2).uppercase()
    val shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp)
    Box(Modifier.size(width = width, height = width * 1.3f).clip(shape).background(tint.copy(alpha = AppAlpha.line))) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val ink = tint.copy(alpha = AppAlpha.strong)
            when (figure) {
                // A hill rising from the bottom.
                0 -> drawOval(ink, topLeft = Offset(w * 0.2f, h * 0.62f), size = Size(w * 1.1f, h * 0.7f))
                // Two lines of text.
                1 -> {
                    drawRoundRect(ink, topLeft = Offset(w * 0.32f, h * 0.6f), size = Size(w * 0.5f, h * 0.07f), cornerRadius = CornerRadius(h * 0.03f))
                    drawRoundRect(ink.copy(alpha = AppAlpha.muted), topLeft = Offset(w * 0.32f, h * 0.74f), size = Size(w * 0.5f, h * 0.07f), cornerRadius = CornerRadius(h * 0.03f))
                }
                // An arch.
                2 -> drawArc(ink, startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = Offset(w * 0.3f, h * 0.55f), size = Size(w * 1.1f, h * 0.9f), style = Stroke(width = w * 0.14f))
                // Mountains.
                else -> {
                    drawPath(Path().apply { moveTo(w * 0.1f, h); lineTo(w * 0.5f, h * 0.5f); lineTo(w * 0.85f, h); close() }, ink)
                    drawPath(Path().apply { moveTo(w * 0.45f, h); lineTo(w * 0.75f, h * 0.62f); lineTo(w * 1.05f, h); close() }, ink.copy(alpha = AppAlpha.muted))
                }
            }
            // The spine.
            drawRect(tint.copy(alpha = AppAlpha.disabled), size = Size(w * 0.05f, h))
        }
        Text(
            code,
            Modifier.padding(start = width * 0.16f, top = width * 0.1f),
            style = if (width > 64.dp) AppText.sectionTitle else AppText.smallTitle,
            color = tint,
        )
    }
}

private val CHECKBOX_GAP = 4.dp
private val CHECKBOX_WIDTH = 48.dp

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
        Modifier.padding(bottom = AppSpacing.l),
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

/** Below the books: how many there are and the words known in the language, with the streak where it is shown. */
@Composable
internal fun Footer(state: BooksUiState) {
    val parts = buildList {
        add(trPlural(state.books.size, "{1} book", "{1} books", formatCount(state.books.size)))
        if (!state.archived) {
            add(trPlural(state.wordsLearned, "{1} word learned", "{1} words learned", formatCount(state.wordsLearned)))
            if (state.showStreak && state.streak > 0) add(trPlural(state.streak, "{0}-day streak", "{0}-day streak"))
        }
    }
    Text(parts.joinToString("  ·  "), Modifier.padding(top = AppSpacing.xl), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun BookMenu(book: BookListItem, callbacks: BooksCallbacks) {
    OverflowMenu(tr("Actions")) { close ->
        AppMenuItem(text = { Text(tr("Read")) }, onClick = { close(); callbacks.onOpen(book) })
        AppMenuItem(text = { Text(tr("Edit")) }, onClick = { close(); callbacks.onEdit(book) })
        AppMenuItem(text = { Text(tr("Bookmarks")) }, onClick = { close(); callbacks.onBookmarks(book) })
        AppMenuItem(text = { Text(if (book.isArchived) tr("Unarchive") else tr("Archive")) }, onClick = { close(); callbacks.onArchive(book) })
        AppMenuItem(text = { Text(tr("Delete")) }, onClick = { close(); callbacks.onDelete(book) })
    }
}

/** No books yet in the language: the free bundle, when there is one, and a book of one's own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EmptyLibrary(state: BooksUiState, onNewBook: () -> Unit, onDownloadBooks: () -> Unit, onFindBooks: () -> Unit = {}) {
    val language = state.languageName
    val downloading = state.packState is PackState.Downloading
    EmptyState(
        AppIcons.MenuBook,
        if (language.isEmpty()) tr("Choose a language to learn") else tr("Your library is empty"),
        Modifier.testTag("empty-library"),
        message = when {
            language.isEmpty() -> tr("Choose a language to learn to see its books.")
            state.packAvailable -> tr("Download a bundle of free {0} books graded from A2 to C2, or add a text of your own.", languageInSentence(language, LanguageCase.PREPOSITIONAL))
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
                        if (downloading) tr("Downloading…") else tr("Download the book bundle"),
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

/** No books in the archive, or none matching the filters. */
@Composable
internal fun NoBooks(archived: Boolean, onNewBook: () -> Unit) {
    EmptyState(
        AppIcons.Book,
        if (archived) tr("No archived books.") else tr("No books match these filters."),
        action = if (archived) null else ({ QuietButton(tr("Create a book"), onClick = onNewBook) }),
    )
}

internal fun words(count: Int) = trPlural(count, "{1} word", "{1} words", count.grouped())

/** 1223 → "1,223". */
private fun Int.grouped(): String = formatCount(this)
