package com.tayra.languages.feature.flashcards

import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.EdgeScrollbar
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import com.tayra.languages.core.ui.components.grammarTags
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.ui.audio.SpeakButton
import com.tayra.languages.core.ui.audio.Speaker
import com.tayra.languages.core.ui.audio.rememberSpeaker
import com.tayra.languages.core.ui.components.MarkedText
import com.tayra.languages.core.ui.components.pushFill
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.Appear
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.FitText
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.LearningLanguageSelector
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
import com.tayra.languages.core.ui.components.animatedCount
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.largeText
import com.tayra.languages.core.ui.hotkeys.HotkeyMatcher
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.core.ui.theme.RatingColors
import com.tayra.languages.feature.terms.examples.ChooseExampleDialog
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

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
    var choosingExample by remember { mutableStateOf<CardContent?>(null) }
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
                onBooks = { onNavigate(Route.Books) },
                onRead = { onNavigate(Route.Read(it)) },
                onBrowse = { onNavigate(Route.BrowseFlashcards()) },
                onChooseExample = { choosingExample = it },
            ),
            modifier = Modifier.padding(padding),
            overlay = choosingExample != null,
        )
    }
    choosingExample?.let { card ->
        ChooseExampleDialog(
            termId = card.termId,
            languageId = card.languageId,
            word = card.word,
            sentence = card.sentence,
            translation = card.sentenceTranslation,
            onReplace = { sentence, translation -> choosingExample = null; viewModel.replaceSentence(sentence, translation) },
            onDismiss = { choosingExample = null },
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
    val onBooks: () -> Unit = {},
    /** Back to the text with this id, read last before the cards. */
    val onRead: (Long) -> Unit = {},
    /** Every card in a list, to search and manage. */
    val onBrowse: () -> Unit = {},
    /** Offers other example sentences for the card, to replace its own with. */
    val onChooseExample: (CardContent) -> Unit = {},
)

internal val Rating.tint: Color
    get() = when (this) {
        Rating.AGAIN -> RatingColors.again
        Rating.HARD -> RatingColors.hard
        Rating.GOOD -> RatingColors.good
        Rating.EASY -> RatingColors.easy
    }

/**
 * The page under the top bar. [speaker] reads the card aloud; without one there are no listen buttons. [hotkeys] holds the
 * shortcut back to the previous card. [overlay] says a dialog over the page has the keys.
 */
@Composable
internal fun FlashcardsContent(state: FlashcardsUiState, speaker: Speaker?, actions: FlashcardActions, modifier: Modifier = Modifier, hotkeys: Map<HotkeyAction, Hotkey?> = HotkeyAction.defaults, overlay: Boolean = false) {
    val compact = LocalWindowWidth.current.isCompact
    val focus = remember { FocusRequester() }
    // The page holds the keys: it takes them with each card and answer, back when the window or the screen returns to the front, and when a dialog over it closes.
    LaunchedEffect(state.card?.termId, state.revealed, state.canUndo, overlay) { if (!overlay) runCatching { focus.requestFocus() } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { runCatching { focus.requestFocus() } }
    val scroll = rememberScrollState()
    Box(modifier.fillMaxSize()) {
        Box(
            // A phone scrolls the card alone, under buttons that stay put; a wide screen scrolls the page.
            Modifier.fillMaxSize().then(if (compact) Modifier else Modifier.verticalScroll(scroll))
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
            if (compact) {
                PhoneReview(state, speaker, actions)
            } else {
                Column(
                    Modifier.widthIn(max = REVIEW_WIDTH).fillMaxWidth().padding(horizontal = if (compact) AppSpacing.l else AppSpacing.xxxl, vertical = if (compact) AppSpacing.l else AppSpacing.xxxl),
                    verticalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.l else AppSpacing.xxl),
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
                        // Each card fades in as it comes up.
                        key(card.termId) { Appear(grow = true) { Card(card, state.revealed, speaker, compact, actions.onReveal, onChooseExample = { actions.onChooseExample(card) }) } }
                        // The answers fade in as the card turns over.
                        if (state.revealed) Appear { Answers(actions, compact, state.twoButtons, state.waits) }
                        CardFooter(card, state.canUndo, actions, compact)
                    }
                }
            }
        }
        EdgeScrollbar(scroll)
    }
}

/**
 * The review on a phone: the card scrolls, and under it one place the thumb finds each time,
 * holding Show answer and then the answers, above the card's actions.
 */
