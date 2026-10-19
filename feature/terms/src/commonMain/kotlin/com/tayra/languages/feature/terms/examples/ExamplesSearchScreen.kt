package com.tayra.languages.feature.terms.examples

import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.theme.ReadingFont
import com.tayra.languages.core.ui.theme.fontFamily
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import org.koin.compose.koinInject
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.ui.components.HoverTranslatedText
import androidx.compose.foundation.focusable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.hotkeys.HotkeyMatcher
import com.tayra.languages.core.ui.theme.TayraTheme
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.YesNo
import com.tayra.languages.core.ui.components.AppBottomSheet
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Crumb
import com.tayra.languages.core.ui.components.EmptyMessage
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LabeledField
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.SidePanel
import com.tayra.languages.core.ui.components.SliderSetting
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.navigation.Route
import io.ktor.http.encodeURLParameter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.tayra.languages.feature.terms.form.TermFormViewModel
import kotlin.random.Random
import com.tayra.languages.feature.terms.form.TermFormPanel
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.form.TermFormEvent
import com.tayra.languages.core.ui.state.CollectEvents
import androidx.compose.foundation.layout.fillMaxHeight
import com.tayra.languages.core.ui.components.ScrollList

/**
 * Searches Tatoeba example sentences with every filter the API offers. Wide windows show the
 * term pane beside the results, for the searched term or a word clicked in an example; on
 * narrower ones a clicked word opens it in a sheet.
 */
