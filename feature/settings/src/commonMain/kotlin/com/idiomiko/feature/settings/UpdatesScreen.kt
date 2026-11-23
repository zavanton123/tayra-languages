package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.idiomiko.core.domain.service.AppUpdateService
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.SettingsPane
import com.idiomiko.core.ui.components.SettingsSection
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSizes
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class UpdatesViewModel(
    private val settings: SettingsRepository,
    /** The desktop app's updates; other platforms have none. */
    val updates: AppUpdateService? = null,
) : ViewModel() {
    val state: StateFlow<UserSettings> = settings.settings

    fun checkAtStart(on: Boolean) {
        viewModelScope.launch { settings.update { it.copy(checkForUpdates = on) } }
    }
}

/** Settings: the app's version and its updates, on the platforms where the app updates itself. */
@Composable
fun UpdatesScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: UpdatesViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppTopBar(title = tr("Updates"), onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        SettingsPane(SettingsSection.UPDATES, padding, onNavigate) { inner ->
            PageColumn(inner) {
                ScreenHeader(tr("Updates"), null, onBackToSettings = onBack)
                // As wide as a line of text, so the switch stays near what it turns on.
                viewModel.updates?.let { AppUpdatesCard(it, settings, Modifier.widthIn(max = AppSizes.readableWidth), viewModel::checkAtStart) }
            }
        }
    }
}
