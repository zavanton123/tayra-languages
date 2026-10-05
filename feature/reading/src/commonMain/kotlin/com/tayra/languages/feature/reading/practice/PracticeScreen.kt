package com.tayra.languages.feature.reading.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.practice.Exercise
import com.tayra.languages.core.domain.practice.ExerciseKind
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Practising the words being learned on a page, right after reading it. */
@Composable
fun PracticeScreen(
    bookId: Long,
    page: Int,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    viewModel: PracticeViewModel = koinViewModel(key = "practice-$bookId-$page") { parametersOf(bookId, page) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker(koinInject(), koinInject(), koinInject<SentenceAudio>())
    DisposableEffect(speaker) { onDispose { speaker.stop() } }
    Scaffold(
        topBar = { AppTopBar(title = "Practice", onNavigate = onNavigate, section = NavSection.BOOKS, onBack = onBack) },
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
                onBack = onBack,
                onSpeak = { text -> speaker.speak(text, state.languageCode) },
            ),
            modifier = Modifier.padding(padding),
        )
    }
}

internal class PracticeActions(
    val onChoose: (String) -> Unit = {},
    val onTyped: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onSkip: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onRestart: () -> Unit = {},
    val onBack: () -> Unit = {},
    /** Reads text aloud; null when there is nothing to read it with, which hides the listen buttons. */
    val onSpeak: ((String) -> Unit)? = null,
)

private val WORD_HIGHLIGHT = Color(0xFFFFD166)
private val RIGHT = Color(0xFF2E9D57)
private val WRONG = Color(0xFFD64545)
private val ALMOST = Color(0xFFD68A00)
private const val BLANK = "[...]"

/** The page under the top bar: the question on show, or the result when all are answered. */
@Composable
internal fun PracticeContent(state: PracticeUiState, actions: PracticeActions, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    val exercise = state.current
    val rootFocus = remember { FocusRequester() }
    // A new listening question is read aloud as it comes up.
    LaunchedEffect(state.index, exercise?.spoken) { exercise?.spoken?.let { actions.onSpeak?.invoke(it) } }
    LaunchedEffect(state.index, state.answered, state.finished) {
        if (exercise == null || exercise.isChoice || state.answered) runCatching { rootFocus.requestFocus() }
    }
    Box(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
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
            Modifier.widthIn(max = 900.dp).fillMaxWidth().padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 20.dp),
        ) {
            Column {
                Text("Practice", style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "The words you are learning on page ${state.pageNumber}${if (state.bookTitle.isEmpty()) "" else " of ${state.bookTitle}"}.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                state.exercises.isEmpty() -> Empty(actions)
                exercise == null -> Finished(state, actions)
                else -> {
                    ProgressStrip(state)
                    Question(state, exercise, actions, compact)
                }
            }
        }
    }
}

@Composable
private fun ProgressStrip(state: PracticeUiState) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Question ${state.index + 1} of ${state.exercises.size}", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, softWrap = false)
        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(colors.onSurface.copy(alpha = 0.08f))) {
            Box(Modifier.fillMaxWidth((state.index + 1f) / state.exercises.size).height(10.dp).clip(RoundedCornerShape(5.dp)).background(colors.primary))
        }
        Text("${state.correct} right", style = MaterialTheme.typography.bodyLarge, color = RIGHT, fontWeight = FontWeight.Medium, softWrap = false)
    }
}

private val Exercise.instruction: String
    get() = when (kind) {
        ExerciseKind.GAP_CHOICE -> "Choose the missing word"
        ExerciseKind.GAP_TYPED -> "Type the missing word"
        ExerciseKind.NEW_CONTEXT -> "A new sentence: choose the missing word"
        ExerciseKind.HEAR_CHOOSE -> if (answer == word) "Listen and choose the word you hear" else "Listen and choose what the word means"
        ExerciseKind.DICTATION -> if (wholeSentence) "Listen and type the sentence" else "Listen and type the missing word"
    }

