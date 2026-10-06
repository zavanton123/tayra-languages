package com.tayra.languages.feature.frequency

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.tayra.languages.core.domain.frequency.LevelChange

/** Asks before the level moves, saying what happens to the words between the two levels. */
@Composable
internal fun LevelConfirmDialog(from: Int, to: Int, onConfirm: () -> Unit, onDismiss: () -> Unit, first: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set vocabulary level") },
        text = {
            Text(
                (if (first) "Set your vocabulary level to ${formatCount(to)}?\n\n" else "Change vocabulary level from ${formatCount(from)} to ${formatCount(to)}?\n\n") + when {
                    first && to == 0 -> "No words are marked as known; you can raise the level whenever you like."
                    to > from -> "The words ranked ${formatCount(from + 1)}–${formatCount(to)}, and their forms, are saved as known. Words you have already saved keep their status."
                    to == 0 -> "The words the level marked as known are taken off your vocabulary, unless you have changed them since."
                    else -> "The words ranked ${formatCount(to + 1)}–${formatCount(from)} that the level marked as known are taken off your vocabulary, unless you have changed them since."
                },
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Yes") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** The toast after a level was set. */
internal fun LevelChange.message(): String {
    val level = if (to == 0) "Vocabulary level cleared" else "Vocabulary level set to ${formatCount(to)}"
    val parts = listOfNotNull(
        "${formatCount(added)} ${if (added == 1) "word" else "words"} marked as known".takeIf { added > 0 },
        "${formatCount(removed)} taken off".takeIf { removed > 0 },
    )
    return if (parts.isEmpty()) "$level." else "$level: ${parts.joinToString(", ")}."
}

/** 10000 as "10,000". */
internal fun formatCount(n: Int): String = n.toString().reversed().chunked(3).joinToString(",").reversed()
