package com.idiomiko.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppBorders
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSizes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * A card of recent days as squares, darker the busier the day: a column per week from Monday, as
 * many weeks as fit, up to a year, or the weeks of a [range] such as a calendar year. The weeks shown depend on the width, so [subtitle] and [levels]
 * are given the first day shown; [levels] maps each busy day to 1 to 4, days left out being empty.
 */
@Composable
fun ActivityHeatmapCard(
    title: String,
    subtitle: (start: LocalDate) -> String?,
    today: LocalDate,
    levels: (start: LocalDate) -> Map<LocalDate, Int>,
    fewer: String,
    more: String,
    icon: ImageVector = AppIcons.BarChart,
    color: Color = AppAccents.green,
    /** What a day held, shown in a tooltip over the square pointed at or tapped. */
    describe: ((LocalDate) -> String)? = null,
    /** A way on from the card, at its header's end. */
    action: (@Composable RowScope.() -> Unit)? = null,
    /**
     * The days to show, such as a calendar year, instead of the recent weeks that fit: their weeks fill the width,
     * days outside them left blank, and where they do not fit the squares scroll sideways, the latest in view.
     */
    range: ClosedRange<LocalDate>? = null,
    /** Given a least height, as beside a taller column, the card grows to it. */
    modifier: Modifier = Modifier,
) {
    val empty = MaterialTheme.colorScheme.onSurface.copy(alpha = AppAlpha.wash)
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outlineVariant
    var pointed by remember { mutableStateOf<LocalDate?>(null) }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val weekdayStyle = MaterialTheme.typography.labelSmall
    // As wide as the longest weekday name, so large text does not run the names into the squares.
    val weekdayWidth = with(density) { WEEKDAY_NAMES.filterNotNull().maxOf { measurer.measure(tr(it), weekdayStyle).size.width }.toDp() }.coerceAtLeast(WEEKDAY_WIDTH)
    BoxWithConstraints(modifier.fillMaxWidth(), propagateMinConstraints = true) {
        // Whole pixels: a fraction rounded up in every square adds up, across a year of weeks, to cutting the last one.
        val gap = with(density) { AppSpacing.xs.toPx().toInt().toDp() }
        // The card's padding takes 20 on each side, and the weekday names a column of their own.
        val inner = maxWidth - 40.dp - weekdayWidth - gap
        val weeks = if (range != null) {
            range.start.minus(range.start.dayOfWeek.ordinal, DateTimeUnit.DAY).daysUntil(range.endInclusive) / 7 + 1
        } else {
            ((inner + gap) / (MIN_CELL + gap)).toInt().coerceIn(MIN_WEEKS, MAX_WEEKS)
        }
        val fitted = (inner - gap * (weeks - 1)) / weeks
        // A range too long for the width keeps its squares readable and scrolls.
        val scrolls = range != null && fitted < MIN_CELL
        val cell: Dp = with(density) { (if (range != null) fitted.coerceIn(MIN_CELL, MAX_CELL) else fitted.coerceAtMost(MAX_CELL)).toPx().toInt().toDp() }
        val thisMonday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
        val start = range?.start?.let { it.minus(it.dayOfWeek.ordinal, DateTimeUnit.DAY) } ?: thisMonday.minus((weeks - 1) * 7, DateTimeUnit.DAY)
        val sideways = rememberScrollState()
        LaunchedEffect(range, scrolls) {
            if (scrolls && range != null && today in range) snapshotFlow { sideways.maxValue }.first { it > 0 }.let { sideways.scrollTo(it) }
        }
        val shown = levels(start)
        ContentCard(title, subtitle(start), icon = icon, headerExtra = { action?.invoke(this) }) {
            val labelStyle = MaterialTheme.typography.labelSmall
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            // A month is named over the week its first day falls in; the first week is named only when no other is near it.
            val monthOf: (Int) -> Int? = { week ->
                // Only the range's own days: a year's last week runs into the next January.
                val days = (0 until 7).map { start.plus(week * 7 + it, DateTimeUnit.DAY) }.filter { range == null || it in range }
                days.firstOrNull { it.day == 1 }?.month?.ordinal
                    ?: if (week == 0 && days.isNotEmpty() && (1..2).none { w -> (0 until 7).any { start.plus(w * 7 + it, DateTimeUnit.DAY).day == 1 } }) days.first().month.ordinal else null
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Column(Modifier.width(weekdayWidth), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Spacer(Modifier.height(MONTH_ROW_HEIGHT))
                    repeat(7) { weekday ->
                        Box(Modifier.height(cell), contentAlignment = Alignment.CenterStart) {
                            WEEKDAY_NAMES[weekday]?.let { Text(tr(it), style = labelStyle, color = labelColor, maxLines = 1, softWrap = false) }
                        }
                    }
                }
                Column(
                    Modifier.weight(1f, fill = false).then(if (scrolls) Modifier.horizontalScroll(sideways) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    Row(Modifier.height(MONTH_ROW_HEIGHT), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(weeks) { week ->
                            Box(Modifier.width(cell)) {
                                monthOf(week)?.let { month ->
                                    Text(tr(MONTH_NAMES[month]), Modifier.wrapContentWidth(Alignment.Start, unbounded = true), style = labelStyle, color = labelColor, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                    Box {
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
                                                else start.plus(week * 7 + weekday, DateTimeUnit.DAY).takeIf { it <= today && (range == null || it in range) }
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
                                        // Days still to come show as blank squares in an outline, so the rest of the year is in view.
                                        val ahead = date > today && (range == null || date in range)
                                        val shade = when {
                                            ahead -> surface
                                            range != null && date !in range -> Color.Transparent
                                            date > today -> Color.Transparent
                                            level == 0 -> empty
                                            else -> color.copy(alpha = heatShade(level))
                                        }
                                        Box(Modifier.size(cell).clip(AppShapes.tiny).background(shade).then(if (ahead) Modifier.border(AppBorders.hairline(outline), AppShapes.tiny) else Modifier))
                                    }
                                }
                            }
                        }
                        val day = pointed
                        if (describe != null && day != null) {
                            val offset = start.daysUntil(day)
                            val step = with(density) { (cell + gap).roundToPx() }
                            ChartTooltip("${day.shortDate(today)} · ${describe(day)}", IntRect(IntOffset(offset / 7 * step, offset % 7 * step), IntSize(with(density) { cell.roundToPx() }, with(density) { cell.roundToPx() })))
                        }
                    }
                }
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

/** The year shown, between small buttons to the one before and the one after. */
@Composable
fun YearSwitch(year: Int, canBack: Boolean, canForward: Boolean, onYear: (Int) -> Unit) {
    // An arrow shows only where there is a year to go to; while the other one shows, its place is kept so the year stays put.
    val either = canBack || canForward
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
        if (canBack) AppIconButton(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft, tr("Previous year"), { onYear(year - 1) },
            size = IconButtonSize.Small, style = IconButtonStyle.Outlined, modifier = Modifier.testTag("calendar-previous-year"),
        ) else if (either) Spacer(Modifier.size(IconButtonSize.Small.box))
        Text("$year", style = AppText.labelStrong, modifier = Modifier.testTag("calendar-year"))
        if (canForward) AppIconButton(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, tr("Next year"), { onYear(year + 1) },
            size = IconButtonSize.Small, style = IconButtonStyle.Outlined, modifier = Modifier.testTag("calendar-next-year"),
        ) else if (either) Spacer(Modifier.size(IconButtonSize.Small.box))
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
