package com.idiomiko.feature.terms.list

import androidx.compose.foundation.lazy.rememberLazyListState
import com.idiomiko.core.ui.components.ScrollColumn
import com.idiomiko.core.ui.components.chosenFor
import com.idiomiko.core.ui.components.shownName
import com.idiomiko.core.ui.components.shownStatuses
import com.idiomiko.core.ui.components.learningLevels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.model.Term
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.ui.audio.SpeakButton
import com.idiomiko.core.ui.components.AppBottomSheet
import com.idiomiko.core.ui.components.AppCheckbox
import com.idiomiko.core.ui.components.AppDivider
import com.idiomiko.core.ui.components.AppFab
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppIcons
import com.idiomiko.core.ui.components.AppMenu
import com.idiomiko.core.ui.components.AppMenuItem
import com.idiomiko.core.ui.components.AppSnackbar
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.AppTriStateCheckbox
import com.idiomiko.core.ui.components.BarSurface
import com.idiomiko.core.ui.components.ButtonSize
import com.idiomiko.core.ui.components.EmptyState
import com.idiomiko.core.ui.components.ListPager
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PhoneSelectionBar
import com.idiomiko.core.ui.components.PickerSheet
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.ScrollList
import com.idiomiko.core.ui.components.SearchField
import com.idiomiko.core.ui.components.SelectButton
import com.idiomiko.core.ui.components.SelectMenu
import com.idiomiko.core.ui.components.SelectionAction
import com.idiomiko.core.ui.components.itemMotion
import com.idiomiko.core.ui.i18n.formatCount
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

/** The days-ago choices the phone's Added filter offers; null leaves that end open. */
private val DAY_CHOICES = listOf(null, 1, 3, 7, 14, 30, 90, 365)
private val SPEAK_BUTTON = 36.dp
private val TICK_BOX = 24.dp

/**
 * The vocabulary on a phone: the overview card under the bar, the search with an add button beside it, a sort
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
    /** Inside another screen, which has its own bar. */
    inside: Boolean = false,
    floatingAction: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    var sorting by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    val selecting = state.selected.isNotEmpty()
    // In pages, as on a wide window; another page starts at its list, the search and the sort above it.
    val list = rememberLazyListState()
    TopOnPageTurn(state.page, list, PHONE_LIST_START)
    Scaffold(
        topBar = {
            if (!inside) AppTopBar(
                title = tr("Vocabulary"),
                onNavigate = onNavigate,
                section = NavSection.TERMS,
                onBack = onBack,
            )
        },
        snackbarHost = {
            state.message?.let { AppSnackbar(it, actionLabel = tr("OK"), onAction = viewModel::dismissMessage) }
        },
        // Adding a term floats at the corner; while selecting, the actions on the chosen terms take the foot.
        floatingActionButton = {
            if (!selecting) {
                if (floatingAction != null) floatingAction() else AppFab(Icons.Default.Add, tr("Add term"), onNew, Modifier.testTag("phone-add-term"))
            }
        },
        bottomBar = {
            if (selecting) {
                // Ticking all the listed terms or some, how many, clearing the choice, and what can be done with them, Anki among it.
                PhoneSelectionBar(
                    state.selected.size,
                    all = state.selected.size >= state.totalCount,
                    onAll = { viewModel.selectAllListed(state.selected.size < state.totalCount) },
                    onClear = { viewModel.selectAllVisible(false) },
                ) {
                    SelectionAction(AppIcons.Flashcards, tr("Practise"), listActions.onPractice, "selection-practise")
                    SelectionAction(Icons.Default.Edit, tr("Edit"), listActions.onBulk, "selection-edit")
                    SelectionAction(AppIcons.Download, tr("Anki"), viewModel::exportAnki, "selection-anki", enabled = state.exporting == null)
                    SelectionAction(Icons.Default.Delete, tr("Delete"), listActions.onDelete, "selection-delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
    ) { padding ->
        ScrollList(
            Modifier.padding(padding).fillMaxSize(),
            state = list,
            // The list's end scrolls clear of the floating button.
            contentPadding = PaddingValues(start = AppSpacing.l, end = AppSpacing.l, top = AppSpacing.m, bottom = FAB_ROOM),
        ) {
            item { PhoneVocabularyOverview(state.totalTerms, known = state.knownCount, learning = state.learningCount, Modifier.padding(bottom = AppSpacing.m)) }
            item {
                SearchField(state.filter.search, { q -> viewModel.updateFilter { it.copy(search = q) } }, tr("Search terms or translations"), Modifier.fillMaxWidth().padding(bottom = AppSpacing.m))
            }
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = AppSpacing.m), horizontalArrangement = Arrangement.spacedBy(AppSpacing.m), verticalAlignment = Alignment.CenterVertically) {
                    val sortLabel = TermSortOption.entries.firstOrNull { it.sort == state.sort }?.label ?: tr("Custom order")
                    SelectButton(sortLabel, onClick = { sorting = true }, modifier = Modifier.weight(1f, fill = false).testTag("phone-sort"), icon = AppIcons.SwapVert)
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
                item { EmptyState(if (state.filter.termIds?.isEmpty() == true) tr("No saved words here yet.") else tr("No terms match the current filters.")) }
            } else {
                itemsIndexed(state.terms, key = { _, term -> term.id }) { _, term -> Column(itemMotion()) {
                    PhoneTermRow(term, state.languageName(term.languageId), selected = term.id in state.selected, rowActions)
                    Spacer(Modifier.height(AppSpacing.s))
                }}
            }
            item { ListPager(state.page, state.pageSize, state.totalCount, viewModel::setPageSize, viewModel::goToPage, compact = true) }
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

/**
 * A term as a card: its checkbox, which alone chooses it, reading it aloud, its text and translation, its status to
 * change and its menu; a tap elsewhere opens it.
 */
@Composable
private fun PhoneTermRow(term: Term, languageName: String, selected: Boolean, actions: RowActions) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(AppShapes.panel)
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
            .border(AppBorders.hairline(if (selected) colors.primary.copy(alpha = AppAlpha.half) else colors.outlineVariant), AppShapes.panel)
            .clickable { actions.onOpen(term) }
            .padding(end = 0.dp, top = AppSpacing.s, bottom = AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppCheckbox(checked = selected, onCheckedChange = { actions.onToggle(term) }, modifier = Modifier.testTag("check-${term.id}"))
        val code = LanguageCodes.codeFor(languageName)
        SpeakButton(term.displayText, code, actions.speaker, Modifier.size(SPEAK_BUTTON))
        Column(Modifier.weight(1f).padding(start = AppSpacing.xs, end = AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
            Text(term.displayText, style = AppText.itemTitle, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            term.translation?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        PhoneStatusTag(term.status) { actions.onStatus(term, it) }
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

/** How wide the empty vocabulary's mascot is drawn. */

/** Room under the list for the floating Add term button and its margin. */
private val FAB_ROOM = 88.dp

/** The search field's place in a phone's list: the overview comes before it. */
private const val PHONE_LIST_START = 1
