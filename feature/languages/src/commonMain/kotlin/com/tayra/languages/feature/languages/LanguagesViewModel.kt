package com.tayra.languages.feature.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.model.LanguageSummary
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.LanguageService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LanguagesUiState(
    val loading: Boolean = true,
    val languages: List<LanguageSummary> = emptyList(),
)

class LanguagesViewModel(
    languages: LanguageRepository,
    private val languageService: LanguageService,
) : ViewModel() {

    val state: StateFlow<LanguagesUiState> = languages.observeSummaries()
        .map { LanguagesUiState(loading = false, languages = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LanguagesUiState())

    fun delete(languageId: Long) {
        viewModelScope.launch { languageService.delete(languageId) }
    }
}
