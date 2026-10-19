package com.tayra.languages.feature.terms.list

import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.i18n.tr
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.repository.TermSortField
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIconButton
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppSnackbar
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.ConfirmDialog
import com.tayra.languages.core.ui.components.EmptyState
import com.tayra.languages.core.ui.components.IconButtonStyle
import com.tayra.languages.core.ui.components.IconTile
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.NumberField
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.RemovableChip
import com.tayra.languages.core.ui.components.ScreenHeader
import com.tayra.languages.core.ui.components.SearchField
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.components.SelectMenu
import com.tayra.languages.core.ui.components.TableHeader
import com.tayra.languages.core.ui.components.TableHeaderCell
import com.tayra.languages.core.ui.components.TintedSurface
import com.tayra.languages.core.ui.components.ToggleButton
import com.tayra.languages.core.ui.components.cardSurface
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.state.CollectEvents
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.TayraTheme
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
import com.tayra.languages.core.ui.components.ScrollList

internal enum class TermSortOption(val sort: TermListSort) {
    RECENT(TermListSort(TermSortField.CREATED, ascending = false)),
    OLDEST(TermListSort(TermSortField.CREATED, ascending = true)),
    TEXT(TermListSort(TermSortField.TEXT, ascending = true)),
    STATUS(TermListSort(TermSortField.STATUS, ascending = true));

