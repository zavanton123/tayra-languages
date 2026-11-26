package com.idiomiko.feature.playlists

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.playlists.PlayerStatus
import com.idiomiko.core.domain.playlists.Playlist
import com.idiomiko.core.domain.playlists.PlaylistItem
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.BookCover
import com.idiomiko.core.ui.components.EmptyState
import com.idiomiko.core.ui.components.IconButtonSize
import com.idiomiko.core.ui.components.IconButtonStyle
import com.idiomiko.core.ui.components.InfoBanner
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LoadingIndicator
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.OverflowMenu
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.state.CollectEvents
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppElevation
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlin.time.Duration
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * A playlist on a page of its own, as a phone or a window too narrow for the Playlists sidebar shows it: its covers
 * and progress, the way to go on, what is playing, and its texts to play from, move or take out.
 */
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = koinViewModel(key = "playlist-$playlistId") { parametersOf(playlistId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppTopBar(title = tr("Playlists"), onNavigate = onNavigate, section = NavSection.PLAYLISTS, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        val playlist = state.playlist
        when {
            state.loading -> LoadingIndicator(Modifier.padding(padding))
            playlist == null -> EmptyState(tr("This playlist no longer exists."), Modifier.padding(padding).padding(AppSpacing.xxl))
            else -> PlaylistPage(state, playlist, viewModel, onNavigate, onBack, Modifier.padding(padding))
        }
    }
}

/** A playlist's own page: the header, the buttons, what is playing and the texts as one card of rows. */
@Composable
internal fun PlaylistPage(
    state: PlaylistDetailUiState,
    playlist: Playlist,
    viewModel: PlaylistDetailViewModel,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    var deleting by remember { mutableStateOf(false) }
    CollectEvents(viewModel.events) { if (it is PlaylistDetailEvent.Deleted) onBack() }
    ScrollColumn(
        modifier.fillMaxSize(),
        contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.s else AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = AppSizes.readableWidth), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            if (!compact) LinkText(tr("Playlists"), onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack, style = AppText.labelStrong)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                CoverMosaic(playlist, PAGE_MOSAIC, AppShapes.card)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                    Text(playlist.name, style = AppText.headline, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (playlist.description.isNotBlank()) Text(playlist.description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(
                        progressFacts(playlist, state.duration, state.remaining),
                        Modifier.testTag("playlist-facts"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            state.error?.let { InfoBanner(it, tint = colors.error, icon = Icons.Default.Warning) }
            val empty = playlist.items.isEmpty()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                val main = mainAction(state, playlist)
                PrimaryButton(main.label, onClick = { onMain(state, viewModel) }, icon = main.icon, enabled = !empty, modifier = Modifier.weight(1f).testTag("play-all"))
                AppIconButton(
                    AppIcons.Shuffle,
                    contentDescription = tr("Shuffle"),
                    onClick = { viewModel.play(shuffle = true) },
                    enabled = !empty,
                    style = IconButtonStyle.Outlined,
                    modifier = Modifier.testTag("shuffle-play"),
                )
                PlaylistMenu(playlist, onNavigate, onDelete = { deleting = true }, withEdit = true, style = IconButtonStyle.Outlined)
            }
            if (state.isLoaded) NowPlayingStrip(state, compact = true, onOpen = { onNavigate(Route.Player) })
            if (empty) {
                EmptyPlaylist(onAdd = { onNavigate(Route.AddToPlaylist(playlist.id)) })
            } else {
                Column(Modifier.fillMaxWidth().cardSurface(AppShapes.large)) {
                    playlist.items.forEachIndexed { index, item ->
                        if (index > 0) AppDivider(color = colors.outlineVariant.copy(alpha = AppAlpha.half))
                        PageTextRow(state, item, index, playlist.items.size, viewModel, onNavigate)
                    }
                }
            }
        }
    }
    if (deleting) DeletePlaylistDialog(playlist, state.duration, onConfirm = { deleting = false; viewModel.delete() }, onDismiss = { deleting = false })
}

/** A text as a row of a playlist's own page: a tap plays from it, or holds it while it is the one playing. */
@Composable
private fun PageTextRow(state: PlaylistDetailUiState, item: PlaylistItem, index: Int, count: Int, viewModel: PlaylistDetailViewModel, onNavigate: (Route) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val current = item.id == state.currentId
    val status = state.statusOf(item)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth()
            .background(rowHighlight(current, hovered))
            .clickable(interaction, LocalIndication.current, onClickLabel = tr("Play")) { openInPlayer(state, item, viewModel, onNavigate) }
            .padding(start = AppSpacing.l, end = AppSpacing.xs, top = AppSpacing.s + AppSpacing.xxs, bottom = AppSpacing.s + AppSpacing.xxs)
            .testTag("item-${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        BookCover(item.title, PAGE_ROW_COVER, titled = false)
        Column(Modifier.weight(1f)) {
            Text(item.title, style = AppText.labelStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val label = statusLabel(status)
            if (status is ItemStatus.Playing || status is ItemStatus.Paused) {
                Text(label?.first.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.primary)
            } else {
                Text(
                    listOfNotNull(item.kind.label, listeningTime(state.durations[item.id] ?: Duration.ZERO), label?.first).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        ItemMenu(item, index, count, viewModel)
    }
}

/**
 * The playlist chosen on a wide window's Playlists page, beside the list: a header card with the covers, the facts,
 * the buttons and what is playing, then the texts to reorder and play from. An empty playlist's card reaches down to
 * fill [height], the room the pane has.
 */
@Composable
internal fun PlaylistPane(viewModel: PlaylistDetailViewModel, onNavigate: (Route) -> Unit, height: Dp = 0.dp) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playlist = state.playlist ?: return
    var deleting by remember(playlist.id) { mutableStateOf(false) }
    val density = LocalDensity.current
    var above by remember { mutableStateOf(0.dp) }
    Column(Modifier.fillMaxWidth().onSizeChanged { above = with(density) { it.height.toDp() } }, verticalArrangement = Arrangement.spacedBy(AppSpacing.pane)) {
        state.error?.let { InfoBanner(it, tint = MaterialTheme.colorScheme.error, icon = Icons.Default.Warning) }
        HeaderCard(state, playlist, viewModel, onNavigate, onDelete = { deleting = true })
    }
    if (playlist.items.isEmpty()) {
        EmptyPlaylist(onAdd = { onNavigate(Route.AddToPlaylist(playlist.id)) }, Modifier.heightIn(min = (height - above - AppSpacing.pane).coerceAtLeast(0.dp)))
    } else TextsCard(state, playlist, viewModel, onNavigate)
    if (deleting) DeletePlaylistDialog(playlist, state.duration, onConfirm = { deleting = false; viewModel.delete() }, onDismiss = { deleting = false })
}

@Composable
private fun HeaderCard(state: PlaylistDetailUiState, playlist: Playlist, viewModel: PlaylistDetailViewModel, onNavigate: (Route) -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val empty = playlist.items.isEmpty()
    Row(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xxl).testTag("playlist-header"),
        // The cover sits in the card's top corner, as far from its top as from its side.
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl),
    ) {
        CoverMosaic(playlist, HEADER_MOSAIC, AppShapes.large, Modifier.shadow(AppElevation.raised, AppShapes.large), titled = true)
        Column(Modifier.weight(1f)) {
            Text(playlist.name, style = AppText.screenTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (playlist.description.isNotBlank()) Text(playlist.description, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(AppSpacing.s))
            Text(headerFacts(state, playlist), Modifier.testTag("playlist-facts"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(AppSpacing.l))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                val main = mainAction(state, playlist)
                PrimaryButton(main.label, onClick = { onMain(state, viewModel) }, icon = main.icon, enabled = !empty, modifier = Modifier.testTag("play-all"))
                SecondaryButton(tr("Shuffle"), onClick = { viewModel.play(shuffle = true) }, icon = AppIcons.Shuffle, enabled = !empty, modifier = Modifier.testTag("shuffle-play"))
                SecondaryButton(tr("Edit"), onClick = { onNavigate(Route.EditPlaylist(playlist.id)) }, icon = Icons.Default.Edit, modifier = Modifier.testTag("edit-playlist"))
                PlaylistMenu(playlist, onNavigate, onDelete, withEdit = false, style = IconButtonStyle.Plain)
            }
            if (state.isLoaded) {
                Spacer(Modifier.height(AppSpacing.l))
                NowPlayingStrip(state, compact = false, onOpen = { onNavigate(Route.Player) })
            }
        }
    }
}

/** "2 of 5 played · 3 min left · 4 lessons, 1 book", the first part stronger. */
@Composable
private fun headerFacts(state: PlaylistDetailUiState, playlist: Playlist): AnnotatedString {
    if (playlist.items.isEmpty()) return AnnotatedString(textCount(0))
    val strong = AppText.labelStrong.toSpanStyle().copy(color = MaterialTheme.colorScheme.onSurface)
    val rest = listOf(if (playlist.isComplete) tr("✓ done") else tr("{0} left", listeningTime(state.remaining)), kindsSummary(playlist))
    return buildAnnotatedString {
        withStyle(strong) { append(trPlural(playlist.items.size, "{1} of {0} played", "{1} of {0} played", playlist.finishedCount)) }
        rest.forEach { append(" · "); append(it) }
    }
}

private class MainAction(val label: String, val icon: ImageVector)

/** The playlist's main button: hold it while playing, play it again once heard, go on with it, or start it. */
private fun mainAction(state: PlaylistDetailUiState, playlist: Playlist): MainAction = when {
    state.isPlaying -> MainAction(tr("Pause"), AppIcons.Pause)
    playlist.isComplete || (state.isLoaded && state.player.status == PlayerStatus.COMPLETE) -> MainAction(tr("Play again"), AppIcons.Replay)
    state.isLoaded || playlist.position != null || playlist.finishedCount > 0 -> MainAction(tr("Continue"), AppIcons.PlayArrow)
    else -> MainAction(tr("Play"), AppIcons.PlayArrow)
}

private fun onMain(state: PlaylistDetailUiState, viewModel: PlaylistDetailViewModel) = if (state.isPlaying) viewModel.pause() else viewModel.play()

/** Plays from [item]: on from where listening got to when it is the current text, else from its start. */
private fun playFrom(state: PlaylistDetailUiState, item: PlaylistItem, viewModel: PlaylistDetailViewModel) =
    if (item.id == state.currentId) viewModel.play() else viewModel.play(itemId = item.id)

/** The row of the text playing is tinted, a little more under the pointer; another row greys under it. */
@Composable
private fun rowHighlight(current: Boolean, hovered: Boolean): Color {
    val colors = MaterialTheme.colorScheme
    return when {
        current -> colors.primary.copy(alpha = if (hovered) AppAlpha.tint else AppAlpha.wash)
        hovered -> colors.onSurface.copy(alpha = AppAlpha.faint)
        else -> Color.Transparent
    }
}

/** A click on a text plays it, or lets it go on when it is the one playing, and opens the player on it. */
private fun openInPlayer(state: PlaylistDetailUiState, item: PlaylistItem, viewModel: PlaylistDetailViewModel, onNavigate: (Route) -> Unit) {
    if (!(item.id == state.currentId && state.isPlaying)) playFrom(state, item, viewModel)
    onNavigate(Route.Player)
}

/** The playlist's own menu: adding texts and deleting it, and changing its details where no Edit button shows. */
@Composable
private fun PlaylistMenu(playlist: Playlist, onNavigate: (Route) -> Unit, onDelete: () -> Unit, withEdit: Boolean, style: IconButtonStyle) {
    val colors = MaterialTheme.colorScheme
    OverflowMenu(tr("More actions"), buttonModifier = Modifier.testTag("playlist-actions"), icon = AppIcons.MoreHoriz, tint = colors.onSurfaceVariant, style = style) { close ->
        if (withEdit) AppMenuItem(text = { Text(tr("Edit details")) }, onClick = { close(); onNavigate(Route.EditPlaylist(playlist.id)) })
        AppMenuItem(text = { Text(tr("Add texts")) }, onClick = { close(); onNavigate(Route.AddToPlaylist(playlist.id)) })
        // The main playlist is always there: it cannot be deleted.
        if (!playlist.main) AppMenuItem(text = { Text(tr("Delete playlist"), color = colors.error) }, onClick = { close(); onDelete() }, modifier = Modifier.testTag("delete-playlist"))
    }
}

/**
 * What the player is reading out of this playlist: the sentence, its word marked as the player marks it, where it
 * is, and the way to the player.
 */
@Composable
private fun NowPlayingStrip(state: PlaylistDetailUiState, compact: Boolean, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val p = state.player
    val sentence = p.currentSentence?.let { spokenSentence(it, p.word) } ?: AnnotatedString(p.item?.title.orEmpty())
    val where = if (p.sentences.isNotEmpty()) tr("sentence {0} of {1}", p.sentence + 1, p.sentences.size) else null
    val shape = AppShapes.card
    val strip = Modifier.fillMaxWidth().clip(shape).background(colors.primary.copy(alpha = AppAlpha.wash))
        .border(AppBorders.hairline(colors.primary.copy(alpha = AppAlpha.soft)), shape).testTag("now-playing")
    if (compact) {
        Column(strip.clickable(onClick = onOpen).padding(horizontal = AppSpacing.m, vertical = AppSpacing.s + AppSpacing.xxs), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(sentence, Modifier.testTag("now-playing-sentence"), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(tr("Now playing"), where, tr("Open the player") + " ›").joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        Row(strip.padding(horizontal = AppSpacing.l, vertical = AppSpacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Equaliser(state.isPlaying)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                // One line each, however long the sentence: the strip keeps its height as listening goes on.
                Text(sentence, Modifier.testTag("now-playing-sentence"), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(tr("Now playing"), p.item?.title, where).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LinkText(
                tr("Open the player"),
                onClick = onOpen,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                style = AppText.labelStrong,
                modifier = Modifier.testTag("now-playing-open"),
            )
        }
    }
}

/** The texts in order, dragged by their handles to reorder them and played from by their play button or a double click. */
@Composable
private fun TextsCard(state: PlaylistDetailUiState, playlist: Playlist, viewModel: PlaylistDetailViewModel, onNavigate: (Route) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val line = colors.outlineVariant.copy(alpha = AppAlpha.half)
    Column(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(vertical = AppSpacing.s)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.m), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Texts"), Modifier.weight(1f), style = AppText.sectionTitle)
        }
        ReorderableColumn(playlist.items, key = { it.id }, onMove = { from, to -> viewModel.move(playlist.items[from].id, to) }) { item, index, handle, dragging ->
            AppDivider(color = line)
            TextRow(state, item, index, playlist.items.size, handle, dragging, viewModel, onNavigate)
        }
        AppDivider(color = line)
        LinkText(
            tr("Add texts from your library"),
            onClick = { onNavigate(Route.AddToPlaylist(playlist.id)) },
            icon = Icons.Default.Add,
            style = AppText.labelStrong,
            modifier = Modifier.padding(start = AppSpacing.xl, top = AppSpacing.l, bottom = AppSpacing.m).testTag("add-texts"),
        )
    }
}

@Composable
private fun TextRow(
    state: PlaylistDetailUiState,
    item: PlaylistItem,
    index: Int,
    count: Int,
    handle: Modifier,
    dragging: Boolean,
    viewModel: PlaylistDetailViewModel,
    onNavigate: (Route) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val current = item.id == state.currentId
    val holds = current && state.isPlaying
    val play by rememberUpdatedState { playFrom(state, item, viewModel) }
    val open by rememberUpdatedState { openInPlayer(state, item, viewModel, onNavigate) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth()
            .then(if (dragging) Modifier.cardSurface(AppShapes.control, colors.surface) else Modifier)
            .background(rowHighlight(current, hovered))
            .clickable(interaction, LocalIndication.current, onClickLabel = tr("Play")) { open() }
            .padding(start = AppSpacing.l, end = AppSpacing.l, top = AppSpacing.s + AppSpacing.xxs, bottom = AppSpacing.s + AppSpacing.xxs)
            .testTag("item-${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Icon(AppIcons.DragIndicator, contentDescription = tr("Drag to reorder"), tint = colors.outline, modifier = handle.padding(AppSpacing.xxs).size(AppSizes.iconSmall).testTag("drag-${item.id}"))
        val label = if (holds) tr("Pause") else tr("Play")
        Box(
            Modifier.size(NUMBER).clip(AppShapes.small).clickable { if (holds) viewModel.pause() else play() }.semantics { contentDescription = label }.testTag("play-item-${item.id}"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (holds) AppIcons.Pause else AppIcons.PlayArrow,
                contentDescription = null,
                tint = if (current) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(AppSizes.icon),
            )
        }
        BookCover(item.title, ROW_COVER, titled = false)
        Column(Modifier.weight(1f)) {
            Text(item.title, style = AppText.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(item.kind.label, state.courses[item.id]).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            listOfNotNull(listeningTime(state.durations[item.id] ?: Duration.ZERO), state.known[item.id]?.let { tr("{0}% known", it) }).joinToString(" · "),
            Modifier.width(META),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = 2,
        )
        val status = statusLabel(state.statusOf(item))
        Text(status?.first.orEmpty(), Modifier.width(STATUS).testTag("status-${item.id}"), style = AppText.badgeBold, color = status?.second ?: colors.onSurfaceVariant, maxLines = 1)
        ItemMenu(item, index, count, viewModel)
    }
}

/** What a row says of its text's place in the listening, and in which colour; nothing for a text not reached. */
@Composable
private fun statusLabel(status: ItemStatus): Pair<String, Color>? {
    val colors = MaterialTheme.colorScheme
    return when (status) {
        ItemStatus.Played -> tr("✓ Played") to AppAccents.green
        is ItemStatus.Playing -> (if (status.sentences > 0) tr("Playing · {0} of {1}", status.sentence, status.sentences) else tr("Now playing")) to colors.primary
        is ItemStatus.Paused -> (if (status.sentences > 0) tr("Paused · {0} of {1}", status.sentence, status.sentences) else tr("Now playing")) to colors.primary
        ItemStatus.UpNext -> tr("Up next") to colors.onSurfaceVariant
        ItemStatus.None -> null
    }
}

/** A text's own actions: moving it and taking it out. */
@Composable
private fun ItemMenu(item: PlaylistItem, index: Int, count: Int, viewModel: PlaylistDetailViewModel) {
    val colors = MaterialTheme.colorScheme
    OverflowMenu(tr("More actions"), buttonModifier = Modifier.testTag("item-menu-${item.id}"), icon = AppIcons.MoreHoriz, tint = colors.onSurfaceVariant) { close ->
        AppMenuItem(text = { Text(tr("Move up")) }, onClick = { close(); viewModel.move(item.id, index - 1) }, enabled = index > 0)
        AppMenuItem(text = { Text(tr("Move down")) }, onClick = { close(); viewModel.move(item.id, index + 1) }, enabled = index < count - 1)
        AppMenuItem(text = { Text(tr("Remove from playlist"), color = colors.error) }, onClick = { close(); viewModel.remove(item.id) })
    }
}

/** A playlist with no texts: what to do about it. */
@Composable
private fun EmptyPlaylist(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        tr("This playlist is empty"),
        modifier.testTag("empty-playlist"),
        message = tr("Add books, course lessons or individual texts to start listening."),
        action = {
            Spacer(Modifier.height(AppSpacing.s))
            PrimaryButton(tr("Add texts"), onClick = onAdd, icon = Icons.Default.Add, modifier = Modifier.testTag("add-texts"))
        },
    )
}

private val HEADER_MOSAIC = 132.dp
private val PAGE_MOSAIC = 96.dp
private val ROW_COVER = 40.dp
private val PAGE_ROW_COVER = 34.dp
private val NUMBER = 28.dp
private val META = 150.dp
private val STATUS = 130.dp
