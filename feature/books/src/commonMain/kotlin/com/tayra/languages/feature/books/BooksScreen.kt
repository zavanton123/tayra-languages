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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
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
import com.tayra.languages.core.ui.components.relativeTo
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

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (archived) "Archived books" else "Tayra Languages",
                onNavigate = onNavigate,
                onBack = onBack,
                section = NavSection.BOOKS,
                actions = {
                    if (!archived) {
                        IconButton(onClick = viewModel::refreshAllStats) { Icon(Icons.Default.Refresh, contentDescription = "Refresh stats") }
                    }
                },
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
                onProgress = viewModel::setProgress,
                onView = viewModel::setView,
            ),
            modifier = Modifier.padding(padding),
        )
    }

    pendingDelete?.let { book ->
        ConfirmDialog(
            title = "Delete \"${book.title}\"?",
            text = "The book and its pages will be deleted. Terms are kept.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { viewModel.delete(book.id); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
    if (confirmWipe) {
        ConfirmDialog(
            title = "Clear the database?",
            text = "This removes all languages, books and terms so you can start fresh. This cannot be undone.",
            confirmLabel = "Clear everything",
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
    val onProgress: (ProgressFilter) -> Unit = {},
    val onView: (BooksView) -> Unit = {},
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
                !wide || state.view == BooksView.GRID -> BookGrid(books, compact, callbacks)
                else -> BookTable(books, state.sort, callbacks)
            }
        }
    }
}

@Composable
private fun PageHeader(archived: Boolean, compact: Boolean, onNewBook: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (archived) "Archived books" else "Books",
                style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (archived) "Books you have set aside. Unarchive one to keep reading it." else "Read, learn, and explore languages through great texts.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!archived) {
            Button(onClick = onNewBook, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("New book", fontWeight = FontWeight.SemiBold)
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
        add(StatCard(state.books.size.grouped(), if (state.books.size == 1) "Book" else "Books", AppIcons.Book, BLUE))
        add(StatCard(state.wordsLearned.grouped(), "Words learned", AppIcons.BarChart, PURPLE))
        add(StatCard(state.currentlyReading.grouped(), "Currently reading", AppIcons.MenuBook, GREEN))
        if (state.showStreak) add(StatCard("${state.streak}", if (state.streak == 1) "Day streak" else "Days streak", AppIcons.Flame, ORANGE))
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
    val sortMenu: @Composable (Modifier) -> Unit = { FilterMenu(AppIcons.SwapVert, state.sort.label, BookSort.entries, { it.label }, callbacks.onSort, it) }
    val progressMenu: @Composable (Modifier) -> Unit = { FilterMenu(AppIcons.BarChart, state.progress.label, ProgressFilter.entries, { it.label }, callbacks.onProgress, it) }
    if (compact) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchBox(state.search, callbacks.onSearch, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                sortMenu(Modifier.weight(1f))
                progressMenu(Modifier.weight(1f))
            }
        }
        return
    }
    Layout(
        content = {
            SearchBox(state.search, callbacks.onSearch)
            sortMenu(Modifier)
            progressMenu(Modifier)
            // Only wide windows have the table to switch to.
            if (wide) ViewToggle(state.view, callbacks.onView) else Spacer(Modifier)
            Text("$count book${if (count == 1) "" else "s"}", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, softWrap = false)
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
    ) { measurables, constraints ->
        // The search takes what the other controls leave, up to a comfortable width; when too
        // little is left it gets a row of its own above them.
        val gap = 14.dp.roundToPx()
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val (search, sort, progress, toggle, label) = measurables
        val controls = listOf(sort, progress, toggle, label).map { it.measure(loose) }
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
            centred(controls[1], x + controls[0].width + gap)
            centred(controls[3], width - controls[3].width)
            centred(controls[2], width - controls[3].width - 20.dp.roundToPx() - controls[2].width)
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
                    if (value.isEmpty()) Text("Search books", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                Icon(icon, contentDescription = "${view.name.lowercase()} view", tint = if (active) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
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
private fun BookTable(books: List<BookListItem>, sort: BookSort, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp)).background(colors.surface),
    ) {
        TableHeader(sort, callbacks.onSort)
        books.forEach { book ->
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.7f))
            BookTableRow(book, callbacks)
        }
    }
}

