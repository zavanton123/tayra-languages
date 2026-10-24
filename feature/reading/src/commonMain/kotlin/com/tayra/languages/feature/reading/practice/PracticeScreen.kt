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
import com.tayra.languages.core.domain.practice.PracticeSetup
import com.tayra.languages.core.domain.practice.PracticeResponse
import com.tayra.languages.core.domain.practice.PracticeContext
import com.tayra.languages.core.domain.practice.ExerciseType
import com.tayra.languages.core.ui.components.label
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.tayra.languages.core.domain.practice.WordResult
import com.tayra.languages.core.domain.practice.PracticeChecker
import androidx.compose.ui.text.style.TextDecoration
import com.tayra.languages.core.domain.practice.PracticeBuilder
import com.tayra.languages.core.ui.audio.rememberMicrophoneAccess

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
    val microphone = rememberMicrophoneAccess()
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
                onMatch = viewModel::match,
                onTyped = viewModel::setTyped,
                onSubmit = viewModel::submit,
                onSkip = viewModel::skip,
                onNext = { speaker.stop(); viewModel.next() },
                onRestart = viewModel::start,
                onRestartMissed = viewModel::startMissed,
                onBack = onBack,
                onSpeak = { text -> speaker.speak(text, state.languageCode) },
                onListen = {
                    speaker.stop()
                    microphone { allowed -> if (allowed) viewModel.listen() else viewModel.refuseSpeaking() }
                },
                onStopListening = viewModel::stopListening,
                onTypeInstead = viewModel::typeInstead,
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
    /** Pairs a word with a meaning in a matching question; says whether the pair was right. */
    val onMatch: (word: String, meaning: String) -> Boolean = { _, _ -> false },
    val onTyped: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onSkip: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onRestart: () -> Unit = {},
    val onRestartMissed: () -> Unit = {},
    val onBack: () -> Unit = {},
    /** Reads text aloud; null when there is nothing to read it with, which hides the listen buttons. */
    val onSpeak: ((String) -> Unit)? = null,
    /** Starts hearing the answer said aloud. */
    val onListen: () -> Unit = {},
    val onStopListening: () -> Unit = {},
    val onTypeInstead: () -> Unit = {},
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
                        exercise.response == PracticeResponse.SPEAK -> return@onPreviewKeyEvent false
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
                    val asked = ExerciseType.entries.filter { type -> state.setup.rotation.any { it.first == type } }
                    if (asked.size < ExerciseType.entries.size) {
                        Text(
                            tr("Questions: {0}", asked.joinToString(", ") { it.label().lowercase() }),
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
                        if (state.speakingRefused) {
                            Notice(tr("The microphone could not be used"), Modifier.testTag("practice-speaking-refused"), message = tr("Type the answers instead."), icon = AppIcons.Mic)
                        }
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
        ExerciseType.GAP -> when (response) {
            PracticeResponse.CHOOSE -> if (context == PracticeContext.EXAMPLE) tr("A new sentence: choose the missing word") else tr("Choose the missing word")
            PracticeResponse.TYPE -> if (context == PracticeContext.EXAMPLE) tr("A new sentence: type the missing word") else tr("Type the missing word")
            PracticeResponse.SPEAK -> if (context == PracticeContext.EXAMPLE) tr("A new sentence: say it with the missing word") else tr("Say the sentence with the missing word")
        }
        ExerciseType.MEANING -> tr("What does the marked word mean?")
        ExerciseType.MATCHING -> tr("Match each word with its meaning")
        ExerciseType.SOUND_ALIKE -> tr("Listen and choose the word you hear")
        ExerciseType.TRUE_FALSE -> tr("Is this what the marked word means?")
        ExerciseType.HEAR_SENTENCE -> tr("Listen to the sentence")
        ExerciseType.HEAR_WORD -> if (answer == word) tr("Listen and choose the word you hear") else tr("Listen and choose what the word means")
        ExerciseType.SHADOW -> tr("Listen and say the sentence")
        ExerciseType.DICTATION -> if (wholeSentence) tr("Listen and type the sentence") else tr("Listen and type the missing word")
        ExerciseType.BUILD -> tr("Put the words in order")
        ExerciseType.FORM -> when (response) {
            PracticeResponse.CHOOSE -> tr("Choose the form of “{0}” that fits", lemma.orEmpty())
            PracticeResponse.TYPE, PracticeResponse.SPEAK -> tr("Type the form of “{0}” that fits", lemma.orEmpty())
        }
        ExerciseType.TRANSLATION -> when (response) {
            PracticeResponse.CHOOSE -> tr("Read the translation and choose the missing word")
            PracticeResponse.TYPE -> tr("Read the translation and type the missing word")
            PracticeResponse.SPEAK -> tr("Read the translation and say the sentence")
        }
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
        if (exercise.type == ExerciseType.MATCHING) {
            Matching(state, exercise, actions, compact)
            if (answered) Feedback(state, exercise)
            AppDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!answered) QuietButton(tr("Show the answer"), onClick = actions.onSkip)
                Spacer(Modifier.weight(1f))
                if (answered) PrimaryButton(if (state.index + 1 >= state.exercises.size) tr("See the result") else tr("Continue"), onClick = actions.onNext, size = ButtonSize.Large)
            }
            return@Column
        }

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
        val fromTranslation = exercise.type == ExerciseType.TRANSLATION || exercise.type == ExerciseType.BUILD
        if (fromTranslation) state.sentenceTranslation?.let { translationBox(it) }

        // What is heard is not shown until answered: the word of a listening choice, or a sentence typed whole.
        val sentenceHidden = !answered && (exercise.type == ExerciseType.HEAR_WORD || exercise.type == ExerciseType.HEAR_SENTENCE || exercise.type == ExerciseType.SOUND_ALIKE || exercise.wholeSentence)
        // The sentence heard is hidden; what is asked is the word with this meaning.
        if (exercise.type == ExerciseType.HEAR_SENTENCE) {
            Text(
                // A sentence from elsewhere may use another form: the question names the word practised.
                if (exercise.otherForm) tr("Which word is a form of “{0}” (“{1}”)?", exercise.practised, exercise.wordTranslation.orEmpty())
                else tr("Which word means “{0}”?", exercise.wordTranslation.orEmpty()),
                style = AppText.cardSentence(if (compact) 21f else 27f).copy(color = colors.onSurface, textAlign = TextAlign.Center),
                modifier = Modifier.testTag("practice-asked"),
            )
        }
        if (!sentenceHidden) {
            val highlight = SpanStyle(background = AppAccents.highlight.copy(alpha = AppAlpha.half), fontWeight = AppText.labelStrong.fontWeight)
            Text(
                buildAnnotatedString {
                    append(exercise.sentence.substring(0, exercise.range.first))
                    // The word is marked rather than blanked where its meaning is asked.
                    withStyle(highlight) { append(if (answered || exercise.type == ExerciseType.MEANING || exercise.type == ExerciseType.TRUE_FALSE) exercise.sentence.substring(exercise.range) else BLANK) }
                    append(exercise.sentence.substring(exercise.range.last + 1))
                },
                style = AppText.cardSentence(if (compact) 21f else 27f).copy(color = colors.onSurface, textAlign = TextAlign.Center, textDirection = direction),
                modifier = Modifier.testTag("practice-sentence"),
            )
        }
        // A sentence from a source that asks to be named, named once it is shown.
        if (!sentenceHidden) exercise.credit?.let { credit ->
            Text(tr("Sentence from {0}", credit), style = MaterialTheme.typography.bodySmall, color = colors.outline, modifier = Modifier.testTag("practice-credit"))
        }
        exercise.offered?.let { offered ->
            Text(
                "${exercise.word} — $offered?",
                style = AppText.cardSentence(if (compact) 19f else 23f).copy(color = colors.primary, textAlign = TextAlign.Center),
                modifier = Modifier.testTag("practice-offered"),
            )
        }
        // A typed gap gets the sentence's meaning as a hint; elsewhere it would give the answer away, so it waits.
        val hintShown = answered || (exercise.type == ExerciseType.GAP && (exercise.response == PracticeResponse.TYPE || exercise.response == PracticeResponse.SPEAK)) || exercise.type == ExerciseType.FORM
        if (hintShown && !fromTranslation) state.sentenceTranslation?.let { translationBox(it) }

        when {
            exercise.isChoice -> Choices(state, exercise, actions, compact)
            exercise.tiles.isNotEmpty() -> TilesAnswer(state, exercise, actions)
            exercise.response == PracticeResponse.SPEAK -> SpokenAnswer(state, actions)
            else -> TypedAnswer(state, exercise, actions)
        }

        if (answered) Feedback(state, exercise)
        // A sentence typed whole and not quite right: which words were.
        if (answered && exercise.wholeSentence && state.result != PracticeResult.CORRECT && state.typed.isNotBlank()) SentenceCheck(exercise.answer, state.typed)

        AppDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!answered) QuietButton(tr("Show the answer"), onClick = actions.onSkip)
            Spacer(Modifier.weight(1f))
            when {
                answered -> PrimaryButton(if (state.index + 1 >= state.exercises.size) tr("See the result") else tr("Continue"), onClick = actions.onNext, size = ButtonSize.Large)
                !exercise.isChoice && exercise.response != PracticeResponse.SPEAK -> PrimaryButton(tr("Check"), onClick = actions.onSubmit, enabled = state.typed.isNotBlank(), size = ButtonSize.Large)
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
            val label = when {
                exercise.type != ExerciseType.TRUE_FALSE -> text
                text == PracticeBuilder.TRUE -> tr("Yes, it means that")
                else -> tr("No, it does not")
            }
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = if (state.answered && tint == null) colors.onSurfaceVariant else colors.onSurface)
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

