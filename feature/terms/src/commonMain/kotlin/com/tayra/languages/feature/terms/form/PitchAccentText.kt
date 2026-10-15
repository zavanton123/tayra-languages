package com.tayra.languages.feature.terms.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.dictionary.PitchAccent
import com.tayra.languages.core.ui.i18n.tr

/** A word's pitch accents, each as its reading with a line over the high morae, its downstep number and the pattern's name. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PitchAccents(accents: List<PitchAccent>, isVerbOrAdjective: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        accents.forEach { accent ->
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("pitch-${accent.reading}-${accent.downstep}")) {
                PitchAccentText(accent)
                val name = accent.category(isVerbOrAdjective)?.let { " " + tr(it) }.orEmpty()
                Text("[${accent.downstep}]$name", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** Yomitan's text notation: a line over each high mora that drops where the pitch falls. */
@Composable
private fun PitchAccentText(accent: PitchAccent) {
    val color = MaterialTheme.colorScheme.primary
    Row {
        accent.morae.forEachIndexed { index, mora ->
            val high = accent.isHigh(index)
            val falls = index + 1 == accent.downstep
            Text(
                mora,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.drawBehind {
                    val stroke = 1.5.dp.toPx()
                    if (high) drawLine(color, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
                    if (falls) drawLine(color, Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, size.height * 0.4f), stroke)
                }.padding(top = 4.dp),
            )
        }
    }
}
