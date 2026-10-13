package com.tayra.languages.feature.flashcards

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppMenu
import com.tayra.languages.core.ui.components.AppMenuItem
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.PickerSheet
import com.tayra.languages.core.ui.components.ScrollList
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.navigation.Route

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
        topBar = {
            AppTopBar(
                title = tr("Browse flashcards"),
                onNavigate = onNavigate,
                section = NavSection.FLASHCARDS,
                onBack = onBack,
                actions = { PhoneMenu(selecting = selecting, onSelecting = { selecting = it; if (!it) viewModel.clearSelection() }, onReview = actions.onReview) },
            )
        },
    ) { padding ->
        val list = rememberLazyListState()
        // Search, groups and the sort row, with the selection's actions when there is a selection, come before the cards.
        FocusRow(state, list, rowsBefore = if (state.selected.isNotEmpty()) 4 else 3, onShown = viewModel::focusShown)
        ScrollList(Modifier.padding(padding).fillMaxSize(), state = list, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
            item { CardSearchBox(state.search, viewModel::setSearch, Modifier.fillMaxWidth().padding(bottom = 10.dp)) }
            item {
                // Unbounded on the right, the chips stay on one line and scroll sideways.
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
                    ViewChips(state, viewModel::setView)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { sorting = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
                        modifier = Modifier.height(44.dp).testTag("phone-sort"),
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (state.selected.isNotEmpty()) item { SelectionBar(state, viewModel, actions, compact = true) }
            when {
                state.loading -> item { Text(tr("Loading..."), Modifier.padding(16.dp), color = colors.onSurfaceVariant) }
                state.rows.isEmpty() -> item { EmptyCards(state) }
                else -> itemsIndexed(state.rows, key = { _, row -> row.termId }) { _, row ->
                    PhoneCardRow(row, state, showCheck = showChecks, actions)
                    Spacer(Modifier.height(8.dp))
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

@Composable
private fun PhoneMenu(selecting: Boolean, onSelecting: (Boolean) -> Unit, onReview: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.testTag("phone-browse-menu")) { Icon(Icons.Default.MoreVert, contentDescription = tr("List actions"), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            AppMenuItem(text = { Text(if (selecting) tr("Done selecting") else tr("Select cards")) }, onClick = { open = false; onSelecting(!selecting) })
            AppMenuItem(text = { Text(tr("Review cards")) }, onClick = { open = false; onReview() })
        }
    }
}

/** A card as a tile: the word and its translation, its state and when it comes. */
@Composable
private fun PhoneCardRow(row: BrowseRow, state: BrowseUiState, showCheck: Boolean, actions: BrowseActions) {
    val colors = MaterialTheme.colorScheme
    val selected = row.termId in state.selected
    val dim = if (row.suspended) colors.outline else colors.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.08f) else colors.surface)
            .border(1.dp, if (selected) colors.primary.copy(alpha = 0.5f) else colors.outlineVariant, RoundedCornerShape(12.dp))
            .clickable { if (showCheck) actions.onToggle(row.termId) else actions.onOpen(row.termId) }
            .padding(start = if (showCheck) 4.dp else 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)
            .testTag("browse-row-${row.termId}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheck) Checkbox(checked = selected, onCheckedChange = { actions.onToggle(row.termId) })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                row.word,
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = if (row.suspended) colors.outline else colors.primary, textDirection = if (row.rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.translation.isNotEmpty()) Text(row.translation, style = MaterialTheme.typography.bodyMedium, color = dim, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatePill(row)
                Text(
                    dueLabel(row, state.now),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.isDue && !row.suspended) LEARNING_TINT else dim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.lapses > 0) Text(trPlural(row.lapses, "{0} lapse", "{0} lapses"), style = MaterialTheme.typography.bodySmall, color = Color(0xFFD64545))
            }
        }
        RowMenu(row, actions)
    }
}
