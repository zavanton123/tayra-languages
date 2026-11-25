package com.idiomiko.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.playlists.Playlist
import com.idiomiko.core.ui.components.AppDialogSurface
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenu
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.ButtonTone
import com.idiomiko.core.ui.components.EmptyState
import com.idiomiko.core.ui.components.IconButtonSize
import com.idiomiko.core.ui.components.IconButtonStyle
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LoadingIndicator
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ProgressTrack
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.SearchField
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlin.time.Duration
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

internal val PlaylistsSort.label: String
    get() = when (this) {
        PlaylistsSort.RECENTLY_PLAYED -> tr("Recently played")
        PlaylistsSort.NAME -> tr("Name")
        PlaylistsSort.CREATED -> tr("Recently created")
    }

/** "8 texts · 34 min", or "0 texts" for an empty playlist. */
internal fun playlistFacts(playlist: Playlist, duration: Duration) =
    if (playlist.items.isEmpty()) trackCount(0) else "${trackCount(playlist.items.size)} · ${listeningTime(duration)}"

/** The sidebar's line under a playlist's name: its length, with "✓ done" once every text was heard. */
private fun listFacts(summary: PlaylistSummary): String =
    playlistFacts(summary.playlist, summary.duration) + if (summary.playlist.isComplete) " · " + tr("✓ done") else ""

/** How far listening has got: "2 of 5 played · 3 min left", "1 track · ✓ done", or the length before it starts. */
internal fun progressFacts(playlist: Playlist, duration: Duration, remaining: Duration): String = when {
    playlist.isComplete -> trackCount(playlist.items.size) + " · " + tr("✓ done")
    playlist.finishedCount > 0 || playlist.position != null ->
        trPlural(playlist.items.size, "{1} of {0} played", "{1} of {0} played", playlist.finishedCount) + " · " + tr("{0} left", listeningTime(remaining))
    else -> playlistFacts(playlist, duration)
}

/**
 * The playlists of the language being learned. A wide window shows them in a sidebar beside the one chosen
 * ([selectId] when the route names one); narrower ones list them, each opening on its own page.
 */
@Composable
fun PlaylistsScreen(
    onNavigate: (Route) -> Unit,
    selectId: Long? = null,
    viewModel: PlaylistsViewModel = koinViewModel(),
    detail: @Composable (Long) -> PlaylistDetailViewModel = { id -> koinViewModel(key = "playlist-$id") { parametersOf(id) } },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val width = LocalWindowWidth.current
    var creating by remember { mutableStateOf(false) }
    LaunchedEffect(selectId) { selectId?.let(viewModel::select) }
    Scaffold(
        topBar = {
            AppTopBar(title = tr("Playlists"), onNavigate = onNavigate, section = NavSection.PLAYLISTS)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) LoadingIndicator(Modifier.padding(padding))
        else PlaylistsContent(state, viewModel, onNavigate, onCreate = { creating = true }, detail, Modifier.padding(padding))
    }
    // A new playlist opens to be filled: beside the list on a wide window, on its own page otherwise.
    if (creating) {
        NewPlaylistDialog(bookId = null, onCreated = { id, _ ->
            creating = false
            viewModel.select(id)
            if (!width.isExpanded) onNavigate(Route.Playlist(id))
        }, onDismiss = { creating = false })
    }
}

@Composable
internal fun PlaylistsContent(
    state: PlaylistsUiState,
    viewModel: PlaylistsViewModel,
    onNavigate: (Route) -> Unit,
    onCreate: () -> Unit,
    detail: @Composable (Long) -> PlaylistDetailViewModel,
    modifier: Modifier = Modifier,
) {
    val width = LocalWindowWidth.current
    when {
        state.playlists.isEmpty() -> NoPlaylists(onCreate, modifier)
        width.isExpanded -> Row(modifier.fillMaxSize()) {
            Sidebar(state, viewModel, onCreate)
            val selected = state.selected
            ScrollColumn(
                Modifier.weight(1f).fillMaxHeight(),
                contentModifier = Modifier.padding(start = PANE_GAP, end = PANE_GAP, top = PANE_GAP, bottom = PANE_GAP),
                verticalArrangement = Arrangement.spacedBy(PANE_GAP),
            ) {
                if (selected != null) PlaylistPane(detail(selected.playlist.id), onNavigate)
            }
        }
        else -> PlaylistList(state, viewModel, onNavigate, onCreate, modifier)
    }
}