    val label: String
        get() = when (this) {
            RECENT -> tr("Recently added")
            OLDEST -> tr("Oldest first")
            TEXT -> tr("Term A–Z")
            STATUS -> tr("Status")
        }
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
    var practising by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Term?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is TermsListEvent.AnkiReady -> scope.launch { if (saveBinaryFile(event.export.fileName.removeSuffix(".apkg"), "apkg", event.export.bytes)) viewModel.ankiSaved(event.export) }
        }
    }
    val compact = LocalWindowWidth.current.isCompact
    val listActions = ListActions(onBulk = { bulkEdit = true }, onDelete = { confirmDelete = true }, onPractice = { practising = true })
    val speaker = rememberSpeaker(koinInject(), koinInject(), koinInject<SentenceAudio>())
    val rowActions = RowActions(
        speaker = speaker,
        onOpen = { onNavigate(Route.EditTerm(it.id)) },
        onDelete = { pendingDelete = it },
        onStatus = { term, status -> viewModel.setStatus(term.id, status) },
        onToggle = { viewModel.toggleSelected(it.id) },
    )

    if (compact) {
        PhoneTermsScreen(state, viewModel, listActions, rowActions, onNavigate = onNavigate, onBack = onBack, onNew = { onNavigate(Route.NewTerm) })
    } else Scaffold(
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
            state.message?.let { AppSnackbar(it, actionLabel = tr("OK"), onAction = viewModel::dismissMessage) }
        },
    ) { padding ->
        val gutter = if (compact) 16.dp else 32.dp
        ScrollList(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(horizontal = gutter, vertical = 16.dp)) {
            item { PageHeader(exporting = state.exporting, onExport = viewModel::exportAnki, onSend = if (viewModel.canSendToAnki) ({ viewModel.sendToAnki() }) else null, onNew = { onNavigate(Route.NewTerm) }) }
            item { StatCards(state, compact) }
            item { Toolbar(state, viewModel, compact) }
            if (state.filtersVisible) item { FilterPanel(state, viewModel) }
            if (state.activeFilterCount > 0) item { ActiveFilters(state, viewModel) }
            if (state.loading) {
                item { Text(tr("Loading..."), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (state.terms.isEmpty()) {
                item { EmptyState(AppIcons.Book, tr("No terms match the current filters.")) }
            } else if (compact) {
                itemsIndexed(state.terms, key = { _, term -> term.id }) { _, term ->
                    CompactTermRow(term, state.languageName(term.languageId), term.id in state.selected, rowActions)
                    AppDivider()
                }
            } else {
                item { TermsTableHeader(state, viewModel, listActions) }
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
    if (practising) {
        PracticeKindsDialog(count = state.selected.size, onStart = { kinds -> practising = false; onNavigate(Route.PracticeWords(state.selected.toList(), kinds.map { it.name })) }, onDismiss = { practising = false })
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = trPlural(state.selected.size, "Delete {0} term?", "Delete {0} terms?"),
            text = tr("This cannot be undone."),
            confirmLabel = tr("Delete"),
            destructive = true,
            onConfirm = { viewModel.deleteSelected(); confirmDelete = false },
            onDismiss = { confirmDelete = false },
        )
    }
    pendingDelete?.let { term ->
        ConfirmDialog(
            title = tr("Delete \"{0}\"?", term.displayText),
            text = tr("This cannot be undone."),
            confirmLabel = tr("Delete"),
            destructive = true,
            onConfirm = { viewModel.delete(term.id); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
}

internal class ListActions(val onBulk: () -> Unit, val onDelete: () -> Unit, val onPractice: () -> Unit)

internal class RowActions(
    /** Reads a word or its example aloud. */
    val speaker: Speaker,
    val onOpen: (Term) -> Unit,
    val onDelete: (Term) -> Unit,
    val onStatus: (Term, TermStatus) -> Unit,
    val onToggle: (Term) -> Unit,
)

@Composable
private fun ListMenu(state: TermsListUiState, actions: ListActions) {
    val selected = state.selected.size
    OverflowMenu(tr("List actions")) { close ->
        AppMenuItem(text = { Text(if (selected > 0) tr("Bulk edit {0} selected", selected) else tr("Bulk edit selected")) }, enabled = selected > 0, onClick = { close(); actions.onBulk() })
        AppMenuItem(text = { Text(if (selected > 0) tr("Delete {0} selected", selected) else tr("Delete selected")) }, enabled = selected > 0, onClick = { close(); actions.onDelete() })
        AppMenuItem(text = { Text(if (selected > 0) tr("Practise {0} selected", selected) else tr("Practise selected")) }, enabled = selected > 0, onClick = { close(); actions.onPractice() })
    }
}

@Composable
private fun PageHeader(exporting: String?, onExport: () -> Unit, onSend: (() -> Unit)?, onNew: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        ScreenHeader(tr("Vocabulary"), tr("Review and manage your vocabulary.")) {
            SecondaryButton(if (exporting == null) tr("Export to Anki") else tr("Exporting {0}", exporting), onClick = onExport, icon = AppIcons.Download, enabled = exporting == null)
            if (onSend != null) {
                // Straight into the Anki that is open, through AnkiConnect, rather than a file to import.
                SecondaryButton(tr("Send to Anki"), onClick = onSend, modifier = Modifier.testTag("send-to-anki"), icon = AppIcons.UploadFile, enabled = exporting == null)
            }
            PrimaryButton(tr("Add term"), onClick = onNew, icon = Icons.Default.Add)
        }
    }
}

private data class StatCard(val label: String, val value: Int, val icon: ImageVector, val tint: Color)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatCards(state: TermsListUiState, compact: Boolean) {
    val cards = listOf(
        StatCard(tr("Total terms"), state.totalTerms, AppIcons.Book, AppAccents.blue),
        StatCard(tr("Learning"), state.learningCount, AppIcons.BarChart, AppAccents.purple),
        StatCard(tr("Known"), state.knownCount, Icons.Default.Check, AppAccents.green),
    )
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        maxItemsInEachRow = if (compact) 2 else cards.size,
    ) {
        cards.forEach { card ->
            TintedSurface(card.tint, Modifier.weight(1f), shape = AppShapes.card, padding = PaddingValues(horizontal = 16.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(card.icon, size = 48, tint = card.tint)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(card.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(card.value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
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
    val sortLabel = TermSortOption.entries.firstOrNull { it.sort == state.sort }?.label ?: tr("Custom order")
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        SearchField(filter.search, { q -> viewModel.updateFilter { it.copy(search = q) } }, tr("Search terms or translations"), if (compact) Modifier.fillMaxWidth() else Modifier.width(560.dp))
        SelectMenu(
            options = TermSortOption.entries,
            selected = TermSortOption.entries.firstOrNull { it.sort == state.sort },
            onSelect = { viewModel.setSort(it.sort) },
            optionLabel = { it.label },
            value = tr("Sort: {0}", sortLabel),
            icon = AppIcons.SwapVert,
        )
        FiltersButton(open = state.filtersVisible, count = state.activeFilterCount, onClick = viewModel::toggleFilters)
        if (!compact) {
            Spacer(Modifier.weight(1f))
            Text(
                if (state.selected.isEmpty()) trPlural(state.totalCount, "{0} term", "{0} terms") else tr("{0} of {1} selected", state.selected.size, state.totalCount),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** Opens and closes the filter panel; the badge counts the kinds of filter that are on. */
@Composable
internal fun FiltersButton(open: Boolean, count: Int, onClick: () -> Unit) {
    ToggleButton(
        tr("Filters"),
        active = open || count > 0,
        onClick = onClick,
        icon = AppIcons.Tune,
        badge = count,
        contentDescription = if (count > 0) trPlural(count, "{0} filter on", "{0} filters on") else null,
    )
}

/** "New", "Learning", "Known" or "Ignored": the status's name without its number, which the badge shows. */
internal val TermStatus.chipName: String get() = tr(label).substringBefore(" (")

/** The status's highlight colour, or a neutral one where the theme has none. */
@Composable
internal fun statusTint(status: TermStatus): Color =
    TayraTheme.current.statusColors.background(status).takeIf { it != Color.Transparent } ?: MaterialTheme.colorScheme.surfaceVariant

/** The filters, applied as soon as they change: the statuses to show and how long ago terms were added. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(state: TermsListUiState, viewModel: TermsListViewModel) {
    val colors = MaterialTheme.colorScheme
    val filter = state.filter
    Column(
        Modifier.fillMaxWidth().padding(bottom = 14.dp).cardSurface(AppShapes.card).padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Filter vocabulary"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            QuietButton(tr("Reset all"), onClick = viewModel::resetFilters)
        }
        Text(tr("Learning status"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TermStatus.selectable.forEach { status ->
                StatusFilterChip(status, selected = status in state.chosenStatuses) { viewModel.toggleStatus(status) }
            }
        }
        AppDivider(Modifier.padding(vertical = 16.dp))
        Text(tr("Added"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Text(tr("From"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            NumberField(filter.minAgeDays, { viewModel.setAddedRange(it, filter.maxAgeDays) }, tr("Added from, days ago"), placeholder = tr("Any"), increaseDescription = tr("More days"), decreaseDescription = tr("Fewer days"))
            Text(tr("To"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            NumberField(filter.maxAgeDays, { viewModel.setAddedRange(filter.minAgeDays, it) }, tr("Added to, days ago"), placeholder = tr("Any"), increaseDescription = tr("More days"), decreaseDescription = tr("Fewer days"))
            Text(tr("days ago"), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        if (filter.termIds != null) {
            Text(
                trPlural(filter.termIds!!.size, "Showing {0} term from the current page.", "Showing {0} terms from the current page."),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** A status to tick: its colour, a mark when ticked, its name and its badge. */
@Composable
private fun StatusFilterChip(status: TermStatus, selected: Boolean, onClick: () -> Unit) {
    val tint = statusTint(status)
    val strong = tint.darken(0.7f)
    Row(
        Modifier.testTag("status-filter-${status.abbreviation}").height(44.dp).clip(AppShapes.control)
            .background(tint.copy(alpha = if (selected) AppAlpha.disabled else AppAlpha.line))
            .border(if (selected) AppBorders.selected(strong) else AppBorders.hairline(tint), AppShapes.control)
            .clickable(onClickLabel = if (selected) tr("Stop showing {0}", tr(status.label)) else tr("Show {0}", tr(status.label)), onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (selected) {
            Box(Modifier.size(20.dp).clip(AppShapes.circle).background(strong), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(AppSizes.iconTiny))
            }
        } else {
            Box(Modifier.size(20.dp).clip(AppShapes.circle).border(AppBorders.selected(strong), AppShapes.circle))
        }
        Text(status.chipName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        StatusBadge(status)
    }
}

/** The status's number or letter on its colour. */
@Composable
internal fun StatusBadge(status: TermStatus) {
    Text(
        status.abbreviation,
        Modifier.clip(AppShapes.small).background(statusTint(status).copy(alpha = AppAlpha.strong)).padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = AppAccents.ink,
    )
}

/** The filters that are on, each removable on its own, under the toolbar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActiveFilters(state: TermsListUiState, viewModel: TermsListViewModel) {
    val filter = state.filter
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        TermStatus.selectable.filter { it in state.chosenStatuses }.forEach { status ->
            val tint = statusTint(status)
            RemovableChip(onRemove = { viewModel.toggleStatus(status) }, removeDescription = tr("Remove {0}", tr(status.label)), tint = tint, background = tint.copy(alpha = AppAlpha.line)) {
                Text(status.chipName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                StatusBadge(status)
            }
        }
        if (state.ageFiltered) {
            val from = filter.minAgeDays
            val to = filter.maxAgeDays
            val label = when {
                from != null && to != null -> trPlural(to, "Added: {1}–{0} day", "Added: {1}–{0} days", from)
                to != null -> trPlural(to, "Added: up to {0} day ago", "Added: up to {0} days ago")
                else -> trPlural(from ?: 0, "Added: {0}+ day ago", "Added: {0}+ days ago")
            }
            RemovableChip(onRemove = { viewModel.setAddedRange(null, null) }, removeDescription = tr("Remove the added filter")) {
                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }
        QuietButton(tr("Clear all"), onClick = viewModel::clearFilters)
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
private fun TermsTableHeader(state: TermsListUiState, viewModel: TermsListViewModel, actions: ListActions) {
    val allSelected = state.terms.isNotEmpty() && state.terms.all { it.id in state.selected }
    TableHeader {
        AppCheckbox(checked = allSelected, onCheckedChange = viewModel::selectAllVisible)
        TableHeaderCell(tr("Term"), Modifier.weight(TERM_WEIGHT), AppIcons.UnfoldMore, active = state.sort.field == TermSortField.TEXT) { viewModel.sortBy(TermSortField.TEXT) }
        TableHeaderCell(tr("Translation"), Modifier.weight(TRANSLATION_WEIGHT))
        TableHeaderCell(tr("Example"), Modifier.weight(EXAMPLE_WEIGHT))
        TableHeaderCell(tr("Status"), Modifier.width(STATUS_WIDTH), Icons.Default.Info, active = state.sort.field == TermSortField.STATUS) { viewModel.sortBy(TermSortField.STATUS) }
        TableHeaderCell(tr("Added"), Modifier.width(ADDED_WIDTH), AppIcons.UnfoldMore, active = state.sort.field == TermSortField.CREATED) { viewModel.sortBy(TermSortField.CREATED) }
        Box(Modifier.width(MENU_WIDTH), contentAlignment = Alignment.Center) { ListMenu(state, actions) }
    }
}

@Composable
private fun TermTableRow(term: Term, languageName: String, selected: Boolean, index: Int, actions: RowActions) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background = when {
        selected || hovered -> colors.primary.copy(alpha = AppAlpha.tint)
        index % 2 == 1 -> colors.surfaceVariant.copy(alpha = AppAlpha.line)
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().background(background).hoverable(interaction).clickable { actions.onOpen(term) }.padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppCheckbox(checked = selected, onCheckedChange = { actions.onToggle(term) })
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
        Modifier.fillMaxWidth().background(if (selected) colors.primary.copy(alpha = AppAlpha.tint) else Color.Transparent).clickable { actions.onOpen(term) }.padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppCheckbox(checked = selected, onCheckedChange = { actions.onToggle(term) })
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
            Modifier.clip(AppShapes.small).background(background).border(AppBorders.hairline(background.darken()), AppShapes.small)
                .clickable { open = true }.padding(horizontal = 10.dp, vertical = 4.dp),
            color = if (colors.onHighlight != Color.Unspecified) colors.onHighlight else AppAccents.ink,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            TermStatus.selectable.forEach { option ->
                AppMenuItem(text = { Text(tr(option.label)) }, onClick = { open = false; onSelect(option) })
            }
        }
    }
}

internal fun Color.darken(factor: Float = 0.85f): Color = Color(red * factor, green * factor, blue * factor, alpha)

@Composable
internal fun RowMenu(term: Term, actions: RowActions) {
    OverflowMenu(tr("Term actions")) { close ->
        AppMenuItem(text = { Text(tr("Edit")) }, onClick = { close(); actions.onOpen(term) })
        AppMenuItem(text = { Text(tr("Delete")) }, onClick = { close(); actions.onDelete(term) })
    }
}

@Composable
internal fun Pager(state: TermsListUiState, viewModel: TermsListViewModel, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val from = if (state.totalCount == 0) 0 else state.page * state.pageSize + 1
    val to = minOf(state.totalCount, (state.page + 1) * state.pageSize)
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        SelectMenu(PAGE_SIZES, state.pageSize, viewModel::setPageSize, { tr("{0} per page", it) })
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val pagerTint = { enabled: Boolean -> if (enabled) colors.primary else colors.outlineVariant }
            AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, tr("Previous page"), { viewModel.goToPage(state.page - 1) }, enabled = state.page > 0, style = IconButtonStyle.Outlined, tint = pagerTint(state.page > 0))
            Text(tr("{0}–{1} of {2}", from, to, state.totalCount), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            AppIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, tr("Next page"), { viewModel.goToPage(state.page + 1) }, enabled = state.page < state.pageCount - 1, style = IconButtonStyle.Outlined, tint = pagerTint(state.page < state.pageCount - 1))
        }
        if (!compact) Spacer(Modifier.weight(1f))
    }
}

/** "Today", "Yesterday", or a short date. */
internal fun addedLabel(instant: Instant, now: Instant = Clock.System.now()): String {
    val zone = TimeZone.currentSystemDefault()
    val date = instant.toLocalDateTime(zone).date
    val today = now.toLocalDateTime(zone).date
    return when (date) {
        today -> tr("Today")
        today.minus(1, DateTimeUnit.DAY) -> tr("Yesterday")
        else -> if (date.year == today.year) shortDate(date.month.ordinal, date.day) else shortDate(date.month.ordinal, date.day, date.year)
    }
}

/** "Jan 5", or "Jan 5, 2024" with a [year]. */
internal fun shortDate(month: Int, day: Int, year: Int? = null): String = if (year == null) {
    when (month) {
        0 -> tr("Jan {0}", day)
        1 -> tr("Feb {0}", day)
        2 -> tr("Mar {0}", day)
        3 -> tr("Apr {0}", day)
        4 -> tr("May {0}", day)
        5 -> tr("Jun {0}", day)
        6 -> tr("Jul {0}", day)
        7 -> tr("Aug {0}", day)
        8 -> tr("Sep {0}", day)
        9 -> tr("Oct {0}", day)
        10 -> tr("Nov {0}", day)
        else -> tr("Dec {0}", day)
    }
} else {
    when (month) {
        0 -> tr("Jan {0}, {1}", day, year)
        1 -> tr("Feb {0}, {1}", day, year)
        2 -> tr("Mar {0}, {1}", day, year)
        3 -> tr("Apr {0}, {1}", day, year)
        4 -> tr("May {0}, {1}", day, year)
        5 -> tr("Jun {0}, {1}", day, year)
        6 -> tr("Jul {0}, {1}", day, year)
        7 -> tr("Aug {0}, {1}", day, year)
        8 -> tr("Sep {0}, {1}", day, year)
        9 -> tr("Oct {0}, {1}", day, year)
        10 -> tr("Nov {0}, {1}", day, year)
        else -> tr("Dec {0}, {1}", day, year)
    }
}
