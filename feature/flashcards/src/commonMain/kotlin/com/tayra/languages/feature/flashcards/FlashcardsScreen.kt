package com.tayra.languages.feature.flashcards

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.tayra.languages.core.ui.components.AppIcons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.ui.audio.SpeakButton
import com.tayra.languages.core.ui.audio.Speaker
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

/** Reviewing the flashcards of the language being learned, one card at a time. */
@Composable
fun FlashcardsScreen(onNavigate: (Route) -> Unit, viewModel: FlashcardsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberSpeaker(koinInject(), koinInject(), koinInject<SentenceAudio>())
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is FlashcardsEvent.Speak -> speaker.speak(event.text, event.languageCode)
        }
    }
    // Coming back from editing a term or the settings shows the card as it is now.
    LaunchedEffect(Unit) { viewModel.load() }
    Scaffold(
        topBar = { AppTopBar(title = "Flashcards", onNavigate = onNavigate, section = NavSection.FLASHCARDS) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        FlashcardsContent(
            state = state,
            speaker = speaker,
            actions = FlashcardActions(
                onReveal = viewModel::reveal,
                onAnswer = viewModel::answer,
                onUndo = viewModel::undo,
                onSuspend = viewModel::suspendCard,
                onEdit = { onNavigate(Route.EditTerm(it)) },
                onVocabulary = { onNavigate(Route.Terms()) },
            ),
            modifier = Modifier.padding(padding),
        )
    }
}

internal class FlashcardActions(
    val onReveal: () -> Unit = {},
    val onAnswer: (Rating) -> Unit = {},
    val onUndo: () -> Unit = {},
    val onSuspend: () -> Unit = {},
    val onEdit: (termId: Long) -> Unit = {},
    val onVocabulary: () -> Unit = {},
)

private val WORD_HIGHLIGHT = Color(0xFFFFD166)
private val POS_BACKGROUND = Color(0xFFFFE9A8)
private val POS_TEXT = Color(0xFF7A6A3A)

private val NEW_TINT = Color(0xFF3B6FE0)
private val LEARNING_TINT = Color(0xFFD9622B)
private val REVIEW_TINT = Color(0xFF2E9D57)

private val Rating.tint: Color
    get() = when (this) {
        Rating.AGAIN -> Color(0xFFD64545)
        Rating.HARD -> Color(0xFFD68A00)
        Rating.GOOD -> Color(0xFF2E9D57)
        Rating.EASY -> Color(0xFF3B6FE0)
    }

/** The page under the top bar. [speaker] reads the card aloud; without one there are no listen buttons. */
@Composable
internal fun FlashcardsContent(state: FlashcardsUiState, speaker: Speaker?, actions: FlashcardActions, modifier: Modifier = Modifier) {
    val compact = LocalWindowWidth.current.isCompact
    val focus = remember { FocusRequester() }
    LaunchedEffect(state.card?.termId, state.revealed) { runCatching { focus.requestFocus() } }
    Box(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .focusRequester(focus).focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || state.card == null) return@onPreviewKeyEvent false
                when {
                    (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.Z -> if (state.canUndo) actions.onUndo()
                    event.isCtrlPressed || event.isMetaPressed -> return@onPreviewKeyEvent false
                    !state.revealed && (event.key == Key.Spacebar || event.key == Key.Enter) -> actions.onReveal()
                    !state.revealed -> return@onPreviewKeyEvent false
                    event.key == Key.Spacebar || event.key == Key.Enter || event.key == Key.Three -> actions.onAnswer(Rating.GOOD)
                    event.key == Key.One -> actions.onAnswer(Rating.AGAIN)
                    event.key == Key.Two -> actions.onAnswer(Rating.HARD)
                    event.key == Key.Four -> actions.onAnswer(Rating.EASY)
                    else -> return@onPreviewKeyEvent false
                }
                true
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 1048.dp).fillMaxWidth().padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 22.dp),
        ) {
            Header(state, compact)
            Progress(state, compact)
            val card = state.card
            if (card == null) {
                Finished(state, actions)
            } else {
                Card(card, state.revealed, speaker, compact, actions.onReveal)
                if (state.revealed) Answers(actions, compact)
                CardFooter(card, state.canUndo, actions)
            }
        }
    }
}

