package com.idiomiko.feature.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import com.idiomiko.core.domain.flashcards.CardState
import com.idiomiko.core.ui.components.AppCheckbox
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenu
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.AppVerticalDivider
import com.idiomiko.core.ui.components.ListPager
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PhoneSelectionBar
import com.idiomiko.core.ui.components.PickerSheet
import com.idiomiko.core.ui.components.ScrollList
import com.idiomiko.core.ui.components.SecondaryButton
import com.idiomiko.core.ui.components.SelectButton
import com.idiomiko.core.ui.components.SelectionAction
import com.idiomiko.core.ui.components.itemMotion
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText

/**
 * The browser on a phone: the search, the Anki export, the three figures, the groups in a scrolling row, the count with
 * the sort, and the cards as cards of their own, a page at a time. Each card has its checkbox, which picks it, as the
 * vocabulary's terms do; a tap elsewhere on it opens it. The review is in the Flashcards tab's sheet.
 */
@Composable
internal fun PhoneBrowseFlashcardsScreen(
    state: BrowseUiState,
    viewModel: BrowseFlashcardsViewModel,
    actions: BrowseActions,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var sorting by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { AppTopBar(title = tr("Browse flashcards"), onNavigate = onNavigate, section = NavSection.FLASHCARDS, onBack = onBack) },
        // While cards are ticked, what can be done with them takes the foot.
        bottomBar = { if (state.selected.isNotEmpty()) PhoneCardSelectionBar(state, viewModel, actions) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        val list = rememberLazyListState()
        val anki = state.canExportAnki && state.counts.all > 0
        // Search, the Anki export, the figures, groups and the count come before the cards.
        val rowsBefore = 4 + (if (anki) 1 else 0)
        FocusRow(state, state.pageRows, list, rowsBefore = rowsBefore, onShown = viewModel::focusShown)
        // A new page starts at its first card.
        var shownPage by remember { mutableStateOf(state.page) }
        LaunchedEffect(state.page) {
            if (state.page != shownPage && list.firstVisibleItemIndex >= rowsBefore) list.scrollToItem(rowsBefore)
            shownPage = state.page
        }
        ScrollList(
            Modifier.padding(padding).fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(start = AppSpacing.l, end = AppSpacing.l, top = AppSpacing.xs, bottom = AppSpacing.l),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            item { CardSearchField(state.search, viewModel::setSearch, Modifier.fillMaxWidth()) }
            if (anki) item {
                SecondaryButton(tr("Export to Anki"), onClick = actions.onExportAnki, modifier = Modifier.fillMaxWidth().testTag("export-anki"), icon = AppIcons.Download)
            }
            item { BrowseFigures(state, viewModel::setView, compact = true) }
            item {
                // Unbounded on the right, the chips stay on one line and scroll sideways.
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    ViewChips(state, viewModel::setView, large = true, short = true)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().heightIn(min = AppSizes.control), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        trPlural(state.rows.size, "{0} card", "{0} cards"),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                    SelectButton(state.sort.field.label, onClick = { sorting = true }, modifier = Modifier.testTag("phone-sort"), icon = AppIcons.SwapVert)
                }
            }
            when {
                state.loading -> item { Text(tr("Loading..."), Modifier.padding(AppSpacing.l), color = colors.onSurfaceVariant) }
                state.rows.isEmpty() -> item { EmptyCards(state) }
                else -> {
                    items(state.pageRows, key = { row -> row.termId }) { row -> Column(itemMotion()) {
                        PhoneCard(row, state, actions)
                    }}
                    item { ListPager(state.page, state.pageSize, state.rows.size, viewModel::setPageSize, viewModel::goToPage, compact = true) }
                }
            }
        }
    }
    if (sorting) {
        PickerSheet(
            title = tr("Sort"),
            options = BrowseSortField.entries,
            selected = state.sort.field,
            label = { it.label },
            onDismiss = { sorting = false },
            onPick = { viewModel.sortBy(it); sorting = false },
        )
    }
    state.detail?.let { DetailScreen(it, state.now, actions, onDismiss = viewModel::closeDetail) }
}

/**
 * The ticked cards' actions at a phone's foot: suspending them (or resuming, when all are suspended), their due date,
 * resetting and deleting them, and under More the rest, learning new ones first or last and resuming the suspended
 * among others.
 */
