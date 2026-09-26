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
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.TermValidationException
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
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
    val parentSuggestions: List<TermMatch> = emptyList(),
    val parentQuery: String = "",
    val references: TermReferences? = null,
    val loadingReferences: Boolean = false,
    val error: String? = null,
    val duplicateOf: Term? = null,
    val saving: Boolean = false,
    val dirty: Boolean = false,
    val lookingUpTranslation: Boolean = false,
    val translationSuggested: Boolean = false,
    /** True once an autosave has persisted the latest edits. */
    val saved: Boolean = false,
    val examples: List<ExampleSentence> = emptyList(),
    val loadingExamples: Boolean = false,
) {
    val language: Language? get() = languages.firstOrNull { it.id == draft.languageId }
    val isNew: Boolean get() = draft.isNew
    val showLanguageSelector: Boolean get() = draft.languageId == 0L
}

sealed interface TermFormEvent {
    /** [keepOpen] is true when the save was implicit (a status click) and the form should stay open. */
    data class Saved(val termId: Long, val keepOpen: Boolean = false) : TermFormEvent
    data object Deleted : TermFormEvent
    data class OpenParent(val languageId: Long, val text: String) : TermFormEvent
}

private const val AUTOSAVE_DELAY_MS = 700L

class TermFormViewModel(
    private val key: TermFormKey,
    private val termService: TermService,
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val translationProvider: TermTranslationProvider,
    private val examplesProvider: ExampleSentencesProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(TermFormUiState())
    val state: StateFlow<TermFormUiState> = _state.asStateFlow()
    val events = UiEvents<TermFormEvent>()
    private var searchJob: Job? = null
    private var autosaveJob: Job? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val languageList = languages.getAll()
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
        _state.update { it.copy(loading = false, draft = draft, languages = languageList) }
        val language = languageList.firstOrNull { it.id == draft.languageId }
        if (draft.translation.isBlank()) suggestTranslation(draft.text, language)
        loadExamples(draft.text, language)
    }

    private fun loadExamples(text: String, language: Language?) {
        if (language == null || text.isBlank()) return
        _state.update { it.copy(loadingExamples = true) }
        viewModelScope.launch {
            val examples = examplesProvider.examples(text, language, settings.current.nativeLanguage)
            _state.update { it.copy(examples = examples, loadingExamples = false) }
        }
    }

    /** Fills an empty translation with a dictionary gloss; never overwrites what the user typed. */
    private fun suggestTranslation(text: String, language: Language?) {
        if (language == null || text.isBlank()) return
        _state.update { it.copy(lookingUpTranslation = true) }
        viewModelScope.launch {
            val suggestion = translationProvider.suggestTranslation(text, language)
            _state.update { s ->
                if (suggestion != null && s.draft.translation.isBlank()) {
                    s.copy(lookingUpTranslation = false, translationSuggested = true, draft = s.draft.copy(translation = suggestion))
                } else {
                    s.copy(lookingUpTranslation = false)
                }
            }
        }
    }

    fun update(transform: (TermDraft) -> TermDraft) {
        var textChanged = false
        _state.update {
            val draft = transform(it.draft)
            textChanged = draft.text != it.draft.text
            it.copy(draft = draft, error = null, duplicateOf = null, dirty = true, saved = false, translationSuggested = it.translationSuggested && draft.translation == it.draft.translation)
        }
        // Edits to the term text are saved with the next other change or on close, never mid-typing.
        if (!textChanged) scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            val id = doSave() ?: return@launch
            events.send(TermFormEvent.Saved(id, keepOpen = true))
        }
    }

    /** Persists pending edits immediately, e.g. before the form closes. */
    fun flush() {
        if (!_state.value.dirty) return
        autosaveJob?.cancel()
        viewModelScope.launch {
            val id = doSave() ?: return@launch
            events.send(TermFormEvent.Saved(id, keepOpen = true))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        // The form can disappear with unsaved edits (panel closed, navigation); persist them.
        val state = _state.value
        if (state.dirty && !state.loading && state.draft.languageId != 0L && state.draft.text.isNotBlank()) {
            GlobalScope.launch { runCatching { termService.save(state.draft) } }
        }
    }

    /** Status clicks save immediately, so the reading screen reflects the change without pressing Save. */
    fun setStatus(status: TermStatus) {
        update { it.copy(status = status, statusExplicitlySet = true) }
        autosaveJob?.cancel()
        viewModelScope.launch {
            val id = doSave() ?: return@launch
            events.send(TermFormEvent.Saved(id, keepOpen = true))
        }
    }

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
        autosaveJob?.cancel()
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
            _state.update { it.copy(saving = false, dirty = it.draft != draft, saved = true, draft = it.draft.copy(id = id, originalText = draft.text)) }
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