@Composable
private fun PhoneReview(state: FlashcardsUiState, speaker: Speaker?, actions: FlashcardActions) {
    val scroll = rememberScrollState()
    // The answer opens under the sentence, below the fold on a small phone: the card follows it down
    // as it grows, so the answer is in view before it is rated.
    LaunchedEffect(state.card?.termId, state.revealed) {
        if (state.revealed) snapshotFlow { scroll.maxValue }.collect { scroll.scrollTo(it) }
    }
    Column(Modifier.fillMaxSize()) {
        ScrollColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = scroll,
            contentModifier = Modifier.padding(AppSpacing.l),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            // The phone's bar carries the Browse button.
            ScreenHeader(
                tr("Flashcards"),
                subtitle = if (state.languageName.isEmpty()) tr("Review the words you are learning.") else tr("Review the {0} words you are learning.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
            )
            Progress(state, compact = true)
            val card = state.card
            if (card == null) Finished(state, actions)
            else key(card.termId) { Appear(grow = true) { Card(card, state.revealed, speaker, compact = true, actions.onReveal, revealButton = false, onChooseExample = { actions.onChooseExample(card) }) } }
        }
        val card = state.card ?: return@Column
        Column(Modifier.fillMaxWidth().padding(start = AppSpacing.l, end = AppSpacing.l, bottom = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            // Show answer and the answers share one slot, so the thumb finds them in the same place.
            if (state.revealed) {
                Appear { Answers(actions, compact = true, state.twoButtons, state.waits) }
            } else {
                PrimaryButton(onClick = actions.onReveal, modifier = Modifier.fillMaxWidth().heightIn(min = ANSWER_SLOT_COMPACT).then(NoFocus)) {
                    Text(tr("Show answer"), style = AppText.itemTitle)
                }
            }
            CardFooter(card, state.canUndo, actions, compact = true)
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
        // Large text may not fit the three in a row; they wrap, without the dividers between them.
        val divided = !largeText
        FlowRow(
            if (compact) Modifier.fillMaxWidth() else Modifier,
            itemVerticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.m else AppSpacing.s, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            // Scheduling words, apart from a word's New, Learning and Known: a card seen for the first time, one in its learning steps, one due.
            CountPill("New", counts.new, trPlural(counts.new, "first time", "first time"), AppAccents.blue, compact)
            if (divided) PillDivider()
            CountPill("Learning", counts.learning, trPlural(counts.learning, "in steps", "in steps"), AppAccents.orange, compact)
            if (divided) PillDivider()
            CountPill("To review", counts.review, trPlural(counts.review, "due", "due"), AppAccents.green, compact)
        }
    }
    val bar: @Composable (Modifier) -> Unit = { m ->
        // Filled by the cards answered: the first card of one shows an empty bar, not a full one.
        ProgressTrack(if (total > 0) state.answered.toFloat() / total else 0f, AppAccents.blue, m, height = 10.dp, trackColor = colors.onSurface.copy(alpha = AppAlpha.tint))
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
        Column(box.padding(horizontal = AppSpacing.l, vertical = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                label()
                bar(Modifier.weight(1f))
            }
            pills()
        }
    } else {
        Row(box.padding(horizontal = AppSpacing.xl, vertical = AppSpacing.l), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
            label()
            bar(Modifier.weight(1f))
            pills()
        }
    }
}

@Composable
private fun PillDivider() {
    AppVerticalDivider(Modifier.height(PILL_DIVIDER_HEIGHT))
}

@Composable
private fun CountPill(tag: String, count: Int, label: String, tint: Color, large: Boolean = false) {
    StatusPill(label, tint, Modifier.testTag("count-$tag"), value = "${animatedCount(count)}", size = if (large) TagSize.Large else TagSize.Medium)
}

/**
 * The card, with the content of the cards exported to Anki. Its front is the sentence the word
 * was read in with the word blanked out, over the sentence's translation; the answer puts the
 * word back and adds the word with its part of speech, translation and romanization. A word
 * saved without a sentence is shown on its own. An answered card with a sentence offers other
 * examples for it: beside its sound on a wide screen, under the translation on a phone.
 */
@Composable
private fun Card(card: CardContent, answered: Boolean, speaker: Speaker?, compact: Boolean, onReveal: () -> Unit, revealButton: Boolean = true, onChooseExample: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    val direction = if (card.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
    val inset = if (compact) AppSpacing.xl else AppSpacing.xxxl
    // Showing the answer turns the card over: it turns edge-on, shows its other side and turns back,
    // growing to hold the answer; a new card starts face up, and nothing turns when motion is reduced.
    val reduceMotion = AppMotion.reduced
    var revealed by remember(card) { mutableStateOf(answered) }
    val turn = remember(card) { Animatable(0f) }
    LaunchedEffect(card, answered) {
        if (answered == revealed) return@LaunchedEffect
        if (reduceMotion) {
            revealed = answered
            return@LaunchedEffect
        }
        turn.animateTo(EDGE_ON, tween(AppMotion.short, easing = AppMotion.exit))
        revealed = answered
        turn.snapTo(-EDGE_ON)
        turn.animateTo(0f, tween(AppMotion.medium, easing = AppMotion.enter))
    }
    Column(
        Modifier.fillMaxWidth()
            .graphicsLayer { rotationY = turn.value; cameraDistance = CAMERA_DISTANCE * density }
            .animateContentSize(AppMotion.spec(AppMotion.medium))
            .cardSurface(AppShapes.large)
            .padding(horizontal = inset, vertical = if (compact) AppSpacing.xl else AppSpacing.xxl).testTag("flashcard"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = AppSizes.tileLarge), verticalAlignment = Alignment.CenterVertically) {
            // No state tag: the counters above say how many cards are new, learning and due, and a tag named
            // Learning would read as the word's status.
            Spacer(Modifier.weight(1f))
            if (revealed && !compact && card.sentence != null) {
                SecondaryButton(tr("Choose another example"), onClick = onChooseExample, modifier = NoFocus.testTag("choose-example"), icon = AppIcons.Forum, tint = colors.primary)
                Spacer(Modifier.width(AppSpacing.m))
            }
            if (speaker != null) {
                Box(
                    Modifier.size(AppSizes.tileLarge).clip(AppShapes.panel).border(AppBorders.hairline(), AppShapes.panel),
                    contentAlignment = Alignment.Center,
                ) { SpeakButton(card.spoken, card.languageCode, speaker, Modifier.size(AppSizes.tileMedium).then(NoFocus)) }
            }
        }
        val fill = AppAccents.highlight.copy(alpha = AppAlpha.half)
        val highlight = SpanStyle(fontWeight = AppText.labelStrong.fontWeight)
        fun AnnotatedString.Builder.marked(word: String) {
            pushFill(fill)
            withStyle(highlight) { append(word) }
            pop()
        }
        val sentence = card.sentence
        val range = card.wordRange
        val front = when {
            sentence == null -> buildAnnotatedString { marked(card.word) }
            range == null -> buildAnnotatedString {
                // The word is not in the sentence as written: it leads, as on the exported card.
                marked(if (revealed) card.word else BLANK)
                append("\n")
                append(sentence)
            }
            else -> buildAnnotatedString {
                append(sentence.substring(0, range.first))
                marked(if (revealed) sentence.substring(range) else BLANK)
                append(sentence.substring(range.last + 1))
            }
        }
        MarkedText(
            front,
            style = AppText.cardSentence(if (compact) 26f else 30f).copy(color = colors.onSurface, textAlign = TextAlign.Center, textDirection = direction),
            modifier = Modifier.padding(top = if (compact) AppSpacing.s else AppSpacing.m, bottom = if (compact) AppSpacing.l else AppSpacing.xxl).testTag("flashcard-front"),
        )
        card.sentenceTranslation?.let { translation ->
            AppDivider()
            Text(
                translation,
                Modifier.padding(top = AppSpacing.l).fillMaxWidth().clip(AppShapes.control).background(colors.primary.copy(alpha = AppAlpha.wash))
                    .padding(horizontal = if (compact) AppSpacing.l else AppSpacing.huge, vertical = AppSpacing.l),
                style = AppText.cardTranslation(compact).copy(color = colors.onSurfaceVariant, textAlign = TextAlign.Center),
            )
        }
        if (revealed && compact && sentence != null) {
            SecondaryButton(tr("Choose another example"), onClick = onChooseExample, modifier = Modifier.padding(top = AppSpacing.m).fillMaxWidth().then(NoFocus).testTag("choose-example"), icon = AppIcons.Forum, tint = colors.primary)
        }
        if (revealed) {
            AppDivider(Modifier.padding(top = AppSpacing.l))
            Column(
                Modifier.fillMaxWidth().padding(top = AppSpacing.xl, bottom = AppSpacing.xs).testTag("flashcard-answer"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.CenterHorizontally)) {
                    if (speaker != null) SpeakButton(card.word, card.languageCode, speaker, Modifier.size(AppSizes.tile).then(NoFocus))
                    Text(card.word, style = AppText.cardWord(compact).copy(color = colors.onSurface, textDirection = direction))
                    if (card.partOfSpeech.isNotEmpty()) {
                        Text(
                            card.partOfSpeech.uppercase(),
                            Modifier.clip(AppShapes.small).background(AppAccents.highlightSoft).padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs),
                            style = AppText.overline.copy(color = AppAccents.onHighlight),
                        )
                    }
                }
                Text(
                    card.translation.ifEmpty { tr("No translation saved yet") },
                    style = AppText.cardBack(compact).copy(color = if (card.translation.isEmpty()) colors.outline else colors.onSurfaceVariant, textAlign = TextAlign.Center),
                )
                if (card.romanization.isNotEmpty()) Text(card.romanization, style = MaterialTheme.typography.bodyLarge, color = colors.outline)
                if (card.hints.isNotEmpty()) CardHints(card)
            }
        } else if (revealButton) {
            PrimaryButton(
                onClick = onReveal,
                modifier = Modifier.padding(top = AppSpacing.xl, bottom = AppSpacing.s).then(if (compact) Modifier.fillMaxWidth(0.85f) else Modifier.widthIn(min = SHOW_ANSWER_WIDTH)).height(if (compact) SHOW_ANSWER_HEIGHT_COMPACT else SHOW_ANSWER_HEIGHT).then(NoFocus),
            ) {
                Text(tr("Show answer"), style = AppText.itemTitle)
            }
        }
    }
}

