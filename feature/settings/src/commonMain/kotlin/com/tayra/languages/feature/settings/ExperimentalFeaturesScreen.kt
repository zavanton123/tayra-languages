package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ContentCard
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ScreenTitle
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SwitchSetting
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.media.videoSupported
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppSpacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExperimentalFeaturesScreen(
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    videosAvailable: Boolean = videoSupported,
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { AppTopBar(title = tr("Experimental features"), onNavigate = onNavigate, onBack = onBack.takeIf { LocalWindowWidth.current.isCompact }, section = NavSection.SETTINGS) }) { padding ->
        ScrollColumn(Modifier.padding(padding).fillMaxSize(), contentModifier = Modifier.padding(AppSpacing.l).widthIn(max = TEXT_WIDTH), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            ScreenTitle(tr("Experimental features"))
            Text(tr("Turn on features that are still being built."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ContentCard(tr("Features"), subtitle = null, icon = AppIcons.Lightbulb) {
                SwitchSetting(
                    tr("Videos"),
                    if (videosAvailable) tr("Watch videos with their subtitles as text you can read and look up.") else tr("Videos aren't available on the web yet."),
                    checked = videosAvailable && settings.experimentalVideos,
                    enabled = videosAvailable,
                    switchModifier = Modifier.testTag("experimental-videos"),
                    onChange = { on -> viewModel.update { it.copy(experimentalVideos = on) } },
                )
            }
        }
    }
}

private val TEXT_WIDTH = 720.dp
