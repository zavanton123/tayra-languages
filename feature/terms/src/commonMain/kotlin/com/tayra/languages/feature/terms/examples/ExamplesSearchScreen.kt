package com.tayra.languages.feature.terms.examples

import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.tayra.languages.core.ui.components.RoundSlider
import com.tayra.languages.core.ui.theme.ReadingFont
import com.tayra.languages.core.ui.theme.fontFamily
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.EmptyMessage
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
import io.ktor.http.encodeURLParameter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.CircularProgressIndicator
import com.tayra.languages.feature.terms.form.TermFormViewModel
import kotlin.random.Random
import com.tayra.languages.feature.terms.form.TermFormPanel
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.form.TermFormEvent
import com.tayra.languages.core.ui.state.CollectEvents
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.fillMaxHeight
import com.tayra.languages.core.ui.components.ScrollList

/**
 * Searches Tatoeba example sentences with every filter the API offers. Wide windows show the
 * term pane beside the results, for the searched term or a word clicked in an example; on
 * narrower ones a clicked word opens it in a sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = { AppTopBar(title = "Tayra Languages", onNavigate = onNavigate, section = NavSection.TERMS, onBack = if (compact) onBack else null) },
    ) { padding ->
        if (state.loading || query == null) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        val direction = if (query.language.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
        val gutter = if (compact) 16.dp else 32.dp
        Row(
            Modifier.padding(padding).fillMaxSize()
                .focusRequester(focus)
                .onFocusChanged { listFocused = it.isFocused }
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !listFocused) return@onPreviewKeyEvent false
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
            ScrollList(
                Modifier.weight(1f).fillMaxHeight()
                    // A press on the results hands them the keyboard; a text field under it takes it right back.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) focusList()
                        }
                    },
                contentPadding = PaddingValues(horizontal = gutter, vertical = 16.dp),
            ) {
                item { PageHeader(query, compact, onBack, onNavigate) }
                item { ErrorMessage(state.error) }
                item { SearchBar(query, viewModel, onSearch = { viewModel.search(); focusList() }) }
                item { FilterRow(query, viewModel) }
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                        ActionButton(icon = AppIcons.FormatSize, description = tr("Text settings"), active = textSettingsOpen, onClick = { textSettingsOpen = !textSettingsOpen })
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
                        Modifier.padding(top = 16.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    state.searching -> item { LoadingIndicator(Modifier.fillMaxWidth().padding(32.dp)) }
                    state.results.isEmpty() -> item { EmptyMessage(tr("No examples match these filters."), Modifier.fillMaxWidth()) }
                    else -> {
                        items(state.results) { example ->
                            ExampleCard(
                                text = emphasize(example.text, query.text)
                                    .withStatuses(state.words[example.text].orEmpty(), TayraTheme.current.statusColors, showHighlights)
                                    .withSelected(state.paneTerm, TayraTheme.current.selectedText),
                                translateWord = translateWord,
                                translation = example.translation,
                                sound = audio.soundOf(example),
                                onPlay = { audio.toggle(example, LanguageCodes.codeFor(query.language.name)) },
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
                            Spacer(Modifier.height(10.dp))
                        }
                        if (state.hasMore) {
                            item {
                                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                                    OutlinedButton(onClick = viewModel::loadMore, enabled = !state.loadingMore, shape = RoundedCornerShape(10.dp)) {
                                        Text(if (state.loadingMore) tr("Loading...") else tr("Load more"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val term = state.paneTerm
            if (wide && term != null) {
                Surface(
                    Modifier.width(420.dp).fillMaxHeight().padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    TermPane(query.language.id, term, state.paneSentence, onClose = viewModel::closePane, onOpenTerm = viewModel::openTerm, onTermsChanged = viewModel::refreshTerms, onForm = { paneForm = it }, onNavigate = onNavigate)
                }
            }
        }
    }

    val sheetTerm = state.paneTerm
    val sheetLanguage = state.language
    if (!wide && sheetOpen && sheetTerm != null && sheetLanguage != null) {
        val close = { sheetOpen = false }
        ModalBottomSheet(onDismissRequest = close, sheetState = rememberModalBottomSheetState()) {
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
private fun PageHeader(query: ExampleSearchQuery, compact: Boolean, onBack: () -> Unit, onNavigate: (Route) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalAlignment = Alignment.Top) {
        if (!compact) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("Vocabulary"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.clickable { onNavigate(Route.Terms()) })
                Text("/", style = MaterialTheme.typography.bodyMedium, color = colors.outline)
                Text(tr("Examples"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Text(
                tr("Examples for ‘{0}’", query.text),
                style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                val from = LanguageCodes.tatoebaCodeFor(query.language.name)?.let { "&from=$it" } ?: ""
                uriHandler.openUri("https://tatoeba.org/en/sentences/search?query=${query.text.encodeURLParameter()}$from")
            }.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!compact) Text(tr("Powered by"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text("Tatoeba", style = MaterialTheme.typography.bodyMedium, color = colors.primary, fontWeight = FontWeight.Medium)
            Icon(AppIcons.OpenInNew, contentDescription = tr("Open on Tatoeba"), tint = colors.primary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SearchBar(query: ExampleSearchQuery, viewModel: ExamplesSearchViewModel, onSearch: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query.text,
                onValueChange = { v -> viewModel.updateQuery { it.copy(text = v) } },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.text.isEmpty()) Text(tr("Search sentences"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                        inner()
                    }
                },
            )
        }
        Button(onClick = onSearch, enabled = query.text.isNotBlank(), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(48.dp)) {
            Text(tr("Search"))
        }
    }
}

private val WORD_COUNTS = listOf(1, 2, 3, 4, 5, 6, 8, 10, 15, 20, 30, 50, 100)
private val PAGE_SIZES = listOf(5, 10, 20, 30, 50, 100)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(query: ExampleSearchQuery, viewModel: ExamplesSearchViewModel) {
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Select(tr("Min words"), (listOf<Int?>(null) + WORD_COUNTS), query.minWords, { it?.toString() ?: tr("Any") }, 110.dp) { v -> viewModel.updateFilters { it.copy(minWords = v) } }
        Select(tr("Max words"), (listOf<Int?>(null) + WORD_COUNTS), query.maxWords, { it?.toString() ?: tr("Any") }, 110.dp) { v -> viewModel.updateFilters { it.copy(maxWords = v) } }
        Select(tr("Sort"), ExampleSort.entries, query.sort, { tr(it.label) }, 170.dp) { v -> viewModel.updateFilters { it.copy(sort = v) } }
        Select(tr("Per page"), PAGE_SIZES, query.limit, { it.toString() }, 110.dp) { v -> viewModel.updateFilters { it.copy(limit = v) } }
        Select(tr("Has audio"), listOf<YesNo?>(null) + YesNo.entries, query.hasAudio, { it?.label?.let { label -> tr(label) } ?: tr("Any") }, 150.dp) { v -> viewModel.updateFilters { it.copy(hasAudio = v) } }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = viewModel::resetFilters, shape = RoundedCornerShape(10.dp), modifier = Modifier.height(44.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("Reset filters"))
        }
    }
}

/** A labelled compact select box. */
@Composable
private fun <T> Select(label: String, options: List<T>, selected: T, optionLabel: (T) -> String, width: androidx.compose.ui.unit.Dp, onSelect: (T) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Box {
            Row(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp))
                    .clickable { open = true }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(optionLabel(selected), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            AppMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    AppMenuItem(text = { Text(optionLabel(option)) }, onClick = { open = false; onSelect(option) })
                }
            }
        }
    }
}

