package com.tayra.languages.core.ui.components

import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.i18n.tr
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** "3 days ago" style relative time. */
fun Instant.relativeTo(now: Instant = Clock.System.now()): String {
    val seconds = (now - this).inWholeSeconds
    if (seconds < 0) return tr("just now")
    val n = { size: Long -> (seconds / size).toInt() }
    return when {
        n(YEAR) >= 1 -> trPlural(n(YEAR), "{0} year ago", "{0} years ago")
        n(MONTH) >= 1 -> trPlural(n(MONTH), "{0} month ago", "{0} months ago")
        n(WEEK) >= 1 -> trPlural(n(WEEK), "{0} week ago", "{0} weeks ago")
        n(DAY) >= 1 -> trPlural(n(DAY), "{0} day ago", "{0} days ago")
        n(HOUR) >= 1 -> trPlural(n(HOUR), "{0} hour ago", "{0} hours ago")
        n(MINUTE) >= 1 -> trPlural(n(MINUTE), "{0} minute ago", "{0} minutes ago")
        else -> tr("just now")
    }
}

private const val MINUTE = 60L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR
private const val WEEK = 7 * DAY
private const val MONTH = 30 * DAY
private const val YEAR = 365 * DAY

/** A date as the interface language writes it, "Oct 7, 2026". */
fun Instant.formatDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): String = toLocalDateTime(timeZone).date.shortDateWithYear()

private val monthKeys = listOf("Jan {0}", "Feb {0}", "Mar {0}", "Apr {0}", "May {0}", "Jun {0}", "Jul {0}", "Aug {0}", "Sep {0}", "Oct {0}", "Nov {0}", "Dec {0}")

/** "Oct 7", or as the interface language writes a short date. */
fun LocalDate.shortDate(): String = tr(monthKeys[month.ordinal], day)

private val monthYearKeys = listOf("Jan {0}, {1}", "Feb {0}, {1}", "Mar {0}, {1}", "Apr {0}, {1}", "May {0}, {1}", "Jun {0}, {1}", "Jul {0}, {1}", "Aug {0}, {1}", "Sep {0}, {1}", "Oct {0}, {1}", "Nov {0}, {1}", "Dec {0}, {1}")

/** A short date, with the year when it is not [today]'s. */
fun LocalDate.shortDate(today: LocalDate): String = if (year == today.year) shortDate() else shortDateWithYear()

/** "Oct 7, 2026", or as the interface language writes a short date with its year. */
fun LocalDate.shortDateWithYear(): String = tr(monthYearKeys[month.ordinal], day, year)
