package com.tayra.languages.feature.playlists

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.playlists.Playlist
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ProgressTrack
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.pressScale
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlin.math.roundToInt
import kotlin.time.Duration
import org.koin.compose.viewmodel.koinViewModel

internal val PlaylistsSort.label: String
    get() = when (this) {
        PlaylistsSort.RECENTLY_PLAYED -> tr("Recently played")
        PlaylistsSort.NAME -> tr("Name")
        PlaylistsSort.CREATED -> tr("Recently created")
    }

internal fun percent(fraction: Float) = "${(fraction * 100).roundToInt()}%"

/** "8 texts · ~34 min", or "0 texts" for an empty playlist. */
internal fun playlistFacts(playlist: Playlist, duration: Duration) =
    if (playlist.items.isEmpty()) textCount(0) else "${textCount(playlist.items.size)} · ${listeningTime(duration)}"

/** The playlists of the language being learned: the one to go on with, then all of them to search and sort. */
@Composable
fun PlaylistsScreen(onNavigate: (Route) -> Unit, viewModel: PlaylistsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppTopBar(title = tr("Playlists"), onNavigate = onNavigate, section = NavSection.PLAYLISTS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        PlaylistsContent(state, onNavigate, onSearch = viewModel::setSearch, onSort = viewModel::setSort, onPlay = viewModel::play, onDelete = viewModel::delete, modifier = Modifier.padding(padding))
    }
}

