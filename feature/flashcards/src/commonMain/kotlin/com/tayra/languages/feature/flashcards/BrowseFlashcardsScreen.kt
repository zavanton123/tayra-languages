package com.tayra.languages.feature.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.ui.components.AppBottomSheet
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.IconButtonSize
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
import com.tayra.languages.core.ui.components.SelectionBar
import com.tayra.languages.core.ui.components.TableHeader
import com.tayra.languages.core.ui.components.TableHeaderCell
import com.tayra.languages.core.ui.components.formatDate
import com.tayra.languages.core.ui.components.relativeTo
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt
import kotlin.time.Instant

/**
 * Every flashcard of the language being learned in one list, as Anki's browser: searched, shown
 * by group, sorted by column, selected for suspending or resetting together, and opened for its
 * schedule and review history.
 */
@Composable
fun BrowseFlashcardsScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, termId: Long? = null, viewModel: BrowseFlashcardsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The card the browser was opened for is opened once; coming back from editing a term only shows the cards as they are now.
    var arrived by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.load(open = termId.takeUnless { arrived })
        arrived = true
    }
    var confirmReset by remember { mutableStateOf<Collection<Long>?>(null) }
    var confirmDelete by remember { mutableStateOf<Collection<Long>?>(null) }
    val actions = BrowseActions(
        onOpen = viewModel::open,
        onToggle = viewModel::toggleSelected,
        onEdit = { onNavigate(Route.EditTerm(it)) },
        onSuspend = viewModel::setSuspended,
        onReset = { confirmReset = it.toList() },
        onDelete = { confirmDelete = it.toList() },
        onReview = { onNavigate(Route.Flashcards) },
        onSaveSentence = viewModel::saveSentence,
    )
    val width = LocalWindowWidth.current
    if (width.isCompact) {
        PhoneBrowseFlashcardsScreen(state, viewModel, actions, onNavigate = onNavigate, onBack = onBack)
    } else {
        Scaffold(topBar = { AppTopBar(title = tr("Browse flashcards"), onNavigate = onNavigate, onBack = onBack, section = NavSection.FLASHCARDS) }) { padding ->
            Row(Modifier.padding(padding).fillMaxSize()) {
                BrowseContent(state, viewModel, actions, Modifier.weight(1f))
                val detail = state.detail
                if (detail != null && width.isExpanded) {
                    DetailPanel(detail, state.now, actions, onClose = viewModel::closeDetail, Modifier.width(DETAIL_WIDTH).fillMaxHeight())
                }
            }
        }
        val detail = state.detail
        if (detail != null && !width.isExpanded) DetailSheet(detail, state.now, actions, onDismiss = viewModel::closeDetail)
    }
    confirmDelete?.let { ids ->
        ConfirmDialog(
            title = trPlural(ids.size, "Delete {0} flashcard?", "Delete {0} flashcards?"),
            text = trPlural(ids.size, "Its word is deleted from your vocabulary too, with the card's review history. This cannot be undone.", "Their words are deleted from your vocabulary too, with the cards' review history. This cannot be undone."),
            confirmLabel = tr("Delete"),
            destructive = true,
            onConfirm = { viewModel.delete(ids); confirmDelete = null },
            onDismiss = { confirmDelete = null },
        )
    }
    confirmReset?.let { ids ->
        ConfirmDialog(
            title = trPlural(ids.size, "Reset {0} flashcard?", "Reset {0} flashcards?"),
            text = tr("The cards become new again and their review schedules are forgotten. The terms' statuses stay as they are."),
            confirmLabel = tr("Reset"),
            destructive = true,
            onConfirm = { viewModel.reset(ids); confirmReset = null },
            onDismiss = { confirmReset = null },
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
    val onSaveSentence: (termId: Long, sentence: String) -> Unit = { _, _ -> },
)

private val DETAIL_WIDTH = 400.dp

/** The page beside the detail panel: heading, search, the groups, the selection's actions and the table. */
@Composable
private fun BrowseContent(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel, actions: BrowseActions, modifier: Modifier) {
    val list = rememberLazyListState()
    // Heading, toolbar, groups, the selection's actions when there is a selection, and the table header come before the rows.
    FocusRow(state, list, rowsBefore = if (state.selected.isNotEmpty()) 5 else 4, onShown = viewModel::focusShown)
    ScrollList(modifier, state = list, contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)) {
        item {
            Box(Modifier.padding(bottom = 20.dp)) {
                ScreenHeader(
                    tr("Browse flashcards"),
                    subtitle = if (state.languageName.isEmpty()) tr("Every card of the words you are learning, with where each stands.")
                    else tr("Every card of the {0} words you are learning, with where each stands.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
                ) { PrimaryButton(tr("Review cards"), onClick = actions.onReview, icon = AppIcons.Style) }
            }
        }
        item { Toolbar(state, viewModel) }
        item { ViewChips(state, viewModel::setView, Modifier.fillMaxWidth().padding(bottom = 14.dp)) }
        if (state.selected.isNotEmpty()) item { CardSelectionBar(state, viewModel, actions) }
        when {
            state.loading -> item { Text(tr("Loading..."), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            state.rows.isEmpty() -> item { EmptyCards(state) }
            else -> {
                item { CardTableHeader(state, viewModel) }
                itemsIndexed(state.rows, key = { _, row -> row.termId }) { index, row ->
                    CardTableRow(row, state, index, actions)
                }
            }
        }
    }
}

/** Scrolls the list to the card asked for, a little above it so the rows before it show too. */
@Composable
internal fun FocusRow(state: BrowseUiState, list: LazyListState, rowsBefore: Int, onShown: () -> Unit) {
    val focus = state.focus
    LaunchedEffect(focus, state.rows) {
        if (focus == null) return@LaunchedEffect
        val index = state.rows.indexOfFirst { it.termId == focus }
        if (index >= 0) list.animateScrollToItem((rowsBefore + index - 2).coerceAtLeast(0))
        onShown()
    }
}

@Composable
private fun Toolbar(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CardSearchField(state.search, viewModel::setSearch, Modifier.width(560.dp))
        Spacer(Modifier.weight(1f))
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
        BrowseView.NEW -> tr("New")
        BrowseView.LEARNING -> tr("Learning")
        BrowseView.REVIEW -> tr("Review")
        BrowseView.FORGOTTEN -> tr("Forgotten before")
        BrowseView.SUSPENDED -> tr("Suspended")
    }

/** The groups of cards to show, each with how many it holds. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ViewChips(state: BrowseUiState, onSelect: (BrowseView) -> Unit, modifier: Modifier = Modifier, large: Boolean = false) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BrowseView.entries.forEach { view -> ViewChip(view, state.counts[view], selected = view == state.view, large) { onSelect(view) } }
    }
}

@Composable
private fun ViewChip(view: BrowseView, count: Int, selected: Boolean, large: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = when (view) {
        BrowseView.NEW -> AppAccents.blue
        BrowseView.LEARNING -> AppAccents.orange
        BrowseView.REVIEW -> AppAccents.green
        BrowseView.DUE -> AppAccents.orange
        BrowseView.FORGOTTEN -> AppAccents.red
        BrowseView.SUSPENDED -> AppAccents.gray
        BrowseView.ALL -> colors.primary
    }
    Row(
        Modifier.height(if (large) 46.dp else 36.dp).clip(AppShapes.pill)
            .background(if (selected) tint.copy(alpha = 0.14f) else Color.Transparent)
            .border(AppBorders.hairline(if (selected) tint else colors.outlineVariant), AppShapes.pill)
            .clickable(onClick = onClick).padding(horizontal = if (large) 18.dp else 14.dp).testTag("browse-view-${view.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(view.label, style = if (large) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) tint else colors.onSurface)
        Text("$count", style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = if (selected) tint else colors.onSurfaceVariant)
    }
}

/** What can be done with the selected cards together. */
@Composable
internal fun CardSelectionBar(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel, actions: BrowseActions) {
    val chosen = state.rows.filter { it.termId in state.selected }
    val anyActive = chosen.any { !it.suspended }
    val anySuspended = chosen.any { it.suspended }
    SelectionBar(tr("{0} selected", state.selected.size), Modifier.padding(bottom = 14.dp).testTag("selection-bar")) {
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
private fun CardTableHeader(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel) {
    val allSelected = state.rows.isNotEmpty() && state.rows.all { it.termId in state.selected }
    TableHeader {
        AppCheckbox(checked = allSelected, onCheckedChange = viewModel::selectAllShown, modifier = Modifier.testTag("browse-select-all"))
        SortHeader(tr("Card"), BrowseSortField.WORD, state.sort, Modifier.weight(CARD_WEIGHT), viewModel::sortBy)
        TableHeaderCell(tr("Sentence"), Modifier.weight(SENTENCE_WEIGHT))
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
        open -> colors.primary.copy(alpha = 0.14f)
        selected || hovered -> colors.primary.copy(alpha = 0.08f)
        index % 2 == 1 -> colors.surfaceVariant.copy(alpha = 0.25f)
        else -> Color.Transparent
    }
    val dim = if (row.suspended) colors.outline else colors.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().background(background).hoverable(interaction).clickable { actions.onOpen(row.termId) }.padding(horizontal = 8.dp, vertical = 2.dp).testTag("browse-row-${row.termId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppCheckbox(checked = selected, onCheckedChange = { actions.onToggle(row.termId) })
        Column(Modifier.weight(CARD_WEIGHT).padding(end = 12.dp)) {
            Text(
                row.word,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (row.suspended) colors.outline else colors.primary, textDirection = if (row.rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.translation.isNotEmpty()) Text(row.translation, style = MaterialTheme.typography.bodySmall, color = dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(row.sentence.orEmpty(), Modifier.weight(SENTENCE_WEIGHT).padding(end = 12.dp), style = MaterialTheme.typography.bodyMedium, color = dim, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Box(Modifier.width(STATE_WIDTH)) { StatePill(row) }
        Text(dueLabel(row, state.now), Modifier.width(DUE_WIDTH), style = MaterialTheme.typography.bodyMedium, color = if (row.isDue && !row.suspended) AppAccents.orange else dim, fontWeight = if (row.isDue) FontWeight.Medium else FontWeight.Normal)
        Text(intervalLabel(row), Modifier.width(INTERVAL_WIDTH), style = MaterialTheme.typography.bodyMedium, color = dim)
        Text("${row.reps}", Modifier.width(COUNT_WIDTH), style = MaterialTheme.typography.bodyMedium, color = dim)
        Text("${row.lapses}", Modifier.width(COUNT_WIDTH), style = MaterialTheme.typography.bodyMedium, color = if (row.lapses > 0) AppAccents.red else dim)
        Text(row.lastReview?.relativeTo(state.now) ?: NONE, Modifier.width(LAST_WIDTH), style = MaterialTheme.typography.bodyMedium, color = dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { RowMenu(row, actions) }
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

/** The card's state on its colour; a suspended card is grey whatever its state. */
@Composable
internal fun StatePill(row: BrowseRow, large: Boolean = false) {
    val (label, tint) = when {
        row.suspended -> tr("Suspended") to AppAccents.gray
        else -> stateName(row.state) to stateTint(row.state)
    }
    Text(
        label,
        Modifier.clip(AppShapes.compact).background(tint.copy(alpha = 0.1f)).border(AppBorders.hairline(tint.copy(alpha = 0.25f)), AppShapes.compact)
            .padding(horizontal = if (large) 12.dp else 10.dp, vertical = 4.dp),
        style = TextStyle(fontSize = if (large) 14.sp else 12.sp, letterSpacing = 0.6.sp, color = tint, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
}

internal fun stateTint(state: CardState): Color = when (state) {
    CardState.NEW -> AppAccents.blue
    CardState.LEARNING, CardState.RELEARNING -> AppAccents.orange
    CardState.REVIEW -> AppAccents.green
}

internal fun stateName(state: CardState): String = when (state) {
    CardState.NEW -> tr("New")
    CardState.LEARNING -> tr("Learning")
    CardState.RELEARNING -> tr("Relearning")
    CardState.REVIEW -> tr("Review")
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

/** The card's schedule, memory and review history, with what can be done to it, beside the table. */
@Composable
private fun DetailPanel(detail: BrowseDetail, now: Instant, actions: BrowseActions, onClose: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.background(colors.surface)) {
        Row(Modifier.fillMaxHeight()) {
            AppVerticalDivider()
            ScrollColumn(Modifier.fillMaxSize().testTag("browse-detail"), contentModifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Card info"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    AppIconButton(Icons.Default.Close, contentDescription = tr("Close card info"), onClick = onClose)
                }
                CardDetail(detail, now, actions, compact = false)
            }
        }
    }
}

@Composable
internal fun DetailSheet(detail: BrowseDetail, now: Instant, actions: BrowseActions, onDismiss: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        ScrollColumn(Modifier.fillMaxWidth().testTag("browse-detail"), contentModifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            CardDetail(detail, now, actions, compact = true)
        }
    }
}

/** The opened card: its content, where it stands, what to do with it, and every answer it got. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CardDetail(detail: BrowseDetail, now: Instant, actions: BrowseActions, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val row = detail.row
    val direction = if (row.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(row.word, Modifier.weight(1f, fill = false), style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, textDirection = direction))
                StatePill(row)
            }
            if (row.translation.isNotEmpty()) Text(row.translation, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        SentenceBox(row, actions.onSaveSentence)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
            val tile = Modifier.weight(1f)
            StatTile(tr("Due"), dueLabel(row, now), tile)
            StatTile(tr("Interval"), intervalLabel(row), tile)
            StatTile(tr("Reviews"), "${row.reps}", tile)
            StatTile(tr("Lapses"), "${row.lapses}", tile)
            StatTile(tr("Last review"), row.lastReview?.relativeTo(now) ?: NONE, tile)
            StatTile(tr("Added"), row.added?.formatDate() ?: NONE, tile)
            // FSRS's memory state, as Anki's card info shows it: difficulty on a percentage scale, stability in days, the chance of recall now.
            StatTile(tr("Difficulty"), row.difficulty?.let { "${((it - 1) / 9 * 100).roundToInt()}%" } ?: NONE, tile)
            StatTile(tr("Stability"), row.stability?.let { trPlural(it.roundToInt(), "{0} day", "{0} days") } ?: NONE, tile)
            StatTile(tr("Recall now"), row.retrievability?.let { "${(it * 100).roundToInt()}%" } ?: NONE, tile)
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(tr("Edit term"), onClick = { actions.onEdit(row.termId) }, icon = Icons.Default.Edit)
            SecondaryButton(
                if (row.suspended) tr("Resume") else tr("Suspend"),
                onClick = { actions.onSuspend(listOf(row.termId), !row.suspended) },
                modifier = Modifier.testTag("detail-suspend"),
                icon = if (row.suspended) AppIcons.PlayArrow else AppIcons.Pause,
            )
            if (row.state != CardState.NEW) QuietButton(tr("Reset progress"), onClick = { actions.onReset(listOf(row.termId)) }, tone = ButtonTone.Danger)
            QuietButton(tr("Delete"), onClick = { actions.onDelete(listOf(row.termId)) }, modifier = Modifier.testTag("detail-delete"), tone = ButtonTone.Danger)
        }
        AppDivider()
        Text(tr("Review history"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        when {
            detail.loading -> Text(tr("Loading..."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            detail.reviews.isEmpty() -> Text(tr("No answers yet."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            else -> Column(Modifier.fillMaxWidth().testTag("review-history")) {
                detail.reviews.forEachIndexed { index, review ->
                    if (index > 0) AppDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
                    ReviewRow(review, compact)
                }
            }
        }
    }
}

/** The example sentence the card blanks the word out of, with a pencil to change it in place; without one, the offer to add one. */
@Composable
private fun SentenceBox(row: BrowseRow, onSave: (termId: Long, sentence: String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val direction = if (row.rightToLeft) TextDirection.Rtl else TextDirection.Ltr
    var editing by remember(row.termId) { mutableStateOf(false) }
    var draft by remember(row.termId, editing) { mutableStateOf(row.sentence.orEmpty()) }
    val sentence = row.sentence
    when {
        editing -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth().testTag("sentence-field"),
                placeholder = { Text(tr("Example sentence")) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(textDirection = direction),
                minLines = 2,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                QuietButton(tr("Cancel"), onClick = { editing = false })
                PrimaryButton(tr("Save"), onClick = { onSave(row.termId, draft); editing = false }, modifier = Modifier.testTag("sentence-save"))
            }
        }
        sentence == null -> QuietButton(tr("Add a sentence"), onClick = { editing = true }, modifier = Modifier.testTag("sentence-edit"), icon = Icons.Default.Edit)
        else -> Row(
            Modifier.fillMaxWidth().clip(AppShapes.control).background(colors.primary.copy(alpha = 0.05f)).padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(sentence, Modifier.weight(1f).padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium.copy(textDirection = direction), color = colors.onSurfaceVariant)
            AppIconButton(Icons.Default.Edit, contentDescription = tr("Edit sentence"), onClick = { editing = true }, modifier = Modifier.testTag("sentence-edit"), size = IconButtonSize.Small)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.clip(AppShapes.control).background(colors.surfaceVariant.copy(alpha = 0.35f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One answer: when, which, the state the card was in, and the wait it gave. */
@Composable
private fun ReviewRow(review: FlashcardReview, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val zone = TimeZone.currentSystemDefault()
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text("${review.at.formatDate(zone)} ${clockTime(review.at)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                if (review.intervalDays > 0) tr("{0}, then {1}", stateName(review.stateBefore), trPlural(review.intervalDays, "{0} day", "{0} days")) else stateName(review.stateBefore),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        val tint = review.rating.tint
        Text(
            tr(review.rating.label),
            Modifier.clip(AppShapes.compact).background(tint.copy(alpha = 0.12f)).padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}
