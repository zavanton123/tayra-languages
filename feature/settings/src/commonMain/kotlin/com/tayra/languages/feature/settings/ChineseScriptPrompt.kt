package com.tayra.languages.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.service.ChineseScriptService
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
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
    AppDialog(
        onDismissRequest = onClosed,
        title = { Text(tr("Which characters do you learn?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                Text(tr("Every text is shown in the characters you pick, whichever it was written in, and your words are saved in them."), style = MaterialTheme.typography.bodyMedium)
                // Two equal choices: neither is the default.
                ScriptOption("简体", tr("Simplified"), tr("Mainland China, Singapore"), Modifier.testTag("script-simplified")) { choose(ChineseScript.SIMPLIFIED) }
                ScriptOption("繁體", tr("Traditional"), tr("Taiwan, Hong Kong"), Modifier.testTag("script-traditional")) { choose(ChineseScript.TRADITIONAL) }
            }
        },
        confirmButton = {},
        dismissButton = { QuietButton(onClick = onClosed) { Text(tr("Decide later")) } },
    )
}

/** One script to choose: its name written in it, large, then its name and where it is used. */
@Composable
private fun ScriptOption(characters: String, name: String, where: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().clip(AppShapes.panel).border(AppBorders.hairline(), AppShapes.panel).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppSpacing.l, vertical = AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        Text(characters, style = AppText.headline, color = colors.primary)
        Column(Modifier.weight(1f)) {
            Text(name, style = AppText.itemTitle)
            Text(where, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

