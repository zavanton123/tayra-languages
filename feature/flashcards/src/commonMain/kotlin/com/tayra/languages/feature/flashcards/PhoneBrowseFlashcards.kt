package com.tayra.languages.feature.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LearningLanguageSelector
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SelectButton
import com.tayra.languages.core.ui.components.itemMotion
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/** The browser on a phone: the cards as a list, the groups in a scrolling row, sorting from a sheet and the card info in another. */
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
        topBar = { AppTopBar(title = tr("Browse flashcards"), onNavigate = onNavigate, section = NavSection.FLASHCARDS, onBack = onBack, actions = { QuietButton(tr("Review cards"), onClick = actions.onReview, modifier = Modifier.testTag("phone-review")) }) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        val list = rememberLazyListState()
        // Search, groups and the sort row, with the selection's actions when there is a selection, come before the cards.
        FocusRow(state, list, rowsBefore = if (state.selected.isNotEmpty()) 4 else 3, onShown = viewModel::focusShown)
        // The controls keep to the page's margins; the cards run edge to edge between dividers.
        ScrollList(Modifier.padding(padding).fillMaxSize(), state = list, contentPadding = PaddingValues(top = AppSpacing.xs, bottom = AppSpacing.m)) {
            item { CardSearchField(state.search, viewModel::setSearch, Modifier.padding(horizontal = AppSpacing.l).fillMaxWidth().padding(bottom = AppSpacing.l)) }
            item {
                // Unbounded on the right, the chips stay on one line and scroll sideways.
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = AppSpacing.l).padding(bottom = AppSpacing.l)) {
                    ViewChips(state, viewModel::setView, large = true)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(start = AppSpacing.l, end = AppSpacing.s, bottom = AppSpacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                    SelectButton(state.sort.field.label, onClick = { sorting = true }, modifier = Modifier.testTag("phone-sort"), icon = AppIcons.SwapVert)
                    Text(
                        if (state.selected.isEmpty()) trPlural(state.rows.size, "{0} card", "{0} cards") else tr("{0} of {1} selected", state.selected.size, state.rows.size),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                    QuietButton(onClick = { selecting = !showChecks; if (!selecting) viewModel.clearSelection() }, modifier = Modifier.testTag("phone-select")) {
                        Text(if (showChecks) tr("Done") else tr("Select"), style = AppText.itemTitle)
                    }
                }
            }
            if (state.selected.isNotEmpty()) item { Box(Modifier.padding(horizontal = AppSpacing.l)) { CardSelectionBar(state, viewModel, actions) } }
            when {
                state.loading -> item { Text(tr("Loading..."), Modifier.padding(AppSpacing.l), color = colors.onSurfaceVariant) }
                state.rows.isEmpty() -> item { Box(Modifier.padding(horizontal = AppSpacing.l)) { EmptyCards(state) } }
                else -> itemsIndexed(state.rows, key = { _, row -> row.termId }) { index, row -> Column(itemMotion()) {
                    AppDivider()
                    PhoneCardRow(row, state, showCheck = showChecks, actions)
                    if (index == state.rows.lastIndex) AppDivider()
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
    state.detail?.let { DetailSheet(it, state.now, actions, onDismiss = viewModel::closeDetail) }
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
        BrowseSortField.ADDED -> tr("Added")
    }

/** A card as a row of the list: the word and its translation, its state and when it comes. */
@Composable
private fun PhoneCardRow(row: BrowseRow, state: BrowseUiState, showCheck: Boolean, actions: BrowseActions) {
    val colors = MaterialTheme.colorScheme
    val selected = row.termId in state.selected
    val dim = if (row.suspended) colors.outline else colors.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) colors.primary.copy(alpha = AppAlpha.tint) else colors.surface)
            .clickable { if (showCheck) actions.onToggle(row.termId) else actions.onOpen(row.termId) }
            .padding(start = if (showCheck) AppSpacing.s else AppSpacing.xl, end = AppSpacing.s, top = AppSpacing.l, bottom = AppSpacing.l)
            .testTag("browse-row-${row.termId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheck) AppCheckbox(checked = selected, onCheckedChange = { actions.onToggle(row.termId) })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            Text(
                row.word,
                style = AppText.listWord(19).copy(color = if (row.suspended) colors.outline else colors.primary, textDirection = if (row.rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.translation.isNotEmpty()) Text(row.translation, style = MaterialTheme.typography.bodyLarge, color = dim, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                StatePill(row, large = true)
                Text(
                    dueLabel(row, state.now),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (row.isDue && !row.suspended) AppAccents.orange else dim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.lapses > 0) Text(trPlural(row.lapses, "{0} lapse", "{0} lapses"), style = MaterialTheme.typography.bodySmall, color = AppAccents.red)
            }
        }
        RowMenu(row, actions)
    }
}
