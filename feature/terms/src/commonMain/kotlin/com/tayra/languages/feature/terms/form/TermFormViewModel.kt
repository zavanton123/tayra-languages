package com.tayra.languages.feature.terms.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.language.LanguageCodes
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
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.TermValidationException
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.state.UiEvents
import com.tayra.languages.feature.terms.examples.ExampleTerms
import com.tayra.languages.feature.terms.examples.WordStatus
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

/** What the term form edits; [sentence] is the one the term is being read in, when it was opened from a text. */
sealed interface TermFormKey {
    val sentence: String? get() = null

    data class ById(val termId: Long, override val sentence: String? = null) : TermFormKey
    data class ByText(val languageId: Long, val text: String, override val sentence: String? = null) : TermFormKey
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
    /** Total matches reported by the examples provider, when known. */
    val examplesTotal: Int? = null,
    val loadingExamples: Boolean = false,
    /** Code of the language translations are shown in. */
    val nativeLanguage: String = "en",
    /** When the stored term was created; null for new terms. */
    val createdAt: Instant? = null,
    /** Offline dictionary entries for the term, empty when no bundled dictionary covers the language pair. */
    val dictionary: DictionaryLookup = DictionaryLookup.EMPTY,
    /** The downloadable pack for the language pair and whether it is on the device; null when none exists. */
    val dictionaryPack: PackStatus? = null,
    /** The words of each example with their statuses, by the example's text. */
    val exampleWords: Map<String, List<WordStatus>> = emptyMap(),
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

    /** A word or phrase of an example was picked; [sentence] is that example. */
    data class OpenTerm(val languageId: Long, val text: String, val sentence: String?) : TermFormEvent
}

private const val AUTOSAVE_DELAY_MS = 700L
private const val LOOKUP_DELAY_MS = 400L

