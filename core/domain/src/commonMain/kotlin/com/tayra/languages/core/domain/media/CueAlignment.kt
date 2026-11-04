package com.tayra.languages.core.domain.media

/**
 * Pairs the lines of a translation track with the lines of the original by the time they are on
 * screen together. A translated line goes to the original line it overlaps most, when that overlap
 * is over half a second or covers most of the translated line; several translated lines falling on
 * one original line are joined.
 */
object CueAlignment {
    private const val MIN_OVERLAP_MS = 500L
    private const val MIN_SHARE = 0.8

    fun align(original: List<Cue>, translation: List<Cue>): List<Cue> {
        if (original.isEmpty() || translation.isEmpty()) return original
        val parts = Array(original.size) { mutableListOf<String>() }
        var start = 0
        for (t in translation.sortedBy { it.beginMs }) {
            // Originals ending before this line can't overlap it or anything after it.
            while (start < original.size && original[start].endMs <= t.beginMs) start++
            var best = -1
            var bestOverlap = 0L
            var i = start
            while (i < original.size && original[i].beginMs < t.endMs) {
                val overlap = original[i].overlapMs(t)
                if (overlap > bestOverlap) {
                    best = i
                    bestOverlap = overlap
                }
                i++
            }
            if (best < 0) continue
            val share = if (t.durationMs == 0L) 0.0 else bestOverlap.toDouble() / t.durationMs
            if (bestOverlap > MIN_OVERLAP_MS || share >= MIN_SHARE) parts[best] += t.text
        }
        return original.mapIndexed { i, cue ->
            if (parts[i].isEmpty()) cue else cue.copy(translation = parts[i].joinToString(" "))
        }
    }

    /** [translations] given line by line, as YouTube returns them for the same track; null when the counts differ. */
    fun byIndex(original: List<Cue>, translations: List<String>): List<Cue>? =
        if (translations.size != original.size) null
        else original.mapIndexed { i, cue -> cue.copy(translation = translations[i].ifBlank { null }) }
}
