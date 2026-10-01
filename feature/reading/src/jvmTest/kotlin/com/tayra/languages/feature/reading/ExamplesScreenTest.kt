package com.tayra.languages.feature.reading

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.feature.terms.examples.ExamplesSearchScreen
import com.tayra.languages.feature.terms.examples.ExamplesSearchViewModel
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.form.TermFormViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import com.tayra.languages.core.data.network.createHttpClient
import com.tayra.languages.core.data.network.KtorRecordingFetcher
import com.tayra.languages.core.domain.service.ExampleRecordings
import kotlinx.coroutines.delay
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
import org.junit.After
import org.junit.Rule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals

class ExamplesScreenTest {

    @get:Rule
    val rule = createComposeRule()

    /** What the fake speech engine was asked to read, as "language:text". */
    private val spoken: MutableList<String> = Collections.synchronizedList(mutableListOf())

    @After
    fun tearDown() {
        stopKoin()
    }

    /** The term pane beside the results shows the searched term, then a clicked word, until it is closed. */
    @Test
    fun thePaneShowsTheSearchedTermThenTheClickedWord() {
        show(listOf(ExampleSentence(text = "O tempo voa depressa.", translation = "Время летит быстро.")))
        rule.waitUntil(5_000) { termField("tempo") }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("voa depressa", substring = true)).fetchSemanticsNodes().isNotEmpty() }

        val sentence = rule.onAllNodes(hasText("O tempo voa depressa.")).fetchSemanticsNodes().first { SemanticsActions.GetTextLayoutResult in it.config }
        val layouts = mutableListOf<TextLayoutResult>()
        sentence.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val box = layouts.single().getBoundingBox("O tempo voa depressa.".indexOf("voa") + 1)
        rule.onAllNodes(hasText("O tempo voa depressa."))[0].performMouseInput { click(Offset(box.center.x, box.center.y)) }
        rule.waitUntil(5_000) { termField("voa") }

        rule.onNodeWithContentDescription("Close").performClick()
        rule.waitUntil(5_000) { !termField("voa") }
    }

    /**
     * Examples without a playable recording are read by the chosen speech engine and voice, made
     * ahead like the rest: one with no recording, and one whose recording cannot be downloaded.
     */
    @Test
    fun examplesWithoutAPlayableRecordingAreReadAloud() {
        show(
            listOf(
                ExampleSentence(text = "Eu não tenho tempo.", translation = null),
                // Nothing listens on port 1, so downloading this recording fails.
                ExampleSentence(text = "Acabou o tempo.", translation = null, audioUrl = "http://127.0.0.1:1/audio.mp3"),
            ),
        )
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Read aloud").fetchSemanticsNodes().size == 2 }
        rule.waitUntil(5_000) { spoken.toSet() == setOf("pt:Eu não tenho tempo.", "pt:Acabou o tempo.") }

        rule.onAllNodesWithContentDescription("Read aloud")[1].performClick()
        rule.waitUntil(1_000) { rule.onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, spoken.size, "both were made ahead; the click plays the prepared audio")
    }

    /** A recording is downloaded before it is clicked: its button spins, then plays at once from memory. */
    @Test
    fun recordingsAreDownloadedBeforeTheyAreClicked() {
        val hits = java.util.concurrent.atomic.AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/audio.mp3") { exchange ->
                hits.incrementAndGet()
                Thread.sleep(1_000)
                val body = ByteArray(64)
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }
        try {
            show(
                listOf(
                    ExampleSentence(text = "Acabou o tempo.", translation = null, audioUrl = "http://127.0.0.1:${server.address.port}/audio.mp3"),
                    ExampleSentence(text = "Eu não tenho tempo.", translation = null),
                ),
            )
            rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Loading recording").fetchSemanticsNodes().isNotEmpty() }
            rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Play recording").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(1, hits.get(), "downloaded before any click")

            rule.onNodeWithContentDescription("Play recording").performClick()
            rule.waitForIdle()
            Thread.sleep(300)
            assertEquals(1, hits.get(), "the downloaded recording is played, not downloaded again")
            assertEquals(false, "pt:Acabou o tempo." in spoken, "a recording that arrives is not read aloud")
        } finally {
            server.stop(0)
        }
    }

    /** The term pane's examples follow the same rules as the screen: recordings, else the speech engine. */
    @Test
    fun thePanesExamplesPlayTheSameWay() {
        show(
            sentences = emptyList(),
            paneSentences = listOf(
                ExampleSentence(text = "Eu não tenho tempo.", translation = null),
                ExampleSentence(text = "Acabou o tempo.", translation = null, audioUrl = "http://127.0.0.1:1/audio.mp3"),
            ),
        )
        // The recording cannot be downloaded, so both examples end up read aloud, made ahead.
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Read aloud").fetchSemanticsNodes().size == 2 }
        rule.waitUntil(5_000) { spoken.toSet() == setOf("pt:Eu não tenho tempo.", "pt:Acabou o tempo.") }

        // The examples sit at the bottom of the scrolling pane, below the test window.
        rule.onAllNodesWithContentDescription("Read aloud")[0].performScrollTo().performClick()
        rule.waitUntil(1_000) { rule.onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isNotEmpty() }
    }

    /** An example without a recording is read ahead of time: its button spins, then plays at once. */
    @Test
    fun readAloudExamplesArePreparedBeforeTheyAreClicked() {
        show(listOf(ExampleSentence(text = "Eu não tenho tempo.", translation = null)), speechDelayMs = 1_000)
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Preparing speech").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Read aloud").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("pt:Eu não tenho tempo."), spoken.toList(), "made before any click")

        rule.onNodeWithContentDescription("Read aloud").performClick()
        rule.waitUntil(1_000) { rule.onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, spoken.size, "the prepared audio is played, not made again")
    }

    private fun show(sentences: List<ExampleSentence>, paneSentences: List<ExampleSentence> = emptyList(), speechDelayMs: Long = 0) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-examples", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        runBlocking { settings.update { it.copy(speechEngine = SpeechEngine.PIPER) } }
        val languageId = runBlocking { languages.save(Language(name = "Portuguese")) }
        val examples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult(sentences, sentences.size, null)
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        val dictionaries = DictionaryService(
            object : DictionaryPackStore {
                override suspend fun installedSize(pack: DictionaryPack): Long? = null
                override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) = error("offline")
                override suspend fun remove(pack: DictionaryPack) {}
            },
            object : DictionaryRepository {
                override suspend fun isAvailable(dictionary: DictionaryId) = false
                override suspend fun entries(dictionary: DictionaryId, wordLc: String) = emptyList<DictionaryEntry>()
                override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
                override suspend fun close(dictionary: DictionaryId) {}
            },
        )
        val termService = TermService(terms, languages)
        startKoin {
            modules(module {
                single { LocalSpeech(listOf(RecordingSpeech(spoken, speechDelayMs))) }
                val cache = MemorySpeechAudioCache()
                single { ExampleRecordings(KtorRecordingFetcher(createHttpClient()), cache) }
                single { SentenceAudio(get(), settings, cache, get()) }
                single<SettingsRepository> { settings }
                single { WordTranslationService(terms, dictionaries, engine, settings) }
                // The pane has its own example list, so its buttons never match the screen's by accident.
                val paneExamples = object : ExampleSentencesProvider {
                    override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult(paneSentences, paneSentences.size, null)
                    override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
                }
                viewModel { (key: TermFormKey) -> TermFormViewModel(key, termService, terms, languages, settings, engine, paneExamples, dictionaries, dictionaries) }
            })
        }
        val vm = ExamplesSearchViewModel(languageId, "tempo", languages, settings, examples)
        rule.setContent { ExamplesSearchScreen(languageId, "tempo", onNavigate = {}, onBack = {}, viewModel = vm) }
    }

    private fun termField(text: String) = rule.onAllNodes(hasSetTextAction() and hasText(text)).fetchSemanticsNodes().isNotEmpty()

    /** A Piper stand-in that notes what it reads and returns a short silent WAV. */
    private class RecordingSpeech(private val spoken: MutableList<String>, private val delayMs: Long) : LocalSpeechEngine {
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
            delay(delayMs)
            spoken += "$languageCode:$text"
            val data = 1600
            val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + data); put("WAVE".toByteArray())
                put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(16_000); putInt(32_000); putShort(2); putShort(16)
                put("data".toByteArray()); putInt(data)
            }.array()
            return header + ByteArray(data)
        }
    }
}