class TermFormViewModel(
    private val key: TermFormKey,
    private val termService: TermService,
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val translationProvider: TermTranslationProvider,
    private val examplesProvider: ExampleSentencesProvider,
    private val dictionary: OfflineDictionary,
    private val dictionaries: DictionaryService,
    /** Lets words of the examples be saved; without it the examples are only shown. */
    private val exampleTerms: ExampleTerms? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(TermFormUiState())
    val state: StateFlow<TermFormUiState> = _state.asStateFlow()
    val events = UiEvents<TermFormEvent>()
    private var searchJob: Job? = null
    private var autosaveJob: Job? = null
    private var packJob: Job? = null
    private var lookupJob: Job? = null
    private var entryJob: Job? = null

    /** The sentence the form's term is read in, stored with a term that starts being learned while it still occurs there. */
    private fun sentenceFor(draft: TermDraft): String? {
        val sentence = key.sentence ?: return null
        val letters = { text: String -> text.filter { it.isLetterOrDigit() }.lowercase() }
        val term = letters(draft.text)
        return sentence.takeIf { term.isNotEmpty() && letters(it).contains(term) }
    }

    init {
        viewModelScope.launch { load() }
        viewModelScope.launch {
            languages.observeAll().collect { list -> _state.update { if (it.loading) it else it.copy(languages = list) } }
        }
    }

    private suspend fun load() {
        val languageList = languages.getAll()
        val draft = try {
            when (key) {
                // An unknown word shows as Unknown; saving it with other edits starts it at 1 (see toSave).
                is TermFormKey.ById -> termService.load(key.termId)
                is TermFormKey.ByText -> if (key.text.isBlank()) emptyDraft(key.languageId) else termService.findOrNew(key.languageId, key.text)
                TermFormKey.New -> {
                    val current = settings.current.currentLanguageId
                    val languageId = if (languageList.any { it.id == current }) current else languageList.singleOrNull()?.id ?: 0L
                    emptyDraft(languageId)
                }
            }
        } catch (e: NoSuchElementException) {
            _state.update { it.copy(loading = false, error = e.message) }
            return
        }
        _state.update { it.copy(loading = false, draft = draft, languages = languageList, nativeLanguage = settings.current.nativeLanguage.ifBlank { "en" }) }
        observePack(_state.value.language)
        showEntry(draft, linkParent = { setParents(listOf(it)) })
    }

    private fun emptyDraft(languageId: Long) = TermDraft(languageId = languageId, text = "", originalText = "", status = TermStatus.UNKNOWN)

    /**
     * Shows [draft] with what is known about its text: dictionary meanings, a suggested
     * translation, the lemma as parent and examples. [linkParent] applies the lemma.
     */
    private fun showEntry(draft: TermDraft, linkParent: (String) -> Unit) {
        entryJob?.cancel()
        _state.update {
            it.copy(
                draft = draft, dirty = false, saved = false, error = null, duplicateOf = null, createdAt = null, references = null,
                translationSuggested = false, lookingUpTranslation = false, dictionary = DictionaryLookup.EMPTY,
                examples = emptyList(), examplesTotal = null, loadingExamples = false,
            )
        }
        entryJob = viewModelScope.launch {
            // Opening the form acknowledges any flash message.
            draft.id?.let { terms.clearFlashMessage(it) }
            val createdAt = draft.id?.let { terms.getById(it)?.createdAt }
            _state.update { it.copy(createdAt = createdAt) }
            val language = _state.value.language
            val lookup = lookupDictionary(draft.text, language)
            // Words on a page exist as placeholders before anyone opens them, so "new" is judged by
            // content: no translation and no parent means nobody has curated the term yet.
            val untouched = draft.translation.isBlank() && draft.parents.isEmpty()
            if (draft.translation.isBlank()) {
                val gloss = lookup.suggestedTranslation
                if (gloss != null) {
                    _state.update { s -> s.copy(translationSuggested = true, draft = s.draft.copy(translation = gloss)) }
                } else {
                    launch { suggestTranslation(draft.text, language) }
                }
            }
            // A new inflected form is linked to its lemma so the family shares one status.
            lookup.parentSuggestion?.let { if (untouched) linkParent(it) }
            loadExamples(draft.text, language)
        }
    }

    /** Looks up [text] like a dictionary: the stored term with that text, or a new one. */
    private fun scheduleLookup(text: String) {
        lookupJob?.cancel()
        val languageId = _state.value.draft.languageId
        if (languageId == 0L) return
        if (text.isBlank()) {
            showEntry(emptyDraft(languageId), linkParent = {})
            return
        }
        lookupJob = viewModelScope.launch {
            delay(LOOKUP_DELAY_MS)
            val found = termService.findOrNew(languageId, text)
            // The field keeps what was typed; saving normalizes it.
            val draft = if (found.id == null) found.copy(text = text, originalText = "", status = TermStatus.UNKNOWN) else found.copy(text = text)
            // A typed word is only stored once it is edited, so the lemma joins the draft without a save.
            showEntry(draft, linkParent = { parent -> _state.update { it.copy(draft = it.draft.copy(parents = listOf(parent), syncStatus = true)) }; inheritParentStatus(parent) })
        }
    }

    private suspend fun lookupDictionary(text: String, language: Language?): DictionaryLookup {
        if (language == null || text.isBlank()) return DictionaryLookup.EMPTY
        val id = DictionaryPacks.find(LanguageCodes.codeFor(language.name), settings.current.nativeLanguage)?.id ?: return DictionaryLookup.EMPTY
        val lookup = dictionary.lookup(id, text)
        _state.update { it.copy(dictionary = lookup) }
        return lookup
    }

    /** Tracks the pack for the language pair, and looks the term up once the pack is installed. */
    private fun observePack(language: Language?) {
        packJob?.cancel()
        val pack = DictionaryPacks.find(language?.let { LanguageCodes.codeFor(it.name) }, settings.current.nativeLanguage)
        if (pack == null) {
            _state.update { it.copy(dictionaryPack = null) }
            return
        }
        packJob = viewModelScope.launch {
            dictionaries.packs.collect { list ->
                val status = list.firstOrNull { it.pack.id == pack.id } ?: PackStatus(pack, PackState.NotInstalled)
                val wasInstalled = _state.value.dictionaryPack?.state is PackState.Installed
                _state.update { it.copy(dictionaryPack = status) }
                if (status.state is PackState.Installed && !wasInstalled && _state.value.dictionary.isEmpty) {
                    val current = _state.value
                    val lookup = lookupDictionary(current.draft.text, current.language)
                    val gloss = lookup.suggestedTranslation
                    if (gloss != null && _state.value.draft.translation.isBlank()) {
                        _state.update { it.copy(translationSuggested = true, draft = it.draft.copy(translation = gloss)) }
                    }
                }
            }
        }
    }

    fun downloadDictionary() {
        val pack = _state.value.dictionaryPack?.pack ?: return
        viewModelScope.launch { dictionaries.download(pack) }
    }

    /** Appends a dictionary gloss to the translation. */
    fun addGloss(gloss: String) {
        update { d -> d.copy(translation = if (d.translation.isBlank()) gloss else "${d.translation.trimEnd()}; $gloss") }
        _state.update { it.copy(translationSuggested = false) }
    }

    private suspend fun loadExamples(text: String, language: Language?) {
        if (language == null || text.isBlank()) return
        _state.update { it.copy(loadingExamples = true) }
        val native = settings.current.nativeLanguage.ifBlank { "en" }
        val result = examplesProvider.search(ExampleSearchQuery(text, language, native, minWords = 1, maxWords = 15, sort = ExampleSort.RANDOM, limit = 10))
        _state.update { it.copy(examples = result.sentences, examplesTotal = result.total, loadingExamples = false, nativeLanguage = native) }
        refreshExampleTerms()
    }

    private suspend fun refreshExampleTerms() {
        val helper = exampleTerms ?: return
        val current = _state.value
        val language = current.language ?: return
        val found = helper.statuses(current.examples.map { it.text }, language)
        _state.update { it.copy(exampleWords = found) }
    }

    /** A right click on a word of an example: saved as the reader does, with the example as its sentence. */
    fun markExampleWord(word: String, sentence: String) {
        val helper = exampleTerms ?: return
        val language = _state.value.language ?: return
        viewModelScope.launch {
            val id = helper.toggle(word, sentence, language) ?: return@launch
            // The form's own term shows its new status too.
            if (id == _state.value.draft.id) terms.getById(id)?.let { term -> _state.update { it.copy(draft = it.draft.copy(status = term.status)) } }
            refreshExampleTerms()
            events.send(TermFormEvent.Saved(id, keepOpen = true))
        }
    }

    /** A click on a word of an example, or a phrase dragged over in one: the form moves on to it. */
    fun openExampleTerm(text: String, sentence: String) {
        val languageId = _state.value.draft.languageId
        if (languageId == 0L || text.isBlank()) return
        viewModelScope.launch {
            if (_state.value.dirty) {
                val saved = doSave() ?: return@launch
                events.send(TermFormEvent.Saved(saved, keepOpen = true))
            }
            events.send(TermFormEvent.OpenTerm(languageId, text.trim(), sentence))
        }
    }

    /** Fills an empty translation with a dictionary gloss; never overwrites what the user typed. */
    private suspend fun suggestTranslation(text: String, language: Language?) {
        if (language == null || text.isBlank()) return
        _state.update { it.copy(lookingUpTranslation = true) }
        val suggestion = translationProvider.suggest(text, language)
        _state.update { s ->
            if (suggestion != null && s.draft.translation.isBlank()) {
                s.copy(lookingUpTranslation = false, translationSuggested = true, draft = s.draft.copy(translation = suggestion.text))
            } else {
                s.copy(lookingUpTranslation = false)
            }
        }
    }

    fun update(transform: (TermDraft) -> TermDraft) {
        val before = _state.value
        val draft = transform(before.draft)
        if (draft.text != before.draft.text) {
            changeText(before, draft.text)
            return
        }
        // An edit made before a typed word was looked up applies to the typed word as a new term.
        lookupJob?.cancel()
        _state.update {
            val stillSuggested = it.translationSuggested && draft.translation == it.draft.translation
            it.copy(draft = transform(it.draft), error = null, duplicateOf = null, dirty = true, saved = false, translationSuggested = stillSuggested)
        }
        scheduleAutosave()
    }

    /** Typing in the term field looks the new text up instead of renaming the term. */
    private fun changeText(before: TermFormUiState, text: String) {
        autosaveJob?.cancel()
        entryJob?.cancel()
        if (before.dirty && before.draft.text.isNotBlank()) {
            val pending = toSave(before.draft)
            val sentence = sentenceFor(before.draft)
            viewModelScope.launch {
                val id = runCatching { termService.save(pending, sentence) }.getOrNull() ?: return@launch
                events.send(TermFormEvent.Saved(id, keepOpen = true))
            }
        }
        _state.update {
            it.copy(
                draft = emptyDraft(it.draft.languageId).copy(text = text), dirty = false, saved = false, error = null, duplicateOf = null,
                createdAt = null, references = null, translationSuggested = false, lookingUpTranslation = false,
                dictionary = DictionaryLookup.EMPTY, examples = emptyList(), examplesTotal = null, loadingExamples = false,
            )
        }
        scheduleLookup(text)
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
            GlobalScope.launch { runCatching { termService.save(toSave(state.draft), sentenceFor(state.draft)) } }
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

    /**
     * What is stored for [draft]: an unknown word given a translation or other edits starts at
     * status 1, as when learning a new word, unless Unknown was picked on purpose.
     */
    private fun toSave(draft: TermDraft): TermDraft =
        if (draft.status == TermStatus.UNKNOWN && !draft.statusExplicitlySet) draft.copy(status = TermStatus.NEW_1) else draft

    private suspend fun doSave(): Long? {
        val draft = toSave(_state.value.draft)
        if (draft.languageId == 0L) {
            _state.update { it.copy(error = "Please select a language") }
            return null
        }
        _state.update { it.copy(saving = true, error = null) }
        return try {
            val id = termService.save(draft, sentenceFor(draft))
            _state.update {
                // The stored status shows from now on: 1 for a word just started.
                val shown = if (it.draft.status == TermStatus.UNKNOWN && !it.draft.statusExplicitlySet) it.draft.copy(status = draft.status) else it.draft
                it.copy(saving = false, dirty = toSave(shown) != draft, saved = true, draft = shown.copy(id = id, originalText = draft.text))
            }
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
