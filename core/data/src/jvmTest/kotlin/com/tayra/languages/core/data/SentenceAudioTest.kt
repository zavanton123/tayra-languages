package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.data.speech.FileSpeechAudioCache
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SentenceAudioState
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SentenceAudioTest {

    private class CountingPiper : LocalSpeechEngine {
        val made = mutableListOf<String>()
        override val engine = SpeechEngine.PIPER
        override val displayName = "Fake Piper"
        override val description = ""
        override val packagesDescription = ""
        override val hasRuntimeSetup = false
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun status() = "ready"
        override suspend fun isReady() = true
        override suspend fun setUp() = "ready"
        override suspend fun packages() = emptyList<SpeechPackage>()
        override suspend fun installPackage(id: String) {}
        override suspend fun removePackage(id: String) {}
        override suspend fun voices(languageCode: String) = emptyList<SpeechVoice>()
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
            made += text
            return if (text.startsWith("Unspeakable")) null else "$languageCode:$text@$speed".encodeToByteArray()
        }
    }

    private val piper = CountingPiper()
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val directory = Files.createTempDirectory("speech-cache").toFile()
    private val audio = SentenceAudio(LocalSpeech(listOf(piper)), settings, FileSpeechAudioCache(directory))
    private val page = listOf("O lobo dorme.", "A noite é longa.", "...", "O lobo dorme.")

    @Test
    fun aPageIsPreparedAheadAndPlayedFromTheCache() = runTest {
        settings.update { it.copy(speechEngine = SpeechEngine.PIPER) }
        audio.prepare(bookId = 1, sentences = page, languageCode = "pt")

        assertEquals(mapOf("O lobo dorme." to SentenceAudioState.READY, "A noite é longa." to SentenceAudioState.READY), audio.states.value)
        assertEquals(listOf("O lobo dorme.", "A noite é longa."), piper.made, "each sentence once; no audio for one without words")
        assertEquals(2, directory.listFiles()!!.size)

        assertContentEquals("pt:A noite é longa.@1.0".encodeToByteArray(), audio.audioFor("A noite é longa.", "pt"))
        assertEquals(2, piper.made.size, "a prepared sentence is not made again")
    }

    @Test
    fun anotherSpeedMakesNewAudioAndAnotherBookEmptiesTheCache() = runTest {
        settings.update { it.copy(speechEngine = SpeechEngine.PIPER) }
        audio.prepare(1, page, "pt")
        settings.update { it.copy(speechSpeed = 1.5f) }
        audio.prepare(1, page, "pt")
        assertEquals(4, piper.made.size)
        assertEquals(4, directory.listFiles()!!.size, "audio for both speeds is kept while the book is open")

        audio.prepare(2, listOf("Outra história."), "pt")
        assertEquals(1, directory.listFiles()!!.size, "opening another book drops the earlier audio")
        assertEquals(mapOf("Outra história." to SentenceAudioState.READY), audio.states.value)
    }

    @Test
    fun aSentenceTheEngineCannotSpeakIsLeftToTheButtonAndTheSystemVoiceNeedsNothing() = runTest {
        settings.update { it.copy(speechEngine = SpeechEngine.PIPER) }
        audio.prepare(1, listOf("Unspeakable sentence.", "O lobo dorme."), "pt")
        assertEquals(mapOf("O lobo dorme." to SentenceAudioState.READY), audio.states.value)

        settings.update { it.copy(speechEngine = SpeechEngine.SYSTEM) }
        audio.prepare(1, page, "pt")
        assertTrue(audio.states.value.isEmpty())
        assertEquals(2, piper.made.size)
    }
}
