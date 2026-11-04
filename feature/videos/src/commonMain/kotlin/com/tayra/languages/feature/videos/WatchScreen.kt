package com.tayra.languages.feature.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.media.AutoPause
import com.tayra.languages.core.domain.media.CuePlayback
import com.tayra.languages.core.domain.media.VideoKind
import com.tayra.languages.core.ui.components.AppBottomSheet
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SelectableChip
import com.tayra.languages.core.ui.components.SidePanel
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.media.VideoController
import com.tayra.languages.core.ui.media.VideoPlayer
import com.tayra.languages.core.ui.media.VideoSource
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.feature.terms.form.TermFormEvent
import com.tayra.languages.feature.terms.form.TermFormPanel
import com.tayra.languages.feature.terms.form.TermFormViewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.tayra.languages.core.data.media.SubtitleFiles
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.media.VideoProblem
import com.tayra.languages.core.ui.media.videoAddress
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.random.Random

@Composable
fun WatchScreen(
    bookId: Long,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: WatchViewModel = koinViewModel(key = "watch-$bookId") { parametersOf(bookId) },
    /** The player; tests give one that only keeps time. */
    player: PlayerSlot = { source, onController, modifier -> VideoPlayer(source, onController, modifier) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) { onDispose { viewModel.detach() } }
    Scaffold(
        topBar = { AppTopBar(title = state.video?.title ?: tr("Video"), onNavigate = onNavigate, onBack = onBack, section = NavSection.VIDEOS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        when {
            state.loading -> LoadingIndicator(Modifier.padding(padding))
            state.video == null -> EmptyState(AppIcons.Movie, tr("Video not found"), Modifier.padding(padding), message = state.error?.let { tr(it) })
            else -> WatchContent(state, playback, viewModel, onNavigate, player, Modifier.padding(padding))
        }
    }
}

@Composable
private fun WatchContent(state: WatchUiState, playback: CuePlayback?, viewModel: WatchViewModel, onNavigate: (Route) -> Unit, playerSlot: PlayerSlot, modifier: Modifier) {
    val video = state.video ?: return
    val compact = LocalWindowWidth.current.isCompact
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val source = remember(video.media, video.kind) { if (video.kind == VideoKind.YOUTUBE) VideoSource.YouTube(video.media) else VideoSource.File(video.media) }
    val current = playback?.current?.collectAsState()?.value
    val problem by viewModel.problem.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val relink = rememberFilePickerLauncher(type = FileKitType.File(SubtitleFiles.mediaExtensions)) { file ->
        if (file != null) scope.launch { viewModel.relink(file.videoAddress()) }
    }
    val player: @Composable (Modifier) -> Unit = { m ->
        Box(m.fillMaxWidth().aspectRatio(VIDEO_RATIO).clip(AppShapes.small)) {
            playerSlot(source, viewModel::attach, Modifier.fillMaxSize())
            problem?.let { VideoProblemNotice(it, onChooseFile = { relink.launch() }, Modifier.align(Alignment.Center).padding(AppSpacing.l)) }
        }
    }
    Box(
        modifier.fillMaxSize().focusRequester(focus).focusable().onKeyEvent { event -> event.type == KeyEventType.KeyDown && handleKey(event.key, event.isAltPressed, event.isCtrlPressed || event.isMetaPressed, playback) },
    ) {
        if (compact) {
            Column(Modifier.fillMaxSize()) {
                player(Modifier)
                CurrentLine(state, current, viewModel, compact = true)
                Controls(state, playback, viewModel, Modifier.padding(horizontal = AppSpacing.m))
                Transcript(state, current, playback, viewModel, Modifier.weight(1f))
            }
            if (state.term != null) {
                AppBottomSheet(onDismissRequest = viewModel::closeTerm) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight(SHEET_HEIGHT)) { TermPane(state, viewModel, onNavigate) }
                }
            }
        } else {
            Row(Modifier.fillMaxSize().padding(AppSpacing.l), horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                Column(Modifier.weight(VIDEO_WEIGHT).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    player(Modifier)
                    Controls(state, playback, viewModel)
                    CurrentLine(state, current, viewModel, compact = false)
                }
                Transcript(state, current, playback, viewModel, Modifier.weight(TRANSCRIPT_WEIGHT).fillMaxHeight())
                if (state.term != null) {
                    SidePanel(TERM_PANE_WIDTH, Modifier.fillMaxHeight()) { TermPane(state, viewModel, onNavigate) }
                }
            }
        }
    }
}

