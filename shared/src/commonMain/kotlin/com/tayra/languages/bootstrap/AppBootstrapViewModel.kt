package com.tayra.languages.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.DemoDataService
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
 * Opens the database and loads the demo data on first start.
 */
class AppBootstrapViewModel(
    private val demoData: DemoDataService,
    private val bookStats: BookStatsService,
) : ViewModel() {

    private val _state = MutableStateFlow<BootstrapState>(BootstrapState.Loading)
    val state: StateFlow<BootstrapState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                demoData.loadIfEmpty()
                _state.value = BootstrapState.Ready
                bookStats.refreshAll()
            } catch (e: Exception) {
                Logger.e(e) { "Bootstrap failed" }
                _state.value = BootstrapState.Failed(e.message ?: e.toString())
            }
        }
    }
}
