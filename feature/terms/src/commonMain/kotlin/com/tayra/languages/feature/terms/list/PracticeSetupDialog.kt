package com.tayra.languages.feature.terms.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.theme.AppBorders
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.Layout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.practice.ExerciseType
import com.tayra.languages.core.domain.practice.PracticeContext
import com.tayra.languages.core.domain.practice.PracticeMaterialLoader
import com.tayra.languages.core.domain.practice.PracticePlanner
import com.tayra.languages.core.domain.practice.PracticeResponse
import com.tayra.languages.core.domain.practice.PracticeSetup
import com.tayra.languages.core.domain.practice.PracticeSkill
import com.tayra.languages.core.domain.practice.PracticeSource
import com.tayra.languages.core.domain.practice.PracticeWord
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.FreshViewModels
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ChipLeading
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SelectableChip
import com.tayra.languages.core.ui.components.description
import com.tayra.languages.core.ui.components.icon
import com.tayra.languages.core.ui.components.label
import com.tayra.languages.core.ui.components.reason
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.seconds
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.practice.forPractice
import com.tayra.languages.core.domain.practice.PracticeExampleStore
import com.tayra.languages.core.domain.practice.PracticeExampleRepository
import com.tayra.languages.core.domain.practice.PracticeExample
import com.tayra.languages.core.domain.practice.WordForms
import com.tayra.languages.core.domain.practice.WordFamily
import com.tayra.languages.core.domain.practice.SpeechRecognition
import com.tayra.languages.core.domain.language.LanguageCodes
import androidx.compose.runtime.LaunchedEffect

/** What the setup dialog knows of the words before the practice: loaded once, when it opens. */
data class PracticeSetupState(
    val loading: Boolean = true,
    val words: List<PracticeWord> = emptyList(),
    val meanings: List<String> = emptyList(),
    /** The translations of the words' sentences, as they arrive. */
    val translations: Map<String, String> = emptyMap(),
    /** Still translating the sentences, so translation questions may yet grow. */
    val translating: Boolean = false,
    /** The sentences from elsewhere kept for the words, by the word's key. */
    val examples: Map<String, List<PracticeExample>> = emptyMap(),
    /** Still looking up sentences from elsewhere for words short of them. */
    val lookingUp: Boolean = false,
    /** The words' families, by the word's key, for the questions on their forms. */
    val families: Map<String, WordFamily> = emptyMap(),
    /** Answers can be said aloud: something here hears the words' language. */
    val speaking: Boolean = false,
)

/**
 * Loads the words a practice from [source] would ask about, and translates their sentences, for
 * the setup dialog's counts; keeps the setup last used in the settings.
 */
