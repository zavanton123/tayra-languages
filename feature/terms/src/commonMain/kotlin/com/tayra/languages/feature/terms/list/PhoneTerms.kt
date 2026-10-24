package com.tayra.languages.feature.terms.list

import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.AppSpinner
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import com.tayra.languages.core.ui.components.ScrollColumn
import com.tayra.languages.core.ui.components.chosenFor
import com.tayra.languages.core.ui.components.shownName
import com.tayra.languages.core.ui.components.shownStatuses
import com.tayra.languages.core.ui.components.learningLevels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.audio.SpeakButton
import com.tayra.languages.core.ui.components.AppBottomSheet
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppSnackbar
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.IconButtonSize
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SelectButton
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.itemMotion
import com.tayra.languages.core.ui.i18n.formatCount
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
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import com.tayra.languages.feature.terms.anki.ankiLabel

/** The days-ago choices the phone's Added filter offers; null leaves that end open. */
private val DAY_CHOICES = listOf(null, 1, 3, 7, 14, 30, 90, 365)
private val SPEAK_BUTTON = 36.dp
private val TICK_BOX = 24.dp

/**
 * The vocabulary on a phone: the counts and an add button under the bar, the search, a sort
 * picker and a Filters button that opens a sheet, then the terms as cards. Terms are ticked
 * for the bulk actions by a long press or after Select terms in the list menu.
 */
@Composable
internal fun PhoneTermsScreen(
    state: TermsListUiState,
    viewModel: TermsListViewModel,
    listActions: ListActions,
    rowActions: RowActions,
    onNavigate: (Route) -> Unit,
    onBack: (() -> Unit)?,
    onNew: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var selecting by rememberSaveable { mutableStateOf(false) }
    var sorting by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    val showChecks = selecting || state.selected.isNotEmpty()
    // No pages on a phone: the list loads more as its end comes near.
    val list = rememberLazyListState()
    LaunchedEffect(list) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index to list.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 5) viewModel.showMore() }
    }
    Scaffold(
        topBar = {
            AppTopBar(
                title = tr("Vocabulary"),
                onNavigate = onNavigate,
                section = NavSection.TERMS,
                onBack = onBack,
                actions = { PhoneListMenu(state, listActions, selecting = selecting, onSelecting = { selecting = it; if (!it) viewModel.selectAllVisible(false) }, onExport = viewModel::exportAnki) },
            )
        },
        snackbarHost = {
            state.message?.let { AppSnackbar(it, actionLabel = tr("OK"), onAction = viewModel::dismissMessage) }
        },
    ) { padding ->
        ScrollList(Modifier.padding(padding).fillMaxSize(), state = list, contentPadding = PaddingValues(horizontal = AppSpacing.l, vertical = AppSpacing.m)) {
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = AppSpacing.m), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        // The filter summary below gives how many terms the filters leave; this line keeps to the whole vocabulary.
                        if (state.selected.isNotEmpty()) tr("{0} of {1} selected", state.selected.size, state.totalCount)
                        else tr("{0} terms · {1} known", formatCount(state.totalTerms), formatCount(state.knownCount)),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                    AppIconButton(Icons.Default.Add, tr("Add term"), onNew, Modifier.testTag("phone-add-term"), size = IconButtonSize.Large, style = IconButtonStyle.Filled)
                }
            }
            item { SearchField(state.filter.search, { q -> viewModel.updateFilter { it.copy(search = q) } }, tr("Search terms or translations"), Modifier.fillMaxWidth().padding(bottom = AppSpacing.m)) }
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = AppSpacing.m), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), verticalAlignment = Alignment.CenterVertically) {
                    val sortLabel = TermSortOption.entries.firstOrNull { it.sort == state.sort }?.label ?: tr("Custom order")
                    SelectButton(sortLabel, onClick = { sorting = true }, modifier = Modifier.testTag("phone-sort"), icon = AppIcons.SwapVert)
                    FiltersButton(open = filtering, count = state.activeFilterCount, onClick = { filtering = true })
                }
            }
            state.filter.termIds?.let { ids ->
                item {
                    Text(
                        trPlural(ids.size, "Showing {0} term from the current page.", "Showing {0} terms from the current page."),
                        Modifier.padding(bottom = AppSpacing.m),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (state.activeFilterCount > 0) item { ActiveFilters(state, viewModel, onEdit = { filtering = true }) }
            if (state.loading) {
                item { Text(tr("Loading..."), Modifier.padding(AppSpacing.l), color = colors.onSurfaceVariant) }
            } else if (state.terms.isEmpty()) {
                item { EmptyState(AppIcons.Book, tr("No terms match the current filters.")) }
            } else {
                itemsIndexed(state.terms, key = { _, term -> term.id }) { _, term -> Column(itemMotion()) {
                    PhoneTermRow(term, state.languageName(term.languageId), selected = term.id in state.selected, showCheck = showChecks, rowActions)
                    Spacer(Modifier.height(AppSpacing.s))
                }}
            }
            if (!state.loading && state.terms.size < state.totalCount) {
                item { Box(Modifier.fillMaxWidth().padding(AppSpacing.l), contentAlignment = Alignment.Center) { AppSpinner(size = SpinnerSize.Small) } }
            }
        }
    }

    if (sorting) {
        PickerSheet(
            title = tr("Sort"),
            options = TermSortOption.entries,
            selected = TermSortOption.entries.firstOrNull { it.sort == state.sort },
            label = { it.label },
            onDismiss = { sorting = false },
            onPick = { viewModel.setSort(it.sort); sorting = false },
        )
    }
    if (filtering) PhoneFiltersSheet(state, viewModel, onDismiss = { filtering = false })
}

