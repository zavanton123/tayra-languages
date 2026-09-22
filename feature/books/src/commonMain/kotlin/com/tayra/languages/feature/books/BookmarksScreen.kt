package com.tayra.languages.feature.books

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.model.PageBookmark
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.EmptyMessage
import com.tayra.languages.core.ui.components.TextInputDialog
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class BookmarksUiState(val title: String = "", val bookmarks: List<PageBookmark> = emptyList())

class BookmarksViewModel(private val bookId: Long, private val books: BookRepository) : ViewModel() {
    val state: StateFlow<BookmarksUiState> = combine(books.observeBook(bookId), books.observeBookmarks(bookId)) { book, bookmarks ->
        BookmarksUiState(book?.title.orEmpty(), bookmarks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookmarksUiState())

    fun rename(id: Long, title: String) = viewModelScope.launch { books.renameBookmark(id, title) }
    fun delete(id: Long) = viewModelScope.launch { books.deleteBookmark(id) }
}

@Composable
fun BookmarksScreen(
    bookId: Long,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: BookmarksViewModel = koinViewModel(key = "bookmarks-$bookId") { parametersOf(bookId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf<PageBookmark?>(null) }

    Scaffold(topBar = { AppTopBar(title = "Bookmarks: ${state.title}", onNavigate = onNavigate, onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.bookmarks.isEmpty()) {
                EmptyMessage("No bookmarks yet. Add one from the reading menu.")
            } else {
                LazyColumn {
                    items(state.bookmarks, key = { it.id }) { bookmark ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onNavigate(Route.Read(bookId, bookmark.pageNumber)) }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(bookmark.title, style = MaterialTheme.typography.titleMedium)
                                Text("Page ${bookmark.pageNumber}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { renaming = bookmark }) { Icon(Icons.Default.Edit, contentDescription = "Rename") }
                            IconButton(onClick = { viewModel.delete(bookmark.id) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    renaming?.let { bookmark ->
        TextInputDialog(
            title = "Rename bookmark",
            label = "Title",
            initial = bookmark.title,
            onConfirm = { viewModel.rename(bookmark.id, it.trim()); renaming = null },
            onDismiss = { renaming = null },
        )
    }
}
