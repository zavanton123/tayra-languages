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

/** An import from Anki as it goes: its progress, then what it did or why it could not. Closes only once it is over. */
@Composable
internal fun AnkiImportDialog(import: AnkiImport, onDismiss: () -> Unit) {
    val working = import is AnkiImport.Working
    AppDialog(
        onDismissRequest = { if (!working) onDismiss() },
        modifier = Modifier.testTag("anki-import"),
        title = {
            Text(
                when (import) {
                    is AnkiImport.Working -> tr("Importing from Anki")
                    is AnkiImport.Done -> tr("Imported from Anki")
                    is AnkiImport.Failed -> tr("Could not import from Anki")
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                when (import) {
                    is AnkiImport.Working -> {
                        Text(if (import.total == 0) tr("Reading the package...") else tr("{0} of {1} words", import.done, import.total))
                        AppProgressBar(if (import.total == 0) null else import.done.toFloat() / import.total, Modifier.fillMaxWidth())
                    }
                    is AnkiImport.Done -> ReportLines(import)
                    is AnkiImport.Failed -> Text(import.reason?.let { tr(it) } ?: tr("Something went wrong while reading the package."))
                }
            }
        },
        confirmButton = { if (!working) QuietButton(tr("OK"), onClick = onDismiss, modifier = Modifier.testTag("anki-import-close")) },
    )
}

@Composable
private fun ReportLines(done: AnkiImport.Done) {
    val report = done.report
    val lines = buildList {
        if (report.added > 0) add(trPlural(report.added, "{0} new word saved with its card", "{0} new words saved with their cards"))
        if (report.carded > 0) add(trPlural(report.carded, "{0} saved word given a card", "{0} saved words given cards"))
        if (report.updated > 0) add(trPlural(report.updated, "{0} card brought up to date", "{0} cards brought up to date"))
        if (report.reviews > 0) add(trPlural(report.reviews, "{0} answer added to the history", "{0} answers added to the history"))
        if (isEmpty()) add(tr("The package has no cards to import."))
    }
    lines.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
    val left = buildList {
        if (report.unreadable > 0) add(trPlural(report.unreadable, "{0} note left out: it has no word to read.", "{0} notes left out: they have no word to read."))
        if (report.missingLanguages.isNotEmpty()) add(tr("Left out as not learned here: {0}.", report.missingLanguages.joinToString(", ")))
    }
    left.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
