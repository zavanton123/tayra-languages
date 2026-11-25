package com.idiomiko.feature.settings

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.playlists.PlaylistEnd
import com.idiomiko.core.domain.settings.UserSettings
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.ContentCard
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.NumberStepper
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.SelectMenu
import com.idiomiko.core.ui.components.SettingRow
import com.idiomiko.core.ui.components.SettingsPane
import com.idiomiko.core.ui.components.SettingsSection
import com.idiomiko.core.ui.components.SwitchSetting
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppSizes
import org.koin.compose.viewmodel.koinViewModel

/** Settings: how the playlist player goes from track to track, what it does at a playlist's end, and whether listening counts toward the daily goal. */
@Composable
fun PlaylistSettingsScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val compact = LocalWindowWidth.current.isCompact
    Scaffold(
        topBar = { AppTopBar(title = tr("Playlists"), onNavigate = onNavigate, onBack = onBack.takeIf { compact }, section = NavSection.SETTINGS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        SettingsPane(SettingsSection.PLAYLISTS, padding, onNavigate) { inner ->
            PageColumn(inner) {
                if (!compact) ScreenHeader(tr("Playlists"), null, onBackToSettings = onBack)
                ContentCard(tr("Playback"), null, icon = AppIcons.NavPlaylists, modifier = Modifier.widthIn(max = AppSizes.readableWidth), dense = compact) {
                    SwitchSetting(
                        tr("Pause between tracks"),
                        null,
                        checked = settings.playlistPauseBetweenTracks,
                        dense = compact,
                        switchModifier = Modifier.testTag("pause-between-tracks"),
                    ) { on -> viewModel.update { it.copy(playlistPauseBetweenTracks = on) } }
                    if (settings.playlistPauseBetweenTracks) {
                        SettingRow(
                            tr("Length of the pause (in seconds)"),
                            null,
                            divider = true,
                            stackOnCompact = true,
                            dense = compact,
                            modifier = Modifier.testTag("pause-seconds"),
                        ) {
                            NumberStepper(
                                value = settings.playlistPauseSeconds,
                                range = UserSettings.MIN_PLAYLIST_PAUSE..UserSettings.MAX_PLAYLIST_PAUSE,
                                name = tr("length of the pause"),
                            ) { n -> viewModel.update { it.copy(playlistPauseSeconds = n) } }
                        }
                    }
                    SettingRow(
                        tr("When a playlist ends"),
                        null,
                        divider = true,
                        stackOnCompact = true,
                        dense = compact,
                    ) {
                        SelectMenu(
                            PlaylistEnd.entries,
                            settings.playlistEnd,
                            { end -> viewModel.update { it.copy(playlistEnd = end) } },
                            optionLabel = { it.label() },
                            buttonModifier = Modifier.testTag("playlist-end"),
                        )
                    }
                }
                ContentCard(tr("Goals"), null, icon = AppIcons.Target, modifier = Modifier.widthIn(max = AppSizes.readableWidth), dense = compact) {
                    SwitchSetting(
                        tr("Count listening toward the daily goal"),
                        null,
                        checked = settings.playlistCountsTowardGoal,
                        dense = compact,
                        switchModifier = Modifier.testTag("listening-counts"),
                    ) { on -> viewModel.update { it.copy(playlistCountsTowardGoal = on) } }
                }
            }
        }
    }
}

private fun PlaylistEnd.label(): String = when (this) {
    PlaylistEnd.STOP -> tr("Stop")
    PlaylistEnd.START_AGAIN -> tr("Start again")
    PlaylistEnd.NEXT_PLAYLIST -> tr("Play the next playlist")
}
