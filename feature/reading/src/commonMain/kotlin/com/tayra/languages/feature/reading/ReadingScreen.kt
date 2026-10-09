package com.tayra.languages.feature.reading

import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.Key
import com.tayra.languages.core.ui.hotkeys.ShiftTracker
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.utf16CodePoint
import co.touchlab.kermit.Logger
import com.tayra.languages.core.ui.audio.Speaker
import com.tayra.languages.core.domain.settings.Hotkey
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.LocalTranslationProblem
import com.tayra.languages.core.domain.service.SentenceTranslation
import com.tayra.languages.core.domain.service.TranslationEngine
import androidx.compose.runtime.collectAsState
import kotlin.math.roundToInt
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.domain.stats.BookStatsCalculator
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.domain.service.SentenceAudioState
import com.tayra.languages.core.domain.service.SentenceAudio
import org.koin.compose.koinInject
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.ui.components.AboveTargetPositionProvider
import androidx.compose.ui.platform.LocalDensity
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.StatusDistributionBar
import com.tayra.languages.core.ui.components.ToastHost
import com.tayra.languages.core.ui.components.rememberToastState
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.TextInputDialog
import com.tayra.languages.core.ui.hotkeys.HotkeyMatcher
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.ReadingFont
import com.tayra.languages.core.ui.theme.fontFamily
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.feature.terms.form.TermFormEvent
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.form.TermFormPanel
import com.tayra.languages.feature.terms.form.TermFormViewModel
import com.tayra.languages.feature.terms.list.BulkEditDialog
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.random.Random
import org.koin.core.parameter.parametersOf
import com.tayra.languages.core.ui.components.EdgeScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    bookId: Long,
    initialPage: Int?,
    onNavigate: (Route) -> Unit,
    onHome: () -> Unit,
    viewModel: ReadingViewModel = koinViewModel(key = "reading-$bookId") { parametersOf(bookId, initialPage) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val theme = TayraTheme.current
    val wide = LocalWindowWidth.current.isExpanded
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var bookmarkDialog by remember { mutableStateOf(false) }
    var panelFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val shift = remember { ShiftTracker() }
    val toast = rememberToastState()

    CollectEvents(viewModel.events) { event ->
        when (event) {
            is ReadingEvent.CopyText -> clipboard.setText(AnnotatedString(event.text))
            is ReadingEvent.OpenUrl -> uriHandler.openUri(event.url)
            is ReadingEvent.Navigate -> onNavigate(Route.Read(event.bookId, event.page))
            ReadingEvent.BookFinished -> onHome()
            is ReadingEvent.Toast -> toast.show(event.message)
        }
    }
    LaunchedEffect(state.loading, state.panel) {
        if (!state.loading && state.panel == ReadingPanel.None) {
            panelFocused = false
            runCatching { focusRequester.requestFocus() }
        }
    }

    val hotkeys = state.settings.hotkeys
    val rtl = state.language?.rightToLeft == true
    val nextIncrement = if (rtl) -1 else 1
    // Shared with the text below, so the keyboard drives the same reading aloud as the buttons.
    val speaker = rememberSpeaker(koinInject(), koinInject(), koinInject<SentenceAudio>())
    val continuous = remember(speaker) { ContinuousReading(speaker) }

    fun handleAction(action: HotkeyAction): Boolean {
        when (action) {
            HotkeyAction.LISTEN_PLAY_PAUSE -> continuous.toggle()
            HotkeyAction.LISTEN_PREVIOUS, HotkeyAction.LISTEN_PREVIOUS_ARROW -> continuous.previous()
            HotkeyAction.LISTEN_NEXT, HotkeyAction.LISTEN_NEXT_ARROW -> continuous.next()
            HotkeyAction.LISTEN_REPEAT, HotkeyAction.LISTEN_REPEAT_ARROW -> continuous.repeat()
            HotkeyAction.LISTEN_PAUSE, HotkeyAction.LISTEN_PAUSE_ARROW -> continuous.toggle()
            HotkeyAction.PREV_COLORED_WORD -> viewModel.moveCursor(-nextIncrement, CursorTarget.COLORED_WORD)
            HotkeyAction.NEXT_COLORED_WORD -> viewModel.moveCursor(nextIncrement, CursorTarget.COLORED_WORD)
            HotkeyAction.TEXT_LARGER -> viewModel.stepFontSize(1)
            HotkeyAction.TEXT_SMALLER -> viewModel.stepFontSize(-1)
            HotkeyAction.LINES_FURTHER -> viewModel.stepLineHeight(1)
            HotkeyAction.LINES_CLOSER -> viewModel.stepLineHeight(-1)
            HotkeyAction.TOGGLE_TERM_PANE -> viewModel.toggleTermPane()
            HotkeyAction.TEXT_RESET -> viewModel.resetFontSize()
            HotkeyAction.LINES_RESET -> viewModel.resetLineHeight()
            HotkeyAction.LISTEN_AUTO_PAUSE -> viewModel.toggleAutoPause()
            HotkeyAction.START_HOVER -> viewModel.startHoverMode()
            HotkeyAction.PREV_WORD -> viewModel.moveCursor(-nextIncrement, CursorTarget.WORD)
            HotkeyAction.NEXT_WORD -> viewModel.moveCursor(nextIncrement, CursorTarget.WORD)
            HotkeyAction.PREV_UNKNOWN_WORD -> viewModel.moveCursor(-nextIncrement, CursorTarget.UNKNOWN_WORD)
            HotkeyAction.NEXT_UNKNOWN_WORD -> viewModel.moveCursor(nextIncrement, CursorTarget.UNKNOWN_WORD)
            HotkeyAction.PREV_SENTENCE -> viewModel.moveCursor(-nextIncrement, CursorTarget.SENTENCE_START)
            HotkeyAction.NEXT_SENTENCE -> viewModel.moveCursor(nextIncrement, CursorTarget.SENTENCE_START)
            HotkeyAction.STATUS_1 -> viewModel.setStatus(TermStatus.NEW_1)
            HotkeyAction.STATUS_2 -> viewModel.setStatus(TermStatus.NEW_2)
            HotkeyAction.STATUS_3 -> viewModel.setStatus(TermStatus.LEARNING_3)
            HotkeyAction.STATUS_4 -> viewModel.setStatus(TermStatus.LEARNING_4)
            HotkeyAction.STATUS_IGNORE -> viewModel.setStatus(TermStatus.IGNORED)
            HotkeyAction.STATUS_WELL_KNOWN -> viewModel.setStatus(TermStatus.WELL_KNOWN)
            HotkeyAction.STATUS_UP -> viewModel.shiftStatus(1)
            HotkeyAction.STATUS_DOWN -> viewModel.shiftStatus(-1)
            HotkeyAction.DELETE_TERM -> viewModel.setStatus(TermStatus.UNKNOWN)
            HotkeyAction.PREVIOUS_PAGE -> viewModel.goToRelativePage(-1)
            HotkeyAction.NEXT_PAGE -> viewModel.goToRelativePage(1)
            HotkeyAction.MARK_READ -> viewModel.markPageRead(false, 1)
            HotkeyAction.MARK_READ_WELL_KNOWN -> viewModel.markPageRead(true, 1)
            HotkeyAction.TRANSLATE_SENTENCE -> viewModel.translate(TextScope.SENTENCE)
            HotkeyAction.TRANSLATE_PARAGRAPH -> viewModel.translate(TextScope.PARAGRAPH)
            HotkeyAction.TRANSLATE_PAGE -> viewModel.translate(TextScope.PAGE)
            HotkeyAction.COPY_SENTENCE -> viewModel.copy(TextScope.SENTENCE)
            HotkeyAction.COPY_PARAGRAPH -> viewModel.copy(TextScope.PARAGRAPH)
            HotkeyAction.COPY_PAGE -> viewModel.copy(TextScope.PAGE)
            HotkeyAction.PAGE_TERM_LIST -> onNavigate(Route.Terms(viewModel.pageTermIds(), bookId, state.pageNumber))
            HotkeyAction.BOOKMARK -> bookmarkDialog = true
            HotkeyAction.EDIT_PAGE -> onNavigate(Route.EditPage(bookId, state.pageNumber))
            HotkeyAction.NEXT_THEME -> viewModel.nextTheme()
            HotkeyAction.TOGGLE_HIGHLIGHT -> viewModel.toggleHighlights()
            HotkeyAction.TOGGLE_FOCUS -> viewModel.toggleFocusMode()
            HotkeyAction.SAVE_TERM -> return false
        }
        return true
    }

    val paneActions = ReaderPaneActions(
        onEditBook = { onNavigate(Route.EditBook(bookId)) },
        onEditPage = { onNavigate(Route.EditPage(bookId, state.pageNumber)) },
        onTermList = { onNavigate(Route.Terms(viewModel.pageTermIds(), bookId, state.pageNumber)) },
        onSource = { state.book?.sourceUri?.let { uriHandler.openUri(it) } },
        onSpeechSettings = { onNavigate(Route.Speech) },
        onTranslationSettings = { onNavigate(Route.OfflineTranslation) },
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                windowInsets = WindowInsets.safeDrawing,
                // Wider than Material's 360dp drawer, so the tabs and tool tiles fit; a phone keeps a strip of the page.
                modifier = if (LocalWindowWidth.current.isCompact) Modifier.fillMaxWidth(0.9f) else Modifier.width(460.dp),
            ) {
                ReaderPane(state, viewModel, paneActions, onClose = { scope.launch { drawerState.close() } })
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(theme.readingBackground)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .focusRequester(focusRequester)
                .focusable()
                // Before the focused button sees it, so Space after clicking a button still plays or pauses.
                .onFocusChanged { if (!it.hasFocus) shift.reset() }
                .onPreviewKeyEvent { event ->
                    val isShift = event.key == Key.ShiftLeft || event.key == Key.ShiftRight
                    if (shift.onKey(isShift, down = event.type == KeyEventType.KeyDown)) return@onPreviewKeyEvent false
                    if (event.type != KeyEventType.KeyDown || panelFocused || state.items.isEmpty()) return@onPreviewKeyEvent false
                    val pressed = HotkeyMatcher.fromEvent(event)?.let(shift::adjust) ?: return@onPreviewKeyEvent false
                    val action = HotkeyAction.resolve(hotkeys, pressed, wordSelected = state.marked.isNotEmpty(), listening = state.settings.showSentencePlay)
                    Logger.d { "Reader key ${event.key} (char ${event.utf16CodePoint}, Shift flag ${event.isShiftPressed}, Shift held ${shift.held}) read as $pressed: ${action ?: "no shortcut"}" }
                    if (action == null) return@onPreviewKeyEvent false
                    handleAction(action)
                },
        ) {
            val compact = LocalWindowWidth.current.isCompact
            when {
                state.settings.focusMode -> FocusBar(state, viewModel, onMenu = { scope.launch { drawerState.open() } })
                compact -> ReadingHeader(state, viewModel, onMenu = { scope.launch { drawerState.open() } }, onHome = onHome)
                else -> {
                    AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, section = if (state.lesson == null) NavSection.BOOKS else NavSection.COURSES)
                    ReaderToolbar(state, viewModel, onMenu = { scope.launch { drawerState.open() } }, onHome = onHome, onCourses = { onNavigate(Route.Courses) })
                }
            }
            Row(Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (state.settings.focusMode) 0f else 0.3f))) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    ReadingBody(state, viewModel, speaker, continuous, onHome = onHome, onPractice = { onNavigate(Route.Practice(bookId, state.pageNumber)) }, onSettings = { onNavigate(Route.OfflineTranslation) }, focusText = { runCatching { focusRequester.requestFocus() } })
                }
                if (wide && state.panel != ReadingPanel.None) {
                    Surface(
                        // The gap at the start keeps the text's scrollbar off the panel's border.
                        Modifier.width(432.dp).fillMaxHeight().padding(start = 12.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            .onFocusChanged { panelFocused = it.hasFocus },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        PanelContent(state, viewModel, onNavigate)
                    }
                }
            }
        }
    }

    if (!wide && state.panel != ReadingPanel.None) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(onDismissRequest = viewModel::closePanel, sheetState = sheetState) {
            Box(Modifier.fillMaxWidth().onFocusChanged { panelFocused = it.hasFocus }) { PanelContent(state, viewModel, onNavigate) }
        }
    }
    if (bookmarkDialog) {
        TextInputDialog(
            title = tr("Add bookmark"),
            label = tr("Title"),
            onConfirm = { viewModel.addBookmark(it.trim()); bookmarkDialog = false },
            onDismiss = { bookmarkDialog = false },
        )
    }
    ToastHost(toast)
}

