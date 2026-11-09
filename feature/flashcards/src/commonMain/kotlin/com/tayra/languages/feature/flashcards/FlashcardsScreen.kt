package com.tayra.languages.feature.flashcards

import kotlinx.coroutines.flow.StateFlow
import com.tayra.languages.core.ui.components.Confetti
import androidx.compose.material.icons.filled.Close
import com.tayra.languages.core.ui.components.FreshViewModels
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.domain.flashcards.FlashcardService
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import com.tayra.languages.core.domain.flashcards.BacklogOrder
import androidx.compose.material.icons.filled.Add
import com.tayra.languages.core.domain.flashcards.ExtraStudy
import com.tayra.languages.core.ui.components.SpeakAnswer
import com.tayra.languages.core.ui.audio.rememberMicrophoneAccess
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.tayra.languages.core.domain.flashcards.AnswerMode
import com.tayra.languages.core.domain.practice.TypedResult
import com.tayra.languages.core.ui.components.AnswerModeToggle
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.SentenceCheck
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
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
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTooltip
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.Appear
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.EdgeScrollbar
import com.tayra.languages.core.ui.components.FitText
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.LearningLanguageSelector
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.MarkedText
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.ProgressTrack
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.animatedCount
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.pushFill
import com.tayra.languages.core.ui.hotkeys.HotkeyMatcher
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
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** The day's flashcards, as the Flashcards page has them, in a dialog over a text, which is not left. */
@Composable
fun FlashcardsDialog(onNavigate: (Route) -> Unit, onClose: () -> Unit) {
    AppDialogSurface(
        onDismissRequest = onClose,
        modifier = Modifier.fillMaxHeight().testTag("flashcards-dialog"),
        maxWidth = FLASHCARDS_DIALOG_WIDTH,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        FreshViewModels { FlashcardsScreen(onNavigate = { onClose(); onNavigate(it) }, onClose = onClose) }
    }
}

/** Reviewing the flashcards of the language being learned, one card at a time; with [onClose], in a dialog. */
@Composable
fun FlashcardsScreen(onNavigate: (Route) -> Unit, viewModel: FlashcardsViewModel = koinViewModel(), onClose: (() -> Unit)? = null) {
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
    val microphone = rememberMicrophoneAccess()
    var confirmDelete by remember { mutableStateOf<CardContent?>(null) }
    var choosingExample by remember { mutableStateOf<CardContent?>(null) }
    // The card's details open over the review, which shows the card as it is now once they close, its answer still showing if it was.
    var details by remember { mutableStateOf<Long?>(null) }
    var studyingMore by remember { mutableStateOf(false) }
    val overlay = choosingExample != null || details != null || confirmDelete != null || studyingMore
    // Nothing moves on by itself while something is open over the card.
    LaunchedEffect(overlay) { viewModel.pauseAuto(overlay) }
    Scaffold(
        topBar = {
            if (onClose != null) {
                Row(Modifier.fillMaxWidth().padding(start = AppSpacing.xl, end = AppSpacing.m, top = AppSpacing.m), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Flashcards"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    AppIconButton(Icons.Default.Close, tr("Close"), onClick = onClose, modifier = Modifier.testTag("close-flashcards-dialog"), style = IconButtonStyle.Tonal)
                }
            } else {
                // Wide windows offer the browser from the page heading instead.
                AppTopBar(title = tr("Flashcards"), onNavigate = onNavigate, section = NavSection.FLASHCARDS, actions = { if (compact) BrowseAction { onNavigate(Route.BrowseFlashcards()) } })
            }
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
            // A lesson's dialog has its own fireworks, once the lesson is done.
            celebrate = onClose == null,
            hotkeys = viewModel.hotkeys,
            actions = FlashcardActions(
                onReveal = viewModel::reveal,
                onAnswer = viewModel::answer,
                onUndo = viewModel::undo,
                onSuspend = viewModel::suspendCard,
                onSkip = viewModel::skipCard,
                onDelete = { confirmDelete = state.card },
                onEdit = { details = it },
                onBooks = { onNavigate(Route.Books) },
                onRead = { onNavigate(Route.Read(it)) },
                onBrowse = { onNavigate(Route.BrowseFlashcards()) },
                onChooseExample = { choosingExample = it },
                onStartWithoutWaiting = viewModel::startWithoutWaiting,
                onType = viewModel::type,
                onCheck = viewModel::check,
                onAnswerMode = viewModel::setAnswerMode,
                onListen = { microphone { allowed -> if (allowed) viewModel.listen() else viewModel.refuseSpeaking() } },
                onStopListening = viewModel::stopListening,
                microphoneLevel = viewModel.microphoneLevel,
                onTypeInstead = viewModel::typeInstead,
                onTryAgain = viewModel::tryAgain,
                onStudyMore = { studyingMore = true },
                onEndPass = viewModel::endPass,
            ),
            modifier = Modifier.padding(padding),
            overlay = overlay,
        )
    }
    if (studyingMore) StudyMoreDialog(viewModel, onAddNew = viewModel::addNewToday, onStart = viewModel::startPass, onDismiss = { studyingMore = false })
    details?.let { termId -> FlashcardDetails(termId, onNavigate, onClose = { details = null; viewModel.reloadCard() }) }
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
    /** Leaves the card until tomorrow, or in an extra pass, moves past it. */
    val onSkip: () -> Unit = {},
    /** Asks before the card is deleted with its word. */
    val onDelete: () -> Unit = {},
    /** Opens the card in the browser, where its sentence, schedule and state are changed. */
    val onEdit: (termId: Long) -> Unit = {},
    val onBooks: () -> Unit = {},
    /** Back to the text with this id, read last before the cards. */
    val onRead: (Long) -> Unit = {},
    /** Every card in a list, to search and manage. */
    val onBrowse: () -> Unit = {},
    /** Offers other example sentences for the card, to replace its own with. */
    val onChooseExample: (CardContent) -> Unit = {},
    /** Starts the session before its speech is all made. */
    val onStartWithoutWaiting: () -> Unit = {},
    val onType: (String) -> Unit = {},
    /** Checks what was typed and shows the answer; with nothing typed, only shows it. */
    val onCheck: () -> Unit = {},
    /** Answers cards another way: shown, or typed or said first. */
    val onAnswerMode: (AnswerMode) -> Unit = {},
    /** Listens to the sentence said aloud, the microphone allowed. */
    val onListen: () -> Unit = {},
    val onStopListening: () -> Unit = {},
    /** How loud the microphone is while the sentence is heard. */
    val microphoneLevel: StateFlow<Float>? = null,
    /** Types the card's word instead of saying it. */
    val onTypeInstead: () -> Unit = {},
    /** Asks a card typed or said wrongly again. */
    val onTryAgain: () -> Unit = {},
    /** Offers cards past the day's limits. */
    val onStudyMore: () -> Unit = {},
    /** Leaves an extra pass for the day's own cards. */
    val onEndPass: () -> Unit = {},
)