@Composable
private fun PhoneCardSelectionBar(state: BrowseUiState, viewModel: BrowseFlashcardsViewModel, actions: BrowseActions) {
    val colors = MaterialTheme.colorScheme
    val chosen = state.rows.filter { it.termId in state.selected }
    val anyActive = chosen.any { !it.suspended }
    val anySuspended = chosen.any { it.suspended }
    val answered = chosen.filter { it.state != CardState.NEW }.map { it.termId }
    val reorders = state.repositions && chosen.any { it.state == CardState.NEW && !it.suspended }
    val all = state.rows.isNotEmpty() && state.selected.containsAll(state.rows.map { it.termId })
    PhoneSelectionBar(state.selected.size, all, onAll = { viewModel.selectAllShown(!all) }, onClear = viewModel::clearSelection) {
        if (anyActive) SelectionAction(AppIcons.Pause, tr("Suspend"), { actions.onSuspend(state.selected, true); viewModel.clearSelection() }, "selection-suspend")
        else SelectionAction(AppIcons.PlayArrow, tr("Resume"), { actions.onSuspend(state.selected, false); viewModel.clearSelection() }, "selection-resume")
        SelectionAction(AppIcons.Schedule, tr("Due date"), { actions.onSetDue(answered) }, "set-due", enabled = answered.isNotEmpty())
        SelectionAction(AppIcons.Replay, tr("Reset"), { actions.onReset(state.selected); viewModel.clearSelection() }, "selection-reset", tint = colors.error)
        SelectionAction(Icons.Default.Delete, tr("Delete"), { actions.onDelete(state.selected) }, "delete-selected", tint = colors.error)
        // Learning first or last, and resuming beside suspending, are less often wanted: under More, when they apply.
        if (reorders || (anyActive && anySuspended)) {
            var more by remember { mutableStateOf(false) }
            Box(Modifier.weight(1f)) {
                Row { SelectionAction(AppIcons.MoreHoriz, tr("More"), { more = true }, "selection-more") }
                AppMenu(more, onDismissRequest = { more = false }) {
                    if (anyActive && anySuspended) AppMenuItem(text = { Text(tr("Resume")) }, onClick = { more = false; actions.onSuspend(state.selected, false); viewModel.clearSelection() })
                    if (reorders) {
                        AppMenuItem(text = { Text(tr("Learn first")) }, onClick = { more = false; actions.onReposition(state.selected, true) }, modifier = Modifier.testTag("learn-first"))
                        AppMenuItem(text = { Text(tr("Learn last")) }, onClick = { more = false; actions.onReposition(state.selected, false) }, modifier = Modifier.testTag("learn-last"))
                    }
                }
            }
        }
    }
}

internal val BrowseSortField.label: String
    get() = when (this) {
        BrowseSortField.WORD -> tr("Word")
        BrowseSortField.STATE -> tr("State")
        BrowseSortField.DUE -> tr("Due")
        BrowseSortField.INTERVAL -> tr("Interval")
        BrowseSortField.REVIEWS -> tr("Reviews")
        BrowseSortField.LAPSES -> tr("Lapses")
        BrowseSortField.LAST_REVIEW -> tr("Last review")
        BrowseSortField.CREATED -> tr("Created")
    }

/**
 * A card on a card of its own: its checkbox, the word with its state, the translation and the sentence, and
 * when it comes, its interval and lapses along the foot beside its menu.
 */
@Composable
private fun PhoneCard(row: BrowseRow, state: BrowseUiState, actions: BrowseActions) {
    val colors = MaterialTheme.colorScheme
    val selected = row.termId in state.selected
    val dim = if (row.suspended) colors.outline else colors.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.card)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
            .border(AppBorders.hairline(if (selected) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), AppShapes.card)
            .clickable { actions.onOpen(row.termId) }
            .padding(start = AppSpacing.xs, end = AppSpacing.xs, top = AppSpacing.m)
            .testTag("browse-row-${row.termId}"),
    ) {
        AppCheckbox(checked = selected, onCheckedChange = { actions.onToggle(row.termId) }, modifier = Modifier.testTag("check-${row.termId}"))
        Column(Modifier.weight(1f)) {
            Row(Modifier.padding(end = AppSpacing.s), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(
                    row.word,
                    Modifier.weight(1f),
                    style = AppText.listWord(22).copy(color = if (row.suspended) colors.outline else colors.primary, textDirection = if (row.rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatePill(row, large = true)
            }
            if (row.translation.isNotEmpty()) Text(row.translation, Modifier.padding(top = AppSpacing.xs, end = AppSpacing.s), style = MaterialTheme.typography.bodyLarge, color = dim, maxLines = 2, overflow = TextOverflow.Ellipsis)
            row.sentence?.let {
                Text(
                    it,
                    Modifier.padding(top = AppSpacing.xs, end = AppSpacing.s),
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = if (row.rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
                    color = dim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AppDivider(Modifier.padding(top = AppSpacing.m, end = AppSpacing.s))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                val dueTint = if (row.isDue && !row.suspended) AppAccents.orange else dim
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Icon(AppIcons.Schedule, contentDescription = null, tint = dueTint, modifier = Modifier.size(AppSizes.iconSmall))
                    Text(dueLabel(row, state.now), style = MaterialTheme.typography.bodyMedium, color = dueTint, maxLines = 1)
                }
                if (row.intervalDays > 0) FootFact(tr("Interval {0}", intervalLabel(row)), dim)
                if (row.lapses > 0) FootFact(trPlural(row.lapses, "{0} lapse", "{0} lapses"), AppAccents.red)
                Spacer(Modifier.weight(1f))
                RowMenu(row, actions)
            }
        }
    }
}

/** One more fact along a card's foot, set off from the one before by a short rule. */
@Composable
private fun FootFact(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        AppVerticalDivider(Modifier.height(AppSizes.iconSmall))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