@Composable
private fun TableHeader(sort: BookSort, onSort: (BookSort) -> Unit) {
    val colors = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant, LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
        Row(
            Modifier.fillMaxWidth().background(colors.surfaceVariant.copy(alpha = 0.35f)).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderCell("Book", BOOK_WEIGHT, active = sort == BookSort.TITLE) { onSort(BookSort.TITLE) }
            HeaderCell("Reading progress", PROGRESS_WEIGHT)
            HeaderCell("Last opened", OPENED_WEIGHT, Icons.Default.KeyboardArrowDown, active = sort == BookSort.RECENT) { onSort(BookSort.RECENT) }
            Row(Modifier.weight(KNOWN_WEIGHT), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HeaderLabel("Vocabulary known", active = sort == BookSort.MASTERY) { onSort(BookSort.MASTERY) }
                InfoTooltip("The share of the book's distinct words that are no longer new: being learned, known or ignored.")
            }
            Spacer(Modifier.width(MENU_WIDTH))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoTooltip(text: String) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(isPersistent = true),
    ) {
        Icon(Icons.Default.Info, contentDescription = text, tint = LocalContentColor.current, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BookTableRow(book: BookListItem, callbacks: BooksCallbacks) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth().background(if (hovered) colors.primary.copy(alpha = 0.04f) else Color.Transparent)
            .hoverable(interaction).clickable { callbacks.onOpen(book) }.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Column(Modifier.weight(PROGRESS_WEIGHT).padding(end = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Page ${book.currentPage} of ${book.pageCount}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            PercentBar(book.progressPercent, PROGRESS)
        }
        Text(book.lastOpened?.relativeTo() ?: "Not opened yet", Modifier.weight(OPENED_WEIGHT).padding(end = 16.dp), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        Box(Modifier.weight(KNOWN_WEIGHT).padding(end = 24.dp)) { PercentBar(book.masteryPercent, KNOWN) }
        Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { BookMenu(book, callbacks) }
    }
}

@Composable
private fun StatusTag(status: ReadingStatus) {
    val (label, tint) = when (status) {
        ReadingStatus.READING -> "Reading" to BLUE
        ReadingStatus.FINISHED -> "Finished" to GREEN
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
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.surfaceVariant)) {
            if (percent != null && percent > 0) Box(Modifier.fillMaxWidth(percent / 100f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
        }
        Text(percent?.let { "$it%" } ?: "…", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.width(40.dp), softWrap = false)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookGrid(books: List<BookListItem>, compact: Boolean, callbacks: BooksCallbacks) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        books.forEach { book -> BookCard(book, callbacks, if (compact) Modifier.fillMaxWidth() else Modifier.width(340.dp)) }
    }
}

@Composable
private fun BookCard(book: BookListItem, callbacks: BooksCallbacks, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp)).background(colors.surface)
            .clickable { callbacks.onOpen(book) }.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LanguageBadge(book.languageName, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(words(book.wordCount), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            BookMenu(book, callbacks)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Page ${book.currentPage} of ${book.pageCount}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            PercentBar(book.progressPercent, PROGRESS)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Vocabulary known", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            PercentBar(book.masteryPercent, KNOWN)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                book.lastOpened?.let { "Opened ${it.relativeTo()}" } ?: "Not opened yet",
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            StatusTag(book.readingStatus)
        }
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
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text("Read") }, onClick = { open = false; callbacks.onOpen(book) })
            AppMenuItem(text = { Text("Edit") }, onClick = { open = false; callbacks.onEdit(book) })
            AppMenuItem(text = { Text("Bookmarks") }, onClick = { open = false; callbacks.onBookmarks(book) })
            AppMenuItem(text = { Text(if (book.isArchived) "Unarchive" else "Archive") }, onClick = { open = false; callbacks.onArchive(book) })
            AppMenuItem(text = { Text("Delete") }, onClick = { open = false; callbacks.onDelete(book) })
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
        Text(if (archived) "No archived books." else "No books match these filters.", style = MaterialTheme.typography.titleMedium)
        if (!archived) TextButton(onClick = onNewBook) { Text("Create a book") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoNotice(tutorialBookId: Long?, callbacks: BooksCallbacks) {
    Card(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("The database has been loaded with a brief tutorial and some languages and short texts for you to try out.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tutorialBookId != null) Button(onClick = { callbacks.onOpenTutorial(tutorialBookId) }) { Text("Open the tutorial") }
                TextButton(onClick = callbacks.onWipe) { Text("Clear database") }
                TextButton(onClick = callbacks.onDismissDemo) { Text("Dismiss") }
            }
        }
    }
}

private fun words(count: Int) = if (count == 1) "1 word" else "${count.grouped()} words"

/** 1223 → "1,223". */
private fun Int.grouped(): String = toString().reversed().chunked(3).joinToString(",").reversed()
