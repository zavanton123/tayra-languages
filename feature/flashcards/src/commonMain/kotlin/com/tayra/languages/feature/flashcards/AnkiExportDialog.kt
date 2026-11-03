package com.tayra.languages.feature.flashcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tayra.languages.core.ui.components.AppDialog
import com.tayra.languages.core.ui.components.AppProgressBar
import com.tayra.languages.core.ui.components.QuietButton
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppSpacing

/** An export to Anki as it goes: its progress, then how to bring the saved deck into Anki, or that it failed. Closes only once it is over. */
@Composable
internal fun AnkiExportDialog(export: AnkiExport, onDismiss: () -> Unit) {
    val working = export is AnkiExport.Working
    AppDialog(
        onDismissRequest = { if (!working) onDismiss() },
        modifier = Modifier.testTag("anki-export"),
        title = {
            Text(
                when (export) {
                    is AnkiExport.Saved -> tr("Exported to Anki")
                    AnkiExport.Failed -> tr("Could not export to Anki")
                    else -> tr("Exporting to Anki")
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                when (export) {
                    is AnkiExport.Working -> {
                        Text(if (export.total == 0) tr("Gathering the cards...") else tr("{0} of {1} cards", export.done, export.total))
                        AppProgressBar(if (export.total == 0) null else export.done.toFloat() / export.total, Modifier.fillMaxWidth())
                    }
                    is AnkiExport.Saved -> {
                        Text(trPlural(export.cards, "{0} card saved with its progress", "{0} cards saved with their progress"), style = MaterialTheme.typography.bodyLarge)
                        listOf(
                            tr("In Anki, open the file with File › Import and tick “Import any learning progress”, or the cards come in as new."),
                            tr("Tick “Import any deck presets” too for the deck to keep these flashcard settings."),
                        ).forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    else -> Text(tr("Something went wrong while making the deck."))
                }
            }
        },
        confirmButton = { if (!working) QuietButton(tr("OK"), onClick = onDismiss, modifier = Modifier.testTag("anki-export-close")) },
    )
}