@Composable
private fun ReadingHeader(state: ReadingUiState, viewModel: ReadingViewModel, onMenu: () -> Unit, onHome: () -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = tr("Menu")) }
                if (state.lesson == null) IconButton(onClick = onHome) { Icon(Icons.Default.Home, contentDescription = tr("Home")) }
                else IconButton(onClick = onHome) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back to course")) }
                Text(
                    state.book?.title.orEmpty(),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = if (state.language?.rightToLeft == true) TextDirection.Rtl else TextDirection.Ltr),
                    maxLines = 1,
                )
            }
            PageProgress(state.pageNumber, state.pageCount, viewModel::goToPage, sliderWidth = null, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp))
        }
    }
}

/** Wide-screen toolbar: breadcrumb on the left, the page position and slider in the middle. */
@Composable
private fun ReaderToolbar(state: ReadingUiState, viewModel: ReadingViewModel, onMenu: () -> Unit, onHome: () -> Unit, onCourses: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val rtl = state.language?.rightToLeft == true
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = tr("Menu")) }
            Spacer(Modifier.width(8.dp))
            val lesson = state.lesson
            val separator: @Composable () -> Unit = {
                Text("/", style = MaterialTheme.typography.bodyLarge, color = colors.outline, modifier = Modifier.padding(horizontal = 8.dp))
            }
            if (lesson == null) {
                Crumb(tr("Books"), onHome) { Icon(Icons.Default.Home, contentDescription = tr("Home"), tint = colors.onSurfaceVariant, modifier = Modifier.padding(end = 2.dp)) }
            } else {
                Crumb(tr("Courses"), onCourses) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.padding(end = 2.dp).size(20.dp)) }
                separator()
                Crumb(lesson.course.title, onHome, Modifier.weight(1f, fill = false))
            }
            separator()
            Text(
                state.book?.title.orEmpty(),
                Modifier.weight(1f, fill = false).padding(end = 16.dp),
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PageProgress(
            state.pageNumber,
            state.pageCount,
            viewModel::goToPage,
            sliderWidth = if (LocalWindowWidth.current.isExpanded) 300.dp else 160.dp,
        )
        Spacer(Modifier.weight(1f))
    }
}