@Composable
private fun VideoProblemNotice(problem: VideoProblem, onChooseFile: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.cardSurface().padding(AppSpacing.l), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(
            when (problem) {
                VideoProblem.FileMissing -> tr("The video file is not where it was.")
                VideoProblem.Unsupported -> tr("This kind of video can't be played here.")
                is VideoProblem.NeedsEngine -> tr("{0} is needed to play videos.", problem.name)
                is VideoProblem.Failed -> tr("The video could not be played.")
            },
            style = AppText.itemTitle,
            textAlign = TextAlign.Center,
        )
        if (problem is VideoProblem.Failed) Text(problem.message, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        if (problem == VideoProblem.FileMissing || problem == VideoProblem.Unsupported) {
            SecondaryButton(tr("Choose the file"), onClick = onChooseFile, icon = AppIcons.Movie, modifier = Modifier.testTag("relink-video"))
        }
    }
}

/** The keys of watching, as reading aloud has them: Space or W plays and pauses, A, S and D go back, again and on. */
private fun handleKey(key: Key, alt: Boolean, ctrl: Boolean, playback: CuePlayback?): Boolean {
    val p = playback ?: return false
    if (ctrl) return false
    when {
        alt && key == Key.DirectionLeft -> p.seekBy(-SEEK_MS)
        alt && key == Key.DirectionRight -> p.seekBy(SEEK_MS)
        alt -> return false
        key == Key.Spacebar || key == Key.W || key == Key.DirectionUp -> p.toggle()
        key == Key.A || key == Key.DirectionLeft -> p.previous()
        key == Key.D || key == Key.DirectionRight -> p.next()
        key == Key.S || key == Key.DirectionDown -> p.repeat()
        key == Key.Q -> p.cycleAutoPause()
        else -> return false
    }
    return true
}