/** No playlist yet: what one is for, and the way to make the first. */
@Composable
private fun NoPlaylists(onCreate: () -> Unit, modifier: Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    ScrollColumn(
        modifier.fillMaxSize(),
        contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = AppSizes.readableWidth), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
            ScreenHeader(tr("Playlists"), subtitle = null)
            EmptyState(
                tr("No playlists yet"),
                modifier = Modifier.testTag("no-playlists"),
                message = tr("Collect books, course lessons and news articles to listen to them one after another."),
                action = {
                    Spacer(Modifier.height(AppSpacing.s))
                    PrimaryButton(tr("Create playlist"), onClick = onCreate, icon = Icons.Default.Add, modifier = Modifier.testTag("new-playlist"))
                },
            )
        }
    }
}

/** A wide window's list of playlists, as a card down the left: the search, the order, and a row for each. */
@Composable
private fun Sidebar(state: PlaylistsUiState, viewModel: PlaylistsViewModel, onCreate: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val selectedId = state.selected?.playlist?.id
    Box(
        // The card scrolls inside itself, so no scrollbar needs room beside it.
        Modifier.width(SIDEBAR + PANE_GAP).fillMaxHeight()
            .padding(start = PANE_GAP, top = PANE_GAP, bottom = PANE_GAP)
            .testTag("sidebar"),
    ) {
        Column(Modifier.fillMaxSize().cardSurface(AppShapes.large).padding(vertical = AppSpacing.l)) {
            Row(Modifier.fillMaxWidth().padding(start = AppSpacing.l, end = AppSpacing.s), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Playlists"), Modifier.weight(1f), style = AppText.itemTitleBold)
                LinkText(tr("New playlist"), onClick = onCreate, icon = Icons.Default.Add, style = AppText.labelStrong, modifier = Modifier.testTag("new-playlist"))
            }
            Spacer(Modifier.height(AppSpacing.m))
            SearchField(state.search, viewModel::setSearch, tr("Search playlists"), Modifier.fillMaxWidth().padding(horizontal = AppSpacing.m))
            Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs), horizontalArrangement = Arrangement.End) {
                SortMenu(state.sort, viewModel::setSort)
            }
            val shown = state.shown
            ScrollColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentModifier = Modifier.padding(start = AppSpacing.s, end = AppSpacing.s + AppSizes.scrollbarRoom),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                if (shown.isEmpty()) {
                    Text(
                        tr("No playlists match"),
                        Modifier.fillMaxWidth().padding(AppSpacing.l),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                shown.forEach { summary ->
                    SidebarRow(
                        summary,
                        selected = summary.playlist.id == selectedId,
                        loaded = state.playingId == summary.playlist.id,
                        playing = state.isPlaying(summary.playlist.id),
                        onClick = { viewModel.select(summary.playlist.id) },
                    )
                }
            }
        }
    }
}

/** A playlist in the sidebar: its covers, name, length and progress, and the bars while it is the one playing. */
@Composable
private fun SidebarRow(summary: PlaylistSummary, selected: Boolean, loaded: Boolean, playing: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val playlist = summary.playlist
    val shape = AppShapes.card
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .then(if (selected) Modifier.background(colors.primary.copy(alpha = AppAlpha.wash)).border(AppBorders.selected(colors.primary.copy(alpha = AppAlpha.line)), shape) else Modifier)
            .clickable(onClick = onClick).semantics { this.selected = selected }
            .padding(horizontal = AppSpacing.s, vertical = AppSpacing.s + AppSpacing.xxs).testTag("playlist-${playlist.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        CoverMosaic(playlist, SIDEBAR_MOSAIC, AppShapes.control)
        Column(Modifier.weight(1f)) {
            Text(playlist.name, style = AppText.labelBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listFacts(summary), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(AppSpacing.s))
            ProgressTrack(playlist.progress, playlistProgressColor(playlist), height = AppSizes.trackThin)
        }
        Box(Modifier.size(AppSizes.iconSmall)) {
            if (loaded) Equaliser(playing, Modifier.testTag("playing-${playlist.id}"))
        }
    }
}

