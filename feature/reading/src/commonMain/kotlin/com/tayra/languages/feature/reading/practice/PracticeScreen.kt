package com.tayra.languages.feature.reading.practice

import com.tayra.languages.core.ui.components.EdgeScrollbar
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.practice.Exercise
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.Notice
import com.tayra.languages.core.ui.components.PopIn
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ProgressTrack
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.unit.Dp
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.domain.practice.PracticeSource
import com.tayra.languages.core.ui.components.PracticeQuestion
import com.tayra.languages.core.domain.practice.PracticeSetup
import com.tayra.languages.core.domain.practice.PracticeResponse
import com.tayra.languages.core.domain.practice.PracticeContext
import com.tayra.languages.core.domain.practice.ExerciseType

/** Practising the words being learned on a page, right after reading it, or words chosen in the vocabulary. */
@Composable
fun PracticeScreen(
    source: PracticeSource,
    setup: PracticeSetup,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    /** In a dialog over the page that started it: no app bar, and a close button beside the title. */
    dialog: Boolean = false,
    viewModel: PracticeViewModel = koinViewModel(key = "practice-$source-${setup.encode()}") { parametersOf(source, setup) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker(koinInject(), koinInject(), koinInject<SentenceAudio>())
    DisposableEffect(speaker) { onDispose { speaker.stop() } }
    Scaffold(
        topBar = { if (!dialog) AppTopBar(title = tr("Practice"), onNavigate = onNavigate, section = if (source is PracticeSource.Words) NavSection.TERMS else NavSection.BOOKS, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        PracticeContent(
            state = state,
            actions = PracticeActions(
                onChoose = viewModel::choose,
                onTyped = viewModel::setTyped,
                onSubmit = viewModel::submit,
                onSkip = viewModel::skip,
                onNext = { speaker.stop(); viewModel.next() },
                onRestart = viewModel::start,
                onRestartMissed = viewModel::startMissed,
                onBack = onBack,
                onSpeak = { text -> speaker.speak(text, state.languageCode) },
            ),
            modifier = Modifier.padding(padding),
            onClose = if (dialog) onBack else null,
        )
    }
}

/** The practice a [Route.Practice] or [Route.PracticeWords] asks for, or null for any other route. */
internal fun practiceOf(route: Route): Pair<PracticeSource, PracticeSetup>? {
    val (source, kinds, setup) = when (route) {
        is Route.Practice -> Triple(route.page?.let { PracticeSource.Page(route.bookId, it) } ?: PracticeSource.Book(route.bookId), route.kinds, route.setup)
        is Route.PracticeWords -> Triple(PracticeSource.Words(route.termIds), route.kinds, route.setup)
        else -> return null
    }
    return source to if (setup.isNotEmpty()) PracticeSetup.decode(setup) else PracticeSetup.ofKinds(kinds)
}

/** A practice in a dialog that fills the window but for its margin, over the page that started it. */
@Composable
fun PracticeDialog(route: Route, onNavigate: (Route) -> Unit, onClose: () -> Unit) {
    val (source, setup) = practiceOf(route) ?: return
    AppDialogSurface(
        onDismissRequest = onClose,
        modifier = Modifier.fillMaxHeight().testTag("practice-dialog"),
        maxWidth = Dp.Infinity,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        PracticeScreen(source, setup, onNavigate = { onClose(); onNavigate(it) }, onBack = onClose, dialog = true)
    }
}

internal class PracticeActions(
    val onChoose: (String) -> Unit = {},
    val onTyped: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onSkip: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onRestart: () -> Unit = {},
    val onRestartMissed: () -> Unit = {},
    val onBack: () -> Unit = {},
    /** Reads text aloud; null when there is nothing to read it with, which hides the listen buttons. */
    val onSpeak: ((String) -> Unit)? = null,
)

private const val BLANK = "[...]"
private val PRACTICE_WIDTH = 900.dp
private val RESULT_BADGE_SIZE = 64.dp

/** The page under the top bar: the question on show, or the result when all are answered. */
@Composable
internal fun PracticeContent(state: PracticeUiState, actions: PracticeActions, modifier: Modifier = Modifier, onClose: (() -> Unit)? = null) {
    val compact = LocalWindowWidth.current.isCompact
    val exercise = state.current
    val rootFocus = remember { FocusRequester() }
    // A new listening question is read aloud as it comes up.
    LaunchedEffect(state.index, exercise?.spoken) { exercise?.spoken?.let { actions.onSpeak?.invoke(it) } }
    LaunchedEffect(state.index, state.answered, state.finished) {
        if (exercise == null || exercise.isChoice || state.answered) runCatching { rootFocus.requestFocus() }
    }
    val scroll = rememberScrollState()
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().verticalScroll(scroll)
                .focusRequester(rootFocus).focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || exercise == null || event.isCtrlPressed || event.isMetaPressed) return@onPreviewKeyEvent false
                    val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
                    when {
                        state.answered && enter -> actions.onNext()
                        state.answered -> return@onPreviewKeyEvent false
                        !exercise.isChoice && enter -> actions.onSubmit()
                        exercise.isChoice -> {
                            val option = listOf(Key.One, Key.Two, Key.Three, Key.Four).indexOf(event.key).takeIf { it >= 0 }?.let { exercise.options.getOrNull(it) }
                                ?: return@onPreviewKeyEvent false
                            actions.onChoose(option)
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.widthIn(max = PRACTICE_WIDTH).fillMaxWidth().padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.l else AppSpacing.xl),
            ) {
                Column {
                    // On a phone the bar carries the title; in a dialog the title has the way out beside it.
                    if (!compact) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Practice"), Modifier.weight(1f), style = AppText.screenTitle)
                        if (onClose != null) {
                            AppIconButton(Icons.Default.Close, tr("Close"), onClick = onClose, modifier = Modifier.testTag("close-practice-dialog"), style = IconButtonStyle.Tonal)
                        }
                    }
                    Text(
                        when {
                            state.wholeBook -> trPlural(state.wordCount, "The {0} word you are learning in {1}.", "The {0} words you are learning in {1}.", state.bookTitle)
                            state.pageNumber == null -> trPlural(state.wordCount, "{0} word from your vocabulary.", "{0} words from your vocabulary.")
                            state.bookTitle.isEmpty() -> tr("The words you are learning on page {0}.", state.pageNumber)
                            else -> tr("The words you are learning on page {0} of {1}.", state.pageNumber, state.bookTitle)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Fewer than all the kinds: which ones.
                    val asked = state.setup.rotation.mapNotNull { (type, response) -> PracticeQuestion.of(type, response) }
                    if (asked.size < PracticeQuestion.all.size) {
                        Text(
                            tr("Questions: {0}", asked.joinToString(", ") { it.label.lowercase() }),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                when {
                    state.exercises.isEmpty() -> Empty(state, actions)
                    exercise == null -> Finished(state, actions)
                    else -> {
                        ProgressStrip(state)
                        Question(state, exercise, actions, compact)
                    }
                }
            }
        }
        EdgeScrollbar(scroll)
    }
}

@Composable
private fun ProgressStrip(state: PracticeUiState) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().cardSurface(AppShapes.card).padding(horizontal = AppSpacing.xl, vertical = AppSpacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl),
    ) {
        Text(tr("Question {0} of {1}", state.index + 1, state.exercises.size), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, softWrap = false)
        // Filled to the questions answered, the current one counting once it is answered.
        ProgressTrack((state.index + if (state.answered) 1f else 0f) / state.exercises.size, colors.primary, Modifier.weight(1f), height = 10.dp, trackColor = colors.onSurface.copy(alpha = AppAlpha.tint))
        Text(tr("{0} right", state.correct), style = AppText.emphasis, color = AppAccents.green, softWrap = false)
    }
}

private val Exercise.instruction: String
    get() = when (type) {
        ExerciseType.GAP -> when {
            response == PracticeResponse.TYPE -> tr("Type the missing word")
            context == PracticeContext.EXAMPLE -> tr("A new sentence: choose the missing word")
            else -> tr("Choose the missing word")
        }
        ExerciseType.MEANING -> tr("What does the marked word mean?")
        ExerciseType.HEAR_WORD -> if (answer == word) tr("Listen and choose the word you hear") else tr("Listen and choose what the word means")
        ExerciseType.DICTATION -> if (wholeSentence) tr("Listen and type the sentence") else tr("Listen and type the missing word")
        ExerciseType.TRANSLATION -> if (response == PracticeResponse.CHOOSE) tr("Read the translation and choose the missing word") else tr("Read the translation and type the missing word")
    }

@Composable
private fun Question(state: PracticeUiState, exercise: Exercise, actions: PracticeActions, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val direction = if (state.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
    val answered = state.answered
    Column(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large)
            .padding(horizontal = if (compact) AppSpacing.xl else AppSpacing.xxxl, vertical = if (compact) AppSpacing.xl else AppSpacing.xxl).testTag("practice-question"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.l else AppSpacing.xl),
    ) {
        Text(exercise.instruction.uppercase(), style = AppText.overline.copy(color = colors.primary), textAlign = TextAlign.Center)

        if (exercise.isListening && actions.onSpeak != null) {
            SecondaryButton(tr("Listen again"), onClick = { actions.onSpeak.invoke(exercise.spoken!!) }, icon = AppIcons.VolumeUp, size = ButtonSize.Large, tint = colors.primary)
        }

        // Captioned, so the box says what it is whether it comes before the sentence, under it, or after the answer.
        val translationBox: @Composable (String) -> Unit = { translation ->
            Column(
                Modifier.fillMaxWidth().clip(AppShapes.control).background(colors.primary.copy(alpha = AppAlpha.wash)).padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = AppSpacing.l)
                    .testTag("practice-translation"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                Text(tr("Translation"), style = AppText.overline, color = colors.onSurfaceVariant)
                Text(translation, style = AppText.cardTranslation(compact).copy(color = colors.onSurfaceVariant, textAlign = TextAlign.Center))
            }
        }
        // A translation question starts from the translation: it comes first, the sentence with its gap under it.
        val fromTranslation = exercise.type == ExerciseType.TRANSLATION
        if (fromTranslation) state.sentenceTranslation?.let { translationBox(it) }

        // What is heard is not shown until answered: the word of a listening choice, or a sentence typed whole.
        val sentenceHidden = !answered && (exercise.type == ExerciseType.HEAR_WORD || exercise.wholeSentence)
        if (!sentenceHidden) {
            val highlight = SpanStyle(background = AppAccents.highlight.copy(alpha = AppAlpha.half), fontWeight = AppText.labelStrong.fontWeight)
            Text(
                buildAnnotatedString {
                    append(exercise.sentence.substring(0, exercise.range.first))
                    // The word is marked rather than blanked where its meaning is asked.
                    withStyle(highlight) { append(if (answered || exercise.type == ExerciseType.MEANING) exercise.sentence.substring(exercise.range) else BLANK) }
                    append(exercise.sentence.substring(exercise.range.last + 1))
                },
                style = AppText.cardSentence(if (compact) 21f else 27f).copy(color = colors.onSurface, textAlign = TextAlign.Center, textDirection = direction),
                modifier = Modifier.testTag("practice-sentence"),
            )
        }
        // A typed gap gets the sentence's meaning as a hint; elsewhere it would give the answer away, so it waits.
        val hintShown = answered || (exercise.type == ExerciseType.GAP && exercise.response == PracticeResponse.TYPE)
        if (hintShown && !fromTranslation) state.sentenceTranslation?.let { translationBox(it) }

        if (exercise.isChoice) Choices(state, exercise, actions, compact) else TypedAnswer(state, exercise, actions)

        if (answered) Feedback(state, exercise)

        AppDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!answered) QuietButton(tr("Show the answer"), onClick = actions.onSkip)
            Spacer(Modifier.weight(1f))
            when {
                answered -> PrimaryButton(if (state.index + 1 >= state.exercises.size) tr("See the result") else tr("Continue"), onClick = actions.onNext, size = ButtonSize.Large)
                !exercise.isChoice -> PrimaryButton(tr("Check"), onClick = actions.onSubmit, enabled = state.typed.isNotBlank(), size = ButtonSize.Large)
            }
        }
    }
}