@Composable
private fun ExampleCard(
    text: AnnotatedString,
    translateWord: suspend (String) -> String?,
    translation: String?,
    sound: ExampleSound,
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
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (sound.playing) colors.primary.copy(alpha = 0.06f) else Color.Transparent)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        Spacer(Modifier.width(16.dp))
        ActionButton(
            icon = sound.icon,
            description = sound.description,
            active = sound.playing,
            stops = true,
            loading = sound.loading,
            onClick = onPlay,
        )
    }
}

/** The font, size and line height of the examples: the reader's settings, changed here as there. */
@Composable
private fun TextSettings(prefs: UserSettings, viewModel: ExamplesSearchViewModel) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(14.dp)).background(colors.surface)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp)).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(tr("Text settings"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Select(tr("Font"), ReadingFont.choices, ReadingFont.byId(prefs.readingFont), { it.label }, 220.dp, viewModel::setReadingFont)
        SliderRow(tr("Font size"), "${(prefs.readingFontScale * 100).roundToInt()}%", prefs.readingFontScale, 0.6f..2.5f, viewModel::setFontScale, "examples-font-size")
        SliderRow(tr("Line height"), "${(prefs.readingLineHeight * 10).roundToInt() / 10f}", prefs.readingLineHeight, 1.0f..3.0f, viewModel::setLineHeight, "examples-line-height")
    }
}

/** A slider with its name and value above it. */
@Composable
private fun SliderRow(title: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, tag: String) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RoundSlider(value = current.coerceIn(range), onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth().testTag(tag))
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    /** Tints the button; with [stops], a stop square replaces the icon, as on a button that plays. */
    active: Boolean = false,
    stops: Boolean = false,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
            .background(if (active) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when {
            loading -> CircularProgressIndicator(Modifier.size(18.dp), color = colors.primary, strokeWidth = 2.dp)
            active && stops -> Box(Modifier.size(12.dp).background(colors.primary, RoundedCornerShape(2.dp)))
            else -> Icon(icon, contentDescription = null, tint = if (enabled) colors.primary else colors.outlineVariant, modifier = Modifier.size(20.dp))
        }
    }
}

/** Highlights the term and its inflections (same stem) in the sentence. */
@Composable
private fun emphasize(sentence: String, term: String): AnnotatedString {
    // Weight alone marks the searched term: the colours are the statuses' and the selection's, as in the reader.
    val highlight = SpanStyle(fontWeight = FontWeight.SemiBold)
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
