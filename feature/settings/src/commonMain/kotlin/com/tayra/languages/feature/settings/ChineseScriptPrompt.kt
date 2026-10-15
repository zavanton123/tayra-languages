package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.service.ChineseScriptService
import com.tayra.languages.core.ui.i18n.tr
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Asks a new learner of Mandarin which characters they learn, simplified (mainland China, Singapore)
 * or traditional (Taiwan, Hong Kong): texts are then shown, and words kept, in those. The reader's
 * settings change it later.
 */
@Composable
fun ChineseScriptPrompt(languageId: Long?, onClosed: () -> Unit) {
    if (languageId == null) return
    val service = koinInject<ChineseScriptService>()
    val scope = rememberCoroutineScope()
    fun choose(script: ChineseScript) {
        scope.launch {
            service.setScript(languageId, script)
            onClosed()
        }
    }
    AlertDialog(
        onDismissRequest = onClosed,
        title = { Text(tr("Which characters do you learn?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Texts are shown, and your words kept, in the characters you choose, whichever a text is written in."), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { choose(ChineseScript.SIMPLIFIED) }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().testTag("script-simplified")) {
                    Text(tr("Simplified · 简体 (mainland China, Singapore)"))
                }
                OutlinedButton(onClick = { choose(ChineseScript.TRADITIONAL) }, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().testTag("script-traditional")) {
                    Text(tr("Traditional · 繁體 (Taiwan, Hong Kong)"))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClosed) { Text(tr("Decide later")) } },
    )
}
