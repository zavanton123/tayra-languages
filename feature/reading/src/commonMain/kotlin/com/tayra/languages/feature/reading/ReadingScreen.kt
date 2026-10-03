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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.LocalTranslationProblem
import com.tayra.languages.core.domain.service.SentenceTranslation
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.service.label
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
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.StatusDistributionBar
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.TextInputDialog
import com.tayra.languages.core.ui.hotkeys.HotkeyMatcher
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
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
    var confirmDeletePage by remember { mutableStateOf(false) }
    var panelFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val shift = remember { ShiftTracker() }

    CollectEvents(viewModel.events) { event ->
        when (event) {
            is ReadingEvent.CopyText -> clipboard.setText(AnnotatedString(event.text))
            is ReadingEvent.OpenUrl -> uriHandler.openUri(event.url)
            is ReadingEvent.Navigate -> onNavigate(Route.Read(event.bookId, event.page))
            ReadingEvent.BookArchived -> onHome()
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

    val menu = ReadingMenuActions(
        onEditPage = { onNavigate(Route.EditPage(bookId, state.pageNumber)) },
        onAddPageAfter = { onNavigate(Route.NewPage(bookId, state.pageNumber, after = true)) },
        onAddPageBefore = { onNavigate(Route.NewPage(bookId, state.pageNumber, after = false)) },
        onDeletePage = { confirmDeletePage = true },
        onBookmarks = { onNavigate(Route.Bookmarks(bookId)) },
        onAddBookmark = { bookmarkDialog = true },
        onTermList = { onNavigate(Route.Terms(viewModel.pageTermIds(), bookId, state.pageNumber)) },
        onTranslateSentence = { viewModel.translate(TextScope.SENTENCE) },
        onTranslatePage = { viewModel.translate(TextScope.PAGE) },
        onNextTheme = viewModel::nextTheme,
        onToggleHighlights = viewModel::toggleHighlights,
        onShortcuts = { onNavigate(Route.Shortcuts) },
        onSpeechSettings = { onNavigate(Route.Speech) },
        onSource = { state.book?.sourceUri?.let { uriHandler.openUri(it) } },
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                windowInsets = WindowInsets.safeDrawing,
            ) {
                ReadingMenu(state, viewModel, menu, onClose = { scope.launch { drawerState.close() } })
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
                    AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, section = NavSection.BOOKS)
                    ReaderToolbar(state, viewModel, onMenu = { scope.launch { drawerState.open() } }, onHome = onHome)
                }
            }
            Row(Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (state.settings.focusMode) 0f else 0.3f))) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    ReadingBody(state, viewModel, speaker, continuous, onHome = onHome, onSettings = { onNavigate(Route.OfflineTranslation) }, focusText = { runCatching { focusRequester.requestFocus() } })
                }
                if (wide && state.panel != ReadingPanel.None) {
                    Surface(
                        Modifier.width(420.dp).fillMaxHeight().padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
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
            title = "Add bookmark",
            label = "Title",
            onConfirm = { viewModel.addBookmark(it.trim()); bookmarkDialog = false },
            onDismiss = { bookmarkDialog = false },
        )
    }
    if (confirmDeletePage) {
        ConfirmDialog(
            title = "Delete current page?",
            text = "Page ${state.pageNumber} will be removed.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { viewModel.deleteCurrentPage(); confirmDeletePage = false },
            onDismiss = { confirmDeletePage = false },
        )
    }
}

private class ReadingMenuActions(
    val onEditPage: () -> Unit,
    val onAddPageAfter: () -> Unit,
    val onAddPageBefore: () -> Unit,
    val onDeletePage: () -> Unit,
    val onBookmarks: () -> Unit,
    val onAddBookmark: () -> Unit,
    val onTermList: () -> Unit,
    val onTranslateSentence: () -> Unit,
    val onTranslatePage: () -> Unit,
    val onNextTheme: () -> Unit,
    val onToggleHighlights: () -> Unit,
    val onShortcuts: () -> Unit,
    val onSource: () -> Unit,
    val onSpeechSettings: () -> Unit,
)

