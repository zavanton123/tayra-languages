package com.tayra.languages.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.hotkeys.HotkeyMatcher
import com.tayra.languages.core.ui.navigation.Route
import org.koin.compose.viewmodel.koinViewModel

/** Keyboard shortcut editor: focus a field and press the desired key combination. */
@Composable
fun ShortcutsScreen(onNavigate: (Route) -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { AppTopBar(title = "Keyboard shortcuts", onNavigate = onNavigate) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 800.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Click a shortcut field and press a key combination. Press Backspace to clear it.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { viewModel.update { it.copy(hotkeys = HotkeyAction.defaults) } }) { Text("Reset to defaults") }
            HotkeyAction.byCategory.forEach { (category, actions) ->
                Text(category.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                actions.forEach { action ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(action.description, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        HotkeyField(settings.hotkeys[action]) { hotkey -> viewModel.update { it.copy(hotkeys = it.hotkeys + (action to hotkey)) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun HotkeyField(hotkey: Hotkey?, onChange: (Hotkey?) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    Text(
        text = if (focused) "press a key..." else hotkey?.serialized ?: "—",
        style = MaterialTheme.typography.bodyMedium,
        color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .width(160.dp)
            .border(1.dp, if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (!focused || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val pressed = HotkeyMatcher.fromEvent(event) ?: return@onKeyEvent false
                onChange(if (pressed.key == "Backspace" || pressed.key == "Delete") null else pressed)
                true
            }
            .clickable { requester.requestFocus() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
