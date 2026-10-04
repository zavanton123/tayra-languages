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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.DueCounts
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.ui.audio.SpeakButton
import com.tayra.languages.core.ui.audio.Speaker
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.HeaderButton
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
                onSettings = { onNavigate(Route.FlashcardSettings) },
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
    val onSettings: () -> Unit = {},
    val onVocabulary: () -> Unit = {},
)

// The card's colours are those of the cards exported to Anki, the same in every theme.
private val CARD_TOP = Color(0xFF24324F)
private val CARD_TEXT = Color(0xFFFFFFFF)
private val CARD_MUTED = Color(0xFFAAB6D0)
private val CARD_WORD = Color(0xFFFFD166)
private val CARD_BOTTOM = Color(0xFFF7F5EF)
private val CARD_BOTTOM_TEXT = Color(0xFF222222)
private val CARD_BOTTOM_MUTED = Color(0xFF444444)
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
            Modifier.widthIn(max = 900.dp).fillMaxWidth().padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header(state, compact, actions)
            val card = state.card
            if (card == null) {
                Finished(state, actions)
            } else {
                Card(card, state.revealed, speaker, compact)
                Answers(state, actions, compact)
                CardFooter(card, state.canUndo, actions)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(state: FlashcardsUiState, compact: Boolean, actions: FlashcardActions) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Flashcards", style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (state.languageName.isEmpty()) "Review the words you are learning." else "Review the ${state.languageName} words you are learning.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Counts(state.counts)
        HeaderButton(if (compact) "" else "Settings", Icons.Default.Settings, onClick = actions.onSettings)
    }
}

/** New, learning and review cards waiting today, as under Anki's cards. */
@Composable
private fun Counts(counts: DueCounts) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CountPill("New", counts.new, NEW_TINT)
        CountPill("Learning", counts.learning, LEARNING_TINT)
        CountPill("To review", counts.review, REVIEW_TINT)
    }
}

@Composable
private fun CountPill(label: String, count: Int, tint: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.1f)).border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp).testTag("count-$label"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("$count", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = tint)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * The card. Its front is the sentence the word was read in with the word blanked out, over the
 * sentence's translation; the answer puts the word back and adds the word with its part of
 * speech, translation and romanization. A word saved without a sentence is shown on its own.
 */
@Composable
private fun Card(card: CardContent, revealed: Boolean, speaker: Speaker?, compact: Boolean) {
    val direction = if (card.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
    val sentenceSize = if (compact) 24.sp else 30.sp
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).testTag("flashcard")) {
        Column(
            Modifier.fillMaxWidth().background(CARD_TOP).heightIn(min = if (compact) 160.dp else 220.dp).padding(horizontal = if (compact) 20.dp else 40.dp, vertical = if (compact) 24.dp else 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            StateLabel(card.state)
            val sentence = card.sentence
            val range = card.wordRange
            val front = when {
                sentence == null -> buildAnnotatedString { withStyle(SpanStyle(color = CARD_WORD, fontWeight = FontWeight.SemiBold)) { append(card.word) } }
                range == null -> buildAnnotatedString {
                    // The word is not in the sentence as written: it leads, as on the exported card.
                    withStyle(SpanStyle(color = CARD_WORD, fontWeight = FontWeight.SemiBold)) { append(if (revealed) card.word else BLANK) }
                    append("\n")
                    append(sentence)
                }
                else -> buildAnnotatedString {
                    append(sentence.substring(0, range.first))
                    withStyle(SpanStyle(color = CARD_WORD, fontWeight = FontWeight.SemiBold)) { append(if (revealed) sentence.substring(range) else BLANK) }
                    append(sentence.substring(range.last + 1))
                }
            }
            Text(
                front,
                style = TextStyle(fontSize = sentenceSize, lineHeight = sentenceSize * 1.35f, color = CARD_TEXT, textAlign = TextAlign.Center, textDirection = direction),
                modifier = Modifier.testTag("flashcard-front"),
            )
            card.sentenceTranslation?.let {
                Text(it, style = TextStyle(fontSize = if (compact) 16.sp else 19.sp, lineHeight = 26.sp, color = CARD_MUTED, textAlign = TextAlign.Center))
            }
        }
        if (revealed) {
            Column(
                Modifier.fillMaxWidth().background(CARD_BOTTOM).padding(horizontal = if (compact) 20.dp else 40.dp, vertical = 22.dp).testTag("flashcard-answer"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
                    if (speaker != null) SpeakButton(card.word, card.languageCode, speaker, Modifier.size(36.dp))
                    Text(card.word, style = TextStyle(fontSize = if (compact) 24.sp else 28.sp, fontWeight = FontWeight.Medium, color = CARD_BOTTOM_TEXT, textDirection = direction))
                    if (card.partOfSpeech.isNotEmpty()) {
                        Text(
                            card.partOfSpeech.uppercase(),
                            Modifier.clip(RoundedCornerShape(6.dp)).background(POS_BACKGROUND).padding(horizontal = 9.dp, vertical = 3.dp),
                            style = TextStyle(fontSize = 12.sp, letterSpacing = 1.2.sp, color = POS_TEXT, fontWeight = FontWeight.Medium),
                        )
                    }
                }
                Text(
                    card.translation.ifEmpty { "No translation saved yet" },
                    style = TextStyle(fontSize = if (compact) 18.sp else 22.sp, color = if (card.translation.isEmpty()) CARD_BOTTOM_MUTED.copy(alpha = 0.6f) else CARD_BOTTOM_MUTED, textAlign = TextAlign.Center),
                )
                if (card.romanization.isNotEmpty()) Text(card.romanization, style = TextStyle(fontSize = 16.sp, color = Color(0xFF8A7F66)))
                if (speaker != null && card.sentence != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SpeakButton(card.sentence, card.languageCode, speaker, Modifier.size(36.dp))
                        Text("Listen to the sentence", style = TextStyle(fontSize = 14.sp, color = CARD_BOTTOM_MUTED))
                    }
                }
            }
        }
    }
}

