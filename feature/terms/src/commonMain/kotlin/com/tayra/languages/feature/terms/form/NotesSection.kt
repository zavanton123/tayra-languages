package com.tayra.languages.feature.terms.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.morphology.JapaneseNote
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import kotlin.math.abs
import kotlin.math.roundToLong

/** The numbers, era years and floor areas at the term, spelled out, as 10ten Japanese Reader shows them above its entries. */
@Composable
internal fun NotesSection(notes: List<JapaneseNote>) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(AppShapes.card).background(colors.secondaryContainer.copy(alpha = 0.35f))
            .border(AppBorders.hairline(), AppShapes.card).padding(horizontal = 14.dp, vertical = 10.dp).testTag("japanese-notes"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        notes.forEach { note ->
            Column {
                Text(headline(note), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                detail(note)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            }
        }
    }
}

internal fun headline(note: JapaneseNote): String = when (note) {
    is JapaneseNote.Number -> "${note.source} = ${decimal(note.value)}"
    is JapaneseNote.EraYear -> buildString {
        append("${note.source} = ${note.gregorianYear}")
        if (note.month != null) append("-" + note.month.toString().padStart(2, '0'))
        if (note.day != null) append("-" + note.day.toString().padStart(2, '0'))
    }
    is JapaneseNote.Area -> if (note.inMats) "${note.source} = ${decimal(note.converted)} m²" else "${note.source} = ${decimal(note.converted)} 帖"
}

private fun detail(note: JapaneseNote): String? = when (note) {
    is JapaneseNote.Number -> null
    is JapaneseNote.EraYear -> if (note.year == 0) tr("{0} ({1}), its first year", note.era, note.reading) else "${note.era} (${note.reading})"
    is JapaneseNote.Area -> note.regional.takeIf { it.isNotEmpty() }?.joinToString(", ") { (name, value) ->
        if (note.inMats) "$name ${decimal(value)} m²" else "$name ${decimal(value)} 畳"
    }
}

/** A value with at most two decimals and its thousands grouped: 7.29, 1,981, 398,000,000. */
internal fun decimal(value: Double): String {
    val hundredths = (abs(value) * 100).roundToLong()
    val whole = (hundredths / 100).toString().reversed().chunked(3).joinToString(",").reversed()
    val fraction = (hundredths % 100).toString().padStart(2, '0').trimEnd('0')
    return (if (value < 0) "-" else "") + whole + if (fraction.isEmpty()) "" else ".$fraction"
}
