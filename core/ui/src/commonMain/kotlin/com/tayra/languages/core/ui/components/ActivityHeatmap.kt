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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

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
    /** What a day held, shown under the squares for the day pointed at or tapped. */
    describe: ((LocalDate) -> String)? = null,
) {
    val empty = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.wash)
    var pointed by remember { mutableStateOf<LocalDate?>(null) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = AppSpacing.xs
        // The card's padding takes 20 on each side, and the weekday names a column of their own.
        val inner = maxWidth - 40.dp - WEEKDAY_WIDTH - gap
        val weeks = ((inner + gap) / (MIN_CELL + gap)).toInt().coerceIn(MIN_WEEKS, MAX_WEEKS)
        val cell: Dp = ((inner - gap * (weeks - 1)) / weeks).coerceAtMost(MAX_CELL)
        val thisMonday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
        val start = thisMonday.minus((weeks - 1) * 7, DateTimeUnit.DAY)
        val shown = levels(start)
        ContentCard(title, subtitle(start), icon = icon) {
            val labelStyle = MaterialTheme.typography.labelSmall
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            // A month is named over the week its first day falls in; the first week is named only when no other is near it.
            val monthOf: (Int) -> Int? = { week ->
                val days = (0 until 7).map { start.plus(week * 7 + it, DateTimeUnit.DAY) }
                days.firstOrNull { it.day == 1 }?.month?.ordinal
                    ?: if (week == 0 && (1..2).none { w -> (0 until 7).any { start.plus(w * 7 + it, DateTimeUnit.DAY).day == 1 } }) days.first().month.ordinal else null
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Column(Modifier.width(WEEKDAY_WIDTH), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Spacer(Modifier.height(MONTH_ROW_HEIGHT))
                    repeat(7) { weekday ->
                        Box(Modifier.height(cell), contentAlignment = Alignment.CenterStart) {
                            WEEKDAY_NAMES[weekday]?.let { Text(tr(it), style = labelStyle, color = labelColor, maxLines = 1, softWrap = false) }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.height(MONTH_ROW_HEIGHT), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(weeks) { week ->
                            Box(Modifier.width(cell)) {
                                monthOf(week)?.let { month ->
                                    Text(tr(MONTH_NAMES[month]), Modifier.wrapContentWidth(Alignment.Start, unbounded = true), style = labelStyle, color = labelColor, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.testTag("heatmap").then(
                            if (describe == null) Modifier else Modifier.pointerInput(start, weeks, cell) {
                                // The square under the pointer, found from where it is, rather than a listener on every square.
                                val step = (cell + gap).toPx()
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val at = event.changes.firstOrNull()?.position ?: continue
                                        pointed = if (event.type == PointerEventType.Exit) null else {
                                            val week = (at.x / step).toInt()
                                            val weekday = (at.y / step).toInt()
                                            if (at.x < 0 || at.y < 0 || week >= weeks || weekday > 6) null
                                            else start.plus(week * 7 + weekday, DateTimeUnit.DAY).takeIf { it <= today }
                                        }
                                    }
                                }
                            },
                        ),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
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
                }
            }
            if (describe != null) {
                Spacer(Modifier.height(AppSpacing.s))
                // Always a line tall, so the legend under it does not move as days are pointed at.
                Text(pointed?.let { "${it.shortDate(today)} · ${describe(it)}" } ?: " ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("heatmap-day"))
            }
            Spacer(Modifier.height(AppSpacing.m))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(fewer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.size(AppSizes.dotXLarge).clip(AppShapes.tiny).background(empty))
                (1..4).forEach { Box(Modifier.size(AppSizes.dotXLarge).clip(AppShapes.tiny).background(color.copy(alpha = heatShade(it)))) }
                Text(more, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private const val MIN_WEEKS = 8
private const val MAX_WEEKS = 53
private val MIN_CELL = 14.dp
private val MAX_CELL = 22.dp

/** A busy day's shade: even the quietest stands well clear of an empty day, in dark themes too. */
private fun heatShade(level: Int): Float = 0.4f + 0.6f * level / 4f

private val WEEKDAY_WIDTH = 28.dp
private val MONTH_ROW_HEIGHT = 16.dp

/** Every other weekday is named, from Monday, so the names fit the rows. */
private val WEEKDAY_NAMES = listOf("Mon", null, "Wed", null, "Fri", null, null)
private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
