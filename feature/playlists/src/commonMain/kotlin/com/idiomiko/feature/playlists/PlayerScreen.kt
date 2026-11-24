package com.idiomiko.feature.playlists

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import com.idiomiko.core.domain.playlists.PlaylistPlayer
import com.idiomiko.core.ui.components.AppSlider
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.idiomiko.core.ui.components.AppBottomSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idiomiko.core.domain.playlists.PlaybackProblem
import com.idiomiko.core.domain.playlists.PlayerStatus
import com.idiomiko.core.domain.playlists.PlaylistItem
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.EmptyState
import com.idiomiko.core.ui.components.IconButtonSize
import com.idiomiko.core.ui.components.IconButtonStyle
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.OverflowMenu
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ScreenHeader
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.InfoBanner
import com.idiomiko.core.ui.components.cardSurface
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
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
        topBar = {
            AppTopBar(
                title = state.player.playlist?.name ?: tr("Player"),
                onNavigate = onNavigate,
                section = NavSection.PLAYLISTS,
                onBack = onBack,
                // On a phone the way back to the text sits in the bar, leaving the screen to the player.
                actions = { if (LocalWindowWidth.current.isCompact) BackToTextButton(state, onNavigate) },
            )
        },
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
    BoxWithConstraints(modifier.fillMaxSize()) {
        // The text being heard takes the window's height, so its sentence sits in the middle.
        val fillHeight = (maxHeight - AppSpacing.xxl * 2).coerceAtLeast(MIN_NOW_PLAYING_HEIGHT)
        val phoneHeight = (maxHeight - AppSpacing.l * 2).coerceAtLeast(MIN_PHONE_PLAYER_HEIGHT)
        var queueOpen by rememberSaveable { mutableStateOf(false) }
        ScrollColumn(
            Modifier.fillMaxSize(),
            contentModifier = Modifier.padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
        ) {
            if (!p.isLoaded || playlist == null) {
                EmptyState(
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
                )
            }
            if (compact) {
                // A phone shows the player alone; what comes next opens in a sheet over it.
                Column(Modifier.fillMaxWidth().height(phoneHeight), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                    header()
                    Box(Modifier.fillMaxWidth().weight(1f).cardSurface(AppShapes.large).padding(AppSpacing.l).testTag("now-playing-card")) {
                        NowPlaying(state, viewModel, onNavigate, fill = true)
                    }
                    // The card shows the place in the text; the queue opens from its icon at the row's start.
                    PlayerControls(state, viewModel, Modifier.fillMaxWidth(), showSeek = false) {
                        AppIconButton(AppIcons.QueueMusic, contentDescription = tr("Open the queue"), onClick = { queueOpen = true }, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("open-queue"))
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                    Column(Modifier.weight(1.6f).height(fillHeight).cardSurface(AppShapes.large).padding(AppSpacing.xxl), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                        header()
                        Box(Modifier.fillMaxWidth().weight(1f).cardSurface(AppShapes.large).padding(AppSpacing.xxl).testTag("now-playing-card")) {
                            NowPlaying(state, viewModel, onNavigate, fill = true)
                        }
                    }
                    Box(Modifier.weight(1f).heightIn(min = fillHeight).cardSurface(AppShapes.large).padding(vertical = AppSpacing.xxl)) { UpNext(state, viewModel, onNavigate, inset = AppSpacing.xxl) }
                }
            }
        }
        if (queueOpen && compact && p.isLoaded && playlist != null) QueueSheet(state, viewModel, onNavigate, onDismiss = { queueOpen = false })
    }
}

/** Below these the card would squeeze its sentence; the page scrolls instead. */
private val MIN_NOW_PLAYING_HEIGHT = 520.dp
private val MIN_PHONE_PLAYER_HEIGHT = 560.dp

/** The queue on a phone, as a sheet that stops half-way and pulls up to the full height. */
@Composable
private fun QueueSheet(state: PlayerUiState, viewModel: PlayerViewModel, onNavigate: (Route) -> Unit, onDismiss: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismiss, skipPartiallyExpanded = false, modifier = Modifier.testTag("queue-sheet")) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = AppSpacing.xl)) {
            UpNext(state, viewModel, onNavigate = { onDismiss(); onNavigate(it) }, inset = AppSpacing.l)
        }
    }
}

