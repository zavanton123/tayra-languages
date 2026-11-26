package com.idiomiko.feature.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.platform.testTag
import com.idiomiko.core.ui.components.AppBottomSheet
import com.idiomiko.core.ui.components.AppDatePickerDialog
import com.idiomiko.core.ui.components.AppDialog
import com.idiomiko.core.ui.components.AppIconButton
import com.idiomiko.core.ui.components.AppSwitch
import com.idiomiko.core.ui.components.LinkText
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.PrimaryButton
import com.idiomiko.core.ui.components.QuietButton
import com.idiomiko.core.ui.components.SelectableChip
import com.idiomiko.core.ui.components.shortDate
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.i18n.trPlural
import com.idiomiko.core.ui.theme.AppAlpha
import com.idiomiko.core.ui.theme.AppShapes
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.AppText
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.time.Instant

/** A quick choice of when the cards come up: [days] from today, one day or a span to spread them over. */
private class DueChoice(val label: String, val days: IntRange)

private fun singleChoices() = listOf(
    DueChoice(tr("Today"), 0..0),
    DueChoice(tr("Tomorrow"), 1..1),
    DueChoice(trPlural(3, "In {0} day", "In {0} days"), 3..3),
    DueChoice(tr("In a week"), 7..7),
    DueChoice(tr("In 2 weeks"), 14..14),
    DueChoice(tr("In a month"), 30..30),
)

private fun spreadChoices() = listOf(
    DueChoice(tr("Next 3 days"), 1..3),
    DueChoice(tr("Next week"), 1..7),
    DueChoice(tr("Next 2 weeks"), 1..14),
    DueChoice(tr("Next month"), 1..30),
)

/**
 * When the [count] cards chosen come up next, as Anki's Set due date sets them: a quick choice or a day from the
 * calendar, or spread over a span of days, with the dates it comes to spelled out. [today] is the study day now;
 * [single] is the card when there is only one, to show when it is due now. A sheet on a phone, a dialog elsewhere.
 */
@Composable
internal fun SetDueDialog(count: Int, today: LocalDate, single: BrowseRow?, now: Instant, onSet: (IntRange) -> Unit, onDismiss: () -> Unit) {
    var days by remember { mutableStateOf(1..1) }
    var spread by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val title = trPlural(count, "Set the due date of {0} card", "Set the due date of {0} cards")
    val set = { onSet(days); onDismiss() }
    val content = @Composable {
        DueChoices(
            count, today, single, now, days, spread,
            onDays = { days = it },
            onSpread = { on ->
                spread = on
                // The span starts where the day chosen was, a week long; back to one day, it keeps its start.
                days = if (on) days.first.coerceAtLeast(1).let { it..it + SPREAD_DEFAULT_DAYS - 1 } else days.first..days.first
            },
            onPick = { picking = true },
        )
    }
    if (LocalWindowWidth.current.isCompact) {
        AppBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("set-due-dialog")) {
            Column(Modifier.padding(horizontal = AppSpacing.l).padding(bottom = AppSpacing.l), verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = AppText.headline)
                    AppIconButton(Icons.Default.Close, tr("Close"), onDismiss)
                }
                content()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.s, Alignment.End)) {
                    QuietButton(tr("Cancel"), onClick = onDismiss)
                    PrimaryButton(tr("Set"), onClick = set, modifier = Modifier.testTag("set-due-confirm"))
                }
            }
        }
    } else {
        AppDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag("set-due-dialog"),
            title = { Text(title) },
            text = content,
            confirmButton = { PrimaryButton(tr("Set"), onClick = set, modifier = Modifier.testTag("set-due-confirm")) },
            dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
        )
    }
    if (picking) {
        AppDatePickerDialog(
            selected = today.plus(days.first, DateTimeUnit.DAY)..today.plus(days.last, DateTimeUnit.DAY),
            earliest = today,
            latest = today.plus(MAX_DUE_DAYS, DateTimeUnit.DAY),
            range = spread,
            onPick = { first, last -> days = today.daysUntil(first)..today.daysUntil(last) },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun DueChoices(
    count: Int,
    today: LocalDate,
    single: BrowseRow?,
    now: Instant,
    days: IntRange,
    spread: Boolean,
    onDays: (IntRange) -> Unit,
    onSpread: (Boolean) -> Unit,
    onPick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
        single?.let { row ->
            Text(
                tr("Currently due: {0}", dueLabel(row, now)) + if (row.intervalDays > 0) " · " + tr("Interval {0}", intervalLabel(row)) else "",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.testTag("set-due-current"),
            )
        }
        val choices = if (spread) spreadChoices() else singleChoices()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s), verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
            choices.forEach { choice -> SelectableChip(choice.label, selected = days == choice.days, onClick = { onDays(choice.days) }, modifier = Modifier.testTag("due-${choice.days.first}-${choice.days.last}")) }
            // A day picked from the calendar shows as a choice of its own.
            if (choices.none { it.days == days }) SelectableChip(span(today, days), selected = true, onClick = onPick, modifier = Modifier.testTag("due-picked"))
        }
        LinkText(if (spread) tr("Pick the days…") else tr("Pick a date…"), onClick = onPick, icon = Icons.Default.DateRange, modifier = Modifier.testTag("due-calendar"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Spread over several days"), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            AppSwitch(spread, onCheckedChange = onSpread, modifier = Modifier.testTag("due-spread"))
        }
        // What the choice comes to, in dates.
        Column(
            Modifier.fillMaxWidth().clip(AppShapes.panel).background(colors.primary.copy(alpha = AppAlpha.wash)).padding(AppSpacing.l).testTag("due-summary"),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
        ) {
            Text(if (days.first == days.last) tr("Due {0}", day(today, days.first)) else span(today, days, weekdays = true), style = AppText.itemTitle)
            val length = days.last - days.first + 1
            Text(
                when {
                    days.first == days.last -> relative(days.first)
                    count >= length -> trPlural(length, "{0} day", "{0} days") + " · " + trPlural((count + length - 1) / length, "About {0} card a day", "About {0} cards a day")
                    else -> trPlural(length, "{0} day", "{0} days")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** "Thu, Nov 27": the weekday with the date. */
private fun day(today: LocalDate, offset: Int): String {
    val date = today.plus(offset, DateTimeUnit.DAY)
    return tr(WEEKDAYS[date.dayOfWeek.ordinal]) + ", " + date.shortDate(today)
}

/** The days [days] from [today] as "Nov 27 – Dec 3", with their weekdays when [weekdays]. */
private fun span(today: LocalDate, days: IntRange, weekdays: Boolean = false): String {
    fun at(offset: Int) = if (weekdays) day(today, offset) else today.plus(offset, DateTimeUnit.DAY).shortDate(today)
    return if (days.first == days.last) at(days.first) else at(days.first) + " – " + at(days.last)
}

private fun relative(offset: Int): String = when (offset) {
    0 -> tr("Today")
    1 -> tr("Tomorrow")
    else -> trPlural(offset, "In {0} day", "In {0} days")
}

private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** A spread starts out a week long. */
private const val SPREAD_DEFAULT_DAYS = 7

/** A hundred years: as far as a card can be put off. */
private const val MAX_DUE_DAYS = 36500