@Composable
private fun Choices(state: PracticeUiState, exercise: Exercise, actions: PracticeActions, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val option: @Composable (Int, String, Modifier) -> Unit = { index, text, modifier ->
        val isAnswer = text == exercise.answer
        val tint = when {
            !state.answered -> null
            isAnswer -> AppAccents.green
            text == state.chosen -> AppAccents.red
            else -> null
        }
        // The right answer and a wrong choice fade to their colours.
        val fill by animateColorAsState(tint?.copy(alpha = AppAlpha.soft) ?: colors.surface, AppMotion.spec(AppMotion.medium))
        val edge by animateColorAsState(tint ?: colors.outlineVariant, AppMotion.spec(AppMotion.medium))
        Row(
            modifier.heightIn(min = AppSizes.row).clip(AppShapes.panel)
                .background(fill)
                .border(if (tint != null) AppBorders.selected(edge) else AppBorders.hairline(edge), AppShapes.panel)
                .clickable(enabled = !state.answered) { actions.onChoose(text) }
                .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.m).testTag("practice-option-$index"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = if (state.answered && tint == null) colors.onSurfaceVariant else colors.onSurface)
            when (tint) {
                AppAccents.green -> PopIn { Icon(Icons.Default.Check, contentDescription = tr("Right answer"), tint = AppAccents.green, modifier = Modifier.size(AppSizes.iconMedium)) }
                AppAccents.red -> Icon(Icons.Default.Close, contentDescription = tr("Your answer"), tint = AppAccents.red, modifier = Modifier.size(AppSizes.iconMedium))
                else -> Unit
            }
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        if (compact) {
            exercise.options.forEachIndexed { i, text -> option(i, text, Modifier.fillMaxWidth()) }
        } else {
            exercise.options.withIndex().chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    pair.forEach { (i, text) -> option(i, text, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TypedAnswer(state: PracticeUiState, exercise: Exercise, actions: PracticeActions) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(state.index) { runCatching { focus.requestFocus() } }
    AppTextField(
        value = state.typed,
        onValueChange = actions.onTyped,
        readOnly = state.answered,
        singleLine = true,
        placeholder = { Text(if (exercise.wholeSentence) tr("Type the sentence") else tr("Type the word")) },
        textStyle = MaterialTheme.typography.titleMedium.copy(textDirection = if (state.rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
        modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("practice-input"),
    )
}

@Composable
private fun Feedback(state: PracticeUiState, exercise: Exercise) {
    val (tint, title) = when (state.result) {
        PracticeResult.CORRECT -> AppAccents.green to tr("Correct")
        PracticeResult.ALMOST -> AppAccents.amber to tr("Almost: mind the accents")
        else -> AppAccents.red to tr("Not quite")
    }
    // The word with its meaning, whatever was asked; for a sentence typed whole, the sentence is above.
    val meaning = exercise.wordTranslation?.let { " — $it" }.orEmpty()
    Notice(
        title,
        Modifier.testTag("practice-feedback"),
        message = "${exercise.word}$meaning",
        tint = tint,
        icon = if (state.result == PracticeResult.WRONG) Icons.Default.Close else Icons.Default.Check,
        // A right answer's tick pops in; a wrong one only fades in, without a jolt.
        popIcon = state.result != PracticeResult.WRONG,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Finished(state: PracticeUiState, actions: PracticeActions) {
    val colors = MaterialTheme.colorScheme
    val total = state.exercises.size
    Column(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Box(Modifier.size(RESULT_BADGE_SIZE).clip(AppShapes.circle).background(AppAccents.green.copy(alpha = AppAlpha.soft)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.green, modifier = Modifier.size(AppSizes.iconXLarge))
        }
        Text(tr("{0} of {1} right", state.correct, total), style = AppText.headline)
        Text(
            if (state.missed.isEmpty()) tr("Every answer was right.") else tr("Words to look at again: {0}", state.missed.joinToString(", ")),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(tr("Practice does not change the status of your words."), style = MaterialTheme.typography.bodySmall, color = colors.outline, textAlign = TextAlign.Center)
        // Missed words lead to practising them again; with none missed, back to where the words came from.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            if (state.missedTargets.isNotEmpty()) {
                PrimaryButton(tr("Practise missed words"), onClick = actions.onRestartMissed, size = ButtonSize.Large, modifier = Modifier.testTag("practise-missed"))
                SecondaryButton(backLabel(state), onClick = actions.onBack, size = ButtonSize.Large)
            } else {
                SecondaryButton(tr("Practise again"), onClick = actions.onRestart, size = ButtonSize.Large)
                PrimaryButton(backLabel(state), onClick = actions.onBack, size = ButtonSize.Large)
            }
        }
    }
}

/** Where Back leads: to the page read, or to the vocabulary the words were chosen in. */
private fun backLabel(state: PracticeUiState): String = if (state.fromVocabulary) tr("Back to vocabulary") else tr("Back to reading")

@Composable
private fun Empty(state: PracticeUiState, actions: PracticeActions) {
    EmptyState(
        icon = AppIcons.School,
        title = if (state.fromVocabulary || state.wholeBook) tr("Nothing to practise") else tr("Nothing to practise on this page"),
        message = when {
            state.fromVocabulary -> tr("Practice asks about a word in the sentence it was read in: none of the chosen words was saved with one.")
            state.wholeBook -> tr("Practice uses the words you are learning: give some words in the text a status of 1 to 4 first.")
            else -> tr("Practice uses the words you are learning: give some words on the page a status of 1 to 4 first.")
        },
        action = { PrimaryButton(backLabel(state), onClick = actions.onBack) },
    )
}
