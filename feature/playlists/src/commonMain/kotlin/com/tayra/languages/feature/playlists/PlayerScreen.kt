package com.tayra.languages.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.playlists.PlaybackProblem
import com.tayra.languages.core.domain.playlists.PlayerStatus
import com.tayra.languages.core.domain.playlists.PlaylistItem
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Crumb
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ProgressTrack
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.InfoBanner
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import org.koin.compose.viewmodel.koinViewModel

/**
 * What the playlist player is playing: the text with the sentence being heard and its translation,
 * the ways through the sentences and texts, and what comes next. On a phone the controls of the
 * player bar are on the screen itself.
 */
@Composable
fun PlayerScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: PlayerViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { AppTopBar(title = state.player.playlist?.name ?: tr("Player"), onNavigate = onNavigate, section = NavSection.PLAYLISTS, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        PlayerContent(state, viewModel, onNavigate, Modifier.padding(padding))
    }
}

@Composable
internal fun PlayerContent(state: PlayerUiState, viewModel: PlayerViewModel, onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    val p = state.player
    val playlist = p.playlist
    ScrollColumn(
        modifier.fillMaxSize(),
        contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        if (!p.isLoaded || playlist == null) {
            EmptyState(
                AppIcons.Headphones,
                tr("Nothing is playing"),
                message = tr("Play a playlist to hear its texts one after another."),
                action = { PrimaryButton(tr("Open playlists"), onClick = { onNavigate(Route.Playlists) }, icon = AppIcons.QueueMusic) },
            )
            return@ScrollColumn
        }
        val header: @Composable () -> Unit = {
            ScreenHeader(
                playlist.name,
                playlistFacts(playlist, state.duration),
                crumbs = if (compact) emptyList() else listOf(Crumb(tr("Playlists")) { onNavigate(Route.Playlists) }, Crumb(playlist.name) { onNavigate(Route.Playlist(playlist.id)) }),
            )
        }
        if (compact) {
            header()
            Box(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.l)) { NowPlaying(state, viewModel, onNavigate) }
            PlayerControls(state, viewModel, Modifier.fillMaxWidth())
            BackToText(state, onNavigate)
            Box(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.l)) { UpNext(state, viewModel, onNavigate) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                Column(Modifier.weight(1.6f).cardSurface(AppShapes.large).padding(AppSpacing.xxl), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    header()
                    Box(Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.xxl)) { NowPlaying(state, viewModel, onNavigate) }
                }
                Box(Modifier.weight(1f).cardSurface(AppShapes.large).padding(AppSpacing.xxl)) { UpNext(state, viewModel, onNavigate) }
            }
        }
    }
}

/** The text being heard and its sentence, or the end of the playlist. */
@Composable
private fun NowPlaying(state: PlayerUiState, viewModel: PlayerViewModel, onNavigate: (Route) -> Unit) {
    val p = state.player
    val colors = MaterialTheme.colorScheme
    val player = viewModel.player
    if (p.status == PlayerStatus.COMPLETE) {
        Complete(state, onPlayAgain = player::playAgain, onBack = { onNavigate(Route.Playlists) })
        return
    }
    val item = p.item
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = tr("Previous text"), onClick = player::previous, style = IconButtonStyle.Outlined, modifier = Modifier.testTag("previous-text"))
            Text(tr("{0} of {1}", p.queueIndex + 1, p.order.size), style = AppText.emphasis)
            AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = tr("Next text"), onClick = player::next, enabled = p.hasNext, style = IconButtonStyle.Outlined, modifier = Modifier.testTag("next-text"))
            if (p.pageCount > 1) Text(tr("Page {0} of {1}", p.page, p.pageCount), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.End)
        }
        Text(item?.title.orEmpty(), style = AppText.sectionTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val problem = p.problem
        if (p.status == PlayerStatus.BLOCKED && problem != null) {
            InfoBanner(problemMessage(problem), tint = AppAccents.orange, icon = Icons.Default.Warning)
        }
        val sentence = p.currentSentence
        if (sentence != null) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                AppIconButton(
                    if (p.isPlaying) AppIcons.Pause else AppIcons.VolumeUp,
                    contentDescription = if (p.isPlaying) tr("Pause") else tr("Play"),
                    onClick = player::toggle,
                    style = IconButtonStyle.Tonal,
                    size = IconButtonSize.Large,
                    loading = p.status == PlayerStatus.LOADING,
                    modifier = Modifier.testTag("sentence-play"),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    Text(spokenSentence(sentence, p.word), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("current-sentence"))
                    state.translation?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant, modifier = Modifier.testTag("translation")) }
                }
            }
        }
        if (p.sentences.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    Text(tr("Sentence {0} of {1}", p.sentence + 1, p.sentences.size), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    ProgressTrack((p.sentence + 1).toFloat() / p.sentences.size, colors.primary, Modifier.fillMaxWidth(), height = AppSizes.track)
                }
                AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = tr("Previous sentence"), onClick = player::previousSentence, style = IconButtonStyle.Outlined, modifier = Modifier.testTag("previous-sentence"))
                AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = tr("Next sentence"), onClick = player::nextSentence, style = IconButtonStyle.Outlined, modifier = Modifier.testTag("next-sentence"))
            }
        }
    }
}

/** The sentence with the word being spoken marked, when the voice tells which it is. */
@Composable
private fun spokenSentence(sentence: String, word: IntRange?): AnnotatedString {
    val mark = MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.soft)
    if (word == null || word.first < 0 || word.last >= sentence.length || word.isEmpty()) return AnnotatedString(sentence)
    return buildAnnotatedString {
        append(sentence.substring(0, word.first))
        withStyle(SpanStyle(background = mark)) { append(sentence.substring(word.first, word.last + 1)) }
        append(sentence.substring(word.last + 1))
    }
}