class PracticeSetupViewModel(
    private val source: PracticeSource,
    private val material: PracticeMaterialLoader,
    private val translator: SentenceTranslator,
    private val settings: SettingsRepository,
    private val exampleStore: PracticeExampleStore? = null,
    private val keptExamples: PracticeExampleRepository? = null,
    private val wordForms: WordForms? = null,
    private val speech: SpeechRecognition? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(PracticeSetupState())
    val state: StateFlow<PracticeSetupState> = _state.asStateFlow()

    private val fromVocabulary = source is PracticeSource.Words

    /** The setup of the last practice, wherever it was started, or the default one the first time. */
    val initialSetup: PracticeSetup = settings.current.let { if (fromVocabulary) it.practiceSetupVocabulary else it.practiceSetupReading }
            .takeIf { it.isNotBlank() }?.let(PracticeSetup::decode) ?: PracticeSetup.FIRST

    /** Keeps [setup] as the one to open with next time, from the reader and the vocabulary alike; kept even as the dialog closes on it. */
    fun remember(setup: PracticeSetup) = viewModelScope.launch(NonCancellable) {
        settings.update { it.copy(practiceSetupReading = setup.encode(), practiceSetupVocabulary = setup.encode()) }
    }

    init {
        viewModelScope.launch {
            val loaded = attempt { material.load(source) }
            val words = loaded?.words.orEmpty()
            _state.value = PracticeSetupState(
                loading = false, words = words, meanings = loaded?.meanings.orEmpty(), translating = loaded != null,
                examples = attempt { keptExamples?.forPractice(words) }.orEmpty(), lookingUp = exampleStore != null && words.isNotEmpty(),
                families = loaded?.let { found -> attempt { wordForms?.let { forms -> words.mapNotNull { w -> forms.family(w.text, found.language)?.let { w.key to it } }.toMap() } } }.orEmpty(),
                speaking = loaded?.let { found -> LanguageCodes.codeFor(found.language.name)?.let { code -> attempt { speech?.isAvailable(code) } } } == true,
            )
            if (loaded == null) return@launch
            launch { lookUpExamples(words) }
            translate(loaded.words, loaded.language)
        }
    }

    /** Looks up sentences from elsewhere for the words short of them, then counts again. */
    private suspend fun lookUpExamples(words: List<PracticeWord>) {
        attempt { exampleStore?.topUp(words.mapNotNull { it.termId }, LOOKUP_WAIT) }
        val examples = attempt { keptExamples?.forPractice(words) }
        _state.update { it.copy(examples = examples ?: it.examples, lookingUp = false) }
    }

    private suspend fun translate(words: List<PracticeWord>, language: Language) {
        withTimeoutOrNull(TRANSLATION_WAIT) {
            for (sentence in words.map { it.sentence }.distinct()) {
                val translated = attempt { translator.translate(sentence, language) }?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                _state.update { it.copy(translations = it.translations + (sentence to translated)) }
            }
        }
        _state.update { it.copy(translating = false) }
    }

    private suspend fun <T> attempt(block: suspend () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private companion object {
        val TRANSLATION_WAIT = 8.seconds
        val LOOKUP_WAIT = 15.seconds
    }
}

/** About how long a question takes, for the estimate in the dialog. */
private const val SECONDS_A_QUESTION = 20
private val SETUP_WIDTH = 1040.dp
private val LENGTHS = listOf(5, 10, 20, null)

/**
 * Asks what to practise the words from [source] with, starting from the setup last used there: how
 * questions are answered, which sentences they come from, how many there are, and the types of
 * question. Each type says how many of the words it can ask about, or what they lack; those that
 * would ask nothing wait folded away. The footer sums up.
 */
@Composable
fun PracticeSetupDialog(source: PracticeSource, onStart: (PracticeSetup) -> Unit, onDismiss: () -> Unit) {
    // Counted afresh each time it opens: the words being learned may have changed since.
    FreshViewModels { PracticeSetupDialogHost(source, onStart, onDismiss) }
}

@Composable
private fun PracticeSetupDialogHost(
    source: PracticeSource,
    onStart: (PracticeSetup) -> Unit,
    onDismiss: () -> Unit,
    viewModel: PracticeSetupViewModel = koinViewModel { parametersOf(source) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PracticeSetupContent(
        state,
        viewModel.initialSetup,
        onStart = { setup -> viewModel.remember(setup); onStart(setup) },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PracticeSetupContent(
    state: PracticeSetupState,
    initial: PracticeSetup,
    onStart: (PracticeSetup) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalWindowWidth.current.isCompact
    var setup by remember { mutableStateOf(initial) }
    var showUnavailable by remember { mutableStateOf(false) }
    // Nothing here hears the language: what was to be said is typed.
    LaunchedEffect(state.loading, state.speaking) { if (!state.loading && !state.speaking) setup = setup.withoutSpeaking() }
    val preview = remember(state, setup) { PracticePlanner.preview(state.words, setup, state.meanings, state.translations, state.examples, state.families) }
    // Every question there is; a length that many or more asks the same as all of them, so it is not offered.
    val total = remember(state, setup) { if (state.loading) null else PracticePlanner.preview(state.words, setup.copy(length = null), state.meanings, state.translations, state.examples, state.families).questions }
    val minutes = ((preview.questions * SECONDS_A_QUESTION + 59) / 60).coerceAtLeast(1)
    // Sentences from elsewhere are fetched when the practice starts, so they may make questions the count does not show.
    val examplesMayAsk = PracticeContext.EXAMPLE in setup.contexts && ExerciseType.GAP in setup.types
    val canStart = !state.loading && state.words.isNotEmpty() && (preview.questions > 0 || examplesMayAsk)

    // In the order of the skills they train; those that would ask nothing of these words, or not the ways ticked, are folded away.
    val types = PracticeSkill.entries.flatMap { skill -> ExerciseType.entries.filter { it.skill == skill } }
    val (available, unavailable) = types.partition { type -> state.loading || (preview.of(type).words > 0 && type.responses.any { it in setup.responses }) }

    val answers: @Composable () -> Unit = {
        Option(tr("Answer with")) {
            PracticeResponse.entries.filter { state.speaking || it != PracticeResponse.SPEAK }.forEach { response ->
                val on = response in setup.responses
                SelectableChip(response.label(), on, onClick = { setup = setup.copy(responses = if (on) setup.responses - response else setup.responses + response) }, leading = ChipLeading.Check, modifier = Modifier.testTag("practice-response-${response.name}"))
            }
        }
    }
    val sentences: @Composable () -> Unit = {
        Option(
            tr("Examples from"),
            note = if (PracticeContext.EXAMPLE in setup.contexts && !state.loading) {
                if (state.lookingUp) tr("Looking up sentences from elsewhere…") else tr("Sentences kept for {0} of {1} words", preview.wordsWithExamples, preview.words)
            } else null,
        ) {
            PracticeContext.entries.forEach { context ->
                val on = context in setup.contexts
                SelectableChip(context.label(), on, onClick = { setup = setup.copy(contexts = if (on) setup.contexts - context else setup.contexts + context) }, leading = ChipLeading.Check, modifier = Modifier.testTag("practice-context-${context.name}"))
            }
        }
    }
    val length: @Composable () -> Unit = {
        Option(tr("Questions")) {
            val lengths = LENGTHS.filter { count -> count == null || total == null || count < total }
            // A length kept from a practice with more questions than these asks them all.
            val chosen = setup.length.takeIf { it in lengths }
            SameWidths {
                lengths.forEach { count ->
                    SelectableChip(count?.toString() ?: tr("All"), chosen == count, onClick = { setup = setup.copy(length = count) }, modifier = Modifier.testTag("practice-length-${count ?: "all"}"))
                }
            }
        }
    }

    AppDialogSurface(onDismissRequest = onDismiss, color = colors.surfaceContainerLow, maxWidth = if (compact) AppSizes.dialogWide else SETUP_WIDTH) {
        Column {
            val gutter = if (compact) AppSpacing.xl else AppSpacing.xxxl
            Row(
                Modifier.padding(start = gutter, end = AppSpacing.xl, top = AppSpacing.xxl, bottom = AppSpacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
            ) {
                IconTile(AppIcons.DoneAll, size = if (compact) 48 else 56)
                Column(Modifier.weight(1f)) {
                    Text(if (state.loading || state.words.isEmpty()) tr("Practice") else trPlural(state.words.size, "Practise {0} word", "Practise {0} words"), style = AppText.headline)
                    Text(tr("Build a focused review session."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                AppIconButton(Icons.Default.Close, tr("Close"), onDismiss)
            }
            // The choices scroll; the title above them and the buttons below stay.
            ScrollColumn(Modifier.weight(1f, fill = false), contentModifier = Modifier.padding(horizontal = gutter, vertical = AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                val panel = Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = AppAlpha.wash)).border(AppBorders.hairline(), AppShapes.card)
                if (compact) {
                    Column(panel.padding(AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                        answers()
                        sentences()
                        length()
                    }
                } else {
                    Row(panel.height(IntrinsicSize.Min).padding(AppSpacing.l), horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
                        // Shares by what each holds, so the chips wrap only in a narrow window.
                        Box(Modifier.weight(1f)) { answers() }
                        AppVerticalDivider(Modifier.fillMaxHeight())
                        Box(Modifier.weight(1.1f)) { sentences() }
                        AppVerticalDivider(Modifier.fillMaxHeight())
                        Box(Modifier.weight(0.8f)) { length() }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Text(tr("Choose exercise types"), style = AppText.sectionTitle)
                    Text(tr("Select one or more."), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    val card: @Composable (ExerciseType, Modifier) -> Unit = { type, modifier ->
                        TypeCard(type, type in setup.types, state, preview.of(type).let { it.words to it.need?.reason() }, total = preview.words, answerable = type.responses.any { it in setup.responses }, modifier) {
                            setup = setup.copy(types = if (type in setup.types) setup.types - type else setup.types + type)
                        }
                    }
                    TypeGrid(available, columns = if (compact) 1 else 2, card)
                    if (unavailable.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().clip(AppShapes.card)
                                .then(if (compact) Modifier.border(AppBorders.hairline(), AppShapes.card) else Modifier)
                                .clickable { showUnavailable = !showUnavailable }.padding(horizontal = AppSpacing.m, vertical = AppSpacing.m)
                                .testTag("practice-unavailable"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
                        ) {
                            Icon(if (showUnavailable) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.primary)
                            Text(
                                if (showUnavailable) tr("Hide unavailable exercises") else trPlural(unavailable.size, "Show {0} unavailable exercise", "Show {0} unavailable exercises"),
                                style = AppText.label,
                                color = colors.primary,
                            )
                        }
                        if (showUnavailable) TypeGrid(unavailable, columns = if (compact) 1 else 2, card)
                    }
                }
            }
            AppDivider()
            val summary = when {
                state.loading -> tr("Checking…")
                state.words.isEmpty() -> tr("No words being learned to practise")
                preview.questions > 0 -> trPlural(preview.questions, "{0} question · about {1} min", "{0} questions · about {1} min", minutes)
                canStart -> tr("Questions on sentences from elsewhere only")
                else -> tr("None of the ticked questions fits these words")
            }
            val buttons: @Composable () -> Unit = {
                QuietButton(tr("Cancel"), onClick = onDismiss, size = ButtonSize.Large)
                Spacer(Modifier.width(AppSpacing.s))
                PrimaryButton(tr("Start practice"), onClick = { onStart(setup) }, modifier = Modifier.testTag("start-practice"), enabled = canStart, size = ButtonSize.Large)
            }
            // A phone has no room for the summary beside the buttons: it goes above them.
            if (compact) {
                Column(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xl, vertical = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    Text(summary, Modifier.testTag("practice-summary"), style = AppText.emphasis)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { buttons() }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xxxl, vertical = AppSpacing.l), verticalAlignment = Alignment.CenterVertically) {
                    Text(summary, Modifier.weight(1f).padding(end = AppSpacing.m).testTag("practice-summary"), style = AppText.emphasis)
                    buttons()
                }
            }
        }
    }
}

/** One of the choices above the types: its [title], its chips, and a [note] under them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Option(title: String, note: String? = null, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(title.uppercase(), style = AppText.overline, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) { chips() }
        if (note != null) Text(note, Modifier.testTag("practice-examples"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The [content] in a row, each as wide as the widest and no narrower than it is high, so short labels make even circles. */
@Composable
private fun SameWidths(content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val gap = AppSpacing.s.roundToPx()
        val height = measurables.maxOfOrNull { it.minIntrinsicHeight(Constraints.Infinity) } ?: 0
        val width = maxOf(height, measurables.maxOfOrNull { it.maxIntrinsicWidth(height) } ?: 0)
        val placeables = measurables.map { it.measure(Constraints.fixed(width, height)) }
        layout(if (placeables.isEmpty()) 0 else width * placeables.size + gap * (placeables.size - 1), height) {
            placeables.forEachIndexed { i, placeable -> placeable.place(i * (width + gap), 0) }
        }
    }
}

/** The [types] as cards, [columns] to a row, the cards of a row as tall as each other. */
@Composable
private fun TypeGrid(types: List<ExerciseType>, columns: Int, card: @Composable (ExerciseType, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        types.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                row.forEach { card(it, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A type of question to tick, with how many words it can ask about, or why none; greyed when it would ask nothing. */
@Composable
private fun TypeCard(type: ExerciseType, ticked: Boolean, state: PracticeSetupState, availability: Pair<Int, String?>, total: Int, answerable: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (words, reason) = availability
    val idle = !state.loading && (words == 0 || !answerable)
    Row(
        modifier.clip(AppShapes.card)
            .background(if (ticked) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
            .border(AppBorders.hairline(if (ticked) colors.primary else colors.outlineVariant), AppShapes.card)
            .clickable(onClick = onToggle).padding(end = AppSpacing.l, top = AppSpacing.m, bottom = AppSpacing.m, start = AppSpacing.xs)
            .testTag("practice-type-${type.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        AppCheckbox(checked = ticked, onCheckedChange = { onToggle() })
        Icon(type.icon(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconLarge).alpha(if (idle) AppAlpha.disabled else 1f))
        Column(Modifier.weight(1f).padding(start = AppSpacing.s).alpha(if (idle) AppAlpha.disabled else 1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Text(type.label(), style = AppText.itemTitle)
            Text(type.description(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text(
                when {
                    state.loading -> tr("Checking…")
                    !answerable -> tr("Not answered the ways ticked")
                    words == total -> trPlural(words, "{0} word", "{0} words")
                    reason == null -> tr("For {0} of {1} words", words, total)
                    type == ExerciseType.BUILD && state.translating -> tr("Translating the sentences…")
                    else -> tr("For {0} of {1} words", words, total) + " · " + reason
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (idle) colors.error else colors.primary,
                modifier = Modifier.testTag("practice-availability-${type.name}"),
            )
        }
    }
}