/**
 * Words on one side, their meanings shuffled on the other: tap a word, then its meaning. A right
 * pair turns green and stays; a wrong one flashes red and the word stays picked to try again.
 */
@Composable
private fun Matching(state: PracticeUiState, exercise: Exercise, actions: PracticeActions, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val meanings = remember(state.index) { exercise.pairs.map { it.second }.shuffled() }
    val words = remember(state.index) { exercise.pairs.map { it.first }.shuffled() }
    var picked by remember(state.index) { mutableStateOf<String?>(null) }
    var wrong by remember(state.index) { mutableStateOf<String?>(null) }
    val meaningOf = exercise.pairs.toMap()
    val cell: @Composable (String, Boolean, Boolean, Boolean, String, () -> Unit) -> Unit = { text, done, chosen, mistaken, tag, onClick ->
        val tint = when {
            done -> AppAccents.green
            mistaken -> AppAccents.red
            chosen -> colors.primary
            else -> null
        }
        Box(
            Modifier.fillMaxWidth().heightIn(min = AppSizes.row).clip(AppShapes.panel)
                .background(tint?.copy(alpha = AppAlpha.soft) ?: colors.surface)
                .border(if (tint != null) AppBorders.selected(tint) else AppBorders.hairline(colors.outlineVariant), AppShapes.panel)
                .clickable(enabled = !done && !state.answered, onClick = onClick)
                .padding(horizontal = AppSpacing.l, vertical = AppSpacing.m).testTag(tag),
            contentAlignment = Alignment.CenterStart,
        ) { Text(text, style = MaterialTheme.typography.titleMedium, color = if (done) colors.onSurfaceVariant else colors.onSurface) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.m else AppSpacing.xl)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            words.forEachIndexed { i, word ->
                cell(word, word in state.matched, picked == word, false, "practice-match-word-$i") { picked = word; wrong = null }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            meanings.forEachIndexed { i, meaning ->
                val done = state.matched.any { meaningOf[it] == meaning }
                cell(meaning, done, false, wrong == meaning, "practice-match-meaning-$i") {
                    val word = picked ?: return@cell
                    if (actions.onMatch(word, meaning)) { picked = null; wrong = null } else wrong = meaning
                }
            }
        }
    }
}

