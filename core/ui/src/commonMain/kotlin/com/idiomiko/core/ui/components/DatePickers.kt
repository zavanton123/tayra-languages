package com.idiomiko.core.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.idiomiko.core.ui.i18n.tr
import com.idiomiko.core.ui.theme.AppShapes
import kotlinx.datetime.LocalDate

/**
 * A calendar to pick a day, or with [range] a first and a last day, between [earliest] and [latest]; [selected] is
 * shown picked to begin with. [onPick] hears the days chosen, the same one twice for a single day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    selected: ClosedRange<LocalDate>,
    earliest: LocalDate,
    latest: LocalDate,
    range: Boolean,
    onPick: (first: LocalDate, last: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectable = remember(earliest, latest) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis.toDate() in earliest..latest
            override fun isSelectableYear(year: Int) = year in earliest.year..latest.year
        }
    }
    val years = earliest.year..latest.year
    if (range) {
        val state = rememberDateRangePickerState(selected.start.toMillis(), selected.endInclusive.toMillis(), yearRange = years, selectableDates = selectable)
        val first = state.selectedStartDateMillis
        val last = state.selectedEndDateMillis ?: first
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { QuietButton(tr("OK"), onClick = { if (first != null && last != null) onPick(first.toDate(), last.toDate()); onDismiss() }, enabled = first != null, modifier = Modifier.testTag("date-picker-ok")) },
            dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
            shape = AppShapes.large,
            modifier = Modifier.testTag("date-picker"),
        ) {
            DateRangePicker(state, title = null, headline = null, showModeToggle = false)
        }
    } else {
        val state = rememberDatePickerState(selected.start.toMillis(), yearRange = years, selectableDates = selectable)
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                QuietButton(tr("OK"), onClick = { state.selectedDateMillis?.toDate()?.let { onPick(it, it) }; onDismiss() }, enabled = state.selectedDateMillis != null, modifier = Modifier.testTag("date-picker-ok"))
            },
            dismissButton = { QuietButton(tr("Cancel"), onClick = onDismiss) },
            shape = AppShapes.large,
            modifier = Modifier.testTag("date-picker"),
        ) {
            DatePicker(state, title = null, showModeToggle = false)
        }
    }
}

/** The calendar counts days as midnights in UTC. */
private fun LocalDate.toMillis(): Long = toEpochDays().toLong() * MILLIS_A_DAY

private fun Long.toDate(): LocalDate = LocalDate.fromEpochDays((this / MILLIS_A_DAY).toInt())

private const val MILLIS_A_DAY = 86_400_000L
