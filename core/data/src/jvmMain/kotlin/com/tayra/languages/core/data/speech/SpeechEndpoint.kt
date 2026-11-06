package com.tayra.languages.core.data.speech

/**
 * What ends a recording of a spoken answer, from its 16-bit little-endian samples as they come,
 * besides the learner pressing Stop: silence or pauses never do. It ends at once when the
 * microphone gives only zeros, as one the system keeps from the app does, and at [LIMIT_MS] when
 * Stop was forgotten.
 */
internal class SpeechEndpoint(private val sampleRate: Int = 16_000) {
    enum class State { LISTENING, FULL, MUTED }

    private var samples = 0L
    private var anySound = false

    fun add(bytes: ByteArray, length: Int): State {
        val count = length / 2
        if (count == 0) return State.LISTENING
        for (i in 0 until count) {
            if (bytes[2 * i] != 0.toByte() || bytes[2 * i + 1] != 0.toByte()) anySound = true
        }
        samples += count
        val elapsedMs = samples * 1000 / sampleRate
        return when {
            !anySound && elapsedMs >= MUTED_MS -> State.MUTED
            elapsedMs >= LIMIT_MS -> State.FULL
            else -> State.LISTENING
        }
    }

    private companion object {
        const val LIMIT_MS = 120_000
        const val MUTED_MS = 1_000
    }
}
