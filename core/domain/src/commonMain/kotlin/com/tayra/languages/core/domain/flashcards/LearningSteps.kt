package com.tayra.languages.core.domain.flashcards

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The waits of the learning steps as typed in settings: "1m 10m", "30s 5m 1h", "1d"; a bare number is minutes. */
object LearningSteps {
    private val STEP = Regex("""(\d+(?:\.\d+)?)\s*([smhd]?)""", RegexOption.IGNORE_CASE)

    /** The steps in [text], or null when part of it is not a step. */
    fun parse(text: String): List<Duration>? {
        val parts = text.trim().split(Regex("""[\s,;]+""")).filter { it.isNotEmpty() }
        return parts.map { part ->
            val match = STEP.matchEntire(part) ?: return null
            val amount = match.groupValues[1].toDouble()
            val step = when (match.groupValues[2].lowercase()) {
                "s" -> amount.seconds
                "h" -> amount.hours
                "d" -> amount.days
                else -> amount.minutes
            }
            if (step <= Duration.ZERO) return null
            step
        }
    }

    /** The steps in [text], or [fallback]'s when it cannot be read. */
    fun parseOr(text: String, fallback: String): List<Duration> = parse(text) ?: parse(fallback).orEmpty()

    fun format(steps: List<Duration>): String = steps.joinToString(" ") { step ->
        val seconds = step.inWholeSeconds
        when {
            seconds % 86_400 == 0L -> "${seconds / 86_400}d"
            seconds % 3_600 == 0L -> "${seconds / 3_600}h"
            seconds % 60 == 0L -> "${seconds / 60}m"
            else -> "${seconds}s"
        }
    }
}

/** How long until a card comes back, for the answer buttons: "<1m", "<10m", "3h", "4d", "1.2mo", "2.1y". */
fun waitLabel(wait: Duration): String {
    val minutes = wait.inWholeSeconds / 60.0
    val days = minutes / 1440.0
    fun oneDecimal(value: Double): String {
        val tenths = kotlin.math.round(value * 10).toLong()
        return if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
    }
    return when {
        minutes < 1 -> "<1m"
        minutes < 60 -> "<${kotlin.math.ceil(minutes).toInt()}m"
        days < 1 -> "${oneDecimal(minutes / 60)}h"
        days < 30.5 -> "${kotlin.math.round(days).toInt()}d"
        days < 365 -> "${oneDecimal(days / 30.4)}mo"
        else -> "${oneDecimal(days / 365.25)}y"
    }
}
