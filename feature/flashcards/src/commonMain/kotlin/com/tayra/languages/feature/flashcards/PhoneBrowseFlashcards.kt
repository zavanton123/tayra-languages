package com.tayra.languages.feature.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.tayra.languages.core.ui.components.LearningLanguageSelector
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayra.languages.core.ui.components.AppCheckbox
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.OverflowMenu
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.components.SecondaryButton
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.AppAccents

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
        topBar = { AppTopBar(title = tr("Browse flashcards"), onNavigate = onNavigate, section = NavSection.FLASHCARDS, onBack = onBack, actions = { BrowseMenu(onReview = actions.onReview) }) },
        containerColor = colors.surfaceContainerLow,
    ) { padding ->
        val list = rememberLazyListState()
        // Search, groups and the sort row, with the selection's actions when there is a selection, come before the cards.
        FocusRow(state, list, rowsBefore = if (state.selected.isNotEmpty()) 4 else 3, onShown = viewModel::focusShown)
        // The controls keep to the page's margins; the cards run edge to edge between dividers.
        ScrollList(Modifier.padding(padding).fillMaxSize(), state = list, contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)) {
            item { CardSearchField(state.search, viewModel::setSearch, Modifier.padding(horizontal = 16.dp).fillMaxWidth().padding(bottom = 14.dp)) }
            item {
                // Unbounded on the right, the chips stay on one line and scroll sideways.
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 14.dp)) {
                    ViewChips(state, viewModel::setView, large = true)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton(
                        onClick = { sorting = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
                        modifier = Modifier.height(48.dp).testTag("phone-sort"),
                    ) {
                        Icon(AppIcons.SwapVert, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(state.sort.field.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.onSurfaceVariant)
                    }
                    Text(
                        if (state.selected.isEmpty()) trPlural(state.rows.size, "{0} card", "{0} cards") else tr("{0} of {1} selected", state.selected.size, state.rows.size),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                    QuietButton(onClick = { selecting = !showChecks; if (!selecting) viewModel.clearSelection() }, modifier = Modifier.testTag("phone-select")) {
                        Text(if (showChecks) tr("Done") else tr("Select"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (state.selected.isNotEmpty()) item { Box(Modifier.padding(horizontal = 16.dp)) { CardSelectionBar(state, viewModel, actions) } }
            when {
                state.loading -> item { Text(tr("Loading..."), Modifier.padding(16.dp), color = colors.onSurfaceVariant) }
                state.rows.isEmpty() -> item { Box(Modifier.padding(horizontal = 16.dp)) { EmptyCards(state) } }
                else -> itemsIndexed(state.rows, key = { _, row -> row.termId }) { index, row ->
                    AppDivider()
                    PhoneCardRow(row, state, showCheck = showChecks, actions)
                    if (index == state.rows.lastIndex) AppDivider()
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

/** The page's menu, in the bar. */
@Composable
private fun BrowseMenu(onReview: () -> Unit) {
    OverflowMenu(tr("List actions"), buttonModifier = Modifier.testTag("phone-browse-menu")) { close ->
        AppMenuItem(text = { Text(tr("Review cards")) }, onClick = { close(); onReview() })
    }
}

/** A card as a row of the list: the word and its translation, its state and when it comes. */
@Composable
private fun PhoneCardRow(row: BrowseRow, state: BrowseUiState, showCheck: Boolean, actions: BrowseActions) {
    val colors = MaterialTheme.colorScheme
    val selected = row.termId in state.selected
    val dim = if (row.suspended) colors.outline else colors.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) colors.primary.copy(alpha = 0.08f) else colors.surface)
            .clickable { if (showCheck) actions.onToggle(row.termId) else actions.onOpen(row.termId) }
            .padding(start = if (showCheck) 6.dp else 18.dp, end = 6.dp, top = 14.dp, bottom = 14.dp)
            .testTag("browse-row-${row.termId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheck) AppCheckbox(checked = selected, onCheckedChange = { actions.onToggle(row.termId) })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                row.word,
                style = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = if (row.suspended) colors.outline else colors.primary, textDirection = if (row.rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.translation.isNotEmpty()) Text(row.translation, style = MaterialTheme.typography.bodyLarge, color = dim, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
