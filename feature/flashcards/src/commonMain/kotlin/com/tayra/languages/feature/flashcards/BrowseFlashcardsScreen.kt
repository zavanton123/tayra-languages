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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.formatDate
import com.tayra.languages.core.ui.components.relativeTo
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
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
fun BrowseFlashcardsScreen(onNavigate: (Route) -> Unit, onBack: () -> Unit, viewModel: BrowseFlashcardsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Coming back from editing a term shows the cards as they are now.
    LaunchedEffect(Unit) { viewModel.load() }
    var confirmReset by remember { mutableStateOf<Collection<Long>?>(null) }
    val actions = BrowseActions(
        onOpen = viewModel::open,
        onToggle = viewModel::toggleSelected,
        onEdit = { onNavigate(Route.EditTerm(it)) },
        onSuspend = viewModel::setSuspended,
        onReset = { confirmReset = it.toList() },
        onReview = { onNavigate(Route.Flashcards) },
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
    val onReview: () -> Unit = {},
)

private val DETAIL_WIDTH = 400.dp

private val SUSPENDED_TINT = Color(0xFF8A8F98)

/** The page beside the detail panel: heading, search, the groups, the selection's actions and the table. */
@Composable
private fun BrowseContent(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel, actions: BrowseActions, modifier: Modifier) {
    ScrollList(modifier, contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)) {
        item { PageHeader(state, actions.onReview) }
        item { Toolbar(state, viewModel) }
        item { ViewChips(state, viewModel::setView, Modifier.fillMaxWidth().padding(bottom = 14.dp)) }
        if (state.selected.isNotEmpty()) item { SelectionBar(state, viewModel, actions) }
        when {
            state.loading -> item { Text(tr("Loading..."), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            state.rows.isEmpty() -> item { EmptyCards(state) }
            else -> {
                item { TableHeader(state, viewModel) }
                itemsIndexed(state.rows, key = { _, row -> row.termId }) { index, row ->
                    CardTableRow(row, state, index, actions)
                }
            }
        }
    }
}

@Composable
private fun PageHeader(state: BrowseUiState, onReview: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(tr("Browse flashcards"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (state.languageName.isEmpty()) tr("Every card of the words you are learning, with where each stands.")
                else tr("Every card of the {0} words you are learning, with where each stands.", languageInSentence(state.languageName, LanguageCase.PREPOSITIONAL)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onReview, shape = RoundedCornerShape(10.dp)) {
            Icon(AppIcons.Style, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(tr("Review cards"))
        }
    }
}

@Composable
private fun Toolbar(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CardSearchBox(state.search, viewModel::setSearch, Modifier.width(560.dp))
        Spacer(Modifier.weight(1f))
        Text(
            if (state.selected.isEmpty()) trPlural(state.rows.size, "{0} card", "{0} cards") else tr("{0} of {1} selected", state.selected.size, state.rows.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun CardSearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(44.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.weight(1f).testTag("browse-search"),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(tr("Search words, translations or sentences"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = tr("Clear search"),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(24.dp).clip(CircleShape).clickable { onChange("") }.padding(4.dp),
            )
        }
    }
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
internal fun ViewChips(state: BrowseUiState, onSelect: (BrowseView) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BrowseView.entries.forEach { view -> ViewChip(view, state.counts[view], selected = view == state.view) { onSelect(view) } }
    }
}

@Composable
private fun ViewChip(view: BrowseView, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = when (view) {
        BrowseView.NEW -> NEW_TINT
        BrowseView.LEARNING -> LEARNING_TINT
        BrowseView.REVIEW -> REVIEW_TINT
        BrowseView.DUE -> LEARNING_TINT
        BrowseView.FORGOTTEN -> Color(0xFFD64545)
        BrowseView.SUSPENDED -> SUSPENDED_TINT
        BrowseView.ALL -> colors.primary
    }
    Row(
        Modifier.height(36.dp).clip(RoundedCornerShape(50))
            .background(if (selected) tint.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (selected) tint else colors.outlineVariant, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp).testTag("browse-view-${view.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(view.label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) tint else colors.onSurface)
        Text("$count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = if (selected) tint else colors.onSurfaceVariant)
    }
}

/** What can be done with the selected cards together. */
@Composable
internal fun SelectionBar(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel, actions: BrowseActions, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val chosen = state.rows.filter { it.termId in state.selected }
    val anyActive = chosen.any { !it.suspended }
    val anySuspended = chosen.any { it.suspended }
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(alpha = 0.06f))
            .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp).testTag("selection-bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tr("{0} selected", state.selected.size), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 6.dp))
        if (anyActive) TextButton(onClick = { actions.onSuspend(state.selected, true); viewModel.clearSelection() }) { Text(tr("Suspend")) }
        if (anySuspended) TextButton(onClick = { actions.onSuspend(state.selected, false); viewModel.clearSelection() }) { Text(tr("Resume")) }
        TextButton(onClick = { actions.onReset(state.selected); viewModel.clearSelection() }) { Text(tr("Reset progress"), color = colors.error) }
        if (!compact) Spacer(Modifier.weight(1f))
        TextButton(onClick = viewModel::clearSelection) { Text(tr("Clear selection")) }
    }
}

@Composable
internal fun EmptyCards(state: BrowseUiState) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(AppIcons.Style, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        Text(if (state.filtered) tr("No cards match.") else tr("No flashcards yet"), style = MaterialTheme.typography.titleMedium)
        if (!state.filtered) {
            Text(
                tr("Words you give a status of 1 to 4 while reading get a card here."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
private fun TableHeader(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel) {
    val colors = MaterialTheme.colorScheme
    val allSelected = state.rows.isNotEmpty() && state.rows.all { it.termId in state.selected }
    CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant, LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)).background(colors.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = allSelected, onCheckedChange = viewModel::selectAllShown, modifier = Modifier.testTag("browse-select-all"))
            SortHeader(tr("Card"), BrowseSortField.WORD, state.sort, Modifier.weight(CARD_WEIGHT), viewModel::sortBy)
            HeaderCell(tr("Sentence"), Modifier.weight(SENTENCE_WEIGHT))
            SortHeader(tr("State"), BrowseSortField.STATE, state.sort, Modifier.width(STATE_WIDTH), viewModel::sortBy)
            SortHeader(tr("Due"), BrowseSortField.DUE, state.sort, Modifier.width(DUE_WIDTH), viewModel::sortBy)
            SortHeader(tr("Interval"), BrowseSortField.INTERVAL, state.sort, Modifier.width(INTERVAL_WIDTH), viewModel::sortBy)
            SortHeader(tr("Reviews"), BrowseSortField.REVIEWS, state.sort, Modifier.width(COUNT_WIDTH), viewModel::sortBy)
            SortHeader(tr("Lapses"), BrowseSortField.LAPSES, state.sort, Modifier.width(COUNT_WIDTH), viewModel::sortBy)
            SortHeader(tr("Last review"), BrowseSortField.LAST_REVIEW, state.sort, Modifier.width(LAST_WIDTH), viewModel::sortBy)
            Spacer(Modifier.width(MENU_WIDTH))
        }
    }
}

@Composable
private fun SortHeader(label: String, field: BrowseSortField, sort: BrowseSort, modifier: Modifier, onSort: (BrowseSortField) -> Unit) {
    val active = sort.field == field
    HeaderCell(label, modifier, icon = if (!active) AppIcons.UnfoldMore else if (sort.ascending) AppIcons.ArrowUp else AppIcons.ArrowDown, active = active) { onSort(field) }
}

@Composable
private fun HeaderCell(label: String, modifier: Modifier, icon: ImageVector? = null, active: Boolean = false, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.then(if (onClick != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick) else Modifier).padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = if (active) colors.primary else LocalContentColor.current, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (active) colors.primary else LocalContentColor.current)
    }
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
        Checkbox(checked = selected, onCheckedChange = { actions.onToggle(row.termId) })
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
        Text(dueLabel(row, state.now), Modifier.width(DUE_WIDTH), style = MaterialTheme.typography.bodyMedium, color = if (row.isDue && !row.suspended) LEARNING_TINT else dim, fontWeight = if (row.isDue) FontWeight.Medium else FontWeight.Normal)
        Text(intervalLabel(row), Modifier.width(INTERVAL_WIDTH), style = MaterialTheme.typography.bodyMedium, color = dim)
        Text("${row.reps}", Modifier.width(COUNT_WIDTH), style = MaterialTheme.typography.bodyMedium, color = dim)
        Text("${row.lapses}", Modifier.width(COUNT_WIDTH), style = MaterialTheme.typography.bodyMedium, color = if (row.lapses > 0) Color(0xFFD64545) else dim)
        Text(row.lastReview?.relativeTo(state.now) ?: NONE, Modifier.width(LAST_WIDTH), style = MaterialTheme.typography.bodyMedium, color = dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { RowMenu(row, actions) }
    }
}

/** Shown where a card has no value yet: a new card's interval, or a last review before the first. */
internal const val NONE = "—"

@Composable
internal fun RowMenu(row: BrowseRow, actions: BrowseActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = tr("Card actions"), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text(tr("Card info")) }, onClick = { open = false; actions.onOpen(row.termId) })
            AppMenuItem(text = { Text(tr("Edit term")) }, onClick = { open = false; actions.onEdit(row.termId) })
            AppMenuItem(text = { Text(if (row.suspended) tr("Resume") else tr("Suspend")) }, onClick = { open = false; actions.onSuspend(listOf(row.termId), !row.suspended) })
            AppMenuItem(text = { Text(tr("Reset progress")) }, enabled = row.state != CardState.NEW, onClick = { open = false; actions.onReset(listOf(row.termId)) })
        }
    }
}

