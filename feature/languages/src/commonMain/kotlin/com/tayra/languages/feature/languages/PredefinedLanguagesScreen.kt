package com.tayra.languages.feature.languages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.service.LanguageService
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.EmptyMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class PredefinedLanguagesUiState(
    val loading: Boolean = true,
    val names: List<String> = emptyList(),
    val loadingName: String? = null,
)

class PredefinedLanguagesViewModel(private val languageService: LanguageService) : ViewModel() {
    private val _state = MutableStateFlow(PredefinedLanguagesUiState())
    val state: StateFlow<PredefinedLanguagesUiState> = _state.asStateFlow()
    val events = UiEvents<Route>()

    init {
        viewModelScope.launch {
            val names = languageService.predefinedNotLoaded().map { it.name }
            _state.update { it.copy(loading = false, names = names) }
        }
    }

    fun load(name: String) {
        if (_state.value.loadingName != null) return
        _state.update { it.copy(loadingName = name) }
        viewModelScope.launch {
            languageService.loadPredefined(name)
            events.send(Route.Home)
        }
    }
}

@Composable
fun PredefinedLanguagesScreen(
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: PredefinedLanguagesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { onNavigate(it) }

    Scaffold(topBar = { AppTopBar(title = "Predefined languages", onNavigate = onNavigate, onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Choose a language to load it along with a short sample text. You can edit its settings and dictionaries afterwards.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            when {
                state.loading -> LoadingIndicator()
                state.names.isEmpty() -> EmptyMessage("All predefined languages are already loaded.")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.names) { name ->
                        val loadingThis = state.loadingName == name
                        Text(
                            if (loadingThis) "$name (loading...)" else name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth().clickable(enabled = state.loadingName == null) { viewModel.load(name) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
