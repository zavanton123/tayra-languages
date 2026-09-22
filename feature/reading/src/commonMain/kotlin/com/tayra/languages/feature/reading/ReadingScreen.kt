package com.tayra.languages.feature.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.ui.components.ConfirmDialog
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

    CollectEvents(viewModel.events) { event ->
        when (event) {
            is ReadingEvent.CopyText -> clipboard.setText(AnnotatedString(event.text))
            is ReadingEvent.OpenUrl -> uriHandler.openUri(event.url)
            is ReadingEvent.Navigate -> onNavigate(Route.Read(event.bookId, event.page))
            ReadingEvent.BookArchived -> onHome()
        }
    }
    LaunchedEffect(state.loading) { if (!state.loading) runCatching { focusRequester.requestFocus() } }

    val hotkeys = state.settings.hotkeys
    val rtl = state.language?.rightToLeft == true
    val nextIncrement = if (rtl) -1 else 1

    fun handleAction(action: HotkeyAction): Boolean {
        when (action) {
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
            HotkeyAction.STATUS_5 -> viewModel.setStatus(TermStatus.LEARNED)
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
        onSource = { state.book?.sourceUri?.let { uriHandler.openUri(it) } },
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ReadingMenu(state, viewModel, menu, onClose = { scope.launch { drawerState.close() } })
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(theme.readingBackground)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || panelFocused || state.items.isEmpty()) return@onKeyEvent false
                    val pressed = HotkeyMatcher.fromEvent(event) ?: return@onKeyEvent false
                    val action = hotkeys.entries.firstOrNull { it.value == pressed }?.key ?: return@onKeyEvent false
                    handleAction(action)
                },
        ) {
            ReadingHeader(state, viewModel, onMenu = { scope.launch { drawerState.open() } }, onHome = onHome)
            Row(Modifier.weight(1f)) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    ReadingBody(state, viewModel, onHome = onHome, focusText = { runCatching { focusRequester.requestFocus() } })
                }
                if (wide && state.panel != ReadingPanel.None) {
                    Surface(Modifier.width(400.dp).fillMaxHeight().onFocusChanged { panelFocused = it.hasFocus }, tonalElevation = 2.dp) {
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
)

@Composable
private fun ReadingMenu(state: ReadingUiState, viewModel: ReadingViewModel, actions: ReadingMenuActions, onClose: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(state.book?.title.orEmpty(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close menu") }
        }
        SwitchRow("Focus mode", state.settings.focusMode) { viewModel.toggleFocusMode() }
        SwitchRow("Quick set status (tap unknown → 1)", state.settings.tapSetsStatus) { viewModel.toggleTapSetsStatus() }
        SwitchRow("Highlight terms", state.settings.showHighlights) { viewModel.toggleHighlights() }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { viewModel.adjustFontScale(-0.1f) }) { Text("A-") }
            OutlinedButton(onClick = { viewModel.adjustFontScale(0.1f) }) { Text("A+") }
            OutlinedButton(onClick = { viewModel.adjustLineHeight(-0.1f) }) { Text("↕-") }
            OutlinedButton(onClick = { viewModel.adjustLineHeight(0.1f) }) { Text("↕+") }
            OutlinedButton(onClick = { viewModel.adjustColumnWidth(-80) }) { Text("↔-") }
            OutlinedButton(onClick = { viewModel.adjustColumnWidth(80) }) { Text("↔+") }
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        fun item(label: String, action: () -> Unit) {
            // helper for terse menu declarations below
        }
        val entries = buildList<Pair<String, () -> Unit>> {
            if (!state.book?.sourceUri.isNullOrBlank()) add("Show source URL" to actions.onSource)
            add("Edit current page" to actions.onEditPage)
            add("Add page after" to actions.onAddPageAfter)
            add("Add page before" to actions.onAddPageBefore)
            add("Delete current page" to actions.onDeletePage)
            add("List bookmarks" to actions.onBookmarks)
            add("Add bookmark" to actions.onAddBookmark)
            add("Term list for this page" to actions.onTermList)
            add("Translate sentence" to actions.onTranslateSentence)
            add("Translate page" to actions.onTranslatePage)
            add("Next theme" to actions.onNextTheme)
            add("Keyboard shortcuts" to actions.onShortcuts)
        }
        entries.forEach { (label, action) ->
            NavigationDrawerItem(label = { Text(label) }, selected = false, onClick = { onClose(); action() })
        }
        @Suppress("UNUSED_VARIABLE") val unused = ::item
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
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

@Composable
private fun ReadingBody(state: ReadingUiState, viewModel: ReadingViewModel, onHome: () -> Unit, focusText: () -> Unit) {
    val theme = TayraTheme.current
    if (state.loading) {
        LoadingIndicator()
        return
    }
    if (state.error != null) {
        ErrorMessage(state.error, Modifier.padding(16.dp))
        return
    }
    val callbacks = remember(viewModel) {
        ReadingTextCallbacks(
            onClick = { index, shift -> viewModel.onWordClick(index, shift); focusText() },
            onTap = { index -> viewModel.onWordTap(index) },
            onLongPress = { index ->
                val tokenIndex = state.items[index].index
                if (viewModel.state.value.selection == null) viewModel.startSelection(tokenIndex) else viewModel.endSelection(tokenIndex, copy = false)
            },
            onHover = viewModel::onHover,
            onDragStart = { index -> viewModel.startSelection(state.items[index].index) },
            onDrag = { index -> viewModel.updateSelection(state.items[index].index) },
            onDragEnd = { index, shift -> viewModel.endSelection(state.items[index].index, copy = shift) },
            popupContent = { _, anchor ->
                state.popup?.let { popup ->
                    Popup(offset = anchor, onDismissRequest = viewModel::hidePopup) { TermPopupCard(popup.popup) }
                }
            },
        )
    }
    val scrollState = rememberScrollState()
    LaunchedEffect(state.pageNumber) { scrollState.scrollTo(0) }
    Column(Modifier.fillMaxSize().verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = state.settings.readingColumnWidth.dp).padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (state.pageNumber == 1) {
                Text(
                    state.book?.title.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall.copy(color = theme.readingText, textDirection = if (state.language?.rightToLeft == true) TextDirection.Rtl else TextDirection.Ltr),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
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
                callbacks = callbacks,
            )
            if (state.selection != null) {
                Text("Long-press the last word of the expression, or tap to cancel.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable { viewModel.cancelSelection() })
            }
            ReadingFooter(state, viewModel, onHome)
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun ReadingFooter(state: ReadingUiState, viewModel: ReadingViewModel, onHome: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!state.isLastPage) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = { viewModel.markPageRead(true, 1) }) { Text("✔ Mark rest as known, next page", color = MaterialTheme.colorScheme.tertiary) }
                TextButton(onClick = { viewModel.markPageRead(false, 1) }) { Text("Mark page read, next page ›") }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = { viewModel.markPageRead(true, 0) }) { Text("✔ Mark rest as known", color = MaterialTheme.colorScheme.tertiary) }
                TextButton(onClick = { viewModel.markPageRead(false, 0) }) { Text("✔ Mark page read") }
            }
            Text("🎉", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::archiveBook) { Text("Archive book") }
                OutlinedButton(onClick = onHome) { Text("Home") }
            }
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
            BulkEditDialog(tags = emptyList(), count = panel.termIds.size, onApply = viewModel::applyBulkUpdate, onDismiss = viewModel::closePanel)
        }
    }
}

@Composable
private fun EmbeddedTermForm(key: TermFormKey, keyString: String, viewModel: ReadingViewModel, onNavigate: (Route) -> Unit) {
    val formViewModel = koinViewModel<TermFormViewModel>(key = "reading-term-$keyString") { parametersOf(key) }
    CollectEvents(formViewModel.events) { event ->
        when (event) {
            is TermFormEvent.Saved, TermFormEvent.Deleted -> viewModel.onTermFormDone()
            is TermFormEvent.OpenParent -> viewModel.openParentTerm(event.languageId, event.text)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = viewModel::closePanel) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
        TermFormPanel(viewModel = formViewModel, modifier = Modifier.weight(1f), embedded = true, onDuplicateClick = { onNavigate(Route.EditTerm(it)) })
    }
}
