package com.tayra.languages.feature.books

import com.tayra.languages.core.ui.components.ScreenTitle
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
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.i18n.tr
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

data class PageEditUiState(
    val loading: Boolean = true,
    val text: String = "",
    val rtl: Boolean = false,
    val error: String? = null,
    val saving: Boolean = false,
)

/** Edits the text of page [page] of the book. */
class PageEditViewModel(
    private val bookId: Long,
    private val page: Int,
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
            val book = books.getBook(bookId)
            val rtl = book?.let { languages.getById(it.languageId)?.rightToLeft } ?: false
            val text = books.getPage(bookId, page)?.text.orEmpty()
            _state.update { it.copy(loading = false, text = text, rtl = rtl) }
        }
    }

    fun setText(text: String) = _state.update { it.copy(text = text, error = null) }

    fun save() {
        val text = _state.value.text
        if (text.isBlank()) {
            _state.update { it.copy(error = tr("Text is required")) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                bookService.updatePageText(bookId, page, text)
                events.send(page)
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = e.message?.let { m -> tr(m) } ?: tr("Could not save page")) }
            }
        }
    }
}

@Composable
fun PageEditScreen(
    bookId: Long,
    page: Int,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onSaved: (page: Int) -> Unit,
    viewModel: PageEditViewModel = koinViewModel(key = "page-edit-$bookId-$page") { parametersOf(bookId, page) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { onSaved(it) }
    val title = tr("Edit page {0}", page)
    Scaffold(topBar = { AppTopBar(title = title, onNavigate = onNavigate, section = NavSection.BOOKS, onBack = onBack, showMenu = false) }) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenTitle(title)
            ErrorMessage(state.error)
            OutlinedTextField(
                value = state.text,
                onValueChange = viewModel::setText,
                label = { Text(tr("Text")) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = if (state.rtl) TextDirection.Rtl else TextDirection.Ltr),
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::save, enabled = !state.saving) { Text(tr("Save")) }
                OutlinedButton(onClick = onBack) { Text(tr("Cancel")) }
            }
        }
    }
}
