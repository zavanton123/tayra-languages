package com.tayra.languages.feature.flashcards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.AppVerticalDivider
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SelectButton
import com.tayra.languages.core.ui.components.itemMotion
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/**
 * The browser on a phone: the search, the review and the sort, the groups in a scrolling row, and
 * the cards as cards of their own; selecting and importing are in the bar's menu, and a long press
 * starts selecting too.
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
    var selecting by rememberSaveable { mutableStateOf(false) }
    var sorting by remember { mutableStateOf(false) }
    val showChecks = selecting || state.selected.isNotEmpty()
    Scaffold(
        topBar = { AppTopBar(title = tr("Browse flashcards"), onNavigate = onNavigate, section = NavSection.FLASHCARDS, onBack = onBack, actions = {
            OverflowMenu(tr("More"), buttonModifier = Modifier.testTag("phone-menu")) { close ->
                AppMenuItem(text = { Text(tr("Select")) }, onClick = { close(); selecting = true }, modifier = Modifier.testTag("phone-select"))
                AppMenuItem(text = { Text(tr("Statistics")) }, onClick = { close(); actions.onStatistics() }, modifier = Modifier.testTag("browse-statistics"))
                if (state.canImportAnki) AppMenuItem(text = { Text(tr("Import from Anki")) }, onClick = { close(); actions.onImportAnki() }, modifier = Modifier.testTag("import-anki"))
            }
        }) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        val list = rememberLazyListState()
        // Search, the review and sort row, groups and the count, with the selection's actions when there is a selection, come before the cards.
        FocusRow(state, state.rows, list, rowsBefore = if (state.selected.isNotEmpty()) 5 else 4, onShown = viewModel::focusShown)
        ScrollList(
            Modifier.padding(padding).fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(start = AppSpacing.l, end = AppSpacing.l, top = AppSpacing.xs, bottom = AppSpacing.l),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            item { CardSearchField(state.search, viewModel::setSearch, Modifier.fillMaxWidth()) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    val due = state.counts.due
                    PrimaryButton(
                        if (due > 0) tr("Review {0} due", due) else tr("Review cards"),
                        onClick = actions.onReview,
                        modifier = Modifier.weight(1f).testTag("phone-review"),
                        icon = AppIcons.Style,
                    )
                    SelectButton(state.sort.field.label, onClick = { sorting = true }, modifier = Modifier.testTag("phone-sort"), icon = AppIcons.SwapVert)
                }
            }
            item {
                // Unbounded on the right, the chips stay on one line and scroll sideways.
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    ViewChips(state, viewModel::setView, large = true, short = true)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().heightIn(min = AppSizes.control), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (state.selected.isEmpty()) trPlural(state.rows.size, "{0} card", "{0} cards") else tr("{0} of {1} selected", state.selected.size, state.rows.size),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                    if (showChecks) QuietButton(onClick = { selecting = false; viewModel.clearSelection() }, modifier = Modifier.testTag("phone-select-done")) {
                        Text(tr("Done"), style = AppText.itemTitle)
                    }
                }
            }
            if (state.selected.isNotEmpty()) item { CardSelectionBar(state, viewModel, actions) }
            when {
                state.loading -> item { Text(tr("Loading..."), Modifier.padding(AppSpacing.l), color = colors.onSurfaceVariant) }
                state.rows.isEmpty() -> item { EmptyCards(state) }
                else -> items(state.rows, key = { row -> row.termId }) { row -> Column(itemMotion()) {
                    PhoneCard(row, state, showCheck = showChecks, actions, onStartSelecting = { selecting = true })
                }}
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
 * A card on a card of its own: the word with its state, the translation and the sentence, and
 * when it comes, its interval and lapses along the foot beside its menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneCard(row: BrowseRow, state: BrowseUiState, showCheck: Boolean, actions: BrowseActions, onStartSelecting: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val selected = row.termId in state.selected
    val dim = if (row.suspended) colors.outline else colors.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.card)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
            .border(AppBorders.hairline(if (selected) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), AppShapes.card)
            .combinedClickable(onLongClick = { onStartSelecting(); actions.onToggle(row.termId) }) { if (showCheck) actions.onToggle(row.termId) else actions.onOpen(row.termId) }
            .padding(start = if (showCheck) AppSpacing.xs else AppSpacing.l, end = AppSpacing.xs, top = AppSpacing.m)
            .testTag("browse-row-${row.termId}"),
    ) {
        if (showCheck) AppCheckbox(checked = selected, onCheckedChange = { actions.onToggle(row.termId) })
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
