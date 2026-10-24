package com.tayra.languages.core.data.speech

import kotlin.math.sqrt

/**
 * When a recording of a spoken answer ends, from its 16-bit little-endian samples as they come:
 * after a pause once something was said, after a while with nothing said, or at once when the
 * microphone gives only zeros, as one the system keeps from the app does.
 */
internal class SpeechEndpoint(private val sampleRate: Int = 16_000) {
    enum class State { LISTENING, SAID, SILENT, MUTED }

    private var samples = 0L
    private var anySound = false
    private var floor = Double.MAX_VALUE
    private var loudMs = 0
    private var quietMs = 0

    /** Something was said: the recording is worth hearing. */
    val spoke: Boolean get() = loudMs >= SPEECH_MS

    fun add(bytes: ByteArray, length: Int): State {
        val count = length / 2
        if (count == 0) return State.LISTENING
        var sum = 0.0
        for (i in 0 until count) {
            val sample = ((bytes[2 * i + 1].toInt() shl 8) or (bytes[2 * i].toInt() and 0xFF)).toShort().toInt()
            if (sample != 0) anySound = true
            sum += sample.toDouble() * sample
        }
        val level = sqrt(sum / count)
        val chunkMs = (count * 1000L / sampleRate).toInt()
        samples += count
        val elapsedMs = samples * 1000 / sampleRate
        // The quietest moment at the start is the room; speech stands well above it, though not so far
        // above that an answer begun at once, which makes the start loud, goes unheard.
        if (elapsedMs <= FLOOR_MS) floor = minOf(floor, level)
        val loud = level > (floor * FLOOR_FACTOR).coerceIn(MIN_SPEECH_LEVEL, MAX_SPEECH_LEVEL)
        if (loud) {
            loudMs += chunkMs
            quietMs = 0
        } else {
            quietMs += chunkMs
        }
        return when {
            !anySound && elapsedMs >= MUTED_MS -> State.MUTED
            spoke && quietMs >= PAUSE_MS -> State.SAID
            elapsedMs >= LIMIT_MS -> if (spoke) State.SAID else State.SILENT
            !spoke && elapsedMs >= WAIT_MS -> State.SILENT
            else -> State.LISTENING
        }
    }

    private companion object {
        const val FLOOR_MS = 300
        const val FLOOR_FACTOR = 3.0
        const val MIN_SPEECH_LEVEL = 500.0
        const val MAX_SPEECH_LEVEL = 1_500.0
        const val SPEECH_MS = 200
        const val PAUSE_MS = 1_200
        const val WAIT_MS = 7_000
        const val LIMIT_MS = 20_000
        const val MUTED_MS = 1_000
    }
}
