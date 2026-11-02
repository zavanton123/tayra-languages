package com.tayra.languages.feature.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDialogSurface
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppScreenDialog
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.ChipSize
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.EscapeStaysOnPage
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.ListPager
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.SelectableChip
import com.tayra.languages.core.ui.components.SelectionBar
import com.tayra.languages.core.ui.components.StatusPill
import com.tayra.languages.core.ui.components.TableHeader
import com.tayra.languages.core.ui.components.TableHeaderCell
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.TintedSurface
import com.tayra.languages.core.ui.components.animatedCount
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.components.formatDate
import com.tayra.languages.core.ui.components.itemMotion
import com.tayra.languages.core.ui.components.relativeTo
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import com.tayra.languages.feature.terms.examples.ChooseExampleDialog
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

/**
 * Every flashcard of the language being learned in one list, as Anki's browser: searched, shown
 * by group, sorted by column, selected for suspending or resetting together, and opened for its
 * schedule and review history.
 */
@Composable
fun BrowseFlashcardsScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, termId: Long? = null, viewModel: BrowseFlashcardsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Escape closes a card's details or the search's focus, never the browser itself.
    EscapeStaysOnPage()
    // The card the browser was opened for is opened once; coming back from editing a term only shows the cards as they are now.
    var arrived by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.load(open = termId.takeUnless { arrived })
        arrived = true
    }
    var confirmReset by remember { mutableStateOf<Collection<Long>?>(null) }
    var confirmDelete by remember { mutableStateOf<Collection<Long>?>(null) }
    val scope = rememberCoroutineScope()
    val ankiPicker = rememberFilePickerLauncher(type = FileKitType.File(listOf("apkg", "colpkg"))) { file ->
        if (file != null) scope.launch { viewModel.importAnki(file.readBytes()) }
    }
    val actions = BrowseActions(
        onOpen = viewModel::open,
        onToggle = viewModel::toggleSelected,
        onEdit = { onNavigate(Route.EditTerm(it)) },
        onSuspend = viewModel::setSuspended,
        onReset = { confirmReset = it.toList() },
        onDelete = { confirmDelete = it.toList() },
        onReview = { onNavigate(Route.Flashcards) },
        onSaveSentence = viewModel::saveSentence,
        onSaveTranslation = viewModel::saveSentenceTranslation,
        onEditing = viewModel::editingText,
        onPickWord = viewModel::chooseHiddenWord,
        onImportAnki = { ankiPicker.launch() },
    )
    val width = LocalWindowWidth.current
    if (width.isCompact) {
        PhoneBrowseFlashcardsScreen(state, viewModel, actions, onNavigate = onNavigate, onBack = onBack)
    } else {
        Scaffold(topBar = { AppTopBar(title = tr("Browse flashcards"), onNavigate = onNavigate, onBack = onBack, section = NavSection.FLASHCARDS) }) { padding ->
            BrowseContent(state, viewModel, actions, Modifier.padding(padding).fillMaxSize())
        }
        state.detail?.let { DetailDialog(it, state.now, actions, onDismiss = viewModel::closeDetail) }
    }
    state.ankiImport?.let { AnkiImportDialog(it, onDismiss = viewModel::closeAnkiImport) }
    CardConfirmations(confirmReset, confirmDelete, viewModel, onResetClosed = { confirmReset = null }, onDeleteClosed = { confirmDelete = null })
}

/**
 * One card's details over another screen, as the review screen opens them: the browser's dialog (a
 * page of its own on a phone) with all it can do, the term's editor opening over it; [onClose] when
 * it is closed, or the card is deleted or not there.
 */