@Composable
private fun Header(state: FlashcardsUiState, compact: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Text("Flashcards", style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (state.languageName.isEmpty()) "Review the words you are learning." else "Review the ${state.languageName} words you are learning.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** How far through today's cards the session is, with the new, learning and review cards still waiting, as under Anki's cards. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Progress(state: FlashcardsUiState, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val counts = state.counts
    // Answers given this session plus what still waits; a card answered Again waits again, so the total can grow.
    val total = state.answered + counts.total
    val current = if (state.card != null) (state.answered + 1).coerceAtMost(total.coerceAtLeast(1)) else state.answered
    val pills: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CountPill("New", counts.new, NEW_TINT)
            PillDivider()
            CountPill("Learning", counts.learning, LEARNING_TINT)
            PillDivider()
            CountPill("To review", counts.review, REVIEW_TINT)
        }
    }
    val bar: @Composable (Modifier) -> Unit = { m ->
        Box(m.height(10.dp).clip(RoundedCornerShape(5.dp)).background(colors.onSurface.copy(alpha = 0.08f))) {
            val share = if (total > 0) current.toFloat() / total else 0f
            if (share > 0f) Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).height(10.dp).clip(RoundedCornerShape(5.dp)).background(NEW_TINT))
        }
    }
    val label: @Composable () -> Unit = {
        Text(
            when {
                total == 0 -> "No cards today"
                state.card == null -> "$current of $total answered"
                else -> "Card $current of $total"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            softWrap = false,
            modifier = Modifier.testTag("flashcard-progress"),
        )
    }
    val box = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(14.dp))
    if (compact) {
        Column(box.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                label()
                bar(Modifier.weight(1f))
            }
            pills()
        }
    } else {
        Row(box.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            label()
            bar(Modifier.weight(1f))
            pills()
        }
    }
}

@Composable
private fun PillDivider() {
    Box(Modifier.width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun CountPill(label: String, count: Int, tint: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.1f)).border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp).testTag("count-$label"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("$count", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = tint)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, softWrap = false)
    }
}

/**
 * The card, with the content of the cards exported to Anki. Its front is the sentence the word
 * was read in with the word blanked out, over the sentence's translation; the answer puts the
 * word back and adds the word with its part of speech, translation and romanization. A word
 * saved without a sentence is shown on its own.
 */
