package com.tayra.languages.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.playlists.Playlist
import com.tayra.languages.core.domain.playlists.PlaylistItem
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.Crumb
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ProgressTrack
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.formatDate
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** A playlist: its cover and progress, its texts to reorder, play from or take out, and ways to change or delete it. */
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = koinViewModel(key = "playlist-$playlistId") { parametersOf(playlistId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppTopBar(title = state.playlist?.name ?: tr("Playlist"), onNavigate = onNavigate, section = NavSection.PLAYLISTS, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        val playlist = state.playlist
        when {
            state.loading -> LoadingIndicator(Modifier.padding(padding))
            playlist == null -> EmptyState(AppIcons.QueueMusic, tr("This playlist no longer exists."), Modifier.padding(padding).padding(AppSpacing.xxl))
            else -> PlaylistDetailContent(state, playlist, viewModel, onNavigate, onBack, Modifier.padding(padding))
        }
    }
}

@Composable
internal fun PlaylistDetailContent(
    state: PlaylistDetailUiState,
    playlist: Playlist,
    viewModel: PlaylistDetailViewModel,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = LocalWindowWidth.current.isCompact
    var deleting by remember { mutableStateOf(false) }
    CollectEvents(viewModel.events) { if (it is PlaylistDetailEvent.Deleted) onBack() }
    val playing = state.isLoaded && state.player.isPlaying
    val empty = playlist.items.isEmpty()
    val playLabel = when {
        playing -> tr("Pause")
        state.isLoaded || playlist.position != null -> tr("Continue")
        else -> tr("Play all")
    }
    val onPlay = { if (playing) viewModel.pause() else viewModel.play() }
    ScrollColumn(
        modifier.fillMaxSize(),
        contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        ScreenHeader(
            playlist.name,
            playlistFacts(playlist, state.duration),
            crumbs = if (compact) emptyList() else listOf(Crumb(tr("Playlists"), onBack)),
            actions = {
                if (!compact) {
                    SecondaryButton(tr("Edit details"), onClick = { onNavigate(Route.EditPlaylist(playlist.id)) }, icon = Icons.Default.Edit, modifier = Modifier.testTag("edit-playlist"))
                    PrimaryButton(playLabel, onClick = onPlay, icon = if (playing) AppIcons.Pause else AppIcons.PlayArrow, enabled = !empty, modifier = Modifier.testTag("play-all"))
                }
                OverflowMenu(tr("More actions"), buttonModifier = Modifier.testTag("playlist-actions")) { close ->
                    if (compact) AppMenuItem(text = { Text(tr("Edit details")) }, onClick = { close(); onNavigate(Route.EditPlaylist(playlist.id)) })
                    AppMenuItem(text = { Text(tr("Shuffle play")) }, onClick = { close(); viewModel.play(shuffle = true) }, enabled = !empty)
                    AppMenuItem(text = { Text(tr("Add texts")) }, onClick = { close(); onNavigate(Route.AddToPlaylist(playlist.id)) })
                    AppMenuItem(text = { Text(tr("Delete playlist"), color = MaterialTheme.colorScheme.error) }, onClick = { close(); deleting = true })
                }
            },
        )
        state.error?.let { InfoBanner(it, tint = MaterialTheme.colorScheme.error, icon = Icons.Default.Warning) }
        if (compact) {
            PrimaryButton(playLabel, onClick = onPlay, icon = if (playing) AppIcons.Pause else AppIcons.PlayArrow, enabled = !empty, modifier = Modifier.fillMaxWidth().testTag("play-all"))
        }
        if (empty) {
            EmptyPlaylist(onAdd = { onNavigate(Route.AddToPlaylist(playlist.id)) }, onBack = onBack)
            return@ScrollColumn
        }
        val summary: @Composable (Modifier) -> Unit = { m -> Summary(playlist, compact, m) { viewModel.play(shuffle = true) } }
        val texts: @Composable (Modifier) -> Unit = { m -> Texts(state, playlist, viewModel, onNavigate, m) }
        if (compact) {
            Box(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.l)) { summary(Modifier.fillMaxWidth()) }
            Box(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.l)) { texts(Modifier.fillMaxWidth()) }
            QuietButton(tr("Delete playlist"), onClick = { deleting = true }, icon = Icons.Default.Delete, tone = ButtonTone.Danger, modifier = Modifier.testTag("delete-playlist"))
        } else {
            Column(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xxl)) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
                    summary(Modifier.width(SUMMARY_WIDTH))
                    AppVerticalDivider(Modifier.fillMaxHeight())
                    texts(Modifier.weight(1f))
                }
                AppDivider(Modifier.padding(vertical = AppSpacing.l))
                QuietButton(tr("Delete playlist"), onClick = { deleting = true }, icon = Icons.Default.Delete, tone = ButtonTone.Danger, modifier = Modifier.testTag("delete-playlist"))
            }
        }
    }
    if (deleting) DeletePlaylistDialog(playlist, state.duration, onConfirm = { deleting = false; viewModel.delete() }, onDismiss = { deleting = false })
}

