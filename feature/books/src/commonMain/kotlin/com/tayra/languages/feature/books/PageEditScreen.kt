package com.tayra.languages.feature.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.PagePosition
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Editing an existing page, or adding a new page before/after the given one. */
sealed interface PageEditMode {
    val bookId: Long

    data class Edit(override val bookId: Long, val page: Int) : PageEditMode
    data class New(override val bookId: Long, val page: Int, val position: PagePosition) : PageEditMode
}

data class PageEditUiState(
    val loading: Boolean = true,
    val text: String = "",
    val rtl: Boolean = false,
    val error: String? = null,
    val saving: Boolean = false,
)

class PageEditViewModel(
    private val mode: PageEditMode,
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val bookService: BookService,
) : ViewModel() {
    private val _state = MutableStateFlow(PageEditUiState())
    val state: StateFlow<PageEditUiState> = _state.asStateFlow()

    /** Emits the page number to show after saving. */
    val events = UiEvents<Int>()

    init {
        viewModelScope.launch {
            val book = books.getBook(mode.bookId)
            val rtl = book?.let { languages.getById(it.languageId)?.rightToLeft } ?: false
            val text = (mode as? PageEditMode.Edit)?.let { books.getPage(it.bookId, it.page)?.text }.orEmpty()
            _state.update { it.copy(loading = false, text = text, rtl = rtl) }
        }
    }

    fun setText(text: String) = _state.update { it.copy(text = text, error = null) }

    fun save() {
        val text = _state.value.text
        if (text.isBlank()) {
            _state.update { it.copy(error = "Text is required") }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val page = when (mode) {
                    is PageEditMode.Edit -> {
                        bookService.updatePageText(mode.bookId, mode.page, text)
                        mode.page
                    }
                    is PageEditMode.New -> bookService.addPage(mode.bookId, mode.position, mode.page, text)
                }
                events.send(page)
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = e.message ?: "Could not save page") }
            }
        }
    }
}

@Composable
fun PageEditScreen(
    mode: PageEditMode,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onSaved: (page: Int) -> Unit,
    viewModel: PageEditViewModel = koinViewModel(key = "page-edit-$mode") { parametersOf(mode) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { onSaved(it) }
    val title = when (mode) {
        is PageEditMode.Edit -> "Edit page ${mode.page}"
        is PageEditMode.New -> "New page"
    }
    Scaffold(topBar = { AppTopBar(title = title, onNavigate = onNavigate, onBack = onBack, showMenu = false) }) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ErrorMessage(state.error)
            OutlinedTextField(
                value = state.text,
                onValueChange = viewModel::setText,
                label = { Text("Text") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = if (state.rtl) TextDirection.Rtl else TextDirection.Ltr),
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::save, enabled = !state.saving) { Text("Save") }
                OutlinedButton(onClick = onBack) { Text("Cancel") }
            }
        }
    }
}
