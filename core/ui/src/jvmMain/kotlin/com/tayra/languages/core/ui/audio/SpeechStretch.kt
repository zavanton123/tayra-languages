package com.tayra.languages.core.ui.audio

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Speech made faster or slower without changing its pitch, as Bill Cox's Sonic does it: the voice's
 * pitch period is found, then whole periods are dropped (faster) or repeated (slower), each joined
 * to its neighbour by a crossfade. Java's sound has no speed of its own, so the desktop plays speech
 * made at normal speed through this.
 */
internal object SpeechStretch {
    /** [wav] at [speed], or as it is when it is not 16-bit mono PCM, or the speed is normal. */
    fun wav(wav: ByteArray, speed: Float): ByteArray {
        if (abs(speed - 1f) < 0.01f) return wav
        return runCatching {
            val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(wav))
            val format = stream.format
            if (format.encoding != AudioFormat.Encoding.PCM_SIGNED || format.sampleSizeInBits != 16 || format.channels != 1) return wav
            val bytes = stream.readAllBytes()
            val samples = ShortArray(bytes.size / 2) { i ->
                if (format.isBigEndian) ((bytes[2 * i].toInt() shl 8) or (bytes[2 * i + 1].toInt() and 0xFF)).toShort()
                else ((bytes[2 * i + 1].toInt() shl 8) or (bytes[2 * i].toInt() and 0xFF)).toShort()
            }
            val out = stretch(samples, format.sampleRate.roundToInt(), speed)
            val pcm = ByteArray(out.size * 2)
            for (i in out.indices) {
                pcm[2 * i] = out[i].toInt().toByte()
                pcm[2 * i + 1] = (out[i].toInt() shr 8).toByte()
            }
            val little = AudioFormat(format.sampleRate, 16, 1, true, false)
            ByteArrayOutputStream().also { AudioSystem.write(AudioInputStream(ByteArrayInputStream(pcm), little, out.size.toLong()), AudioFileFormat.Type.WAVE, it) }.toByteArray()
        }.getOrDefault(wav)
    }

    /** [samples] at [sampleRate] played [speed] times as fast, the pitch kept. */
    fun stretch(samples: ShortArray, sampleRate: Int, speed: Float): ShortArray {
        val minPeriod = sampleRate / MAX_PITCH
        val maxPeriod = sampleRate / MIN_PITCH
        val required = 2 * maxPeriod
        if (samples.size < required) return samples
        val out = ShortArrayBuilder(samples.size)
        var position = 0
        var toCopy = 0
        while (position + required <= samples.size) {
            if (toCopy > 0) {
                val n = minOf(toCopy, required)
                out.append(samples, position, n)
                position += n
                toCopy -= n
                continue
            }
            val period = pitchPeriod(samples, position, minPeriod, maxPeriod)
            if (speed > 1f) {
                // Two periods become one: the first fading out into the second.
                val made = if (speed >= 2f) (period / (speed - 1f)).toInt() else period.also { toCopy = (period * (2f - speed) / (speed - 1f)).toInt() }
                out.crossfade(samples, position, position + period, made)
                position += period + made
            } else {
                // One period is kept, then made again: the next fading into it.
                val made = if (speed < 0.5f) (period * speed / (1f - speed)).toInt().coerceAtLeast(1) else period.also { toCopy = (period * (2f * speed - 1f) / (1f - speed)).toInt() }
                out.append(samples, position, period)
                out.crossfade(samples, position + period, position, made)
                position += made
            }
        }
        out.append(samples, position, samples.size - position)
        return out.toArray()
    }

    /** The period, in samples, whose two neighbouring copies at [start] differ least for their length: the voice's pitch. */
    private fun pitchPeriod(samples: ShortArray, start: Int, minPeriod: Int, maxPeriod: Int): Int {
        var best = minPeriod
        var bestDiff = 1L
        for (period in minPeriod..maxPeriod) {
            var diff = 0L
            for (i in 0 until period) diff += abs(samples[start + i] - samples[start + i + period])
            // Compared per sample, so a longer period does not lose for its length alone.
            if (diff * best < bestDiff * period) {
                bestDiff = diff
                best = period
            }
        }
        return best
    }

    private class ShortArrayBuilder(capacity: Int) {
        private var data = ShortArray(maxOf(capacity, 16))
        private var size = 0

        private fun room(n: Int) {
            if (size + n > data.size) data = data.copyOf(maxOf(data.size * 2, size + n))
        }

        fun append(from: ShortArray, start: Int, n: Int) {
            room(n)
            from.copyInto(data, size, start, start + n)
            size += n
        }

        /** [n] samples going from [down]'s, fading, into [up]'s. */
        fun crossfade(from: ShortArray, down: Int, up: Int, n: Int) {
            room(n)
            for (i in 0 until n) data[size + i] = ((from[down + i] * (n - i) + from[up + i] * i) / n).toShort()
            size += n
        }

        fun toArray(): ShortArray = data.copyOf(size)
    }

    /** The pitches a voice is looked for between, in hertz. */
    private const val MIN_PITCH = 65
    private const val MAX_PITCH = 400
}
