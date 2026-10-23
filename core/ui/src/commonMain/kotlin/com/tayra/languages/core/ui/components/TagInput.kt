package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import com.tayra.languages.core.ui.i18n.tr
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing

/**
 * A chip-based multi-value input with suggestions, used for tags and parent terms.
 *
 * Enter, comma or Tab commit the pending text; Backspace on an empty field removes the last chip.
 */
@OptIn(ExperimentalLayoutApi::class)
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
            FlowRow(Modifier.padding(vertical = AppSpacing.s), horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                values.forEach { value ->
                    RemovableChip(
                        onRemove = { onValuesChange(values - value) },
                        removeDescription = tr("Remove {0}", value),
                        modifier = if (onChipClick != null) Modifier.clip(AppShapes.compact).clickable { onChipClick(value) } else Modifier,
                    ) {
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        AppTextField(
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
            PopupCard(Modifier.fillMaxWidth()) {
                val list = rememberLazyListState()
                Box(Modifier.heightIn(max = SUGGESTIONS_MAX_HEIGHT)) {
                    LazyColumn(state = list) {
                        items(visible.take(20)) { suggestion ->
                            AppMenuItem(text = { suggestionContent?.invoke(suggestion) ?: Text(suggestion) }, onClick = { commit(suggestion) })
                        }
                    }
                    EdgeScrollbar(list)
                }
            }
        }
    }
}

private val SUGGESTIONS_MAX_HEIGHT = 200.dp
