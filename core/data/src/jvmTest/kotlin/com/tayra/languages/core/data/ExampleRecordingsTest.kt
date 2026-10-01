package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.service.ExampleRecordings
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.RecordingFetcher
import com.tayra.languages.core.domain.service.RecordingState
import com.tayra.languages.core.domain.service.SentenceAudio
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExampleRecordingsTest {

    private val fetched = mutableListOf<String>()
    private val fetcher = RecordingFetcher { url ->
        delay(100)
        fetched += url
        if (url.endsWith("refused")) null else url.encodeToByteArray()
    }
    private val cache = MemorySpeechAudioCache()
    private val recordings = ExampleRecordings(fetcher, cache)

    @Test
    fun recordingsAreDownloadedOnceAheadOfTime() = runTest {
        recordings.prefetch(listOf("https://x/1", "https://x/refused", "https://x/1"))

        assertEquals(mapOf("https://x/1" to RecordingState.READY, "https://x/refused" to RecordingState.FAILED), recordings.states.value)
        assertContentEquals("https://x/1".encodeToByteArray(), recordings.audioFor("https://x/1"))
        assertEquals(listOf("https://x/1", "https://x/refused"), fetched, "each once, and a ready one is not fetched again")

        recordings.prefetch(listOf("https://x/refused"))
        assertEquals(2, fetched.size, "a refused recording is not asked for again")
    }

    @Test
    fun aClickDuringTheDownloadWaitsForIt() = runTest {
        val both = listOf(async { recordings.audioFor("https://x/2") }, async { recordings.audioFor("https://x/2") }).awaitAll()
        assertTrue(both.all { it != null })
        assertEquals(listOf("https://x/2"), fetched)
    }

    @Test
    fun openingAnotherBookForgetsTheRecordings() = runTest {
        val settings = SettingsRepositoryImpl(MapSettings())
        val audio = SentenceAudio(LocalSpeech(emptyList()), settings, cache, recordings)
        audio.prepare(emptyList(), "pt", bookId = 1)
        recordings.prefetch(listOf("https://x/3"))
        audio.prepare(emptyList(), "pt", bookId = 2)

        assertTrue(recordings.states.value.isEmpty())
        recordings.audioFor("https://x/3")
        assertEquals(listOf("https://x/3", "https://x/3"), fetched, "downloaded again after the cache was emptied")
    }
}
