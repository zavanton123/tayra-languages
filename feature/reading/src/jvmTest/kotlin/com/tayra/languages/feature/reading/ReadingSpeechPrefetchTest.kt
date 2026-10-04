package com.tayra.languages.feature.reading

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.TermPopupBuilder
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import java.io.File
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** After the open page, the reader makes the next page's audio too, without opening that page. */
class ReadingSpeechPrefetchTest {

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theNextPageIsPreparedAfterTheOpenOne() = runBlocking {
        Dispatchers.setMain(Dispatchers.Default)
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-prefetch", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        settings.update { it.copy(speechEngine = SpeechEngine.PIPER) }
        val termService = TermService(terms, languages)
        val readingService = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), termService)
        val bookService = BookService(books, languages)
        val languageId = languages.save(Language(name = "Portuguese"))
        val bookId = bookService.create(BookDraft(languageId = languageId, title = "T", text = "O lobo dorme na floresta.\n---\nA noite é longa e fria.\n---\nO dia chega devagar."))
        assertTrue(books.pageCount(bookId) >= 3, "the test needs three pages")

        val made = Collections.synchronizedList(mutableListOf<String>())
        val piper = object : LocalSpeechEngine {
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
            override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray {
                made += text
                return text.encodeToByteArray()
            }
        }
        val speech = LocalSpeech(listOf(piper))
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        val offline = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val vm = ReadingViewModel(
            bookId, 1, readingService, books, termService,
            TermPopupBuilder(terms, languages, readingService), BookStatsService(books, languages, settings, readingService), settings,
            object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null },
            LocalTranslation(null), speech, WordTranslationService(terms, offline, engine, settings),
            SentenceAudio(speech, settings, MemorySpeechAudioCache()),
        )
        withTimeout(10_000) { while (made.size < 2) delay(20) }
        delay(300)

        assertEquals(listOf("O lobo dorme na floresta.", "A noite é longa e fria."), made.toList(), "the open page, then the next; not the one after")
        assertEquals(1, vm.state.value.pageNumber)
        assertEquals(1, books.getBook(bookId)?.currentPageId?.let { books.getPageById(it)?.order }, "the next page is not opened")
    }
}
