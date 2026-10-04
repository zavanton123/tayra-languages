package com.tayra.languages.feature.terms.list

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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.repository.TermSortField
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.feature.terms.export.saveTextFile
import com.tayra.languages.core.ui.files.saveBinaryFile
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.ui.audio.Speaker
import com.tayra.languages.core.ui.audio.SpeakButton
import com.tayra.languages.core.ui.audio.rememberSpeaker
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock
import kotlin.time.Instant

private enum class TermSortOption(val label: String, val sort: TermListSort) {
    RECENT("Recently added", TermListSort(TermSortField.CREATED, ascending = false)),
    OLDEST("Oldest first", TermListSort(TermSortField.CREATED, ascending = true)),
    TEXT("Term A–Z", TermListSort(TermSortField.TEXT, ascending = true)),
    STATUS("Status", TermListSort(TermSortField.STATUS, ascending = true)),
}

private val PAGE_SIZES = listOf(25, 50, 100)

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
    var pendingDelete by remember { mutableStateOf<Term?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is TermsListEvent.ExportReady -> scope.launch { saveTextFile("terms", "csv", event.csv) }
            is TermsListEvent.AnkiReady -> scope.launch { if (saveBinaryFile(event.export.fileName.removeSuffix(".apkg"), "apkg", event.export.bytes)) viewModel.ankiSaved(event.export) }
        }
    }
    val compact = LocalWindowWidth.current.isCompact
    val listActions = ListActions(onBulk = { bulkEdit = true }, onDelete = { confirmDelete = true }, onExport = viewModel::exportCsv)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Tayra Languages",
                onNavigate = onNavigate,
                section = NavSection.TERMS,
                onBack = onBack,
                actions = { if (compact) ListMenu(state, listActions) },
            )
        },
        snackbarHost = {
            state.message?.let { Snackbar(action = { TextButton(onClick = viewModel::dismissMessage) { Text("OK") } }) { Text(it) } }
        },
    ) { padding ->
        val speaker = rememberSpeaker(koinInject(), koinInject(), koinInject<SentenceAudio>())
        val gutter = if (compact) 16.dp else 32.dp
        val rowActions = RowActions(
            speaker = speaker,
            onOpen = { onNavigate(Route.EditTerm(it.id)) },
            onDelete = { pendingDelete = it },
            onStatus = { term, status -> viewModel.setStatus(term.id, status) },
            onToggle = { viewModel.toggleSelected(it.id) },
        )
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(horizontal = gutter, vertical = 16.dp)) {
            item { PageHeader(compact, exporting = state.exporting, onExport = viewModel::exportAnki, onNew = { onNavigate(Route.NewTerm) }) }
            item { StatCards(state, compact) }
            item { Toolbar(state, viewModel, compact) }
            if (state.filtersVisible) item { FilterPanel(state, viewModel) }
            if (state.loading) {
                item { Text("Loading...", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (state.terms.isEmpty()) {
                item { EmptyState() }
            } else if (compact) {
                itemsIndexed(state.terms, key = { _, term -> term.id }) { _, term ->
                    CompactTermRow(term, state.languageName(term.languageId), term.id in state.selected, rowActions)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            } else {
                item { TableHeader(state, viewModel, listActions) }
                itemsIndexed(state.terms, key = { _, term -> term.id }) { index, term ->
                    TermTableRow(term, state.languageName(term.languageId), term.id in state.selected, index, rowActions)
                }
            }
            item { Pager(state, viewModel, compact) }
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
    pendingDelete?.let { term ->
        ConfirmDialog(
            title = "Delete \"${term.displayText}\"?",
            text = "This cannot be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { viewModel.delete(term.id); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
}

private class ListActions(val onBulk: () -> Unit, val onDelete: () -> Unit, val onExport: () -> Unit)

private class RowActions(
    /** Reads a word or its example aloud. */
    val speaker: Speaker,
    val onOpen: (Term) -> Unit,
    val onDelete: (Term) -> Unit,
    val onStatus: (Term, TermStatus) -> Unit,
    val onToggle: (Term) -> Unit,
)

@Composable
private fun ListMenu(state: TermsListUiState, actions: ListActions) {
    var open by remember { mutableStateOf(false) }
    val selected = state.selected.size
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = "List actions", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text(if (selected > 0) "Bulk edit $selected selected" else "Bulk edit selected") }, enabled = selected > 0, onClick = { open = false; actions.onBulk() })
            AppMenuItem(text = { Text(if (selected > 0) "Delete $selected selected" else "Delete selected") }, enabled = selected > 0, onClick = { open = false; actions.onDelete() })
            AppMenuItem(text = { Text("Export CSV") }, onClick = { open = false; actions.onExport() })
        }
    }
}

@Composable
private fun PageHeader(compact: Boolean, exporting: String?, onExport: () -> Unit, onNew: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Vocabulary", style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Review and manage your vocabulary.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onExport, enabled = exporting == null, shape = RoundedCornerShape(10.dp)) {
            Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (exporting == null) "Export to Anki" else "Exporting $exporting")
        }
        Button(onClick = onNew, shape = RoundedCornerShape(10.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add term")
        }
    }
}

