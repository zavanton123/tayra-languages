package com.tayra.languages.core.data.media

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WavTest {
    /** A mono 16-bit WAV of [samples] at [rate], each sample its own index, as VLC writes them (data size unknown, -1). */
    private fun wav(samples: Int, rate: Int, unknownSize: Boolean = false): ByteArray {
        val pcm = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN).apply { repeat(samples) { putShort(it.toShort()) } }.array()
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + pcm.size); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(rate); putInt(rate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(if (unknownSize) -1 else pcm.size)
        }.array()
        return header + pcm
    }

    @Test
    fun theLastPartOfTheSoundIsKept() {
        val trimmed = Wav.trim(wav(1000, 1000), keepMs = 250)!!
        assertEquals(44 + 250 * 2, trimmed.size)
        val pcm = ByteBuffer.wrap(trimmed, 44, 500).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(750.toShort(), pcm.getShort(44), "the kept sound begins 750 ms in")
        assertEquals(1000, ByteBuffer.wrap(trimmed).order(ByteOrder.LITTLE_ENDIAN).getInt(24), "the rate stays")
    }

    @Test
    fun aSoundShorterThanAskedIsKeptWholeAndAnUnknownSizeIsRead() {
        val whole = wav(100, 1000, unknownSize = true)
        val trimmed = Wav.trim(whole, keepMs = 5000)!!
        assertContentEquals(whole.copyOfRange(44, whole.size), trimmed.copyOfRange(44, trimmed.size))
        assertNull(Wav.trim("not a wav file at all".toByteArray(), 100))
    }
}
