package com.tayra.languages.feature.frequency

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.network.KtorRecordingFetcher
import com.tayra.languages.core.data.network.createHttpClient
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.frequency.WordFrequencyService
import com.tayra.languages.core.domain.frequency.WordKnowledge
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.ExampleRecordings
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.form.TermFormViewModel
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Hovering a word translates it, and clicking it opens the term panel beside the list, whose status shows in the list at once. */
@OptIn(ExperimentalTestApi::class)
class WordFrequencyPanelTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-frequency-panel", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val portuguese = runBlocking { languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) } } }

    private val engine = object : TermTranslationProvider {
        override val name = "Fake"
        override suspend fun suggestTranslation(text: String, language: Language): String? = "<$text>"
    }
    private val offline = object : OfflineDictionary {
        override suspend fun isAvailable(dictionary: DictionaryId) = false
        override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
    }
    private val translations = WordTranslationService(terms, offline, engine, settings)
    private val viewModel = WordFrequencyViewModel(WordFrequencyService(ResourceFrequencyLists(), terms, languages), languages, settings, translations)

    private val testModule = module {
        single { LocalSpeech(emptyList()) }
        single<SettingsRepository> { settings }
        single { SentenceAudio(LocalSpeech(emptyList()), settings, MemorySpeechAudioCache()) }
        single { translations }
        single { ExampleRecordings(KtorRecordingFetcher(createHttpClient()), MemorySpeechAudioCache()) }
        viewModel { (key: TermFormKey) ->
            val noPacks = object : DictionaryPackStore {
                override suspend fun installedSize(pack: DictionaryPack): Long? = null
                override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) = error("offline")
                override suspend fun remove(pack: DictionaryPack) {}
            }
            val noEntries = object : DictionaryRepository {
                override suspend fun isAvailable(dictionary: DictionaryId) = false
                override suspend fun entries(dictionary: DictionaryId, wordLc: String) = emptyList<DictionaryEntry>()
                override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
                override suspend fun close(dictionary: DictionaryId) {}
            }
            val noExamples = object : ExampleSentencesProvider {
                override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult.EMPTY
                override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
            }
            val dictionaries = DictionaryService(noPacks, noEntries)
            TermFormViewModel(key, TermService(terms, languages), terms, languages, settings, engine, noExamples, dictionaries, dictionaries)
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun hoveringTranslatesAndClickingSavesTheWord() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        startKoin { modules(testModule) }
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) { WordFrequencyScreen(onNavigate = {}, viewModel = viewModel) }
                }
            }
        }
        waitUntil(timeoutMillis = 10_000) { viewModel.state.value.overview != null }
        onNodeWithTag("word-dizer").performMouseInput { moveTo(center) }
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("<dizer>").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("FREQUENCY_TOOLTIP_SCREENSHOT")?.let { save(it) }
        onNodeWithTag("word-dizer").performMouseInput { moveTo(center + androidx.compose.ui.geometry.Offset(0f, 400f)) }

        onNodeWithTag("word-casa").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Term details").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText("K")[0].performClick()
        waitUntil(timeoutMillis = 10_000) { viewModel.state.value.overview?.count(WordKnowledge.KNOWN) == 1 }
        assertEquals(TermStatus.WELL_KNOWN, runBlocking { terms.findByTextLc(portuguese, "casa") }?.status)
        waitForIdle()
        System.getenv("FREQUENCY_PANEL_SCREENSHOT")?.let { save(it) }
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}
