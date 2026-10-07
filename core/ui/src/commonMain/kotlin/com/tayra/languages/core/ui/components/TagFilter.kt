package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural

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
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val active = selected.isNotEmpty()
    Box(modifier) {
        Row(
            Modifier.height(48.dp).clip(RoundedCornerShape(10.dp))
                .border(if (active || open) 1.5.dp else 1.dp, if (active || open) colors.primary else colors.outlineVariant, RoundedCornerShape(10.dp))
                .background(colors.surface).clickable { open = true }.padding(start = 14.dp, end = 10.dp).testTag("tag-filter"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(AppIcons.Tag, contentDescription = null, tint = if (active) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text(tr("Tags"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = if (active) colors.primary else colors.onSurface, softWrap = false)
            if (active) {
                Text(
                    "${selected.size}",
                    Modifier.clip(CircleShape).background(colors.primary).padding(horizontal = 8.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colors.onSurfaceVariant)
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.width(440.dp)) {
            // The panel is built anew each time it opens, from the tags chosen so far.
            var picked by remember { mutableStateOf(selected) }
            var all by remember { mutableStateOf(matchAll) }
            var query by remember { mutableStateOf("") }
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Filter by tags"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        tr("Clear all"),
                        Modifier.clip(RoundedCornerShape(6.dp)).clickable(enabled = picked.isNotEmpty()) { picked = emptySet() }.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (picked.isNotEmpty()) colors.primary else colors.outline,
                    )
                }
                FindField(query, { query = it }, Modifier.fillMaxWidth())
                val found = available.filter { it.contains(query.trim(), ignoreCase = true) }
                if (found.isEmpty()) {
                    Text(tr("No tags match."), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        found.forEach { tag -> TagChip(tag, tag in picked) { picked = if (tag in picked) picked - tag else picked + tag } }
                    }
                }
                HorizontalDivider(color = colors.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(tr("Match"), style = MaterialTheme.typography.bodyLarge)
                    Row(Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(3.dp)) {
                        listOf(false to tr("Any tag"), true to tr("All tags")).forEach { (needsAll, label) ->
                            val chosen = all == needsAll
                            Text(
                                label,
                                Modifier.clip(RoundedCornerShape(7.dp)).background(if (chosen) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { all = needsAll }.padding(horizontal = 16.dp, vertical = 8.dp).testTag(if (needsAll) "match-all" else "match-any"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (chosen) colors.primary else colors.onSurfaceVariant,
                            )
                        }
                    }
                }
                HorizontalDivider(color = colors.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (picked.isEmpty()) tr("No tags selected") else trPlural(picked.size, "{0} tag selected", "{0} tags selected"),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Button(onClick = { open = false; onApply(picked, all) }, shape = RoundedCornerShape(10.dp), modifier = Modifier.testTag("apply-tags")) {
                        Text(tr("Apply"))
                    }
                }
            }
        }
    }
}

/** A tag to pick: outlined, or tinted with a cross once picked. */
@Composable
private fun TagChip(tag: String, picked: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (picked) colors.primary.copy(alpha = 0.1f) else colors.surface)
            .border(1.dp, if (picked) colors.primary.copy(alpha = 0.6f) else colors.outlineVariant, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(start = 14.dp, end = if (picked) 10.dp else 14.dp, top = 6.dp, bottom = 6.dp)
            .testTag("tag-$tag"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(tag, style = MaterialTheme.typography.bodyMedium, color = if (picked) colors.primary else colors.onSurface)
        if (picked) Icon(Icons.Default.Close, contentDescription = tr("Remove {0}", tag), tint = colors.primary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun FindField(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(10.dp)).background(colors.surface).border(1.dp, colors.outlineVariant, RoundedCornerShape(10.dp)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.weight(1f).testTag("tag-search"),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(tr("Find tags"), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            },
        )
    }
}