@Composable
private fun ReadingMenu(state: ReadingUiState, viewModel: ReadingViewModel, actions: ReadingMenuActions, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val prefs = state.settings
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Otter, contentDescription = null, tint = colors.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Reader settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(state.book?.title.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1)
            }
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceVariant.copy(alpha = 0.6f)).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Close, contentDescription = "Close menu", modifier = Modifier.size(20.dp)) }
        }

        MenuSection("Reading")
        SwitchRow(AppIcons.Fullscreen, "Focus mode", prefs.focusMode) { viewModel.toggleFocusMode() }
        SwitchRow(AppIcons.Palette, "Highlight terms", prefs.showHighlights) { viewModel.toggleHighlights() }
        SwitchRow(AppIcons.LineSpacing, "One sentence per line", prefs.splitSentences) { viewModel.toggleSplitSentences() }
        SwitchRow(AppIcons.Translate, "Show translations", prefs.showTranslations) { viewModel.toggleShowTranslations() }
        SwitchRow(AppIcons.ViewColumn, "Translations side by side", prefs.sideBySideTranslations) { viewModel.toggleSideBySideTranslations() }
        EngineRow(
            selected = prefs.translationEngine,
            options = viewModel.availableEngines,
            localTranslatorName = viewModel.localTranslatorName,
            keyed = setOfNotNull(
                TranslationEngine.GOOGLE.takeIf { prefs.googleTranslateApiKey.isNotBlank() },
                TranslationEngine.AZURE.takeIf { prefs.azureTranslatorApiKey.isNotBlank() },
                TranslationEngine.ALIBABA.takeIf { prefs.alibabaAccessKeyId.isNotBlank() && prefs.alibabaAccessKeySecret.isNotBlank() },
                TranslationEngine.BAIDU.takeIf { prefs.baiduAppId.isNotBlank() && prefs.baiduSecretKey.isNotBlank() },
                TranslationEngine.DEEPL.takeIf { prefs.deeplApiKey.isNotBlank() },
                TranslationEngine.QWEN.takeIf { prefs.qwenApiKey.isNotBlank() },
            ),
            onSelect = viewModel::setTranslationEngine,
        )
        MenuRow(Icons.Default.Refresh, "Clear translation cache") { onClose(); viewModel.clearTranslationCache() }

        SpeechSection(state, viewModel, onSettings = { onClose(); actions.onSpeechSettings() })

        MenuSection("Typography")
        AdjustRow(AppIcons.FormatSize, "Font size", "${(prefs.readingFontScale * 100).toInt()}%", onLess = { viewModel.adjustFontScale(-0.1f) }, onMore = { viewModel.adjustFontScale(0.1f) })
        AdjustRow(AppIcons.LineSpacing, "Line height", "${(prefs.readingLineHeight * 10).toInt() / 10f}", onLess = { viewModel.adjustLineHeight(-0.1f) }, onMore = { viewModel.adjustLineHeight(0.1f) })
        AdjustRow(AppIcons.OpenInFull, "Text width", "${prefs.readingColumnWidth}", onLess = { viewModel.adjustColumnWidth(-80) }, onMore = { viewModel.adjustColumnWidth(80) })

        MenuSection("Page")
        MenuRow(AppIcons.Page, "Edit current page") { onClose(); actions.onEditPage() }
        MenuRow(AppIcons.PageAdd, "Add page after") { onClose(); actions.onAddPageAfter() }
        MenuRow(AppIcons.PageAdd, "Add page before") { onClose(); actions.onAddPageBefore() }
        MenuRow(Icons.Default.Delete, "Delete current page", destructive = true) { onClose(); actions.onDeletePage() }

        MenuSection("Bookmarks")
        MenuRow(AppIcons.Bookmark, "List bookmarks") { onClose(); actions.onBookmarks() }
        MenuRow(AppIcons.BookmarkAdd, "Add bookmark") { onClose(); actions.onAddBookmark() }

        MenuSection("Language tools")
        MenuRow(Icons.AutoMirrored.Filled.List, "Term list for this page") { onClose(); actions.onTermList() }
        MenuRow(AppIcons.Translate, "Translate sentence") { onClose(); actions.onTranslateSentence() }
        MenuRow(AppIcons.Page, "Translate page") { onClose(); actions.onTranslatePage() }

        MenuSection("More")
        MenuRow(AppIcons.Palette, "Next theme") { onClose(); actions.onNextTheme() }
        MenuRow(AppIcons.Keyboard, "Keyboard shortcuts") { onClose(); actions.onShortcuts() }
        if (!state.book?.sourceUri.isNullOrBlank()) MenuRow(AppIcons.Link, "Show source URL") { onClose(); actions.onSource() }
    }
}