/** Wiktionary's gender, plural and verb forms of the word, "neuter · plural: Häuser", over the credit its licence asks for. */
@Composable
private fun CardHints(card: CardContent) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    Column(Modifier.padding(top = AppSpacing.xs).testTag("flashcard-hints"), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            card.hints.joinToString(" \u00b7 ") { hint -> hint.form?.let { "${grammarTags(hint.tags)}: $it" } ?: grammarTags(hint.tags) },
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            color = colors.onSurfaceVariant,
        )
        Text(
            tr("Wiktionary contributors · CC BY-SA 4.0"),
            Modifier.clip(AppShapes.small).clickable(enabled = card.hintsSource != null) { card.hintsSource?.let(uriHandler::openUri) }.then(NoFocus).padding(AppSpacing.xxs),
            style = MaterialTheme.typography.labelSmall,
            color = colors.outline,
        )
    }
}

private const val BLANK = "[...]"

/** How far a card turns before it shows its other side, in degrees, and how far away it is seen from as it does. */
private const val EDGE_ON = 90f
private const val CAMERA_DISTANCE = 12f
private val REVIEW_WIDTH = 1048.dp
private val PILL_DIVIDER_HEIGHT = 22.dp
private val SHOW_ANSWER_WIDTH = 360.dp
private val SHOW_ANSWER_HEIGHT = 64.dp
private val SHOW_ANSWER_HEIGHT_COMPACT = 54.dp

