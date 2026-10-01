package com.tayra.languages.feature.reading

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
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
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import kotlin.test.assertTrue
import kotlin.math.abs
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** The page's play button reads sentence after sentence and turns pages; auto-pause stops after each. */
class ContinuousReadingTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var settings: SettingsRepositoryImpl
    private val sentences = listOf("O lobo dorme.", "A noite é fria.", "O dia chega.")

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun theWholeTextIsReadAcrossPages() {
        show(autoPause = false)
        rule.onNodeWithContentDescription("Read the page").performClick()

        assertEquals(sentences, readUntilSilent())
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Read the page").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun autoPauseStopsAfterEachSentence() {
        show(autoPause = true)
        rule.onNodeWithContentDescription("Auto-pause on").fetchSemanticsNode()

        rule.onNodeWithContentDescription("Read the page").performClick()
        assertEquals(listOf("O lobo dorme."), readUntilSilent())
        rule.onNodeWithContentDescription("Read the page").performClick()
        assertEquals(listOf("A noite é fria."), readUntilSilent())
    }

    /** On a long page the sentence being read is scrolled to the middle of the reading area. */
    @Test
    fun theSentenceBeingReadIsCentred() {
        val long = (1..30).joinToString(" ") { "Frase número $it." }
        show(autoPause = false, text = long, sentenceMillis = 3_000)
        rule.onNodeWithContentDescription("Read the page").performClick()

        // Jump to sentence 15 with its own button; reading carries on from there.
        rule.onNode(hasText("Frase número 15.", substring = true)).performScrollTo()
        rule.waitForIdle()
        val row = rule.onNode(hasText("Frase número 15.", substring = true)).fetchSemanticsNode().boundsInRoot
        val index = rule.onAllNodes(hasContentDescription("Play sentence")).fetchSemanticsNodes()
            .indexOfFirst { it.boundsInRoot.center.y in row.top..row.bottom }
        rule.onAllNodes(hasContentDescription("Play sentence"))[index].performClick()
        rule.waitUntil(5_000) { sentenceBeingRead(listOf("Frase número 15.")) != null }
        Thread.sleep(800)
        rule.waitForIdle()

        // The Stop button sits in the middle of the row being read.
        // The reader's scrolling area; the closed drawer has one too, off to the left.
        val area = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
            .map { it.boundsInRoot }.first { it.left >= 0f }
        val reading = rule.onNode(hasContentDescription("Stop")).fetchSemanticsNode().boundsInRoot
        assertEquals("Frase número 15.", sentenceBeingRead(listOf("Frase número 15.")))
        assertTrue(abs(reading.center.y - area.center.y) < 30, "the row being read (${reading.center.y}) should sit at the middle (${area.center.y})")
    }

    /** The sentences read aloud, in order, until reading stops for a moment. */
    private fun readUntilSilent(): List<String> {
        val heard = mutableListOf<String>()
        var quiet = 0
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            rule.waitForIdle()
            val now = sentenceBeingRead()
            if (now != null && heard.lastOrNull() != now) heard += now
            quiet = if (now == null && heard.isNotEmpty()) quiet + 1 else 0
            if (quiet >= 15) break
            Thread.sleep(40)
        }
        return heard
    }

    /** The sentence whose button shows Stop, matched by the row it sits in. */
    private fun sentenceBeingRead(among: List<String> = sentences): String? {
        val stop = rule.onAllNodes(hasContentDescription("Stop")).fetchSemanticsNodes().firstOrNull() ?: return null
        val y = stop.boundsInRoot.center.y
        return among.firstOrNull { sentence ->
            rule.onAllNodes(hasText(sentence, substring = true)).fetchSemanticsNodes().any { it.boundsInRoot.top <= y && y <= it.boundsInRoot.bottom }
        }
    }

    private fun show(autoPause: Boolean, text: String = "O lobo dorme. A noite é fria.\n---\nO dia chega.", sentenceMillis: Int = 400) {
        val vm = runBlocking {
            val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-continuous", ".db").also { it.delete() }))
            val languages = LanguageRepositoryImpl(provider)
            val books = BookRepositoryImpl(provider)
            val terms = TermRepositoryImpl(provider)
            settings = SettingsRepositoryImpl(MapSettings())
            settings.update { it.copy(speechEngine = SpeechEngine.PIPER, splitSentences = true, autoPause = autoPause) }
            val termService = TermService(terms, languages)
            val readingService = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), termService)
            val bookService = BookService(books, languages)
            val languageId = languages.save(Language(name = "Portuguese"))
            val bookId = bookService.create(BookDraft(languageId = languageId, title = "T", text = text, wordsPerPage = 500))
            val engine = object : TermTranslationProvider {
                override val name = "Fake"
                override suspend fun suggestTranslation(text: String, language: Language): String? = null
            }
            val offline = object : OfflineDictionary {
                override suspend fun isAvailable(dictionary: DictionaryId) = false
                override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
            }
            val speech = LocalSpeech(listOf(ShortPiper(sentenceMillis)))
            val audio = SentenceAudio(speech, settings, MemorySpeechAudioCache())
            startKoin { modules(module { single { speech }; single<SettingsRepository> { settings }; single { audio } }) }
            ReadingViewModel(
                bookId, 1, readingService, bookService, books, termService,
                TermPopupBuilder(terms, languages, readingService), BookStatsService(books, languages, settings, readingService), settings,
                object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null },
                LocalTranslation(null), speech, WordTranslationService(terms, offline, engine, settings), audio,
            )
        }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = 1, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("Read the page").fetchSemanticsNodes().isNotEmpty() }
        // Every sentence of the page is ready, so the reading below does not wait on synthesis.
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("Preparing sentence").fetchSemanticsNodes().isEmpty() }
    }

    /** A Piper stand-in whose every sentence is [millis] of silence. */
    private class ShortPiper(private val millis: Int) : LocalSpeechEngine {
        override val engine = SpeechEngine.PIPER
        override val displayName = "Short Piper"
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
            val data = 16_000 * 2 * millis / 1000
            val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + data); put("WAVE".toByteArray())
                put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(16_000); putInt(32_000); putShort(2); putShort(16)
                put("data".toByteArray()); putInt(data)
            }.array()
            return header + ByteArray(data)
        }
    }
}