@Composable
private fun MenuSection(label: String) {
    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun MenuIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = if (destructive) colors.error else colors.primary
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (destructive) colors.error.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MenuIcon(icon, tint)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (destructive) colors.error else colors.onSurface)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.outline, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AdjustRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onLess: () -> Unit, onMore: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        MenuIcon(icon, colors.primary)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        StepButton("−", onLess)
        StepButton("+", onMore)
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(colors.primary.copy(alpha = 0.1f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.titleMedium, color = colors.primary) }
}

/** The translation engine, picked from a menu anchored to the row; engines that need a key are offered once one is set. */
@Composable
private fun EngineRow(
    selected: TranslationEngine,
    options: List<TranslationEngine>,
    localTranslatorName: String?,
    keyed: Set<TranslationEngine>,
    onSelect: (TranslationEngine) -> Unit,
) {
    fun usable(engine: TranslationEngine) = engine in setOf(TranslationEngine.MYMEMORY, TranslationEngine.ARGOS) || engine in keyed
    fun name(engine: TranslationEngine) = engine.label(localTranslatorName)
    ChoiceRow(
        icon = AppIcons.Globe,
        title = "Translation engine",
        value = name(selected),
        options = options,
        optionLabel = { if (usable(it)) name(it) else "${name(it)} \u2013 add a key in Settings" },
        optionEnabled = ::usable,
        onSelect = onSelect,
    )
}

/** The Speech screen's everyday settings: the play buttons, speaking clicked words, the engine, the voice for this book's language and the speed. */
@Composable
private fun SpeechSection(state: ReadingUiState, viewModel: ReadingViewModel, onSettings: () -> Unit) {
    val prefs = state.settings
    val voices by viewModel.speechVoices.collectAsState()
    LaunchedEffect(prefs.speechEngine, state.language?.id) { viewModel.loadSpeechVoices() }
    val local = prefs.speechEngine != SpeechEngine.SYSTEM && prefs.speechEngine in viewModel.speechEngines
    val languageName = state.language?.name.orEmpty()

    MenuSection("Speech")
    SwitchRow(AppIcons.VolumeUp, "Play audio", prefs.showSentencePlay) { viewModel.toggleSentencePlay() }
    SwitchRow(AppIcons.Abc, "Speak word on click", prefs.speakWordOnClick) { viewModel.toggleSpeakWordOnClick() }
    ChoiceRow(
        icon = AppIcons.VolumeUp,
        title = "Speech engine",
        value = (prefs.speechEngine.takeIf { it in viewModel.speechEngines } ?: SpeechEngine.SYSTEM).label,
        options = viewModel.speechEngines,
        optionLabel = { it.label },
        onSelect = viewModel::setSpeechEngine,
    )
    if (local) {
        if (voices.isNotEmpty()) {
            val chosen = prefs.speechVoices["${prefs.speechEngine.name}:${viewModel.speechLanguage}"]
            ChoiceRow(
                icon = AppIcons.RecordVoiceOver,
                title = if (languageName.isEmpty()) "Voice" else "$languageName voice",
                value = (voices.firstOrNull { it.id == chosen } ?: voices.first()).name,
                options = voices,
                optionLabel = { it.name },
                onSelect = { viewModel.setSpeechVoice(it.id) },
            )
        } else {
            Text(
                "No voice for $languageName is downloaded, so the system voice reads this book. Download one in Speech settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        AdjustRow(AppIcons.Speed, "Speed", "${(prefs.speechSpeed * 100).roundToInt()}%", onLess = { viewModel.adjustSpeechSpeed(-0.1f) }, onMore = { viewModel.adjustSpeechSpeed(0.1f) })
    }
    MenuRow(AppIcons.Tune, "Speech settings", onClick = onSettings)
}

/** A setting picked from a menu anchored to the row, showing the current [value] under the [title]. */
@Composable
private fun <T> ChoiceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    optionEnabled: (T) -> Boolean = { true },
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Box {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { open = true }.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MenuIcon(icon, colors.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(value, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Icon(AppIcons.UnfoldMore, contentDescription = null, tint = colors.outline, modifier = Modifier.size(20.dp))
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                AppMenuItem(text = { Text(optionLabel(option)) }, onClick = { open = false; onSelect(option) }, enabled = optionEnabled(option))
            }
        }
    }
}

@Composable
private fun SwitchRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MenuIcon(icon, MaterialTheme.colorScheme.primary)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun ReadingHeader(state: ReadingUiState, viewModel: ReadingViewModel, onMenu: () -> Unit, onHome: () -> Unit) {
    val compact = LocalWindowWidth.current.isCompact
    Surface(tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                IconButton(onClick = onHome) { Icon(Icons.Default.Home, contentDescription = "Home") }
                Text(
                    state.book?.title.orEmpty(),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = if (state.language?.rightToLeft == true) TextDirection.Rtl else TextDirection.Ltr),
                    maxLines = 1,
                )
                Text("${state.pageNumber}/${state.pageCount}", style = MaterialTheme.typography.labelLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.goToRelativePage(-1) }, enabled = !state.isFirstPage) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
                }
                if (state.pageCount > 1) {
                    var sliderValue by remember(state.pageNumber) { mutableStateOf(state.pageNumber.toFloat()) }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { viewModel.goToPage(sliderValue.toInt()) },
                        valueRange = 1f..state.pageCount.toFloat(),
                        steps = (state.pageCount - 2).coerceAtLeast(0),
                        modifier = Modifier.weight(1f).padding(horizontal = if (compact) 4.dp else 16.dp),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                IconButton(onClick = { viewModel.goToRelativePage(1) }, enabled = !state.isLastPage) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
                }
            }
        }
    }
}

