package com.tayra.languages.feature.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.tayra.languages.core.ui.components.LearningLanguageSelector
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.tayra.languages.core.ui.components.ConfirmDialog
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.hotkeys.HotkeyMatcher
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.ui.audio.SpeakButton
import com.tayra.languages.core.ui.audio.Speaker
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ProgressTrack
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.theme.RatingColors
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
            FlashcardsEvent.StopSpeaking -> speaker.stop()
        }
    }
    // Leaving the screen stops the reading too.
    DisposableEffect(speaker) { onDispose { speaker.stop() } }
    // Coming back from editing a term or the settings shows the card as it is now.
    LaunchedEffect(Unit) { viewModel.load() }
    val compact = LocalWindowWidth.current.isCompact
    var confirmDelete by remember { mutableStateOf<CardContent?>(null) }
    Scaffold(
        topBar = {
            // Wide windows offer the browser from the page heading instead.
            AppTopBar(title = tr("Flashcards"), onNavigate = onNavigate, section = NavSection.FLASHCARDS, actions = { if (compact) BrowseAction { onNavigate(Route.BrowseFlashcards()) } })
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        if (state.loading) {
            LoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        FlashcardsContent(
            state = state,
            speaker = speaker,
            hotkeys = viewModel.hotkeys,
            actions = FlashcardActions(
                onReveal = viewModel::reveal,
                onAnswer = viewModel::answer,
                onUndo = viewModel::undo,
                onSuspend = viewModel::suspendCard,
                onDelete = { confirmDelete = state.card },
                onEdit = { onNavigate(Route.BrowseFlashcards(termId = it)) },
                onVocabulary = { onNavigate(Route.Terms()) },
                onBrowse = { onNavigate(Route.BrowseFlashcards()) },
            ),
            modifier = Modifier.padding(padding),
        )
    }
    confirmDelete?.let { card ->
        ConfirmDialog(
            title = tr("Delete the flashcard of \"{0}\"?", card.word),
            text = trPlural(1, "Its word is deleted from your vocabulary too, with the card's review history. This cannot be undone.", "Their words are deleted from your vocabulary too, with the cards' review history. This cannot be undone."),
            confirmLabel = tr("Delete"),
            destructive = true,
            onConfirm = { confirmDelete = null; viewModel.deleteCard() },
            onDismiss = { confirmDelete = null },
        )
    }
}

internal class FlashcardActions(
    val onReveal: () -> Unit = {},
    val onAnswer: (Rating) -> Unit = {},
    /** Back to the previous card, its answer taken back. */
    val onUndo: () -> Unit = {},
    val onSuspend: () -> Unit = {},
    /** Asks before the card is deleted with its word. */
    val onDelete: () -> Unit = {},
    /** Opens the card in the browser, where its sentence, schedule and state are changed. */
    val onEdit: (termId: Long) -> Unit = {},
    val onVocabulary: () -> Unit = {},
    /** Every card in a list, to search and manage. */
    val onBrowse: () -> Unit = {},
)

internal val Rating.tint: Color
    get() = when (this) {
        Rating.AGAIN -> RatingColors.again
        Rating.HARD -> RatingColors.hard
        Rating.GOOD -> RatingColors.good
        Rating.EASY -> RatingColors.easy
    }

/** The page under the top bar. [speaker] reads the card aloud; without one there are no listen buttons. [hotkeys] holds the shortcut back to the previous card. */
@Composable
internal fun FlashcardsContent(state: FlashcardsUiState, speaker: Speaker?, actions: FlashcardActions, modifier: Modifier = Modifier, hotkeys: Map<HotkeyAction, Hotkey?> = HotkeyAction.defaults) {
    val compact = LocalWindowWidth.current.isCompact
    val focus = remember { FocusRequester() }
    // The page holds the keys: it takes them with each card and answer, and back when the window or the screen returns to the front.
    LaunchedEffect(state.card?.termId, state.revealed, state.canUndo) { runCatching { focus.requestFocus() } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { runCatching { focus.requestFocus() } }
    Box(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .focusRequester(focus).focusable().testTag("flashcards-keys")
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || state.card == null) return@onPreviewKeyEvent false
                val pressed = HotkeyMatcher.fromEvent(event)
                when {
                    // The configurable shortcut (Ctrl+Z unless changed), and the left arrow always.
                    pressed != null && pressed == hotkeys[HotkeyAction.FLASHCARDS_PREVIOUS] -> if (state.canUndo) actions.onUndo()
                    event.key == Key.DirectionLeft -> if (state.canUndo) actions.onUndo()
                    event.isCtrlPressed || event.isMetaPressed -> return@onPreviewKeyEvent false
                    !state.revealed && (event.key == Key.Spacebar || event.key == Key.Enter) -> actions.onReveal()
                    !state.revealed -> return@onPreviewKeyEvent false
                    event.key == Key.Spacebar || event.key == Key.Enter || event.key == Key.Three -> actions.onAnswer(Rating.GOOD)
                    event.key == Key.One -> actions.onAnswer(Rating.AGAIN)
                    // With two buttons the hidden answers have no keys either, so none is given by accident.
                    event.key == Key.Two && !state.twoButtons -> actions.onAnswer(Rating.HARD)
                    event.key == Key.Four && !state.twoButtons -> actions.onAnswer(Rating.EASY)
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
            ScreenHeader(
                tr("Flashcards"),
                subtitle = if (state.languageName.isEmpty()) tr("Review the words you are learning.") else tr("Review the {0} words you are learning.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
            ) {
                // The phone's bar carries this button instead.
                if (!compact) SecondaryButton(tr("Browse flashcards"), onClick = actions.onBrowse, modifier = Modifier.testTag("browse-flashcards"), icon = AppIcons.ViewList)
            }
            Progress(state, compact)
            val card = state.card
            if (card == null) {
                Finished(state, actions)
            } else {
                Card(card, state.revealed, speaker, compact, actions.onReveal)
                if (state.revealed) Answers(actions, compact, state.twoButtons)
                CardFooter(card, state.canUndo, actions, compact)
            }
        }
    }
}

/** The phone bar's way to every card. */
@Composable
internal fun BrowseAction(onBrowse: () -> Unit) {
    AppIconButton(AppIcons.ViewList, contentDescription = tr("Browse flashcards"), onClick = onBrowse, modifier = Modifier.testTag("browse-flashcards"), size = IconButtonSize.Large)
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
        Row(
            if (compact) Modifier.fillMaxWidth() else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 10.dp, Alignment.CenterHorizontally),
        ) {
            CountPill("New", counts.new, trPlural(counts.new, "new", "new"), AppAccents.blue, compact)
            PillDivider()
            CountPill("Learning", counts.learning, trPlural(counts.learning, "learning", "learning"), AppAccents.orange, compact)
            PillDivider()
            CountPill("To review", counts.review, trPlural(counts.review, "to review", "to review"), AppAccents.green, compact)
        }
    }
    val bar: @Composable (Modifier) -> Unit = { m ->
        ProgressTrack(if (total > 0) current.toFloat() / total else 0f, AppAccents.blue, m, height = 10.dp, trackColor = colors.onSurface.copy(alpha = AppAlpha.tint))
    }
    val label: @Composable () -> Unit = {
        Text(
            when {
                total == 0 -> tr("No cards today")
                state.card == null -> tr("{0} of {1} answered", current, total)
                else -> tr("Card {0} of {1}", current, total)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            softWrap = false,
            modifier = Modifier.testTag("flashcard-progress"),
        )
    }
    val box = Modifier.fillMaxWidth().cardSurface(AppShapes.card)
    if (compact) {
        Column(box.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
    AppVerticalDivider(Modifier.height(22.dp))
}

@Composable
private fun CountPill(tag: String, count: Int, label: String, tint: Color, large: Boolean = false) {
    StatusPill(label, tint, Modifier.testTag("count-$tag"), value = "$count", size = if (large) TagSize.Large else TagSize.Medium)
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
    val inset = if (compact) 18.dp else 32.dp
    Column(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large)
            .padding(horizontal = inset, vertical = if (compact) 18.dp else 22.dp).testTag("flashcard"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            StateLabel(card.state)
            Spacer(Modifier.weight(1f))
            if (speaker != null) {
                Box(
                    Modifier.size(48.dp).clip(AppShapes.panel).border(AppBorders.hairline(), AppShapes.panel),
                    contentAlignment = Alignment.Center,
                ) { SpeakButton(card.spoken, card.languageCode, speaker, Modifier.size(44.dp).then(NoFocus)) }
            }
        }
        val highlight = SpanStyle(background = AppAccents.highlight.copy(alpha = AppAlpha.half), fontWeight = FontWeight.SemiBold)
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
            style = AppText.cardSentence(if (compact) 26f else 30f).copy(color = colors.onSurface, textAlign = TextAlign.Center, textDirection = direction),
            modifier = Modifier.padding(top = if (compact) 8.dp else 12.dp, bottom = if (compact) 16.dp else 24.dp).testTag("flashcard-front"),
        )
        card.sentenceTranslation?.let { translation ->
            AppDivider()
            Text(
                translation,
                Modifier.padding(top = 14.dp).fillMaxWidth().clip(AppShapes.control).background(colors.primary.copy(alpha = AppAlpha.wash))
                    .padding(horizontal = if (compact) 14.dp else 40.dp, vertical = 16.dp),
                style = AppText.cardTranslation(compact).copy(color = colors.onSurfaceVariant, textAlign = TextAlign.Center),
            )
        }
        if (revealed) {
            AppDivider(Modifier.padding(top = 16.dp))
            Column(
                Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp).testTag("flashcard-answer"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                    if (speaker != null) SpeakButton(card.word, card.languageCode, speaker, Modifier.size(40.dp).then(NoFocus))
                    Text(card.word, style = AppText.cardWord(compact).copy(color = colors.onSurface, textDirection = direction))
                    if (card.partOfSpeech.isNotEmpty()) {
                        Text(
                            card.partOfSpeech.uppercase(),
                            Modifier.clip(AppShapes.small).background(AppAccents.highlightSoft).padding(horizontal = 9.dp, vertical = 3.dp),
                            style = AppText.overline.copy(color = AppAccents.onHighlight),
                        )
                    }
                }
                Text(
                    card.translation.ifEmpty { tr("No translation saved yet") },
                    style = TextStyle(fontSize = if (compact) 18.sp else 22.sp, color = if (card.translation.isEmpty()) colors.outline else colors.onSurfaceVariant, textAlign = TextAlign.Center),
                )
                if (card.romanization.isNotEmpty()) Text(card.romanization, style = MaterialTheme.typography.bodyLarge, color = colors.outline)
            }
        } else {
            PrimaryButton(
                onClick = onReveal,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp).then(if (compact) Modifier.fillMaxWidth(0.85f) else Modifier.widthIn(min = 360.dp)).height(if (compact) 54.dp else 64.dp).then(NoFocus),
            ) {
                Text(tr("Show answer"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private const val BLANK = "[...]"

/**
 * For the page's buttons: a click does not move the keyboard focus onto them, so it stays with
 * the page, whose keys answer the card. A focused button that goes away (Show answer once
 * pressed, Previous card once disabled) would otherwise leave nothing to take the keys.
 */
private val NoFocus = Modifier.focusProperties { canFocus = false }

@Composable
private fun StateLabel(state: CardState) {
    val (label, tint) = when (state) {
        CardState.NEW -> tr("NEW") to AppAccents.blue
        CardState.LEARNING -> tr("LEARNING") to AppAccents.orange
        CardState.RELEARNING -> tr("RELEARNING") to AppAccents.orange
        CardState.REVIEW -> tr("REVIEW") to AppAccents.green
    }
    Text(
        label,
        Modifier.clip(AppShapes.compact).background(tint.copy(alpha = AppAlpha.tint)).border(AppBorders.hairline(tint.copy(alpha = AppAlpha.line)), AppShapes.compact)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        style = AppText.overline.copy(color = tint),
    )
}

/** The answers: all four, or Again and Good only. */
@Composable
private fun Answers(actions: FlashcardActions, compact: Boolean, twoButtons: Boolean) {
    val ratings = if (twoButtons) listOf(Rating.AGAIN, Rating.GOOD) else Rating.entries
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)) {
        ratings.forEach { rating ->
            val tint = rating.tint
            val label = tr(rating.label)
            Column(
                Modifier.weight(1f).clip(AppShapes.panel).background(tint.copy(alpha = AppAlpha.tint)).border(AppBorders.hairline(tint.copy(alpha = AppAlpha.disabled)), AppShapes.panel)
                    .then(NoFocus).clickable(onClickLabel = label) { actions.onAnswer(rating) }.padding(vertical = if (compact) 16.dp else 20.dp, horizontal = 6.dp).testTag("answer-${rating.label}"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tint)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardFooter(card: CardContent, canUndo: Boolean, actions: FlashcardActions, compact: Boolean) {
    if (compact) {
        // A phone has the room for the short names only, spread over the width.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FooterLink(Icons.AutoMirrored.Filled.ArrowBack, tr("Previous"), Modifier.weight(1f), tight = true, enabled = canUndo, onClick = actions.onUndo)
            PillDivider()
            FooterLink(Icons.Default.Edit, tr("Edit"), Modifier.weight(1f), tight = true, onClick = { actions.onEdit(card.termId) })
            PillDivider()
            FooterLink(AppIcons.Pause, tr("Suspend"), Modifier.weight(1f), tight = true, onClick = actions.onSuspend)
            PillDivider()
            FooterLink(Icons.Default.Delete, tr("Delete"), Modifier.weight(1f), tight = true, destructive = true, onClick = actions.onDelete)
        }
        return
    }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), itemVerticalAlignment = Alignment.CenterVertically) {
        FooterLink(Icons.AutoMirrored.Filled.ArrowBack, tr("Previous card"), enabled = canUndo, onClick = actions.onUndo)
        PillDivider()
        FooterLink(Icons.Default.Edit, tr("Edit flashcard"), onClick = { actions.onEdit(card.termId) })
        PillDivider()
        FooterLink(AppIcons.Pause, tr("Suspend card"), onClick = actions.onSuspend)
        PillDivider()
        FooterLink(Icons.Default.Delete, tr("Delete card"), destructive = true, onClick = actions.onDelete)
    }
}

/** One of the actions under the card; [tight] for a phone's row of four. */
@Composable
private fun FooterLink(icon: ImageVector, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, destructive: Boolean = false, tight: Boolean = false, onClick: () -> Unit) {
    QuietButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(NoFocus),
        size = if (tight) ButtonSize.Compact else ButtonSize.Regular,
        tone = if (destructive) ButtonTone.Danger else ButtonTone.Normal,
    ) {
        if (tight) {
            // The icon over its name, so each name has the whole column.
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(AppSizes.iconMedium))
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AppSizes.icon))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** No card to show: the day's cards are done, a card being learned is still waiting, or there are none yet. */
@Composable
private fun Finished(state: FlashcardsUiState, actions: FlashcardActions) {
    val colors = MaterialTheme.colorScheme
    val waiting = state.nextLearningAt
    Column(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).clip(AppShapes.circle).background(AppAccents.green.copy(alpha = AppAlpha.soft)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.green, modifier = Modifier.size(AppSizes.iconXLarge))
        }
        val title = when {
            waiting != null -> tr("Nothing to review right now")
            state.answered > 0 -> tr("All done for today")
            else -> tr("No cards are due")
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        val detail = when {
            waiting != null -> tr("A card you are learning comes back at {0}.", clockTime(waiting))
            state.answered > 0 -> trPlural(state.answered, "You answered {0} card. Come back tomorrow for more.", "You answered {0} cards. Come back tomorrow for more.")
            else -> tr("Words you give a status of 1 to 4 while reading get a card here. More cards are due as their reviews come round.")
        }
        Text(detail, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.canUndo) QuietButton(tr("Previous card"), onClick = actions.onUndo, modifier = NoFocus)
            QuietButton(tr("Open vocabulary"), onClick = actions.onVocabulary, modifier = NoFocus)
        }
    }
}

internal fun clockTime(instant: Instant): String {
    val time = instant.toLocalDateTime(TimeZone.currentSystemDefault()).time
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}
