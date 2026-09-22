package com.tayra.languages.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.data.files.FileImportException
import com.tayra.languages.core.data.files.FileTextExtractor
import com.tayra.languages.core.data.network.WebImportException
import com.tayra.languages.core.data.network.WebPageImporter
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.PageSplitMode
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.BookValidationException
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookFormUiState(
    val loading: Boolean = true,
    val draft: BookDraft = BookDraft(languageId = 0, title = ""),
    val languages: List<Language> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val importUrl: String = "",
    val importedFileName: String? = null,
    val error: String? = null,
    val notice: String? = null,
    val busy: Boolean = false,
) {
    val isNew: Boolean get() = draft.id == null
    val language: Language? get() = languages.firstOrNull { it.id == draft.languageId }
}

sealed interface BookFormEvent {
    data class Saved(val bookId: Long, val isNew: Boolean) : BookFormEvent
}

class BookFormViewModel(
    private val bookId: Long?,
    initialImportUrl: String?,
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val bookService: BookService,
    private val webPageImporter: WebPageImporter,
) : ViewModel() {

    private val _state = MutableStateFlow(BookFormUiState(importUrl = initialImportUrl.orEmpty()))
    val state: StateFlow<BookFormUiState> = _state.asStateFlow()
    val events = UiEvents<BookFormEvent>()

    init {
        viewModelScope.launch {
            val languageList = languages.getAll()
            val tags = books.allBookTags()
            val draft = if (bookId != null) {
                val book = books.getBook(bookId)
                if (book == null) {
                    BookDraft(languageId = 0, title = "")
                } else {
                    BookDraft(
                        id = book.id,
                        languageId = book.languageId,
                        title = book.title,
                        sourceUri = book.sourceUri.orEmpty(),
                        tags = book.tags,
                        audioFilename = book.audioFilename,
                    )
                }
            } else {
                val current = settings.current.currentLanguageId
                val languageId = if (languageList.any { it.id == current }) current else languageList.singleOrNull()?.id ?: 0
                BookDraft(languageId = languageId, title = "")
            }
            _state.update { it.copy(loading = false, draft = draft, languages = languageList, tagSuggestions = tags) }
            if (!initialImportUrl.isNullOrBlank()) importWebPage()
        }
    }

    fun update(transform: (BookDraft) -> BookDraft) {
        _state.update { it.copy(draft = transform(it.draft), error = null) }
    }

    fun setImportUrl(url: String) = _state.update { it.copy(importUrl = url) }

    fun importWebPage() {
        val url = _state.value.importUrl.trim()
        if (url.isEmpty()) return
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                val page = webPageImporter.import(url)
                _state.update {
                    it.copy(
                        busy = false,
                        draft = it.draft.copy(
                            title = it.draft.title.ifBlank { page.title },
                            text = page.text,
                            sourceUri = page.sourceUrl,
                        ),
                        notice = "Imported ${page.text.length} characters from the page.",
                    )
                }
            } catch (e: WebImportException) {
                _state.update { it.copy(busy = false, error = e.message) }
            }
        }
    }

    fun importFile(fileName: String, bytes: ByteArray) {
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                val text = FileTextExtractor.extract(fileName, bytes)
                _state.update {
                    it.copy(
                        busy = false,
                        importedFileName = fileName,
                        draft = it.draft.copy(text = text, title = it.draft.title.ifBlank { fileName.substringBeforeLast('.') }),
                        notice = "Loaded ${text.length} characters from $fileName.",
                    )
                }
            } catch (e: FileImportException) {
                _state.update { it.copy(busy = false, error = e.message) }
            }
        }
    }

    fun save() {
        val draft = _state.value.draft
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val id = if (draft.id == null) bookService.create(draft) else { bookService.update(draft); draft.id!! }
                events.send(BookFormEvent.Saved(id, draft.id == null))
            } catch (e: BookValidationException) {
                _state.update { it.copy(busy = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.message ?: "Could not save book") }
            }
        }
    }

    companion object {
        val splitModes = PageSplitMode.entries
    }
}