/** The end of the playlist: everything heard, and the ways on. */
@Composable
private fun Complete(state: PlayerUiState, onPlayAgain: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val playlist = state.player.playlist ?: return
    Column(Modifier.fillMaxWidth().padding(vertical = AppSpacing.xxl).testTag("playlist-complete"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Box(Modifier.size(DONE_CIRCLE).clip(AppShapes.circle).background(colors.primary.copy(alpha = AppAlpha.tint)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(DONE_ICON))
        }
        Text(tr("Playlist complete"), style = AppText.screenTitle, textAlign = TextAlign.Center)
        Text(
            "${tr("You listened to all {0}", textCount(playlist.items.size))} · ${listeningTime(state.duration)}",
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpacing.s))
        PrimaryButton(tr("Play again"), onClick = onPlayAgain, icon = AppIcons.Replay, modifier = Modifier.width(DONE_BUTTON).testTag("play-again"))
        SecondaryButton(tr("Back to playlists"), onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack, modifier = Modifier.width(DONE_BUTTON))
    }
}

/** The playlist's texts in the order they play, the current one marked, with a way to change them. */
@Composable
private fun UpNext(state: PlayerUiState, viewModel: PlayerViewModel, onNavigate: (Route) -> Unit) {
    val p = state.player
    val playlist = p.playlist ?: return
    val queue: List<PlaylistItem> = p.order.mapNotNull { id -> playlist.items.firstOrNull { it.id == id } }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Up next"), Modifier.weight(1f), style = AppText.headline)
            LinkText(tr("Edit queue"), onClick = { onNavigate(Route.Playlist(playlist.id)) }, modifier = Modifier.testTag("edit-queue"))
        }
        Spacer(Modifier.height(AppSpacing.m))
        // Shuffled, the queue's order is the player's own; dragging moves texts in the playlist's order, so only then.
        ReorderableColumn(queue, key = { it.id }, onMove = { from, to -> if (!p.shuffle) viewModel.move(queue[from].id, to) }) { item, index, handle, dragging ->
            QueueRow(
                item,
                index,
                current = item.id == p.itemId && p.status != PlayerStatus.COMPLETE,
                time = listeningTime(state.durations[item.id] ?: kotlin.time.Duration.ZERO),
                handle = if (p.shuffle) Modifier else handle,
                dragging = dragging,
                onPlay = { viewModel.player.jumpTo(item.id) },
                onRemove = { viewModel.remove(item.id) },
            )
            AppDivider()
        }
        Spacer(Modifier.height(AppSpacing.l))
        SecondaryButton(tr("Add texts"), onClick = { onNavigate(Route.AddToPlaylist(playlist.id)) }, icon = Icons.Default.Add, modifier = Modifier.fillMaxWidth().testTag("queue-add-texts"))
    }
}

@Composable
private fun QueueRow(item: PlaylistItem, index: Int, current: Boolean, time: String, handle: Modifier, dragging: Boolean, onPlay: () -> Unit, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth()
            .then(if (dragging) Modifier.cardSurface(AppShapes.control) else Modifier)
            .then(if (current) Modifier.background(colors.primary.copy(alpha = AppAlpha.tint), AppShapes.control) else Modifier)
            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.m).testTag("queue-${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Icon(AppIcons.DragIndicator, contentDescription = tr("Drag to reorder"), tint = colors.onSurfaceVariant, modifier = handle.size(AppSizes.icon))
        if (!LocalWindowWidth.current.isCompact) Text("${index + 1}", Modifier.width(NUMBER_WIDTH), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.End)
        Icon(AppIcons.Description, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconLarge))
        Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(time, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
        if (item.finished) {
            Box(Modifier.size(AppSizes.iconLarge).clip(AppShapes.circle).background(colors.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = tr("Heard to the end"), tint = AppAccents.onAccent, modifier = Modifier.size(AppSizes.iconSmall))
            }
        }
        OverflowMenu(tr("More actions"), buttonModifier = Modifier.testTag("queue-menu-${item.id}")) { close ->
            AppMenuItem(text = { Text(tr("Play from here")) }, onClick = { close(); onPlay() })
            AppMenuItem(text = { Text(tr("Remove from playlist"), color = colors.error) }, onClick = { close(); onRemove() })
        }
    }
}

/** Opens the text being heard in the reader, at its page. */
@Composable
internal fun BackToText(state: PlayerUiState, onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    val label = backToTextLabel(state) ?: return
    LinkText(
        label,
        onClick = { textRoute(state)?.let(onNavigate) },
        modifier = modifier.testTag("back-to-text"),
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** "Back to lesson", "Back to article" or "Back to book", by the kind of text being heard. */
@Composable
internal fun backToTextLabel(state: PlayerUiState): String? = when (state.player.item?.kind ?: return null) {
    com.tayra.languages.core.domain.playlists.PlaylistTextKind.LESSON -> tr("Back to lesson")
    com.tayra.languages.core.domain.playlists.PlaylistTextKind.NEWS -> tr("Back to article")
    com.tayra.languages.core.domain.playlists.PlaylistTextKind.BOOK -> tr("Back to book")
}

/** The text being heard in the reader, at its page. */
internal fun textRoute(state: PlayerUiState): Route? = state.player.item?.let { Route.Read(it.bookId, state.player.page) }

/** Why a text cannot be played, in a sentence. */
@Composable
internal fun problemMessage(problem: PlaybackProblem): String = when (problem) {
    is PlaybackProblem.NoVoice -> tr("The {0} voice used by this playlist is not available on this device.", tr(problem.languageName))
}

private val DONE_CIRCLE = 160.dp
private val DONE_ICON = 88.dp
private val DONE_BUTTON = 340.dp
private val NUMBER_WIDTH = 20.dp