/** A place to go back to, in the toolbar's breadcrumb. */
@Composable
private fun Crumb(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: @Composable () -> Unit = {}) {
    Row(
        modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon()
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Minimal header shown in focus mode: just the menu, the page position and a way out. */
@Composable
private fun FocusBar(state: ReadingUiState, viewModel: ReadingViewModel, onMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = tr("Menu")) }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { viewModel.goToRelativePage(-1) }, enabled = !state.isFirstPage) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = tr("Previous page"))
        }
        Text("${state.pageNumber}/${state.pageCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = { viewModel.goToRelativePage(1) }, enabled = !state.isLastPage) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = tr("Next page"))
        }
        TextButton(onClick = viewModel::toggleFocusMode) { Text(tr("Exit focus")) }
    }
}

@Composable
private fun ReadingBody(
    state: ReadingUiState,
    viewModel: ReadingViewModel,
    speaker: Speaker,
    continuous: ContinuousReading,
    onHome: () -> Unit,
    onPractice: () -> Unit,
    onSettings: () -> Unit,
    focusText: () -> Unit,
) {
    val theme = TayraTheme.current
    if (state.loading) {
        LoadingIndicator()
        return
    }
    if (state.error != null) {
        ErrorMessage(state.error, Modifier.padding(16.dp))
        return
    }
    val speechLanguage = state.language?.name?.let { LanguageCodes.codeFor(it) }
    val speakSentence: (String) -> Unit = remember(continuous) { { text -> continuous.sentenceClicked(text) } }
    val playingSentence by speaker.playing.collectAsState()
    val synthesizing by speaker.working.collectAsState()
    val audioStates by viewModel.sentenceAudioStates.collectAsState()
    // Sentences whose audio is still being made: queued or under way ahead of time, or the one just asked for.
    val preparingSentences = remember(audioStates, playingSentence, synthesizing) {
        audioStates.filterValues { it == SentenceAudioState.PREPARING }.keys + listOfNotNull(playingSentence.takeIf { synthesizing })
    }
    // The callbacks are built once, so they read the state through this rather than the value of the first frame.
    val current by rememberUpdatedState(state)
    val pageSentences = remember(state.page) { state.page.paragraphs.flatMap { it.sentences }.map { it.displayText }.filter { it.any(Char::isLetter) } }
    LaunchedEffect(pageSentences, speechLanguage) { continuous.setPage(pageSentences, speechLanguage) }
    LaunchedEffect(continuous) { speaker.playing.collect(continuous::speakerTaken) }
    SideEffect {
        continuous.autoPause = state.settings.autoPause
        continuous.turnPage = {
            if (current.isLastPage) false else {
                viewModel.goToRelativePage(1)
                true
            }
        }
    }
    val speakWord by rememberUpdatedState { index: Int -> viewModel.wordToSpeak(index)?.let { speaker.speak(it, speechLanguage) } }
    val callbacks = remember(viewModel) {
        ReadingTextCallbacks(
            onClick = { index, shift ->
                if (!shift) speakWord(index)
                viewModel.onWordClick(index, shift)
                focusText()
            },
            onSecondaryClick = { index -> viewModel.markToLearn(index); focusText() },
            onTap = { index ->
                speakWord(index)
                viewModel.onWordTap(index)
            },
            onLongPress = { index ->
                val tokenIndex = current.items[index].index
                if (!viewModel.state.value.selecting) viewModel.startSelection(tokenIndex) else viewModel.endSelection(tokenIndex, copy = false)
            },
            onHover = viewModel::onHover,
            onDragStart = { index -> viewModel.startSelection(current.items[index].index) },
            onDrag = { index -> viewModel.updateSelection(current.items[index].index) },
            onDragEnd = { index, shift -> viewModel.endSelection(current.items[index].index, copy = shift) },
            popupContent = { _, word ->
                current.popup?.let { popup ->
                    val gap = with(LocalDensity.current) { 6.dp.roundToPx() }
                    Popup(
                        popupPositionProvider = remember(word, gap) { AboveTargetPositionProvider(word, gap) },
                        onDismissRequest = viewModel::hidePopup,
                    ) { TermPopupCard(popup.popup) }
                }
            },
        )
    }
    val scrollState = rememberScrollState()
    LaunchedEffect(state.pageNumber) { scrollState.scrollTo(0) }
    val compact = LocalWindowWidth.current.isCompact
    val focus = state.settings.focusMode
    val edgePadding = if (focus || compact) 16.dp else 32.dp
    val cardModifier = if (focus) {
        Modifier.widthIn(max = state.settings.readingColumnWidth.dp).padding(horizontal = edgePadding, vertical = 12.dp)
    } else {
        Modifier.padding(if (compact) 12.dp else 16.dp)
            .widthIn(max = state.settings.readingColumnWidth.dp + 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(theme.readingBackground)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            // No bottom padding: the footer runs to the card's bottom edge.
            .padding(start = edgePadding, end = edgePadding, top = if (compact) 16.dp else 28.dp)
    }
    Box(Modifier.fillMaxSize()) {
    // The visible height, so the sentence being read can be scrolled to the middle of it.
    var visibleHeight by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().onSizeChanged { visibleHeight = it.height }.verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(cardModifier) {
            if (!focus) {
                Text(
                    state.book?.title.orEmpty(),
                    style = MaterialTheme.typography.headlineMedium.copy(color = theme.readingText, textDirection = if (state.language?.rightToLeft == true) TextDirection.Rtl else TextDirection.Ltr),
                    fontWeight = FontWeight.Bold,
                )
                PageVocabulary(state)
                state.translationProgress?.let { TranslationProgress(it) }
                    ?: state.translationError?.let { TranslationNotice(it, viewModel, onSettings) }
                HorizontalDivider(Modifier.padding(bottom = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
            state.flash?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge) }
            ReadingText(
                page = state.page,
                theme = theme,
                showHighlights = state.settings.showHighlights,
                marked = state.marked,
                hovered = state.hovered,
                selection = state.selection,
                popupItem = state.popup?.itemIndex,
                fontScale = state.settings.readingFontScale,
                lineHeight = state.settings.readingLineHeight,
                rightToLeft = state.language?.rightToLeft == true,
                fontFamily = ReadingFont.byId(state.settings.readingFont).fontFamily(),
                splitSentences = state.settings.splitSentences,
                justify = state.settings.readingJustified,
                translations = if (state.settings.showTranslations) state.translations else null,
                sideBySide = state.settings.sideBySideTranslations,
                onSpeakSentence = if (state.settings.showSentencePlay) speakSentence else null,
                playingSentence = playingSentence,
                preparingSentences = preparingSentences,
                // A sentence played on its own is highlighted while heard; continuous reading's place stays marked when paused.
                readingSentence = if (state.settings.showSentencePlay) playingSentence ?: continuous.current else null,
                readingHeard = playingSentence != null,
                followReading = continuous.active,
                visibleHeight = visibleHeight,
                edgePadding = edgePadding,
                callbacks = callbacks,
            )
            if (state.selecting) {
                Text(tr("Long-press the last word of the expression, or tap to cancel."), style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable { viewModel.cancelSelection() })
            }
            if (!state.settings.focusMode) ReadingFooter(state, viewModel, onHome, onPractice, edgePadding)
        }
        Spacer(Modifier.height(120.dp))
    }
    EdgeScrollbar(scrollState)
    if (state.settings.showSentencePlay && pageSentences.isNotEmpty()) {
        ContinuousControls(
            playing = continuous.active,
            autoPause = state.settings.autoPause,
            hotkeys = state.settings.hotkeys.takeUnless { LocalWindowWidth.current.isCompact },
            onPlay = continuous::toggle,
            onAutoPause = viewModel::toggleAutoPause,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
        )
    }
    }
}

/** The small round buttons beside the play button share one size and border. */
private val CONTROL_SIZE = 40.dp
private val CONTROL_BORDER = 1.5.dp

/** The listening and paging shortcuts as they are set now, keys first. */
@Composable
private fun ShortcutsCard(hotkeys: Map<HotkeyAction, Hotkey?>) {
    val colors = MaterialTheme.colorScheme
    // Keys for one action are alternatives ("or"); a row of two opposite actions pairs them with "/".
    val or = tr("or")
    val rows = listOf(
        Triple(tr("Play / pause"), listOf(HotkeyAction.LISTEN_PLAY_PAUSE), or),
        Triple(tr("Previous sentence"), listOf(HotkeyAction.LISTEN_PREVIOUS, HotkeyAction.LISTEN_PREVIOUS_ARROW), or),
        Triple(tr("Next sentence"), listOf(HotkeyAction.LISTEN_NEXT, HotkeyAction.LISTEN_NEXT_ARROW), or),
        Triple(tr("Repeat sentence"), listOf(HotkeyAction.LISTEN_REPEAT, HotkeyAction.LISTEN_REPEAT_ARROW), or),
        Triple(tr("Pause / resume"), listOf(HotkeyAction.LISTEN_PAUSE, HotkeyAction.LISTEN_PAUSE_ARROW), or),
        Triple(tr("Mark word as known"), listOf(HotkeyAction.STATUS_WELL_KNOWN), or),
        Triple(tr("Mark word as unknown"), listOf(HotkeyAction.DELETE_TERM), or),
        Triple(tr("Show / hide the term pane"), listOf(HotkeyAction.TOGGLE_TERM_PANE), or),
        Triple(tr("Next coloured word"), listOf(HotkeyAction.NEXT_COLORED_WORD), or),
        Triple(tr("Previous coloured word"), listOf(HotkeyAction.PREV_COLORED_WORD), or),
        Triple(tr("Larger / smaller text"), listOf(HotkeyAction.TEXT_LARGER, HotkeyAction.TEXT_SMALLER), "/"),
        Triple(tr("More / less line height"), listOf(HotkeyAction.LINES_FURTHER, HotkeyAction.LINES_CLOSER), "/"),
        Triple(tr("Text size / line height back to default"), listOf(HotkeyAction.TEXT_RESET, HotkeyAction.LINES_RESET), "/"),
        Triple(tr("Auto-pause on / off"), listOf(HotkeyAction.LISTEN_AUTO_PAUSE), or),
        Triple(tr("Next page"), listOf(HotkeyAction.NEXT_PAGE), or),
        Triple(tr("Previous page"), listOf(HotkeyAction.PREVIOUS_PAGE), or),
    ).mapNotNull { (label, actions, separator) -> actions.mapNotNull { hotkeys[it] }.takeIf { it.isNotEmpty() }?.let { Triple(label, it, separator) } }
    Surface(shape = RoundedCornerShape(14.dp), color = colors.surface, shadowElevation = 8.dp, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(Modifier.padding(16.dp).width(440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("Keyboard shortcuts"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            rows.forEach { (label, keys, separator) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.width(250.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        keys.forEachIndexed { index, key ->
                            if (index > 0) Text(separator, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            KeyCap(key)
                        }
                    }
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                tr("Ctrl (⌘ on a Mac) with the arrows moves between words and changes a word's status; K and U need a selected word."),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** A key as printed on the keyboard: arrows as arrows, modifiers joined with +. */
@Composable
private fun KeyCap(hotkey: Hotkey) {
    val colors = MaterialTheme.colorScheme
    val key = when (hotkey.key) {
        "Left" -> "\u2190"
        "Right" -> "\u2192"
        "Up" -> "\u2191"
        "Down" -> "\u2193"
        "Plus" -> "+"
        "Minus" -> "\u2212"
        else -> hotkey.key
    }
    val label = buildList {
        if (hotkey.ctrl) add("Ctrl")
        if (hotkey.alt) add("Alt")
        if (hotkey.shift) add("Shift")
        add(key)
    }.joinToString(" + ")
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(colors.surfaceVariant)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/**
 * The page's play button, to read on from sentence to sentence, with the auto-pause switch after
 * it and, where there is a keyboard ([hotkeys] given), a button listing the listening shortcuts
 * before it.
 */
@Composable
private fun ContinuousControls(
    playing: Boolean,
    autoPause: Boolean,
    hotkeys: Map<HotkeyAction, Hotkey?>?,
    onPlay: () -> Unit,
    onAutoPause: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var showShortcuts by remember { mutableStateOf(false) }
    val shortcutsDescription = tr("Keyboard shortcuts")
    val playDescription = if (playing) tr("Pause reading") else tr("Read the page")
    val autoPauseDescription = if (autoPause) tr("Auto-pause on") else tr("Auto-pause off")
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (hotkeys != null) {
            Box {
                Box(
                    Modifier.size(CONTROL_SIZE).clip(CircleShape).background(colors.surface)
                        .border(CONTROL_BORDER, colors.primary, CircleShape)
                        .clickable { showShortcuts = !showShortcuts }
                        .semantics { contentDescription = shortcutsDescription },
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Keyboard, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp)) }
                if (showShortcuts) {
                    Popup(
                        alignment = Alignment.BottomEnd,
                        offset = with(LocalDensity.current) { IntOffset(0, -52.dp.roundToPx()) },
                        onDismissRequest = { showShortcuts = false },
                    ) { ShortcutsCard(hotkeys) }
                }
            }
        }
        Box(
            Modifier.size(60.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(colors.primary)
                .clickable(onClick = onPlay)
                .semantics { contentDescription = playDescription },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) AppIcons.Pause else AppIcons.PlayArrow, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(32.dp))
        }
        Box(
            Modifier.size(CONTROL_SIZE).clip(CircleShape)
                .background(if (autoPause) colors.primary else colors.surface)
                .border(CONTROL_BORDER, colors.primary, CircleShape)
                .clickable(onClick = onAutoPause)
                .semantics { contentDescription = autoPauseDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(tr("AP"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (autoPause) colors.onPrimary else colors.primary)
        }
    }
}

/** Distribution of term statuses on the current page, shown under the title. */
/** Missing models get an Install button, unsupported pairs a way back to MyMemory, anything else a retry. */
@Composable
private fun TranslationNotice(problem: LocalTranslationProblem, viewModel: ReadingViewModel, onSettings: () -> Unit) {
    val failed = problem is LocalTranslationProblem.Failed
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (failed) colors.errorContainer else colors.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                when (problem) {
                    is LocalTranslationProblem.Failed -> tr("Offline translation failed: {0}", problem.message)
                    is LocalTranslationProblem.ModelMissing -> tr("Offline translation for {0} \u2192 {1} is available, but its models are not installed.", languageName(problem.fromCode), languageName(problem.toCode))
                    is LocalTranslationProblem.NoModel -> tr("{0} has no {1} \u2192 {2} model. Turn offline translation off to use MyMemory.", problem.engineName, tr(problem.fromName), tr(problem.toName))
                },
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = if (failed) colors.onErrorContainer else colors.onSecondaryContainer,
            )
            when (problem) {
                is LocalTranslationProblem.ModelMissing -> Button(onClick = viewModel::installOfflineModels) { Text(tr("Install")) }
                is LocalTranslationProblem.NoModel -> {
                    Text(tr("Offline"), style = MaterialTheme.typography.bodySmall, color = colors.onSecondaryContainer)
                    Switch(checked = true, onCheckedChange = { on -> if (!on) viewModel.useOnlineEngine() })
                }
                is LocalTranslationProblem.Failed -> OutlinedButton(onClick = viewModel::retryOfflineTranslation) { Text(tr("Try again")) }
            }
            TextButton(onClick = onSettings) { Text(tr("Settings")) }
        }
    }
}

/** The translated name of the language with [code], or the code when it is not a known one. */
private fun languageName(code: String): String = LanguageCodes.option(code)?.name?.let { tr(it) } ?: code

@Composable
private fun TranslationProgress(message: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Setting up offline translation: {0}", message), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PageVocabulary(state: ReadingUiState) {
    val stats = remember(state.page) { BookStatsCalculator.calculate(state.items) }
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(tr("Vocabulary on this page"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        StatusDistributionBar(stats, Modifier.widthIn(max = 360.dp).weight(1f, fill = false).fillMaxWidth(), scope = "on this page")
        Text(if (stats.distinctTerms > 0) tr("{0}% new", stats.unknownPercent) else "—", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun ReadingFooter(state: ReadingUiState, viewModel: ReadingViewModel, onHome: () -> Unit, onPractice: () -> Unit, edgePadding: Dp) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val uriHandler = LocalUriHandler.current
    val unknowns = remember(state.page) { BookStatsCalculator.calculate(state.items).distinctUnknowns }
    val last = state.isLastPage
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        val source = state.book?.sourceUri
        if (!source.isNullOrBlank()) {
            SourceChip(source) { uriHandler.openUri(source) }
        }
        // Reaches the card's edges, below the text.
        val strip = Modifier.bleed(edgePadding).drawBehind { drawLine(colors.outlineVariant, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
            .background(colors.onSurface.copy(alpha = 0.025f)).padding(horizontal = edgePadding, vertical = if (compact) 16.dp else 20.dp)
        // Next page also marks this one read, which counts its words as read; on the last page that completes the book.
        val markRemaining: @Composable (Modifier) -> Unit = { modifier ->
            if (unknowns > 0) MarkRemainingButton(unknowns, modifier) { viewModel.markPageRead(true, if (last) 0 else 1) }
        }
        // Practice asks about the words being learned, so it is offered only when the page has some.
        val practisable = remember(state.page, state.items) { state.items.any { it.isWord && it.status.isLearning } }
        val practice: @Composable (Modifier) -> Unit = { modifier -> if (practisable) PracticeButton(modifier, onPractice) }
        val next: @Composable (Modifier) -> Unit = { modifier ->
            if (last) PrimaryFooterButton(if (state.lesson == null) tr("Finish book") else tr("Finish lesson"), modifier, viewModel::finishBook) else PrimaryFooterButton(tr("Next page"), modifier) { viewModel.markPageRead(false, 1) }
        }
        Layout(
            content = {
                BackToLibrary(if (state.lesson == null) tr("Back to library") else tr("Back to course"), onHome)
                practice(Modifier)
                markRemaining(Modifier)
                next(Modifier)
            },
            modifier = strip,
        ) { measurables, constraints ->
            // One row when the buttons fit side by side; otherwise the actions stack at full width above the way back.
            val gap = 12.dp.roundToPx()
            val needed = measurables.sumOf { it.maxIntrinsicWidth(Constraints.Infinity) } + gap * (measurables.size - 1)
            val width = constraints.maxWidth
            val back = measurables.first()
            val actions = measurables.drop(1)
            if (needed <= width) {
                val placeables = measurables.map { it.measure(Constraints(maxWidth = width)) }
                val height = placeables.maxOf { it.height }
                layout(width, height) {
                    placeables.first().place(0, (height - placeables.first().height) / 2)
                    var x = width
                    for (p in placeables.drop(1).reversed()) {
                        x -= p.width
                        p.place(x, (height - p.height) / 2)
                        x -= gap
                    }
                }
            } else {
                val rowGap = 12.dp.roundToPx()
                val placeables = actions.map { it.measure(Constraints.fixedWidth(width)) } + back.measure(Constraints(maxWidth = width))
                layout(width, placeables.sumOf { it.height } + rowGap * (placeables.size - 1)) {
                    var y = 0
                    for (p in placeables) {
                        p.place(0, y)
                        y += p.height + rowGap
                    }
                }
            }
        }
    }
}

/** Widens the content by [horizontal] on each side, into its parent's padding. */
private fun Modifier.bleed(horizontal: Dp) = layout { measurable, constraints ->
    val extra = horizontal.roundToPx()
    val width = constraints.maxWidth + 2 * extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
}

@Composable
private fun BackToLibrary(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.primary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PracticeButton(modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(52.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .background(colors.primary.copy(alpha = 0.06f)).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        Icon(AppIcons.Abc, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Text(tr("Practice this page"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.primary, softWrap = false)
    }
}

@Composable
private fun MarkRemainingButton(unknowns: Int, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(52.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .background(colors.surface).clickable(onClick = onClick).padding(start = 16.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        Icon(AppIcons.DoneAll, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Text(tr("Mark remaining words as known"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.onSurface, softWrap = false)
        Text(
            "$unknowns",
            Modifier.clip(RoundedCornerShape(50)).background(colors.surfaceVariant).padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrimaryFooterButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(52.dp).clip(RoundedCornerShape(10.dp)).background(colors.primary).clickable(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.onPrimary, softWrap = false)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SourceChip(url: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val host = url.substringAfter("://").substringBefore("/").removePrefix("www.")
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(colors.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(AppIcons.Link, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        }
        Text(tr("Read the full story"), style = MaterialTheme.typography.bodyLarge, color = colors.primary, fontWeight = FontWeight.Medium)
        Box(Modifier.width(1.dp).height(20.dp).background(colors.outlineVariant))
        Text(host, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Icon(AppIcons.OpenInNew, contentDescription = tr("Open source"), tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun PanelContent(state: ReadingUiState, viewModel: ReadingViewModel, onNavigate: (Route) -> Unit) {
    when (val panel = state.panel) {
        ReadingPanel.None -> Unit
        is ReadingPanel.EditTerm -> EmbeddedTermForm(TermFormKey.ById(panel.termId, panel.sentence), "edit-${panel.termId}-${panel.version}", viewModel, onNavigate)
        is ReadingPanel.NewTerm -> EmbeddedTermForm(TermFormKey.ByText(panel.languageId, panel.text, panel.sentence), "new-${panel.languageId}-${panel.text}", viewModel, onNavigate)
        is ReadingPanel.BulkEdit -> Column(Modifier.padding(12.dp)) {
            Text(trPlural(panel.termIds.size, "Updating {0} term", "Updating {0} terms"), style = MaterialTheme.typography.titleMedium)
            BulkEditDialog(count = panel.termIds.size, onApply = viewModel::applyBulkUpdate, onDismiss = viewModel::closePanel)
        }
    }
}

@Composable
private fun EmbeddedTermForm(key: TermFormKey, keyString: String, viewModel: ReadingViewModel, onNavigate: (Route) -> Unit) {
    // Each opening gets a new form, since a form may have moved on to a looked-up word.
    val opening = remember(keyString) { Random.nextLong() }
    val formViewModel = koinViewModel<TermFormViewModel>(key = "reading-term-$keyString-$opening") { parametersOf(key) }
    CollectEvents(formViewModel.events) { event ->
        when (event) {
            is TermFormEvent.Saved -> if (event.keepOpen) viewModel.onTermChanged() else viewModel.onTermFormDone()
            TermFormEvent.Deleted -> viewModel.onTermFormDone()
            is TermFormEvent.OpenParent -> viewModel.openParentTerm(event.languageId, event.text)
            is TermFormEvent.OpenTerm -> viewModel.openTermFromExample(event.languageId, event.text, event.sentence)
        }
    }
    Column(Modifier.fillMaxSize()) {
        TermFormPanel(
            viewModel = formViewModel,
            modifier = Modifier.weight(1f),
            embedded = true,
            onClose = { formViewModel.flush(); viewModel.closePanel() },
            onDuplicateClick = { onNavigate(Route.EditTerm(it)) },
            onOpenExamples = { languageId, text -> onNavigate(Route.Examples(languageId, text)) },
            onManageDictionaries = { languageId -> onNavigate(Route.ManageDictionaries(languageId)) },
        )
    }
}
