package com.tayra.languages.feature.terms.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.TermSortField
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.Dropdown
import com.tayra.languages.core.ui.components.EmptyMessage
import com.tayra.languages.core.ui.components.LoadingIndicator
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.feature.terms.export.saveTextFile
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TermsScreen(
    termIds: List<Long>?,
    onNavigate: (Route) -> Unit,
    onBack: (() -> Unit)?,
    viewModel: TermsListViewModel = koinViewModel(key = "terms-${termIds?.size}") { parametersOf(termIds) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var bulkEdit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    CollectEvents(viewModel.events) { event ->
        if (event is TermsListEvent.ExportReady) scope.launch { saveTextFile("terms", "csv", event.csv) }
    }
    val compact = LocalWindowWidth.current.isCompact

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Terms",
                onNavigate = onNavigate,
                onBack = onBack,
                actions = { ActionsMenu(state, onNew = { onNavigate(Route.NewTerm) }, onBulk = { bulkEdit = true }, onDelete = { confirmDelete = true }, onExport = viewModel::exportCsv) },
            )
        },
        snackbarHost = {
            state.message?.let { Snackbar(action = { TextButton(onClick = viewModel::dismissMessage) { Text("OK") } }) { Text(it) } }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.loading) {
                LoadingIndicator()
                return@Column
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.filter.search,
                    onValueChange = { q -> viewModel.updateFilter { it.copy(search = q) } },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::toggleFilters) { Text(if (state.filtersVisible) "Hide filters" else "Filters") }
            }
            if (state.filtersVisible) FilterPanel(state, viewModel)

            if (state.terms.isEmpty()) {
                EmptyMessage("No terms match the current filters.")
            } else {
                if (!compact) HeaderRow(state, viewModel)
                LazyColumn(Modifier.weight(1f)) {
                    items(state.terms, key = { it.id }) { term ->
                        TermRow(
                            term = term,
                            languageName = state.languageName(term.languageId),
                            selected = term.id in state.selected,
                            compact = compact,
                            onToggle = { viewModel.toggleSelected(term.id) },
                            onOpen = { onNavigate(Route.EditTerm(term.id)) },
                            onStatus = { viewModel.setStatus(term.id, it) },
                        )
                        HorizontalDivider()
                    }
                }
            }
            Pager(state, viewModel)
        }
    }

    if (bulkEdit) {
        BulkEditDialog(count = state.selected.size, onApply = { viewModel.applyBulkUpdate(it); bulkEdit = false }, onDismiss = { bulkEdit = false })
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete ${state.selected.size} term(s)?",
            text = "This cannot be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { viewModel.deleteSelected(); confirmDelete = false },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ActionsMenu(state: TermsListUiState, onNew: () -> Unit, onBulk: () -> Unit, onDelete: () -> Unit, onExport: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Actions") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(text = { Text("Create new") }, onClick = { open = false; onNew() })
        DropdownMenuItem(text = { Text("Bulk edit selected") }, enabled = state.selected.isNotEmpty(), onClick = { open = false; onBulk() })
        DropdownMenuItem(text = { Text("Delete selected") }, enabled = state.selected.isNotEmpty(), onClick = { open = false; onDelete() })
        DropdownMenuItem(text = { Text("Export CSV") }, onClick = { open = false; onExport() })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(state: TermsListUiState, viewModel: TermsListViewModel) {
    val filter = state.filter
    val languageOptions = listOf<Pair<Long?, String>>(null to "(all)") + state.languages.map { it.id to it.name }
    val statuses = TermStatus.entries.filter { it != TermStatus.IGNORED }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Dropdown(
                options = languageOptions,
                selected = languageOptions.firstOrNull { it.first == filter.languageId } ?: languageOptions.first(),
                onSelect = { viewModel.setLanguage(it.first) },
                label = "Language",
                optionLabel = { it.second },
                modifier = Modifier.width(220.dp),
            )
            Dropdown(options = statuses, selected = filter.minStatus, onSelect = { s -> viewModel.updateFilter { it.copy(minStatus = s) } }, label = "Status from", optionLabel = { it.label }, modifier = Modifier.width(180.dp))
            Dropdown(options = statuses, selected = filter.maxStatus, onSelect = { s -> viewModel.updateFilter { it.copy(maxStatus = s) } }, label = "Status to", optionLabel = { it.label }, modifier = Modifier.width(180.dp))
            OutlinedTextField(
                value = filter.minAgeDays?.toString().orEmpty(),
                onValueChange = { v -> viewModel.updateFilter { it.copy(minAgeDays = v.toIntOrNull()) } },
                label = { Text("Age min (days)") },
                singleLine = true,
                modifier = Modifier.width(150.dp),
            )
            OutlinedTextField(
                value = filter.maxAgeDays?.toString().orEmpty(),
                onValueChange = { v -> viewModel.updateFilter { it.copy(maxAgeDays = v.toIntOrNull()) } },
                label = { Text("Age max (days)") },
                singleLine = true,
                modifier = Modifier.width(150.dp),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = filter.parentsOnly, onCheckedChange = { v -> viewModel.updateFilter { it.copy(parentsOnly = v) } })
                Text("Parent terms only")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = filter.includeIgnored, onCheckedChange = { v -> viewModel.updateFilter { it.copy(includeIgnored = v) } })
                Text("Include ignored")
            }
            if (filter.termIds != null) Text("Showing ${filter.termIds!!.size} terms from the current page", Modifier.padding(top = 12.dp))
            TextButton(onClick = viewModel::clearFilters) { Text("Clear all") }
        }
    }
}

