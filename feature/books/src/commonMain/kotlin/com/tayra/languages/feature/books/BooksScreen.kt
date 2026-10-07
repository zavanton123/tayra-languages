package com.tayra.languages.feature.books

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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Card
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
    var confirmWipe by remember { mutableStateOf(false) }
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
                onOpenTutorial = { onNavigate(Route.Read(it, 1)) },
                onWipe = { confirmWipe = true },
                onDismissDemo = viewModel::dismissDemoNotice,
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
    if (confirmWipe) {
        ConfirmDialog(
            title = tr("Clear the database?"),
            text = tr("This removes all languages, books and terms so you can start fresh. This cannot be undone."),
            confirmLabel = tr("Clear everything"),
            destructive = true,
            onConfirm = { viewModel.wipeDatabase(); confirmWipe = false },
            onDismiss = { confirmWipe = false },
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
    val onOpenTutorial: (Long) -> Unit = {},
    val onWipe: () -> Unit = {},
    val onDismissDemo: () -> Unit = {},
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
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 24.dp),
    ) {
        if (state.isDemo && !state.archived) {
            item { DemoNotice(state.tutorialBookId, callbacks) }
        }
        item { PageHeader(state.archived, compact, callbacks.onNewBook) }
        if (!state.archived) item { StatCards(state, stacked = !wide) }
        item { Toolbar(state, compact, wide, books.size, callbacks) }
        item {
            when {
                books.isEmpty() -> EmptyState(state.archived, callbacks.onNewBook)
                !wide || state.view == BooksView.GRID -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Cards have no header, so the bulk actions show up once something is ticked.
                    if (state.selectedBooks.isNotEmpty()) SelectionBar(state, callbacks)
                    BookGrid(books, state.selected, compact, callbacks)
                }
                else -> BookTable(state, callbacks)
            }
        }
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
                if (archived) tr("Books you have set aside. Unarchive one to keep reading it.") else tr("Read, learn, and explore languages through great texts."),
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

private data class StatCard(val value: String, val label: String, val icon: ImageVector, val tint: Color)