private val AnswerMode.shortcut: HotkeyAction
    get() = when (this) {
        AnswerMode.SHOW -> HotkeyAction.FLASHCARDS_MODE_SHOW
        AnswerMode.TYPE -> HotkeyAction.FLASHCARDS_MODE_TYPE
        AnswerMode.SPEAK -> HotkeyAction.FLASHCARDS_MODE_SPEAK
    }

internal val Rating.tint: Color
    get() = when (this) {
        Rating.AGAIN -> RatingColors.again
        Rating.HARD -> RatingColors.hard
        Rating.GOOD -> RatingColors.good
        Rating.EASY -> RatingColors.easy
    }

/**
 * The page under the top bar. [speaker] reads the card aloud; without one there are no listen buttons. [hotkeys] holds the
 * shortcuts back to the previous card and to the card's sound. [overlay] says a dialog over the page has the keys.
 */
@Composable
internal fun FlashcardsContent(
    state: FlashcardsUiState,
    speaker: Speaker?,
    actions: FlashcardActions,
    modifier: Modifier = Modifier,
    hotkeys: Map<HotkeyAction, Hotkey?> = HotkeyAction.defaults,
    overlay: Boolean = false,
    /** Fireworks once the day's cards are all answered. */
    celebrate: Boolean = true,
) {
    val compact = LocalWindowWidth.current.isCompact
    val finished = state.card == null && state.nextLearningAt == null && state.answered > 0 && state.pass == null
    if (celebrate && finished) Confetti()
    val focus = remember { FocusRequester() }
    // The page holds the keys: it takes them with each card and answer, back when the window or the screen returns to the front, and when a dialog over it closes.
    // While the word is typed, its field has them instead.
    LaunchedEffect(state.card?.termId, state.revealed, state.canUndo, overlay, state.typing) { if (!overlay && !state.typing) runCatching { focus.requestFocus() } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (!state.typing) runCatching { focus.requestFocus() } }
    val scroll = rememberScrollState()
    Box(modifier.fillMaxSize()) {
        Box(
            // A phone scrolls the card alone, under buttons that stay put; so does a wide screen while a card is on it.
            Modifier.fillMaxSize()
                .focusRequester(focus).focusable().testTag("flashcards-keys")
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val pressed = HotkeyMatcher.fromEvent(event)
                    // While the word is typed, only a key that types nothing changes the way of answering.
                    if (pressed != null && (!state.typing || pressed.ctrl || pressed.alt)) {
                        state.modes.firstOrNull { hotkeys[it.shortcut] == pressed }?.let { mode ->
                            if (mode != state.answerMode) actions.onAnswerMode(mode)
                            return@onPreviewKeyEvent true
                        }
                    }
                    // The keys typed are the word's, but Enter, which checks it.
                    if (state.typing) {
                        if (event.key != Key.Enter && event.key != Key.NumPadEnter) return@onPreviewKeyEvent false
                        actions.onCheck()
                        return@onPreviewKeyEvent true
                    }
                    if (pressed == null) return@onPreviewKeyEvent false
                    fun isKey(vararg shortcuts: HotkeyAction) = shortcuts.any { hotkeys[it] == pressed }
                    // Every card is a key away, with one on show or none.
                    if (isKey(HotkeyAction.FLASHCARDS_BROWSE)) {
                        actions.onBrowse()
                        return@onPreviewKeyEvent true
                    }
                    // Nothing is answered until the first card's sound is made.
                    if (state.card == null || state.preparingAudio) return@onPreviewKeyEvent false
                    when {
                        isKey(HotkeyAction.FLASHCARDS_PREVIOUS, HotkeyAction.FLASHCARDS_PREVIOUS_ARROW) -> if (state.canUndo) actions.onUndo()
                        isKey(HotkeyAction.FLASHCARDS_LISTEN) && speaker != null -> state.card?.let { speaker.toggle(it.spoken, it.languageCode) }
                        isKey(HotkeyAction.FLASHCARDS_EDIT) -> state.card?.let { actions.onEdit(it.termId) }
                        isKey(HotkeyAction.FLASHCARDS_SUSPEND) -> actions.onSuspend()
                        isKey(HotkeyAction.FLASHCARDS_SKIP) -> actions.onSkip()
                        // Asks first, as from the menu.
                        isKey(HotkeyAction.FLASHCARDS_DELETE) -> actions.onDelete()
                        // Space starts and stops listening while the sentence is to be said; Enter still shows the answer.
                        state.speaking && isKey(HotkeyAction.FLASHCARDS_SHOW_ANSWER) -> if (state.listening) actions.onStopListening() else actions.onListen()
                        isKey(HotkeyAction.FLASHCARDS_SHOW_ANSWER, HotkeyAction.FLASHCARDS_SHOW_ANSWER_ENTER) -> if (state.revealed) actions.onAnswer(state.suggested ?: Rating.GOOD) else actions.onReveal()
                        isKey(HotkeyAction.FLASHCARDS_TRY_AGAIN) && state.canTryAgain -> actions.onTryAgain()
                        !state.revealed -> return@onPreviewKeyEvent false
                        isKey(HotkeyAction.FLASHCARDS_AGAIN) -> actions.onAnswer(Rating.AGAIN)
                        // With two buttons the hidden answers have no keys either, so none is given by accident.
                        isKey(HotkeyAction.FLASHCARDS_HARD) && !state.twoButtons -> actions.onAnswer(Rating.HARD)
                        isKey(HotkeyAction.FLASHCARDS_GOOD) -> actions.onAnswer(Rating.GOOD)
                        isKey(HotkeyAction.FLASHCARDS_EASY) && !state.twoButtons -> actions.onAnswer(Rating.EASY)
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            if (compact) {
                PhoneReview(state, speaker, actions)
            } else {
                val reviewing = state.card != null && !state.preparingAudio
                Column(
                    // The card takes the height between the progress and the answers at the foot; with no card, the page scrolls.
                    Modifier.widthIn(max = REVIEW_WIDTH).fillMaxWidth().then(if (reviewing) Modifier.fillMaxHeight() else Modifier.verticalScroll(scroll))
                        .padding(horizontal = AppSpacing.xxxl, vertical = AppSpacing.xxxl),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xxl),
                ) {
                    ScreenHeader(
                        tr("Flashcards"),
                        subtitle = null,
                    ) {
                        AnswerModeToggle(state.answerMode, actions.onAnswerMode, NoFocus, modes = state.modes)
                        // The phone's bar carries this button instead.
                        if (!compact) SecondaryButton(tr("Browse flashcards"), onClick = actions.onBrowse, modifier = Modifier.testTag("browse-flashcards"), icon = AppIcons.ViewList)
                    }
                    Progress(state, compact, onEndPass = actions.onEndPass)
                    val card = state.card
                    if (state.preparingAudio) {
                        PreparingAudio(actions.onStartWithoutWaiting, compact = false)
                    } else if (card == null) {
                        Finished(state, actions)
                    } else {
                        // Each card fades in as it comes up, filling the height left to it.
                        key(card.termId) { Appear(Modifier.weight(1f), grow = true) { WideCard(card, state.revealed, state.canUndo, state.pass != null, speaker, actions, timer = { AnswerTimer(state) }) { AnswerGiven(state, actions, compact = false) } } }
                        // Show answer and the answers share one place at the foot; the answers fade in as the card turns over.
                        if (state.revealed) {
                            Appear { Answers(actions, compact = false, state.twoButtons, state.waits, state.suggested) }
                        } else {
                            RevealButton(state, actions, Modifier.fillMaxWidth().height(ANSWER_SLOT))
                        }
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
                subtitle = null,
            ) {
                AnswerModeToggle(state.answerMode, actions.onAnswerMode, NoFocus, modes = state.modes, iconsOnly = true)
            }
            Progress(state, compact = true, onEndPass = actions.onEndPass)
            val card = state.card
            if (state.preparingAudio) PreparingAudio(actions.onStartWithoutWaiting, compact = true)
            else if (card == null) Finished(state, actions)
            else key(card.termId) { Appear(grow = true) { Card(card, state.revealed, speaker, compact = true, onChooseExample = { actions.onChooseExample(card) }, timer = { AnswerTimer(state) }) { AnswerGiven(state, actions, compact = true) } } }
        }
        val card = state.card?.takeIf { !state.preparingAudio } ?: return@Column
        Column(Modifier.fillMaxWidth().padding(start = AppSpacing.l, end = AppSpacing.l, bottom = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            // Show answer and the answers share one slot, so the thumb finds them in the same place.
            if (state.revealed) {
                Appear { Answers(actions, compact = true, state.twoButtons, state.waits, state.suggested) }
            } else {
                RevealButton(state, actions, Modifier.fillMaxWidth().heightIn(min = ANSWER_SLOT_COMPACT))
            }
            CardFooter(card, state.canUndo, state.pass != null, actions)
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
private fun Progress(state: FlashcardsUiState, compact: Boolean, onEndPass: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    val counts = state.counts
    val pass = state.pass
    // The day's cards stay one number through the session; an extra pass counts its own cards. A card
    // in its steps still waits, so the way through goes by the answers given against those still to give.
    val total = pass?.termIds?.size ?: state.sessionTotal
    val progress = when {
        total == 0 -> 0f
        pass != null -> pass.done.toFloat() / total
        else -> state.answered.toFloat() / (state.answered + counts.total).coerceAtLeast(1)
    }
    // The card on show is still to do.
    val behind = pass?.done ?: (progress * total).roundToInt().coerceAtMost(if (state.card != null) total - 1 else total).coerceAtLeast(0)
    val current = if (state.card != null) (behind + 1).coerceAtMost(total.coerceAtLeast(1)) else behind
    val pills: @Composable () -> Unit = {
        // Large text may not fit the three in a row; they wrap.
        FlowRow(
            if (compact) Modifier.fillMaxWidth() else Modifier,
            itemVerticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            // Scheduling words, apart from a word's New, Learning and Known: a card seen for the first time, one in its learning steps, one due.
            CountPill("New", counts.new, trPlural(counts.new, "first time", "first time"), AppAccents.blue, compact)
            CountPill("Learning", counts.learning, trPlural(counts.learning, "in steps", "in steps"), AppAccents.orange, compact)
            CountPill("To review", counts.review, trPlural(counts.review, "due", "due"), AppAccents.green, compact)
        }
    }
    val bar: @Composable (Modifier) -> Unit = { m ->
        // Filled by the cards answered: the first card of one shows an empty bar, not a full one.
        ProgressTrack(progress, if (pass != null) AppAccents.purple else AppAccents.blue, m, height = 10.dp, trackColor = colors.onSurface.copy(alpha = AppAlpha.tint))
    }
    val label: @Composable () -> Unit = {
        Text(
            when {
                pass != null -> tr("{0} · card {1} of {2}", pass.study.title(), current, total)
                total == 0 -> tr("No cards today")
                state.card == null -> tr("{0} of {1} answered", current, total)
                else -> tr("{0} of {1}", total - behind, total)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            softWrap = false,
            modifier = Modifier.testTag("flashcard-progress"),
        )
    }
    val box = Modifier.fillMaxWidth().cardSurface(AppShapes.card)
    // Far more due than usual: the order the reviews take instead of the due date, said once under the counts.
    val backlog: @Composable () -> Unit = {
        val order = state.backlog?.takeIf { pass == null && state.card != null }
        if (order != null) {
            Text(
                when (order) {
                    BacklogOrder.RELATIVE_OVERDUENESS -> tr("You're behind: the cards most at risk come first.")
                    BacklogOrder.RECALL_HIGH_FIRST -> tr("You're behind: the cards you most likely remember come first.")
                    BacklogOrder.READING_FIRST -> tr("You're behind: the words ahead in your reading come first.")
                    BacklogOrder.DUE_DATE -> tr("You're behind: the cards due longest come first.")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.testTag("backlog-note"),
            )
        }
    }
    if (compact) {
        Column(box.padding(horizontal = AppSpacing.l, vertical = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                label()
                bar(Modifier.weight(1f))
            }
            if (pass != null) EndPass(onEndPass, Modifier.align(Alignment.CenterHorizontally)) else pills()
            backlog()
            state.leech?.let { LeechNotice(it) }
        }
    } else {
        Row(box.padding(horizontal = AppSpacing.xl, vertical = AppSpacing.l), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxl)) {
            label()
            bar(Modifier.weight(1f))
            if (pass != null) EndPass(onEndPass) else pills()
        }
        if (state.backlog != null && pass == null && state.card != null) Box(Modifier.fillMaxWidth().padding(top = AppSpacing.s), contentAlignment = Alignment.Center) { backlog() }
        state.leech?.let { Box(Modifier.fillMaxWidth().padding(top = AppSpacing.s), contentAlignment = Alignment.Center) { LeechNotice(it) } }
    }
}

/** The card answered last became a leech: said under the counts until the next answer. */
@Composable
private fun LeechNotice(note: LeechNote) {
    Text(
        if (note.suspended) {
            trPlural(note.lapses, "\"{1}\" was forgotten {0} time and is a leech, so it was suspended. Give it a better sentence or a note, then resume it in Browse.", "\"{1}\" was forgotten {0} times and is a leech, so it was suspended. Give it a better sentence or a note, then resume it in Browse.", note.word)
        } else {
            trPlural(note.lapses, "\"{1}\" was forgotten {0} time and is a leech. A better sentence or a note may help it stick.", "\"{1}\" was forgotten {0} times and is a leech. A better sentence or a note may help it stick.", note.word)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("leech-note"),
    )
}

/** The way back from an extra pass to the day's own cards. */
@Composable
private fun EndPass(onEndPass: () -> Unit, modifier: Modifier = Modifier) {
    QuietButton(tr("Back to today's cards"), onClick = onEndPass, modifier = modifier.then(NoFocus).testTag("end-pass"), size = ButtonSize.Compact)
}

/** The name of an extra pass, as its progress shows it. */
@Composable
private fun ExtraStudy.title(): String = when (this) {
    is ExtraStudy.Ahead -> tr("Review ahead")
    is ExtraStudy.Forgotten -> tr("Forgotten cards again")
    is ExtraStudy.Practise -> tr("Practise words")
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
 * examples for it, beside its sound.
 */
@Composable
private fun Card(card: CardContent, answered: Boolean, speaker: Speaker?, compact: Boolean, onChooseExample: () -> Unit = {}, timer: @Composable () -> Unit = {}, given: @Composable () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    val direction = if (card.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
    val inset = if (compact) AppSpacing.xl else AppSpacing.xxxl
    // The answer opens under the sentence in place, the card growing to hold it.
    val revealed = answered
    Column(
        Modifier.fillMaxWidth()
            .animateContentSize(AppMotion.spec(AppMotion.medium))
            .cardSurface(AppShapes.large)
            .padding(horizontal = inset, vertical = if (compact) AppSpacing.xl else AppSpacing.xxl).testTag("flashcard"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = AppSizes.tileLarge), verticalAlignment = Alignment.CenterVertically) {
            // No state tag: the counters above say how many cards are new, learning and due, and a tag named
            // Learning would read as the word's status.
            timer()
            Spacer(Modifier.weight(1f))
            // Two round buttons in the corner, each named by a tooltip: other examples once answered, and the sentence's sound.
            if (revealed && card.sentence != null) {
                val label = tr("Choose another example")
                AppTooltip(label) {
                    AppIconButton(
                        AppIcons.Sync,
                        contentDescription = label,
                        onClick = onChooseExample,
                        modifier = NoFocus.testTag("choose-example"),
                        size = IconButtonSize.Large,
                        style = IconButtonStyle.Outlined,
                        tint = colors.primary,
                        shape = AppShapes.circle,
                    )
                }
                if (speaker != null) Spacer(Modifier.width(AppSpacing.m))
            }
            if (speaker != null) {
                SpeakButton(
                    card.spoken, card.languageCode, speaker, NoFocus,
                    style = IconButtonStyle.Outlined, size = IconButtonSize.Large, shape = AppShapes.circle,
                    tooltip = if (card.sentence != null) tr("Listen to the sentence") else tr("Pronounce"),
                )
            }
        }
        CardText(card, revealed, compact)
        given()
        if (revealed) {
            Column(
                Modifier.fillMaxWidth().padding(top = AppSpacing.xxl, bottom = AppSpacing.xs).testTag("flashcard-answer"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m, Alignment.CenterHorizontally)) {
                    // Ringed as the sentence's button is, so it reads as a button beside the word.
                    if (speaker != null) SpeakButton(card.word, card.languageCode, speaker, NoFocus, style = IconButtonStyle.Outlined, size = IconButtonSize.Large, shape = AppShapes.circle)
                    Text(card.word, style = AppText.cardWord(compact).copy(color = colors.onSurface, textDirection = direction))
                    if (card.partOfSpeech.isNotEmpty()) PartOfSpeech(card.partOfSpeech)
                }
                Text(
                    card.translation.ifEmpty { tr("No translation saved yet") },
                    style = AppText.cardBack(compact).copy(color = if (card.translation.isEmpty()) colors.outline else colors.onSurfaceVariant, textAlign = TextAlign.Center),
                )
                if (card.romanization.isNotEmpty()) Text(card.romanization, style = MaterialTheme.typography.bodyLarge, color = colors.outline)
            }
        }
    }
}

/** The card's sentence with its word blanked out, or put back once answered, over the sentence's translation. */
@Composable
private fun CardText(card: CardContent, revealed: Boolean, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val direction = if (card.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
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
    // A sentence keeps its translation's place from the start, in the same type, so nothing moves when it comes.
    if (sentence != null || card.sentenceTranslation != null) {
        val translation = card.sentenceTranslation
        Text(
            translation ?: if (card.translating) tr("Translating…") else tr("No translation available"),
            Modifier.fillMaxWidth().clip(AppShapes.control).background(colors.primary.copy(alpha = AppAlpha.wash))
                .padding(horizontal = if (compact) AppSpacing.l else AppSpacing.huge, vertical = AppSpacing.l).testTag("flashcard-translation"),
            style = AppText.cardTranslation(compact).copy(color = if (translation != null) colors.onSurfaceVariant else colors.outline, textAlign = TextAlign.Center),
        )
    }
}

/**
 * The card on a wide screen, as tall as the room it is given: its buttons along the top (back to
 * the previous card on the left; the sentence's sound, other examples once answered and the rest
 * on the right) and its content in the middle, scrolling should it not fit. The answer puts the
 * word under the sentence, its sound above it.
 */
@Composable
private fun WideCard(card: CardContent, revealed: Boolean, canUndo: Boolean, inPass: Boolean, speaker: Speaker?, actions: FlashcardActions, modifier: Modifier = Modifier, timer: @Composable () -> Unit = {}, given: @Composable () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    val direction = if (card.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
    Column(modifier.fillMaxSize().cardSurface(AppShapes.large).padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.xl).testTag("flashcard")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            CardButton(Icons.AutoMirrored.Filled.ArrowBack, tr("Previous card"), actions.onUndo, enabled = canUndo, tag = "previous-card")
            timer()
            Spacer(Modifier.weight(1f))
            if (speaker != null) {
                SpeakButton(
                    card.spoken, card.languageCode, speaker, NoFocus,
                    style = IconButtonStyle.Outlined, size = IconButtonSize.Large, shape = AppShapes.circle,
                    tooltip = if (card.sentence != null) tr("Listen to the sentence") else tr("Pronounce"),
                )
            }
            // Other examples show the word, so they are offered once it is shown.
            if (revealed && card.sentence != null) CardButton(AppIcons.Sync, tr("Choose another example"), { actions.onChooseExample(card) }, tag = "choose-example")
            CardMenu(card, inPass, actions)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val scroll = rememberScrollState()
            Column(
                Modifier.fillMaxWidth().verticalScroll(scroll).heightIn(min = maxHeight).padding(horizontal = AppSpacing.l, vertical = AppSpacing.l),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CardText(card, revealed, compact = false)
                given()
                if (revealed) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = AppSpacing.xxl).testTag("flashcard-answer"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
                    ) {
                        if (speaker != null) SpeakButton(card.word, card.languageCode, speaker, NoFocus, style = IconButtonStyle.Outlined, size = IconButtonSize.Large, shape = AppShapes.circle)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                            Text(card.word, style = AppText.cardWord(compact = false).copy(color = colors.onSurface, textDirection = direction))
                            if (card.partOfSpeech.isNotEmpty()) PartOfSpeech(card.partOfSpeech)
                        }
                        Text(
                            card.translation.ifEmpty { tr("No translation saved yet") },
                            style = AppText.cardBack(compact = false).copy(color = if (card.translation.isEmpty()) colors.outline else colors.onSurfaceVariant, textAlign = TextAlign.Center),
                        )
                        if (card.romanization.isNotEmpty()) Text(card.romanization, style = MaterialTheme.typography.bodyLarge, color = colors.outline)
                    }
                }
            }
            // Shown when the card's content is taller than the card, as a long sentence and its answer may be.
            EdgeScrollbar(scroll)
        }
    }
}

/** A round button along the card's top, named by its tooltip. */
@Composable
private fun CardButton(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true, tag: String) {
    AppTooltip(label) {
        AppIconButton(
            icon,
            contentDescription = label,
            onClick = onClick,
            modifier = NoFocus.testTag(tag),
            enabled = enabled,
            size = IconButtonSize.Large,
            style = IconButtonStyle.Outlined,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            shape = AppShapes.circle,
        )
    }
}

/**
 * How long the card has been on show, as Anki's timer shows it, when the settings ask for it: red
 * once it reaches the most an answer is taken to take.
 */
@Composable
private fun AnswerTimer(state: FlashcardsUiState) {
    val shown = state.shownAt?.takeIf { state.showTimer } ?: return
    val most = FlashcardService.MAX_ANSWER_TIME.inWholeSeconds
    var seconds by remember(shown) { mutableStateOf(shown.elapsedNow().inWholeSeconds.coerceAtMost(most)) }
    LaunchedEffect(shown) {
        while (seconds < most) {
            delay(TIMER_TICK)
            seconds = shown.elapsedNow().inWholeSeconds.coerceAtMost(most)
        }
    }
    Text(
        "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
        style = MaterialTheme.typography.titleMedium,
        color = if (seconds >= most) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = AppSpacing.s).testTag("answer-timer"),
    )
}

private val TIMER_TICK = 250.milliseconds

/** The card's other actions behind one round button: its details, and suspending or deleting it. */
@Composable
private fun CardMenu(card: CardContent, inPass: Boolean, actions: FlashcardActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        CardButton(AppIcons.MoreHoriz, tr("More actions"), { open = true }, tag = "card-menu")
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuAction(Icons.Default.Edit, tr("Edit flashcard")) { open = false; actions.onEdit(card.termId) }
            MenuAction(Icons.Default.Add, tr("Study more")) { open = false; actions.onStudyMore() }
            MenuAction(AppIcons.SkipNext, if (inPass) tr("Skip card") else tr("Skip until tomorrow")) { open = false; actions.onSkip() }
            MenuAction(AppIcons.Pause, tr("Suspend card")) { open = false; actions.onSuspend() }
            MenuAction(Icons.Default.Delete, tr("Delete card"), destructive = true) { open = false; actions.onDelete() }
        }
    }
}

@Composable
private fun MenuAction(icon: ImageVector, label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    AppMenuItem(text = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(AppSizes.icon))
            Text(label, color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified)
        }
    }, onClick = onClick)
}

