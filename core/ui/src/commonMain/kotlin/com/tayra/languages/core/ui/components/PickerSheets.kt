package com.tayra.languages.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppShapes

/**
 * A phone's bottom sheet listing [options] to pick one from, the chosen one ticked; [labelContent]
 * draws an option's name and [leading] goes before it.
 */
@Composable
fun <T> PickerSheet(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit,
    labelContent: @Composable (T) -> Unit = { Text(label(it), style = MaterialTheme.typography.bodyLarge) },
    leading: @Composable ((T) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp).padding(bottom = 16.dp)) {
            Text(title, Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            options.forEach { option ->
                Row(
                    Modifier.fillMaxWidth().clip(AppShapes.control).clickable { onPick(option) }.padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    leading?.let { it(option) }
                    Row(Modifier.weight(1f)) { labelContent(option) }
                    if (option == selected) Icon(Icons.Default.Check, contentDescription = tr("Selected"), tint = colors.primary, modifier = Modifier.size(22.dp))
                    else Spacer(Modifier.width(22.dp))
                }
            }
        }
    }
}

/**
 * A phone's bottom sheet of languages with their flags, the chosen one ticked and the ones that
 * cannot be chosen dimmed with why; [flagName] is the English name the flag is looked up by.
 */
@Composable
fun <T> LanguagePickerSheet(
    title: String,
    options: List<T>,
    selected: T?,
    name: (T) -> String,
    flagName: (T) -> String,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit,
    unavailable: (T) -> String? = { null },
) {
    val colors = MaterialTheme.colorScheme
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp).padding(bottom = 16.dp)) {
            Text(title, Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            options.forEach { option ->
                val chosen = option == selected
                val note = if (chosen) null else unavailable(option)
                Row(
                    Modifier.fillMaxWidth().clip(AppShapes.control).clickable(enabled = note == null) { if (!chosen) onPick(option) else onDismiss() }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                ) {
                    LanguageMenuRow(flagName(option), name(option), chosen, note)
                }
            }
        }
    }
}
