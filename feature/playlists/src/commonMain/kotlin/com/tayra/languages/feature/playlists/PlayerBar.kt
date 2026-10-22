package com.tayra.languages.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.playlists.PlaybackProblem
import com.tayra.languages.core.domain.playlists.PlayerStatus
import com.tayra.languages.core.domain.playlists.RepeatMode
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppSlider
import com.tayra.languages.core.ui.components.AppTooltip
import com.tayra.languages.core.ui.components.BarSurface
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel

/**
 * The bar the app shows while a playlist is loaded, whatever screen is open: the text and the
 * playlist, the controls, the place in the text and the speed, with a way back to the text in the
 * reader, and a close button at its end that stops the player until a playlist is played again.
 * A phone shows a small one with play and next, which opens the player; [onPlayerScreen] leaves it
 * out there, as the player screen has the controls itself.
 */
@Composable
fun PlayerBar(onNavigate: (Route) -> Unit, onPlayerScreen: Boolean, viewModel: PlayerViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.player.isLoaded) return
    val compact = LocalWindowWidth.current.isCompact
    if (compact && onPlayerScreen) return
    BarSurface(raised = true, modifier = Modifier.testTag("player-bar")) {
        Column {
            AppDivider()
            if (compact) MiniBar(state, viewModel, onOpen = { onNavigate(Route.Player) })
            else WideBar(state, viewModel, onNavigate, onPlayerScreen)
        }
    }
}

@Composable
private fun NowPlayingTitle(state: PlayerUiState, modifier: Modifier, onOpen: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val p = state.player
    Row(
        modifier.clip(AppShapes.control).then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier).padding(AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        IconTile(AppIcons.Description, size = TILE)
        Column(Modifier.weight(1f)) {
            Text(p.item?.title.orEmpty(), style = AppText.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(p.playlist?.name.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MiniBar(state: PlayerUiState, viewModel: PlayerViewModel, onOpen: () -> Unit) {
    val p = state.player
    Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.m, vertical = AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
        NowPlayingTitle(state, Modifier.weight(1f).testTag("open-player"), onOpen)
        PlayPauseButton(state, viewModel, IconButtonSize.Regular)
        AppIconButton(AppIcons.SkipNext, contentDescription = tr("Next text"), onClick = viewModel.player::next, enabled = p.hasNext)
        CloseButton(viewModel)
    }
}

@Composable
private fun WideBar(state: PlayerUiState, viewModel: PlayerViewModel, onNavigate: (Route) -> Unit, onPlayerScreen: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.l, vertical = AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        BackToText(state, onNavigate)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            NowPlayingTitle(state, Modifier.weight(1f), onOpen = if (onPlayerScreen) null else { { onNavigate(Route.Player) } })
            PlayerControls(state, viewModel, Modifier.weight(2.2f), wide = true)
            if (!onPlayerScreen) AppIconButton(AppIcons.QueueMusic, contentDescription = tr("Open the player"), onClick = { onNavigate(Route.Player) }, style = IconButtonStyle.Outlined, modifier = Modifier.testTag("open-player"))
            CloseButton(viewModel)
        }
    }
}

/** Stops the player, keeping where listening got to; the bar, and the system's controls, go with it. */
@Composable
private fun CloseButton(viewModel: PlayerViewModel) {
    val label = tr("Close player")
    AppTooltip(label) {
        AppIconButton(Icons.Default.Close, contentDescription = label, onClick = viewModel.player::stop, style = IconButtonStyle.Outlined, shape = AppShapes.circle, modifier = Modifier.testTag("close-player"))
    }
}

/**
 * Shuffle, previous, play or pause, next and repeat, then the place in the text with its seek bar
 * and the speed; on one row when [wide], else the buttons above the seek bar.
 */
@Composable
internal fun PlayerControls(state: PlayerUiState, viewModel: PlayerViewModel, modifier: Modifier = Modifier, wide: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val p = state.player
    val player = viewModel.player
    val buttons: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            AppIconButton(AppIcons.Shuffle, contentDescription = if (p.shuffle) tr("Shuffle on") else tr("Shuffle off"), onClick = { player.setShuffle(!p.shuffle) }, selected = p.shuffle, tint = if (p.shuffle) colors.primary else null, modifier = Modifier.testTag("shuffle"))
            AppIconButton(AppIcons.SkipPrevious, contentDescription = tr("Previous text"), onClick = player::previous, tint = colors.primary, modifier = Modifier.testTag("bar-previous"))
            PlayPauseButton(state, viewModel, IconButtonSize.Large)
            AppIconButton(AppIcons.SkipNext, contentDescription = tr("Next text"), onClick = player::next, enabled = p.hasNext, tint = colors.primary, modifier = Modifier.testTag("bar-next"))
            AppIconButton(
                if (p.repeat == RepeatMode.ONE) AppIcons.RepeatOne else AppIcons.Repeat,
                contentDescription = when (p.repeat) {
                    RepeatMode.OFF -> tr("Repeat off")
                    RepeatMode.ALL -> tr("Repeat the playlist")
                    RepeatMode.ONE -> tr("Repeat the text")
                },
                onClick = viewModel::cycleRepeat,
                selected = p.repeat != RepeatMode.OFF,
                tint = if (p.repeat != RepeatMode.OFF) colors.primary else null,
                modifier = Modifier.testTag("repeat"),
            )
        }
    }
    val seek: @Composable (Modifier) -> Unit = { m ->
        var dragging by remember { mutableStateOf<Float?>(null) }
        Row(m, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(clockTime(state.elapsed), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            AppSlider(
                value = dragging ?: p.textProgress,
                onValueChange = { dragging = it },
                valueRange = 0f..1f,
                onValueChangeFinished = { dragging?.let(player::seek); dragging = null },
                modifier = Modifier.weight(1f).testTag("seek"),
            )
            Text(clockTime(state.total), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            SelectMenu(
                PlayerViewModel.SPEEDS,
                PlayerViewModel.SPEEDS.minByOrNull { kotlin.math.abs(it - state.speed) },
                onSelect = player::setSpeed,
                optionLabel = ::speedLabel,
                buttonModifier = Modifier.testTag("speed"),
            )
        }
    }
    if (wide) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
            buttons()
            seek(Modifier.weight(1f))
        }
    } else {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            buttons()
            seek(Modifier.fillMaxWidth())
        }
    }
}