@Composable
internal fun PlaylistsContent(
    state: PlaylistsUiState,
    onNavigate: (Route) -> Unit,
    onSearch: (String) -> Unit,
    onSort: (PlaylistsSort) -> Unit,
    onPlay: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val width = LocalWindowWidth.current
    val compact = width.isCompact
    var deleting by remember { mutableStateOf<PlaylistSummary?>(null) }
    ScrollColumn(
        modifier.fillMaxSize(),
        contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        ScreenHeader(
            tr("Playlists"),
            tr("Listen to your texts, one after another."),
            actions = {
                if (state.playlists.isNotEmpty()) {
                    PrimaryButton(if (compact) tr("Create") else tr("New playlist"), onClick = { onNavigate(Route.NewPlaylist()) }, icon = Icons.Default.Add, modifier = Modifier.testTag("new-playlist"))
                }
            },
        )
        if (state.playlists.isEmpty()) {
            EmptyState(
                AppIcons.QueueMusic,
                tr("No playlists yet"),
                message = tr("Collect books, course lessons and news articles to listen to them one after another."),
                action = {
                    Spacer(Modifier.height(AppSpacing.s))
                    PrimaryButton(tr("Create playlist"), onClick = { onNavigate(Route.NewPlaylist()) }, icon = Icons.Default.Add, modifier = Modifier.testTag("new-playlist"))
                },
            )
            return@ScrollColumn
        }
        state.continueWith?.let { summary ->
            ContinueCard(summary, playing = state.playing && state.playingId == summary.playlist.id, compact = compact, onPlay = { onPlay(summary.playlist.id) }, onOpen = { onNavigate(Route.Playlist(summary.playlist.id)) })
        }
        if (compact) {
            Text(tr("Your playlists"), style = AppText.headline)
            SearchField(state.search, onSearch, tr("Search playlists"), Modifier.fillMaxWidth())
            SelectMenu(PlaylistsSort.entries, state.sort, onSort, { it.label }, buttonModifier = Modifier.testTag("playlist-sort"))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(tr("Your playlists"), Modifier.weight(1f), style = AppText.headline)
                SearchField(state.search, onSearch, tr("Search playlists"), Modifier.width(SEARCH_WIDTH))
                SelectMenu(PlaylistsSort.entries, state.sort, onSort, { it.label }, buttonModifier = Modifier.testTag("playlist-sort"))
            }
        }
        val shown = state.shown
        if (shown.isEmpty()) {
            EmptyState(AppIcons.QueueMusic, tr("No playlists match"), message = tr("Try another search."))
        } else {
            val columns = when {
                compact -> 1
                width.isExpanded -> 3
                else -> 2
            }
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                shown.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                        row.forEach { summary ->
                            PlaylistCard(
                                summary,
                                playing = state.playing && state.playingId == summary.playlist.id,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onOpen = { onNavigate(Route.Playlist(summary.playlist.id)) },
                                onPlay = { onPlay(summary.playlist.id) },
                                onEdit = { onNavigate(Route.EditPlaylist(summary.playlist.id)) },
                                onDelete = { deleting = summary },
                            )
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        AppDivider()
        Text(
            "${trPlural(state.playlists.size, "{0} playlist", "{0} playlists")} · ${textCount(state.textCount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
    deleting?.let { summary ->
        DeletePlaylistDialog(summary.playlist, summary.duration, onConfirm = { onDelete(summary.playlist.id); deleting = null }, onDismiss = { deleting = null })
    }
}

/** The playlist being listened to, with how far it has got and the way back into it. */
@Composable
private fun ContinueCard(summary: PlaylistSummary, playing: Boolean, compact: Boolean, onPlay: () -> Unit, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val playlist = summary.playlist
    val shape = AppShapes.large
    val card = Modifier.fillMaxWidth().clip(shape).background(colors.primary.copy(alpha = AppAlpha.wash))
        .border(AppBorders.hairline(colors.primary.copy(alpha = AppAlpha.soft)), shape).clickable(onClick = onOpen).testTag("continue-playlist")
    val details: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(tr("Continue listening").uppercase(), style = AppText.overline, color = colors.primary)
            Text(playlist.name, style = AppText.sectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            playlist.currentItem?.let { Text(it.title, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Spacer(Modifier.height(AppSpacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                Text(trPlural(playlist.items.size, "{1} of {0} text", "{1} of {0} texts", playlist.finishedCount), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, softWrap = false)
                ProgressTrack(playlist.progress, colors.primary, Modifier.weight(1f), height = AppSizes.trackThick)
                Text(tr("{0} left", listeningTime(summary.remaining)), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, softWrap = false)
            }
        }
    }
    val button: @Composable (Modifier) -> Unit = { m ->
        PrimaryButton(if (playing) tr("Pause") else tr("Continue"), onClick = onPlay, modifier = m.testTag("continue-play"), icon = if (playing) AppIcons.Pause else AppIcons.PlayArrow, size = ButtonSize.Large)
    }
    if (compact) {
        Column(card.padding(AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                CoverTile(playlist.cover, AppSizes.tileXLarge, icon = AppIcons.MenuBook)
                details(Modifier.weight(1f))
            }
            button(Modifier.fillMaxWidth())
        }
    } else {
        Row(card.padding(AppSpacing.xl), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
            CoverTile(playlist.cover, CONTINUE_TILE, icon = AppIcons.MenuBook)
            details(Modifier.weight(1f))
            button(Modifier)
        }
    }
}

/** A playlist as a card of the grid: its cover and menu, name, length and description, and how far listening has got. */
@Composable
private fun PlaylistCard(
    summary: PlaylistSummary,
    playing: Boolean,
    modifier: Modifier,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val playlist = summary.playlist
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier.pressScale(interaction).clip(AppShapes.large).background(colors.surface)
            .border(AppBorders.hairline(if (hovered) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), AppShapes.large)
            .hoverable(interaction).clickable(interaction, LocalIndication.current, onClick = onOpen).padding(AppSpacing.xl).testTag("playlist-${playlist.id}"),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CoverTile(playlist.cover, AppSizes.tileXLarge)
            Spacer(Modifier.weight(1f))
            OverflowMenu(tr("More actions"), buttonModifier = Modifier.testTag("playlist-menu-${playlist.id}")) { close ->
                if (playlist.items.isNotEmpty()) AppMenuItem(text = { Text(if (playing) tr("Pause") else tr("Play")) }, onClick = { close(); onPlay() })
                AppMenuItem(text = { Text(tr("Edit details")) }, onClick = { close(); onEdit() })
                AppMenuItem(text = { Text(tr("Delete playlist"), color = colors.error) }, onClick = { close(); onDelete() })
            }
        }
        Spacer(Modifier.height(AppSpacing.l))
        Text(playlist.name, style = AppText.itemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(playlistFacts(playlist, summary.duration), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        if (playlist.description.isNotBlank()) {
            Spacer(Modifier.height(AppSpacing.xs))
            Text(playlist.description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        // Cards in a row share the tallest one's height; the progress keeps to the bottom.
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(AppSpacing.l))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            ProgressTrack(playlist.progress, colors.primary, Modifier.weight(1f), height = AppSizes.track)
            Text(percent(playlist.progress), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            AppIconButton(
                if (playing) AppIcons.Pause else AppIcons.PlayArrow,
                contentDescription = if (playing) tr("Pause") else tr("Play {0}", playlist.name),
                onClick = onPlay,
                enabled = playlist.items.isNotEmpty(),
                size = IconButtonSize.Large,
                style = IconButtonStyle.Filled,
                modifier = Modifier.testTag("play-playlist-${playlist.id}"),
            )
        }
    }
}

/** Asks before deleting a playlist, saying its texts stay in the library. */
@Composable
internal fun DeletePlaylistDialog(playlist: Playlist, duration: Duration, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppDialogSurface(onDismissRequest = onDismiss, maxWidth = AppSizes.dialog) {
        Column(Modifier.padding(AppSpacing.xxl).testTag("delete-playlist-dialog"), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(tr("Delete playlist?"), Modifier.weight(1f), style = AppText.headline)
                AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss, size = IconButtonSize.Small)
            }
            Text(
                tr("{0} will be removed. The books, courses and texts inside it will stay in your library.", playlist.name),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
            Text(playlistFacts(playlist, duration), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            AppDivider(Modifier.padding(vertical = AppSpacing.xs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.End)) {
                SecondaryButton(tr("Cancel"), onClick = onDismiss)
                PrimaryButton(tr("Delete playlist"), onClick = onConfirm, tone = ButtonTone.Danger, modifier = Modifier.testTag("confirm-delete-playlist"))
            }
        }
    }
}

private val SEARCH_WIDTH = 360.dp
private val CONTINUE_TILE = 96.dp
