package com.tayra.languages.feature.terms.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermDraft
import com.tayra.languages.core.domain.model.TermMatch
import com.tayra.languages.core.domain.model.TermReferences
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermValidationException
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the term form edits. */
sealed interface TermFormKey {
    data class ById(val termId: Long) : TermFormKey
    data class ByText(val languageId: Long, val text: String) : TermFormKey
    data object New : TermFormKey
}

data class TermFormUiState(
    val loading: Boolean = true,
    val draft: TermDraft = TermDraft(languageId = 0, text = ""),
    val languages: List<Language> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val parentSuggestions: List<TermMatch> = emptyList(),
    val parentQuery: String = "",
    val references: TermReferences? = null,
    val loadingReferences: Boolean = false,
    val error: String? = null,
    val duplicateOf: Term? = null,
    val saving: Boolean = false,
    val dirty: Boolean = false,
) {
    val language: Language? get() = languages.firstOrNull { it.id == draft.languageId }
    val isNew: Boolean get() = draft.isNew
    val showLanguageSelector: Boolean get() = draft.languageId == 0L
}

sealed interface TermFormEvent {
    data class Saved(val termId: Long) : TermFormEvent
    data object Deleted : TermFormEvent
    data class OpenParent(val languageId: Long, val text: String) : TermFormEvent
}

class TermFormViewModel(
    private val key: TermFormKey,
    private val termService: TermService,
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TermFormUiState())
    val state: StateFlow<TermFormUiState> = _state.asStateFlow()
    val events = UiEvents<TermFormEvent>()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val languageList = languages.getAll()
        val tags = terms.allTags().map { it.text }
        val draft = try {
            when (key) {
                is TermFormKey.ById -> termService.load(key.termId).let { if (it.status == TermStatus.UNKNOWN) it.copy(status = TermStatus.NEW_1) else it }
                is TermFormKey.ByText -> termService.findOrNew(key.languageId, key.text).let { if (it.status == TermStatus.UNKNOWN) it.copy(status = TermStatus.NEW_1) else it }
                TermFormKey.New -> {
                    val current = settings.current.currentLanguageId
                    val languageId = if (languageList.any { it.id == current }) current else languageList.singleOrNull()?.id ?: 0L
                    TermDraft(languageId = languageId, text = "", originalText = "")
                }
            }
        } catch (e: NoSuchElementException) {
            _state.update { it.copy(loading = false, error = e.message) }
            return
        }
        // Opening the form acknowledges any flash message.
        draft.id?.let { terms.clearFlashMessage(it) }
        _state.update { it.copy(loading = false, draft = draft, languages = languageList, tagSuggestions = tags) }
    }

    fun update(transform: (TermDraft) -> TermDraft) {
        _state.update { it.copy(draft = transform(it.draft), error = null, duplicateOf = null, dirty = true) }
    }

    fun setStatus(status: TermStatus) = update { it.copy(status = status, statusExplicitlySet = true) }

    fun setParents(parents: List<String>) {
        val previous = _state.value.draft.parents
        update { it.copy(parents = parents, syncStatus = if (parents.size == 1) (it.syncStatus || previous.size != 1) else false) }
        val added = parents.filter { it !in previous }
        if (parents.size == 1 && added.size == 1) inheritParentStatus(added.single())
    }

    private fun inheritParentStatus(parentText: String) {
        val languageId = _state.value.draft.languageId
        if (languageId == 0L) return
        viewModelScope.launch {
            val parent = termService.find(languageId, parentText) ?: return@launch
            if (parent.status != TermStatus.UNKNOWN) {
                _state.update { it.copy(draft = it.draft.copy(status = parent.status, statusExplicitlySet = false)) }
            }
        }
    }

    fun setParentQuery(query: String) {
        _state.update { it.copy(parentQuery = query) }
        searchJob?.cancel()
        val languageId = _state.value.draft.languageId
        if (query.isBlank() || languageId == 0L) {
            _state.update { it.copy(parentSuggestions = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(150)
            val matches = termService.search(languageId, query).filter { it.id != _state.value.draft.id }
            _state.update { it.copy(parentSuggestions = matches) }
        }
    }

    fun openParent(text: String) {
        val languageId = _state.value.draft.languageId
        if (languageId == 0L) return
        viewModelScope.launch {
            if (_state.value.dirty) {
                val saved = doSave() ?: return@launch
                events.send(TermFormEvent.Saved(saved))
            }
            events.send(TermFormEvent.OpenParent(languageId, text))
        }
    }

    fun loadReferences() {
        val draft = _state.value.draft
        if (draft.languageId == 0L || draft.text.isBlank()) return
        _state.update { it.copy(loadingReferences = true) }
        viewModelScope.launch {
            val refs = termService.references(draft.languageId, draft.text)
            _state.update { it.copy(references = refs, loadingReferences = false) }
        }
    }

    fun save() {
        viewModelScope.launch {
            val id = doSave() ?: return@launch
            events.send(TermFormEvent.Saved(id))
        }
    }

    private suspend fun doSave(): Long? {
        val draft = _state.value.draft
        if (draft.languageId == 0L) {
            _state.update { it.copy(error = "Please select a language") }
            return null
        }
        _state.update { it.copy(saving = true, error = null) }
        return try {
            val id = termService.save(draft)
            _state.update { it.copy(saving = false, dirty = false, draft = it.draft.copy(id = id, originalText = it.draft.text)) }
            id
        } catch (e: TermValidationException) {
            _state.update { it.copy(saving = false, error = e.message, duplicateOf = e.duplicateOf) }
            null
        } catch (e: Exception) {
            _state.update { it.copy(saving = false, error = e.message ?: "Could not save term") }
            null
        }
    }

    fun delete() {
        val id = _state.value.draft.id ?: return
        viewModelScope.launch {
            termService.delete(id)
            events.send(TermFormEvent.Deleted)
        }
    }
}
