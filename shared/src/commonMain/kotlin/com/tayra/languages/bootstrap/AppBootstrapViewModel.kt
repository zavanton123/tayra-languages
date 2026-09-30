package com.tayra.languages.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.DemoDataService
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.TranslationLanguageKeeper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BootstrapState {
    data object Loading : BootstrapState
    data object Ready : BootstrapState
    data class Failed(val message: String) : BootstrapState
}

/**
 * Opens the database and makes sure the catalog languages exist, with demo data on first start,
 * then keeps saved translations in the native language for as long as the app runs.
 */
class AppBootstrapViewModel(
    private val demoData: DemoDataService,
    private val bookStats: BookStatsService,
    private val dictionaries: DictionaryService,
    private val translationLanguages: TranslationLanguageKeeper,
) : ViewModel() {

    private val _state = MutableStateFlow<BootstrapState>(BootstrapState.Loading)
    val state: StateFlow<BootstrapState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                demoData.ensureLanguages()
                _state.value = BootstrapState.Ready
                bookStats.refreshAll()
                dictionaries.refresh()
                translationLanguages.start(viewModelScope)
            } catch (e: Exception) {
                Logger.e(e) { "Bootstrap failed" }
                _state.value = BootstrapState.Failed(e.message ?: e.toString())
            }
        }
    }
}