/**
 * The text being heard and its sentence, or the end of the playlist. With [fill] it takes its card's height:
 * the title at the top, the sentence in the middle and the progress at the bottom.
 */
@Composable
private fun NowPlaying(state: PlayerUiState, viewModel: PlayerViewModel, onNavigate: (Route) -> Unit, fill: Boolean = false) {
    val p = state.player
    val colors = MaterialTheme.colorScheme
    val player = viewModel.player
    if (p.status == PlayerStatus.COMPLETE) {
        Box(if (fill) Modifier.fillMaxSize() else Modifier, contentAlignment = Alignment.Center) {
            Complete(state, onPlayAgain = player::playAgain, onBack = { onNavigate(Route.Playlists) })
        }
        return
    }
    val item = p.item
    Column(if (fill) Modifier.fillMaxSize() else Modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
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
        if (fill) Spacer(Modifier.weight(1f))
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
        if (fill) Spacer(Modifier.weight(1f))
        if (p.sentences.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("{0} of {1}", p.sentence + 1, p.sentences.size), Modifier.weight(1f).testTag("sentence-progress"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        Text("${clockTime(state.elapsed)} / ${clockTime(state.total)}", Modifier.testTag("text-time"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    // The place in the text, to drag or click to, as the bar's; the wheel steps a sentence at a time.
                    var dragging by remember { mutableStateOf<Float?>(null) }
                    AppSlider(
                        value = dragging ?: p.textProgress,
                        onValueChange = { dragging = it },
                        valueRange = 0f..1f,
                        onValueChangeFinished = { dragging?.let(player::seek); dragging = null },
                        modifier = Modifier.fillMaxWidth().wheelThroughSentences(player).testTag("card-seek"),
                    )
                }
                AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = tr("Previous sentence"), onClick = player::previousSentence, style = IconButtonStyle.Outlined, modifier = Modifier.testTag("previous-sentence"))
                AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = tr("Next sentence"), onClick = player::nextSentence, style = IconButtonStyle.Outlined, modifier = Modifier.testTag("next-sentence"))
            }
        }
    }
}

/** Turns the wheel, or a two-finger swipe, into a sentence back or on per notch. */
private fun Modifier.wheelThroughSentences(player: PlaylistPlayer): Modifier = pointerInput(player) {
    var turned = 0f
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type != PointerEventType.Scroll) continue
            val delta = event.changes.fold(0f) { sum, change -> sum + change.scrollDelta.y + change.scrollDelta.x }
            event.changes.forEach { it.consume() }
            turned += delta
            while (turned >= 1f) { player.nextSentence(); turned -= 1f }
            while (turned <= -1f) { player.previousSentence(); turned += 1f }
        }
    }
}

/** The sentence with the word being spoken marked, when the voice tells which it is. */
@Composable
internal fun spokenSentence(sentence: String, word: IntRange?): AnnotatedString {
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
        Box(Modifier.size(DONE_CIRCLE).clip(AppShapes.circle).background(AppAccents.green.copy(alpha = AppAlpha.tint)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.green, modifier = Modifier.size(DONE_ICON))
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

/**
 * The playlist's texts in the order they play, the current one marked, with a way to change them. The rows
 * run to the container's edges, so their highlight fills them; [inset] keeps their content off the edges.
 */
@Composable
private fun UpNext(state: PlayerUiState, viewModel: PlayerViewModel, onNavigate: (Route) -> Unit, inset: Dp) {
    val p = state.player
    val playlist = p.playlist ?: return
    val queue: List<PlaylistItem> = p.order.mapNotNull { id -> playlist.items.firstOrNull { it.id == id } }
    Column {
        Text(tr("Up next"), Modifier.padding(horizontal = inset), style = AppText.headline)
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
                inset = inset,
                // The text being heard goes on from where it is; another one starts from its beginning.
                onPlay = { if (item.id == p.itemId && p.status != PlayerStatus.COMPLETE) { if (!p.isPlaying) viewModel.player.resume() } else viewModel.player.jumpTo(item.id, play = true) },
                onRemove = { viewModel.remove(item.id) },
            )
            AppDivider()
        }
        Spacer(Modifier.height(AppSpacing.l))
        SecondaryButton(tr("Add texts"), onClick = { onNavigate(Route.AddToPlaylist(playlist.id)) }, icon = Icons.Default.Add, modifier = Modifier.padding(horizontal = inset).fillMaxWidth().testTag("queue-add-texts"))
    }
}

