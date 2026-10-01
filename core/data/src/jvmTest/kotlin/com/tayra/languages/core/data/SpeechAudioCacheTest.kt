package com.tayra.languages.core.data

import com.tayra.languages.core.data.speech.FileSpeechAudioCache
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SpeechAudioCache
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The caches keep a bounded amount and drop the audio used longest ago first. */
class SpeechAudioCacheTest {

    private suspend fun SpeechAudioCache.fillPastItsLimit() {
        write("a", ByteArray(100))
        Thread.sleep(20)
        write("b", ByteArray(100))
        Thread.sleep(20)
        // Playing "a" again makes "b" the one used longest ago.
        assertNotNull(read("a"))
        Thread.sleep(20)
        write("c", ByteArray(100))
    }

    @Test
    fun filesUsedLongestAgoAreDeletedPastTheLimit() = runTest {
        val directory = Files.createTempDirectory("speech-cap").toFile()
        val cache = FileSpeechAudioCache(directory, maxBytes = 250)
        cache.fillPastItsLimit()

        assertNull(cache.read("b"))
        assertNotNull(cache.read("a"))
        assertNotNull(cache.read("c"))
        assertEquals(200, directory.listFiles()!!.sumOf { it.length() })
    }

    @Test
    fun theMemoryCacheKeepsTheSameLimit() = runTest {
        val cache = MemorySpeechAudioCache(maxBytes = 250)
        cache.fillPastItsLimit()

        assertNull(cache.read("b"))
        assertNotNull(cache.read("a"))
        assertNotNull(cache.read("c"))
    }

    @Test
    fun aSingleClipLargerThanTheLimitIsStillKept() = runTest {
        val cache = MemorySpeechAudioCache(maxBytes = 50)
        cache.write("big", ByteArray(100))
        assertNotNull(cache.read("big"))
    }
}
