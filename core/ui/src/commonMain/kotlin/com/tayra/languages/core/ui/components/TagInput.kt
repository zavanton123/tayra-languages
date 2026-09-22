package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

/**
 * A chip-based multi-value input with suggestions, used for tags and parent terms.
 *
 * Enter, comma or Tab commit the pending text; Backspace on an empty field removes the last chip.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagInput(
    values: List<String>,
    onValuesChange: (List<String>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suggestions: List<String> = emptyList(),
    onQueryChange: ((String) -> Unit)? = null,
    onChipClick: ((String) -> Unit)? = null,
    suggestionContent: (@Composable (String) -> Unit)? = null,
    pendingText: String? = null,
    onPendingTextChange: ((String) -> Unit)? = null,
) {
    var internalPending by remember { mutableStateOf("") }
    val pending = pendingText ?: internalPending
    fun setPending(text: String) {
        if (onPendingTextChange != null) onPendingTextChange(text) else internalPending = text
        onQueryChange?.invoke(text)
    }
    var focused by remember { mutableStateOf(false) }

    fun commit(text: String) {
        val cleaned = text.trim().trimEnd(',')
        if (cleaned.isNotEmpty() && values.none { it.equals(cleaned, ignoreCase = true) }) onValuesChange(values + cleaned)
        setPending("")
    }

    Column(modifier) {
        if (values.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                values.forEach { value ->
                    InputChip(
                        selected = false,
                        onClick = { onChipClick?.invoke(value) },
                        label = { Text(value) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove $value",
                                modifier = Modifier.clickable { onValuesChange(values - value) },
                            )
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = pending,
            onValueChange = { text ->
                if (text.endsWith(",")) commit(text) else setPending(text)
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter, Key.Tab -> {
                            if (pending.isNotBlank()) {
                                commit(pending)
                                true
                            } else {
                                false
                            }
                        }
                        Key.Backspace -> {
                            if (pending.isEmpty() && values.isNotEmpty()) {
                                onValuesChange(values.dropLast(1))
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                },
        )
        val visible = suggestions.filter { s -> values.none { it.equals(s, ignoreCase = true) } }
            .filter { pending.isBlank() || it.contains(pending, ignoreCase = true) }
        if (focused && pending.isNotBlank() && visible.isNotEmpty()) {
            Surface(tonalElevation = 3.dp, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                LazyColumn(Modifier.heightIn(max = 200.dp)) {
                    items(visible.take(20)) { suggestion ->
                        ListItem(
                            headlineContent = { suggestionContent?.invoke(suggestion) ?: Text(suggestion) },
                            modifier = Modifier.clickable { commit(suggestion) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                        )
                    }
                }
            }
        }
    }
}
