package com.tayra.languages.core.ui.components

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** "3 days ago" style relative time. */
fun Instant.relativeTo(now: Instant = Clock.System.now()): String {
    val seconds = (now - this).inWholeSeconds
    if (seconds < 0) return "just now"
    val units = listOf("year" to 365L * 24 * 3600, "month" to 30L * 24 * 3600, "week" to 7L * 24 * 3600, "day" to 24L * 3600, "hour" to 3600L, "minute" to 60L)
    for ((name, size) in units) {
        val n = seconds / size
        if (n >= 1) return "$n $name${if (n > 1) "s" else ""} ago"
    }
    return "just now"
}

fun Instant.formatDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val dt = toLocalDateTime(timeZone)
    return "${dt.year}-${dt.month.ordinal.plus(1).toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
}