@Composable
fun ExamplesSearchScreen(
    languageId: Long,
    text: String,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: ExamplesSearchViewModel = koinViewModel(key = "examples-$languageId-$text") { parametersOf(languageId, text) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResumed() }
    val query = state.query
    val audio = rememberExampleAudio()
    audio.PrepareSpeech(state.results, state.language?.let { LanguageCodes.codeFor(it.name) })
    val compact = LocalWindowWidth.current.isCompact
    val wide = LocalWindowWidth.current.isExpanded
    var sheetOpen by remember { mutableStateOf(false) }
    val openWord: (String, String) -> Unit = { word, sentence ->
        viewModel.openTerm(word, sentence)
        if (!wide) sheetOpen = true
    }
    val wordTranslations = koinInject<WordTranslationService>()
    val translateWord: suspend (String) -> String? = { word -> state.language?.let { wordTranslations.translate(it, word) } }

    val prefs by koinInject<SettingsRepository>().settings.collectAsStateWithLifecycle()
    val showHighlights = prefs.showHighlights
    var textSettingsOpen by rememberSaveable { mutableStateOf(false) }
    // Status shortcuts work while the results, not a text field, have the keyboard.
    val focus = remember { FocusRequester() }
    var listFocused by remember { mutableStateOf(false) }
    val focusList: () -> Unit = { runCatching { focus.requestFocus() } }
    /** The word under the mouse and the example it is in. */
    var hovered by remember { mutableStateOf<Pair<String, String>?>(null) }
    var paneForm by remember { mutableStateOf<TermFormViewModel?>(null) }
    // A status set on a result's word may be the pane's term (or its family): the pane reads it again.
    LaunchedEffect(state.termsChanged) { if (state.termsChanged > 0) paneForm?.refresh() }
    // The result the keys act on, marked on screen; none until a key picks one, and a new search starts over.
    var activeIndex by rememberSaveable { mutableStateOf(-1) }
    // Counts the plays of the marked result, which bring it back on screen after the list was scrolled away from it.
    var reveals by remember { mutableStateOf(0) }
    LaunchedEffect(state.results) { if (activeIndex >= state.results.size) activeIndex = -1 }
    /** Marks the next or previous result and plays it; at either end the mark stays where it is, unplayed. */
    fun moveActive(delta: Int) {
        if (state.results.isEmpty() || query == null) return
        val next = if (activeIndex < 0) 0 else (activeIndex + delta).coerceIn(0, state.results.lastIndex)
        if (next == activeIndex) return
        activeIndex = next
        reveals++
        audio.play(state.results[next], LanguageCodes.codeFor(query.language.name))
    }
    fun playActive() {
        if (state.results.isEmpty() || query == null) return
        if (activeIndex < 0) activeIndex = 0
        reveals++
        audio.toggle(state.results[activeIndex], LanguageCodes.codeFor(query.language.name))
    }

    /** Applies a status shortcut to the word under the mouse, or else to the pane's term; [status] null means a step of [delta]. */
    fun applyStatus(status: TermStatus?, delta: Int = 0): Boolean {
        val (word, sentence) = hovered ?: state.paneTerm?.let { it to state.paneSentence } ?: return false
        val form = paneForm
        // The pane's own term changes through its form, which holds its unsaved edits.
        if (form != null && state.paneTerm.equals(word, ignoreCase = true)) {
            form.setStatus(status ?: TermStatus.shifted(form.state.value.draft.status, delta))
        } else if (status != null) {
            viewModel.setStatus(word, sentence, status)
        } else {
            viewModel.shiftStatus(word, sentence, delta)
        }
        return true
    }

    Scaffold(
        topBar = { AppTopBar(title = tr("Examples"), onNavigate = onNavigate, section = NavSection.TERMS, onBack = if (compact) onBack else null) },
    ) { padding ->
        if (state.loading || query == null) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        val direction = if (query.language.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
        val gutter = if (compact) AppSpacing.l else AppSpacing.xxxl
        Row(
            Modifier.padding(padding).fillMaxSize()
                .focusRequester(focus)
                .onFocusChanged { listFocused = it.isFocused }
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !listFocused) return@onPreviewKeyEvent false
                    // The results are walked and heard as the reader's sentences are: A and D (or ← and →) move to the next and play it, W, S, Space, ↑ and ↓ play or stop the marked one.
                    if (!event.isCtrlPressed && !event.isMetaPressed && !event.isAltPressed) {
                        when (event.key) {
                            Key.A, Key.DirectionLeft -> { moveActive(-1); return@onPreviewKeyEvent true }
                            Key.D, Key.DirectionRight -> { moveActive(1); return@onPreviewKeyEvent true }
                            Key.W, Key.S, Key.Spacebar, Key.DirectionUp, Key.DirectionDown -> { playActive(); return@onPreviewKeyEvent true }
                        }
                    }
                    val pressed = HotkeyMatcher.fromEvent(event) ?: return@onPreviewKeyEvent false
                    when (HotkeyAction.resolve(viewModel.hotkeys, pressed, wordSelected = true, listening = false)) {
                        HotkeyAction.STATUS_1 -> applyStatus(TermStatus.NEW_1)
                        HotkeyAction.STATUS_2 -> applyStatus(TermStatus.NEW_2)
                        HotkeyAction.STATUS_3 -> applyStatus(TermStatus.LEARNING_3)
                        HotkeyAction.STATUS_4 -> applyStatus(TermStatus.LEARNING_4)
                        HotkeyAction.STATUS_IGNORE -> applyStatus(TermStatus.IGNORED)
                        HotkeyAction.STATUS_WELL_KNOWN -> applyStatus(TermStatus.WELL_KNOWN)
                        HotkeyAction.DELETE_TERM -> applyStatus(TermStatus.UNKNOWN)
                        HotkeyAction.STATUS_UP -> applyStatus(null, 1)
                        HotkeyAction.STATUS_DOWN -> applyStatus(null, -1)
                        else -> false
                    }
                },
        ) {
            // Asked for here, once the row exists: the scaffold builds its content after the screen's own effects ran.
            LaunchedEffect(state.paneTerm == null) { focusList() }
            // One scrolling list holds the filters and the results so both fit on small screens.
            val listState = rememberLazyListState()
            // A marked result off the screen is not composed, so it cannot ask to be scrolled to: the list goes to it first.
            LaunchedEffect(activeIndex, reveals) {
                if (activeIndex < 0) return@LaunchedEffect
                val info = listState.layoutInfo
                val first = info.totalItemsCount - state.results.size - (if (state.hasMore) 1 else 0)
                val target = first + activeIndex
                if (first >= 0 && info.visibleItemsInfo.none { it.index == target }) listState.scrollToItem(target)
            }
            ScrollList(
                Modifier.weight(1f).fillMaxHeight()
                    // A press on the results hands them the keyboard; a text field under it takes it right back.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) focusList()
                        }
                    },
                state = listState,
                contentPadding = PaddingValues(horizontal = gutter, vertical = AppSpacing.l),
            ) {
                item { PageHeader(query, compact, onBack, onTerms = { onNavigate(Route.Terms()) }) }
                item { ErrorMessage(state.error) }
                item { SearchBar(query, viewModel, onSearch = { viewModel.search(); focusList() }) }
                item { FilterRow(query, viewModel) }
                item {
                    Row(Modifier.fillMaxWidth().padding(top = AppSpacing.l)) {
                        AppIconButton(AppIcons.FormatSize, tr("Text settings"), onClick = { textSettingsOpen = !textSettingsOpen }, style = IconButtonStyle.Outlined, tint = MaterialTheme.colorScheme.primary, selected = textSettingsOpen)
                    }
                }
                if (textSettingsOpen) item { TextSettings(prefs, viewModel) }
                item {
                    Text(
                        when {
                            state.searching -> tr("Searching...")
                            state.total != null -> trPlural(state.total!!, "{0} sentence", "{0} sentences")
                            else -> ""
                        },
                        Modifier.padding(top = AppSpacing.l, bottom = AppSpacing.m),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    state.searching -> item { LoadingIndicator(Modifier.fillMaxWidth().padding(AppSpacing.xxxl)) }
                    state.results.isEmpty() -> item { EmptyMessage(tr("No examples match these filters."), Modifier.fillMaxWidth()) }
                    else -> {
                        itemsIndexed(state.results) { index, example ->
                            ExampleCard(
                                text = emphasize(example.text, query.text)
                                    .withStatuses(state.words[example.text].orEmpty(), TayraTheme.current.statusColors, showHighlights)
                                    .withSelected(state.paneTerm, TayraTheme.current.selectedText),
                                translateWord = translateWord,
                                translation = example.translation,
                                sound = audio.soundOf(example),
                                active = index == activeIndex,
                                onPlay = { activeIndex = index; audio.toggle(example, LanguageCodes.codeFor(query.language.name)) },
                                direction = direction,
                                fontFamily = ReadingFont.byId(prefs.readingFont).fontFamily(),
                                fontScale = prefs.readingFontScale,
                                lineHeight = prefs.readingLineHeight,
                                onWordClick = { word -> openWord(word, example.text) },
                                onWordSecondaryClick = { word -> viewModel.markWord(word, example.text) },
                                onPhraseSelect = { phrase -> openWord(phrase, example.text) },
                                onHover = { word ->
                                    if (word != null) hovered = word to example.text
                                    else if (hovered?.second == example.text) hovered = null
                                },
                            )
                            Spacer(Modifier.height(AppSpacing.m))
                        }
                        if (state.hasMore) {
                            item {
                                Row(Modifier.fillMaxWidth().padding(AppSpacing.l), horizontalArrangement = Arrangement.Center) {
                                    SecondaryButton(if (state.loadingMore) tr("Loading...") else tr("Load more"), onClick = viewModel::loadMore, enabled = !state.loadingMore)
                                }
                            }
                        }
                    }
                }
            }
            val term = state.paneTerm
            if (wide && term != null) {
                SidePanel(AppSizes.sidePanel, Modifier.fillMaxHeight().padding(top = AppSpacing.l, end = AppSpacing.l, bottom = AppSpacing.l)) {
                    TermPane(query.language.id, term, state.paneSentence, onClose = viewModel::closePane, onOpenTerm = viewModel::openTerm, onTermsChanged = viewModel::refreshTerms, onForm = { paneForm = it }, onNavigate = onNavigate)
                }
            }
        }
    }

    val sheetTerm = state.paneTerm
    val sheetLanguage = state.language
    if (!wide && sheetOpen && sheetTerm != null && sheetLanguage != null) {
        val close = { sheetOpen = false }
        AppBottomSheet(onDismissRequest = close, skipPartiallyExpanded = false) {
            TermPane(sheetLanguage.id, sheetTerm, state.paneSentence, onClose = close, onOpenTerm = viewModel::openTerm, onTermsChanged = viewModel::refreshTerms, onForm = { paneForm = it }, onNavigate = onNavigate)
        }
    }
}

