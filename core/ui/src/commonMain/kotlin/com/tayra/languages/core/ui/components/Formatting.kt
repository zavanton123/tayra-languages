package com.tayra.languages.core.ui.components

import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.i18n.tr
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

fun Instant.formatDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val dt = toLocalDateTime(timeZone)
    return "${dt.year}-${dt.month.ordinal.plus(1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}