/** The phone's answer slot: Show answer and the answers under it are the same height, so neither moves the other. */
private val ANSWER_SLOT_COMPACT = 76.dp
private val FINISHED_ICON_TILE = 64.dp

/**
 * For the page's buttons: a click does not move the keyboard focus onto them, so it stays with
 * the page, whose keys answer the card. A focused button that goes away (Show answer once
 * pressed, Previous card once disabled) would otherwise leave nothing to take the keys.
 */
private val NoFocus = Modifier.focusProperties { canFocus = false }

/** The answers: all four, or Again and Good only. */
@Composable
private fun Answers(actions: FlashcardActions, compact: Boolean, twoButtons: Boolean, waits: Map<Rating, CardWait> = emptyMap()) {
    val ratings = if (twoButtons) listOf(Rating.AGAIN, Rating.GOOD) else Rating.entries
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.s else AppSpacing.l)) {
        ratings.forEach { rating ->
            val tint = rating.tint
            val label = tr(rating.label)
            Column(
                Modifier.weight(1f).then(if (compact) Modifier.heightIn(min = ANSWER_SLOT_COMPACT) else Modifier).clip(AppShapes.panel).background(tint.copy(alpha = AppAlpha.tint)).border(AppBorders.hairline(tint.copy(alpha = AppAlpha.disabled)), AppShapes.panel)
                    .then(NoFocus).clickable(onClickLabel = label) { actions.onAnswer(rating) }.padding(vertical = if (compact) AppSpacing.l else AppSpacing.xl, horizontal = AppSpacing.xs).testTag("answer-${rating.label}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(label, style = AppText.itemTitle, color = tint)
                // When the card would come back, so the four answers can be told apart before choosing.
                waits[rating]?.let { Text(it.short(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("wait-${rating.label}")) }
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
            MoreActions(actions, tight = true, Modifier.weight(1f))
        }
        return
    }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.CenterHorizontally), itemVerticalAlignment = Alignment.CenterVertically) {
        FooterLink(Icons.AutoMirrored.Filled.ArrowBack, tr("Previous card"), enabled = canUndo, onClick = actions.onUndo)
        PillDivider()
        FooterLink(Icons.Default.Edit, tr("Edit flashcard"), onClick = { actions.onEdit(card.termId) })
        PillDivider()
        MoreActions(actions, tight = false)
    }
}

