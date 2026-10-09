package com.tayra.languages.feature.reading

import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.Lesson
import com.tayra.languages.core.domain.courses.LessonReading
import com.russhwolf.settings.MapSettings
import kotlin.test.assertTrue
import org.koin.dsl.module
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.assertIsFocused
import com.tayra.languages.core.domain.model.TermStatus
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.input.key.Key
import com.tayra.languages.core.data.network.createHttpClient
import com.tayra.languages.core.data.network.KtorRecordingFetcher
import com.tayra.languages.core.domain.service.ExampleRecordings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
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
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toAwtImage
import com.tayra.languages.core.ui.components.STATUS_BAR_TAG
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
    private lateinit var termRepository: TermRepositoryImpl
    private lateinit var termService: TermService
    private var languageId = 0L

    /** The reader's prepared sentence audio, shared with the screen's speaker through Koin. */
    private lateinit var sentenceAudio: SentenceAudio

    /** What the term pane needs when a click opens it on the real screen. */
    private lateinit var termPane: org.koin.core.module.Module

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    /** [mainIsDefault] swaps the UI thread for a pool, for tests that never draw the screen. */
    private suspend fun reader(mainIsDefault: Boolean = true, speech: LocalSpeech = LocalSpeech(emptyList()), pages: Int = 1, translation: String? = null, lesson: LessonReading? = null): ReadingViewModel {
        if (mainIsDefault) Dispatchers.setMain(Dispatchers.Default)
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-hover", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        termRepository = terms
        settings = SettingsRepositoryImpl(MapSettings())
        val termService = TermService(terms, languages).also { this.termService = it }
        val readingService = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), termService)
        val bookService = BookService(books, languages)
        languageId = languages.save(Language(name = "Portuguese"))
        val bookId = bookService.create(BookDraft(languageId = languageId, title = "T", text = List(pages) { "O lobo dorme na floresta." }.joinToString("\n\n"), wordsPerPage = 5))
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
                TermFormViewModel(key, termService, terms, languages, settings, engine, noExamples, dictionaries, dictionaries)
            }
        }
        sentenceAudio = SentenceAudio(speech, settings, MemorySpeechAudioCache())
        val vm = ReadingViewModel(
            bookId, null, readingService, books, termService,
            TermPopupBuilder(terms, languages, readingService), BookStatsService(books, languages, settings, readingService), settings,
            object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = translation },
            LocalTranslation(null), speech, words, sentenceAudio,
            lessonOf = { lesson },
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

    /**
     * The footer offers to mark the unknown words as known, with their count, only while there
     * are some, and a toast confirms it; on the last page "Finish book" goes back to the library.
     */
    @Test
    fun theFooterMarksUnknownWordsAndFinishesTheBook() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        var home = false
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = { home = true }, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Mark remaining words as known").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Mark remaining words as known").performScrollTo().assertTextContains("5")
        System.getenv("READING_FOOTER_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }

        rule.onNodeWithText("Mark remaining words as known").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Mark remaining words as known").fetchSemanticsNodes().isEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("5 words marked as known").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("READING_TOAST_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        assertEquals(TermStatus.WELL_KNOWN, runBlocking { termRepository.findByTextLc(languageId, "lobo") }?.status)
        rule.waitUntil(8_000) { rule.onAllNodesWithText("5 words marked as known").fetchSemanticsNodes().isEmpty() }

        rule.onNodeWithText("Finish book").performScrollTo().performClick()
        rule.waitUntil(5_000) { home }
    }

    /** The toolbar shows "page of pages" between the arrows, with a slider across the book. */
    @Test
    fun theToolbarShowsThePagePositionAndMovesBetweenPages() {
        val vm = runBlocking { reader(mainIsDefault = false, pages = 12) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        val count = vm.state.value.pageCount
        assertTrue(count > 2, "the book should have several pages")
        rule.waitUntil(5_000) { rule.onAllNodesWithText("1  of $count").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Next page").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("2  of $count").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, vm.state.value.pageNumber)
        System.getenv("PAGE_PROGRESS_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        rule.onNodeWithContentDescription("Previous page").performClick()
        rule.waitUntil(5_000) { vm.state.value.pageNumber == 1 }
    }

    /** The menu's Edit section opens the whole book or the current page for editing, and adds or deletes no pages. */
    @Test
    fun theMenuEditsTheBookOrThePage() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        val visited = mutableListOf<com.tayra.languages.core.ui.navigation.Route>()
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = { visited += it }, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Menu").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("tool-edit").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("tool-edit").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Edit book").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Edit current page").assertExists()
        for (gone in listOf("Add page after", "Add page before", "Delete current page")) {
            assertTrue(rule.onAllNodesWithText(gone).fetchSemanticsNodes().isEmpty(), "$gone is no longer offered")
        }
        rule.onNodeWithText("Edit book").performClick()
        rule.waitUntil(5_000) { visited.isNotEmpty() }
        assertEquals(listOf<com.tayra.languages.core.ui.navigation.Route>(com.tayra.languages.core.ui.navigation.Route.EditBook(1)), visited)
    }

    /** A course lesson is shown as part of its course: no word of books or the library, and no editing. */
    @Test
    fun aLessonIsShownAsPartOfItsCourse() {
        val course = Course(
            "pt-mini-0100", "pt", "Em casa e com a família", "", CourseLevel.A1, "",
            listOf(Lesson("pt-mini-0100-01", "A casa nova", "", "O lobo dorme."), Lesson("pt-mini-0100-02", "Onde está o Tom?", "", "O lobo dorme.")),
        )
        val vm = runBlocking { reader(mainIsDefault = false, lesson = LessonReading(course, course.lessons[1])) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        val visited = mutableListOf<com.tayra.languages.core.ui.navigation.Route>()
        var back = 0
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = { visited += it }, onHome = { back++ }, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Finish lesson").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("LESSON_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        for (gone in listOf("Back to library", "Finish book")) {
            assertTrue(rule.onAllNodesWithText(gone).fetchSemanticsNodes().isEmpty(), "$gone is not shown in a lesson")
        }
        rule.onNodeWithText(course.title).performClick()
        rule.waitUntil(5_000) { back == 1 }
        rule.onNodeWithText("Back to course").performScrollTo().performClick()
        rule.waitUntil(5_000) { back == 2 }
        rule.onNodeWithText("Finish lesson").performScrollTo().performClick()
        rule.waitUntil(5_000) { back == 3 }

        rule.onNodeWithContentDescription("Menu").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("tool-bookmarks").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(rule.onAllNodesWithTag("tool-edit").fetchSemanticsNodes().isEmpty(), "a lesson's text is not edited")
    }

    /** The pane sorts its settings into Reading, Audio and Appearance tabs; a layout choice is saved as it is made. */
    @Test
    fun theReaderPaneSortsItsSettingsIntoTabs() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Menu").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Reading experience").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("READER_PANE_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File("$path-reading.png"))
        }
        for (shown in listOf("Translations", "Show translations", "Translation layout", "Translation engine", "Sentence layout")) {
            rule.onNodeWithText(shown).assertExists()
        }
        rule.onNodeWithText("Below").performScrollTo().performClick()
        rule.waitUntil(5_000) { !settings.current.sideBySideTranslations }
        rule.onNodeWithText("One per line").performClick()
        rule.waitUntil(5_000) { settings.current.splitSentences }

        rule.onNodeWithTag("pane-tab-AUDIO").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Playback").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Speak word on click").assertExists()
        System.getenv("READER_PANE_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File("$path-audio.png"))
        }
        assertTrue(rule.onAllNodesWithText("Reading experience").fetchSemanticsNodes().isEmpty())

        rule.onNodeWithTag("pane-tab-APPEARANCE").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Typography").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("READER_PANE_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File("$path-appearance.png"))
        }
        rule.onNodeWithText("Justified").performScrollTo().performClick()
        rule.waitUntil(5_000) { settings.current.readingJustified }
    }

    /** Typography offers the reading fonts; picking one saves it and the row shows it. */
    @Test
    fun theMenuChangesTheReadingFont() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Menu").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("pane-tab-APPEARANCE").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("pane-tab-APPEARANCE").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("font-choice").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("font-choice").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Lora").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("FONT_MENU_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        rule.onNodeWithText("Lora").performClick()
        rule.waitUntil(5_000) { settings.current.readingFont == "lora" }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Lora").fetchSemanticsNodes().isNotEmpty() }
    }

    /** Bookmarks open inside the pane: the current page is added, listed with its opening words, and opened from the list. */
    @Test
    fun thePaneBookmarksTheCurrentPage() {
        val vm = runBlocking { reader(mainIsDefault = false, pages = 3) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Menu").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("tool-bookmarks").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("tool-bookmarks").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Page 1 is not bookmarked yet").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("add-bookmark").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("bookmark-1").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Page 1 is bookmarked").assertExists()
        rule.onNodeWithTag("bookmark-1").assertTextContains("O lobo dorme na floresta.", substring = true)
        System.getenv("BOOKMARKS_PANE_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        for (gone in listOf("LANGUAGE TOOLS", "Translate sentence", "Translate page", "Next theme", "Keyboard shortcuts")) {
            assertTrue(rule.onAllNodesWithText(gone).fetchSemanticsNodes().isEmpty(), "$gone is not in the pane")
        }
    }

    /** A translation under its sentence starts where the sentence does, with or without the play buttons before sentences. */
    @Test
    fun aTranslationUnderItsSentenceLinesUpWithIt() = translationLinesUp(playButtons = false)

    @Test
    fun aTranslationLinesUpWithItsSentenceBesideAPlayButton() = translationLinesUp(playButtons = true)

    private fun translationLinesUp(playButtons: Boolean) {
        val vm = runBlocking { reader(mainIsDefault = false, translation = "The wolf sleeps in the forest.") }
        runBlocking { settings.update { it.copy(showTranslations = true, sideBySideTranslations = false, showSentencePlay = playButtons) } }
        // The page was loaded before translations were on; loading it again translates it.
        vm.refresh()
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("The wolf sleeps in the forest.").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("TRANSLATION_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path.replace(".png", "-$playButtons.png")))
        }
        val sentence = rule.onNodeWithText("lobo dorme", substring = true).fetchSemanticsNode().boundsInRoot
        val translation = rule.onNodeWithText("The wolf sleeps in the forest.").fetchSemanticsNode().boundsInRoot
        assertEquals(sentence.left, translation.left, 1f, "the translation starts under the sentence's first letter")
    }

    /** Once the page has a word being learned, the footer offers to practise it. */
    @Test
    fun theFooterOffersPracticeOnceAWordIsBeingLearned() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        val visited = mutableListOf<com.tayra.languages.core.ui.navigation.Route>()
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = { visited += it }, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(rule.onAllNodesWithText("Practice this page").fetchSemanticsNodes().isEmpty(), "every word is still unknown")

        vm.markToLearn(vm.index("lobo"))
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Practice this page").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Practice this page").performScrollTo().performClick()
        rule.waitUntil(5_000) { visited.isNotEmpty() }
        assertEquals(listOf<com.tayra.languages.core.ui.navigation.Route>(com.tayra.languages.core.ui.navigation.Route.Practice(1, 1)), visited)
    }

    /** Hovering the page's vocabulary bar explains it; ignored words count as known. */
    @Test
    fun hoveringTheVocabularyBarShowsTheCountsByStatus() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        fun status(word: String) = runBlocking { termRepository.findByTextLc(languageId, word)?.status }
        fun mark(word: String, expected: TermStatus) {
            vm.markToLearn(vm.index(word))
            rule.waitUntil(5_000) { status(word) == expected && vm.state.value.items[vm.index(word)].status == expected }
        }
        mark("lobo", TermStatus.NEW_1)
        mark("dorme", TermStatus.NEW_1)
        mark("dorme", TermStatus.WELL_KNOWN)
        mark("floresta", TermStatus.NEW_1)
        runBlocking { termService.setStatus(listOf(termRepository.findByTextLc(languageId, "floresta")!!.id), TermStatus.IGNORED, emptyMap()) }
        vm.refresh()
        rule.waitUntil(5_000) { vm.state.value.items[vm.index("floresta")].status == TermStatus.IGNORED }

        rule.onNodeWithTag(STATUS_BAR_TAG, useUnmergedTree = true).performMouseInput { moveTo(center) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Vocabulary on this page", substring = false).fetchSemanticsNodes().size > 1 }
        assertEquals(2, rule.onAllNodesWithText("2 words (40%)").fetchSemanticsNodes().size, "unknown: o, na; known: dorme and the ignored floresta")
        rule.onNodeWithText("1 word (20%)").assertExists()
        rule.onNodeWithText("5 words in total").assertExists()
        fun tooltipShown() = rule.onAllNodesWithText("5 words in total").fetchSemanticsNodes().isNotEmpty()
        // Longer than the 1.5 s a timed tooltip stays for; that timeout runs on real time.
        Thread.sleep(2_500)
        rule.waitForIdle()
        assertTrue(tooltipShown(), "the tooltip stays while the pointer is on the bar")
        System.getenv("STATUS_BAR_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        rule.onNodeWithTag(STATUS_BAR_TAG, useUnmergedTree = true).performMouseInput { moveTo(center + Offset(0f, 200f)) }
        rule.waitUntil(5_000) { !tooltipShown() }
    }

    /** The whole screen, as the app shows it: hovering a word with the mouse brings up its card. */
    @Test
    fun theReaderScreenShowsTheCardOfTheHoveredWord() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
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
        startKoin { modules(module { single { LocalSpeech(listOf(piper)) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
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

        vm.toggleSpeakWordOnClick()
        rule.waitUntil(2_000) { !settings.current.speakWordOnClick }
        click("lobo")
        rule.waitForIdle()
        Thread.sleep(500)
        assertEquals(listOf("pt:floresta"), spoken.toList())
    }

    /** Dragging across words colours the phrase's letters while the button is still held, and nothing else. */
    @Test
    fun aPhraseIsColouredWhileItIsBeingDragged() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val paragraph = rule.onNodeWithText("lobo dorme", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        paragraph.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val text = layouts.single().layoutInput.text.text
        val padding = with(rule.density) { 6.dp.toPx() }
        fun at(word: String): Offset = layouts.single().getBoundingBox(text.indexOf(word) + 1).let { Offset(it.center.x, it.center.y + padding) }
        fun selected(): String {
            val shown = paragraph.fetchSemanticsNode().config[SemanticsProperties.Text].single()
            return shown.getStringAnnotations(SELECTED_ANNOTATION, 0, shown.length).sortedBy { it.start }
                .joinToString("") { shown.text.substring(it.start, it.end) }
        }
        fun coloured(): String {
            val shown = paragraph.fetchSemanticsNode().config[SemanticsProperties.Text].single()
            return shown.spanStyles.filter { it.item.color == com.tayra.languages.core.ui.theme.AppThemes.default.selectedText }.sortedBy { it.start }
                .joinToString("") { shown.text.substring(it.start, it.end) }
        }

        paragraph.performMouseInput { moveTo(at("lobo")); press() }
        paragraph.performMouseInput { moveTo(at("dorme")); moveTo(at("floresta")) }
        rule.waitForIdle()
        assertEquals("lobo dorme na floresta", selected(), "selected before the button is released")
        assertEquals("lobo dorme na floresta", coloured(), "coloured before the button is released")
        paragraph.performMouseInput { release() }
        rule.waitForIdle()
        assertEquals("lobo dorme na floresta", selected(), "still selected while its term is open")
        assertEquals("lobo dorme na floresta", coloured(), "still coloured while its term is open")
    }

    /**
     * With a local engine the page's sentences are made ahead: the button spins until its audio is
     * ready, and a click then plays it without making it again.
     */
    @Test
    fun sentencesArePreparedBeforeTheirButtonIsClicked() {
        val made = java.util.Collections.synchronizedList(mutableListOf<String>())
        val piper = object : LocalSpeechEngine {
            override val engine = SpeechEngine.PIPER
            override val displayName = "Slow Piper"
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
                delay(1_000)
                made += text
                return silentWav()
            }
        }
        val speech = LocalSpeech(listOf(piper))
        val vm = runBlocking {
            val reader = reader(mainIsDefault = false, speech = speech)
            settings.update { it.copy(speechEngine = SpeechEngine.PIPER) }
            reader
        }
        startKoin { modules(module { single { speech }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        vm.goToPage(1)

        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Preparing sentence").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Play sentence").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("O lobo dorme na floresta."), made.toList())

        rule.onNodeWithContentDescription("Play sentence").performClick()
        rule.waitUntil(1_000) { rule.onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, made.size, "the prepared audio is played, not made again")
    }

    /** With a word selected, Ctrl (⌘) with the up and down arrows steps its status up and down. */
    @Test
    fun ctrlArrowsStepTheSelectedWordsStatus() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val wolf = vm.state.value.items.indexOfFirst { it.isWord && it.renderText == "lobo" }
        vm.onWordClick(wolf, shift = false)
        rule.waitForIdle()
        fun status() = runBlocking { termRepository.findByTextLc(languageId, "lobo")?.status }
        fun press(key: Key, meta: Boolean = false) = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput {
            val modifier = if (meta) Key.MetaLeft else Key.CtrlLeft
            keyDown(modifier)
            pressKey(key)
            keyUp(modifier)
        }

        press(Key.DirectionUp)
        rule.waitUntil(5_000) { status() == TermStatus.NEW_1 }
        press(Key.DirectionUp)
        rule.waitUntil(5_000) { status() == TermStatus.NEW_2 }
        press(Key.DirectionDown)
        rule.waitUntil(5_000) { status() == TermStatus.NEW_1 }
        press(Key.DirectionUp, meta = true)
        rule.waitUntil(5_000) { status() == TermStatus.NEW_2 }
        press(Key.DirectionDown, meta = true)
        rule.waitUntil(5_000) { status() == TermStatus.NEW_1 }
    }

    /** A word opened in the pane at status 3: Ctrl+↓ makes it 2, and the pane does not put it back. */
    @Test
    fun ctrlDownLowersAnOpenWordAndStaysLowered() {
        val vm = runBlocking {
            val reader = reader(mainIsDefault = false)
            val id = termRepository.findByTextLc(languageId, "lobo")!!.id
            termRepository.updateStatus(listOf(id), TermStatus.LEARNING_3)
            reader.refresh()
            reader
        }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { vm.state.value.items.any { it.renderText == "lobo" && it.status == TermStatus.LEARNING_3 } }
        vm.onWordClick(vm.state.value.items.indexOfFirst { it.isWord && it.renderText == "lobo" }, shift = false)
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.hasSetTextAction() and androidx.compose.ui.test.hasText("lobo")).fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        fun status() = runBlocking { termRepository.findByTextLc(languageId, "lobo")?.status }

        rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.DirectionDown)
            keyUp(Key.CtrlLeft)
        }
        rule.waitUntil(5_000) { status() == TermStatus.NEW_2 }
        Thread.sleep(1_500)
        rule.waitForIdle()
        assertEquals(TermStatus.NEW_2, status(), "the open pane must not restore the old status")
    }

    /** An unknown word opens with U selected; a translation typed in starts it at 1, and U puts it back. */
    @Test
    fun unknownWordsShowAndKeepTheUnknownStatus() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        vm.onWordClick(vm.state.value.items.indexOfFirst { it.isWord && it.renderText == "lobo" }, shift = false)
        fun status() = runBlocking { termRepository.findByTextLc(languageId, "lobo")?.status }
        fun selected(label: String) = rule.onAllNodes(androidx.compose.ui.test.hasText(label) and androidx.compose.ui.test.isSelected()).fetchSemanticsNodes().isNotEmpty()

        rule.waitUntil(5_000) { selected("U") }
        assertEquals(TermStatus.UNKNOWN, status())

        rule.onNode(androidx.compose.ui.test.hasSetTextAction() and androidx.compose.ui.test.hasText("Translation")).performTextInput("lobo em russo")
        rule.waitUntil(5_000) { status() == TermStatus.NEW_1 }
        rule.waitUntil(5_000) { selected("1") }

        rule.onNode(androidx.compose.ui.test.hasText("U") and androidx.compose.ui.test.hasClickAction()).performClick()
        rule.waitUntil(5_000) { status() == TermStatus.UNKNOWN }
        rule.waitUntil(5_000) { selected("U") }
    }

    /** Ctrl+Shift with the arrows jumps between coloured words, skipping known and ignored ones. */
    @Test
    fun ctrlShiftArrowsJumpBetweenColouredWords() {
        val vm = runBlocking {
            val reader = reader(mainIsDefault = false)
            termRepository.updateStatus(listOf(termRepository.findByTextLc(languageId, "lobo")!!.id), TermStatus.WELL_KNOWN)
            termRepository.updateStatus(listOf(termRepository.findByTextLc(languageId, "dorme")!!.id), TermStatus.IGNORED)
            reader.refresh()
            reader
        }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { vm.state.value.items.any { it.renderText == "dorme" && it.status == TermStatus.IGNORED } }
        fun press(key: Key) = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput {
            keyDown(Key.CtrlLeft); keyDown(Key.ShiftLeft)
            pressKey(key)
            keyUp(Key.ShiftLeft); keyUp(Key.CtrlLeft)
        }
        fun selected() = vm.state.value.marked.singleOrNull()?.let { vm.state.value.items[it].renderText }

        press(Key.DirectionRight)
        rule.waitUntil(2_000) { selected() == "O" }
        press(Key.DirectionRight)
        rule.waitUntil(2_000) { selected() == "na" }
        press(Key.DirectionRight)
        rule.waitUntil(2_000) { selected() == "floresta" }
        press(Key.DirectionLeft)
        rule.waitUntil(2_000) { selected() == "na" }
        press(Key.DirectionLeft)
        rule.waitUntil(2_000) { selected() == "O" }
    }

    /** E hides the term pane of the selected word and shows it again, keeping the word selected. */
    @Test
    fun eTogglesTheTermPane() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val wolf = vm.state.value.items.indexOfFirst { it.isWord && it.renderText == "lobo" }
        vm.onWordClick(wolf, shift = false)
        rule.waitUntil(5_000) { vm.state.value.panel != ReadingPanel.None }
        fun press() = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { pressKey(Key.E) }

        press()
        rule.waitUntil(2_000) { vm.state.value.panel == ReadingPanel.None }
        assertEquals(setOf(wolf), vm.state.value.marked, "the word stays selected")
        press()
        rule.waitUntil(2_000) { vm.state.value.panel != ReadingPanel.None }
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.hasSetTextAction() and androidx.compose.ui.test.hasText("lobo")).fetchSemanticsNodes().isNotEmpty() }
    }

    /** E with no word selected opens an empty pane whose term field looks words up as they are typed. */
    @Test
    fun eWithNothingSelectedOpensAnEmptyPaneThatLooksWordsUp() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        runBlocking { termService.save(termService.findOrNew(languageId, "lobo").copy(translation = "wolf", status = TermStatus.LEARNING_3)) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { pressKey(Key.E) }
        rule.waitUntil(2_000) { vm.state.value.panel == ReadingPanel.NewTerm(languageId, "") }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Type a word or phrase to look it up.").fetchSemanticsNodes().isNotEmpty() }
        val field = rule.onAllNodes(androidx.compose.ui.test.hasSetTextAction())[0]
        field.assertIsFocused()
        fun shows(text: String) = rule.onAllNodes(androidx.compose.ui.test.hasSetTextAction() and androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().isNotEmpty()

        field.performTextInput("casa")
        rule.waitUntil(5_000) { shows("<casa>") }
        field.performTextClearance()
        field.performTextInput("lobo")
        rule.waitUntil(5_000) { shows("wolf") }
        assertTrue(rule.onAllNodes(androidx.compose.ui.test.hasText("3") and androidx.compose.ui.test.isSelected()).fetchSemanticsNodes().isNotEmpty(), "the stored status shows")
        field.performTextClearance()
        field.performTextInput("floresta")
        rule.waitUntil(5_000) { shows("<floresta>") }
        rule.runOnIdle { vm.closePanel() }
        rule.waitForIdle()
        runBlocking {
            assertEquals(null, termRepository.findByTextLc(languageId, "casa"), "a looked-up word is not stored")
            assertEquals(TermStatus.UNKNOWN, termRepository.findByTextLc(languageId, "floresta")?.status, "looking a word up leaves it unknown")
            assertEquals("wolf", termRepository.findByTextLc(languageId, "lobo")?.translation)
        }
    }

    /** A word that starts being learned keeps the sentence it was read in, until it is known, ignored or unknown again. */
    @Test
    fun aWordStartingToBeLearnedKeepsItsSentence() = runBlocking {
        val vm = reader()
        suspend fun lobo() = termRepository.findByTextLc(languageId, "lobo")
        suspend fun await(status: TermStatus) = withTimeout(5_000) { while (lobo()?.status != status) delay(20) }

        vm.markToLearn(vm.index("lobo"))
        await(TermStatus.NEW_1)
        assertEquals("O lobo dorme na floresta.", lobo()?.sentence, "a right click stores the sentence")
        // The right click left the word selected, so the status keys act on it.
        vm.shiftStatus(1)
        await(TermStatus.NEW_2)
        assertEquals("O lobo dorme na floresta.", lobo()?.sentence, "kept while the word goes on being learned")
        vm.setStatus(TermStatus.WELL_KNOWN)
        await(TermStatus.WELL_KNOWN)
        assertNull(lobo()?.sentence, "cleared once the word is known")
        vm.markToLearn(vm.index("lobo"))
        await(TermStatus.NEW_1)
        assertEquals("O lobo dorme na floresta.", lobo()?.sentence)
        vm.setStatus(TermStatus.UNKNOWN)
        await(TermStatus.UNKNOWN)
        assertNull(lobo()?.sentence, "cleared once the word is unknown again")
    }

    /** A status picked in the term pane stores the sentence the word was opened from. */
    @Test
    fun theTermPaneStoresTheSentenceOfTheOpenedWord() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        vm.onWordClick(vm.index("lobo"), shift = false)
        val three = androidx.compose.ui.test.hasText("3") and androidx.compose.ui.test.hasClickAction()
        rule.waitUntil(5_000) { rule.onAllNodes(three).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("TERM_PANE_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        rule.onAllNodes(three)[0].performClick()
        fun lobo() = runBlocking { termRepository.findByTextLc(languageId, "lobo") }
        rule.waitUntil(5_000) { lobo()?.status == TermStatus.LEARNING_3 }
        assertEquals("O lobo dorme na floresta.", lobo()?.sentence)
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
