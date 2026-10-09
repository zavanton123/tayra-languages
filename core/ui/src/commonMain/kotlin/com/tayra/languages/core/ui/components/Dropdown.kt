package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha

/** A simple select box. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Dropdown(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: String?,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** How an option is drawn in the menu, such as a font name in its own font; the label is shown otherwise. */
    optionContent: (@Composable (T) -> Unit)? = null,
    /** Why an option cannot be chosen, or null when it can; such options are listed dimmed. */
    unavailable: (T) -> String? = { null },
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = label?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(10.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            options.forEach { option ->
                val note = if (option == selected) null else unavailable(option)
                AppMenuItem(
                    text = {
                        Row(Modifier.alpha(if (note != null) 0.45f else 1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) { if (optionContent != null) optionContent(option) else Text(optionLabel(option)) }
                            if (note != null) Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    enabled = note == null,
                )
            }
        }
    }
}
