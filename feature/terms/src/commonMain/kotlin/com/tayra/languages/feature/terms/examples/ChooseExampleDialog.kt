package com.tayra.languages.feature.terms.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.flashcards.Cloze
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSource
import com.tayra.languages.core.domain.service.SentenceTranslation
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.EmptyMessage
import com.tayra.languages.core.ui.components.ErrorMessage
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.LinkText
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.MarkedText
import com.tayra.languages.core.ui.components.PlaceholderCards
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.addFill
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.theme.ReadingFont
import com.tayra.languages.core.ui.theme.fontFamily
import kotlin.random.Random
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Picks another example sentence for the flashcard of [word]: the Examples dialog's search, with
 * its results to choose from. A result holding no form of the word, which the card could not
 * blank it out of, is shown but cannot be chosen. [onReplace] gets the chosen sentence with its
 * translation, the source's own or a machine one, when there is one yet.
 */
@Composable
fun ChooseExampleDialog(
    termId: Long,
    languageId: Long,
    word: String,
    /** The card's sentence now, and its translation; null for a card without one. */
    sentence: String?,
    translation: String?,
    onReplace: (sentence: String, translation: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    // Each opening searches anew.
    val opening = remember { Random.nextLong() }
    val viewModel = koinViewModel<ExamplesSearchViewModel>(key = "choose-example-$termId-$opening") { parametersOf(languageId, word) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val terms = koinInject<TermRepository>()
    val term by produceState<Term?>(null, termId) { value = terms.getById(termId) }
    // Where the card would blank its word out of each result; null for a result without a form of it.
    val ranges by produceState(emptyMap<String, IntRange?>(), term, state.results) {
        val card = term ?: return@produceState
        value = state.results.associate { it.text to Cloze.cardRange(it.text, card, terms) }
    }
    val currentRange by produceState<IntRange?>(null, term, sentence) {
        value = term?.let { card -> sentence?.let { Cloze.cardRange(it, card, terms) } }
    }
    var chosen by remember { mutableStateOf<String?>(null) }
    val usable = state.results.filter { ranges[it.text] != null }
    // A new search that no longer holds the choice drops it.
    val picked = usable.firstOrNull { it.text == chosen }
    val replace = {
        picked?.let { example ->
            onReplace(example.text, example.translation ?: (state.translations[example.text] as? SentenceTranslation.Done)?.text)
        }
        Unit
    }
    fun move(delta: Int) {
        if (usable.isEmpty()) return
        val at = usable.indexOf(picked)
        chosen = usable[if (at < 0) 0 else (at + delta).coerceIn(0, usable.lastIndex)].text
    }

    val compact = LocalWindowWidth.current.isCompact
    val gutter = if (compact) AppSpacing.l else AppSpacing.xxl
    val audio = rememberExampleAudio()
    audio.PrepareSpeech(state.results, state.language?.let { LanguageCodes.codeFor(it.name) })
    val prefs by koinInject<SettingsRepository>().settings.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }

    AppDialogSurface(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(if (compact) PHONE_HEIGHT else 1f).windowInsetsPadding(WindowInsets.safeDrawing).testTag("choose-example-dialog"),
        maxWidth = if (compact) Dp.Infinity else DIALOG_WIDTH,
    ) {
        Column(
            Modifier.fillMaxSize().focusRequester(focus).focusable()
                // ↑ and ↓ move the choice; Enter, outside the search field, replaces the example with it.
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> { move(-1); true }
                        Key.DirectionDown -> { move(1); true }
                        else -> false
                    }
                }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || event.key != Key.Enter || searchFocused || picked == null) return@onKeyEvent false
                    replace()
                    true
                },
        ) {
            // Asked for here, where the dialog's content exists, so that the keys work from the start.
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            Header(word, compact, gutter, onDismiss)
            val query = state.query
            if (state.loading || query == null) {
                LoadingIndicator(Modifier.weight(1f))
                return@Column
            }
            val direction = if (query.language.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
            val fill = AppAccents.highlight.copy(alpha = AppAlpha.half)
            val strong = SpanStyle(fontWeight = AppText.labelStrong.fontWeight)
            ScrollList(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = gutter, vertical = AppSpacing.m)) {
                if (sentence != null) item { CurrentExample(marked(sentence, currentRange, fill, strong), translation, direction) }
                item { ErrorMessage(state.error) }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = AppSpacing.l).onFocusChanged { searchFocused = it.hasFocus },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
                    ) {
                        SearchField(query.text, { v -> viewModel.updateQuery { it.copy(text = v) } }, tr("Search sentences"), Modifier.weight(1f), onSearch = viewModel::search)
                        PrimaryButton(tr("Search"), onClick = viewModel::search, enabled = query.text.isNotBlank())
                    }
                }
                item { if (compact) FilterGrid(query, state, viewModel) else FilterRow(query, state.sources, state.features, viewModel) }
                if (!state.features.phrases) item { SourceNote(tr("This source finds single words only, in the exact form typed.")) }
                item { AppDivider(Modifier.padding(top = AppSpacing.xl)) }
                item {
                    Text(
                        when {
                            state.searching -> tr("Searching...")
                            state.total != null && query.source == ExampleSource.LEIPZIG -> trPlural(state.total!!, "About {0} sentence in the corpus", "About {0} sentences in the corpus")
                            state.total != null -> trPlural(state.total!!, "{0} sentence", "{0} sentences")
                            else -> ""
                        },
                        Modifier.padding(top = AppSpacing.l),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item { Text(tr("Choose a replacement"), Modifier.padding(top = AppSpacing.s, bottom = AppSpacing.m), style = AppText.sectionTitle) }
                when {
                    state.searching -> item { PlaceholderCards(4, lines = 1) }
                    state.results.isEmpty() -> item {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            EmptyMessage(if (state.failed) unreachable(query.source) else tr("No examples match these filters."), Modifier.fillMaxWidth())
                            if (state.failed) SecondaryButton(tr("Retry"), onClick = viewModel::search, icon = Icons.Default.Refresh)
                        }
                    }
                    else -> {
                        itemsIndexed(state.results) { _, example ->
                            if (!state.features.translations) LaunchedEffect(example.text) { viewModel.translate(example.text) }
                            val range = ranges[example.text]
                            Choice(
                                text = marked(example.text, range, fill, strong),
                                word = word,
                                usable = range != null || example.text !in ranges,
                                selected = example.text == picked?.text,
                                translation = example.translation,
                                machineTranslation = if (state.features.translations) null else state.translations[example.text] ?: SentenceTranslation.Loading,
                                sound = audio.soundOf(example),
                                onPlay = { audio.toggle(example, LanguageCodes.codeFor(query.language.name)) },
                                onSelect = { chosen = example.text },
                                style = TextStyle(
                                    fontSize = (18 * prefs.readingFontScale).sp,
                                    lineHeight = (18 * prefs.readingFontScale * prefs.readingLineHeight).sp,
                                    fontFamily = ReadingFont.byId(prefs.readingFont).fontFamily(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textDirection = direction,
                                ),
                                fontScale = prefs.readingFontScale,
                            )
                            Spacer(Modifier.height(AppSpacing.m))
                        }
                        if (state.hasMore) item {
                            Row(Modifier.fillMaxWidth().padding(AppSpacing.s), horizontalArrangement = Arrangement.Center) {
                                SecondaryButton(if (state.loadingMore) tr("Loading...") else tr("Load more"), onClick = viewModel::loadMore, enabled = !state.loadingMore)
                            }
                        }
                    }
                }
                item { Row(verticalAlignment = Alignment.CenterVertically) { SourceCredit(query, compact = false) } }
            }
            AppDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = AppSpacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
            ) {
                Text(
                    when {
                        !compact -> tr("The selected sentence will replace the current example.")
                        picked != null -> tr("1 example selected")
                        else -> tr("No example selected")
                    },
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (compact) QuietButton(tr("Cancel"), onClick = onDismiss) else SecondaryButton(tr("Cancel"), onClick = onDismiss)
                PrimaryButton(tr("Replace example"), onClick = replace, enabled = picked != null, modifier = Modifier.testTag("replace-example"))
            }
        }
    }
}