/** The cover, the description, how far listening has got and when the playlist was made. */
@Composable
private fun Summary(playlist: Playlist, compact: Boolean, modifier: Modifier, onShuffle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        val tint = coverColor(playlist.cover)
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                CoverTile(playlist.cover, AppSizes.tileXLarge)
                if (playlist.description.isNotBlank()) Text(playlist.description, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Box(Modifier.fillMaxWidth().height(COVER_HEIGHT).clip(AppShapes.large).background(tint.copy(alpha = AppAlpha.soft)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Headphones, contentDescription = null, tint = AppAccents.onWash(tint), modifier = Modifier.size(COVER_ICON))
            }
            if (playlist.description.isNotBlank()) Text(playlist.description, style = MaterialTheme.typography.bodyLarge)
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Row {
                Text(trPlural(playlist.items.size, "{1} of {0} played", "{1} of {0} played", playlist.finishedCount), Modifier.weight(1f), style = AppText.label)
                Text(percent(playlist.progress), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            ProgressTrack(playlist.progress, colors.primary, Modifier.fillMaxWidth(), height = AppSizes.trackThick)
        }
        AppDivider()
        Text(tr("Created {0}", playlist.createdAt.formatDate()), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        SecondaryButton(tr("Shuffle play"), onClick = onShuffle, icon = AppIcons.Shuffle, modifier = Modifier.fillMaxWidth().testTag("shuffle-play"))
    }
}

/** The texts in order, dragged by their handles to reorder them. */
@Composable
private fun Texts(state: PlaylistDetailUiState, playlist: Playlist, viewModel: PlaylistDetailViewModel, onNavigate: (Route) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val current = if (state.isLoaded) state.player.itemId else playlist.position?.itemId
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Texts"), style = AppText.sectionTitle)
                Text(tr("Drag to reorder."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            SecondaryButton(tr("Add texts"), onClick = { onNavigate(Route.AddToPlaylist(playlist.id)) }, icon = Icons.Default.Add, modifier = Modifier.testTag("add-texts"))
        }
        Spacer(Modifier.height(AppSpacing.m))
        AppDivider()
        ReorderableColumn(playlist.items, key = { it.id }, onMove = { from, to -> viewModel.move(playlist.items[from].id, to) }) { item, index, handle, dragging ->
            ItemRow(
                item,
                index,
                playlist.items.size,
                current = item.id == current,
                playing = item.id == current && state.isLoaded && state.player.isPlaying,
                time = listeningTime(state.durations[item.id] ?: kotlin.time.Duration.ZERO),
                handle = handle,
                dragging = dragging,
                onPlay = { viewModel.play(itemId = item.id) },
                onPause = viewModel::pause,
                onRead = { onNavigate(Route.Read(item.bookId)) },
                onMove = { to -> viewModel.move(item.id, to) },
                onRemove = { viewModel.remove(item.id) },
            )
            AppDivider()
        }
    }
}

@Composable
internal fun ItemRow(
    item: PlaylistItem,
    index: Int,
    count: Int,
    current: Boolean,
    /** Whether this is the text the player is reading out now. */
    playing: Boolean,
    time: String,
    handle: Modifier,
    dragging: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onRead: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    Row(
        Modifier.fillMaxWidth()
            .then(if (dragging) Modifier.cardSurface(AppShapes.control, colors.surface) else Modifier)
            .then(if (current) Modifier.background(colors.primary.copy(alpha = AppAlpha.wash), AppShapes.control) else Modifier)
            .padding(vertical = AppSpacing.s).testTag("item-${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Icon(AppIcons.DragIndicator, contentDescription = tr("Drag to reorder"), tint = colors.onSurfaceVariant, modifier = handle.padding(AppSpacing.xxs).size(AppSizes.icon).testTag("drag-${item.id}"))
        // A phone leaves the numbers out, so the titles have the room.
        if (!compact) Text("${index + 1}", Modifier.width(NUMBER_WIDTH), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.End)
        IconTile(item.kind.icon, size = TILE)
        if (compact) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    Text("${item.kind.label} · $time", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    if (current) Tag(tr("Current"), size = TagSize.Small)
                    if (item.finished) Icon(AppIcons.DoneAll, contentDescription = tr("Heard to the end"), tint = AppAccents.green, modifier = Modifier.size(AppSizes.iconSmall))
                }
            }
        } else {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                Text(item.title, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (current) Tag(tr("Current"), size = TagSize.Small)
            }
            Text("${item.kind.label} · $time", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
            // The tick has a place on every row, so the lengths line up.
            Box(Modifier.size(AppSizes.icon)) {
                if (item.finished) Icon(AppIcons.DoneAll, contentDescription = tr("Heard to the end"), tint = AppAccents.green, modifier = Modifier.size(AppSizes.icon))
            }
        }
        AppIconButton(
            if (playing) AppIcons.Pause else AppIcons.PlayArrow,
            contentDescription = if (playing) tr("Pause") else tr("Play"),
            onClick = if (playing) onPause else onPlay,
            tint = colors.primary,
            modifier = Modifier.testTag("play-item-${item.id}"),
        )
        OverflowMenu(tr("More actions"), buttonModifier = Modifier.testTag("item-menu-${item.id}")) { close ->
            AppMenuItem(text = { Text(tr("Open in the reader")) }, onClick = { close(); onRead() })
            AppMenuItem(text = { Text(tr("Move up")) }, onClick = { close(); onMove(index - 1) }, enabled = index > 0)
            AppMenuItem(text = { Text(tr("Move down")) }, onClick = { close(); onMove(index + 1) }, enabled = index < count - 1)
            AppMenuItem(text = { Text(tr("Remove from playlist"), color = colors.error) }, onClick = { close(); onRemove() })
        }
    }
}

/** A playlist with no texts: what to do about it, and a tip on adding from the reader. */
@Composable
private fun EmptyPlaylist(onAdd: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
        Column(
            Modifier.fillMaxWidth().clip(AppShapes.large).background(colors.primary.copy(alpha = AppAlpha.wash))
                .border(AppBorders.hairline(colors.primary.copy(alpha = AppAlpha.tint)), AppShapes.large)
                .padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.huge).testTag("empty-playlist"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            Box(Modifier.size(EMPTY_CIRCLE).clip(AppShapes.circle).background(colors.primary.copy(alpha = AppAlpha.tint)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.PlaylistAdd, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconHuge))
            }
            Text(tr("This playlist is empty"), style = AppText.headline, textAlign = TextAlign.Center)
            Text(tr("Add books, course lessons or individual texts to start listening."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(AppSpacing.xs))
            PrimaryButton(tr("Add texts"), onClick = onAdd, icon = Icons.Default.Add, modifier = Modifier.testTag("add-texts"))
            LinkText(tr("Back to playlists"), onClick = onBack)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Icon(AppIcons.Lightbulb, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.icon))
            Text(tr("You can also add the current text from the reader."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

private const val TILE = 40
private val SUMMARY_WIDTH = 360.dp
private val COVER_HEIGHT = 220.dp
private val COVER_ICON = 96.dp
private val NUMBER_WIDTH = 24.dp
private val EMPTY_CIRCLE = 112.dp
