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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.StatusDistributionBar
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
    val compact = LocalWindowWidth.current.isCompact

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (archived) "Archived books" else "Tayra Languages",
                onNavigate = onNavigate,
                onBack = onBack,
                section = NavSection.BOOKS,
                centerContent = if (compact) null else ({ SearchBox(state.search, viewModel::setSearch) }),
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
        val books = state.filteredBooks
        val gutter = if (compact) 16.dp else 32.dp
        val actions = BookActions(
            onOpen = { onNavigate(Route.Read(it.id)) },
            onEdit = { onNavigate(Route.EditBook(it.id)) },
            onArchive = { if (it.isArchived) viewModel.unarchive(it.id) else viewModel.archive(it.id) },
            onDelete = { pendingDelete = it },
            onBookmarks = { onNavigate(Route.Bookmarks(it.id)) },
        )
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = gutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (state.isDemo && !archived) {
                item {
                    DemoNotice(
                        tutorialBookId = state.tutorialBookId,
                        onOpenTutorial = { onNavigate(Route.Read(it, 1)) },
                        onWipe = { confirmWipe = true },
                        onDismiss = viewModel::dismissDemoNotice,
                    )
                }
            }
            item { PageHeader(archived, compact, onNewBook = { onNavigate(Route.NewBook()) }) }
            if (!archived) item { StatCards(state, compact) }
            item { Toolbar(state, compact, books.size, viewModel) }
            when {
                books.isEmpty() -> item { EmptyState(archived, onNewBook = { onNavigate(Route.NewBook()) }) }
                compact || state.view == BooksView.GRID -> item { BookGrid(books, compact, actions) }
                else -> {
                    item { TableHeader(state.sort, onSort = viewModel::setSort) }
                    itemsIndexed(books, key = { _, book -> book.id }) { index, book -> BookTableRow(book, index, actions) }
                }
            }
            item { Footer(state) }
        }
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

private class BookActions(
    val onOpen: (BookListItem) -> Unit,
    val onEdit: (BookListItem) -> Unit,
    val onArchive: (BookListItem) -> Unit,
    val onDelete: (BookListItem) -> Unit,
    val onBookmarks: (BookListItem) -> Unit,
)

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text("Search books", color = colors.onSurfaceVariant, style = LocalTextStyle.current)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun PageHeader(archived: Boolean, compact: Boolean, onNewBook: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                if (archived) "Archived books" else "Books",
                style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (archived) "Books you have set aside. Unarchive one to keep reading it." else "Read, learn, and explore languages through great texts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!archived) {
            Button(onClick = onNewBook, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New book")
            }
        }
    }
}

