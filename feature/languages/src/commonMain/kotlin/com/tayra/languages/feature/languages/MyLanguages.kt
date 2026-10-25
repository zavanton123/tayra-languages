package com.tayra.languages.feature.languages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppDivider
import com.tayra.languages.core.ui.components.ButtonSize
import com.tayra.languages.core.ui.components.ButtonTone
import com.tayra.languages.core.ui.components.LanguageFlag
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.components.formatSize
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/** The languages being learned, each with the words known in it and a way to remove it with all its data. */
@Composable
internal fun MyLanguagesCard(learning: LearningLanguageState, onRemove: (Long, String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(AppShapes.card).background(colors.surface).border(AppBorders.hairline(colors.outlineVariant), AppShapes.card).padding(AppSpacing.l).testTag("my-languages"),
    ) {
        Text(tr("My languages").uppercase(), style = AppText.overline.copy(color = colors.onSurfaceVariant))
        Text(tr("Languages I'm learning"), Modifier.padding(top = AppSpacing.xs, bottom = AppSpacing.m), style = AppText.sectionTitle)
        val mine = learning.myChoices
        mine.forEachIndexed { index, choice ->
            Row(Modifier.fillMaxWidth().padding(vertical = AppSpacing.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                LanguageFlag(choice.label, 26.dp)
                Column(Modifier.weight(1f)) {
                    Text(tr(choice.label), style = AppText.emphasis, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val words = learning.wordsKnown(choice.id)
                    val note = listOfNotNull(
                        words?.let { trPlural(it, "{0} word", "{0} words", formatCount(it)) },
                        tr("Learning now").takeIf { choice == learning.current },
                    ).joinToString(" · ")
                    if (note.isNotEmpty()) Text(note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                QuietButton(tr("Remove"), onClick = { onRemove(choice.id, choice.name) }, tone = ButtonTone.Danger, size = ButtonSize.Compact, modifier = Modifier.testTag("remove-" + choice.tag))
            }
            if (index < mine.lastIndex) AppDivider(color = colors.outlineVariant.copy(alpha = AppAlpha.muted))
        }
        Text(tr("Removing a language deletes its texts, words and progress, and the dictionaries, voices and other downloads kept for it."), Modifier.padding(top = AppSpacing.m), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}

/** Asks before a language goes, saying what goes with it; then says what could not be removed, if anything. */
@Composable
internal fun RemoveLanguageDialog(prompt: LanguagesViewModel.RemovalPrompt, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val name = tr(prompt.name)
    if (prompt.problems.isNotEmpty()) {
        AppDialog(
            onDismissRequest = onDismiss,
            title = { Text(tr("{0} was removed", name)) },
            text = { Text(tr("Some downloads could not be removed:") + "\n" + prompt.problems.joinToString("\n")) },
            confirmButton = { QuietButton(tr("OK"), onClick = onDismiss) },
        )
        return
    }
    val counted = prompt.removal
    val text = if (counted == null) tr("Counting what it has…") else buildList {
        add(trPlural(counted.texts, "{0} text", "{0} texts", formatCount(counted.texts)))
        add(trPlural(counted.words, "{0} saved word", "{0} saved words", formatCount(counted.words)))
        if (counted.downloadBytes > 0) add(tr("{0} of downloads", formatSize(counted.downloadBytes)))
    }.let { parts ->
        tr("Everything you have in {0} is deleted: {1}, with your progress and flashcards. This cannot be undone.", name, parts.joinToString(", "))
    }
    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("remove-language-dialog"),
        title = { Text(tr("Remove {0}?", name)) },
        text = { Text(text) },
        confirmButton = { QuietButton(tr("Remove"), onClick = onConfirm, tone = ButtonTone.Danger, loading = prompt.removing, enabled = counted != null, modifier = Modifier.testTag("confirm-remove")) },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss, enabled = !prompt.removing) },
    )
}
