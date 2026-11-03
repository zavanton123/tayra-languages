package com.tayra.languages.feature.flashcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppTextField
import com.tayra.languages.core.ui.components.PrimaryButton
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppSpacing

/** The days from today typed for Set due date: a number, or a range such as 3-7; null when it reads as neither. */
internal fun dueDays(text: String): IntRange? {
    val parts = text.trim().split('-').map { it.trim().toIntOrNull() ?: return null }
    val range = when (parts.size) {
        1 -> parts[0]..parts[0]
        2 -> parts[0]..parts[1]
        else -> return null
    }
    return range.takeIf { it.first in 0..MAX_DUE_DAYS && it.last in it.first..MAX_DUE_DAYS }
}

private const val MAX_DUE_DAYS = 36500

/** Asks how many days from today the [count] cards chosen come up, as Anki's Set due date does. */
@Composable
internal fun SetDueDialog(count: Int, onSet: (IntRange) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("1") }
    val days = dueDays(text)
    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("set-due-dialog"),
        title = { Text(trPlural(count, "Set the due date of {0} card", "Set the due date of {0} cards")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
                val name = tr("Days from today")
                AppTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(name) },
                    singleLine = true,
                    isError = days == null,
                    supportingText = if (days == null) ({ Text(tr("Use a number, or a range such as 3-7")) }) else null,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = name },
                )
                Text(
                    tr("0 is today and 1 tomorrow; a range spreads the cards over its days. Their memory stays as it is, and new cards are left alone."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            PrimaryButton(tr("Set"), onClick = { days?.let(onSet); onDismiss() }, enabled = days != null, modifier = Modifier.testTag("set-due-confirm"))
        },
        dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
    )
}