@Composable
private fun PhoneListMenu(state: TermsListUiState, actions: ListActions, selecting: Boolean, onSelecting: (Boolean) -> Unit, onExport: () -> Unit) {
    val selected = state.selected.size
    OverflowMenu(tr("List actions")) { close ->
        AppMenuItem(text = { Text(if (selecting) tr("Done selecting") else tr("Select terms")) }, onClick = { close(); onSelecting(!selecting) })
        AppMenuItem(text = { Text(if (selected > 0) tr("Bulk edit {0} selected", selected) else tr("Bulk edit selected")) }, enabled = selected > 0, onClick = { close(); actions.onBulk() })
        AppMenuItem(text = { Text(if (selected > 0) tr("Delete {0} selected", selected) else tr("Delete selected")) }, enabled = selected > 0, onClick = { close(); actions.onDelete() })
        AppMenuItem(text = { Text(if (selected > 0) tr("Practise {0} selected", selected) else tr("Practise selected")) }, enabled = selected > 0, onClick = { close(); actions.onPractice() })
        AppMenuItem(text = { Text(ankiLabel(state.exporting, state.sendingToAnki)) }, enabled = state.exporting == null, onClick = { close(); onExport() })
    }
}

/** A term as a card: read aloud, its text and translation, its status to change, when it was added, and its menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneTermRow(term: Term, languageName: String, selected: Boolean, showCheck: Boolean, actions: RowActions) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.panel)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
            .border(AppBorders.hairline(if (selected) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), AppShapes.panel)
            .combinedClickable(onLongClick = { actions.onToggle(term) }) { if (showCheck) actions.onToggle(term) else actions.onOpen(term) }
            .padding(start = if (showCheck) 0.dp else AppSpacing.xs, end = 0.dp, top = AppSpacing.s, bottom = AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheck) AppCheckbox(checked = selected, onCheckedChange = { actions.onToggle(term) })
        val code = LanguageCodes.codeFor(languageName)
        SpeakButton(term.displayText, code, actions.speaker, Modifier.size(SPEAK_BUTTON))
        Column(Modifier.weight(1f).padding(start = AppSpacing.xs, end = AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Text(term.displayText, style = AppText.itemTitle, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            term.translation?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            PhoneStatusTag(term.status) { actions.onStatus(term, it) }
            term.createdAt?.let { Text(addedPhrase(it), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
        RowMenu(term, actions)
    }
}

/** The status's letter and name on its colour; a tap offers the others. */
@Composable
private fun PhoneStatusTag(status: TermStatus, onSelect: (TermStatus) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val tint = statusTint(status)
    Box {
        Row(
            Modifier.clip(AppShapes.compact).background(tint.copy(alpha = AppAlpha.disabled)).clickable(onClickLabel = tr("Status")) { open = true }.padding(start = AppSpacing.xs, end = AppSpacing.s, top = AppSpacing.xs, bottom = AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            StatusBadge(status)
            Text(status.chipName, style = AppText.label, color = MaterialTheme.colorScheme.onSurface)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            shownStatuses(TermStatus.selectable).forEach { option ->
                val chosen = option.chosenFor(status)
                AppMenuItem(text = { Text(option.shownName()) }, onClick = { open = false; onSelect(chosen) })
            }
        }
    }
}

/** "Added today", "Added yesterday", or "Added Jan 5". */
private fun addedPhrase(instant: Instant, now: Instant = Clock.System.now()): String {
    val zone = TimeZone.currentSystemDefault()
    val date = instant.toLocalDateTime(zone).date
    val today = now.toLocalDateTime(zone).date
    return when (date) {
        today -> tr("Added today")
        today.minus(1, DateTimeUnit.DAY) -> tr("Added yesterday")
        else -> tr("Added {0}", addedLabel(instant, now))
    }
}

/**
 * The filters as a sheet: the statuses in two columns, how long ago terms were added, and a
 * button that closes it. Every change applies to the list at once, as on the desktop.
 */
@Composable
private fun PhoneFiltersSheet(state: TermsListUiState, viewModel: TermsListViewModel, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val filter = state.filter
    AppBottomSheet(onDismissRequest = onDismiss) {
        ScrollColumn(Modifier.fillMaxWidth(), contentModifier = Modifier.padding(horizontal = AppSpacing.xl).padding(bottom = AppSpacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Filters"), Modifier.weight(1f), style = AppText.headline)
                QuietButton(tr("Show learning words"), onClick = viewModel::resetFilters)
                AppIconButton(Icons.Default.Close, tr("Close"), onDismiss)
            }
            Text(tr("Learning status"), Modifier.padding(top = AppSpacing.xs, bottom = AppSpacing.m), style = AppText.itemTitle)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                // Without the levels, Learning is one option for all four.
                val group = !learningLevels
                shownStatuses(TermStatus.selectable).chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                        pair.forEach { status ->
                            if (group && status.isLearning) StatusOption(status, selected = state.chosenStatuses.any { it.isLearning }, Modifier.weight(1f)) { viewModel.toggleLearning() }
                            else StatusOption(status, selected = status in state.chosenStatuses, Modifier.weight(1f)) { viewModel.toggleStatus(status) }
                        }
                        // An odd last option keeps its half of the row.
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            AppDivider(Modifier.padding(vertical = AppSpacing.xl))
            Text(tr("Added"), Modifier.padding(bottom = AppSpacing.m), style = AppText.itemTitle)
            AddedPresets(filter.minAgeDays, filter.maxAgeDays, viewModel::setAddedRange)
            filter.termIds?.let { ids ->
                Text(
                    trPlural(ids.size, "Showing {0} term from the current page.", "Showing {0} terms from the current page."),
                    Modifier.padding(top = AppSpacing.m),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(AppSpacing.xl))
            PrimaryButton(trPlural(state.totalCount, "Show {0} term", "Show {0} terms"), onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("phone-show-terms"), size = ButtonSize.Large)
        }
    }
}

/** A status to tick in the sheet: its badge, its name and a box that is ticked when it is on. */
@Composable
private fun StatusOption(status: TermStatus, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint = statusTint(status)
    val strong = tint.darken(0.7f)
    Row(
        modifier.testTag("status-filter-${status.abbreviation}").height(AppSizes.row).clip(AppShapes.panel)
            .background(tint.copy(alpha = if (selected) AppAlpha.disabled else AppAlpha.line))
            .border(if (selected) AppBorders.selected(strong) else AppBorders.hairline(tint), AppShapes.panel)
            .clickable(onClickLabel = if (selected) tr("Stop showing {0}", tr(status.label)) else tr("Show {0}", tr(status.label)), onClick = onClick)
            .padding(horizontal = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.m),
    ) {
        StatusBadge(status)
        Text(status.chipName, Modifier.weight(1f), style = AppText.emphasis, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier.size(TICK_BOX).clip(AppShapes.small).background(if (selected) strong else Color.Transparent).border(AppBorders.selected(strong), AppShapes.small),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = AppAccents.onAccent, modifier = Modifier.size(AppSizes.iconSmall))
        }
    }
}

/** One end of the Added range, picked from a list of days; "Any" leaves it open. */
