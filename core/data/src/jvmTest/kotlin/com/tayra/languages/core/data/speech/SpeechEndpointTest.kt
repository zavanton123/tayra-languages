package com.tayra.languages.core.data.speech

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A recording of an answer goes on through any silence: only a muted microphone or the limit ends it. */
class SpeechEndpointTest {
    private val random = Random(1)

    /** A tenth of a second at [level]: a tone for speech, faint noise for a quiet room, zeros for a muted microphone. */
    private fun chunk(level: Int, tone: Boolean = level > 100): ByteArray {
        val bytes = ByteArray(3_200)
        for (i in 0 until 1_600) {
            val sample = when {
                level == 0 -> 0
                tone -> (level * 1.4 * sin(2 * PI * 220 * i / 16_000.0)).toInt()
                else -> random.nextInt(-level, level + 1)
            }
            bytes[2 * i] = sample.toByte()
            bytes[2 * i + 1] = (sample shr 8).toByte()
        }
        return bytes
    }

    /** Feeds [chunks] in turn and returns the state after each. */
    private fun SpeechEndpoint.feed(chunks: List<ByteArray>) = chunks.map { add(it, it.size) }

    @Test
    fun aLongPauseAfterSpeakingDoesNotEndIt() {
        val states = SpeechEndpoint().feed(List(15) { chunk(4_000) } + List(300) { chunk(30) } + List(15) { chunk(4_000) })
        assertTrue(states.all { it == SpeechEndpoint.State.LISTENING })
    }

    @Test
    fun nothingSaidDoesNotEndIt() {
        val states = SpeechEndpoint().feed(List(600) { chunk(30) })
        assertTrue(states.all { it == SpeechEndpoint.State.LISTENING })
    }

    @Test
    fun aMutedMicrophoneIsToldApart() {
        val states = SpeechEndpoint().feed(List(10) { chunk(0) })
        assertEquals(SpeechEndpoint.State.MUTED, states.last())
        assertEquals(SpeechEndpoint.State.LISTENING, states[8])
    }

    @Test
    fun aMicrophoneThatGoesQuietAfterSoundIsNotMuted() {
        val states = SpeechEndpoint().feed(List(5) { chunk(30) } + List(50) { chunk(0) })
        assertTrue(states.all { it == SpeechEndpoint.State.LISTENING })
    }

    @Test
    fun aForgottenStopEndsItAtTheLimit() {
        val states = SpeechEndpoint().feed(List(1_200) { chunk(30) })
        assertEquals(SpeechEndpoint.State.FULL, states.last())
        assertEquals(SpeechEndpoint.State.LISTENING, states[1_198])
    }
}