@Composable
fun FlashcardDetails(
    termId: Long,
    onNavigate: (Route) -> Unit,
    onClose: () -> Unit,
    viewModel: BrowseFlashcardsViewModel = koinViewModel(key = "flashcard-details-$termId"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val closed by rememberUpdatedState(onClose)
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(termId) { if (viewModel.openAlone(termId)) opened = true else closed() }
    LaunchedEffect(opened, state.detail == null) { if (opened && state.detail == null) closed() }
    var confirmReset by remember { mutableStateOf<Collection<Long>?>(null) }
    var confirmDelete by remember { mutableStateOf<Collection<Long>?>(null) }
    val actions = BrowseActions(
        onEdit = { onNavigate(Route.EditTerm(it)) },
        onSuspend = viewModel::setSuspended,
        onReset = { confirmReset = it.toList() },
        onDelete = { confirmDelete = it.toList() },
        onSaveSentence = viewModel::saveSentence,
        onSaveTranslation = viewModel::saveSentenceTranslation,
        onEditing = viewModel::editingText,
        onPickWord = viewModel::chooseHiddenWord,
    )
    state.detail?.let { detail ->
        if (LocalWindowWidth.current.isCompact) DetailScreen(detail, state.now, actions, onDismiss = viewModel::closeDetail)
        else DetailDialog(detail, state.now, actions, onDismiss = viewModel::closeDetail)
    }
    CardConfirmations(confirmReset, confirmDelete, viewModel, onResetClosed = { confirmReset = null }, onDeleteClosed = { confirmDelete = null })
}

/** Asks before the cards are made new again, or deleted with their words. */
@Composable
private fun CardConfirmations(reset: Collection<Long>?, delete: Collection<Long>?, viewModel: BrowseFlashcardsViewModel, onResetClosed: () -> Unit, onDeleteClosed: () -> Unit) {
    delete?.let { ids ->
        ConfirmDialog(
            title = trPlural(ids.size, "Delete {0} flashcard?", "Delete {0} flashcards?"),
            text = trPlural(ids.size, "Its word is deleted from your vocabulary too, with the card's review history. This cannot be undone.", "Their words are deleted from your vocabulary too, with the cards' review history. This cannot be undone."),
            confirmLabel = tr("Delete"),
            destructive = true,
            onConfirm = { viewModel.delete(ids); onDeleteClosed() },
            onDismiss = onDeleteClosed,
        )
    }
    reset?.let { ids ->
        ConfirmDialog(
            title = trPlural(ids.size, "Reset {0} flashcard?", "Reset {0} flashcards?"),
            text = tr("The cards become new again and their review schedules are forgotten. The terms' statuses stay as they are."),
            confirmLabel = tr("Reset"),
            destructive = true,
            onConfirm = { viewModel.reset(ids); onResetClosed() },
            onDismiss = onResetClosed,
        )
    }
}

internal class BrowseActions(
    /** Opens the card for a closer look. */
    val onOpen: (termId: Long) -> Unit = {},
    val onToggle: (termId: Long) -> Unit = {},
    val onEdit: (termId: Long) -> Unit = {},
    val onSuspend: (termIds: Collection<Long>, suspended: Boolean) -> Unit = { _, _ -> },
    /** Asks before the cards are made new again. */
    val onReset: (termIds: Collection<Long>) -> Unit = {},
    /** Asks before the cards are deleted with their words. */
    val onDelete: (termIds: Collection<Long>) -> Unit = {},
    val onReview: () -> Unit = {},
    /** Replaces the example sentence of the card's term. */
    val onSaveSentence: (termId: Long, sentence: String, translation: String?) -> Unit = { _, _, _ -> },
    /** Writes the translation of the card's sentence by hand; blank has a new one made. */
    val onSaveTranslation: (termId: Long, translation: String) -> Unit = { _, _ -> },
    /** The card's sentence or its translation is being typed, or no longer is: no translation is made meanwhile. */
    val onEditing: (termId: Long, editing: Boolean) -> Unit = { _, _ -> },
    /** Makes a word of the sentence the one the card hides. */
    val onPickWord: (termId: Long, form: String) -> Unit = { _, _ -> },
    /** Asks for an Anki package to import. */
    val onImportAnki: () -> Unit = {},
)

private val DETAIL_DIALOG_WIDTH = 820.dp
/** Narrower than this, the dialog's schedule and history stack rather than stand side by side. */
private val DETAIL_TWO_COLUMNS = 640.dp

/** The page: heading, search, the groups, the selection's actions and the table. */
@Composable
private fun BrowseContent(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel, actions: BrowseActions, modifier: Modifier) {
    val list = rememberLazyListState()
    // Heading, the figures, toolbar, groups, the selection's actions when there is a selection, and the table header come before the rows.
    val rowsBefore = if (state.selected.isNotEmpty()) 6 else 5
    FocusRow(state, state.pageRows, list, rowsBefore, onShown = viewModel::focusShown)
    // A new page starts at its first card, under the table's header.
    var shownPage by remember { mutableStateOf(state.page) }
    LaunchedEffect(state.page) {
        if (state.page != shownPage && list.firstVisibleItemIndex >= rowsBefore - 1) list.scrollToItem(rowsBefore - 1)
        shownPage = state.page
    }
    ScrollList(modifier, state = list, contentPadding = PaddingValues(horizontal = AppSpacing.xxxl, vertical = AppSpacing.l)) {
        item {
            Box(Modifier.padding(bottom = AppSpacing.xl)) {
                ScreenHeader(
                    tr("Browse flashcards"),
                    subtitle = if (state.languageName.isEmpty()) tr("Every card of the words you are learning, with where each stands.")
                    else tr("Every card of the {0} words you are learning, with where each stands.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                        if (state.canImportAnki) SecondaryButton(tr("Import from Anki"), onClick = actions.onImportAnki, modifier = Modifier.testTag("import-anki"), icon = AppIcons.UploadFile)
                        val due = state.counts.due
                        PrimaryButton(if (due > 0) tr("Review {0} due", due) else tr("Review cards"), onClick = actions.onReview, icon = AppIcons.Style)
                    }
                }
            }
        }
        item { BrowseFigures(state, viewModel::setView) }
        item { Toolbar(state, viewModel) }
        item { ViewChips(state, viewModel::setView, Modifier.fillMaxWidth().padding(bottom = AppSpacing.l)) }
        if (state.selected.isNotEmpty()) item { CardSelectionBar(state, viewModel, actions) }
        when {
            state.loading -> item { Text(tr("Loading..."), Modifier.padding(AppSpacing.l), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            state.rows.isEmpty() -> item { EmptyCards(state) }
            else -> {
                // The header, the rows and the pager make one card, each drawing its part of the outline.
                item { CardTableHeader(state, viewModel, Modifier.tableFrame(TableSlice.TOP)) }
                itemsIndexed(state.pageRows, key = { _, row -> row.termId }) { index, row -> Column(itemMotion().tableFrame(TableSlice.MIDDLE)) {
                    CardTableRow(row, state, index, actions)
                }}
                item {
                    Box(Modifier.fillMaxWidth().tableFrame(TableSlice.BOTTOM).padding(start = AppSpacing.l, end = AppSpacing.l, bottom = AppSpacing.m).heightIn(min = AppSpacing.s)) {
                        ListPager(state.page, state.pageSize, state.rows.size, viewModel::setPageSize, viewModel::goToPage)
                    }
                }
            }
        }
    }
}

/** Scrolls the list to the card asked for, a little above it so the rows before it show too. */
@Composable
internal fun FocusRow(state: BrowseUiState, rows: List<BrowseRow>, list: LazyListState, rowsBefore: Int, onShown: () -> Unit) {
    val focus = state.focus
    LaunchedEffect(focus, rows) {
        if (focus == null) return@LaunchedEffect
        val index = rows.indexOfFirst { it.termId == focus }
        if (index >= 0) list.animateScrollToItem((rowsBefore + index - 2).coerceAtLeast(0))
        onShown()
    }
}

/** The three figures that matter most, each a way to its group: every card, those due today, those added today. */
@Composable
private fun BrowseFigures(state: BrowseUiState, onSelect: (BrowseView) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = AppSpacing.xl), horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        val figure = Modifier.weight(1f)
        Figure(BrowseView.ALL, tr("Total cards"), AppIcons.Style, AppAccents.blue, state, onSelect, figure)
        Figure(BrowseView.DUE, tr("Due today"), AppIcons.Schedule, AppAccents.green, state, onSelect, figure)
        Figure(BrowseView.ADDED_TODAY, tr("Added today"), Icons.Default.Add, AppAccents.purple, state, onSelect, figure)
    }
}

/** One figure: its number over its name, ringed while its group is the one shown. */
@Composable
private fun Figure(view: BrowseView, label: String, icon: ImageVector, tint: Color, state: BrowseUiState, onSelect: (BrowseView) -> Unit, modifier: Modifier) {
    val shown = state.view == view
    TintedSurface(
        tint,
        modifier.clip(AppShapes.card).then(if (shown) Modifier.border(AppBorders.selected(tint), AppShapes.card) else Modifier)
            .clickable(onClickLabel = label) { onSelect(view) }.testTag("browse-figure-${view.name}"),
        shape = AppShapes.card,
        padding = PaddingValues(horizontal = AppSpacing.xl, vertical = AppSpacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
            IconTile(icon, size = 48, tint = tint)
            Column {
                Text("${animatedCount(state.counts[view])}", style = AppText.headline)
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The search and the sort on a card of their own, with how many cards there are or are chosen. */
@Composable
private fun Toolbar(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = AppSpacing.l).cardSurface(AppShapes.card).padding(AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        CardSearchField(state.search, viewModel::setSearch, Modifier.weight(1f))
        SelectMenu(
            options = BrowseSortField.entries,
            selected = state.sort.field,
            onSelect = viewModel::sortBy,
            optionLabel = { it.label },
            value = tr("Sort: {0}", state.sort.field.label),
            icon = if (state.sort.ascending) AppIcons.ArrowUp else AppIcons.ArrowDown,
            buttonModifier = Modifier.testTag("browse-sort"),
        )
        Text(
            if (state.selected.isEmpty()) trPlural(state.rows.size, "{0} card", "{0} cards") else tr("{0} of {1} selected", state.selected.size, state.rows.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun CardSearchField(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    SearchField(value, onChange, tr("Search words, translations or sentences"), modifier, fieldModifier = Modifier.testTag("browse-search"))
}

/** The group's name, as Anki's sidebar lists them. */
internal val BrowseView.label: String
    get() = when (this) {
        BrowseView.ALL -> tr("All cards")
        BrowseView.DUE -> tr("Due today")
        BrowseView.NEW -> tr("First time")
        BrowseView.LEARNING -> tr("In steps")
        BrowseView.REVIEW -> tr("Scheduled")
        BrowseView.FORGOTTEN -> tr("Forgotten before")
        BrowseView.SUSPENDED -> tr("Suspended")
        BrowseView.ADDED_TODAY -> tr("Added today")
    }

/** The groups of cards to show, each with how many it holds. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ViewChips(state: BrowseUiState, onSelect: (BrowseView) -> Unit, modifier: Modifier = Modifier, large: Boolean = false, short: Boolean = false) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        // Added today is opened from its figure; the chips are Anki's groups.
        BrowseView.entries.filter { it != BrowseView.ADDED_TODAY }.forEach { view -> ViewChip(view, state.counts[view], selected = view == state.view, large, short) { onSelect(view) } }
    }
}

@Composable
private fun ViewChip(view: BrowseView, count: Int, selected: Boolean, large: Boolean, short: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = when (view) {
        BrowseView.NEW -> AppAccents.blue
        BrowseView.LEARNING -> AppAccents.orange
        BrowseView.REVIEW -> AppAccents.green
        BrowseView.DUE -> AppAccents.orange
        BrowseView.FORGOTTEN -> AppAccents.red
        BrowseView.SUSPENDED -> AppAccents.gray
        BrowseView.ADDED_TODAY -> AppAccents.purple
        BrowseView.ALL -> colors.primary
    }
    // A phone's row is narrow: the first two groups go by one word.
    val label = when {
        short && view == BrowseView.ALL -> tr("All")
        short && view == BrowseView.DUE -> tr("Due")
        else -> view.label
    }
    SelectableChip(label, selected, onClick, Modifier.testTag("browse-view-${view.name}"), tint = tint, count = count, size = if (large) ChipSize.Large else ChipSize.Small)
}

/** What can be done with the selected cards together. */
@Composable
internal fun CardSelectionBar(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel, actions: BrowseActions) {
    val chosen = state.rows.filter { it.termId in state.selected }
    val anyActive = chosen.any { !it.suspended }
    val anySuspended = chosen.any { it.suspended }
    SelectionBar(tr("{0} selected", state.selected.size), Modifier.padding(bottom = AppSpacing.l).testTag("selection-bar")) {
        if (anyActive) QuietButton(tr("Suspend"), onClick = { actions.onSuspend(state.selected, true); viewModel.clearSelection() })
        if (anySuspended) QuietButton(tr("Resume"), onClick = { actions.onSuspend(state.selected, false); viewModel.clearSelection() })
        QuietButton(tr("Reset progress"), onClick = { actions.onReset(state.selected); viewModel.clearSelection() }, tone = ButtonTone.Danger)
        QuietButton(tr("Delete"), onClick = { actions.onDelete(state.selected) }, modifier = Modifier.testTag("delete-selected"), tone = ButtonTone.Danger)
        QuietButton(tr("Clear selection"), onClick = viewModel::clearSelection)
    }
}

@Composable
internal fun EmptyCards(state: BrowseUiState) {
    EmptyState(
        AppIcons.Style,
        title = if (state.filtered) tr("No cards match.") else tr("No flashcards yet"),
        message = if (state.filtered) null else tr("Words you give a status of 1 to 4 while reading get a card here."),
    )
}

// Column weights and widths shared by the table header and rows.
private const val CARD_WEIGHT = 2.2f
private const val SENTENCE_WEIGHT = 3f
private val STATE_WIDTH = 130.dp
private val DUE_WIDTH = 120.dp
private val INTERVAL_WIDTH = 90.dp
private val COUNT_WIDTH = 80.dp
private val LAST_WIDTH = 120.dp
private val MENU_WIDTH = 48.dp

@Composable
private fun CardTableHeader(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel, modifier: Modifier) {
    val allSelected = state.rows.isNotEmpty() && state.rows.all { it.termId in state.selected }
    TableHeader(modifier) {
        AppCheckbox(checked = allSelected, onCheckedChange = viewModel::selectAllShown, modifier = Modifier.testTag("browse-select-all"))
        SortHeader(tr("Card"), BrowseSortField.WORD, state.sort, Modifier.weight(CARD_WEIGHT), viewModel::sortBy)
        TableHeaderCell(tr("Sentence"), Modifier.weight(SENTENCE_WEIGHT))
        SortHeader(tr("Created"), BrowseSortField.CREATED, state.sort, Modifier.width(LAST_WIDTH), viewModel::sortBy)
        SortHeader(tr("State"), BrowseSortField.STATE, state.sort, Modifier.width(STATE_WIDTH), viewModel::sortBy)
        SortHeader(tr("Due"), BrowseSortField.DUE, state.sort, Modifier.width(DUE_WIDTH), viewModel::sortBy)
        SortHeader(tr("Interval"), BrowseSortField.INTERVAL, state.sort, Modifier.width(INTERVAL_WIDTH), viewModel::sortBy)
        SortHeader(tr("Reviews"), BrowseSortField.REVIEWS, state.sort, Modifier.width(COUNT_WIDTH), viewModel::sortBy)
        SortHeader(tr("Lapses"), BrowseSortField.LAPSES, state.sort, Modifier.width(COUNT_WIDTH), viewModel::sortBy)
        SortHeader(tr("Last review"), BrowseSortField.LAST_REVIEW, state.sort, Modifier.width(LAST_WIDTH), viewModel::sortBy)
        Spacer(Modifier.width(MENU_WIDTH))
    }
}

@Composable
private fun SortHeader(label: String, field: BrowseSortField, sort: BrowseSort, modifier: Modifier, onSort: (BrowseSortField) -> Unit) {
    val active = sort.field == field
    TableHeaderCell(label, modifier, icon = if (!active) AppIcons.UnfoldMore else if (sort.ascending) AppIcons.ArrowUp else AppIcons.ArrowDown, active = active) { onSort(field) }
}

@Composable
private fun CardTableRow(row: BrowseRow, state: BrowseUiState, index: Int, actions: BrowseActions) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val selected = row.termId in state.selected
    val open = state.detail?.row?.termId == row.termId
    val background = when {
        open -> colors.primary.copy(alpha = AppAlpha.soft)
        selected || hovered -> colors.primary.copy(alpha = AppAlpha.tint)
        index % 2 == 1 -> colors.surfaceVariant.copy(alpha = AppAlpha.line)
        else -> Color.Transparent
    }
    val dim = if (row.suspended) colors.outline else colors.onSurfaceVariant
    val accent = colors.primary
    Row(
        Modifier.fillMaxWidth().background(background)
            // The open card's row is marked down its leading edge as well as by its tint.
            .then(if (open) Modifier.drawBehind { drawRect(accent, size = Size(OPEN_MARK.toPx(), size.height)) } else Modifier)
            .hoverable(interaction).clickable { actions.onOpen(row.termId) }.padding(horizontal = AppSpacing.s, vertical = AppSpacing.xxs).testTag("browse-row-${row.termId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppCheckbox(checked = selected, onCheckedChange = { actions.onToggle(row.termId) })
        Column(Modifier.weight(CARD_WEIGHT).padding(end = AppSpacing.m)) {
            Text(
                row.word,
                style = AppText.listWord(16, FontWeight.Medium).copy(color = if (row.suspended) colors.outline else colors.primary, textDirection = if (row.rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.translation.isNotEmpty()) Text(row.translation, style = MaterialTheme.typography.bodySmall, color = dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(row.sentence.orEmpty(), Modifier.weight(SENTENCE_WEIGHT).padding(end = AppSpacing.m), style = MaterialTheme.typography.bodyMedium, color = dim, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(row.created?.relativeTo(state.now) ?: NONE, Modifier.width(LAST_WIDTH), style = MaterialTheme.typography.bodyMedium, color = dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.width(STATE_WIDTH)) { StatePill(row) }
        Text(dueLabel(row, state.now), Modifier.width(DUE_WIDTH), style = MaterialTheme.typography.bodyMedium, color = if (row.isDue && !row.suspended) AppAccents.orange else dim, fontWeight = if (row.isDue) FontWeight.Medium else FontWeight.Normal)
        Text(intervalLabel(row), Modifier.width(INTERVAL_WIDTH), style = MaterialTheme.typography.bodyMedium, color = dim)
        Text("${row.reps}", Modifier.width(COUNT_WIDTH), style = MaterialTheme.typography.bodyMedium, color = dim)
        Text("${row.lapses}", Modifier.width(COUNT_WIDTH), style = MaterialTheme.typography.bodyMedium, color = if (row.lapses > 0) AppAccents.red else dim)
        Text(row.lastReview?.relativeTo(state.now) ?: NONE, Modifier.width(LAST_WIDTH), style = MaterialTheme.typography.bodyMedium, color = dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { RowMenu(row, actions) }
    }
}

private val OPEN_MARK = 3.dp

/** Which part of the table's card an item is: its top with the header, a row between, or its foot with the pager. */
private enum class TableSlice { TOP, MIDDLE, BOTTOM }

/** The table card's corners, as the header's own. */
private val TABLE_CORNER = 10.dp

/** Draws [slice]'s part of the table card's outline over the item: down both sides, and round the top or the foot. */
@Composable
private fun Modifier.tableFrame(slice: TableSlice): Modifier {
    val border = AppBorders.hairline()
    return drawWithContent {
        drawContent()
        val stroke = border.width.toPx()
        val left = stroke / 2
        val right = size.width - stroke / 2
        val r = TABLE_CORNER.toPx()
        val path = Path().apply {
            when (slice) {
                TableSlice.TOP -> {
                    val top = stroke / 2
                    moveTo(left, size.height); lineTo(left, top + r)
                    arcTo(Rect(left, top, left + 2 * r, top + 2 * r), 180f, 90f, false)
                    lineTo(right - r, top)
                    arcTo(Rect(right - 2 * r, top, right, top + 2 * r), 270f, 90f, false)
                    lineTo(right, size.height)
                }
                TableSlice.MIDDLE -> { moveTo(left, 0f); lineTo(left, size.height); moveTo(right, 0f); lineTo(right, size.height) }
                TableSlice.BOTTOM -> {
                    val bottom = size.height - stroke / 2
                    moveTo(left, 0f); lineTo(left, bottom - r)
                    arcTo(Rect(left, bottom - 2 * r, left + 2 * r, bottom), 180f, -90f, false)
                    lineTo(right - r, bottom)
                    arcTo(Rect(right - 2 * r, bottom - 2 * r, right, bottom), 90f, -90f, false)
                    lineTo(right, 0f)
                }
            }
        }
        drawPath(path, border.brush, style = Stroke(stroke))
    }
}

/** Shown where a card has no value yet: a new card's interval, or a last review before the first. */
internal const val NONE = "—"

@Composable
internal fun RowMenu(row: BrowseRow, actions: BrowseActions) {
    OverflowMenu(tr("Card actions")) { close ->
        AppMenuItem(text = { Text(tr("Card info")) }, onClick = { close(); actions.onOpen(row.termId) })
        AppMenuItem(text = { Text(tr("Edit term")) }, onClick = { close(); actions.onEdit(row.termId) })
        AppMenuItem(text = { Text(if (row.suspended) tr("Resume") else tr("Suspend")) }, onClick = { close(); actions.onSuspend(listOf(row.termId), !row.suspended) })
        AppMenuItem(text = { Text(tr("Reset progress")) }, enabled = row.state != CardState.NEW, onClick = { close(); actions.onReset(listOf(row.termId)) })
        AppMenuItem(text = { Text(tr("Delete"), color = MaterialTheme.colorScheme.error) }, onClick = { close(); actions.onDelete(listOf(row.termId)) })
    }
}

/** The card's state on its colour, in the scheduling words rather than a word's status; a suspended card is grey whatever its state. */
@Composable
internal fun StatePill(row: BrowseRow, large: Boolean = false) {
    val (label, tint) = when {
        row.suspended -> tr("Suspended") to AppAccents.gray
        else -> stateName(row.state) to stateTint(row.state)
    }
    StatusPill(label, tint, dot = false, size = if (large) TagSize.Medium else TagSize.Small, shape = AppShapes.compact)
}

internal fun stateTint(state: CardState): Color = when (state) {
    CardState.NEW -> AppAccents.blue
    CardState.LEARNING, CardState.RELEARNING -> AppAccents.orange
    CardState.REVIEW -> AppAccents.green
}

internal fun stateName(state: CardState): String = when (state) {
    CardState.NEW -> tr("First time")
    CardState.LEARNING -> tr("In steps")
    CardState.RELEARNING -> tr("Relearning")
    CardState.REVIEW -> tr("Scheduled")
}

/** When the card comes: now, a time today, tomorrow, in some days, or on a date; a new card has no time yet. */
internal fun dueLabel(row: BrowseRow, now: Instant): String {
    if (row.state == CardState.NEW) return tr("Not shown yet")
    if (row.due <= now) return tr("Now")
    val zone = TimeZone.currentSystemDefault()
    val date = row.due.toLocalDateTime(zone).date
    val today = now.toLocalDateTime(zone).date
    return when (date) {
        today -> tr("Today {0}", clockTime(row.due))
        today.plus(1, DateTimeUnit.DAY) -> tr("Tomorrow")
        else -> {
            val days = today.daysUntil(date)
            if (days < 60) trPlural(days, "In {0} day", "In {0} days") else row.due.formatDate(zone)
        }
    }
}

internal fun intervalLabel(row: BrowseRow): String = if (row.intervalDays > 0) trPlural(row.intervalDays, "{0} day", "{0} days") else NONE

/** The opened card on a wide screen: a dialog with its schedule beside its review history, and its reset and delete at the foot. */
@Composable
private fun DetailDialog(detail: BrowseDetail, now: Instant, actions: BrowseActions, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val row = detail.row
    AppDialogSurface(onDismissRequest = onDismiss, modifier = Modifier.testTag("browse-detail"), maxWidth = DETAIL_DIALOG_WIDTH) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = AppSpacing.xxl, end = AppSpacing.l, top = AppSpacing.l, bottom = AppSpacing.l), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Flashcard details"), Modifier.weight(1f), style = AppText.sectionTitle)
                AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss, style = IconButtonStyle.Tonal, tint = colors.onSurface)
            }
            AppDivider()
            ScrollColumn(Modifier.weight(1f, fill = false), contentModifier = Modifier.padding(horizontal = AppSpacing.xxl, vertical = AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                CardHeading(row)
                SentenceBox(row, detail.wordRange, detail.translating, compact = false, actions)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth >= DETAIL_TWO_COLUMNS) {
                        // The rule between the halves runs down the middle as tall as the taller one; asking the schedule's
                        // grid for its height ahead (intrinsics) leaves out its last line.
                        val rule = MaterialTheme.colorScheme.outlineVariant
                        Row(Modifier.fillMaxWidth().padding(top = AppSpacing.s).drawBehind {
                            val x = size.width / 2
                            drawLine(rule, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                        }) {
                            Column(Modifier.weight(1f).padding(end = AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) { CardSchedule(row, now, actions, withButtons = false) }
                            Column(Modifier.weight(1f).padding(start = AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                                Text(tr("Review history"), style = AppText.itemTitle)
                                ReviewHistory(detail, compact = false, Modifier.border(AppBorders.hairline(), AppShapes.card).padding(horizontal = AppSpacing.l, vertical = AppSpacing.xs))
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                            CardSchedule(row, now, actions, withButtons = false)
                            AppDivider(Modifier.padding(vertical = AppSpacing.s))
                            Text(tr("Review history"), style = AppText.itemTitle)
                            ReviewHistory(detail, compact = false)
                        }
                    }
                }
            }
            AppDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xl, vertical = AppSpacing.l), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                if (row.state != CardState.NEW) QuietButton(tr("Reset progress"), onClick = { actions.onReset(listOf(row.termId)) }, tone = ButtonTone.Danger)
                QuietButton(tr("Delete flashcard"), onClick = { actions.onDelete(listOf(row.termId)) }, modifier = Modifier.testTag("detail-delete"), tone = ButtonTone.Danger)
                Spacer(Modifier.weight(1f))
                // What changes the card itself sits by Done; what cannot be undone stays apart on the left.
                CardButtons(row, actions)
                PrimaryButton(tr("Done"), onClick = onDismiss, modifier = Modifier.testTag("detail-done"))
            }
        }
    }
}

/** The opened card on a phone: a page of its own over the list, its back arrow returning to the list, with reset and delete set apart at the end. */
@Composable
internal fun DetailScreen(detail: BrowseDetail, now: Instant, actions: BrowseActions, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val row = detail.row
    AppScreenDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("browse-detail")) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = AppSpacing.xs, end = AppSpacing.l, top = AppSpacing.xs, bottom = AppSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), onClick = onDismiss, modifier = Modifier.testTag("detail-back"))
                Text(tr("Flashcard details"), Modifier.weight(1f).padding(start = AppSpacing.xs), style = AppText.headline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AppDivider()
            ScrollColumn(Modifier.weight(1f), contentModifier = Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                CardHeading(row)
                SentenceBox(row, detail.wordRange, detail.translating, compact = true, actions)
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) { CardSchedule(row, now, actions) }
                AppDivider()
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Text(tr("Review history"), style = AppText.itemTitle)
                    ReviewHistory(detail, compact = true)
                }
                // What cannot be undone is kept apart from the rest, at the end.
                Column(
                    Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.error.copy(alpha = AppAlpha.wash)).padding(AppSpacing.l),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
                ) {
                    Text(tr("Danger zone"), style = AppText.itemTitle)
                    if (row.state != CardState.NEW) SecondaryButton(tr("Reset progress"), onClick = { actions.onReset(listOf(row.termId)) }, modifier = Modifier.fillMaxWidth(), icon = AppIcons.Replay, tone = ButtonTone.Danger)
                    SecondaryButton(tr("Delete flashcard"), onClick = { actions.onDelete(listOf(row.termId)) }, modifier = Modifier.fillMaxWidth().testTag("detail-delete"), icon = Icons.Default.Delete, tone = ButtonTone.Danger)
                }
            }
        }
    }
}

/** The card's word with its state, and its translation under it. */
@Composable
private fun CardHeading(row: BrowseRow) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            Text(row.word, Modifier.weight(1f, fill = false), style = AppText.listWord(28).copy(color = colors.onSurface, textDirection = if (row.rightToLeft) TextDirection.Rtl else TextDirection.Ltr))
            StatePill(row, large = true)
        }
        if (row.translation.isNotEmpty()) Text(row.translation, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
    }
}

/** Where the card stands, two figures to a line, with its edit and suspend under them [withButtons]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.CardSchedule(row: BrowseRow, now: Instant, actions: BrowseActions, withButtons: Boolean = true) {
    Text(tr("Schedule"), style = AppText.itemTitle)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), verticalArrangement = Arrangement.spacedBy(AppSpacing.m), maxItemsInEachRow = 2) {
        val tile = Modifier.weight(1f)
        StatTile(tr("Due"), dueLabel(row, now), tile)
        StatTile(tr("Interval"), intervalLabel(row), tile)
        StatTile(tr("Reviews"), "${row.reps}", tile)
        StatTile(tr("Lapses"), "${row.lapses}", tile)
        StatTile(tr("Last review"), row.lastReview?.relativeTo(now) ?: NONE, tile)
        StatTile(tr("Created"), row.created?.formatDate() ?: NONE, tile)
        // FSRS's memory state, as Anki's card info shows it: difficulty on a percentage scale, stability in days, the chance of recall now.
        StatTile(tr("Difficulty"), row.difficulty?.let { "${((it - 1) / 9 * 100).roundToInt()}%" } ?: NONE, tile)
        StatTile(tr("Stability"), row.stability?.let { trPlural(it.roundToInt(), "{0} day", "{0} days") } ?: NONE, tile)
        StatTile(tr("Recall now"), row.retrievability?.let { "${(it * 100).roundToInt()}%" } ?: NONE, tile)
    }
    if (withButtons) {
        Row(Modifier.fillMaxWidth().padding(top = AppSpacing.xs), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
            CardButtons(row, actions, Modifier.weight(1f))
        }
    }
}

/** The term's editor and suspending the card, under a phone's schedule or beside the dialog's Done. */
@Composable
private fun RowScope.CardButtons(row: BrowseRow, actions: BrowseActions, modifier: Modifier = Modifier) {
    SecondaryButton(tr("Edit term"), onClick = { actions.onEdit(row.termId) }, modifier = modifier, icon = Icons.Default.Edit)
    SecondaryButton(
        if (row.suspended) tr("Resume") else tr("Suspend"),
        onClick = { actions.onSuspend(listOf(row.termId), !row.suspended) },
        modifier = modifier.testTag("detail-suspend"),
        icon = if (row.suspended) AppIcons.PlayArrow else AppIcons.Pause,
    )
}

/** Every answer the card got, the latest first. */
@Composable
private fun ReviewHistory(detail: BrowseDetail, compact: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    when {
        detail.loading -> Text(tr("Loading..."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        detail.reviews.isEmpty() -> Text(tr("No answers yet."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        else -> Column(modifier.fillMaxWidth().testTag("review-history")) {
            detail.reviews.forEachIndexed { index, review ->
                if (index > 0) AppDivider(color = colors.outlineVariant.copy(alpha = AppAlpha.muted))
                ReviewRow(review, compact)
            }
        }
    }
}

/**
 * The example sentence the card blanks the word out of, large, as the card shows it, with the
 * word marked; a pencil changes it in place and the examples offer another. Without one, the
 * offers to add one either way.
 */
@Composable
private fun SentenceBox(row: BrowseRow, wordRange: IntRange?, translating: Boolean, compact: Boolean, actions: BrowseActions) {
    val colors = MaterialTheme.colorScheme
    val direction = if (row.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
    val sentenceStyle = AppText.cardSentence(if (compact) 22f else 28f, FontWeight.SemiBold).copy(color = colors.onSurface, textDirection = direction)
    var editing by remember(row.termId) { mutableStateOf(false) }
    var choosing by remember(row.termId) { mutableStateOf(false) }
    if (choosing) {
        ChooseExampleDialog(
            termId = row.termId,
            languageId = row.languageId,
            word = row.word,
            sentence = row.sentence,
            translation = null,
            onReplace = { sentence, translation -> choosing = false; actions.onSaveSentence(row.termId, sentence, translation) },
            onDismiss = { choosing = false },
        )
    }
    val sentence = row.sentence
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(tr("Example sentence"), style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
        when {
            editing -> {
                // Cancel puts back the sentence and its translation as they were when the typing began.
                val original = remember { row.sentence to row.sentenceTranslation }
                EditingNotice(row.termId, actions.onEditing)
                AutoSavedField(
                    initial = row.sentence.orEmpty(),
                    placeholder = tr("Example sentence"),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = direction),
                    fieldTag = "sentence-field",
                    onSave = { actions.onSaveSentence(row.termId, it, null) },
                    onCancel = { actions.onSaveSentence(row.termId, original.first.orEmpty(), original.second); editing = false },
                    onFinish = { editing = false },
                )
            }
            sentence == null -> Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                QuietButton(tr("Add a sentence"), onClick = { editing = true }, modifier = Modifier.testTag("sentence-edit"), icon = Icons.Default.Edit)
                QuietButton(tr("Choose from examples"), onClick = { choosing = true }, modifier = Modifier.testTag("sentence-choose"), icon = AppIcons.Sync)
            }
            else -> Row(
                Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.primary.copy(alpha = AppAlpha.wash))
                    .border(AppBorders.hairline(colors.primary.copy(alpha = AppAlpha.line)), AppShapes.card)
                    .padding(start = if (compact) AppSpacing.l else AppSpacing.xl, end = AppSpacing.m, top = if (compact) AppSpacing.l else AppSpacing.xl, bottom = if (compact) AppSpacing.l else AppSpacing.xl),
                verticalAlignment = if (compact) Alignment.Top else Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
            ) {
                val hover = SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)
                Text(markWord(sentence, wordRange, hover) { actions.onPickWord(row.termId, it) }, Modifier.weight(1f).testTag("detail-sentence"), style = sentenceStyle)
                AppIconButton(AppIcons.Sync, contentDescription = tr("Choose from examples"), onClick = { choosing = true }, modifier = Modifier.testTag("sentence-choose"), style = IconButtonStyle.Outlined)
                AppIconButton(Icons.Default.Edit, contentDescription = tr("Edit sentence"), onClick = { editing = true }, modifier = Modifier.testTag("sentence-edit"), style = IconButtonStyle.Outlined)
            }
        }
        if (!editing && sentence != null) {
            Text(
                if (compact) tr("Tap another word to hide it on the card instead.") else tr("Click another word to hide it on the card instead."),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        if (!editing && sentence != null) SentenceTranslation(row, translating, compact, actions.onSaveTranslation, actions.onEditing)
    }
}

/** Tells the screen [termId]'s text is being typed while this is shown. */
@Composable
private fun EditingNotice(termId: Long, onEditing: (termId: Long, editing: Boolean) -> Unit) {
    DisposableEffect(termId) {
        onEditing(termId, true)
        onDispose { onEditing(termId, false) }
    }
}

/**
 * A field that keeps what is typed: saved a moment after the typing stops, and at once when Enter
 * ends it ([onFinish]) or the field goes, as with the dialog closed. Cancel saves nothing more and
 * leaves [onCancel] to put back what was there before.
 */
@Composable
private fun AutoSavedField(
    initial: String,
    placeholder: String,
    textStyle: TextStyle,
    fieldTag: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    var saved by remember { mutableStateOf(initial) }
    var cancelled by remember { mutableStateOf(false) }
    val save by rememberUpdatedState(onSave)
    val latest by rememberUpdatedState(draft)
    LaunchedEffect(draft) {
        if (draft == saved) return@LaunchedEffect
        delay(AUTOSAVE_DELAY)
        save(draft)
        saved = draft
    }
    DisposableEffect(Unit) { onDispose { if (!cancelled && latest != saved) save(latest) } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        AppTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth().testTag(fieldTag).onPreviewKeyEvent { event ->
                // Enter ends the typing, the text kept; Shift+Enter is left to the field.
                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                    onFinish()
                    true
                } else {
                    false
                }
            },
            placeholder = { Text(placeholder) },
            textStyle = textStyle,
            minLines = 2,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            QuietButton(tr("Cancel"), onClick = { cancelled = true; onCancel() }, modifier = Modifier.testTag("$fieldTag-cancel"))
        }
    }
}

/** How long typing rests before it is saved. */
private val AUTOSAVE_DELAY = 500.milliseconds

/** The sentence's translation as the card shows it, with a pencil to write it by hand; emptied, a new one is made. */
@Composable
private fun SentenceTranslation(row: BrowseRow, translating: Boolean, compact: Boolean, onSave: (termId: Long, translation: String) -> Unit, onEditing: (termId: Long, editing: Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var editing by remember(row.termId) { mutableStateOf(false) }
    val translation = row.sentenceTranslation
    val style = AppText.cardTranslation(compact)
    Column(Modifier.fillMaxWidth().padding(top = AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        Text(tr("Translation"), style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
        if (editing) {
            val original = remember { translation.orEmpty() }
            EditingNotice(row.termId, onEditing)
            AutoSavedField(
                initial = original,
                placeholder = tr("Translation"),
                textStyle = MaterialTheme.typography.bodyLarge,
                fieldTag = "translation-field",
                onSave = { onSave(row.termId, it) },
                onCancel = { onSave(row.termId, original); editing = false },
                onFinish = { editing = false },
            )
        } else {
            Row(
                Modifier.fillMaxWidth().clip(AppShapes.control).background(colors.surfaceVariant.copy(alpha = AppAlpha.disabled)).padding(start = AppSpacing.l, end = AppSpacing.xs, top = AppSpacing.xs, bottom = AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    translation ?: if (translating) tr("Translating…") else tr("No translation available"),
                    Modifier.weight(1f).padding(vertical = AppSpacing.s).testTag("detail-translation"),
                    style = style.copy(color = if (translation != null) colors.onSurfaceVariant else colors.outline),
                )
                AppIconButton(Icons.Default.Edit, contentDescription = tr("Edit translation"), onClick = { editing = true }, modifier = Modifier.testTag("translation-edit"), size = IconButtonSize.Small)
            }
        }
    }
}

/**
 * [sentence] with the word at [range] on the highlighter's yellow (as it is when the range does not
 * fit it), and every other word a link that [onPick]s it, [hover] while the pointer is over it.
 */
private fun markWord(sentence: String, range: IntRange?, hover: SpanStyle, onPick: (String) -> Unit): AnnotatedString = buildAnnotatedString {
    append(sentence)
    val marked = range?.takeIf { it.first >= 0 && it.last < sentence.length && !it.isEmpty() }
    marked?.let { addStyle(SpanStyle(background = AppAccents.highlightSoft, color = AppAccents.inkOnHighlight), it.first, it.last + 1) }
    SENTENCE_WORD.findAll(sentence).filter { it.range != marked }.forEach { word ->
        addLink(LinkAnnotation.Clickable(word.value, TextLinkStyles(hoveredStyle = hover)) { onPick(word.value) }, word.range.first, word.range.last + 1)
    }
}

/** A word of a sentence, as a card's word is found in one. */
private val SENTENCE_WORD = Regex("""[\p{L}\p{M}\p{Nd}'’-]+""")

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.clip(AppShapes.control).background(colors.surfaceVariant.copy(alpha = AppAlpha.disabled)).padding(horizontal = AppSpacing.m, vertical = AppSpacing.s)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Text(value, style = AppText.emphasis, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One answer: when, which, the state the card was in, and the wait it gave. */
@Composable
private fun ReviewRow(review: FlashcardReview, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val zone = TimeZone.currentSystemDefault()
    Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Column(Modifier.weight(1f)) {
            Text("${review.at.formatDate(zone)} ${clockTime(review.at)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                if (review.intervalDays > 0) tr("{0}, then {1}", stateName(review.stateBefore), trPlural(review.intervalDays, "{0} day", "{0} days")) else stateName(review.stateBefore),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Tag(tr(review.rating.label), review.rating.tint, size = if (compact) TagSize.Small else TagSize.Medium, shape = AppShapes.compact)
    }
}
