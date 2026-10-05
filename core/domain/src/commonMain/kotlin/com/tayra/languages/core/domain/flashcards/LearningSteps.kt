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
