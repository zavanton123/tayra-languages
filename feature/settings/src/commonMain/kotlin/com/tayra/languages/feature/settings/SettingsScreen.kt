package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppThemes
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<UserSettings> = settings.settings
    fun update(transform: (UserSettings) -> UserSettings) = viewModelScope.launch { settings.update(transform) }
}

@Composable
fun SettingsScreen(onNavigate: (Route) -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { AppTopBar(title = "Settings", onNavigate = onNavigate) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("Appearance")
            Dropdown(
                options = AppThemes.all,
                selected = AppThemes.byId(settings.themeId),
                onSelect = { theme -> viewModel.update { it.copy(themeId = theme.id) } },
                label = "Theme",
                optionLabel = { it.label },
                modifier = Modifier.fillMaxWidth(),
            )
            SwitchRow("Highlight terms by status", settings.showHighlights) { v -> viewModel.update { it.copy(showHighlights = v) } }
            Text("Reading font size: ${(settings.readingFontScale * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(value = settings.readingFontScale, onValueChange = { v -> viewModel.update { it.copy(readingFontScale = v) } }, valueRange = 0.6f..2.5f)
            Text("Reading line height: ${(settings.readingLineHeight * 10).toInt() / 10f}", style = MaterialTheme.typography.bodyMedium)
            Slider(value = settings.readingLineHeight, onValueChange = { v -> viewModel.update { it.copy(readingLineHeight = v) } }, valueRange = 1.0f..3.0f)

            Section("Behaviour")
            SwitchRow("Show reading streak on home page", settings.showStreakOnHome) { v -> viewModel.update { it.copy(showStreakOnHome = v) } }
            SwitchRow("Quick set status: tapping an unknown word sets status 1", settings.tapSetsStatus) { v -> viewModel.update { it.copy(tapSetsStatus = v) } }
            OutlinedTextField(
                value = settings.statsSampleSize.toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { n -> viewModel.update { it.copy(statsSampleSize = n.coerceIn(UserSettings.MIN_STATS_SAMPLE_SIZE, UserSettings.MAX_STATS_SAMPLE_SIZE)) } } },
                label = { Text("Book stats page sample size") },
                supportingText = { Text("Number of pages used for book statistics") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Term popups")
            SwitchRow("Promote parent translation to term translation if possible", settings.promoteParentTranslation) { v -> viewModel.update { it.copy(promoteParentTranslation = v) } }
            SwitchRow("Show component terms", settings.showComponents) { v -> viewModel.update { it.copy(showComponents = v) } }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