/** The word's part of speech, as a small tag beside it. */
@Composable
private fun PartOfSpeech(pos: String) {
    Text(
        pos.uppercase(),
        Modifier.clip(AppShapes.small).background(AppAccents.highlightSoft).padding(horizontal = AppSpacing.s, vertical = AppSpacing.xs),
        style = AppText.overline.copy(color = AppAccents.onHighlight),
    )
}

/** Show answer, or, while the word is typed, Check; with nothing typed it only shows the answer. */
@Composable
private fun RevealButton(state: FlashcardsUiState, actions: FlashcardActions, modifier: Modifier) {
    val checking = state.cardMode == AnswerMode.TYPE && state.typed.isNotBlank()
    PrimaryButton(onClick = if (state.cardMode == AnswerMode.TYPE) actions.onCheck else actions.onReveal, modifier = modifier.then(NoFocus).testTag("reveal")) {
        Text(if (checking) tr("Check") else tr("Show answer"), style = AppText.itemTitle)
    }
}

/** The word typed for the blank, as it is typed and, once checked, against the word. */
@Composable
private fun AnswerGiven(state: FlashcardsUiState, actions: FlashcardActions, compact: Boolean) {
    val card = state.card ?: return
    val hidden = card.hidden ?: return
    val direction = if (card.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
    // Speaking was asked for, but the word is typed: why.
    if (state.answerMode == AnswerMode.SPEAK && state.typing && !state.typeInstead) {
        Text(
            if (state.speakingRefused) tr("The microphone could not be used, so type the word.") else tr("Speech in this language cannot be recognised here, so type the word."),
            Modifier.padding(top = if (compact) AppSpacing.l else AppSpacing.xl).testTag("flashcard-speak-unavailable"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    when {
        state.speaking -> SpeakAnswer(
            listening = state.listening,
            recognising = state.recognising,
            heard = "",
            retry = state.retry,
            answered = false,
            onListen = actions.onListen,
            onStop = actions.onStopListening,
            onTypeInstead = actions.onTypeInstead,
            tag = "flashcard",
            modifier = Modifier.padding(top = if (compact) AppSpacing.l else AppSpacing.xl),
            rightToLeft = card.rightToLeft,
            buttonModifier = NoFocus,
            level = actions.microphoneLevel,
        )
        state.revealed && state.spokenResult != null -> SpokenVerdict(card.sentence ?: hidden, state.heard, state.spokenResult, compact)
        state.typing -> {
            val focus = remember { FocusRequester() }
            LaunchedEffect(card.termId) { runCatching { focus.requestFocus() } }
            AppTextField(
                value = state.typed,
                onValueChange = actions.onType,
                singleLine = true,
                placeholder = { Text(tr("Type the word"), Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                textStyle = MaterialTheme.typography.titleMedium.copy(textDirection = direction, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onDone = { actions.onCheck() }),
                modifier = Modifier.widthIn(max = ANSWER_FIELD_WIDTH).fillMaxWidth().padding(top = if (compact) AppSpacing.l else AppSpacing.xl)
                    .focusRequester(focus).testTag("flashcard-typed"),
            )
        }
        state.revealed && state.typedResult != null -> TypedVerdict(hidden, state.typed, state.typedResult, compact)
    }
    if (state.canTryAgain) QuietButton(tr("Try again"), onClick = actions.onTryAgain, modifier = NoFocus.padding(top = AppSpacing.s).testTag("flashcard-try-again"))
}

/** How the word typed compared: right, right but for an accent or a slip, or wrong; short of right, what was typed against the word. */
@Composable
private fun TypedVerdict(hidden: String, typed: String, result: TypedResult, compact: Boolean) {
    val (tint, title) = when (result) {
        TypedResult.CORRECT -> AppAccents.green to tr("Correct")
        TypedResult.ALMOST -> AppAccents.amber to tr("Almost: mind the accents")
        TypedResult.TYPO -> AppAccents.amber to tr("Almost: mind the spelling")
        TypedResult.WRONG -> AppAccents.red to tr("Not quite")
    }
    Column(
        Modifier.fillMaxWidth().padding(top = if (compact) AppSpacing.l else AppSpacing.xl).testTag("flashcard-verdict"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Text(title, style = AppText.itemTitle, color = tint)
        if (result != TypedResult.CORRECT) SentenceCheck(hidden, typed, Modifier.testTag("flashcard-typed-check"))
    }
}

/** Whether the hidden word was heard, and the sentence word by word as it was heard. */
@Composable
private fun SpokenVerdict(sentence: String, heard: String, result: TypedResult, compact: Boolean) {
    val right = result != TypedResult.WRONG
    Column(
        Modifier.fillMaxWidth().padding(top = if (compact) AppSpacing.l else AppSpacing.xl).testTag("flashcard-verdict"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        Text(if (right) tr("Correct") else tr("Not quite"), style = AppText.itemTitle, color = if (right) AppAccents.green else AppAccents.red)
        SentenceCheck(sentence, heard, Modifier.testTag("flashcard-heard-check"))
    }
}

private const val BLANK = "[...]"

private val REVIEW_WIDTH = 1048.dp

/** The answer slot at the foot: Show answer and the answers in its place are the same height, so neither moves the other. */
private val ANSWER_SLOT = 76.dp
private val ANSWER_SLOT_COMPACT = 76.dp
private val FINISHED_ICON_TILE = 64.dp
private val ANSWER_FIELD_WIDTH = 420.dp

/**
 * For the page's buttons: a click does not move the keyboard focus onto them, so it stays with
 * the page, whose keys answer the card. A focused button that goes away (Show answer once
 * pressed, Previous card once disabled) would otherwise leave nothing to take the keys.
 */
private val NoFocus = Modifier.focusProperties { canFocus = false }

/** The answers: all four, or Again and Good only. */
@Composable
private fun Answers(actions: FlashcardActions, compact: Boolean, twoButtons: Boolean, waits: Map<Rating, CardWait> = emptyMap(), suggested: Rating? = null) {
    val ratings = if (twoButtons) listOf(Rating.AGAIN, Rating.GOOD) else Rating.entries
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.s else AppSpacing.l)) {
        ratings.forEach { rating ->
            val tint = rating.tint
            val label = tr(rating.label)
            // The answer a typed or said word earned stands out, and Enter gives it.
            val chosen = rating == suggested
            Column(
                Modifier.weight(1f).heightIn(min = if (compact) ANSWER_SLOT_COMPACT else ANSWER_SLOT).clip(AppShapes.panel)
                    .background(tint.copy(alpha = if (chosen) AppAlpha.soft else AppAlpha.tint))
                    .border(if (chosen) AppBorders.selected(tint) else AppBorders.hairline(tint.copy(alpha = AppAlpha.disabled)), AppShapes.panel)
                    .then(if (chosen) Modifier.semantics { selected = true } else Modifier)
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

/** A phone's actions under the card: the short names only, spread over the width. */
@Composable
private fun CardFooter(card: CardContent, canUndo: Boolean, inPass: Boolean, actions: FlashcardActions) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FooterLink(Icons.AutoMirrored.Filled.ArrowBack, tr("Previous"), Modifier.weight(1f), enabled = canUndo, onClick = actions.onUndo)
        FooterLink(Icons.Default.Edit, tr("Edit"), Modifier.weight(1f), onClick = { actions.onEdit(card.termId) })
        MoreActions(inPass, actions, Modifier.weight(1f))
    }
}

/** Suspend and Delete, a step away from the card so neither is pressed by mistake while reviewing. */
@Composable
private fun MoreActions(inPass: Boolean, actions: FlashcardActions, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        FooterLink(AppIcons.MoreHoriz, tr("More"), Modifier.fillMaxWidth(), onClick = { open = true })
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text(tr("Study more")) }, onClick = { open = false; actions.onStudyMore() })
            AppMenuItem(text = { Text(if (inPass) tr("Skip card") else tr("Skip until tomorrow")) }, onClick = { open = false; actions.onSkip() })
            AppMenuItem(text = { Text(tr("Suspend card")) }, onClick = { open = false; actions.onSuspend() })
            AppMenuItem(text = { Text(tr("Delete card"), color = MaterialTheme.colorScheme.error) }, onClick = { open = false; actions.onDelete() })
        }
    }
}

/** One of the phone's actions under the card: the icon over its name, so each name has the whole column. */
@Composable
private fun FooterLink(icon: ImageVector, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    QuietButton(onClick = onClick, enabled = enabled, modifier = modifier.then(NoFocus), size = ButtonSize.Compact) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AppSizes.iconMedium))
            FitText(label, style = MaterialTheme.typography.labelLarge)
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
        } else {
            PrimaryButton(tr("Keep reading"), onClick = actions.onBooks, modifier = NoFocus.padding(top = AppSpacing.s), icon = AppIcons.MenuBook)
        }
        // Past the day's limits: more new cards, the coming days' reviews, the forgotten ones, or practice.
        SecondaryButton(tr("Study more"), onClick = actions.onStudyMore, modifier = NoFocus.testTag("study-more"), icon = Icons.Default.Add)
        if (state.canUndo) QuietButton(tr("Previous card"), onClick = actions.onUndo, modifier = NoFocus)
    }
}

/**
 * The wait before the first card while the session's speech is made, so that no card waits for its
 * sound: how much is ready, and the way to start at once.
 */
@Composable
private fun PreparingAudio(onStart: () -> Unit, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().cardSurface(AppShapes.large).padding(if (compact) AppSpacing.xl else AppSpacing.huge).testTag("preparing-audio"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        Box(Modifier.size(FINISHED_ICON_TILE).clip(AppShapes.circle).background(colors.primary.copy(alpha = AppAlpha.tint)), contentAlignment = Alignment.Center) {
            Icon(AppIcons.Headphones, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.iconXLarge))
        }
        Text(tr("Preparing the audio"), style = AppText.headline, textAlign = TextAlign.Center)
        Text(
            tr("Making the first card's sound. The others are made in the background while you review."),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        LoadingIndicator(Modifier.padding(top = AppSpacing.s).testTag("preparing-audio-progress"))
        SecondaryButton(tr("Start now"), onClick = onStart, modifier = NoFocus.padding(top = AppSpacing.s).testTag("start-without-waiting"))
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

private val FLASHCARDS_DIALOG_WIDTH = 880.dp
