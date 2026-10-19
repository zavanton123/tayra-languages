package com.tayra.languages.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppShapes

/**
 * The Tags button of a list, with how many tags are chosen, and its panel: the [available] tags
 * to pick, found by typing, and whether an item needs any or all of them. The choice applies with
 * Apply; closing the panel otherwise leaves the [selected] tags as they were.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFilterButton(
    available: List<String>,
    selected: Set<String>,
    matchAll: Boolean,
    onApply: (tags: Set<String>, matchAll: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val active = selected.isNotEmpty()
    Box(modifier) {
        ToggleButton(
            tr("Tags"),
            active = active || open,
            onClick = { open = true },
            modifier = Modifier.testTag("tag-filter"),
            icon = AppIcons.Tag,
            badge = selected.size,
            trailingIcon = Icons.Default.ArrowDropDown,
        )
        AppMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.width(440.dp)) {
            TagFilterBody(available, selected, matchAll) { tags, all -> open = false; onApply(tags, all) }
        }
    }
}

/**
 * A phone's bottom sheet with the same tag filter; [onApply] gets the tags picked and whether a
 * book needs all of them, and the sheet is closed by whoever shows it.
 */
@Composable
fun TagFilterSheet(available: List<String>, selected: Set<String>, matchAll: Boolean, onApply: (tags: Set<String>, matchAll: Boolean) -> Unit, onDismiss: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
            TagFilterBody(available, selected, matchAll, onApply)
        }
    }
}

/** The tags to pick from, how they match, and Apply; built anew each time it is shown, from the tags chosen so far. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFilterBody(available: List<String>, selected: Set<String>, matchAll: Boolean, onApply: (tags: Set<String>, matchAll: Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var picked by remember { mutableStateOf(selected) }
    var all by remember { mutableStateOf(matchAll) }
    var query by remember { mutableStateOf("") }
    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Filter by tags"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                tr("Clear all"),
                Modifier.clip(AppShapes.small).clickable(enabled = picked.isNotEmpty()) { picked = emptySet() }.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (picked.isNotEmpty()) colors.primary else colors.outline,
            )
        }
        SearchField(query, { query = it }, tr("Find tags"), Modifier.fillMaxWidth(), fieldModifier = Modifier.testTag("tag-search"))
        val found = available.filter { it.contains(query.trim(), ignoreCase = true) }
        if (found.isEmpty()) {
            Text(tr("No tags match."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                found.forEach { tag ->
                    val toggle = { picked = if (tag in picked) picked - tag else picked + tag }
                    SelectableChip(
                        tag,
                        selected = tag in picked,
                        onClick = toggle,
                        modifier = Modifier.testTag("tag-$tag"),
                        size = ChipSize.Small,
                        onRemove = if (tag in picked) toggle else null,
                        removeDescription = tr("Remove {0}", tag),
                    )
                }
            }
        }
        AppDivider()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Match"), style = MaterialTheme.typography.bodyLarge)
            SegmentedToggle(
                listOf(Segment(false, label = tr("Any tag"), testTag = "match-any"), Segment(true, label = tr("All tags"), testTag = "match-all")),
                selected = all,
                onSelect = { all = it },
            )
        }
        AppDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (picked.isEmpty()) tr("No tags selected") else trPlural(picked.size, "{0} tag selected", "{0} tags selected"),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            PrimaryButton(tr("Apply"), onClick = { onApply(picked, all) }, modifier = Modifier.testTag("apply-tags"))
        }
    }
}
