package com.tayra.languages.feature.reading

import com.russhwolf.settings.MapSettings
import kotlin.test.assertTrue
import org.koin.dsl.module
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.SemanticsProperties
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.junit.Rule
import com.tayra.languages.core.domain.settings.SettingsRepository
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Offset
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
import org.koin.core.module.dsl.viewModel
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.feature.terms.form.TermFormViewModel
import com.tayra.languages.feature.terms.form.TermFormKey
import androidx.compose.ui.test.click
import kotlinx.coroutines.flow.MutableStateFlow
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermPopupBuilder
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadingHoverTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var settings: SettingsRepositoryImpl

    /** What the term pane needs when a click opens it on the real screen. */
    private lateinit var termPane: org.koin.core.module.Module

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    /** [mainIsDefault] swaps the UI thread for a pool, for tests that never draw the screen. */
    private suspend fun reader(mainIsDefault: Boolean = true): ReadingViewModel {
        if (mainIsDefault) Dispatchers.setMain(Dispatchers.Default)
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-hover", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        settings = SettingsRepositoryImpl(MapSettings())
        val termService = TermService(terms, languages)
        val readingService = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), termService)
        val bookService = BookService(books, languages)
        val languageId = languages.save(Language(name = "Portuguese"))
        val bookId = bookService.create(BookDraft(languageId = languageId, title = "T", text = "O lobo dorme na floresta."))
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = "<$text>"
        }
        val offline = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val words = WordTranslationService(terms, offline, engine, settings)
        termPane = module {
            single { words }
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
                TermFormViewModel(key, termService, terms, languages, settings, engine, noExamples, dictionaries, dictionaries)
            }
        }
        val vm = ReadingViewModel(
            bookId, null, readingService, bookService, books, termService,
            TermPopupBuilder(terms, languages, readingService), BookStatsService(books, languages, settings, readingService), settings,
            object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null },
            LocalTranslation(null), LocalSpeech(emptyList()), words,
        )
        withTimeout(10_000) { while (vm.state.value.items.none { it.isWord }) delay(20) }
        return vm
    }

    private fun ReadingViewModel.index(word: String) = state.value.items.indexOfFirst { it.isWord && it.renderText == word }

    /** Hovers [word] and returns the translation on the card that appears, or null. */
    private suspend fun ReadingViewModel.hoverCard(word: String): String? {
        onHover(index(word))
        delay(700)
        val card = state.value.popup?.popup?.translation
        onHover(null)
        delay(50)
        return card
    }

    @Test
    fun unsavedWordsGetACardWithALookedUpTranslation() = runBlocking {
        val vm = reader()
        assertEquals("<lobo>", vm.hoverCard("lobo"))
    }

    @Test
    fun hoverKeepsWorkingWhileAnotherWordsPaneIsOpen() = runBlocking {
        val vm = reader()
        vm.onWordClick(vm.index("lobo"), shift = false)
        delay(200)
        assertEquals(setOf(vm.index("lobo")), vm.state.value.marked)
        assertEquals("<floresta>", vm.hoverCard("floresta"))
        assertNull(vm.hoverCard("lobo"), "the word whose pane is open needs no card")
    }

    /** The whole screen, as the app shows it: hovering a word with the mouse brings up its card. */
    @Test
    fun theReaderScreenShowsTheCardOfTheHoveredWord() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings } }) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        // Matched by a phrase only the page has, since the card repeats the word.
        val paragraph = rule.onNodeWithText("lobo dorme", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        paragraph.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        val text = layout.layoutInput.text.text
        val start = text.indexOf("floresta")
        val box = layout.getBoundingBox(start + 2)
        paragraph.performMouseInput { moveTo(Offset(box.center.x, box.center.y + 6.dp.toPx())) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("<floresta>").fetchSemanticsNodes().isNotEmpty() }
        val card = rule.onNodeWithText("<floresta>").fetchSemanticsNode().boundsInWindow
        val words = paragraph.fetchSemanticsNode().boundsInWindow
        assertTrue(card.bottom <= words.top + box.top + 6.dp.value * 2 + 1, "the card should sit above the word")
    }

    @Test
    fun noCardDuringADragSelection() = runBlocking {
        val vm = reader()
        vm.startSelection(vm.state.value.items[vm.index("lobo")].index)
        assertNull(vm.hoverCard("floresta"))
    }

    /** With "Speak word on click" on, a click reads the word aloud with the chosen engine; with it off, nothing is said. */
    @Test
    fun clickingAWordSpeaksItWhenTheSettingIsOn() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        val spoken = java.util.Collections.synchronizedList(mutableListOf<String>())
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
                spoken += "$languageCode:$text"
                return silentWav()
            }
        }
        runBlocking { settings.update { it.copy(speechEngine = SpeechEngine.PIPER, speakWordOnClick = true) } }
        startKoin { modules(module { single { LocalSpeech(listOf(piper)) }; single<SettingsRepository> { settings } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val paragraph = rule.onNodeWithText("lobo dorme", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        paragraph.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val text = layouts.single().layoutInput.text.text
        fun click(word: String) {
            val box = layouts.single().getBoundingBox(text.indexOf(word) + 1)
            paragraph.performMouseInput { click(Offset(box.center.x, box.center.y + 6.dp.toPx())) }
        }

        click("floresta")
        rule.waitUntil(5_000) { "pt:floresta" in spoken }

        runBlocking { settings.update { it.copy(speakWordOnClick = false) } }
        click("lobo")
        rule.waitForIdle()
        Thread.sleep(500)
        assertEquals(listOf("pt:floresta"), spoken.toList())
    }

    /** Dragging across words underlines the phrase while the button is still held, and nothing else changes colour. */
    @Test
    fun aPhraseIsUnderlinedWhileItIsBeingDragged() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val paragraph = rule.onNodeWithText("lobo dorme", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        paragraph.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val text = layouts.single().layoutInput.text.text
        val padding = with(rule.density) { 6.dp.toPx() }
        fun at(word: String): Offset = layouts.single().getBoundingBox(text.indexOf(word) + 1).let { Offset(it.center.x, it.center.y + padding) }
        fun underlined(): String {
            val shown = paragraph.fetchSemanticsNode().config[SemanticsProperties.Text].single()
            return shown.spanStyles.filter { it.item.textDecoration == TextDecoration.Underline }.sortedBy { it.start }
                .joinToString("") { shown.text.substring(it.start, it.end) }
        }

        paragraph.performMouseInput { moveTo(at("lobo")); press() }
        paragraph.performMouseInput { moveTo(at("dorme")); moveTo(at("floresta")) }
        rule.waitForIdle()
        assertEquals("lobo dorme na floresta", underlined(), "underlined before the button is released")
        paragraph.performMouseInput { release() }
        rule.waitForIdle()
        assertEquals("lobo dorme na floresta", underlined(), "still underlined while its term is open")
    }

    private fun silentWav(samples: Int = 800): ByteArray {
        val data = samples * 2
        val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(16_000); putInt(32_000); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data)
        }.array()
        return header + ByteArray(data)
    }
}
