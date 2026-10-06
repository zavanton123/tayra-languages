package com.tayra.languages.feature.frequency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.ToastHost
import com.tayra.languages.core.ui.components.rememberToastState
import com.tayra.languages.core.ui.state.CollectEvents
import org.koin.compose.viewmodel.koinViewModel

/**
 * Asks for the vocabulary level of the language with [languageId], just chosen to learn, when no
 * level was ever chosen for it. [onClosed] is called once a level is set or the reader puts it off;
 * put off, they are asked again the next time they choose the language.
 */
@Composable
fun VocabularyLevelPrompt(languageId: Long?, onClosed: () -> Unit, viewModel: VocabularySettingsViewModel = koinViewModel(key = "vocabulary-level-prompt")) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = rememberToastState()
    CollectEvents(viewModel.events) { toast.show(it) }
    ToastHost(toast)
    if (languageId == null) return
    // The view model follows the language being learned, which has just become this one.
    val ready = !state.loading && state.languageId == languageId && state.list != null
    LaunchedEffect(languageId, state.chosen, state.saving, ready) {
        if (ready && state.chosen && !state.saving) onClosed()
    }
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(
            Modifier.widthIn(max = 1100.dp).fillMaxWidth(0.94f).padding(vertical = 24.dp).testTag("vocabulary-level-prompt"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            if (!ready) {
                Box(Modifier.fillMaxWidth().heightIn(min = 240.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Surface
            }
            VocabularyLevelPromptContent(
                state,
                onPick = viewModel::pick,
                onSave = viewModel::save,
                onLater = { viewModel.forgetPick(); onClosed() },
            )
        }
    }
}

@Composable
internal fun VocabularyLevelPromptContent(state: VocabularySettingsUiState, onPick: (Int) -> Unit, onSave: () -> Unit, onLater: () -> Unit) {
    val list = state.list ?: return
    val wide = LocalWindowWidth.current.isExpanded
    Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("How much ${state.languageName} do you know?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Click the last row where you know all the words. They and their forms are saved as known, so your texts show them as known from the start. You can change this later in Settings → Vocabulary.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val picker = Modifier.heightIn(max = 460.dp)
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                LevelPicker(list, state.picked, onPick, picker.weight(1.1f))
                ExampleText(state, Modifier.weight(1f))
            }
        } else {
            LevelPicker(list, state.picked, onPick, picker.fillMaxWidth())
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.saving) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Saving…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onLater, enabled = !state.saving) { Text("Not now") }
            Button(onClick = onSave, enabled = state.picked != null && !state.saving, modifier = Modifier.testTag("prompt-set-level")) {
                Text(state.picked?.let { "Set level to ${formatCount(it)}" } ?: "Set vocabulary level")
            }
        }
    }
}
