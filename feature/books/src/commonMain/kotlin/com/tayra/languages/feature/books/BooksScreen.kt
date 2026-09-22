package com.tayra.languages.feature.books

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
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
                actions = {
                    if (!archived && state.hasLanguages) {
                        IconButton(onClick = viewModel::refreshAllStats) { Icon(Icons.Default.Refresh, contentDescription = "Refresh stats") }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading) {
                LoadingIndicator()
                return@Column
            }
            if (state.isDemo && !archived) {
                DemoNotice(
                    tutorialBookId = state.tutorialBookId,
                    onOpenTutorial = { onNavigate(Route.Read(it, 1)) },
                    onWipe = { confirmWipe = true },
                    onDismiss = viewModel::dismissDemoNotice,
                )
            }
            if (!state.hasLanguages) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("To get started, first load a predefined language and sample text, or create your own language.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onNavigate(Route.PredefinedLanguages) }) { Text("Load predefined language") }
                        TextButton(onClick = { onNavigate(Route.NewLanguage()) }) { Text("Create language") }
                    }
                }
                return@Column
            }

            FilterRow(
                state = state,
                compact = compact,
                onSearch = viewModel::setSearch,
                onLanguage = viewModel::setLanguageFilter,
                onNewBook = { onNavigate(Route.NewBook()) },
                archived = archived,
            )

            val books = state.filteredBooks
            if (books.isEmpty()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (archived) "No archived books." else "No books available.")
                    if (!archived) {
                        TextButton(onClick = { onNavigate(Route.NewBook()) }) { Text("Create one?") }
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(books, key = { it.id }) { book ->
                        BookRow(
                            book = book,
                            compact = compact,
                            onOpen = { onNavigate(Route.Read(book.id)) },
                            onEdit = { onNavigate(Route.EditBook(book.id)) },
                            onArchive = { if (book.isArchived) viewModel.unarchive(book.id) else viewModel.archive(book.id) },
                            onDelete = { pendingDelete = book },
                            onBookmarks = { onNavigate(Route.Bookmarks(book.id)) },
                        )
                        HorizontalDivider()
                    }
                }
            }
            if (!archived && state.showStreak && state.streak > 0) {
                Text(
                    "Reading streak: ${state.streak} day${if (state.streak == 1) "" else "s"}",
                    Modifier.fillMaxWidth().padding(12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoNotice(tutorialBookId: Long?, onOpenTutorial: (Long) -> Unit, onWipe: () -> Unit, onDismiss: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
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

@Composable
private fun FilterRow(
    state: BooksUiState,
    compact: Boolean,
    archived: Boolean,
    onSearch: (String) -> Unit,
    onLanguage: (Long) -> Unit,
    onNewBook: () -> Unit,
) {
    val languageOptions = listOf(0L to "(all languages)") + state.languages.map { it.id to it.name }
    val content: @Composable (Modifier) -> Unit = { fieldModifier ->
        if (state.languages.size > 1) {
            Dropdown(
                options = languageOptions,
                selected = languageOptions.firstOrNull { it.first == state.currentLanguageId } ?: languageOptions.first(),
                onSelect = { onLanguage(it.first) },
                label = "Language",
                optionLabel = { it.second },
                modifier = fieldModifier,
            )
        }
        OutlinedTextField(
            value = state.search,
            onValueChange = onSearch,
            label = { Text("Search") },
            singleLine = true,
            modifier = fieldModifier,
        )
        if (!archived) Button(onClick = onNewBook) { Text("New book") }
    }
    if (compact) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content(Modifier.width(260.dp))
        }
    }
}

@Composable
private fun BookRow(
    book: BookListItem,
    compact: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onBookmarks: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                )
                if (book.isCompleted) Text("✓", color = MaterialTheme.colorScheme.tertiary)
            }
            val details = buildList {
                add(book.languageName)
                if (book.tags.isNotEmpty()) add(book.tags.joinToString(", "))
                add("${book.wordCount} words")
                add("page ${book.currentPage}/${book.pageCount}")
                book.lastOpened?.let { add("read ${it.relativeTo()}") }
            }
            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDistributionBar(book.stats, Modifier.width(if (compact) 140.dp else 220.dp))
                book.stats?.let { stats ->
                    Text(
                        "${stats.distinctUnknowns} unknown of ${stats.distinctTerms} (${stats.unknownPercent}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Actions") }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Read") }, onClick = { menuOpen = false; onOpen() })
            DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
            DropdownMenuItem(text = { Text("Bookmarks") }, onClick = { menuOpen = false; onBookmarks() })
            DropdownMenuItem(text = { Text(if (book.isArchived) "Unarchive" else "Archive") }, onClick = { menuOpen = false; onArchive() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
        }
    }
    Spacer(Modifier.height(0.dp))
}