private val BLUE = Color(0xFF3B6FE0)
private val PURPLE = Color(0xFF7C4DDB)
private val GREEN = Color(0xFF2E9D57)
private val ORANGE = Color(0xFFEA7A1B)
private val PROGRESS = Color(0xFF4A90E2)
private val KNOWN = Color(0xFF4CC38A)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatCards(state: BooksUiState, stacked: Boolean) {
    val cards = buildList {
        add(StatCard(state.books.size.grouped(), if (state.books.size == 1) tr("Book") else tr("Books"), AppIcons.Book, BLUE))
        add(StatCard(state.wordsLearned.grouped(), tr("Words learned"), AppIcons.BarChart, PURPLE))
        add(StatCard(state.currentlyReading.grouped(), tr("Currently reading"), AppIcons.MenuBook, GREEN))
        if (state.showStreak) add(StatCard("${state.streak}", trPlural(state.streak, "Day streak", "Days streak"), AppIcons.Flame, ORANGE))
    }
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(if (stacked) 10.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = if (stacked && cards.size > 3) 2 else cards.size,
    ) {
        cards.forEach { card ->
            val shape = RoundedCornerShape(14.dp)
            val box = Modifier.weight(1f).clip(shape).background(card.tint.copy(alpha = 0.05f)).border(1.dp, card.tint.copy(alpha = 0.16f), shape)
            if (stacked) {
                // Narrow windows stack the icon, figure and label so three cards fit side by side.
                Column(box.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(card.tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(card.icon, contentDescription = null, tint = card.tint, modifier = Modifier.size(18.dp))
                    }
                    Text(card.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(card.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2, maxLines = 2)
                }
            } else {
                Row(box.padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(card.tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(card.icon, contentDescription = null, tint = card.tint, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(22.dp))
                    Column {
                        Text(card.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(card.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun Toolbar(state: BooksUiState, compact: Boolean, wide: Boolean, count: Int, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val sortMenu: @Composable (Modifier) -> Unit = { FilterMenu(AppIcons.SwapVert, tr(state.sort.label), BookSort.entries, { tr(it.label) }, callbacks.onSort, it) }
    val progressMenu: @Composable (Modifier) -> Unit = { FilterMenu(AppIcons.BarChart, tr(state.progress.label), ProgressFilter.entries, { tr(it.label) }, callbacks.onProgress, it) }
    val hasTags = state.availableTags.isNotEmpty()
    val tagFilter: @Composable (Modifier) -> Unit = { TagFilterButton(state.availableTags, state.tags, state.matchAllTags, callbacks.onTags, it) }
    if (compact) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchBox(state.search, callbacks.onSearch, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                sortMenu(Modifier.weight(1f))
                progressMenu(Modifier.weight(1f))
            }
            if (hasTags) tagFilter(Modifier)
        }
        return
    }
    Layout(
        content = {
            SearchBox(state.search, callbacks.onSearch)
            sortMenu(Modifier)
            progressMenu(Modifier)
            if (hasTags) tagFilter(Modifier) else Spacer(Modifier)
            // Only wide windows have the table to switch to.
            if (wide) ViewToggle(state.view, callbacks.onView) else Spacer(Modifier)
            val ticked = state.selectedBooks.size
            Text(if (ticked > 0) tr("{0} of {1} selected", ticked, count) else trPlural(count, "{0} book", "{0} books"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, softWrap = false)
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
    ) { measurables, constraints ->
        // The search takes what the other controls leave, up to a comfortable width; when too
        // little is left it gets a row of its own above them.
        val gap = 14.dp.roundToPx()
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val search = measurables[0]
        // Sort, progress and tags after the search; the view toggle and the count at the far end.
        val controls = measurables.drop(1).map { it.measure(loose) }
        val controlsWidth = controls.sumOf { it.width } + gap * 4 + 16.dp.roundToPx()
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
            centred(controls[1], x + controls[0].width + gap)
            centred(controls[2], x + controls[0].width + controls[1].width + gap * 2)
            centred(controls[4], width - controls[4].width)
            centred(controls[3], width - controls[4].width - 20.dp.roundToPx() - controls[3].width)
        }
    }
}

private val CONTROL_HEIGHT = 48.dp

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(CONTROL_HEIGHT).clip(RoundedCornerShape(10.dp)).background(colors.surfaceVariant.copy(alpha = 0.45f))
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
                    if (value.isEmpty()) Text(tr("Search books"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private const val BOOK_WEIGHT = 3.2f
private const val PROGRESS_WEIGHT = 2.6f
private const val OPENED_WEIGHT = 1.4f
private const val KNOWN_WEIGHT = 2.2f
private val MENU_WIDTH = 48.dp

@Composable
private fun BookTable(state: BooksUiState, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp)).background(colors.surface),
    ) {
        TableHeader(state, callbacks)
        state.filteredBooks.forEach { book ->
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.7f))
            BookTableRow(book, book.id in state.selected, callbacks)
        }
    }
}

@Composable
private fun TableHeader(state: BooksUiState, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val sort = state.sort
    val onSort = callbacks.onSort
    val allSelected = state.filteredBooks.isNotEmpty() && state.selectedBooks.size == state.filteredBooks.size
    CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant, LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
        Row(
            Modifier.fillMaxWidth().background(colors.surfaceVariant.copy(alpha = 0.35f)).padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = allSelected, onCheckedChange = callbacks.onSelectAll, modifier = Modifier.semantics { contentDescription = tr("Select all books") })
            Spacer(Modifier.width(CHECKBOX_GAP))
            HeaderCell(tr("Book"), BOOK_WEIGHT, active = sort == BookSort.TITLE) { onSort(BookSort.TITLE) }
            HeaderCell(tr("Reading progress"), PROGRESS_WEIGHT)
            HeaderCell(tr("Vocabulary known"), KNOWN_WEIGHT, active = sort == BookSort.MASTERY) { onSort(BookSort.MASTERY) }
            HeaderCell(tr("Last opened"), OPENED_WEIGHT, Icons.Default.KeyboardArrowDown, active = sort == BookSort.RECENT) { onSort(BookSort.RECENT) }
            Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { BulkMenu(state, callbacks) }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(label: String, weight: Float, icon: ImageVector? = null, active: Boolean = false, onClick: (() -> Unit)? = null) {
    Box(Modifier.weight(weight)) { HeaderLabel(label, icon, active, onClick) }
}

@Composable
private fun HeaderLabel(label: String, icon: ImageVector? = null, active: Boolean = false, onClick: (() -> Unit)? = null) {
    val tint = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current
    Row(
        (if (onClick != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick) else Modifier).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = tint, fontWeight = FontWeight.Medium)
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
    }
}

@Composable
private fun BookTableRow(book: BookListItem, selected: Boolean, callbacks: BooksCallbacks) {
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
            .hoverable(interaction).clickable { callbacks.onOpen(book) }.padding(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCheckbox(book, selected, callbacks)
        Spacer(Modifier.width(CHECKBOX_GAP))
        Row(Modifier.weight(BOOK_WEIGHT).padding(end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            LanguageBadge(book.languageName, 48.dp)
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        book.title,
                        Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    StatusTag(book.readingStatus)
                }
                Text(words(book.wordCount), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
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
        ReadingStatus.NOT_STARTED -> return
    }
    Text(
        label,
        Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelLarge,
        color = tint,
        maxLines = 1,
        softWrap = false,
    )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookGrid(books: List<BookListItem>, selected: Set<Long>, compact: Boolean, callbacks: BooksCallbacks) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        books.forEach { book -> BookCard(book, book.id in selected, callbacks, if (compact) Modifier.fillMaxWidth() else Modifier.width(340.dp)) }
    }
}

@Composable
private fun BookCard(book: BookListItem, selected: Boolean, callbacks: BooksCallbacks, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.clip(shape).border(1.dp, if (selected) colors.primary.copy(alpha = 0.6f) else colors.outlineVariant, shape)
            .background(if (selected) colors.primary.copy(alpha = 0.05f) else colors.surface)
            .clickable { callbacks.onOpen(book) }.padding(start = 6.dp, end = 18.dp, top = 18.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BookCheckbox(book, selected, callbacks)
            Spacer(Modifier.width(4.dp))
            LanguageBadge(book.languageName, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(words(book.wordCount), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            BookMenu(book, callbacks)
        }
        Column(Modifier.padding(start = CARD_INDENT), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Reading progress"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            PercentBar(book.progressPercent, PROGRESS)
        }
        Column(Modifier.padding(start = CARD_INDENT), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Vocabulary known"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            PercentBar(book.masteryPercent, KNOWN)
        }
        Row(Modifier.padding(start = CARD_INDENT), verticalAlignment = Alignment.CenterVertically) {
            Text(
                book.lastOpened?.let { tr("Opened {0}", it.relativeTo()) } ?: tr("Not opened yet"),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            StatusTag(book.readingStatus)
        }
    }
}

private val CHECKBOX_GAP = 4.dp

/** Lines the card's bars up with its badge, past the checkbox beside it. */
private val CARD_INDENT = 52.dp

@Composable
private fun BookCheckbox(book: BookListItem, selected: Boolean, callbacks: BooksCallbacks) {
    Checkbox(checked = selected, onCheckedChange = { callbacks.onToggle(book) }, modifier = Modifier.semantics { contentDescription = tr("Select {0}", book.title) })
}

/** The header's menu of actions on the ticked books, as on the vocabulary page. */
@Composable
private fun BulkMenu(state: BooksUiState, callbacks: BooksCallbacks) {
    var open by remember { mutableStateOf(false) }
    val count = state.selectedBooks.size
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = tr("Selected books actions"), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            val archiveLabel = when {
                state.archived && count > 0 -> tr("Unarchive {0} selected", count)
                state.archived -> tr("Unarchive selected")
                count > 0 -> tr("Archive {0} selected", count)
                else -> tr("Archive selected")
            }
            AppMenuItem(text = { Text(archiveLabel) }, enabled = count > 0, onClick = { open = false; callbacks.onArchiveSelected() })
            AppMenuItem(text = { Text(if (count > 0) tr("Delete {0} selected", count) else tr("Delete selected")) }, enabled = count > 0, onClick = { open = false; callbacks.onDeleteSelected() })
        }
    }
}

@Composable
private fun SelectionBar(state: BooksUiState, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val count = state.selectedBooks.size
    val all = count == state.filteredBooks.size
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(alpha = 0.07f))
            .border(1.dp, colors.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = all, onCheckedChange = callbacks.onSelectAll, modifier = Modifier.semantics { contentDescription = tr("Select all books") })
        Text(tr("{0} selected", count), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        TextButton(onClick = callbacks.onArchiveSelected) { Text(if (state.archived) tr("Unarchive") else tr("Archive")) }
        TextButton(onClick = callbacks.onDeleteSelected) { Text(tr("Delete"), color = colors.error) }
        IconButton(onClick = { callbacks.onSelectAll(false) }) { Icon(Icons.Default.Close, contentDescription = tr("Clear selection"), tint = colors.onSurfaceVariant) }
    }
}

private val badgeTints = listOf(
    Color(0xFF1FA463), Color(0xFF7C4DDB), Color(0xFFDC4A4A), Color(0xFFC98A05),
    Color(0xFF3B6FE0), Color(0xFF0E96B0), Color(0xFFE0641B), Color(0xFFD9337E),
)

@Composable
private fun LanguageBadge(languageName: String, size: Dp) {
    val code = LanguageCodes.codeFor(languageName)?.uppercase() ?: languageName.take(2).uppercase()
    val tint = badgeTints[(code.hashCode() and Int.MAX_VALUE) % badgeTints.size]
    Box(Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        Text(code, color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoNotice(tutorialBookId: Long?, callbacks: BooksCallbacks) {
    Card(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("The database has been loaded with a brief tutorial and some languages and short texts for you to try out."))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tutorialBookId != null) Button(onClick = { callbacks.onOpenTutorial(tutorialBookId) }) { Text(tr("Open the tutorial")) }
                TextButton(onClick = callbacks.onWipe) { Text(tr("Clear database")) }
                TextButton(onClick = callbacks.onDismissDemo) { Text(tr("Dismiss")) }
            }
        }
    }
}

private fun words(count: Int) = trPlural(count, "{1} word", "{1} words", count.grouped())

/** 1223 → "1,223". */
private fun Int.grouped(): String = formatCount(this)