/** Suspend and Delete, a step away from the card so neither is pressed by mistake while reviewing. */
@Composable
private fun MoreActions(actions: FlashcardActions, tight: Boolean, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        FooterLink(AppIcons.MoreHoriz, tr("More"), if (tight) Modifier.fillMaxWidth() else Modifier, tight = tight, onClick = { open = true })
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text(tr("Suspend card")) }, onClick = { open = false; actions.onSuspend() })
            AppMenuItem(text = { Text(tr("Delete card"), color = MaterialTheme.colorScheme.error) }, onClick = { open = false; actions.onDelete() })
        }
    }
}

/** One of the actions under the card; [tight] for a phone's row of them. */
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(AppSizes.iconMedium))
                FitText(label, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AppSizes.icon))
            Spacer(Modifier.width(AppSpacing.s))
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
        Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(AppSpacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Box(Modifier.size(FINISHED_ICON_TILE).clip(AppShapes.circle).background(AppAccents.green.copy(alpha = AppAlpha.soft)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.green, modifier = Modifier.size(AppSizes.iconXLarge))
        }
        val title = when {
            waiting != null -> tr("Nothing to review right now")
            state.answered > 0 -> tr("All done for today")
            else -> tr("No cards are due")
        }
        Text(title, style = AppText.headline, textAlign = TextAlign.Center)
        val detail = when {
            waiting != null -> tr("A card you are learning comes back at {0}.", clockTime(waiting))
            state.answered > 0 -> trPlural(state.answered, "You answered {0} card. Come back tomorrow for more.", "You answered {0} cards. Come back tomorrow for more.")
            else -> tr("Words you give a status of 1 to 4 while reading get a card here. More cards are due as their reviews come round.")
        }
        Text(detail, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        // Reading is where new cards come from, so it is the way on: back into the text read last, or to the books.
        val reading = state.reading
        if (reading != null) {
            PrimaryButton(tr("Continue reading"), onClick = { actions.onRead(reading.bookId) }, modifier = NoFocus.padding(top = AppSpacing.s).testTag("continue-reading"), icon = AppIcons.MenuBook)
            Text(reading.title, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            PrimaryButton(tr("Keep reading"), onClick = actions.onBooks, modifier = NoFocus.padding(top = AppSpacing.s), icon = AppIcons.MenuBook)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            if (state.canUndo) QuietButton(tr("Previous card"), onClick = actions.onUndo, modifier = NoFocus)
            QuietButton(tr("Open vocabulary"), onClick = actions.onVocabulary, modifier = NoFocus)
        }
    }
}

internal fun clockTime(instant: Instant): String {
    val time = instant.toLocalDateTime(TimeZone.currentSystemDefault()).time
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

/** A wait as short as a button has room for: "<1m", "10m", "4h", "3d", "2mo", "1.5y". */
internal fun CardWait.short(): String {
    val days = days ?: duration.inWholeDays.toInt().takeIf { duration >= 1.days }
    return when {
        days == null && duration < 1.minutes -> tr("<1m")
        days == null && duration < 1.hours -> tr("{0}m", duration.inWholeMinutes)
        days == null -> tr("{0}h", duration.inWholeHours)
        days < 30 -> tr("{0}d", days)
        days < 365 -> tr("{0}mo", days / 30)
        else -> tr("{0}y", (days / 36.5).roundToInt() / 10.0)
    }
}