/**
 * The term pane for [text], as the reader shows it; each opening gets a new form, since a form
 * may have moved on to a looked-up word. An old form keeps saving on its own.
 */
@Composable
private fun TermPane(
    languageId: Long,
    text: String,
    sentence: String?,
    onClose: () -> Unit,
    /** Another term to show, with the sentence it was picked in. */
    onOpenTerm: (String, String?) -> Unit,
    /** A term was saved in the pane, so the results' colours may be out of date. */
    onTermsChanged: () -> Unit,
    /** The form showing the term, null once the pane is gone. */
    onForm: (TermFormViewModel?) -> Unit,
    onNavigate: (Route) -> Unit,
) {
    val opening = remember(languageId, text) { Random.nextLong() }
    val form = koinViewModel<TermFormViewModel>(key = "examples-term-$languageId-$text-$opening") { parametersOf(TermFormKey.ByText(languageId, text, sentence)) }
    DisposableEffect(form) {
        onForm(form)
        onDispose { onForm(null) }
    }
    CollectEvents(form.events) { event ->
        when (event) {
            is TermFormEvent.Saved -> onTermsChanged()
            TermFormEvent.Deleted -> { onTermsChanged(); onClose() }
            is TermFormEvent.OpenParent -> onOpenTerm(event.text, sentence)
            is TermFormEvent.OpenTerm -> onOpenTerm(event.text, event.sentence)
        }
    }
    TermFormPanel(
        viewModel = form,
        modifier = Modifier.fillMaxSize(),
        embedded = true,
        onClose = { form.flush(); onClose() },
        onDuplicateClick = { onNavigate(Route.EditTerm(it)) },
        onOpenExamples = { id, term -> onNavigate(Route.Examples(id, term)) },
        onManageDictionaries = { id -> onNavigate(Route.ManageDictionaries(id)) },
    )
}