/** Wide-screen toolbar: breadcrumb on the left, the pager in the middle and the page slider on the right. */
@Composable
private fun ReaderToolbar(state: ReadingUiState, viewModel: ReadingViewModel, onMenu: () -> Unit, onHome: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val rtl = state.language?.rightToLeft == true
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onHome).padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = colors.onSurfaceVariant, modifier = Modifier.padding(end = 2.dp))
                Text("Books", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            Text("/", style = MaterialTheme.typography.bodyLarge, color = colors.outline, modifier = Modifier.padding(horizontal = 8.dp))
            Text(
                state.book?.title.orEmpty(),
                Modifier.weight(1f, fill = false).padding(end = 16.dp),
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PagerButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous page", enabled = !state.isFirstPage) { viewModel.goToRelativePage(-1) }
            Text("Page ${state.pageNumber} of ${state.pageCount}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            PagerButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next page", enabled = !state.isLastPage) { viewModel.goToRelativePage(1) }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (state.pageCount > 1) {
                var sliderValue by remember(state.pageNumber) { mutableStateOf(state.pageNumber.toFloat()) }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { viewModel.goToPage(sliderValue.toInt()) },
                    valueRange = 1f..state.pageCount.toFloat(),
                    steps = (state.pageCount - 2).coerceAtLeast(0),
                    modifier = Modifier.widthIn(max = 280.dp).padding(start = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun PagerButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(6.dp),
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) colors.onSurface else colors.outlineVariant)
    }
}

/** Minimal header shown in focus mode: just the menu, the page position and a way out. */
@Composable
private fun FocusBar(state: ReadingUiState, viewModel: ReadingViewModel, onMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { viewModel.goToRelativePage(-1) }, enabled = !state.isFirstPage) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
        }
        Text("${state.pageNumber}/${state.pageCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = { viewModel.goToRelativePage(1) }, enabled = !state.isLastPage) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
        }
        TextButton(onClick = viewModel::toggleFocusMode) { Text("Exit focus") }
    }
}