@Composable
private fun CurrentLine(state: WatchUiState, current: Int?, viewModel: WatchViewModel, compact: Boolean) {
    val line = current?.let { state.lines.getOrNull(it) }
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag("current-line"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        if (line == null) {
            Text(" ", style = if (compact) AppText.itemTitle else AppText.headline)
            return@Column
        }
        TranscriptText(
            line.items,
            style = (if (compact) AppText.itemTitle else AppText.headline).copy(color = colors.onSurface),
            picked = state.picked?.takeIf { it.line == line.index }?.item,
            textAlign = TextAlign.Center,
            onWord = { viewModel.pickWord(line.index, it) },
            onLine = {},
        )
        if (state.showTranslations) {
            Text(line.translation.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Controls(state: WatchUiState, playback: CuePlayback?, viewModel: WatchViewModel, modifier: Modifier = Modifier) {
    val autoPause = playback?.autoPause?.collectAsState()?.value ?: AutoPause.OFF
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(AppIcons.SkipPrevious, tr("Previous line"), onClick = { playback?.previous() }, enabled = playback != null, modifier = Modifier.testTag("previous-line"))
        PlayPause(playback)
        AppIconButton(AppIcons.SkipNext, tr("Next line"), onClick = { playback?.next() }, enabled = playback != null, modifier = Modifier.testTag("next-line"))
        AppIconButton(AppIcons.Replay, tr("Repeat line"), onClick = { playback?.repeat() }, enabled = playback != null, modifier = Modifier.testTag("repeat-line"))
        SelectableChip(
            when (autoPause) {
                AutoPause.OFF -> tr("Auto-pause off")
                AutoPause.EVERY_LINE -> tr("Pause after each line")
                AutoPause.UNKNOWN_LINES -> tr("Pause on new words")
            },
            selected = autoPause != AutoPause.OFF,
            onClick = { viewModel.setAutoPause(AutoPause.entries[(autoPause.ordinal + 1) % AutoPause.entries.size]) },
            modifier = Modifier.testTag("auto-pause"),
        )
        SelectableChip(tr("Translation"), selected = state.showTranslations, onClick = { viewModel.setShowTranslations(!state.showTranslations) }, modifier = Modifier.testTag("show-translations"))
        SpeedMenu(playback)
    }
}

@Composable
private fun PlayPause(playback: CuePlayback?) {
    val playing = playback?.playing?.collectAsState()?.value ?: false
    AppIconButton(
        if (playing) AppIcons.Pause else AppIcons.PlayArrow,
        if (playing) tr("Pause") else tr("Play"),
        onClick = { playback?.toggle() },
        enabled = playback != null,
        size = IconButtonSize.Large,
        style = IconButtonStyle.Filled,
        modifier = Modifier.testTag("play-pause").semantics { stateDescription = if (playing) "playing" else "paused" },
    )
}

@Composable
private fun SpeedMenu(playback: CuePlayback?) {
    val rate = playback?.rate?.collectAsState()?.value ?: 1f
    var open by remember { mutableStateOf(false) }
    // The menu anchors to its enclosing composable, so the button gets a Box of its own.
    Box {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconButton(AppIcons.Speed, tr("Playback speed"), onClick = { open = true }, enabled = playback != null, modifier = Modifier.testTag("speed"))
            Text(speedLabel(rate), style = AppText.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            SPEEDS.forEach { speed ->
                AppMenuItem(text = { Text(if (speed == rate) "✓ ${speedLabel(speed)}" else speedLabel(speed)) }, onClick = { open = false; playback?.setRate(speed) })
            }
        }
    }
}

private fun speedLabel(speed: Float): String = if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "$speed×"

@Composable
private fun Transcript(state: WatchUiState, current: Int?, playback: CuePlayback?, viewModel: WatchViewModel, modifier: Modifier) {
    val list = rememberLazyListState()
    val colors = MaterialTheme.colorScheme
    LaunchedEffect(current) {
        val at = current ?: return@LaunchedEffect
        val visible = list.layoutInfo.visibleItemsInfo
        val shown = visible.any { it.index == at } && visible.lastOrNull()?.index != at
        if (!shown) list.animateScrollToItem((at - FOLLOW_ABOVE).coerceAtLeast(0))
    }
    ScrollList(modifier.clip(AppShapes.card).background(colors.surface).testTag("transcript"), state = list) {
        itemsIndexed(state.lines, key = { _, line -> line.index }) { _, line ->
            val isCurrent = line.index == current
            Column(
                Modifier.fillMaxWidth()
                    .background(if (isCurrent) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
                    .clickable { playback?.jumpTo(line.index) }
                    .padding(horizontal = AppSpacing.l, vertical = AppSpacing.s)
                    .testTag("line-${line.index}")
                    .semantics { if (isCurrent) stateDescription = CURRENT_LINE },
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
            ) {
                TranscriptText(
                    line.items,
                    style = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    picked = state.picked?.takeIf { it.line == line.index }?.item,
                    onWord = { viewModel.pickWord(line.index, it) },
                    onLine = { playback?.jumpTo(line.index) },
                )
                if (state.showTranslations && line.translation != null) {
                    Text(line.translation, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TermPane(state: WatchUiState, viewModel: WatchViewModel, onNavigate: (Route) -> Unit) {
    val key = state.term ?: return
    // Each opening gets a new form, since a form may have moved on to a looked-up word.
    val opening = remember(state.termKey) { Random.nextLong() }
    val form = koinViewModel<TermFormViewModel>(key = "watch-term-${state.termKey}-$opening") { parametersOf(key) }
    CollectEvents(form.events) { event ->
        when (event) {
            is TermFormEvent.Saved -> viewModel.onTermChanged(close = !event.keepOpen)
            TermFormEvent.Deleted -> viewModel.onTermChanged(close = true)
            is TermFormEvent.OpenParent -> viewModel.openTerm(event.languageId, event.text, key.sentence)
            is TermFormEvent.OpenTerm -> viewModel.openTerm(event.languageId, event.text, event.sentence)
        }
    }
    TermFormPanel(
        viewModel = form,
        modifier = Modifier.fillMaxSize(),
        embedded = true,
        onClose = { form.flush(); viewModel.closeTerm() },
        onDuplicateClick = { onNavigate(Route.EditTerm(it)) },
        onOpenExamples = { languageId, text -> onNavigate(Route.Examples(languageId, text)) },
        onManageDictionaries = { languageId -> onNavigate(Route.ManageDictionaries(languageId)) },
    )
}

/** Where the video plays: the platform's player, or a stand-in. */
typealias PlayerSlot = @Composable (source: VideoSource, onController: (VideoController) -> Unit, modifier: Modifier) -> Unit

/** The state description of the line being spoken, for tests and accessibility tools. */
internal const val CURRENT_LINE = "current line"

private const val VIDEO_RATIO = 16f / 9f
private const val VIDEO_WEIGHT = 0.56f
private const val TRANSCRIPT_WEIGHT = 0.44f
private const val SHEET_HEIGHT = 0.7f
private const val FOLLOW_ABOVE = 2
private const val SEEK_MS = 5_000L
private val SPEEDS = listOf(0.5f, 0.75f, 0.9f, 1f, 1.25f, 1.5f)
private val TERM_PANE_WIDTH = 420.dp