@Composable
private fun PageHeader(query: ExampleSearchQuery, compact: Boolean, onBack: () -> Unit, onTerms: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    // On a phone the bar has the way back and the screen's name.
    val crumbs = if (compact) emptyList() else listOf(Crumb(tr("Vocabulary"), onTerms), Crumb(tr("Examples")))
    Column(Modifier.fillMaxWidth().padding(bottom = AppSpacing.xl)) {
        ScreenHeader(tr("Examples for ‘{0}’", query.text), subtitle = null, crumbs = crumbs, titleOnCompact = true, onBack = onBack) {
            if (!compact) Text(tr("Powered by"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            LinkText(
                "Tatoeba",
                onClick = {
                    val from = LanguageCodes.tatoebaCodeFor(query.language.name)?.let { "&from=$it" } ?: ""
                    uriHandler.openUri("https://tatoeba.org/en/sentences/search?query=${query.text.encodeURLParameter()}$from")
                },
                modifier = Modifier.semantics { contentDescription = tr("Open on Tatoeba") },
                style = MaterialTheme.typography.bodyMedium,
                trailingIcon = AppIcons.OpenInNew,
            )
        }
    }
}

@Composable
private fun SearchBar(query: ExampleSearchQuery, viewModel: ExamplesSearchViewModel, onSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        SearchField(query.text, { v -> viewModel.updateQuery { it.copy(text = v) } }, tr("Search sentences"), Modifier.weight(1f), onSearch = onSearch)
        PrimaryButton(tr("Search"), onClick = onSearch, enabled = query.text.isNotBlank())
    }
}

private val WORD_COUNTS = listOf(1, 2, 3, 4, 5, 6, 8, 10, 15, 20, 30, 50, 100)
private val PAGE_SIZES = listOf(5, 10, 20, 30, 50, 100)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(query: ExampleSearchQuery, viewModel: ExamplesSearchViewModel) {
    FlowRow(
        Modifier.fillMaxWidth().padding(top = AppSpacing.l),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Select(tr("Min words"), (listOf<Int?>(null) + WORD_COUNTS), query.minWords, { it?.toString() ?: tr("Any") }, 110.dp) { v -> viewModel.updateFilters { it.copy(minWords = v) } }
        Select(tr("Max words"), (listOf<Int?>(null) + WORD_COUNTS), query.maxWords, { it?.toString() ?: tr("Any") }, 110.dp) { v -> viewModel.updateFilters { it.copy(maxWords = v) } }
        Select(tr("Sort"), ExampleSort.entries, query.sort, { tr(it.label) }, 170.dp) { v -> viewModel.updateFilters { it.copy(sort = v) } }
        Select(tr("Per page"), PAGE_SIZES, query.limit, { it.toString() }, 110.dp) { v -> viewModel.updateFilters { it.copy(limit = v) } }
        Select(tr("Has audio"), listOf<YesNo?>(null) + YesNo.entries, query.hasAudio, { it?.label?.let { label -> tr(label) } ?: tr("Any") }, 150.dp) { v -> viewModel.updateFilters { it.copy(hasAudio = v) } }
        Spacer(Modifier.weight(1f))
        SecondaryButton(tr("Reset filters"), onClick = viewModel::resetFilters, icon = Icons.Default.Refresh)
    }
}

/** A labelled compact select box. */
@Composable
private fun <T> Select(label: String, options: List<T>, selected: T, optionLabel: (T) -> String, width: androidx.compose.ui.unit.Dp, onSelect: (T) -> Unit) {
    LabeledField(label, Modifier.width(width)) {
        SelectMenu(options, selected, onSelect, optionLabel, Modifier.fillMaxWidth(), value = optionLabel(selected))
    }
}

@Composable
private fun ExampleCard(
    text: AnnotatedString,
    translateWord: suspend (String) -> String?,
    translation: String?,
    sound: ExampleSound,
    /** The result the keys act on, marked and kept in view. */
    active: Boolean,
    onPlay: () -> Unit,
    direction: TextDirection,
    fontFamily: FontFamily,
    fontScale: Float,
    lineHeight: Float,
    onWordClick: (String) -> Unit,
    onWordSecondaryClick: (String) -> Unit,
    onPhraseSelect: (String) -> Unit,
    onHover: (String?) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val intoView = remember { BringIntoViewRequester() }
    // Marked or played, the example is kept on screen, scrolled no further than that takes.
    LaunchedEffect(active, sound.playing) { if (active) intoView.bringIntoView() }
    Row(
        Modifier.fillMaxWidth().bringIntoViewRequester(intoView).clip(AppShapes.panel)
            .background(if (sound.playing || active) colors.primary.copy(alpha = AppAlpha.wash) else Color.Transparent)
            .border(if (active) AppBorders.selected() else AppBorders.hairline(), AppShapes.panel)
            .semantics { selected = active }
            .padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The button leads, as the reader's sentence buttons do; its description sits on it so that it stays while the spinner replaces the icon.
        AppIconButton(
            if (sound.playing) AppIcons.Stop else sound.icon,
            contentDescription = null,
            onClick = onPlay,
            modifier = Modifier.semantics { contentDescription = sound.description },
            style = IconButtonStyle.Outlined,
            tint = colors.primary,
            selected = sound.playing,
            loading = sound.loading,
        )
        Spacer(Modifier.width(AppSpacing.l))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            HoverTranslatedText(
                text,
                translate = translateWord,
                // Set as the reader's text, from the same settings.
                style = TextStyle(fontSize = (18 * fontScale).sp, lineHeight = (18 * fontScale * lineHeight).sp, fontFamily = fontFamily, color = colors.onSurface, textDirection = direction),
                onWordClick = onWordClick,
                onWordSecondaryClick = onWordSecondaryClick,
                onPhraseSelect = onPhraseSelect,
                onHover = onHover,
            )
            translation?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale), color = colors.onSurfaceVariant) }
        }
    }
}