private data class StatCard(val label: String, val value: String, val icon: ImageVector, val tint: Color)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatCards(state: BooksUiState, compact: Boolean) {
    val cards = buildList {
        add(StatCard("Total books", state.books.size.toString(), AppIcons.Book, Color(0xFF3B6FE0)))
        add(StatCard("Words learned", state.wordsLearned.toString(), AppIcons.BarChart, Color(0xFF7C4DDB)))
        if (state.showStreak) add(StatCard("Reading streak", "${state.streak} day${if (state.streak == 1) "" else "s"}", AppIcons.Flame, Color(0xFFEA7A1B)))
    }
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        maxItemsInEachRow = if (compact) 2 else cards.size,
    ) {
        cards.forEach { card ->
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(card.tint.copy(alpha = 0.06f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(card.tint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(card.icon, contentDescription = null, tint = card.tint, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(card.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(card.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Toolbar(state: BooksUiState, compact: Boolean, count: Int, viewModel: BooksViewModel) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (compact) {
            OutlinedTextField(
                value = state.search,
                onValueChange = viewModel::setSearch,
                placeholder = { Text("Search books") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FilterMenu(icon = AppIcons.SwapVert, label = state.sort.label, options = BookSort.entries, optionLabel = { it.label }, onSelect = viewModel::setSort)
            FilterMenu(icon = AppIcons.BarChart, label = state.mastery.label, options = MasteryFilter.entries, optionLabel = { it.label }, onSelect = viewModel::setMastery)
            if (!compact) {
                Spacer(Modifier.weight(1f))
                ViewToggle(state.view, onSelect = viewModel::setView)
                Text("$count book${if (count == 1) "" else "s"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun <T> FilterMenu(icon: ImageVector, label: String, options: List<T>, optionLabel: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { open = true },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Row(Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(3.dp)) {
        listOf(BooksView.LIST to AppIcons.ViewList, BooksView.GRID to AppIcons.GridView).forEach { (view, icon) ->
            val active = view == selected
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (active) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelect(view) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = view.name.lowercase(), tint = if (active) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// Column weights shared by the table header and rows.
private const val TITLE_WEIGHT = 3f
private const val POSITION_WEIGHT = 2.4f
private const val OPENED_WEIGHT = 1.5f
private const val MASTERY_WEIGHT = 1.8f
private val MENU_WIDTH = 48.dp

@Composable
private fun TableHeader(sort: BookSort, onSort: (BookSort) -> Unit) {
    val colors = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant, LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)).background(colors.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderCell("Title", TITLE_WEIGHT, AppIcons.UnfoldMore, active = sort == BookSort.TITLE) { onSort(BookSort.TITLE) }
            HeaderCell("Reading position", POSITION_WEIGHT)
            HeaderCell("Last opened", OPENED_WEIGHT, Icons.Default.KeyboardArrowDown, active = sort == BookSort.RECENT) { onSort(BookSort.RECENT) }
            HeaderCell("Vocabulary mastery", MASTERY_WEIGHT, Icons.Default.Info, active = sort == BookSort.MASTERY) { onSort(BookSort.MASTERY) }
            Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { Text("···") }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(label: String, weight: Float, icon: ImageVector? = null, active: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.weight(weight).then(if (onClick != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current, fontWeight = FontWeight.Medium)
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current)
    }
}

@Composable
private fun BookTableRow(book: BookListItem, index: Int, actions: BookActions) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background = when {
        hovered -> colors.primary.copy(alpha = 0.08f)
        index % 2 == 1 -> colors.surfaceVariant.copy(alpha = 0.25f)
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().background(background).hoverable(interaction).clickable { actions.onOpen(book) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(TITLE_WEIGHT).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            LanguageBadge(book.languageName)
            Spacer(Modifier.width(14.dp))
            Text(
                book.title,
                Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (book.isCompleted) Text(" ✓", color = colors.tertiary)
        }
        Column(Modifier.weight(POSITION_WEIGHT).padding(end = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("page ${book.currentPage}/${book.pageCount}  ·  ${book.wordCount} words", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusDistributionBar(book.stats, Modifier.weight(1f))
                Text(book.stats?.let { "${it.unknownPercent}% new" } ?: "…", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        Text(book.lastOpened?.relativeTo() ?: "Not yet", Modifier.weight(OPENED_WEIGHT), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Box(Modifier.weight(MASTERY_WEIGHT).padding(end = 16.dp)) { MasteryBar(book.masteryPercent) }
        Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { BookMenu(book, actions) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookGrid(books: List<BookListItem>, compact: Boolean, actions: BookActions) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        books.forEach { book -> BookCard(book, actions, if (compact) Modifier.fillMaxWidth() else Modifier.width(320.dp)) }
    }
}

@Composable
private fun BookCard(book: BookListItem, actions: BookActions, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp))
            .clickable { actions.onOpen(book) }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LanguageBadge(book.languageName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BookMenu(book, actions)
        }
        Text(
            "page ${book.currentPage}/${book.pageCount}  ·  ${book.wordCount} words  ·  ${book.lastOpened?.relativeTo() ?: "not opened yet"}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusDistributionBar(book.stats, Modifier.weight(1f))
            Text(book.stats?.let { "${it.unknownPercent}% new" } ?: "…", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        MasteryBar(book.masteryPercent)
    }
}

@Composable
private fun MasteryBar(percent: Int?) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.surfaceVariant)) {
            if (percent != null && percent > 0) {
                Box(Modifier.fillMaxWidth(percent / 100f).height(8.dp).background(Color(0xFF4CC38A)))
            }
        }
        Text(percent?.let { "$it%" } ?: "…", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.width(40.dp))
    }
}

private val badgeTints = listOf(
    Color(0xFF1FA463), Color(0xFF7C4DDB), Color(0xFFDC4A4A), Color(0xFFC98A05),
    Color(0xFF3B6FE0), Color(0xFF0E96B0), Color(0xFFE0641B), Color(0xFFD9337E),
)

@Composable
private fun LanguageBadge(languageName: String) {
    val code = LanguageCodes.codeFor(languageName)?.uppercase() ?: languageName.take(2).uppercase()
    val tint = badgeTints[(code.hashCode() and Int.MAX_VALUE) % badgeTints.size]
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Text(code, color = tint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BookMenu(book: BookListItem, actions: BookActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text("Read") }, onClick = { open = false; actions.onOpen(book) })
            AppMenuItem(text = { Text("Edit") }, onClick = { open = false; actions.onEdit(book) })
            AppMenuItem(text = { Text("Bookmarks") }, onClick = { open = false; actions.onBookmarks(book) })
            AppMenuItem(text = { Text(if (book.isArchived) "Unarchive" else "Archive") }, onClick = { open = false; actions.onArchive(book) })
            AppMenuItem(text = { Text("Delete") }, onClick = { open = false; actions.onDelete(book) })
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

@Composable
private fun Footer(state: BooksUiState) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        val parts = buildList {
            add("${state.books.size} book${if (state.books.size == 1) "" else "s"}")
            if (!state.archived) add("${state.wordsLearned} words learned")
        }
        Text(parts.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoNotice(tutorialBookId: Long?, onOpenTutorial: (Long) -> Unit, onWipe: () -> Unit, onDismiss: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("The database has been loaded with a brief tutorial and some languages and short texts for you to try out.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tutorialBookId != null) Button(onClick = { onOpenTutorial(tutorialBookId) }) { Text("Open the tutorial") }
                TextButton(onClick = onWipe) { Text("Clear database") }
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}
