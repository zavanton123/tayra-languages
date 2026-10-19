package com.tayra.languages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppShapes

/**
 * A card of recent days as squares, darker the busier the day: a column per week from Monday, as
 * many weeks as fit, up to a year. The weeks shown depend on the width, so [subtitle] and [levels]
 * are given the first day shown; [levels] maps each busy day to 1 to 4, days left out being empty.
 */
@Composable
fun ActivityHeatmapCard(
    title: String,
    subtitle: (start: LocalDate) -> String,
    today: LocalDate,
    levels: (start: LocalDate) -> Map<LocalDate, Int>,
    fewer: String,
    more: String,
    icon: ImageVector = AppIcons.BarChart,
    color: Color = AppAccents.green,
) {
    val empty = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 4.dp
        // The card's padding takes 20 on each side.
        val inner = maxWidth - 40.dp
        val weeks = ((inner + gap) / (MIN_CELL + gap)).toInt().coerceIn(MIN_WEEKS, MAX_WEEKS)
        val cell: Dp = ((inner - gap * (weeks - 1)) / weeks).coerceAtMost(MAX_CELL)
        val thisMonday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
        val start = thisMonday.minus((weeks - 1) * 7, DateTimeUnit.DAY)
        val shown = levels(start)
        ContentCard(title, subtitle(start), icon = icon) {
            Row(Modifier.testTag("heatmap"), horizontalArrangement = Arrangement.spacedBy(gap)) {
                repeat(weeks) { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(7) { weekday ->
                            val date = start.plus(week * 7 + weekday, DateTimeUnit.DAY)
                            val level = shown[date] ?: 0
                            val shade = when {
                                date > today -> Color.Transparent
                                level == 0 -> empty
                                else -> color.copy(alpha = heatShade(level))
                            }
                            Box(Modifier.size(cell).clip(AppShapes.tiny).background(shade))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(fewer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.size(12.dp).clip(AppShapes.tiny).background(empty))
                (1..4).forEach { Box(Modifier.size(12.dp).clip(AppShapes.tiny).background(color.copy(alpha = heatShade(it)))) }
                Text(more, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private const val MIN_WEEKS = 8
private const val MAX_WEEKS = 53
private val MIN_CELL = 14.dp
private val MAX_CELL = 22.dp

private fun heatShade(level: Int): Float = 0.25f + 0.75f * level / 4f