@Composable
private fun Header(word: String, compact: Boolean, gutter: Dp, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(start = gutter, end = AppSpacing.l, top = AppSpacing.xl, bottom = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Box(Modifier.size(HEADER_TILE).clip(AppShapes.panel).background(colors.primary.copy(alpha = AppAlpha.tint)), contentAlignment = Alignment.Center) {
            Icon(AppIcons.Forum, contentDescription = null, tint = colors.primary, modifier = Modifier.size(HEADER_ICON))
        }
        // A phone puts the word under the title, where it has the room.
        Column(Modifier.weight(1f)) {
            Text(if (compact) tr("Choose an example") else tr("Choose an example for ‘{0}’", word), style = AppText.headline)
            Text(
                if (compact) tr("For ‘{0}’", word) else tr("Replace the sentence on this flashcard."),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }
        AppIconButton(Icons.Default.Close, tr("Close"), onClick = onDismiss, modifier = Modifier.testTag("close-choose-example"), style = IconButtonStyle.Tonal)
    }
}

/** The card's sentence as it is now, with its word marked. */
@Composable
private fun CurrentExample(sentence: AnnotatedString, translation: String?, direction: TextDirection) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = AppAlpha.wash)).padding(AppSpacing.l).testTag("current-example"),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Text(tr("Current example").uppercase(), style = AppText.overline, color = colors.onSurfaceVariant)
        MarkedText(sentence, style = MaterialTheme.typography.titleLarge.copy(color = colors.onSurface, textDirection = direction))
        translation?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant) }
    }
}