/** The order of the sidebar's playlists, as a small link that opens the choices. */
@Composable
private fun SortMenu(sort: PlaylistsSort, onSort: (PlaylistsSort) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(AppShapes.small).clickable { open = true }.padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs).testTag("playlist-sort"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Icon(AppIcons.SwapVert, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconSmall))
            Text(sort.label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.iconSmall))
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            PlaylistsSort.entries.forEach { option ->
                AppMenuItem(
                    text = { Text(option.label, color = if (option == sort) colors.primary else colors.onSurface) },
                    onClick = { open = false; onSort(option) },
                )
            }
        }
    }
}

/**
 * The playlists as a phone lists them, and a window too narrow for the sidebar: a card for each, played or held
 * from its own button, then a row to make another.
 */
@Composable
private fun PlaylistList(state: PlaylistsUiState, viewModel: PlaylistsViewModel, onNavigate: (Route) -> Unit, onCreate: () -> Unit, modifier: Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    ScrollColumn(
        modifier.fillMaxSize(),
        contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.s else AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = AppSizes.readableWidth), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            // The New playlist row under the list makes a playlist, so the header needs no button of its own.
            if (!compact) ScreenHeader(tr("Playlists"), subtitle = null)
            // A long list gets a search; a few playlists are seen at a glance.
            if (state.playlists.size > SEARCH_FROM) SearchField(state.search, viewModel::setSearch, tr("Search playlists"), Modifier.fillMaxWidth())
            state.shown.forEach { summary ->
                PlaylistCard(
                    summary,
                    playing = state.isPlaying(summary.playlist.id),
                    onOpen = { viewModel.select(summary.playlist.id); onNavigate(Route.Playlist(summary.playlist.id)) },
                    onPlay = { viewModel.play(summary.playlist.id) },
                )
            }
            NewPlaylistRow(onCreate)
        }
    }
}

/** A playlist as a phone's card: its covers, name, progress, and a button that plays, holds or replays it where it is. */
@Composable
private fun PlaylistCard(summary: PlaylistSummary, playing: Boolean, onOpen: () -> Unit, onPlay: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val playlist = summary.playlist
    Row(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large).clickable(onClick = onOpen).padding(AppSpacing.m).testTag("playlist-${playlist.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        CoverMosaic(playlist, PHONE_MOSAIC, AppShapes.panel)
        Column(Modifier.weight(1f)) {
            Text(playlist.name, style = AppText.itemTitleBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                progressFacts(playlist, summary.duration, summary.remaining),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(AppSpacing.s))
            ProgressTrack(playlist.progress, playlistProgressColor(playlist), height = AppSizes.trackThin)
        }
        val replay = playlist.isComplete && !playing
        AppIconButton(
            when {
                playing -> AppIcons.Pause
                replay -> AppIcons.Replay
                else -> AppIcons.PlayArrow
            },
            contentDescription = when {
                playing -> tr("Pause")
                replay -> tr("Play again")
                else -> tr("Play {0}", playlist.name)
            },
            onClick = onPlay,
            enabled = playlist.items.isNotEmpty(),
            size = IconButtonSize.Large,
            style = if (replay) IconButtonStyle.Tonal else IconButtonStyle.Filled,
            modifier = Modifier.testTag("play-playlist-${playlist.id}"),
        )
    }
}

/** The dashed row under the list that makes another playlist. */
@Composable
private fun NewPlaylistRow(onCreate: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val line = colors.outlineVariant
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.large)
            .drawBehind {
                val stroke = DASH_WIDTH.toPx()
                drawRoundRect(
                    line,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(DASH_CORNER.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH.toPx(), DASH.toPx()))),
                )
            }
            .clickable(onClick = onCreate).padding(vertical = AppSpacing.l).testTag("new-playlist-row"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.icon))
        Spacer(Modifier.width(AppSpacing.s))
        Text(tr("New playlist"), style = AppText.labelBold, color = colors.primary)
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

/** The width of a wide window's list of playlists, as wide as the Courses page's sidebar. */
private val SIDEBAR = 300.dp

/** The one gap a wide window keeps around and between its cards: at the window's edges, between the panes, between the cards. */
private val PANE_GAP = AppSpacing.xl
private val SIDEBAR_MOSAIC = 48.dp
private val PHONE_MOSAIC = 56.dp
private val DASH = 6.dp
private val DASH_WIDTH = 2.dp
private val DASH_CORNER = 18.dp

/** From this many playlists a phone's list gets a search. */
private const val SEARCH_FROM = 6
