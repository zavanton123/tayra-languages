package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.SettingsSection
import com.idiomiko.core.ui.components.SettingsPane
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.SwitchSetting
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.media.videoSupported
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSizes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExperimentalFeaturesScreen(
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    videosAvailable: Boolean = videoSupported,
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val compact = LocalWindowWidth.current.isCompact
    Scaffold(
        topBar = { AppTopBar(title = tr("Experiments"), onNavigate = onNavigate, onBack = onBack.takeIf { compact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        SettingsPane(SettingsSection.EXPERIMENTS, padding, onNavigate) { inner ->
            PageColumn(inner) {
                ScreenHeader(tr("Experiments"), tr("Turn on features that are still being built."), onBackToSettings = onBack)
                ContentCard(tr("Features"), subtitle = null, icon = AppIcons.Lightbulb, modifier = Modifier.widthIn(max = AppSizes.readableWidth), dense = compact) {
                    SwitchSetting(
                        tr("Videos"),
                        if (videosAvailable) tr("Watch videos with their subtitles as text you can read and look up.") else tr("Videos aren't available on the web yet."),
                        checked = videosAvailable && settings.experimentalVideos,
                        enabled = videosAvailable,
                        dense = compact,
                        switchModifier = Modifier.testTag("experimental-videos"),
                        onChange = { on -> viewModel.update { it.copy(experimentalVideos = on) } },
                    )
                }
            }
        }
    }
}