/** The font, size and line height of the examples: the reader's settings, changed here as there. */
@Composable
private fun TextSettings(prefs: UserSettings, viewModel: ExamplesSearchViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(top = AppSpacing.l).cardSurface(AppShapes.card).padding(horizontal = AppSpacing.xl, vertical = AppSpacing.l),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Text(tr("Text settings"), style = AppText.itemTitle)
        Select(tr("Font"), ReadingFont.choices, ReadingFont.byId(prefs.readingFont), { it.label }, 220.dp, viewModel::setReadingFont)
        SliderSetting(tr("Font size"), "${(prefs.readingFontScale * 100).roundToInt()}%", prefs.readingFontScale, 0.6f..2.5f, viewModel::setFontScale, sliderModifier = Modifier.testTag("examples-font-size"))
        SliderSetting(tr("Line height"), "${(prefs.readingLineHeight * 10).roundToInt() / 10f}", prefs.readingLineHeight, 1.0f..3.0f, viewModel::setLineHeight, sliderModifier = Modifier.testTag("examples-line-height"))
    }
}

/** Highlights the term and its inflections (same stem) in the sentence. */
@Composable
private fun emphasize(sentence: String, term: String): AnnotatedString {
    // Weight alone marks the searched term: the colours are the statuses' and the selection's, as in the reader.
    val highlight = SpanStyle(fontWeight = AppText.labelStrong.fontWeight)
    return remember(sentence, term) {
        buildAnnotatedString {
            val needle = term.trim().lowercase()
            if (needle.isEmpty()) {
                append(sentence)
                return@buildAnnotatedString
            }
            val stem = if (needle.length >= 5) needle.dropLast(1) else needle
            val words = Regex("""[\p{L}\p{M}\p{Nd}'’-]+""")
            var index = 0
            for (match in words.findAll(sentence)) {
                append(sentence.substring(index, match.range.first))
                val word = match.value
                val lower = word.lowercase()
                val matches = lower == needle || lower.startsWith(needle) || (needle != stem && lower.startsWith(stem))
                if (matches) withStyle(highlight) { append(word) } else append(word)
                index = match.range.last + 1
            }
            append(sentence.substring(index))
        }
    }
}