/** The filters two to a row, as a phone has the room for, under a heading with the way to reset them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGrid(query: ExampleSearchQuery, state: ExamplesSearchUiState, viewModel: ExamplesSearchViewModel) {
    Column(Modifier.fillMaxWidth().padding(top = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Filters").uppercase(), Modifier.weight(1f), style = AppText.overline, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinkText(tr("Reset"), onClick = viewModel::resetFilters, style = AppText.label)
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
            maxItemsInEachRow = 2,
        ) {
            FilterFields(query, state.sources, state.features, viewModel) { Modifier.weight(1f) }
        }
    }
}

/** One result to choose: its sound, the sentence with the card's word marked, its translation and the choice. */
@Composable
private fun Choice(
    text: AnnotatedString,
    word: String,
    /** Holds a form of the word, so the card can blank it out. */
    usable: Boolean,
    selected: Boolean,
    translation: String?,
    machineTranslation: SentenceTranslation?,
    sound: ExampleSound,
    onPlay: () -> Unit,
    onSelect: () -> Unit,
    style: TextStyle,
    fontScale: Float,
) {
    val colors = MaterialTheme.colorScheme
    val intoView = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) { if (selected) intoView.bringIntoView() }
    Row(
        Modifier.fillMaxWidth().bringIntoViewRequester(intoView).clip(AppShapes.panel)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.tint) else Color.Transparent)
            .border(if (selected) AppBorders.selected() else AppBorders.hairline(), AppShapes.panel)
            .selectable(selected = selected, enabled = usable, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = AppSpacing.l, vertical = AppSpacing.m)
            .testTag("example-choice"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Column(Modifier.weight(1f).alpha(if (usable) 1f else AppAlpha.disabled), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            MarkedText(text, style = style)
            translation?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale), color = colors.onSurfaceVariant) }
            machineTranslation?.let { MachineTranslation(it, fontScale) }
            if (!usable) Text(tr("No form of ‘{0}’ in this sentence", word), style = MaterialTheme.typography.bodySmall, color = colors.outline)
        }
        RadioButton(selected = selected, onClick = null, enabled = usable)
    }
}

/** [sentence] with the card's word, at [range], on the card's mark. */
private fun marked(sentence: String, range: IntRange?, fill: Color, strong: SpanStyle): AnnotatedString = buildAnnotatedString {
    append(sentence)
    if (range != null) {
        addFill(fill, range.first, range.last + 1)
        addStyle(strong, range.first, range.last + 1)
    }
}

private val DIALOG_WIDTH = 920.dp
private val HEADER_TILE = 56.dp
private val HEADER_ICON = 30.dp

/** A phone's dialog leaves a strip of the page above it, as the reader's examples do. */
private const val PHONE_HEIGHT = 0.92f