@Composable
private fun HeaderRow(state: TermsListUiState, viewModel: TermsListViewModel) {
    val allSelected = state.terms.isNotEmpty() && state.terms.all { it.id in state.selected }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = allSelected, onCheckedChange = viewModel::selectAllVisible)
        SortHeader("Term", TermSortField.TEXT, state, viewModel, Modifier.weight(2f))
        Text("Parents", Modifier.weight(1.2f), style = MaterialTheme.typography.labelLarge)
        Text("Translation", Modifier.weight(2f), style = MaterialTheme.typography.labelLarge)
        SortHeader("Language", TermSortField.LANGUAGE, state, viewModel, Modifier.weight(1f))
        SortHeader("Status", TermSortField.STATUS, state, viewModel, Modifier.width(90.dp))
        SortHeader("Added", TermSortField.CREATED, state, viewModel, Modifier.width(60.dp))
    }
    HorizontalDivider()
}

@Composable
private fun SortHeader(label: String, field: TermSortField, state: TermsListUiState, viewModel: TermsListViewModel, modifier: Modifier) {
    val active = state.sort.field == field
    val arrow = if (!active) "" else if (state.sort.ascending) " ▲" else " ▼"
    Text(
        "$label$arrow",
        modifier.clickable { viewModel.sortBy(field) },
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
    )
}

@Composable
private fun TermRow(
    term: Term,
    languageName: String,
    selected: Boolean,
    compact: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onStatus: (TermStatus) -> Unit,
) {
    val colors = TayraTheme.current.statusColors
    var statusMenu by remember { mutableStateOf(false) }
    val statusChip: @Composable () -> Unit = {
        Text(
            term.status.abbreviation,
            Modifier.clip(RoundedCornerShape(4.dp)).background(colors.background(term.status).takeIf { it != androidx.compose.ui.graphics.Color.Transparent } ?: MaterialTheme.colorScheme.surfaceVariant)
                .clickable { statusMenu = true }.padding(horizontal = 10.dp, vertical = 4.dp),
            color = if (colors.onHighlight != androidx.compose.ui.graphics.Color.Unspecified) colors.onHighlight else androidx.compose.ui.graphics.Color.Black,
            style = MaterialTheme.typography.labelMedium,
        )
        DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
            TermStatus.selectable.forEach { status ->
                DropdownMenuItem(text = { Text(status.label) }, onClick = { statusMenu = false; onStatus(status) })
            }
        }
    }
    if (compact) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(term.displayText, style = MaterialTheme.typography.titleSmall)
                val details = listOfNotNull(
                    term.parents.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.displayText }?.let { "parents: $it" },
                    term.translation?.takeIf { it.isNotBlank() },
                    languageName,
                )
                details.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            statusChip()
        }
    } else {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Text(term.displayText, Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(term.parents.joinToString(", ") { it.displayText }, Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall)
            Text(term.translation.orEmpty(), Modifier.weight(2f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Text(languageName, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Row(Modifier.width(90.dp)) { statusChip() }
            Text(if (term.syncStatus) "↔" else "", Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Pager(state: TermsListUiState, viewModel: TermsListViewModel) {
    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { viewModel.goToPage(state.page - 1) }, enabled = state.page > 0) { Text("Previous") }
        val from = if (state.totalCount == 0) 0 else state.page * state.pageSize + 1
        val to = minOf(state.totalCount, (state.page + 1) * state.pageSize)
        Text("$from to $to of ${state.totalCount}", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { viewModel.goToPage(state.page + 1) }, enabled = state.page < state.pageCount - 1) { Text("Next") }
        if (state.selected.isNotEmpty()) Text("  ${state.selected.size} selected", style = MaterialTheme.typography.bodySmall)
    }
}
