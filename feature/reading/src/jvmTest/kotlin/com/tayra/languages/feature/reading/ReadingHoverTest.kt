package com.tayra.languages.feature.reading

import androidx.compose.ui.test.assertCountEquals
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
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.input.key.Key
import com.tayra.languages.core.data.network.createHttpClient
import com.tayra.languages.core.data.network.KtorRecordingFetcher
import com.tayra.languages.core.domain.service.ExampleRecordings
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import androidx.compose.ui.unit.height
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.performMouseInput
import kotlinx.coroutines.flow.first
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import com.tayra.languages.core.ui.theme.AppSpacing
import androidx.compose.ui.test.isRoot
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
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.feature.terms.form.TermFormViewModel
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.examples.ExamplesSearchViewModel
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
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
import androidx.compose.ui.test.performTouchInput
import kotlin.test.assertEquals
import kotlin.test.assertNull
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.runtime.CompositionLocalProvider
import com.tayra.languages.core.ui.components.LocalWindowWidth
import com.tayra.languages.core.ui.components.WindowWidth
import com.tayra.languages.core.data.export.AnkiPackagerImpl
import com.tayra.languages.core.domain.export.AnkiExportService
import com.tayra.languages.core.domain.export.AnkiAddResult
import com.tayra.languages.core.domain.export.AnkiCollection
import com.tayra.languages.core.domain.export.AnkiConnection

class ReadingHoverTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var settings: SettingsRepositoryImpl
    private lateinit var termRepository: TermRepositoryImpl
    private lateinit var termService: TermService
    private lateinit var provider: DatabaseProvider
    private var languageId = 0L

    /** The reader's prepared sentence audio, shared with the screen's speaker through Koin. */
    private lateinit var sentenceAudio: SentenceAudio
    private lateinit var languageRepository: LanguageRepositoryImpl
    private lateinit var examplesProvider: ExampleSentencesProvider

    /** What the term pane needs when a click opens it on the real screen. */
    private lateinit var termPane: org.koin.core.module.Module

    /** The practice setup dialog's view model, on this reader's book. */
    private lateinit var practicePane: org.koin.core.module.Module

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    /** [mainIsDefault] swaps the UI thread for a pool, for tests that never draw the screen. */
    private suspend fun reader(
        mainIsDefault: Boolean = true,
        speech: LocalSpeech = LocalSpeech(emptyList()),
        pages: Int = 1,
        translation: String? = null,
        lesson: LessonReading? = null,
        /** The term pane's example sentences; none by default. */
        examples: ExampleSentencesProvider? = null,
        /** The term pane's online translation suggestion for a word. */
        suggest: suspend (String) -> String? = { "<$it>" },
        /** How the book's paragraphs (one per [pages]) are cut into pages: one each by default. */
        wordsPerPage: Int = 5,
        language: Language = Language(name = "Portuguese"),
        paragraph: String = "O lobo dorme na floresta.",
        /** A running Anki the text's words can be sent to; without one the reader offers no Anki export. */
        anki: AnkiConnection? = null,
    ): ReadingViewModel {
        if (mainIsDefault) Dispatchers.setMain(Dispatchers.Default)
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-hover", ".db").also { it.delete() })).also { this.provider = it }
        val languages = LanguageRepositoryImpl(provider).also { languageRepository = it }
        val books = BookRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        termRepository = terms
        settings = SettingsRepositoryImpl(MapSettings())
        val termService = TermService(terms, languages).also { this.termService = it }
        val readingService = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), termService)
        val bookService = BookService(books, languages)
        languageId = languages.save(language)
        val bookId = bookService.create(BookDraft(languageId = languageId, title = "T", text = List(pages) { paragraph }.joinToString("\n\n"), wordsPerPage = wordsPerPage))
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = suggest(text)
        }
        val offline = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val words = WordTranslationService(terms, offline, engine, settings)
        val noExamples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult.EMPTY
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        examplesProvider = examples ?: noExamples
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
                val dictionaries = DictionaryService(noPacks, noEntries)
                TermFormViewModel(key, termService, terms, languages, settings, engine, examplesProvider, dictionaries, dictionaries)
            }
            viewModel { (id: Long, text: String) -> ExamplesSearchViewModel(id, text, languageRepository, settings, examplesProvider) }
        }
        sentenceAudio = SentenceAudio(speech, settings, MemorySpeechAudioCache())
        practicePane = module {
            viewModel { (source: com.tayra.languages.core.domain.practice.PracticeSource) ->
                com.tayra.languages.feature.terms.list.PracticeSetupViewModel(
                    source,
                    com.tayra.languages.core.domain.practice.PracticeMaterialLoader(books, languages, readingService, terms),
                    object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = translation },
                )
            }
        }
        val vm = ReadingViewModel(
            bookId, null, readingService, books, termService,
            TermPopupBuilder(terms, languages, readingService), BookStatsService(books, languages, settings, readingService), settings,
            object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = translation },
            LocalTranslation(null), speech, words, sentenceAudio,
            lessonOf = { lesson },
            anki = anki?.let { AnkiExportService(terms, languages, offline, object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null }, sentenceAudio, settings, AnkiPackagerImpl()) },
            ankiConnection = anki,
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
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Mark remaining words as known").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Mark remaining words as known").performScrollTo()
        rule.onNodeWithTag("footer-badge", useUnmergedTree = true).assertTextEquals("5")
        System.getenv("READING_FOOTER_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }

        rule.onNodeWithContentDescription("Mark remaining words as known").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Mark remaining words as known").fetchSemanticsNodes().isEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("5 words marked as known").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("READING_TOAST_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        assertEquals(TermStatus.WELL_KNOWN, runBlocking { termRepository.findByTextLc(languageId, "lobo") }?.status)
        rule.waitUntil(8_000) { rule.onAllNodesWithText("5 words marked as known").fetchSemanticsNodes().isEmpty() }

        rule.onNodeWithContentDescription("Finish book").performScrollTo().performClick()
        rule.waitUntil(5_000) { home }
    }

    /** Beside the play controls, a round button puts the text in a playlist, or makes one with it. */
    @Test
    fun theReaderPutsTheTextInAPlaylist() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val courses = com.tayra.languages.core.domain.courses.CourseService(books, languages, BookService(books, languages), com.tayra.languages.core.data.repository.CourseRepositoryImpl(provider)) { emptyList() }
        val time = com.tayra.languages.core.domain.playlists.ListeningTime(settings)
        val repository = com.tayra.languages.core.data.repository.PlaylistRepositoryImpl(provider)
        val playlists = com.tayra.languages.core.domain.playlists.PlaylistService(repository, courses, languages, time)
        val silent = object : com.tayra.languages.core.domain.playlists.SpeechOutput {
            override fun play(wav: ByteArray, onDone: () -> Unit) {}
            override fun speak(text: String, languageCode: String?, rate: Float, onWord: (IntRange) -> Unit, onDone: () -> Unit) {}
            override fun pause() = false
            override fun resume() {}
            override fun stop() {}
        }
        val player = com.tayra.languages.core.domain.playlists.PlaylistPlayer(
            playlists, repository, books, languages, ReadingService(books, languages, termRepository, WordsReadRepositoryImpl(provider), termService), sentenceAudio,
            LocalSpeech(emptyList()), settings, silent, time, kotlinx.coroutines.CoroutineScope(Dispatchers.Default),
        )
        val morning = runBlocking {
            settings.update { it.copy(currentLanguageId = languageId) }
            playlists.create(languageId, com.tayra.languages.core.domain.playlists.PlaylistDraft("Morning Portuguese"))
        }
        val koin = startKoin {
            modules(
                module {
                    single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio }; single { player }
                    viewModel { (source: com.tayra.languages.core.domain.playlists.PlaylistSource) -> com.tayra.languages.feature.playlists.AddToPlaylistViewModel(source, playlists, settings) }
                    viewModel { (mode: com.tayra.languages.feature.playlists.PlaylistFormMode) -> com.tayra.languages.feature.playlists.PlaylistFormViewModel(mode, playlists, settings) }
                },
            )
        }
        val visited = mutableListOf<com.tayra.languages.core.ui.navigation.Route>()
        // Given its Koin outright: compositions of earlier tests can leave another one as the default.
        @Suppress("DEPRECATION")
        rule.setContent {
            org.koin.compose.KoinContext(koin.koin) {
                CompositionLocalProvider(LocalWindowWidth provides WindowWidth.COMPACT) {
                    ReadingScreen(bookId = 1, initialPage = null, onNavigate = { visited += it }, onHome = {}, viewModel = vm)
                }
            }
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("add-to-playlist").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("add-to-playlist").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Morning Portuguese").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("menu-playlist-$morning").performClick()
        // The menu closes and the reader's toast confirms.
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Added to Morning Portuguese").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("add-to-playlist-menu").fetchSemanticsNodes().isEmpty() }
        assertEquals(listOf(1L), runBlocking { playlists.playlist(morning)!!.items.map { it.bookId } })
        System.getenv("READING_PLAYLIST_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        rule.onNodeWithTag("add-to-playlist").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("menu-new-playlist").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("menu-new-playlist").performClick()
        // A new playlist is made in a dialog over the reader, starting with the open text.
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("new-playlist-dialog").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("playlist-name").performTextInput("Evening Portuguese")
        rule.onNodeWithTag("form-save").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Added to Evening Portuguese").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("new-playlist-dialog").fetchSemanticsNodes().isEmpty() }
        val evening = runBlocking { playlists.observePlaylists(languageId).first().single { it.name == "Evening Portuguese" } }
        assertEquals(listOf(1L), evening.items.map { it.bookId })
        assertEquals(emptyList(), visited, "the reader stays open")
    }

    /** The compact toolbar shows the page count, while the footer keeps the page-turning action. */
    @Test
    fun theToolbarShowsThePagePositionAndMovesBetweenPages() {
        val vm = runBlocking { reader(mainIsDefault = false, pages = 12) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent {
            CompositionLocalProvider(LocalWindowWidth provides WindowWidth.COMPACT) {
                ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm)
            }
        }
        val count = vm.state.value.pageCount
        assertTrue(count > 2, "the book should have several pages")
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Page 1 of $count").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, rule.onAllNodesWithText("T").fetchSemanticsNodes().size, "a phone shows the book title only in its toolbar")
        assertTrue(rule.onAllNodesWithContentDescription("Previous page").fetchSemanticsNodes().isEmpty(), "the compact toolbar stays informational")
        rule.onNodeWithContentDescription("Next page").performScrollTo().performClick()
        rule.waitUntil(5_000) { vm.state.value.pageNumber == 2 }
        rule.onNodeWithContentDescription("Page 2 of $count").assertExists()
        System.getenv("PAGE_PROGRESS_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
    }

    /** A one-page text still has the compact title bar, but no page controls that cannot do anything. */
    @Test
    fun theCompactToolbarHidesThePagerForAOnePageText() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent {
            CompositionLocalProvider(LocalWindowWidth provides WindowWidth.COMPACT) {
                ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm)
            }
        }

        rule.waitUntil(5_000) { rule.onAllNodesWithText("T").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, vm.state.value.pageCount)
        assertEquals(1, rule.onAllNodesWithText("T").fetchSemanticsNodes().size, "the title is not repeated above the text")
        assertTrue(rule.onAllNodesWithContentDescription("Page 1 of 1").fetchSemanticsNodes().isEmpty(), "a single page has no redundant page count")
        assertTrue(rule.onAllNodesWithContentDescription("Previous page").fetchSemanticsNodes().isEmpty(), "a single page has no previous-page control")
        assertTrue(rule.onAllNodesWithContentDescription("Next page").fetchSemanticsNodes().isEmpty(), "a single page has no next-page control")
        assertEquals(2, rule.onAllNodesWithContentDescription("Reader settings").fetchSemanticsNodes().size, "settings stay available in the toolbar and stable action bar")
        rule.onNodeWithContentDescription("Back").assertExists()
    }

    /** Playback stays reachable in a fixed compact action bar with accessible touch targets. */
    @Test
    fun theCompactReaderKeepsSentencePlaybackActionsStable() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent {
            CompositionLocalProvider(LocalWindowWidth provides WindowWidth.COMPACT) {
                ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm)
            }
        }

        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Read the page").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Previous sentence").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Next sentence").assertIsEnabled()
        for (description in listOf("Reader settings", "Previous sentence", "Read the page", "Next sentence")) {
            val index = if (description == "Reader settings") 1 else 0
            val control = rule.onAllNodesWithContentDescription(description)[index].getBoundsInRoot()
            assertTrue(control.right - control.left >= 48.dp, "$description is at least 48dp wide")
            assertTrue(control.bottom - control.top >= 48.dp, "$description is at least 48dp tall")
        }
        assertEquals(1, vm.state.value.pageNumber, "sentence controls do not replace page navigation")
    }

    /** Turning read-aloud controls off removes playback actions without removing reader settings. */
    @Test
    fun theCompactReaderRespectsTheReadAloudPreference() {
        val vm = runBlocking {
            reader(mainIsDefault = false).also {
                settings.update { current -> current.copy(showSentencePlay = false) }
            }
        }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent {
            CompositionLocalProvider(LocalWindowWidth provides WindowWidth.COMPACT) {
                ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm)
            }
        }

        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Reader settings").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, rule.onAllNodesWithContentDescription("Reader settings").fetchSemanticsNodes().size, "only the toolbar action remains")
        assertTrue(rule.onAllNodesWithContentDescription("Read the page").fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithContentDescription("Previous sentence").fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithContentDescription("Next sentence").fetchSemanticsNodes().isEmpty())
    }

    /** The menu's Edit section opens the whole book or the current page for editing, and adds or deletes no pages. */
    @Test
    fun theMenuEditsTheBookOrThePage() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        val visited = mutableListOf<com.tayra.languages.core.ui.navigation.Route>()
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = { visited += it }, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Reader settings").performClick()
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
        val courses = mutableListOf<String>()
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = { visited += it }, onHome = { back++ }, onCourse = { courses += it }, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Finish lesson").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("LESSON_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        assertTrue(rule.onAllNodesWithText("Back to library").fetchSemanticsNodes().isEmpty(), "Back to library is not shown in a lesson")
        assertTrue(rule.onAllNodesWithContentDescription("Finish book").fetchSemanticsNodes().isEmpty(), "Finish book is not shown in a lesson")
        // Every way out of a lesson leads to its course, never to the library.
        rule.onNodeWithText(course.title).performClick()
        rule.waitUntil(5_000) { courses.size == 1 }
        rule.onNodeWithText("Back to course").performScrollTo().performClick()
        rule.waitUntil(5_000) { courses.size == 2 }
        rule.onNodeWithContentDescription("Finish lesson").performScrollTo().performClick()
        rule.waitUntil(5_000) { courses.size == 3 }
        assertEquals(List(3) { course.id }, courses)
        assertEquals(0, back, "a lesson never leaves for the library")

        rule.onNodeWithContentDescription("Reader settings").performClick()
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
        rule.onNodeWithContentDescription("Reader settings").performClick()
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
        rule.onNodeWithText("Speak selected words").assertExists()
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

    /** A long setting name, as in Russian, goes above its choices rather than being squeezed beside them. */
    @Test
    fun aLongSettingNameGoesAboveItsChoices() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        com.tayra.languages.core.ui.i18n.UiLanguage.set("ru")
        try {
            rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
            rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithContentDescription("Настройки чтения").performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithText("Предложения").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Предложения").performScrollTo()
            System.getenv("READER_PANE_SCREENSHOT")?.let { path ->
                rule.waitForIdle()
                javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File("$path-ru.png"))
            }
            val title = rule.onNodeWithText("Предложения").getBoundsInRoot()
            val choice = rule.onNodeWithText("По одному в строке").getBoundsInRoot()
            assertTrue(title.height < 40.dp, "one line, not one letter per line: ${title.height}")
            assertTrue(choice.top >= title.bottom, "the choices sit below the name")
        } finally {
            com.tayra.languages.core.ui.i18n.UiLanguage.set("en")
        }
    }

    /** Typography offers the reading fonts; picking one saves it and the row shows it. */
    @Test
    fun theMenuChangesTheReadingFont() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Reader settings").performClick()
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
        rule.onNodeWithContentDescription("Reader settings").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("tool-bookmarks").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("tool-bookmarks").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Add bookmark to page 1").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("add-bookmark").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("bookmark-1").fetchSemanticsNodes().isNotEmpty() }
        // The bookmarked page's button now takes its bookmark away.
        rule.onNodeWithText("Remove bookmark from page 1").assertExists()
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
    /** A click on the word the pane shows leaves the pane open; only its close button puts it away. */
    @Test
    fun clickingTheShownWordAgainKeepsThePane() = runBlocking {
        val vm = reader()
        vm.onWordClick(vm.index("lobo"), shift = false)
        delay(200)
        val panel = vm.state.value.panel
        assertTrue(panel != ReadingPanel.None)
        vm.onWordClick(vm.index("lobo"), shift = false)
        delay(200)
        assertEquals(panel, vm.state.value.panel, "the same pane, still open")
        assertEquals(setOf(vm.index("lobo")), vm.state.value.marked)
        vm.closePanel()
        delay(200)
        assertEquals(ReadingPanel.None, vm.state.value.panel)
    }

    /** A phone's reader bar opens the appearance straight away, in the part-height sheet over the text. */
    @Test
    fun aPhonesAppearanceButtonOpensTheAppearanceTab() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent {
            CompositionLocalProvider(LocalWindowWidth provides WindowWidth.COMPACT) {
                ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm)
            }
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Appearance").performClick()
        // The font is on the Appearance tab, reached without choosing the tab.
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("font-choice").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("lobo dorme", substring = true).assertCountEquals(1)
    }

    /** On a phone the pane is a sheet over a live page: a tap on the text above it does not put it away. */
    @Test
    fun aPhonesTermSheetStaysOpenWhenTheTextIsTapped() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(com.tayra.languages.core.ui.components.LocalWindowWidth provides com.tayra.languages.core.ui.components.WindowWidth.COMPACT) {
                ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm)
            }
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        vm.onWordTap(vm.index("lobo"))
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("term-pane-header", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        // A touch on the page above the sheet: the text's own, not a scrim's.
        val root = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0]
        root.performTouchInput { click(Offset(width / 2f, height * 0.2f)) }
        rule.waitForIdle()
        assertTrue(vm.state.value.panel != ReadingPanel.None, "the pane stays open")
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("term-pane-header", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        // Another word shows that word in the same sheet.
        vm.onWordTap(vm.index("floresta"))
        rule.waitUntil(5_000) { vm.state.value.marked == setOf(vm.index("floresta")) && vm.state.value.panel != ReadingPanel.None }
        // The new word's form loads before its heading shows.
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("term-pane-header", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

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
        // Several pages: a one-page text offers only the whole text's practice.
        val vm = runBlocking { reader(mainIsDefault = false, pages = 3) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, practicePane) }
        val visited = mutableListOf<com.tayra.languages.core.ui.navigation.Route>()
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = { visited += it }, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(rule.onAllNodesWithContentDescription("Practise this page").fetchSemanticsNodes().isEmpty(), "every word is still unknown")

        vm.markToLearn(vm.index("lobo"))
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Practise this page").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Practise this page").performScrollTo().performClick()
        // The page's practice starts from its setup.
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Practise 1 word").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("start-practice").performClick()
        rule.waitUntil(5_000) { visited.isNotEmpty() }
        val setup = com.tayra.languages.core.domain.practice.PracticePreset.DEFAULT.setup.encode()
        assertEquals(listOf<com.tayra.languages.core.ui.navigation.Route>(com.tayra.languages.core.ui.navigation.Route.Practice(1, 1, setup = setup)), visited)
    }

    /** On the last page the footer offers every word being learned in the text, after asking for the kinds of question. */
    @Test
    fun theLastPageOffersTheWholeTextsWordsWithTheKindsChosen() {
        val vm = runBlocking { reader(mainIsDefault = false, pages = 3) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, practicePane) }
        val visited = mutableListOf<com.tayra.languages.core.ui.navigation.Route>()
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = { visited += it }, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        vm.markToLearn(vm.index("lobo"))
        rule.waitUntil(5_000) { vm.state.value.items.any { it.renderText == "lobo" && it.status.isLearning } }
        // Not the last page yet.
        assertTrue(rule.onAllNodesWithTag("practice-all").fetchSemanticsNodes().isEmpty())

        vm.goToPage(vm.state.value.pageCount)
        rule.waitUntil(5_000) { vm.state.value.isLastPage && vm.state.value.pageNumber > 1 && vm.state.value.items.any { it.isWord } }
        vm.markToLearn(vm.index("floresta"))
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("practice-all").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("PRACTICE_ALL_SCREENSHOT")?.let { path ->
            rule.waitForIdle()
            javax.imageio.ImageIO.write(rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
        }
        rule.onNodeWithContentDescription("Practise the book's words").performScrollTo().performClick()
        // Both pages' words: lobo, and floresta from this one.
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Practise 2 words").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("practice-response-TYPE").performScrollTo().performClick()
        rule.onNodeWithTag("start-practice").performClick()
        rule.waitUntil(5_000) { visited.isNotEmpty() }
        val route = visited.single() as com.tayra.languages.core.ui.navigation.Route.Practice
        assertEquals(null, route.page, "the whole text")
        val asked = com.tayra.languages.core.domain.practice.PracticeSetup.decode(route.setup).rotation
        assertTrue(asked.isNotEmpty() && asked.none { it.second == com.tayra.languages.core.domain.practice.PracticeResponse.TYPE }, asked.toString())
    }

    /** At the end of the text the footer puts the words being learned on all its pages into Anki: here straight into a running one. */
    @Test
    fun theLastPageSendsTheTextsWordsToARunningAnki() {
        val added = mutableListOf<AnkiCollection>()
        val running = object : AnkiConnection {
            override suspend fun version() = 6
            override suspend fun add(collection: AnkiCollection): AnkiAddResult {
                added += collection
                return AnkiAddResult(collection.notes.indices.toSet(), emptySet(), emptyMap())
            }
        }
        val vm = runBlocking { reader(mainIsDefault = false, pages = 3, anki = running) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, practicePane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        vm.markToLearn(vm.index("lobo"))
        rule.waitUntil(5_000) { vm.state.value.items.any { it.renderText == "lobo" && it.status.isLearning } }
        assertTrue(rule.onAllNodesWithTag("export-to-anki").fetchSemanticsNodes().isEmpty(), "not on the first page")

        vm.goToPage(vm.state.value.pageCount)
        rule.waitUntil(5_000) { vm.state.value.isLastPage && vm.state.value.pageNumber > 1 && vm.state.value.items.any { it.isWord } }
        vm.markToLearn(vm.index("floresta"))
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("export-to-anki").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Export to Anki").performScrollTo().performClick()
        rule.waitUntil(10_000) { added.isNotEmpty() }
        // Both pages' words: lobo, and floresta from this one.
        assertEquals(2, added.single().notes.size)
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Added 2 cards to Anki").fetchSemanticsNodes().isNotEmpty() }
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
        // A click elsewhere while the tooltip is up reaches its target the first time: a focusable tooltip would swallow it.
        val paragraph = rule.onNodeWithText("lobo dorme", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        paragraph.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val box = layouts.single().getBoundingBox(layouts.single().layoutInput.text.text.indexOf("lobo") + 1)
        paragraph.performMouseInput { click(Offset(box.center.x, box.center.y + 6.dp.toPx())) }
        rule.waitUntil(5_000) { vm.index("lobo") in vm.state.value.marked }
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

    /**
     * Every Chinese word is the one the mouse is over, before and after a word is saved that joins
     * the characters it was read as (我 and 们 becoming 我们) and moves the words after it.
     */
    @Test
    fun theHoveredChineseWordIsTheOneUnderTheMouse() {
        val mandarin = com.tayra.languages.core.domain.language.LanguageDefinitions.all.first { it.name == "Mandarin Chinese" }.language
        val vm = runBlocking { reader(mainIsDefault = false, language = mandarin, paragraph = "我们是学生。他们也是学生，我们都喜欢读书。", wordsPerPage = 100) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("我们是学生", substring = true).fetchSemanticsNodes().isNotEmpty() }
        fun check(stage: String) {
            rule.waitForIdle()
            val paragraph = rule.onNodeWithText("我们是学生", substring = true)
            val layouts = mutableListOf<TextLayoutResult>()
            paragraph.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            val layout = layouts.single()
            val text = layout.layoutInput.text.text
            val items = vm.state.value.items
            var offset = 0
            val wrong = mutableListOf<String>()
            items.forEachIndexed { index, item ->
                val start = text.indexOf(item.renderText, offset).takeIf { it >= 0 } ?: return@forEachIndexed
                offset = start + item.renderText.length
                if (!item.isWord) return@forEachIndexed
                val box = layout.getBoundingBox(start)
                paragraph.performMouseInput { moveTo(Offset(box.center.x, box.center.y + 6.dp.toPx())) }
                rule.waitForIdle()
                val hovered = vm.state.value.hovered
                if (hovered != index) wrong += "${item.renderText}@$index got ${hovered?.let { items[it].renderText }}@$hovered"
            }
            paragraph.performMouseInput { moveTo(Offset(-50f, -50f)) }
            assertTrue(wrong.isEmpty(), "$stage: $wrong")
        }
        check("as read")
        runBlocking {
            termService.save(com.tayra.languages.core.domain.model.TermDraft(languageId = languageId, text = "我们", originalText = "", status = TermStatus.NEW_1))
        }
        vm.refresh()
        rule.waitUntil(5_000) { vm.state.value.items.any { it.renderText == "我们" } }
        check("after saving 我们")
        // A click picks the word under it too.
        val paragraph = rule.onNodeWithText("我们是学生", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        paragraph.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val box = layouts.single().getBoundingBox(layouts.single().layoutInput.text.text.indexOf("是"))
        paragraph.performMouseInput { click(Offset(box.center.x, box.center.y + 6.dp.toPx())) }
        rule.waitUntil(5_000) { vm.state.value.marked.isNotEmpty() }
        assertEquals("是", vm.state.value.items[vm.state.value.marked.single()].renderText)
    }

    @Test
    fun everyLetterOfAWordHoversIt() = assertEveryLetterHovers("Portuguese", "O lobo dorme na floresta e sonha com 3 coelhos.")

    /** Right to left, the far half of a word's last letter is its left half; numbers and Latin words sit among the words. */
    @Test
    fun everyLetterOfAHebrewWordHoversIt() = assertEveryLetterHovers("Hebrew", "הילד קרא 3 ספרים על Python בבית הספר.")

    @Test
    fun everyLetterOfAnArabicWordHoversIt() = assertEveryLetterHovers("Arabic", "ذهب الولد إلى المدرسة في الصباح مع 3 كتب.")

    /** The marks of a pointed text sit on the letters, so the mouse over them is over the word. */
    @Test
    fun everyLetterOfAVowelledArabicWordHoversIt() = assertEveryLetterHovers("Arabic", "ذَهَبَ الْوَلَدُ إِلَى الْمَدْرَسَةِ صَبَاحًا.")

    @Test
    fun everyLetterOfAPointedHebrewWordHoversIt() = assertEveryLetterHovers("Hebrew", "הַיֶּלֶד קָרָא סֵפֶר בַּבַּיִת.")

    /** A word ending in a vowel sign (है, पानी) ends in a letter the sign is drawn with. */
    @Test
    fun everyLetterOfAHindiWordHoversIt() = assertEveryLetterHovers("Hindi", "लड़का किताब पढ़ता है और पानी पीता है।")

    /** 𠮷 is one character of two UTF-16 units, so the word after it begins two units on. */
    @Test
    fun everyLetterOfAJapaneseWordHoversIt() = assertEveryLetterHovers("Japanese", "𠮷野家で牛丼を食べた。")

    /** The mouse anywhere across a word of [paragraph], from edge to edge, hovers that word. */
    private fun assertEveryLetterHovers(name: String, paragraph: String) {
        val language = com.tayra.languages.core.domain.language.LanguageDefinitions.all.first { it.name == name }.language
        val vm = runBlocking { reader(mainIsDefault = false, language = language, paragraph = paragraph, wordsPerPage = 100) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        val opening = paragraph.substring(0, 8)
        rule.waitUntil(5_000) { rule.onAllNodesWithText(opening, substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        val node = rule.onNodeWithText(opening, substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        val text = layout.layoutInput.text.text
        val items = vm.state.value.items
        var offset = 0
        val wrong = mutableListOf<String>()
        items.forEachIndexed { index, item ->
            val start = text.indexOf(item.renderText, offset).takeIf { it >= 0 } ?: return@forEachIndexed
            offset = start + item.renderText.length
            if (!item.isWord) return@forEachIndexed
            if (layout.getLineForOffset(start) != layout.getLineForOffset(offset - 1)) return@forEachIndexed
            // From the caret before the word to the caret after it, whichever way it runs: a mark has no box of its own.
            val a = layout.getHorizontalPosition(start, usePrimaryDirection = true)
            val b = layout.getHorizontalPosition(offset, usePrimaryDirection = true)
            val left = minOf(a, b)
            val width = maxOf(a, b) - left
            val line = layout.getLineForOffset(start)
            val y = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2
            for (i in 0..20) {
                val x = left + 0.5f + (width - 1f) * i / 20f
                node.performMouseInput { moveTo(Offset(x, y + 6.dp.toPx())) }
                rule.waitForIdle()
                val hovered = vm.state.value.hovered
                if (hovered != index) wrong += "${item.renderText}@${(100 * (x - left) / width).toInt()}% got ${hovered?.let { items[it].renderText }}"
            }
        }
        assertTrue(wrong.isEmpty(), wrong.toString())
    }

    /** A phrase saved as one term joins the words it was read as; the words after it are still the ones under the mouse. */
    @Test
    fun theHoveredWordIsTheOneUnderTheMouseAfterAPhraseIsSaved() {
        val vm = runBlocking { reader(mainIsDefault = false, paragraph = "O lobo dorme na floresta e o lobo sonha.", wordsPerPage = 100) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        // The reader is in use before the phrase is saved: its pointer handlers are already running.
        rule.onNodeWithText("lobo dorme", substring = true).performMouseInput { moveTo(center) }
        rule.waitForIdle()
        runBlocking { termService.save(com.tayra.languages.core.domain.model.TermDraft(languageId = languageId, text = "o lobo", originalText = "", status = TermStatus.NEW_1)) }
        vm.refresh()
        rule.waitUntil(5_000) { vm.state.value.items.any { it.renderText == "O lobo" } }
        rule.waitForIdle()
        val paragraph = rule.onNodeWithText("dorme na floresta", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        paragraph.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        val text = layout.layoutInput.text.text
        val wrong = mutableListOf<String>()
        for (word in listOf("dorme", "floresta", "sonha")) {
            val box = layout.getBoundingBox(text.indexOf(word) + 1)
            paragraph.performMouseInput { moveTo(Offset(box.center.x, box.center.y + 6.dp.toPx())) }
            rule.waitForIdle()
            val hovered = vm.state.value.hovered?.let { vm.state.value.items[it].renderText }
            if (hovered != word) wrong += "$word got $hovered"
        }
        assertTrue(wrong.isEmpty(), wrong.toString())
    }

    @Test
    fun noCardDuringADragSelection() = runBlocking {
        val vm = reader()
        vm.startSelection(vm.state.value.items[vm.index("lobo")].index)
        assertNull(vm.hoverCard("floresta"))
    }

    /** The pane's "View all examples" opens the examples at the first click, with speech, translations and split sentences on. */
    @Test
    fun viewAllExamplesOpensAtTheFirstClick() {
        val examples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult(listOf(ExampleSentence(text = "O lobo uiva.", translation = "The wolf howls.")), 762, null)
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        val vm = runBlocking { reader(mainIsDefault = false, translation = "The wolf sleeps in the forest.", examples = examples) }
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
                delay(300)
                return silentWav()
            }
        }
        runBlocking { settings.update { it.copy(speechEngine = SpeechEngine.PIPER, showSentencePlay = true, showTranslations = true, splitSentences = true, sideBySideTranslations = true) } }
        startKoin { modules(module { single { LocalSpeech(listOf(piper)) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        val visited = mutableListOf<com.tayra.languages.core.ui.navigation.Route>()
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = { visited += it }, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val paragraph = rule.onNodeWithText("lobo dorme", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        paragraph.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val box = layouts.single().getBoundingBox(layouts.single().layoutInput.text.text.indexOf("lobo") + 1)
        paragraph.performMouseInput { click(Offset(box.center.x, box.center.y + 6.dp.toPx())) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("View all", substring = true).fetchSemanticsNodes().isNotEmpty() }

        rule.onNodeWithText("View all", substring = true).performScrollTo().performMouseInput {
            moveTo(center)
            press()
            advanceEventTime(120)
            release()
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("examples-dialog").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Examples for ‘lobo’").assertExists()
        assertEquals(emptyList(), visited, "examples stay over the reader")
        rule.onNodeWithTag("close-examples-dialog").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("examples-dialog").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("lobo dorme", substring = true).assertExists()
        rule.onNodeWithText("View all", substring = true).assertExists()
    }

    /** The examples button beside the word heading the pane opens the examples of that word. */
    @Test
    fun theHeadingsExamplesButtonOpensTheExamples() {
        val vm = runBlocking { reader(mainIsDefault = false, translation = "The wolf sleeps in the forest.") }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        val visited = mutableListOf<com.tayra.languages.core.ui.navigation.Route>()
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = { visited += it }, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val paragraph = rule.onNodeWithText("lobo dorme", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        paragraph.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val box = layouts.single().getBoundingBox(layouts.single().layoutInput.text.text.indexOf("lobo") + 1)
        paragraph.performMouseInput { click(Offset(box.center.x, box.center.y + 6.dp.toPx())) }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("term-heading").fetchSemanticsNodes().isNotEmpty() }

        rule.onNodeWithContentDescription("Examples").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("examples-dialog").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Examples for ‘lobo’").assertExists()
        assertEquals(emptyList(), visited, "the heading opens examples without replacing the reader")
        // On a desktop window the dialog fills it but for its margin.
        val window = rule.onAllNodes(isRoot())[0].fetchSemanticsNode().size
        val dialog = rule.onNodeWithTag("examples-dialog").fetchSemanticsNode().size
        val margin = with(rule.density) { 2 * AppSpacing.l.roundToPx() } + 1
        assertTrue(dialog.width >= window.width - margin && dialog.height >= window.height - margin, "the dialog $dialog in the window $window")
        rule.onNodeWithTag("close-examples-dialog").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("examples-dialog").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithTag("term-heading").assertExists()
    }

    /** Coming back to the reader (from the examples, say) finds the page scrolled where it was left; a new page starts at its top. */
    @Test
    fun comingBackToTheReaderKeepsThePlaceOnThePage() {
        val vm = runBlocking { reader(mainIsDefault = false, pages = 80, wordsPerPage = 1_000) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        // Shown and hidden through a saveable state holder, as the navigation keeps a back-stack entry's state.
        val shown = androidx.compose.runtime.mutableStateOf(true)
        rule.setContent {
            val holder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
            if (shown.value) holder.SaveableStateProvider("reader") { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().size > 10 }
        rule.onAllNodesWithText("lobo dorme", substring = true)[60].performScrollTo()
        rule.waitForIdle()
        // Positions are unclipped, so a paragraph scrolled off the top sits above the root.
        val before = rule.onAllNodesWithText("lobo dorme", substring = true)[60].fetchSemanticsNode().positionInRoot
        assertTrue(rule.onAllNodesWithText("lobo dorme", substring = true)[0].fetchSemanticsNode().positionInRoot.y < 0, "the page is scrolled down")

        shown.value = false
        rule.waitForIdle()
        shown.value = true
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().size > 10 }
        val after = rule.onAllNodesWithText("lobo dorme", substring = true)[60].fetchSemanticsNode().positionInRoot
        assertEquals(before.y, after.y, "the paragraph is where it was")
    }

    /** Back from another screen with the term pane still open (the examples are opened from it), the hotkeys work again without a click on the text first. */
    @Test
    fun hotkeysWorkAgainAfterComingBackWithThePaneOpen() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        val shown = androidx.compose.runtime.mutableStateOf(true)
        rule.setContent {
            val holder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
            if (shown.value) {
                holder.SaveableStateProvider("reader") { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
            } else {
                // The other screen takes the keyboard, as the examples list does.
                val focus = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.focusRequester(focus).focusable())
                androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }
            }
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        vm.onWordClick(vm.index("lobo"), shift = false)
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.hasSetTextAction() and androidx.compose.ui.test.hasText("Translation")).fetchSemanticsNodes().isNotEmpty() }

        shown.value = false
        rule.waitForIdle()
        shown.value = true
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val font = settings.current.readingFontScale
        rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.Equals); keyUp(Key.CtrlLeft) }
        rule.waitUntil(2_000) { settings.current.readingFontScale > font }
    }

    /** The display hotkeys work after a click in the term pane (a button keeps the focus) and stay out of a field being typed in. */
    @Test
    fun displayHotkeysWorkAfterAClickInThePaneButNotWhileTyping() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        vm.onWordClick(vm.index("lobo"), shift = false)
        val translationField = androidx.compose.ui.test.hasSetTextAction() and androidx.compose.ui.test.hasText("Translation")
        rule.waitUntil(5_000) { rule.onAllNodes(translationField).fetchSemanticsNodes().isNotEmpty() }
        fun larger() = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.Equals); keyUp(Key.CtrlLeft) }

        // A status button in the pane takes the focus when clicked; the hotkey still sizes the text.
        rule.onNode(androidx.compose.ui.test.hasText("3") and androidx.compose.ui.test.hasClickAction()).performClick()
        rule.waitUntil(5_000) { runBlocking { termRepository.findByTextLc(languageId, "lobo") }?.status == TermStatus.LEARNING_3 }
        val font = settings.current.readingFontScale
        larger()
        rule.waitUntil(2_000) { settings.current.readingFontScale > font }

        // Typing in the translation field: Ctrl + = is the field's, not a hotkey.
        rule.onNode(translationField).performClick()
        rule.waitForIdle()
        val typed = settings.current.readingFontScale
        larger()
        rule.waitForIdle()
        Thread.sleep(300)
        assertEquals(typed, settings.current.readingFontScale, "no hotkey while typing")
    }

    /** The pane's buttons stay put while the translation is looked up online: a reply arriving as the user clicks must not move them. */
    @Test
    fun thePaneDoesNotShiftWhenTheTranslationLookupEnds() {
        val examples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult(listOf(ExampleSentence(text = "O lobo uiva.", translation = "The wolf howls.")), 762, null)
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        // The lookup answers only when the test lets it, after the examples are on screen.
        val reply = kotlinx.coroutines.CompletableDeferred<String?>()
        val vm = runBlocking { reader(mainIsDefault = false, examples = examples, suggest = { reply.await() }) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        vm.onWordClick(vm.index("lobo"), shift = false)
        rule.waitUntil(5_000) { rule.onAllNodesWithText("View all", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Looking up translation...").assertExists()
        // The dictionaries card sits below the translation field, in view; the examples and their button under it would move the same way.
        val before = rule.onNodeWithText("Dictionaries").fetchSemanticsNode().boundsInRoot

        reply.complete("wolf")
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Looking up translation...").fetchSemanticsNodes().isEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("wolf").fetchSemanticsNodes().isNotEmpty() }
        val after = rule.onNodeWithText("Dictionaries").fetchSemanticsNode().boundsInRoot
        assertEquals(before.top, after.top, "what is below the field is where it was")
    }

    /** With "Speak selected words" on, a click reads the word aloud with the chosen engine; with it off, nothing is said. */
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
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.hasTestTag("term-heading") and androidx.compose.ui.test.hasText("lobo")).fetchSemanticsNodes().isNotEmpty() }
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
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.hasTestTag("term-heading") and androidx.compose.ui.test.hasText("lobo")).fetchSemanticsNodes().isNotEmpty() }
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

    /** Marking the rest of a page known can be undone: its words go back to unknown. */
    @Test
    fun markingTheRestKnownCanBeUndone() = runBlocking {
        val vm = reader()
        suspend fun lobo() = termRepository.findByTextLc(languageId, "lobo")?.status
        withTimeout(5_000) { while (vm.state.value.items.isEmpty()) delay(20) }
        vm.markPageRead(true, 0)
        withTimeout(5_000) { while (!vm.canUndoMarkKnown) delay(20) }
        assertEquals(TermStatus.WELL_KNOWN, lobo())
        vm.undoMarkKnown()
        withTimeout(5_000) { while (lobo() == TermStatus.WELL_KNOWN) delay(20) }
        assertEquals(TermStatus.UNKNOWN, lobo() ?: TermStatus.UNKNOWN)
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

    /** A status set on another screen (the examples, say) shows once the reader comes back to the front. */
    @Test
    fun comingBackToTheReaderShowsStatusesChangedElsewhere() = runBlocking {
        val vm = reader()
        vm.onResumed()
        assertEquals(TermStatus.UNKNOWN, vm.state.value.items[vm.index("lobo")].status)
        termService.save(termService.findOrNew(languageId, "lobo").copy(status = TermStatus.LEARNING_3))
        vm.onResumed()
        withTimeout(5_000) { while (vm.state.value.items[vm.index("lobo")].status != TermStatus.LEARNING_3) delay(20) }
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

    /** A phrase picked in the reader and given a status in the term pane stores the sentences it spans, past a full stop too. */
    @Test
    fun theTermPaneStoresTheSentencesOfASelectedPhrase() {
        val vm = runBlocking { reader(mainIsDefault = false, paragraph = "O lobo dorme. A raposa corre na floresta.", wordsPerPage = 100) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }, termPane) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        val items = vm.state.value.items
        vm.startSelection(items[vm.index("dorme")].index)
        vm.endSelection(items[vm.index("A")].index, copy = false)
        val three = androidx.compose.ui.test.hasText("3") and androidx.compose.ui.test.hasClickAction()
        rule.waitUntil(5_000) { rule.onAllNodes(three).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(three)[0].performClick()
        fun phrase() = runBlocking { termRepository.list(com.tayra.languages.core.domain.repository.TermListFilter(languageId = languageId, minStatus = TermStatus.LEARNING_3, maxStatus = TermStatus.LEARNING_3), com.tayra.languages.core.domain.repository.TermListSort(), 0, 5) }.items.singleOrNull()
        rule.waitUntil(5_000) { phrase() != null }
        assertEquals("dorme. A", phrase()?.displayText)
        assertEquals("O lobo dorme. A raposa corre na floresta.", phrase()?.sentence)
    }

    /**
     * A saved phrase running past a full stop is read in both sentences, and so is a word of the
     * second, which is shown without the words the phrase covers.
     */
    @Test
    fun aPhraseAcrossAFullStopIsReadInBothSentences() = runBlocking {
        val vm = reader(paragraph = "O lobo dorme. A raposa corre na floresta. O lobo sonha.", wordsPerPage = 100)
        val id = termService.save(termService.findOrNew(languageId, "dorme. A").copy(status = TermStatus.WELL_KNOWN))
        vm.refresh()
        withTimeout(5_000) { while (vm.index("dorme. A") < 0) delay(20) }
        suspend fun sentence(id: Long?) = id?.let { termRepository.getById(it) }?.takeIf { it.status.isLearning }?.sentence

        vm.markToLearn(vm.index("dorme. A"))
        withTimeout(5_000) { while (sentence(id) == null) delay(20) }
        assertEquals("O lobo dorme. A raposa corre na floresta.", sentence(id))
        vm.markToLearn(vm.index("raposa"))
        withTimeout(5_000) { while (sentence(termRepository.findByTextLc(languageId, "raposa")?.id) == null) delay(20) }
        assertEquals("O lobo dorme. A raposa corre na floresta.", sentence(termRepository.findByTextLc(languageId, "raposa")?.id))
        vm.markToLearn(vm.index("sonha"))
        withTimeout(5_000) { while (sentence(termRepository.findByTextLc(languageId, "sonha")?.id) == null) delay(20) }
        assertEquals("O lobo sonha.", sentence(termRepository.findByTextLc(languageId, "sonha")?.id), "the sentences after it are their own")
    }

    /** A status given to several words and a phrase at once in the bulk pane stores the sentences they are read in. */
    @Test
    fun theBulkPaneStoresTheSentences() = runBlocking {
        val vm = reader(paragraph = "O lobo dorme. A raposa corre na floresta.", wordsPerPage = 100)
        val lobo = termService.save(termService.findOrNew(languageId, "lobo").copy(status = TermStatus.WELL_KNOWN))
        val phrase = termService.save(termService.findOrNew(languageId, "raposa corre").copy(status = TermStatus.WELL_KNOWN))
        vm.refresh()
        withTimeout(5_000) { while (vm.index("raposa corre") < 0) delay(20) }

        vm.onWordClick(vm.index("lobo"), shift = true)
        vm.onWordClick(vm.index("raposa corre"), shift = true)
        withTimeout(5_000) { while ((vm.state.value.panel as? ReadingPanel.BulkEdit)?.termIds?.size != 2) delay(20) }
        vm.applyBulkUpdate(com.tayra.languages.core.domain.service.BulkTermUpdate(emptyList(), status = TermStatus.NEW_1))
        withTimeout(5_000) { while (termRepository.getById(phrase)?.status != TermStatus.NEW_1) delay(20) }
        assertEquals("O lobo dorme.", termRepository.getById(lobo)?.sentence)
        assertEquals("A raposa corre na floresta.", termRepository.getById(phrase)?.sentence)
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
