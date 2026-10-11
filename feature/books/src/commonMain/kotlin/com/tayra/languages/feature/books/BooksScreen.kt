package com.tayra.languages.feature.books

import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.TagFilterButton
import com.tayra.languages.core.ui.components.relativeTo
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.tayra.languages.core.ui.components.ScrollList

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

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (archived) tr("Archived books") else "Tayra Languages",
                onNavigate = onNavigate,
                onBack = onBack,
                section = NavSection.BOOKS,
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        BooksContent(
            state = state,
            callbacks = BooksCallbacks(
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
            ),
            modifier = Modifier.padding(padding),
        )
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
        contentPadding = PaddingValues(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 24.dp),
    ) {
        item { PageHeader(state.archived, compact, callbacks.onNewBook) }
        if (!state.archived) state.continueWith?.let { book -> item { ContinueCard(book, compact) { callbacks.onOpen(book) } } }
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
                books.isEmpty() -> EmptyState(state.archived, callbacks.onNewBook)
                !wide || state.view == BooksView.GRID -> BookGrid(books, state.selected, showChecks, compact, callbacks)
                else -> BookTable(state, showChecks, callbacks)
            }
        }
        item { Footer(state) }
    }
}

@Composable
private fun PageHeader(archived: Boolean, compact: Boolean, onNewBook: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (archived) tr("Archived books") else tr("Books"),
                style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (archived) tr("Books you have set aside. Unarchive one to keep reading it.") else tr("Read, learn, and build your vocabulary with every text."),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!archived) {
            Button(onClick = onNewBook, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("New book"), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private val BLUE = Color(0xFF3B6FE0)
private val GREEN = Color(0xFF2E9D57)
private val PROGRESS = Color(0xFF4A90E2)
private val KNOWN = Color(0xFF4CC38A)

/** The book being read most recently: where the reader is, what they know of it, and the way back in. */
@Composable
private fun ContinueCard(book: BookListItem, compact: Boolean, onContinue: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val card = Modifier.fillMaxWidth().padding(bottom = 24.dp).clip(shape).background(colors.primary.copy(alpha = 0.06f)).border(1.dp, colors.primary.copy(alpha = 0.16f), shape)
    val title: @Composable (Modifier) -> Unit = { m ->
        Row(m, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(if (compact) 52.dp else 76.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.MenuBook, contentDescription = null, tint = colors.primary, modifier = Modifier.size(if (compact) 26.dp else 36.dp))
            }
            Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tr("Continue reading"), style = MaterialTheme.typography.titleSmall, color = colors.primary, fontWeight = FontWeight.SemiBold)
                Text(book.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    tr("Page {0} of {1} · {2}% read", book.currentPage, book.pageCount, book.progressPercent),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
    val bar: @Composable (String, Int?, Color, Modifier) -> Unit = { label, percent, tint, m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            PercentBar(percent, tint)
        }
    }
    val button: @Composable (Modifier) -> Unit = { m ->
        Button(onClick = onContinue, shape = RoundedCornerShape(12.dp), modifier = m.height(48.dp).testTag("continue-reading")) {
            Text(tr("Continue"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
    if (compact) {
        Column(card.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            title(Modifier)
            bar(tr("Reading progress"), book.progressPercent, PROGRESS, Modifier.fillMaxWidth())
            bar(tr("Vocabulary known"), book.masteryPercent, KNOWN, Modifier.fillMaxWidth())
            button(Modifier.fillMaxWidth())
        }
    } else {
        Row(card.height(IntrinsicSize.Min).padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            title(Modifier.weight(1.2f))
            bar(tr("Reading progress"), book.progressPercent, PROGRESS, Modifier.weight(1f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.outlineVariant))
            bar(tr("Vocabulary known"), book.masteryPercent, KNOWN, Modifier.weight(1f))
            button(Modifier.width(180.dp))
        }
    }
}

private val ProgressFilter.tabLabel: String
    get() = when (this) {
        ProgressFilter.ALL -> tr("All")
        else -> tr(label)
    }

/** All books, then those in progress, not started and finished, each with how many it holds. */
@Composable
private fun ProgressTabs(counts: Map<ProgressFilter, Int>, selected: ProgressFilter, onSelect: (ProgressFilter) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.padding(bottom = 18.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(ProgressFilter.ALL, ProgressFilter.READING, ProgressFilter.NOT_STARTED, ProgressFilter.FINISHED).forEach { filter ->
            val active = filter == selected
            Row(
                Modifier.height(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (active) colors.primary.copy(alpha = 0.1f) else Color.Transparent)
                    .clickable { onSelect(filter) }.padding(horizontal = 16.dp).testTag("progress-${filter.name}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(filter.tabLabel, style = MaterialTheme.typography.titleMedium, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium, color = if (active) colors.primary else colors.onSurfaceVariant, softWrap = false)
                Text(
                    formatCount(counts[filter] ?: 0),
                    Modifier.clip(RoundedCornerShape(50)).background(if (active) colors.primary.copy(alpha = 0.14f) else colors.onSurface.copy(alpha = 0.07f)).padding(horizontal = 8.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Toolbar(state: BooksUiState, compact: Boolean, wide: Boolean, count: Int, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val sortMenu: @Composable (Modifier) -> Unit = { FilterMenu(AppIcons.SwapVert, tr(state.sort.label), BookSort.entries, { tr(it.label) }, callbacks.onSort, it) }
    val hasTags = state.availableTags.isNotEmpty()
    val tagFilter: @Composable (Modifier) -> Unit = { TagFilterButton(state.availableTags, state.tags, state.matchAllTags, callbacks.onTags, it) }
    if (compact) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchBox(state.search, callbacks.onSearch, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasTags) tagFilter(Modifier)
                sortMenu(Modifier.weight(1f))
            }
        }
        return
    }
    Layout(
        content = {
            SearchBox(state.search, callbacks.onSearch)
            if (hasTags) tagFilter(Modifier) else Spacer(Modifier)
            sortMenu(Modifier)
            // Only wide windows have the table to switch to.
            if (wide) ViewToggle(state.view, callbacks.onView) else Spacer(Modifier)
            Text(trPlural(count, "{0} book", "{0} books"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, softWrap = false)
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
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
        SelectionBar(state, callbacks, onDone)
        return
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (state.archived) tr("Archive") else tr("Your library"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (state.filteredBooks.isNotEmpty()) TextButton(onClick = onSelect, modifier = Modifier.testTag("select-books")) { Text(tr("Select")) }
    }
}

private val CONTROL_HEIGHT = 48.dp

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(CONTROL_HEIGHT).clip(RoundedCornerShape(10.dp)).background(colors.surface)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(tr("Search titles or tags"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun <T> FilterMenu(icon: ImageVector, label: String, options: List<T>, optionLabel: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier.height(CONTROL_HEIGHT).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp))
                .background(colors.surface).clickable { open = true }.padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text(label, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                AppMenuItem(text = { Text(optionLabel(option)) }, onClick = { open = false; onSelect(option) })
            }
        }
    }
}

@Composable
private fun ViewToggle(selected: BooksView, onSelect: (BooksView) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.height(CONTROL_HEIGHT).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp))
            .background(colors.surface).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(BooksView.LIST to AppIcons.ViewList, BooksView.GRID to AppIcons.GridView).forEach { (view, icon) ->
            val active = view == selected
            Box(
                Modifier.fillMaxHeight().width(44.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (active) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelect(view) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = if (view == BooksView.LIST) tr("list view") else tr("grid view"), tint = if (active) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// Column weights shared by the table header and rows.
private const val BOOK_WEIGHT = 3.4f
private const val STATUS_WIDTH = 140
private const val PROGRESS_WEIGHT = 2.2f
private const val KNOWN_WEIGHT = 2.2f
private const val OPENED_WEIGHT = 1.3f
private val MENU_WIDTH = 72.dp

@Composable
private fun BookTable(state: BooksUiState, selecting: Boolean, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp)).background(colors.surface),
    ) {
        TableHeader(state, selecting, callbacks)
        state.filteredBooks.forEach { book ->
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.7f))
            BookTableRow(book, book.id in state.selected, selecting, callbacks)
        }
    }
}

@Composable
private fun TableHeader(state: BooksUiState, selecting: Boolean, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val sort = state.sort
    val onSort = callbacks.onSort
    CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant, LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
        Row(
            Modifier.fillMaxWidth().background(colors.surfaceVariant.copy(alpha = 0.35f)).padding(start = if (selecting) 8.dp else 24.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ticking all is in the selection bar above; this keeps the columns over the rows' checkboxes.
            if (selecting) Spacer(Modifier.width(CHECKBOX_WIDTH + CHECKBOX_GAP))
            HeaderCell(tr("Book"), BOOK_WEIGHT, active = sort == BookSort.TITLE) { onSort(BookSort.TITLE) }
            Spacer(Modifier.width(STATUS_WIDTH.dp))
            HeaderCell(tr("Reading"), PROGRESS_WEIGHT)
            HeaderCell(tr("Vocabulary"), KNOWN_WEIGHT, active = sort == BookSort.MASTERY) { onSort(BookSort.MASTERY) }
            HeaderCell(tr("Last opened"), OPENED_WEIGHT, active = sort == BookSort.RECENT) { onSort(BookSort.RECENT) }
            Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { Text(tr("Actions"), fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(label: String, weight: Float, active: Boolean = false, onClick: (() -> Unit)? = null) {
    Box(Modifier.weight(weight)) {
        val tint = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current
        Text(
            label,
            (if (onClick != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick) else Modifier).padding(vertical = 2.dp),
            color = tint,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun BookTableRow(book: BookListItem, selected: Boolean, selecting: Boolean, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background = when {
        selected -> colors.primary.copy(alpha = 0.07f)
        hovered -> colors.primary.copy(alpha = 0.04f)
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().background(background)
            .hoverable(interaction).clickable { callbacks.onOpen(book) }.padding(start = if (selecting) 8.dp else 24.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            BookCheckbox(book, selected, callbacks)
            Spacer(Modifier.width(CHECKBOX_GAP))
        }
        Row(Modifier.weight(BOOK_WEIGHT).padding(end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(book, 52.dp)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(words(book.wordCount), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                BookTags(book.tags)
            }
        }
        Box(Modifier.width(STATUS_WIDTH.dp)) { StatusTag(book.readingStatus) }
        Box(Modifier.weight(PROGRESS_WEIGHT).padding(end = 32.dp)) { PercentBar(book.progressPercent, PROGRESS) }
        Box(Modifier.weight(KNOWN_WEIGHT).padding(end = 32.dp)) { PercentBar(book.masteryPercent, KNOWN) }
        Text(book.lastOpened?.relativeTo() ?: tr("Not opened yet"), Modifier.weight(OPENED_WEIGHT).padding(end = 16.dp), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { BookMenu(book, callbacks) }
    }
}

@Composable
private fun StatusTag(status: ReadingStatus) {
    val (label, tint) = when (status) {
        ReadingStatus.READING -> tr("In progress") to BLUE
        ReadingStatus.FINISHED -> tr("Finished") to GREEN
        ReadingStatus.NOT_STARTED -> tr("Not started") to MaterialTheme.colorScheme.outline
    }
    Text(
        label,
        Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = tint,
        maxLines = 1,
        softWrap = false,
    )
}

/** A book's tags as small grey chips, the first few of them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookTags(tags: List<String>, max: Int = 3) {
    if (tags.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    FlowRow(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (tags.take(max) + listOfNotNull(if (tags.size > max) "+${tags.size - max}" else null)).forEach { tag ->
            Text(
                tag,
                Modifier.clip(RoundedCornerShape(50)).background(colors.onSurface.copy(alpha = 0.06f)).padding(horizontal = 10.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** A thin bar filled to [percent] with the figure beside it; "…" while the figure is not known yet. */
@Composable
private fun PercentBar(percent: Int?, color: Color) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.onSurface.copy(alpha = 0.07f))) {
            if (percent != null && percent > 0) Box(Modifier.fillMaxWidth(percent / 100f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
        }
        Text(percent?.let { "$it%" } ?: "…", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.width(40.dp), softWrap = false)
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
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier.clip(shape).border(1.dp, if (selected || hovered) colors.primary.copy(alpha = 0.5f) else colors.outlineVariant, shape)
            .background(if (selected) colors.primary.copy(alpha = 0.05f) else colors.surface)
            .hoverable(interaction).clickable { callbacks.onOpen(book) }.padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (selecting) {
                Box(Modifier.offset(x = (-12).dp, y = (-8).dp)) { BookCheckbox(book, selected, callbacks) }
            }
            BookCover(book, 88.dp)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(words(book.wordCount), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                BookTags(book.tags)
            }
            Box(Modifier.offset(x = 12.dp, y = (-8).dp)) { BookMenu(book, callbacks) }
        }
        Spacer(Modifier.height(18.dp))
        Spacer(Modifier.weight(1f))
        Text(tr("Reading"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        PercentBar(book.progressPercent, PROGRESS)
        Spacer(Modifier.height(14.dp))
        Text(tr("Vocabulary"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        PercentBar(book.masteryPercent, KNOWN)
        Spacer(Modifier.height(18.dp))
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

private val coverTints = listOf(Color(0xFF5B8DEF), Color(0xFF9B7BEA), Color(0xFF34B5A0), Color(0xFFF0A13A), Color(0xFFE2738F), Color(0xFF6CA85A))

/**
 * A book's cover: a tinted page with the language's code and a simple figure, both picked from
 * the book's id, so each book keeps its look and books side by side differ.
 */
@Composable
private fun BookCover(book: BookListItem, width: Dp) {
    val tint = coverTints[(book.id % coverTints.size).toInt().let { if (it < 0) -it else it }]
    val figure = ((book.id + book.id / coverTints.size) % 4).toInt().let { if (it < 0) -it else it }
    val code = LanguageCodes.codeFor(book.languageName)?.uppercase() ?: book.languageName.take(2).uppercase()
    val shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp)
    Box(Modifier.size(width = width, height = width * 1.3f).clip(shape).background(tint.copy(alpha = 0.22f))) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val ink = tint.copy(alpha = 0.85f)
            when (figure) {
                // A hill rising from the bottom.
                0 -> drawOval(ink, topLeft = Offset(w * 0.2f, h * 0.62f), size = Size(w * 1.1f, h * 0.7f))
                // Two lines of text.
                1 -> {
                    drawRoundRect(ink, topLeft = Offset(w * 0.32f, h * 0.6f), size = Size(w * 0.5f, h * 0.07f), cornerRadius = CornerRadius(h * 0.03f))
                    drawRoundRect(ink.copy(alpha = 0.6f), topLeft = Offset(w * 0.32f, h * 0.74f), size = Size(w * 0.5f, h * 0.07f), cornerRadius = CornerRadius(h * 0.03f))
                }
                // An arch.
                2 -> drawArc(ink, startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = Offset(w * 0.3f, h * 0.55f), size = Size(w * 1.1f, h * 0.9f), style = Stroke(width = w * 0.14f))
                // Mountains.
                else -> {
                    drawPath(Path().apply { moveTo(w * 0.1f, h); lineTo(w * 0.5f, h * 0.5f); lineTo(w * 0.85f, h); close() }, ink)
                    drawPath(Path().apply { moveTo(w * 0.45f, h); lineTo(w * 0.75f, h * 0.62f); lineTo(w * 1.05f, h); close() }, ink.copy(alpha = 0.65f))
                }
            }
            // The spine.
            drawRect(tint.copy(alpha = 0.35f), size = Size(w * 0.05f, h))
        }
        Text(
            code,
            Modifier.padding(start = width * 0.16f, top = width * 0.1f),
            style = if (width > 64.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}

private val CHECKBOX_GAP = 4.dp
private val CHECKBOX_WIDTH = 48.dp

@Composable
private fun BookCheckbox(book: BookListItem, selected: Boolean, callbacks: BooksCallbacks) {
    Checkbox(checked = selected, onCheckedChange = { callbacks.onToggle(book) }, modifier = Modifier.semantics { contentDescription = tr("Select {0}", book.title) })
}

/** While books are being ticked: how many, all of them at once, the bulk actions, and Done. */
@Composable
private fun SelectionBar(state: BooksUiState, callbacks: BooksCallbacks, onDone: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val count = state.selectedBooks.size
    val all = count > 0 && count == state.filteredBooks.size
    Row(
        Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(alpha = 0.07f))
            .border(1.dp, colors.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = all, onCheckedChange = callbacks.onSelectAll, modifier = Modifier.semantics { contentDescription = tr("Select all books") })
        Text(tr("{0} of {1} selected", count, state.filteredBooks.size), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        TextButton(onClick = callbacks.onArchiveSelected, enabled = count > 0) { Text(if (state.archived) tr("Unarchive") else tr("Archive")) }
        TextButton(onClick = callbacks.onDeleteSelected, enabled = count > 0) { Text(tr("Delete"), color = if (count > 0) colors.error else colors.outline) }
        TextButton(onClick = onDone, modifier = Modifier.testTag("done-selecting")) { Text(tr("Done")) }
    }
}

/** Below the books: how many there are and the words known in the language, with the streak where it is shown. */
@Composable
private fun Footer(state: BooksUiState) {
    val parts = buildList {
        add(trPlural(state.books.size, "{1} book", "{1} books", formatCount(state.books.size)))
        if (!state.archived) {
            add(trPlural(state.wordsLearned, "{1} word learned", "{1} words learned", formatCount(state.wordsLearned)))
            if (state.showStreak && state.streak > 0) add(trPlural(state.streak, "{0}-day streak", "{0}-day streak"))
        }
    }
    Text(parts.joinToString("  ·  "), Modifier.padding(top = 20.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BookMenu(book: BookListItem, callbacks: BooksCallbacks) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = tr("Actions"), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text(tr("Read")) }, onClick = { open = false; callbacks.onOpen(book) })
            AppMenuItem(text = { Text(tr("Edit")) }, onClick = { open = false; callbacks.onEdit(book) })
            AppMenuItem(text = { Text(tr("Bookmarks")) }, onClick = { open = false; callbacks.onBookmarks(book) })
            AppMenuItem(text = { Text(if (book.isArchived) tr("Unarchive") else tr("Archive")) }, onClick = { open = false; callbacks.onArchive(book) })
            AppMenuItem(text = { Text(tr("Delete")) }, onClick = { open = false; callbacks.onDelete(book) })
        }
    }
}

@Composable
private fun EmptyState(archived: Boolean, onNewBook: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(AppIcons.Book, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        Text(if (archived) tr("No archived books.") else tr("No books match these filters."), style = MaterialTheme.typography.titleMedium)
        if (!archived) TextButton(onClick = onNewBook) { Text(tr("Create a book")) }
    }
}

private fun words(count: Int) = trPlural(count, "{1} word", "{1} words", count.grouped())

/** 1223 → "1,223". */
private fun Int.grouped(): String = formatCount(this)
