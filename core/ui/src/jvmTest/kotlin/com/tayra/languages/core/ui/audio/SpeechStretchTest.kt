package com.tayra.languages.core.ui.audio

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/** Speech played faster or slower takes that much less or more time, and keeps its pitch. */
class SpeechStretchTest {
    private val rate = 22_050

    /** A second of a voice-like tone at [hz]. */
    private fun tone(hz: Double, seconds: Double = 1.0) = ShortArray((rate * seconds).toInt()) { i -> (8_000 * sin(2 * PI * hz * i / rate)).toInt().toShort() }

    /** The tone's pitch, from how often it crosses zero. */
    private fun pitch(samples: ShortArray): Double {
        var crossings = 0
        for (i in 1 until samples.size) if ((samples[i - 1] < 0) != (samples[i] < 0)) crossings++
        return crossings / 2.0 / (samples.size.toDouble() / rate)
    }

    @Test
    fun theLengthFollowsTheSpeedAndThePitchStays() {
        val voice = tone(180.0)
        for (speed in listOf(0.5f, 0.75f, 1.25f, 1.5f, 2f)) {
            val out = SpeechStretch.stretch(voice, rate, speed)
            val expected = voice.size / speed
            assertTrue(abs(out.size - expected) / expected < 0.05, "at $speed: ${out.size} samples for $expected")
            assertTrue(abs(pitch(out) - 180.0) < 6, "at $speed the pitch is ${pitch(out)}")
        }
    }

    @Test
    fun aWavIsStretchedAndOneOfAnotherKindIsLeftAlone() {
        val voice = tone(180.0)
        val pcm = ByteArray(voice.size * 2).also { b -> voice.forEachIndexed { i, s -> b[2 * i] = s.toInt().toByte(); b[2 * i + 1] = (s.toInt() shr 8).toByte() } }
        val format = AudioFormat(rate.toFloat(), 16, 1, true, false)
        val wav = ByteArrayOutputStream().also { AudioSystem.write(AudioInputStream(ByteArrayInputStream(pcm), format, voice.size.toLong()), AudioFileFormat.Type.WAVE, it) }.toByteArray()
        val faster = SpeechStretch.wav(wav, 1.5f)
        val frames = AudioSystem.getAudioInputStream(ByteArrayInputStream(faster)).frameLength
        assertTrue(abs(frames - voice.size / 1.5) < voice.size * 0.05, "$frames frames")
        assertContentEquals(wav, SpeechStretch.wav(wav, 1f), "normal speed is played as it is")
        val notWav = "not audio".encodeToByteArray()
        assertContentEquals(notWav, SpeechStretch.wav(notWav, 1.5f))
    }
}