@Composable
private fun ReadingBody(
    state: ReadingUiState,
    viewModel: ReadingViewModel,
    speaker: Speaker,
    continuous: ContinuousReading,
    onHome: () -> Unit,
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
            .padding(horizontal = edgePadding, vertical = if (compact) 16.dp else 28.dp)
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
                splitSentences = state.settings.splitSentences,
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
                Text("Long-press the last word of the expression, or tap to cancel.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable { viewModel.cancelSelection() })
            }
            if (!state.settings.focusMode) ReadingFooter(state, viewModel, onHome)
        }
        Spacer(Modifier.height(120.dp))
    }
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
    val rows = listOf(
        "Play / pause" to listOf(HotkeyAction.LISTEN_PLAY_PAUSE),
        "Previous sentence" to listOf(HotkeyAction.LISTEN_PREVIOUS, HotkeyAction.LISTEN_PREVIOUS_ARROW),
        "Next sentence" to listOf(HotkeyAction.LISTEN_NEXT, HotkeyAction.LISTEN_NEXT_ARROW),
        "Repeat sentence" to listOf(HotkeyAction.LISTEN_REPEAT, HotkeyAction.LISTEN_REPEAT_ARROW),
        "Pause / resume" to listOf(HotkeyAction.LISTEN_PAUSE, HotkeyAction.LISTEN_PAUSE_ARROW),
        "Mark word as known" to listOf(HotkeyAction.STATUS_WELL_KNOWN),
        "Mark word as unknown" to listOf(HotkeyAction.DELETE_TERM),
        "Show / hide the term pane" to listOf(HotkeyAction.TOGGLE_TERM_PANE),
        "Next coloured word" to listOf(HotkeyAction.NEXT_COLORED_WORD),
        "Previous coloured word" to listOf(HotkeyAction.PREV_COLORED_WORD),
        "Larger / smaller text" to listOf(HotkeyAction.TEXT_LARGER, HotkeyAction.TEXT_SMALLER),
        "More / less line height" to listOf(HotkeyAction.LINES_FURTHER, HotkeyAction.LINES_CLOSER),
        "Text size / line height back to default" to listOf(HotkeyAction.TEXT_RESET, HotkeyAction.LINES_RESET),
        "Auto-pause on / off" to listOf(HotkeyAction.LISTEN_AUTO_PAUSE),
        "Next page" to listOf(HotkeyAction.NEXT_PAGE),
        "Previous page" to listOf(HotkeyAction.PREVIOUS_PAGE),
    ).mapNotNull { (label, actions) -> actions.mapNotNull { hotkeys[it] }.takeIf { it.isNotEmpty() }?.let { label to it } }
    // Keys for one action are alternatives ("or"); a "this / that" row pairs two opposite actions.
    fun separator(label: String) = if (" / " in label && !label.startsWith("Pause")) "/" else "or"
    Surface(shape = RoundedCornerShape(14.dp), color = colors.surface, shadowElevation = 8.dp, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(Modifier.padding(16.dp).width(440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Keyboard shortcuts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            rows.forEach { (label, keys) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.width(250.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        keys.forEachIndexed { index, key ->
                            if (index > 0) Text(separator(label), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            KeyCap(key)
                        }
                    }
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                "Ctrl (⌘ on a Mac) with the arrows moves between words and changes a word's status; K and U need a selected word.",
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
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (hotkeys != null) {
            Box {
                Box(
                    Modifier.size(CONTROL_SIZE).clip(CircleShape).background(colors.surface)
                        .border(CONTROL_BORDER, colors.primary, CircleShape)
                        .clickable { showShortcuts = !showShortcuts }
                        .semantics { contentDescription = "Keyboard shortcuts" },
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
                .semantics { contentDescription = if (playing) "Pause reading" else "Read the page" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) AppIcons.Pause else AppIcons.PlayArrow, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(32.dp))
        }
        Box(
            Modifier.size(CONTROL_SIZE).clip(CircleShape)
                .background(if (autoPause) colors.primary else colors.surface)
                .border(CONTROL_BORDER, colors.primary, CircleShape)
                .clickable(onClick = onAutoPause)
                .semantics { contentDescription = if (autoPause) "Auto-pause on" else "Auto-pause off" },
            contentAlignment = Alignment.Center,
        ) {
            Text("AP", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (autoPause) colors.onPrimary else colors.primary)
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
                if (failed) "Offline translation failed: ${problem.message}" else problem.message,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = if (failed) colors.onErrorContainer else colors.onSecondaryContainer,
            )
            when (problem) {
                is LocalTranslationProblem.ModelMissing -> Button(onClick = viewModel::installOfflineModels) { Text("Install") }
                is LocalTranslationProblem.NoModel -> {
                    Text("Offline", style = MaterialTheme.typography.bodySmall, color = colors.onSecondaryContainer)
                    Switch(checked = true, onCheckedChange = { on -> if (!on) viewModel.useOnlineEngine() })
                }
                is LocalTranslationProblem.Failed -> OutlinedButton(onClick = viewModel::retryOfflineTranslation) { Text("Try again") }
            }
            TextButton(onClick = onSettings) { Text("Settings") }
        }
    }
}

@Composable
private fun TranslationProgress(message: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Setting up offline translation: $message", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PageVocabulary(state: ReadingUiState) {
    val stats = remember(state.page) { BookStatsCalculator.calculate(state.items) }
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Vocabulary on this page", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        StatusDistributionBar(stats, Modifier.widthIn(max = 360.dp).weight(1f, fill = false).fillMaxWidth())
        Text(if (stats.distinctTerms > 0) "${stats.unknownPercent}% new" else "—", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun ReadingFooter(state: ReadingUiState, viewModel: ReadingViewModel, onHome: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    val uriHandler = LocalUriHandler.current
    val unknowns = remember(state.page) { BookStatsCalculator.calculate(state.items).distinctUnknowns }
    val last = state.isLastPage
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        val source = state.book?.sourceUri
        if (!source.isNullOrBlank()) {
            SourceChip(source) { uriHandler.openUri(source) }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.primary.copy(alpha = 0.05f))
                .border(1.dp, colors.primary.copy(alpha = 0.15f), RoundedCornerShape(14.dp)).padding(if (compact) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF16A34A).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(30.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (last) "You\u2019ve reached the end" else "End of page ${state.pageNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (last) "Review the remaining words, then mark this page as complete." else "Review the remaining words, then continue to the next page.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            val known = @Composable { modifier: Modifier ->
                ActionCard(
                    modifier = modifier,
                    title = "Mark remaining words as known",
                    subtitle = "$unknowns unknown word${if (unknowns == 1) "" else "s"}${if (last) "" else " \u00b7 then next page"}",
                    filled = false,
                    onClick = { viewModel.markPageRead(true, if (last) 0 else 1) },
                )
            }
            val read = @Composable { modifier: Modifier ->
                ActionCard(
                    modifier = modifier,
                    title = "Mark page as read",
                    subtitle = if (last) "Save your reading progress" else "Continue to page ${state.pageNumber + 1}",
                    filled = true,
                    onClick = { viewModel.markPageRead(false, if (last) 0 else 1) },
                )
            }
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { known(Modifier.fillMaxWidth()); read(Modifier.fillMaxWidth()) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { known(Modifier.weight(1f)); read(Modifier.weight(1f)) }
            }
            HorizontalDivider(color = colors.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).background(colors.surface)
                        .clickable(onClick = onHome).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Text("Back to library", style = MaterialTheme.typography.bodyLarge, color = colors.primary, fontWeight = FontWeight.Medium)
                }
                val archived = state.book?.archived == true
                if (last || archived) {
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = if (archived) viewModel::unarchiveBook else viewModel::archiveBook)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(AppIcons.Book, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Text(if (archived) "Unarchive book" else "Archive book", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (!compact) Text("Page ${state.pageNumber} of ${state.pageCount}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
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
        Text("Read the full story", style = MaterialTheme.typography.bodyLarge, color = colors.primary, fontWeight = FontWeight.Medium)
        Box(Modifier.width(1.dp).height(20.dp).background(colors.outlineVariant))
        Text(host, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Icon(AppIcons.OpenInNew, contentDescription = "Open source", tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ActionCard(modifier: Modifier, title: String, subtitle: String, filled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val foreground = if (filled) colors.onPrimary else colors.primary
    Row(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(if (filled) colors.primary else colors.surface)
            .border(1.dp, if (filled) colors.primary else colors.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = foreground, modifier = Modifier.size(28.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (filled) colors.onPrimary else colors.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = if (filled) colors.onPrimary.copy(alpha = 0.8f) else colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun PanelContent(state: ReadingUiState, viewModel: ReadingViewModel, onNavigate: (Route) -> Unit) {
    when (val panel = state.panel) {
        ReadingPanel.None -> Unit
        is ReadingPanel.EditTerm -> EmbeddedTermForm(TermFormKey.ById(panel.termId), "edit-${panel.termId}-${panel.version}", viewModel, onNavigate)
        is ReadingPanel.NewTerm -> EmbeddedTermForm(TermFormKey.ByText(panel.languageId, panel.text), "new-${panel.languageId}-${panel.text}", viewModel, onNavigate)
        is ReadingPanel.BulkEdit -> Column(Modifier.padding(12.dp)) {
            Text("Updating ${panel.termIds.size} term(s)", style = MaterialTheme.typography.titleMedium)
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