@Composable
private fun QueueRow(item: PlaylistItem, index: Int, current: Boolean, time: String, handle: Modifier, dragging: Boolean, inset: Dp, onPlay: () -> Unit, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth()
            .then(if (dragging) Modifier.cardSurface(AppShapes.control) else Modifier)
            .then(if (current) Modifier.background(colors.primary.copy(alpha = AppAlpha.tint)) else Modifier)
            .clickable(onClickLabel = tr("Play"), role = Role.Button, onClick = onPlay)
            .padding(horizontal = inset, vertical = AppSpacing.m).testTag("queue-${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Icon(AppIcons.DragIndicator, contentDescription = tr("Drag to reorder"), tint = colors.onSurfaceVariant, modifier = handle.size(AppSizes.icon))
        if (!LocalWindowWidth.current.isCompact) Text("${index + 1}", Modifier.width(NUMBER_WIDTH), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.End)
        Icon(AppIcons.Description, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconLarge))
        Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(time, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
        if (item.finished) {
            Box(Modifier.size(AppSizes.iconLarge).clip(AppShapes.circle).background(AppAccents.green), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = tr("Heard to the end"), tint = AppAccents.onAccent, modifier = Modifier.size(AppSizes.iconSmall))
            }
        }
        OverflowMenu(tr("More actions"), buttonModifier = Modifier.testTag("queue-menu-${item.id}")) { close ->
            AppMenuItem(text = { Text(tr("Remove from playlist"), color = colors.error) }, onClick = { close(); onRemove() })
        }
    }
}

/** Opens the text being heard in the reader, at its page: a button in a phone's top bar. */
@Composable
private fun BackToTextButton(state: PlayerUiState, onNavigate: (Route) -> Unit) {
    val label = backToTextLabel(state) ?: return
    AppIconButton(AppIcons.MenuBook, contentDescription = label, onClick = { textRoute(state)?.let(onNavigate) }, modifier = Modifier.testTag("back-to-text"))
}

/** "Back to lesson", "Back to article" or "Back to book", by the kind of text being heard. */
@Composable
internal fun backToTextLabel(state: PlayerUiState): String? = when (state.player.item?.kind ?: return null) {
    com.idiomiko.core.domain.playlists.PlaylistTextKind.LESSON -> tr("Back to lesson")
    com.idiomiko.core.domain.playlists.PlaylistTextKind.NEWS -> tr("Back to article")
    com.idiomiko.core.domain.playlists.PlaylistTextKind.BOOK -> tr("Back to book")
}

/** The text being heard in the reader, at its page. */
internal fun textRoute(state: PlayerUiState): Route? = state.player.item?.let { Route.Read(it.bookId, state.player.page) }

/** Why a text cannot be played, in a sentence. */
@Composable
internal fun problemMessage(problem: PlaybackProblem): String = when (problem) {
    is PlaybackProblem.NoVoice -> tr("The {0} voice used by this playlist is not available on this device.", tr(problem.languageName))
    is PlaybackProblem.NoText -> tr("This text has no words to read aloud.")
    is PlaybackProblem.TextGone -> tr("\"{0}\" was deleted from your library, so it left the playlist. Listening goes on with the next text.", problem.title)
}

private val DONE_CIRCLE = 160.dp
private val DONE_ICON = 88.dp
private val DONE_BUTTON = 340.dp
private val NUMBER_WIDTH = 20.dp
