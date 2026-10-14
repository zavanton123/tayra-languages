package com.tayra.languages.feature.flashcards

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The review screen has the local engine make the speech of the whole session as it opens, before any answer is shown. */
class SessionSpeechTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-session-speech", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = FlashcardService(FlashcardRepositoryImpl(provider), terms, settings)

    private val made = Collections.synchronizedList(mutableListOf<String>())
    private val engine = object : LocalSpeechEngine {
        override val engine = SpeechEngine.PIPER
        override val displayName = "Fake"
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
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray {
            made += text
            return "$languageCode:$text".encodeToByteArray()
        }
    }

    @Test
    fun theSessionsSpeechIsMadeAsItOpens() = runBlocking {
        val id = languages.save(Language(name = "Portuguese"))
        settings.update { it.copy(currentLanguageId = id, speechEngine = SpeechEngine.PIPER) }
        terms.save(Term(languageId = id, text = "comprar", textLc = "comprar", status = TermStatus.NEW_1, sentence = "Vou comprar pão."))
        terms.save(Term(languageId = id, text = "casa", textLc = "casa", status = TermStatus.NEW_1))
        val noDictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val translator = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null }
        val suggestions = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        val audio = SentenceAudio(LocalSpeech(listOf(engine)), settings, MemorySpeechAudioCache())
        val viewModel = FlashcardsViewModel(service, languages, settings, translator, noDictionary, WordTranslationService(terms, noDictionary, suggestions, settings), audio)

        val deadline = System.currentTimeMillis() + 5_000
        while (made.size < 3 && System.currentTimeMillis() < deadline) Thread.sleep(20)
        // The sentence read when the first card's answer is shown, the second card's word (it has no sentence), then the first's word.
        assertEquals(listOf("Vou comprar pão.", "casa", "comprar"), made.toList())
        assertTrue(!viewModel.state.value.revealed, "no answer was shown")
        assertEquals("pt:Vou comprar pão.", audio.audioFor("Vou comprar pão.", "pt")?.decodeToString(), "the speaker finds it ready")
        assertEquals(3, made.size, "found ready, not made again")
    }
}