/** "1×", "1.25×". */
internal fun speedLabel(speed: Float): String {
    val text = if (speed == speed.toInt().toFloat()) speed.toInt().toString() else speed.toString().trimEnd('0')
    return "$text×"
}

@Composable
private fun PlayPauseButton(state: PlayerUiState, viewModel: PlayerViewModel, size: IconButtonSize) {
    val p = state.player
    val complete = p.status == PlayerStatus.COMPLETE
    AppIconButton(
        when {
            complete -> AppIcons.Replay
            p.isPlaying -> AppIcons.Pause
            else -> AppIcons.PlayArrow
        },
        contentDescription = when {
            complete -> tr("Play again")
            p.isPlaying -> tr("Pause")
            else -> tr("Play")
        },
        onClick = viewModel.player::toggle,
        size = size,
        style = IconButtonStyle.Filled,
        loading = p.status == PlayerStatus.LOADING,
        modifier = Modifier.testTag("play-pause"),
    )
}

/**
 * Says the text cannot be played, why, and what can be done: download a voice (through the
 * language's downloads, [onDownloadVoice]), use the system's voice, or skip the text.
 */
@Composable
fun PlaybackProblemDialog(onDownloadVoice: (languageId: Long) -> Unit, viewModel: PlayerViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val p = state.player
    val problem = p.problem ?: return
    val player = viewModel.player
    ProblemDialog(
        problem,
        title = p.item?.title.orEmpty(),
        place = tr("Text {0} of {1}", p.queueIndex + 1, p.order.size),
        onDownload = {
            player.dismissProblem()
            when (problem) {
                is PlaybackProblem.NoVoice -> onDownloadVoice(problem.languageId)
            }
        },
        onSystemVoice = player::useSystemVoice,
        onSkip = player::skip,
        onDismiss = player::dismissProblem,
    )
}

@Composable
internal fun ProblemDialog(problem: PlaybackProblem, title: String, place: String, onDownload: () -> Unit, onSystemVoice: () -> Unit, onSkip: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    AppDialogSurface(onDismissRequest = onDismiss, maxWidth = DIALOG_WIDTH) {
        Box(Modifier.testTag("playback-problem")) {
            AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(AppSpacing.m))
            Column(Modifier.fillMaxWidth().padding(AppSpacing.xxl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Box(Modifier.size(WARNING_CIRCLE).clip(AppShapes.circle).background(AppAccents.orange.copy(alpha = AppAlpha.soft)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AppAccents.orange, modifier = Modifier.size(WARNING_ICON))
                }
                Text(tr("Can't play this text"), style = AppText.screenTitle, textAlign = TextAlign.Center)
                Text(problemMessage(problem), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                Row(
                    Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.surfaceContainerLow).padding(AppSpacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
                ) {
                    IconTile(AppIcons.Description, size = TILE)
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(place, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
                val download: @Composable (Modifier) -> Unit = { m -> PrimaryButton(tr("Download voice"), onClick = onDownload, modifier = m.testTag("download-voice")) }
                val system: @Composable (Modifier) -> Unit = { m -> SecondaryButton(tr("Use system voice"), onClick = onSystemVoice, modifier = m.testTag("system-voice")) }
                val skip: @Composable (Modifier) -> Unit = { m -> QuietButton(tr("Skip text"), onClick = onSkip, modifier = m.testTag("skip-text")) }
                if (compact) {
                    download(Modifier.fillMaxWidth())
                    system(Modifier.fillMaxWidth())
                    skip(Modifier.fillMaxWidth())
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), verticalAlignment = Alignment.CenterVertically) {
                        download(Modifier)
                        system(Modifier)
                        skip(Modifier.width(SKIP_WIDTH))
                    }
                }
            }
        }
    }
}

private const val TILE = 40
private val DIALOG_WIDTH = 640.dp
private val WARNING_CIRCLE = 88.dp
private val WARNING_ICON = 44.dp
private val SKIP_WIDTH = 120.dp