private data class StatCard(val label: String, val value: Int, val icon: ImageVector, val tint: Color)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatCards(state: TermsListUiState, compact: Boolean) {
    val cards = listOf(
        StatCard("Total terms", state.totalTerms, AppIcons.Book, Color(0xFF3B6FE0)),
        StatCard("Learning", state.learningCount, AppIcons.BarChart, Color(0xFF7C4DDB)),
        StatCard("Known", state.knownCount, Icons.Default.Check, Color(0xFF1FA463)),
    )
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        maxItemsInEachRow = if (compact) 2 else cards.size,
    ) {
        cards.forEach { card ->
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(card.tint.copy(alpha = 0.06f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(card.tint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(card.icon, contentDescription = null, tint = card.tint, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(card.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(card.value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Toolbar(state: TermsListUiState, viewModel: TermsListViewModel, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val filter = state.filter
    val statusOptions = listOf<TermStatus?>(null) + TermStatus.selectable
    val statusLabel = state.statusChoice?.label ?: "All statuses"
    val sortLabel = TermSortOption.entries.firstOrNull { it.sort == state.sort }?.label ?: "Custom order"
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        SearchBox(filter.search, { q -> viewModel.updateFilter { it.copy(search = q) } }, if (compact) Modifier.fillMaxWidth() else Modifier.width(300.dp))
        FilterMenu(AppIcons.BarChart, statusLabel, statusOptions, { it?.label ?: "All statuses" }, viewModel::setStatusChoice)
        FilterMenu(AppIcons.SwapVert, sortLabel, TermSortOption.entries, { it.label }) { viewModel.setSort(it.sort) }
        val active = state.filtersVisible
        OutlinedButton(
            onClick = viewModel::toggleFilters,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (active) colors.primary.copy(alpha = 0.1f) else Color.Transparent,
                contentColor = if (active) colors.primary else colors.onSurface,
            ),
        ) {
            Icon(AppIcons.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Filters", style = MaterialTheme.typography.bodyMedium)
        }
        if (!compact) {
            Spacer(Modifier.weight(1f))
            Text(
                if (state.selected.isEmpty()) "${state.totalCount} term${if (state.totalCount == 1) "" else "s"}" else "${state.selected.size} of ${state.totalCount} selected",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier) {
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
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text("Search terms or translations", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun <T> FilterMenu(icon: ImageVector, label: String, options: List<T>, optionLabel: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { open = true },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                AppMenuItem(text = { Text(optionLabel(option)) }, onClick = { open = false; onSelect(option) })
            }
        }
    }
}

/** Less common filters, shown on demand. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(state: TermsListUiState, viewModel: TermsListViewModel) {
    val colors = MaterialTheme.colorScheme
    val filter = state.filter
    Column(
        Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceVariant.copy(alpha = 0.4f)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = filter.minAgeDays?.toString().orEmpty(),
                onValueChange = { v -> viewModel.updateFilter { it.copy(minAgeDays = v.toIntOrNull()) } },
                label = { Text("Age min (days)") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(150.dp),
            )
            OutlinedTextField(
                value = filter.maxAgeDays?.toString().orEmpty(),
                onValueChange = { v -> viewModel.updateFilter { it.copy(maxAgeDays = v.toIntOrNull()) } },
                label = { Text("Age max (days)") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(150.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = filter.parentsOnly, onCheckedChange = { v -> viewModel.updateFilter { it.copy(parentsOnly = v) } })
                Text("Parent terms only")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = filter.includeIgnored, onCheckedChange = { v -> viewModel.updateFilter { it.copy(includeIgnored = v) } })
                Text("Include ignored")
            }
            TextButton(onClick = viewModel::clearFilters) { Text("Clear all") }
        }
        if (filter.termIds != null) {
            Text("Showing ${filter.termIds!!.size} terms from the current page.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

// Column weights shared by the table header and rows.
private const val TERM_WEIGHT = 1.6f
private const val TRANSLATION_WEIGHT = 2.4f
private const val EXAMPLE_WEIGHT = 3f
private val STATUS_WIDTH = 90.dp
private val ADDED_WIDTH = 110.dp
private val MENU_WIDTH = 48.dp

@Composable
private fun TableHeader(state: TermsListUiState, viewModel: TermsListViewModel, actions: ListActions) {
    val colors = MaterialTheme.colorScheme
    val allSelected = state.terms.isNotEmpty() && state.terms.all { it.id in state.selected }
    CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant, LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)).background(colors.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = allSelected, onCheckedChange = viewModel::selectAllVisible)
            HeaderCell("Term", Modifier.weight(TERM_WEIGHT), AppIcons.UnfoldMore, active = state.sort.field == TermSortField.TEXT) { viewModel.sortBy(TermSortField.TEXT) }
            HeaderCell("Translation", Modifier.weight(TRANSLATION_WEIGHT))
            HeaderCell("Example", Modifier.weight(EXAMPLE_WEIGHT))
            HeaderCell("Status", Modifier.width(STATUS_WIDTH), Icons.Default.Info, active = state.sort.field == TermSortField.STATUS) { viewModel.sortBy(TermSortField.STATUS) }
            HeaderCell("Added", Modifier.width(ADDED_WIDTH), AppIcons.UnfoldMore, active = state.sort.field == TermSortField.CREATED) { viewModel.sortBy(TermSortField.CREATED) }
            Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { ListMenu(state, actions) }
        }
    }
}

@Composable
private fun HeaderCell(label: String, modifier: Modifier, icon: ImageVector? = null, active: Boolean = false, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.then(if (onClick != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick) else Modifier).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = if (active) colors.primary else LocalContentColor.current, fontWeight = FontWeight.Medium)
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (active) colors.primary else LocalContentColor.current)
    }
}

@Composable
private fun TermTableRow(term: Term, languageName: String, selected: Boolean, index: Int, actions: RowActions) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background = when {
        selected || hovered -> colors.primary.copy(alpha = 0.08f)
        index % 2 == 1 -> colors.surfaceVariant.copy(alpha = 0.25f)
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().background(background).hoverable(interaction).clickable { actions.onOpen(term) }.padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { actions.onToggle(term) })
        val code = LanguageCodes.codeFor(languageName)
        Row(Modifier.weight(TERM_WEIGHT).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            SpeakButton(term.displayText, code, actions.speaker, Modifier.size(32.dp))
            Text(term.displayText, style = MaterialTheme.typography.bodyLarge, color = colors.primary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(term.translation.orEmpty(), Modifier.weight(TRANSLATION_WEIGHT).padding(end = 12.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.weight(EXAMPLE_WEIGHT).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            term.sentence?.let { SpeakButton(it, code, actions.speaker, Modifier.size(32.dp)) }
            Text(term.sentence.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.width(STATUS_WIDTH)) { StatusChip(term.status) { actions.onStatus(term, it) } }
        Text(term.createdAt?.let(::addedLabel).orEmpty(), Modifier.width(ADDED_WIDTH), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { RowMenu(term, actions) }
    }
}

@Composable
private fun CompactTermRow(term: Term, languageName: String, selected: Boolean, actions: RowActions) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background(if (selected) colors.primary.copy(alpha = 0.08f) else Color.Transparent).clickable { actions.onOpen(term) }.padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { actions.onToggle(term) })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val code = LanguageCodes.codeFor(languageName)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(term.displayText, style = MaterialTheme.typography.bodyLarge, color = colors.primary, fontWeight = FontWeight.Medium)
                SpeakButton(term.displayText, code, actions.speaker, Modifier.size(32.dp))
            }
            term.translation?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            term.sentence?.let { sentence ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sentence, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    SpeakButton(sentence, code, actions.speaker, Modifier.size(32.dp))
                }
            }
            term.createdAt?.let { Text(addedLabel(it), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
        StatusChip(term.status) { actions.onStatus(term, it) }
        RowMenu(term, actions)
    }
}

/** A small coloured disc with the language code, standing in for a flag. */
@Composable
private fun StatusChip(status: TermStatus, onSelect: (TermStatus) -> Unit) {
    val colors = TayraTheme.current.statusColors
    var open by remember { mutableStateOf(false) }
    val background = colors.background(status).takeIf { it != Color.Transparent } ?: MaterialTheme.colorScheme.surfaceVariant
    Box {
        Text(
            status.abbreviation,
            Modifier.clip(RoundedCornerShape(6.dp)).background(background).border(1.dp, background.darken(), RoundedCornerShape(6.dp))
                .clickable { open = true }.padding(horizontal = 10.dp, vertical = 4.dp),
            color = if (colors.onHighlight != Color.Unspecified) colors.onHighlight else Color(0xFF1B1F24),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            TermStatus.selectable.forEach { option ->
                AppMenuItem(text = { Text(option.label) }, onClick = { open = false; onSelect(option) })
            }
        }
    }
}

private fun Color.darken(): Color = Color(red * 0.85f, green * 0.85f, blue * 0.85f, alpha)

@Composable
private fun RowMenu(term: Term, actions: RowActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Term actions", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text("Edit") }, onClick = { open = false; actions.onOpen(term) })
            AppMenuItem(text = { Text("Delete") }, onClick = { open = false; actions.onDelete(term) })
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(AppIcons.Book, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        Text("No terms match the current filters.", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Pager(state: TermsListUiState, viewModel: TermsListViewModel, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val from = if (state.totalCount == 0) 0 else state.page * state.pageSize + 1
    val to = minOf(state.totalCount, (state.page + 1) * state.pageSize)
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        var open by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { open = true },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
            ) {
                Text("${state.pageSize} per page", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            AppMenu(expanded = open, onDismissRequest = { open = false }) {
                PAGE_SIZES.forEach { size -> AppMenuItem(text = { Text("$size per page") }, onClick = { open = false; viewModel.setPageSize(size) }) }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PagerButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous page", enabled = state.page > 0) { viewModel.goToPage(state.page - 1) }
            Text("$from–$to of ${state.totalCount}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            PagerButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next page", enabled = state.page < state.pageCount - 1) { viewModel.goToPage(state.page + 1) }
        }
        if (!compact) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PagerButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).clickable(enabled = enabled, onClick = onClick).padding(8.dp),
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) colors.primary else colors.outlineVariant, modifier = Modifier.size(20.dp))
    }
}

private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "Today", "Yesterday", or a short date. */
private fun addedLabel(instant: Instant, now: Instant = Clock.System.now()): String {
    val zone = TimeZone.currentSystemDefault()
    val date = instant.toLocalDateTime(zone).date
    val today = now.toLocalDateTime(zone).date
    return when (date) {
        today -> "Today"
        today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
        else -> {
            val base = "${monthNames[date.month.ordinal]} ${date.day}"
            if (date.year == today.year) base else "$base, ${date.year}"
        }
    }
}