/** The card's state on its colour; a suspended card is grey whatever its state. */
@Composable
internal fun StatePill(row: BrowseRow) {
    val (label, tint) = when {
        row.suspended -> tr("Suspended") to SUSPENDED_TINT
        else -> stateName(row.state) to stateTint(row.state)
    }
    Text(
        label,
        Modifier.clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.1f)).border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        style = TextStyle(fontSize = 12.sp, letterSpacing = 0.6.sp, color = tint, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
}

internal fun stateTint(state: CardState): Color = when (state) {
    CardState.NEW -> NEW_TINT
    CardState.LEARNING, CardState.RELEARNING -> LEARNING_TINT
    CardState.REVIEW -> REVIEW_TINT
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
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.outlineVariant))
            ScrollColumn(Modifier.fillMaxSize().testTag("browse-detail"), contentModifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Card info"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = tr("Close card info")) }
                }
                CardDetail(detail, now, actions, compact = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailSheet(detail: BrowseDetail, now: Instant, actions: BrowseActions, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), sheetMaxWidth = Dp.Unspecified, containerColor = MaterialTheme.colorScheme.surface) {
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
        row.sentence?.let { sentence ->
            Text(
                sentence,
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.primary.copy(alpha = 0.05f)).padding(horizontal = 14.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = direction),
                color = colors.onSurfaceVariant,
            )
        }
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
            OutlinedButton(onClick = { actions.onEdit(row.termId) }, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(tr("Edit term"))
            }
            OutlinedButton(onClick = { actions.onSuspend(listOf(row.termId), !row.suspended) }, shape = RoundedCornerShape(10.dp), modifier = Modifier.testTag("detail-suspend")) {
                Icon(if (row.suspended) AppIcons.PlayArrow else AppIcons.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (row.suspended) tr("Resume") else tr("Suspend"))
            }
            if (row.state != CardState.NEW) TextButton(onClick = { actions.onReset(listOf(row.termId)) }) { Text(tr("Reset progress"), color = colors.error) }
        }
        HorizontalDivider(color = colors.outlineVariant)
        Text(tr("Review history"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        when {
            detail.loading -> Text(tr("Loading..."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            detail.reviews.isEmpty() -> Text(tr("No answers yet."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            else -> Column(Modifier.fillMaxWidth().testTag("review-history")) {
                detail.reviews.forEachIndexed { index, review ->
                    if (index > 0) HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
                    ReviewRow(review, compact)
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(colors.surfaceVariant.copy(alpha = 0.35f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
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
            Modifier.clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.12f)).padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}