@Composable
private fun Card(card: CardContent, revealed: Boolean, speaker: Speaker?, compact: Boolean, onReveal: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val direction = if (card.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
    val sentenceSize = if (compact) 22.sp else 30.sp
    val inset = if (compact) 18.dp else 32.dp
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp))
            .padding(horizontal = inset, vertical = if (compact) 18.dp else 22.dp).testTag("flashcard"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            StateLabel(card.state)
            Spacer(Modifier.weight(1f))
            if (speaker != null) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) { SpeakButton(card.spoken, card.languageCode, speaker, Modifier.size(44.dp)) }
            }
        }
        val highlight = SpanStyle(background = WORD_HIGHLIGHT.copy(alpha = 0.55f), fontWeight = FontWeight.SemiBold)
        val sentence = card.sentence
        val range = card.wordRange
        val front = when {
            sentence == null -> buildAnnotatedString { withStyle(highlight) { append(card.word) } }
            range == null -> buildAnnotatedString {
                // The word is not in the sentence as written: it leads, as on the exported card.
                withStyle(highlight) { append(if (revealed) card.word else BLANK) }
                append("\n")
                append(sentence)
            }
            else -> buildAnnotatedString {
                append(sentence.substring(0, range.first))
                withStyle(highlight) { append(if (revealed) sentence.substring(range) else BLANK) }
                append(sentence.substring(range.last + 1))
            }
        }
        Text(
            front,
            style = TextStyle(fontSize = sentenceSize, lineHeight = sentenceSize * 1.45f, color = colors.onSurface, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, textDirection = direction),
            modifier = Modifier.padding(top = if (compact) 8.dp else 12.dp, bottom = if (compact) 16.dp else 24.dp).testTag("flashcard-front"),
        )
        card.sentenceTranslation?.let { translation ->
            HorizontalDivider(color = colors.outlineVariant)
            Text(
                translation,
                Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.primary.copy(alpha = 0.05f))
                    .padding(horizontal = if (compact) 14.dp else 40.dp, vertical = 16.dp),
                style = TextStyle(fontSize = if (compact) 15.sp else 18.sp, lineHeight = 26.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center),
            )
        }
        if (revealed) {
            HorizontalDivider(Modifier.padding(top = 16.dp), color = colors.outlineVariant)
            Column(
                Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp).testTag("flashcard-answer"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                    if (speaker != null) SpeakButton(card.word, card.languageCode, speaker, Modifier.size(40.dp))
                    Text(card.word, style = TextStyle(fontSize = if (compact) 24.sp else 30.sp, fontWeight = FontWeight.Medium, color = colors.onSurface, textDirection = direction))
                    if (card.partOfSpeech.isNotEmpty()) {
                        Text(
                            card.partOfSpeech.uppercase(),
                            Modifier.clip(RoundedCornerShape(6.dp)).background(POS_BACKGROUND).padding(horizontal = 9.dp, vertical = 3.dp),
                            style = TextStyle(fontSize = 12.sp, letterSpacing = 1.2.sp, color = POS_TEXT, fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
                Text(
                    card.translation.ifEmpty { "No translation saved yet" },
                    style = TextStyle(fontSize = if (compact) 18.sp else 22.sp, color = if (card.translation.isEmpty()) colors.outline else colors.onSurfaceVariant, textAlign = TextAlign.Center),
                )
                if (card.romanization.isNotEmpty()) Text(card.romanization, style = MaterialTheme.typography.bodyLarge, color = colors.outline)
            }
        } else {
            Button(
                onClick = onReveal,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp).widthIn(min = if (compact) 220.dp else 360.dp).height(if (compact) 52.dp else 64.dp),
            ) {
                Text("Show answer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private const val BLANK = "[...]"

@Composable
private fun StateLabel(state: CardState) {
    val (label, tint) = when (state) {
        CardState.NEW -> "NEW" to NEW_TINT
        CardState.LEARNING -> "LEARNING" to LEARNING_TINT
        CardState.RELEARNING -> "RELEARNING" to LEARNING_TINT
        CardState.REVIEW -> "REVIEW" to REVIEW_TINT
    }
    Text(
        label,
        Modifier.clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.1f)).border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        style = TextStyle(fontSize = 13.sp, letterSpacing = 1.2.sp, color = tint, fontWeight = FontWeight.Bold),
    )
}

/** The four answers. */
@Composable
private fun Answers(actions: FlashcardActions, compact: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)) {
        Rating.entries.forEach { rating ->
            val tint = rating.tint
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.1f)).border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = rating.label) { actions.onAnswer(rating) }.padding(vertical = if (compact) 16.dp else 20.dp, horizontal = 6.dp).testTag("answer-${rating.label}"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(rating.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tint)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardFooter(card: CardContent, canUndo: Boolean, actions: FlashcardActions) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), itemVerticalAlignment = Alignment.CenterVertically) {
        FooterLink(Icons.Default.Refresh, "Undo last answer", enabled = canUndo, onClick = actions.onUndo)
        PillDivider()
        FooterLink(Icons.Default.Edit, "Edit term", onClick = { actions.onEdit(card.termId) })
        PillDivider()
        FooterLink(AppIcons.Pause, "Suspend card", onClick = actions.onSuspend)
    }
}

@Composable
private fun FooterLink(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** No card to show: the day's cards are done, a card being learned is still waiting, or there are none yet. */
@Composable
private fun Finished(state: FlashcardsUiState, actions: FlashcardActions) {
    val colors = MaterialTheme.colorScheme
    val waiting = state.nextLearningAt
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(22.dp)).padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(REVIEW_TINT.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = REVIEW_TINT, modifier = Modifier.size(34.dp))
        }
        val title = when {
            waiting != null -> "Nothing to review right now"
            state.answered > 0 -> "All done for today"
            else -> "No cards are due"
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        val detail = when {
            waiting != null -> "A card you are learning comes back at ${clockTime(waiting)}."
            state.answered > 0 -> "You answered ${state.answered} card${if (state.answered == 1) "" else "s"}. Come back tomorrow for more."
            else -> "Words you give a status of 1 to 4 while reading get a card here. More cards are due as their reviews come round."
        }
        Text(detail, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.canUndo) TextButton(onClick = actions.onUndo) { Text("Undo last answer") }
            TextButton(onClick = actions.onVocabulary) { Text("Open vocabulary") }
        }
    }
}

private fun clockTime(instant: Instant): String {
    val time = instant.toLocalDateTime(TimeZone.currentSystemDefault()).time
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}