@Composable
private fun Question(state: PracticeUiState, exercise: Exercise, actions: PracticeActions, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val direction = if (state.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
    val answered = state.answered
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp))
            .padding(horizontal = if (compact) 18.dp else 32.dp, vertical = if (compact) 18.dp else 26.dp).testTag("practice-question"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
    ) {
        Text(exercise.instruction.uppercase(), style = TextStyle(fontSize = 13.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold, color = colors.primary), textAlign = TextAlign.Center)

        if (exercise.isListening && actions.onSpeak != null) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(colors.primary.copy(alpha = 0.08f)).border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(50))
                    .clickable(onClickLabel = "Listen again") { actions.onSpeak.invoke(exercise.spoken!!) }.padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(AppIcons.VolumeUp, contentDescription = null, tint = colors.primary, modifier = Modifier.size(26.dp))
                Text("Listen again", style = MaterialTheme.typography.bodyLarge, color = colors.primary, fontWeight = FontWeight.Medium)
            }
        }

        // What is heard is not shown until answered: the word of a listening choice, or a sentence typed whole.
        val sentenceHidden = !answered && (exercise.kind == ExerciseKind.HEAR_CHOOSE || exercise.wholeSentence)
        if (!sentenceHidden) {
            val highlight = SpanStyle(background = WORD_HIGHLIGHT.copy(alpha = 0.55f), fontWeight = FontWeight.SemiBold)
            val size = if (compact) 21.sp else 27.sp
            Text(
                buildAnnotatedString {
                    append(exercise.sentence.substring(0, exercise.range.first))
                    withStyle(highlight) { append(if (answered) exercise.sentence.substring(exercise.range) else BLANK) }
                    append(exercise.sentence.substring(exercise.range.last + 1))
                },
                style = TextStyle(fontSize = size, lineHeight = size * 1.45f, color = colors.onSurface, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, textDirection = direction),
                modifier = Modifier.testTag("practice-sentence"),
            )
        }
        // A typed gap gets the sentence's meaning as a hint; elsewhere it would give the answer away, so it waits.
        val hintShown = answered || exercise.kind == ExerciseKind.GAP_TYPED
        if (hintShown) state.sentenceTranslation?.let { translation ->
            Text(
                translation,
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.primary.copy(alpha = 0.05f)).padding(horizontal = if (compact) 14.dp else 32.dp, vertical = 14.dp),
                style = TextStyle(fontSize = if (compact) 15.sp else 17.sp, lineHeight = 25.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center),
            )
        }

        if (exercise.isChoice) Choices(state, exercise, actions, compact) else TypedAnswer(state, exercise, actions)

        if (answered) Feedback(state, exercise)

        HorizontalDivider(color = colors.outlineVariant)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!answered) TextButton(onClick = actions.onSkip) { Text("Show the answer") }
            Spacer(Modifier.weight(1f))
            when {
                answered -> Button(onClick = actions.onNext, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(48.dp)) {
                    Text(if (state.index + 1 >= state.exercises.size) "See the result" else "Continue", Modifier.padding(horizontal = 14.dp), fontWeight = FontWeight.SemiBold)
                }
                !exercise.isChoice -> Button(onClick = actions.onSubmit, enabled = state.typed.isNotBlank(), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(48.dp)) {
                    Text("Check", Modifier.padding(horizontal = 14.dp), fontWeight = FontWeight.SemiBold)
                }
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
            isAnswer -> RIGHT
            text == state.chosen -> WRONG
            else -> null
        }
        Row(
            modifier.heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp))
                .background(tint?.copy(alpha = 0.12f) ?: colors.surface)
                .border(if (tint != null) 1.5.dp else 1.dp, tint ?: colors.outlineVariant, RoundedCornerShape(12.dp))
                .clickable(enabled = !state.answered) { actions.onChoose(text) }
                .padding(horizontal = 18.dp, vertical = 12.dp).testTag("practice-option-$index"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = if (state.answered && tint == null) colors.onSurfaceVariant else colors.onSurface)
            when (tint) {
                RIGHT -> Icon(Icons.Default.Check, contentDescription = "Right answer", tint = RIGHT, modifier = Modifier.size(22.dp))
                WRONG -> Icon(Icons.Default.Close, contentDescription = "Your answer", tint = WRONG, modifier = Modifier.size(22.dp))
                else -> Unit
            }
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (compact) {
            exercise.options.forEachIndexed { i, text -> option(i, text, Modifier.fillMaxWidth()) }
        } else {
            exercise.options.withIndex().chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
    OutlinedTextField(
        value = state.typed,
        onValueChange = actions.onTyped,
        readOnly = state.answered,
        singleLine = true,
        placeholder = { Text(if (exercise.wholeSentence) "Type the sentence" else "Type the word") },
        textStyle = MaterialTheme.typography.titleMedium.copy(textDirection = if (state.rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("practice-input"),
    )
}

@Composable
private fun Feedback(state: PracticeUiState, exercise: Exercise) {
    val (tint, title) = when (state.result) {
        PracticeResult.CORRECT -> RIGHT to "Correct"
        PracticeResult.ALMOST -> ALMOST to "Almost: mind the accents"
        else -> WRONG to "Not quite"
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.1f)).border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp).testTag("practice-feedback"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
            Icon(if (state.result == PracticeResult.WRONG) Icons.Default.Close else Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tint)
            // The word with its meaning, whatever was asked; for a sentence typed whole, the sentence is above.
            val meaning = exercise.wordTranslation?.let { " — $it" }.orEmpty()
            Text("${exercise.word}$meaning", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Finished(state: PracticeUiState, actions: PracticeActions) {
    val colors = MaterialTheme.colorScheme
    val total = state.exercises.size
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp)).padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(RIGHT.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = RIGHT, modifier = Modifier.size(34.dp))
        }
        Text("${state.correct} of $total right", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            if (state.missed.isEmpty()) "Every answer was right." else "Words to look at again: ${state.missed.joinToString(", ")}",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text("Practice does not change the status of your words.", style = MaterialTheme.typography.bodySmall, color = colors.outline, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = actions.onRestart, shape = RoundedCornerShape(12.dp)) { Text("Practice again") }
            Button(onClick = actions.onBack, shape = RoundedCornerShape(12.dp)) { Text("Back to reading") }
        }
    }
}

@Composable
private fun Empty(actions: PracticeActions) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp)).padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Nothing to practise on this page", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(
            "Practice uses the words you are learning: give some words on the page a status of 1 to 4 first.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = actions.onBack, shape = RoundedCornerShape(12.dp)) { Text("Back to reading") }
    }
}
