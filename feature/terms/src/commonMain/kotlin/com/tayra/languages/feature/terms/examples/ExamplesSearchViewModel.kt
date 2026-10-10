package com.tayra.languages.feature.terms.examples

import com.tayra.languages.core.ui.i18n.tr
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.theme.ReadingFont
import kotlin.math.roundToInt

data class ExamplesSearchUiState(
    val loading: Boolean = true,
    val language: Language? = null,
    val query: ExampleSearchQuery? = null,
    val results: List<ExampleSentence> = emptyList(),
    val total: Int? = null,
    val nextPage: String? = null,
    val searching: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    /** The term shown in the term pane: the searched one, or a word clicked in an example; null once closed. */
    val paneTerm: String? = null,
    /** The example the pane's term was clicked in, if any. */
    val paneSentence: String? = null,
    /** The words of each result with their statuses, by the result's text. */
    val words: Map<String, List<WordStatus>> = emptyMap(),
) {
    val hasMore: Boolean get() = nextPage != null
}

class ExamplesSearchViewModel(
    private val languageId: Long,
    private val initialText: String,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val provider: ExampleSentencesProvider,
    /** Lets words of the results be saved; without it the results are only shown. */
    private val exampleTerms: ExampleTerms? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(ExamplesSearchUiState())
    val state: StateFlow<ExamplesSearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var filterJob: Job? = null

    init {
        viewModelScope.launch {
            val language = languages.getById(languageId)
            if (language == null) {
                _state.update { it.copy(loading = false, error = tr("Language not found")) }
                return@launch
            }
            val query = defaultFilters(ExampleSearchQuery(text = initialText, language = language, targetLanguage = settings.current.nativeLanguage.ifBlank { "en" }))
            _state.update { it.copy(loading = false, language = language, query = query, paneTerm = initialText.trim().ifEmpty { null }) }
            search()
        }
    }

    fun updateQuery(transform: (ExampleSearchQuery) -> ExampleSearchQuery) {
        _state.update { s -> s.query?.let { s.copy(query = transform(it)) } ?: s }
    }

    /** Applies a filter change and re-runs the search after a short pause, so typed numbers settle first. */
    fun updateFilters(transform: (ExampleSearchQuery) -> ExampleSearchQuery) {
        updateQuery(transform)
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(FILTER_DEBOUNCE_MS)
            search()
        }
    }

    fun resetFilters() {
        updateQuery(::defaultFilters)
        search()
    }

    private fun defaultFilters(query: ExampleSearchQuery) =
        query.copy(minWords = 1, maxWords = 50, sort = ExampleSort.RANDOM, limit = 10, hasAudio = null)

    fun search() {
        val query = _state.value.query ?: return
        if (query.text.isBlank()) return
        filterJob?.cancel()
        searchJob?.cancel()
        _state.update { it.copy(searching = true, error = null, results = emptyList(), total = null, nextPage = null, paneTerm = query.text.trim(), paneSentence = null) }
        searchJob = viewModelScope.launch {
            val result = provider.search(query)
            _state.update { it.copy(searching = false, results = result.sentences, total = result.total, nextPage = result.nextPage) }
            loadLearning()
        }
    }

    /** Looks the saved terms of the results up again, after one of them may have changed. */
    fun refreshTerms() {
        viewModelScope.launch { loadLearning() }
    }

    private var shown = false

    /** The screen came back to the front: terms may have changed on a screen opened from here. The first time, it is still loading. */
    fun onResumed() {
        if (shown && !_state.value.loading) refreshTerms()
        shown = true
    }

    // ---- the examples' text, set as the reader's (one setting for both)

    fun setReadingFont(font: ReadingFont) = updateSettings { it.copy(readingFont = font.id) }
    fun setFontScale(scale: Float) = updateSettings { it.copy(readingFontScale = ((scale * 10).roundToInt() / 10f).coerceIn(0.6f, 2.5f)) }
    fun setLineHeight(height: Float) = updateSettings { it.copy(readingLineHeight = ((height * 10).roundToInt() / 10f).coerceIn(1.0f, 3.0f)) }

    private fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    private suspend fun loadLearning() {
        val helper = exampleTerms ?: return
        val current = _state.value
        val language = current.language ?: return
        val found = helper.statuses(current.results.map { it.text }, language)
        _state.update { it.copy(words = found) }
    }

    /** The status shortcuts, as the reader uses them. */
    val hotkeys: Map<HotkeyAction, Hotkey?> get() = settings.current.hotkeys

    /** A status shortcut on a word of a result. */
    fun setStatus(word: String, sentence: String?, status: TermStatus) {
        val helper = exampleTerms ?: return
        val language = _state.value.language ?: return
        viewModelScope.launch {
            helper.setStatus(word, sentence, language, status)
            loadLearning()
        }
    }

    fun shiftStatus(word: String, sentence: String?, delta: Int) {
        val helper = exampleTerms ?: return
        val language = _state.value.language ?: return
        viewModelScope.launch {
            helper.shiftStatus(word, sentence, language, delta)
            loadLearning()
        }
    }

    /** A right click on a word of a result: saved as the reader does, with the result as its sentence. */
    fun markWord(word: String, sentence: String) {
        val helper = exampleTerms ?: return
        val language = _state.value.language ?: return
        viewModelScope.launch {
            helper.toggle(word, sentence, language)
            loadLearning()
        }
    }

    /** Shows the pane for [word]; [sentence] is the example it was clicked in. */
    fun openTerm(word: String, sentence: String? = null) {
        word.trim().takeIf { it.isNotEmpty() }?.let { text -> _state.update { it.copy(paneTerm = text, paneSentence = sentence) } }
    }

    fun closePane() = _state.update { it.copy(paneTerm = null, paneSentence = null) }

    fun loadMore() {
        val s = _state.value
        val next = s.nextPage ?: return
        if (s.loadingMore || s.searching) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            val result = provider.nextPage(next, s.query?.targetLanguage ?: "en")
            _state.update { it.copy(loadingMore = false, results = it.results + result.sentences, nextPage = result.nextPage) }
            loadLearning()
        }
    }

    private companion object {
        const val FILTER_DEBOUNCE_MS = 400L
    }
}
