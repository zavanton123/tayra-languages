package com.tayra.languages.core.data.speech

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** A recording of an answer ends after a pause once something was said, after a long silence, or at once on a muted microphone. */
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
    fun aPauseAfterSpeakingEndsIt() {
        val endpoint = SpeechEndpoint()
        val states = endpoint.feed(List(5) { chunk(30) } + List(15) { chunk(4_000) } + List(12) { chunk(30) })
        assertEquals(SpeechEndpoint.State.SAID, states.last())
        assertEquals(SpeechEndpoint.State.LISTENING, states[states.size - 2], "a pause of 1.1 s is not yet the end")
        assertEquals(true, endpoint.spoke)
    }

    @Test
    fun anAnswerBegunAtOnceIsHeard() {
        val states = SpeechEndpoint().feed(List(15) { chunk(4_000) } + List(12) { chunk(30) })
        assertEquals(SpeechEndpoint.State.SAID, states.last())
    }

    @Test
    fun nothingSaidEndsItAfterAWhile() {
        val states = SpeechEndpoint().feed(List(70) { chunk(30) })
        assertEquals(SpeechEndpoint.State.SILENT, states.last())
        assertEquals(SpeechEndpoint.State.LISTENING, states[68])
    }

    @Test
    fun aMutedMicrophoneIsToldApart() {
        val states = SpeechEndpoint().feed(List(10) { chunk(0) })
        assertEquals(SpeechEndpoint.State.MUTED, states.last())
        assertEquals(SpeechEndpoint.State.LISTENING, states[8])
    }

    @Test
    fun aLongAnswerIsCutAtTheLimit() {
        val states = SpeechEndpoint().feed(List(200) { chunk(4_000) })
        assertEquals(SpeechEndpoint.State.SAID, states.last())
        assertEquals(SpeechEndpoint.State.LISTENING, states[198])
    }
}
