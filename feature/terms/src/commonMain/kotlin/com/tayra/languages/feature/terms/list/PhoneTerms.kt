package com.tayra.languages.feature.terms.list

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.audio.SpeakButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** The days-ago choices the phone's Added filter offers; null leaves that end open. */
private val DAY_CHOICES = listOf(null, 1, 3, 7, 14, 30, 90, 365)

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
            state.message?.let { Snackbar(action = { TextButton(onClick = viewModel::dismissMessage) { Text(tr("OK")) } }) { Text(it) } }
        },
    ) { padding ->
        ScrollList(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (state.selected.isEmpty()) tr("{0} terms · {1} known", formatCount(state.totalTerms), formatCount(state.knownCount))
                        else tr("{0} of {1} selected", state.selected.size, state.totalCount),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                    FilledIconButton(onClick = onNew, modifier = Modifier.size(48.dp).testTag("phone-add-term")) {
                        Icon(Icons.Default.Add, contentDescription = tr("Add term"))
                    }
                }
            }
            item { SearchBox(state.filter.search, { q -> viewModel.updateFilter { it.copy(search = q) } }, Modifier.fillMaxWidth().padding(bottom = 10.dp)) }
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val sortLabel = TermSortOption.entries.firstOrNull { it.sort == state.sort }?.label ?: tr("Custom order")
                    OutlinedButton(
                        onClick = { sorting = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
                        modifier = Modifier.height(44.dp).testTag("phone-sort"),
                    ) {
                        Icon(AppIcons.SwapVert, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(sortLabel, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.onSurfaceVariant)
                    }
                    FiltersButton(open = filtering, count = state.activeFilterCount, onClick = { filtering = true })
                }
            }
            state.filter.termIds?.let { ids ->
                item {
                    Text(
                        trPlural(ids.size, "Showing {0} term from the current page.", "Showing {0} terms from the current page."),
                        Modifier.padding(bottom = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            // The filters that are on show as the badge on the Filters button; the sheet lists them.
            if (state.loading) {
                item { Text(tr("Loading..."), Modifier.padding(16.dp), color = colors.onSurfaceVariant) }
            } else if (state.terms.isEmpty()) {
                item { EmptyState() }
            } else {
                itemsIndexed(state.terms, key = { _, term -> term.id }) { _, term ->
                    PhoneTermRow(term, state.languageName(term.languageId), selected = term.id in state.selected, showCheck = showChecks, rowActions)
                    Spacer(Modifier.height(8.dp))
                }
            }
            item { Pager(state, viewModel, compact = true) }
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
    var open by remember { mutableStateOf(false) }
    val selected = state.selected.size
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = tr("List actions"), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text(if (selecting) tr("Done selecting") else tr("Select terms")) }, onClick = { open = false; onSelecting(!selecting) })
            AppMenuItem(text = { Text(if (selected > 0) tr("Bulk edit {0} selected", selected) else tr("Bulk edit selected")) }, enabled = selected > 0, onClick = { open = false; actions.onBulk() })
            AppMenuItem(text = { Text(if (selected > 0) tr("Delete {0} selected", selected) else tr("Delete selected")) }, enabled = selected > 0, onClick = { open = false; actions.onDelete() })
            AppMenuItem(text = { Text(if (selected > 0) tr("Practise {0} selected", selected) else tr("Practise selected")) }, enabled = selected > 0, onClick = { open = false; actions.onPractice() })
            AppMenuItem(text = { Text(if (state.exporting == null) tr("Export to Anki") else tr("Exporting {0}", state.exporting)) }, enabled = state.exporting == null, onClick = { open = false; onExport() })
        }
    }
}

/** A term as a card: read aloud, its text and translation, its status to change, when it was added, and its menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneTermRow(term: Term, languageName: String, selected: Boolean, showCheck: Boolean, actions: RowActions) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.08f) else colors.surface)
            .border(1.dp, if (selected) colors.primary.copy(alpha = 0.5f) else colors.outlineVariant, RoundedCornerShape(12.dp))
            .combinedClickable(onLongClick = { actions.onToggle(term) }) { if (showCheck) actions.onToggle(term) else actions.onOpen(term) }
            .padding(start = if (showCheck) 0.dp else 4.dp, end = 0.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheck) Checkbox(checked = selected, onCheckedChange = { actions.onToggle(term) })
        val code = LanguageCodes.codeFor(languageName)
        SpeakButton(term.displayText, code, actions.speaker, Modifier.size(36.dp))
        Column(Modifier.weight(1f).padding(start = 4.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(term.displayText, style = MaterialTheme.typography.titleMedium, color = colors.primary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            term.translation?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            Modifier.clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.35f)).clickable(onClickLabel = tr("Status")) { open = true }.padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusBadge(status)
            Text(status.chipName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            TermStatus.selectable.forEach { option ->
                AppMenuItem(text = { Text(tr(option.label)) }, onClick = { open = false; onSelect(option) })
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneFiltersSheet(state: TermsListUiState, viewModel: TermsListViewModel, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val filter = state.filter
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), sheetMaxWidth = Dp.Unspecified, containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Filters"), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = viewModel::resetFilters) { Text(tr("Reset")) }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = tr("Close")) }
            }
            Text(tr("Learning status"), Modifier.padding(top = 4.dp, bottom = 10.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TermStatus.selectable.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { status ->
                            StatusOption(status, selected = status in state.chosenStatuses, Modifier.weight(1f)) { viewModel.toggleStatus(status) }
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 18.dp), color = colors.outlineVariant)
            Text(tr("Added"), Modifier.padding(bottom = 10.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                DaysPicker(tr("From"), filter.minAgeDays, tr("Added from, days ago"), Modifier.weight(1f)) { viewModel.setAddedRange(it, filter.maxAgeDays) }
                DaysPicker(tr("To"), filter.maxAgeDays, tr("Added to, days ago"), Modifier.weight(1f)) { viewModel.setAddedRange(filter.minAgeDays, it) }
                Text(tr("days ago"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            filter.termIds?.let { ids ->
                Text(
                    trPlural(ids.size, "Showing {0} term from the current page.", "Showing {0} terms from the current page."),
                    Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(52.dp).testTag("phone-show-terms")) {
                Text(trPlural(state.totalCount, "Show {0} term", "Show {0} terms"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** A status to tick in the sheet: its badge, its name and a box that is ticked when it is on. */
@Composable
private fun StatusOption(status: TermStatus, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint = statusTint(status)
    val strong = tint.darken(0.7f)
    Row(
        modifier.testTag("status-filter-${status.abbreviation}").height(56.dp).clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = if (selected) 0.45f else 0.2f))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) strong else tint, RoundedCornerShape(12.dp))
            .clickable(onClickLabel = if (selected) tr("Stop showing {0}", tr(status.label)) else tr("Show {0}", tr(status.label)), onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusBadge(status)
        Text(status.chipName, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(if (selected) strong else Color.Transparent).border(1.5.dp, strong, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/** One end of the Added range, picked from a list of days; "Any" leaves it open. */
@Composable
private fun DaysPicker(label: String, value: Int?, description: String, modifier: Modifier, onChange: (Int?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp))
                .clickable { open = true }.semantics { contentDescription = description }.padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(value?.toString() ?: tr("Any"), style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            DAY_CHOICES.forEach { days ->
                AppMenuItem(text = { Text(days?.toString() ?: tr("Any")) }, onClick = { open = false; onChange(days) })
            }
        }
    }
}
