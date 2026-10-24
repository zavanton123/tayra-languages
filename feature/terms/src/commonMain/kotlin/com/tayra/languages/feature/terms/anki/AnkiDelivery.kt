package com.tayra.languages.feature.terms.anki

import com.tayra.languages.core.domain.export.AnkiConnection
import com.tayra.languages.core.domain.export.AnkiExportService
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/** How far putting words into Anki has got ("3 of 12"), and whether they go straight into a running Anki. */
class AnkiProgress(val done: String, val sending: Boolean)

/** The Anki button's label: its progress while the words go straight into Anki or into a file. */
fun ankiLabel(exporting: String?, sending: Boolean): String = when {
    exporting == null -> tr("Export to Anki")
    sending -> tr("Sending {0}", exporting)
    else -> tr("Exporting {0}", exporting)
}

/** What putting words into Anki came to: a message to show, or a package for the reader to save and import. */
sealed interface AnkiOutcome {
    class Done(val message: String) : AnkiOutcome
    class File(val export: AnkiExportService.Export) : AnkiOutcome
}

/** How long a running Anki has to answer before the words go into a file instead. */
val ANKI_LOOKUP = 2.seconds

/**
 * Puts the terms [ids] into Anki: straight into a running Anki through [connection] when one
 * answers (only the desktop can reach it), or else as a package to save and import.
 */
suspend fun putIntoAnki(ids: List<Long>, anki: AnkiExportService, connection: AnkiConnection?, onProgress: (AnkiProgress) -> Unit): AnkiOutcome {
    onProgress(AnkiProgress(tr("{0} of {1}", 0, ids.size), sending = false))
    val running = connection?.takeIf { withTimeoutOrNull(ANKI_LOOKUP) { it.version() } != null }
    return try {
        if (running != null) {
            val sent = anki.send(ids, running) { done, total -> onProgress(AnkiProgress(tr("{0} of {1}", done, total), sending = true)) }
                ?: return AnkiOutcome.Done(alreadyInAnki(ids.size))
            AnkiOutcome.Done(
                listOfNotNull(
                    trPlural(sent.added, "Added {0} card to Anki", "Added {0} cards to Anki"),
                    (sent.duplicates + sent.skipped).takeIf { it > 0 }?.let { tr("{0} already in Anki left out", it) },
                    sent.failed.firstOrNull()?.let { tr("Anki refused some: {0}", it) },
                ).joinToString(", "),
            )
        } else {
            val export = anki.export(ids) { done, total -> onProgress(AnkiProgress(tr("{0} of {1}", done, total), sending = false)) }
                ?: return AnkiOutcome.Done(alreadyInAnki(ids.size))
            AnkiOutcome.File(export)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AnkiOutcome.Done(if (running != null) tr("Could not send to Anki: {0}", e.message) else tr("Could not export: {0}", e.message))
    }
}

/** The package was saved: its words are marked exported, so they are not exported again; the message says so. */
suspend fun ankiSaved(anki: AnkiExportService, export: AnkiExportService.Export): String {
    anki.markExported(export)
    val exported = trPlural(export.termIds.size, "Exported {0} word to Anki", "Exported {0} words to Anki")
    return if (export.skipped > 0) exported + ", " + tr("{0} already in Anki left out", export.skipped) else exported
}

private fun alreadyInAnki(count: Int) =
    if (count == 1) tr("This word is already in Anki") else trPlural(count, "All {0} words are already in Anki", "All {0} words are already in Anki")