private const val BLANK = "[...]"

@Composable
private fun StateLabel(state: CardState) {
    val (label, tint) = when (state) {
        CardState.NEW -> "NEW" to Color(0xFF8FB1FF)
        CardState.LEARNING -> "LEARNING" to Color(0xFFFFB38A)
        CardState.RELEARNING -> "RELEARNING" to Color(0xFFFFB38A)
        CardState.REVIEW -> "REVIEW" to Color(0xFF8ADBA6)
    }
    Text(label, style = TextStyle(fontSize = 12.sp, letterSpacing = 1.4.sp, color = tint, fontWeight = FontWeight.SemiBold))
}

/** "Show answer", then the four answers with how long each puts the card away for. */
@Composable
private fun Answers(state: FlashcardsUiState, actions: FlashcardActions, compact: Boolean) {
    if (!state.revealed) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Button(onClick = actions.onReveal, shape = RoundedCornerShape(12.dp), modifier = Modifier.widthIn(min = 240.dp).height(52.dp)) {
                Text("Show answer", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (!compact) KeyHint("Space", onPrimary = true)
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)) {
        Rating.entries.forEach { rating ->
            val tint = rating.tint
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.1f)).border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = rating.label) { actions.onAnswer(rating) }.padding(vertical = 10.dp, horizontal = 6.dp).testTag("answer-${rating.label}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(state.waits[rating].orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(rating.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = tint)
                if (!compact) Text("${rating.value}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun KeyHint(key: String, onPrimary: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Spacer(Modifier.width(12.dp))
    Text(
        key,
        Modifier.clip(RoundedCornerShape(6.dp)).background((if (onPrimary) colors.onPrimary else colors.onSurface).copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelMedium,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardFooter(card: CardContent, canUndo: Boolean, actions: FlashcardActions) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), itemVerticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = actions.onUndo, enabled = canUndo) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Undo last answer")
        }
        TextButton(onClick = { actions.onEdit(card.termId) }) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Edit term")
        }
        TextButton(onClick = actions.onSuspend) { Text("Suspend card") }
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
