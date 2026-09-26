package com.tayra.languages.feature.terms.examples

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExamplesSearchUiState(
    val loading: Boolean = true,
    val language: Language? = null,
    val query: ExampleSearchQuery? = null,
    val results: List<ExampleSentence> = emptyList(),
    val total: Int? = null,
    val nextPage: String? = null,
    val searching: Boolean = false,
    val loadingMore: Boolean = false,
    val filtersVisible: Boolean = true,
    val error: String? = null,
) {
    val hasMore: Boolean get() = nextPage != null
}

class ExamplesSearchViewModel(
    private val languageId: Long,
    private val initialText: String,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val provider: ExampleSentencesProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(ExamplesSearchUiState())
    val state: StateFlow<ExamplesSearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val language = languages.getById(languageId)
            if (language == null) {
                _state.update { it.copy(loading = false, error = "Language not found") }
                return@launch
            }
            val query = ExampleSearchQuery(
                text = initialText,
                language = language,
                targetLanguage = settings.current.translationTargetLanguage.ifBlank { "en" },
                minWords = 3,
                maxWords = 14,
            )
            _state.update { it.copy(loading = false, language = language, query = query) }
            search()
        }
    }

    fun updateQuery(transform: (ExampleSearchQuery) -> ExampleSearchQuery) {
        _state.update { s -> s.query?.let { s.copy(query = transform(it)) } ?: s }
    }

    fun toggleFilters() = _state.update { it.copy(filtersVisible = !it.filtersVisible) }

    fun search() {
        val query = _state.value.query ?: return
        if (query.text.isBlank()) return
        searchJob?.cancel()
        _state.update { it.copy(searching = true, error = null, results = emptyList(), total = null, nextPage = null) }
        searchJob = viewModelScope.launch {
            val result = provider.search(query)
            _state.update { it.copy(searching = false, results = result.sentences, total = result.total, nextPage = result.nextPage) }
        }
    }

    fun loadMore() {
        val s = _state.value
        val next = s.nextPage ?: return
        if (s.loadingMore || s.searching) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            val result = provider.nextPage(next, s.query?.targetLanguage ?: "en")
            _state.update { it.copy(loadingMore = false, results = it.results + result.sentences, nextPage = result.nextPage) }
        }
    }
}