/** The sentence built from its words: each tile is used once, the last one taken back with the button beside them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TilesAnswer(state: PracticeUiState, exercise: Exercise, actions: PracticeActions) {
    val colors = MaterialTheme.colorScheme
    var used by remember(state.index) { mutableStateOf(emptyList<Int>()) }
    val build: (List<Int>) -> Unit = { picked -> used = picked; actions.onTyped(picked.joinToString(" ") { exercise.tiles[it] }) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = AppSizes.row).clip(AppShapes.control).border(AppBorders.hairline(), AppShapes.control).padding(horizontal = AppSpacing.l)
                .testTag("practice-built"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                state.typed.ifEmpty { tr("Tap the words in order") },
                style = if (state.typed.isEmpty()) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineSmall,
                color = if (state.typed.isEmpty()) colors.onSurfaceVariant else colors.onSurface,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            exercise.tiles.forEachIndexed { index, word ->
                val free = index !in used && !state.answered
                Box(
                    Modifier.heightIn(min = AppSizes.touch).widthIn(min = AppSizes.touch).clip(AppShapes.control)
                        .background(if (free) colors.primary.copy(alpha = AppAlpha.wash) else colors.surface)
                        .border(AppBorders.hairline(if (free) colors.primary else colors.outlineVariant), AppShapes.control)
                        .clickable(enabled = free) { build(used + index) }
                        .testTag("practice-tile-$index"),
                    contentAlignment = Alignment.Center,
                ) { Text(word, Modifier.padding(horizontal = AppSpacing.m), style = MaterialTheme.typography.titleLarge, color = if (free) colors.onSurface else colors.outline) }
            }
            AppIconButton(AppIcons.ArrowLeft, tr("Remove the last word"), onClick = { build(used.dropLast(1)) }, enabled = used.isNotEmpty() && !state.answered, modifier = Modifier.testTag("practice-tile-back"))
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

/** The answer said aloud: a button to start and stop hearing it, what was heard, and the way to type it instead. */
@Composable
private fun SpokenAnswer(state: PracticeUiState, actions: PracticeActions) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        if (!state.answered) {
            PrimaryButton(
                if (state.listening) tr("Stop") else tr("Speak"),
                onClick = if (state.listening) actions.onStopListening else actions.onListen,
                icon = AppIcons.Mic,
                size = ButtonSize.Large,
                modifier = Modifier.testTag("practice-speak"),
            )
        }
        val note = when {
            state.listening -> tr("Listening…")
            state.typed.isNotBlank() -> tr("Heard: {0}", state.typed)
            state.nothingHeard -> tr("Nothing was heard. Try again.")
            else -> null
        }
        note?.let {
            Text(
                it,
                Modifier.testTag("practice-heard"),
                style = MaterialTheme.typography.titleMedium.copy(textDirection = if (state.rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (!state.answered) QuietButton(tr("Type instead"), onClick = actions.onTypeInstead, modifier = Modifier.testTag("practice-type-instead"))
    }
}

/** The sentence expected, word by word as it was typed: right, right but for accents, wrong (with what was typed), or missing. */
@Composable
private fun SentenceCheck(expected: String, typed: String) {
    val colors = MaterialTheme.colorScheme
    val checks = remember(expected, typed) { PracticeChecker.compareWords(expected, typed) }
    Text(
        buildAnnotatedString {
            checks.forEachIndexed { index, check ->
                if (index > 0) append(" ")
                when (check.result) {
                    WordResult.RIGHT -> withStyle(SpanStyle(color = AppAccents.green)) { append(check.word) }
                    WordResult.ACCENT -> withStyle(SpanStyle(color = AppAccents.amber, fontWeight = AppText.labelStrong.fontWeight)) { append(check.word) }
                    WordResult.WRONG -> {
                        withStyle(SpanStyle(color = AppAccents.red, fontWeight = AppText.labelStrong.fontWeight)) { append(check.word) }
                        withStyle(SpanStyle(color = colors.onSurfaceVariant, textDecoration = TextDecoration.LineThrough)) { append(" ${check.typed}") }
                    }
                    WordResult.MISSING -> withStyle(SpanStyle(color = AppAccents.red, textDecoration = TextDecoration.Underline)) { append(check.word) }
                }
            }
        },
        Modifier.fillMaxWidth().testTag("practice-sentence-check"),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Feedback(state: PracticeUiState, exercise: Exercise) {
    val (tint, title) = when (state.result) {
        PracticeResult.CORRECT -> AppAccents.green to tr("Correct")
        // Said aloud, almost is a sentence most of whose words were heard.
        PracticeResult.ALMOST -> AppAccents.amber to if (exercise.response == PracticeResponse.SPEAK) tr("Almost: most of the words were heard") else tr("Almost: mind the accents")
        PracticeResult.TYPO -> AppAccents.amber to tr("Almost: mind the spelling")
        else -> AppAccents.red to tr("Not quite")
    }
    if (exercise.type == ExerciseType.MATCHING) {
        Notice(
            if (state.result == PracticeResult.WRONG) tr("Not quite") else if (state.wrongPairs == 0) tr("Correct") else tr("Almost"),
            Modifier.testTag("practice-feedback"),
            message = if (state.result == PracticeResult.WRONG) tr("The pairs are shown above.") else trPlural(state.wrongPairs, "Every pair matched, with {0} wrong try", "Every pair matched, with {0} wrong tries").takeIf { state.wrongPairs > 0 } ?: tr("Every pair matched."),
            tint = if (state.result == PracticeResult.WRONG) AppAccents.red else if (state.wrongPairs == 0) AppAccents.green else AppAccents.amber,
            icon = if (state.result == PracticeResult.WRONG) Icons.Default.Close else Icons.Default.Check,
            popIcon = state.result != PracticeResult.WRONG,
        )
        return
    }
    // The word with its meaning, whatever was asked; for a sentence typed whole, the sentence is above.
    val meaning = exercise.wordTranslation?.let { " — $it" }.orEmpty()
    Notice(
        title,
        Modifier.testTag("practice-feedback"),
        message = (if (exercise.otherForm) tr("{0}, a form of {1}", exercise.word, exercise.practised) else exercise.word) + meaning,
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
