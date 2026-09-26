package com.tayra.languages.feature.languages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.model.LanguageSummary
import com.tayra.languages.core.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LanguagesUiState(
    val loading: Boolean = true,
    val languages: List<LanguageSummary> = emptyList(),
)

class LanguagesViewModel(languages: LanguageRepository) : ViewModel() {

    val state: StateFlow<LanguagesUiState> = languages.observeSummaries()
        .map { LanguagesUiState(loading = false, languages = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LanguagesUiState())
}
