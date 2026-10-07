package com.tayra.languages.feature.frequency

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.tayra.languages.core.domain.frequency.LevelChange
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural

/** Asks before the level moves, saying what happens to the words between the two levels. */
@Composable
internal fun LevelConfirmDialog(from: Int, to: Int, onConfirm: () -> Unit, onDismiss: () -> Unit, first: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Set vocabulary level")) },
        text = {
            val question = if (first) tr("Set your vocabulary level to {0}?", formatCount(to)) else tr("Change vocabulary level from {0} to {1}?", formatCount(from), formatCount(to))
            val outcome = when {
                first && to == 0 -> tr("No words are marked as known; you can raise the level whenever you like.")
                to > from -> tr("The words ranked {0}–{1}, and their forms, are saved as known. Words you have already saved keep their status.", formatCount(from + 1), formatCount(to))
                to == 0 -> tr("The words the level marked as known are taken off your vocabulary, unless you have changed them since.")
                else -> tr("The words ranked {0}–{1} that the level marked as known are taken off your vocabulary, unless you have changed them since.", formatCount(to + 1), formatCount(from))
            }
            Text("$question\n\n$outcome")
        },
        confirmButton = { Button(onClick = onConfirm) { Text(tr("Yes")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

/** The toast after a level was set. */
internal fun LevelChange.message(): String {
    val level = if (to == 0) tr("Vocabulary level cleared") else tr("Vocabulary level set to {0}", formatCount(to))
    val parts = listOfNotNull(
        trPlural(added, "{1} word marked as known", "{1} words marked as known", formatCount(added)).takeIf { added > 0 },
        trPlural(removed, "{1} taken off", "{1} taken off", formatCount(removed)).takeIf { removed > 0 },
    )
    return if (parts.isEmpty()) "$level." else "$level: ${parts.joinToString(", ")}."
}

