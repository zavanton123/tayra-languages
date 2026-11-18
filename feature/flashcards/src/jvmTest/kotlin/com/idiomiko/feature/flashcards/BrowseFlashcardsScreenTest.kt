package com.idiomiko.feature.flashcards

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.LinkAnnotation
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.export.AnkiPackagerImpl
import com.idiomiko.core.data.repository.FlashcardRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.dictionary.DictionaryId
import com.idiomiko.core.domain.dictionary.DictionaryLookup
import com.idiomiko.core.domain.dictionary.OfflineDictionary
import com.idiomiko.core.domain.export.AnkiDeckExportService
import com.idiomiko.core.domain.export.AnkiExportService
import com.idiomiko.core.domain.flashcards.AnkiArchive
import com.idiomiko.core.domain.flashcards.AnkiArchiveCard
import com.idiomiko.core.domain.flashcards.AnkiArchiveNote
import com.idiomiko.core.domain.flashcards.AnkiArchiveReader
import com.idiomiko.core.domain.flashcards.AnkiImportService
import com.idiomiko.core.domain.flashcards.AnswerMode
import com.idiomiko.core.domain.flashcards.CardState
import com.idiomiko.core.domain.flashcards.FlashcardService
import com.idiomiko.core.domain.flashcards.Rating
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.Term
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.service.LocalSpeech
import com.idiomiko.core.domain.service.MemorySpeechAudioCache
import com.idiomiko.core.domain.service.SentenceAudio
import com.idiomiko.core.domain.service.SentenceTranslator
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalFlashcardsDue
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.IdiomikoTheme
import com.russhwolf.settings.MapSettings
import java.io.File
import java.sql.DriverManager
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking

/** The browser lists every card, narrows them by search and group, opens one for its history, and suspends or resets a selection. */
@OptIn(ExperimentalTestApi::class)
class BrowseFlashcardsScreenTest {

    private val file = File.createTempFile("idiomiko-browse", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val cards = FlashcardRepositoryImpl(provider)
    private val service = FlashcardService(cards, terms, settings)
    private var comprar = 0L
    private var casa = 0L
    private var livro = 0L

    /** Three cards: one new, one answered Easy and so in review days away, one suspended. */
    /** Whether a back below the screen was reached, and the window's back, as Escape sends it. */
    private var wentBack = false
    private val backInput = DirectNavigationEventInput()

    /** Where the browser was asked to go. */
    private val visited = mutableListOf<Route>()

    private fun ComposeUiTest.show(
        width: Int = 1586,
        open: (() -> Long)? = null,
        importer: AnkiImportService? = null,
        exporter: AnkiDeckExportService? = null,
        saveFile: suspend (String, String, ByteArray) -> Boolean = { _, _, _ -> false },
        before: () -> Unit = {},
    ): BrowseFlashcardsViewModel {
        val languageId = runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = id, nativeLanguage = "ru") }
            comprar = terms.save(Term(languageId = id, text = "comprar", textLc = "comprar", status = TermStatus.NEW_1, translation = "купить", sentence = "Vou comprar pão."))
            casa = terms.save(Term(languageId = id, text = "casa", textLc = "casa", status = TermStatus.NEW_1, translation = "дом", sentence = "Uma frase com casa."))
            livro = terms.save(Term(languageId = id, text = "livro", textLc = "livro", status = TermStatus.NEW_1, translation = "книга", sentence = "Uma frase com livro."))
            service.answer(service.next(id, prefer = casa).card!!, Rating.EASY, AnswerMode.TYPE)
            service.setSuspended(livro, true)
            id
        }
        before()
        val viewModel = BrowseFlashcardsViewModel(service, languages, settings, terms, importer, exporter)
        @OptIn(ExperimentalComposeUiApi::class)
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(
                        LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {},
                        LocalFlashcardsDue provides 0,
                    ) {
                        // Below the screen, as the navigation's own way back is.
                        BackHandler { wentBack = true }
                        LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher?.let { dispatcher ->
                            DisposableEffect(dispatcher) { dispatcher.addInput(backInput); onDispose { dispatcher.removeInput(backInput) } }
                        }
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            BrowseFlashcardsScreen(onNavigate = { visited += it }, onBack = {}, termId = open?.invoke(), viewModel = viewModel, saveFile = saveFile)
                        }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("browse-row-$comprar").fetchSemanticsNodes().isNotEmpty() }
        return viewModel
    }

    private fun ComposeUiTest.save(env: String) {
        System.getenv(env)?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }

    /** The topmost window, a dialog over the page when one is open. */
    private fun ComposeUiTest.saveTop(env: String) {
        System.getenv(env)?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot()).fetchSemanticsNodes().indices.last().let { i -> onAllNodes(isRoot())[i] }.captureToImage().toAwtImage(), "png", File(it)) }
    }

    private fun sql(statement: String) = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.createStatement().execute(statement) }

    private fun ComposeUiTest.rows(): Int = listOf(comprar, casa, livro).count { onAllNodesWithTag("browse-row-$it").fetchSemanticsNodes().isNotEmpty() }

    private fun card(id: Long) = runBlocking { cards.get(id)!! }

    @Test
    fun theListNarrowsBySearchAndGroup() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        assertEquals(3, rows())
        onNodeWithText("3 cards").assertExists()
        // The review card shows where it stands in the scheduling words (the group chip says Scheduled too), apart from a word's
        // New or Learning; the first-time and suspended cards have no time to come yet.
        assertEquals(2, onAllNodesWithText("Scheduled").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithText("Review").fetchSemanticsNodes().size)
        assertEquals(2, onAllNodesWithText("Not shown yet").fetchSemanticsNodes().size)
        save("BROWSE_SCREENSHOT")
        assertTrue(onAllNodesWithTag("import-anki").fetchSemanticsNodes().isEmpty(), "no import where packages cannot be read")

        onNodeWithTag("browse-search").performTextInput("cas")
        waitUntil(timeoutMillis = 5_000) { rows() == 1 }
        onNodeWithTag("browse-row-$casa").assertExists()
        onNodeWithContentDescription("Clear search").performClick()
        waitUntil(timeoutMillis = 5_000) { rows() == 3 }

        onNodeWithTag("browse-view-NEW").performClick()
        waitUntil(timeoutMillis = 5_000) { rows() == 1 }
        onNodeWithTag("browse-row-$comprar").assertExists()
        onNodeWithTag("browse-view-SUSPENDED").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("browse-row-$livro").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, rows())
        onNodeWithTag("browse-view-ALL").performClick()
        waitUntil(timeoutMillis = 5_000) { rows() == 3 }
    }

    /** An Anki package's cards come in from the page's Import button, and the import says what it did. */
    @Test
    fun ankiCardsComeInAndTheImportSaysWhatItDid() = runDesktopComposeUiTest(width = 1500, height = 900) {
        val card = AnkiArchiveCard("Idiomiko::Portuguese", 2, 2, 400, null, 30, 3, 0, 0, 40.5, 5.25, null, emptyList())
        val note = AnkiArchiveNote(1, mapOf("Word" to "pretendo", "Language" to "pt", "Subtitle" to "Eu pretendo partir cedo.", "Word Definition" to "намереваюсь"), emptyList(), listOf(card))
        val reader = object : AnkiArchiveReader {
            override val available = true
            override suspend fun read(bytes: ByteArray) = AnkiArchive(Instant.parse("2026-02-14T04:00:00Z"), listOf(note))
        }
        val viewModel = show(importer = AnkiImportService(reader, cards, terms, languages, settings))
        onNodeWithTag("import-anki").assertExists()
        viewModel.importAnki(ByteArray(0))
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("1 new word saved with its card").fetchSemanticsNodes().isNotEmpty() }
        save("BROWSE_ANKI_SCREENSHOT")
        onNodeWithTag("anki-import-close").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("anki-import").fetchSemanticsNodes().isEmpty() }
        onNodeWithText("pretendo").assertExists()
        onNodeWithText("4 cards").assertExists()
    }

    /** The language's cards go to Anki as a deck with their progress, and the dialog says how to bring it in with it. */
    @Test
    fun theCardsGoToAnkiWithTheirProgress() = runDesktopComposeUiTest(width = 1500, height = 900) {
        val dictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = DictionaryLookup(emptyList(), emptyList())
        }
        val translator = object : SentenceTranslator {
            override suspend fun translate(text: String, language: Language): String? = null
        }
        val notes = AnkiExportService(terms, languages, dictionary, translator, SentenceAudio(LocalSpeech(emptyList()), settings, MemorySpeechAudioCache()), settings, AnkiPackagerImpl())
        val saved = mutableListOf<Pair<String, ByteArray>>()
        show(exporter = AnkiDeckExportService(cards, terms, languages, settings, notes, AnkiPackagerImpl()), saveFile = { name, extension, bytes -> saved += "$name.$extension" to bytes; true })
        onNodeWithTag("export-anki").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("3 cards saved with their progress").fetchSemanticsNodes().isNotEmpty() }
        saveTop("BROWSE_ANKI_EXPORT_SCREENSHOT")
        val (name, bytes) = saved.single()
        assertTrue(name.startsWith("Portuguese ") && name.endsWith(".apkg"), name)
        assertTrue(bytes.isNotEmpty())
        // Reading sends none of them again.
        assertTrue(runBlocking { terms.getByIds(listOf(comprar, casa, livro)) }.all { it.ankiExportedAt != null })
        onNodeWithTag("anki-export-close").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("anki-export").fetchSemanticsNodes().isEmpty() }
    }

    /** An export under way can be cancelled: the dialog closes, nothing is saved and no word counts as sent. */
    @Test
    fun anExportCanBeCancelled() = runDesktopComposeUiTest(width = 1500, height = 900) {
        // Lookups that never answer keep the export going until it is cancelled.
        val dictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId): Boolean = awaitCancellation()
            override suspend fun lookup(dictionary: DictionaryId, text: String): DictionaryLookup = awaitCancellation()
        }
        val translator = object : SentenceTranslator {
            override suspend fun translate(text: String, language: Language): String? = awaitCancellation()
        }
        val notes = AnkiExportService(terms, languages, dictionary, translator, SentenceAudio(LocalSpeech(emptyList()), settings, MemorySpeechAudioCache()), settings, AnkiPackagerImpl())
        val saved = mutableListOf<String>()
        show(exporter = AnkiDeckExportService(cards, terms, languages, settings, notes, AnkiPackagerImpl()), saveFile = { name, _, _ -> saved += name; true })
        onNodeWithTag("export-anki").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("anki-export-cancel").fetchSemanticsNodes().isNotEmpty() }
        saveTop("BROWSE_ANKI_EXPORT_CANCEL_SCREENSHOT")
        onNodeWithTag("anki-export-cancel").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("anki-export").fetchSemanticsNodes().isEmpty() }
        waitForIdle()
        assertEquals(emptyList(), saved)
        assertTrue(runBlocking { terms.getByIds(listOf(comprar, casa, livro)) }.none { it.ankiExportedAt != null })
    }

    /** The table says when each card was made, and sorts by it, the latest first. */
    @Test
    fun cardsSayWhenTheyWereMade() = runDesktopComposeUiTest(width = 1500, height = 700) {
        val day = 86_400_000L
        val now = System.currentTimeMillis()
        val viewModel = show(width = 1000) {
            sql("UPDATE flashcards SET created_at = ${now - 3 * day} WHERE term_id = $comprar")
            sql("UPDATE flashcards SET created_at = ${now - 14 * day} WHERE term_id = $casa")
            sql("UPDATE flashcards SET created_at = ${now - day} WHERE term_id = $livro")
        }
        onNodeWithText("3 days ago").assertExists()
        onNodeWithText("2 weeks ago").assertExists()
        // Created comes right after the sentence, before the state.
        val left = { label: String -> onNodeWithText(label).fetchSemanticsNode().boundsInRoot.left }
        assertTrue(left("Sentence") < left("Created") && left("Created") < left("State"))
        save("BROWSE_CREATED_SCREENSHOT")
        onNodeWithText("Created").performClick()
        waitForIdle()
        assertEquals(listOf(livro, comprar, casa), viewModel.state.value.rows.map { it.termId })
    }

    /** The figures over the table count their groups and show them when clicked. */
    @Test
    fun theFiguresShowTheirGroups() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val day = 86_400_000L
        // One card was made two days ago; the other two today.
        val viewModel = show { sql("UPDATE flashcards SET created_at = ${System.currentTimeMillis() - 2 * day} WHERE term_id = $livro") }
        onNodeWithTag("browse-figure-ALL").assertExists()
        save("BROWSE_DESKTOP_SCREENSHOT")
        assertEquals(2, viewModel.state.value.counts.addedToday)
        onNodeWithTag("browse-figure-ADDED_TODAY").performClick()
        waitForIdle()
        assertEquals(BrowseView.ADDED_TODAY, viewModel.state.value.view)
        assertEquals(setOf(comprar, casa), viewModel.state.value.rows.map { it.termId }.toSet())
        // It is not one of the chips.
        assertEquals(0, onAllNodesWithTag("browse-view-ADDED_TODAY").fetchSemanticsNodes().size)
        onNodeWithTag("browse-figure-ALL").performClick()
        waitForIdle()
        assertEquals(3, viewModel.state.value.rows.size)
    }

    /** Opened from the review screen, a card's details stand alone over it: the term's editor opens over them, and Done closes them. */
    @Test
    fun aCardsDetailsOpenOverAnotherScreen() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        val routes = mutableListOf<Route>()
        var closed = false
        // The browser's own content goes, leaving the details over a screen of their own.
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    FlashcardDetails(comprar, onNavigate = { routes += it }, onClose = { closed = true }, viewModel = viewModel)
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Flashcard details").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Vou comprar pão.").assertExists()
        onNodeWithText("Edit term").performClick()
        assertEquals(listOf<Route>(Route.EditTerm(comprar)), routes)
        onNodeWithTag("detail-done").performClick()
        waitUntil(timeoutMillis = 5_000) { closed }
    }

    /** On a desktop, the back a window makes of Escape stays in the browser rather than leaving it. */
    @Test
    fun escapeDoesNotLeaveTheBrowser() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        runOnUiThread { backInput.backCompleted() }
        waitForIdle()
        assertFalse(wentBack)
    }

    /** A wide browser leaves the review and the statistics to the bar's Flashcards menu. */
    @Test
    fun aWideBrowserHasNoReviewOrStatisticsButtons() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("browse-statistics").assertDoesNotExist()
        onNodeWithText("Statistics").assertDoesNotExist()
        onNodeWithText("Review 1 due").assertDoesNotExist()
        onNodeWithText("Review cards").assertDoesNotExist()
    }

    @Test
    fun aCardOpensWithItsHistoryAndCanBeReset() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("browse-row-$casa").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("review-history").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("browse-detail").assertExists()
        onNodeWithText("Flashcard details").assertExists()
        onNodeWithText("Easy").assertExists()
        onNodeWithTag("answered-by-TYPE", useUnmergedTree = true).assertExists()
        // Every figure shows, the last one too, though Edit term and Suspend went down beside Done.
        onNodeWithText("Recall now").assertIsDisplayed()
        onNodeWithTag("detail-suspend").assertIsDisplayed()
        saveTop("BROWSE_DETAIL_SCREENSHOT")

        onNodeWithText("Reset progress").performClick()
        onNodeWithText("Reset 1 flashcard?").assertExists()
        onNodeWithText("Reset").performClick()
        waitUntil(timeoutMillis = 5_000) { card(casa).schedule.state == CardState.NEW }
        // A new card has nothing to reset; its history stays, as in Anki.
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Reset progress").fetchSemanticsNodes().isEmpty() }
        onNodeWithText("Easy").assertExists()
        onNodeWithTag("detail-done").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("browse-detail").fetchSemanticsNodes().isEmpty() }
    }

    /** A word of the sentence clicked in the card's details becomes the one the card hides. */
    @Test
    fun aWordClickedInTheSentenceIsTheOneHidden() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        onNodeWithTag("browse-row-$comprar").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.detail?.wordRange == 4 until 11 }
        onNodeWithText("Click another word to hide it on the card instead.").assertExists()
        onNodeWithTag("detail-sentence").performFirstLinkClick { (it.item as? LinkAnnotation.Clickable)?.tag == "pão" }
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.detail?.wordRange == 12 until 15 }
        assertEquals("pão", runBlocking { terms.getById(comprar)!!.cardForm })
    }

    /** The details show the sentence's translation kept with the word, and it is written over by hand there. */
    @Test
    fun theSentencesTranslationIsWrittenByHand() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show { runBlocking { terms.updateSentenceTranslation(comprar, "I'll buy bread.") } }
        onNodeWithTag("browse-row-$comprar").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("detail-translation").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("detail-translation").assertTextEquals("I'll buy bread.")
        onNodeWithTag("translation-edit").performClick()
        onNodeWithTag("translation-field").performTextClearance()
        onNodeWithTag("translation-field").performTextInput("Я куплю хлеб.")
        // Kept as it is typed; Enter ends the typing.
        waitUntil(timeoutMillis = 5_000) { runBlocking { terms.getById(comprar)!!.sentenceTranslation } == "Я куплю хлеб." }
        onNodeWithTag("translation-field").performKeyInput { pressKey(Key.Enter) }
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.detail?.row?.sentenceTranslation == "Я куплю хлеб." }
        onNodeWithTag("detail-translation").assertTextEquals("Я куплю хлеб.")
        saveTop("BROWSE_TRANSLATION_SCREENSHOT")

        // Cancel puts back the translation from before the typing, though the typing was kept meanwhile.
        onNodeWithTag("translation-edit").performClick()
        onNodeWithTag("translation-field").performTextClearance()
        onNodeWithTag("translation-field").performTextInput("Куплю.")
        waitUntil(timeoutMillis = 5_000) { runBlocking { terms.getById(comprar)!!.sentenceTranslation } == "Куплю." }
        onNodeWithTag("translation-field-cancel").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { terms.getById(comprar)!!.sentenceTranslation } == "Я куплю хлеб." }
    }

    /** A term edited over the card's details, as in its editor, shows there as it is now once the editor closes. */
    @Test
    fun theDetailsFollowTheTermEditedOverThem() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        onNodeWithTag("browse-row-$comprar").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.detail != null }
        runBlocking { terms.save(terms.getById(comprar)!!.copy(translation = "покупать")) }
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.detail?.row?.translation == "покупать" }
        // In the details and in the card's row under them.
        assertEquals(2, onAllNodesWithText("покупать").fetchSemanticsNodes().size)
    }

    /** The sentence the card is built on can be changed in the card info, in place. */
    @Test
    fun theSentenceIsEditedInTheCardInfo() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(before = { runBlocking { terms.updateSentence(casa, null) } })
        onNodeWithTag("browse-row-$casa").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("sentence-edit").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Add a sentence").performClick()
        onNodeWithTag("sentence-field").performTextInput("A casa é grande.")
        // Kept as it is typed, with no Save to press; Enter ends the typing.
        waitUntil(timeoutMillis = 5_000) { runBlocking { terms.getById(casa)!!.sentence } == "A casa é grande." }
        onNodeWithTag("sentence-field").performKeyInput { pressKey(Key.Enter) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("A casa é grande.").fetchSemanticsNodes().size == 2 }
        // The word is marked where the card blanks it out.
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.detail?.wordRange == 2..5 }
        saveTop("BROWSE_SENTENCE_SCREENSHOT")
        // The pencil opens the sentence again; what is typed is kept, and Cancel puts the sentence back as it was.
        onNodeWithTag("sentence-edit").performClick()
        onNodeWithTag("sentence-field").performTextClearance()
        onNodeWithTag("sentence-field").performTextInput("A casa é grande. Muito.")
        waitUntil(timeoutMillis = 5_000) { runBlocking { terms.getById(casa)!!.sentence } == "A casa é grande. Muito." }
        onNodeWithTag("sentence-field-cancel").performClick()
        assertEquals(0, onAllNodesWithTag("sentence-field").fetchSemanticsNodes().size)
        waitUntil(timeoutMillis = 5_000) { runBlocking { terms.getById(casa)!!.sentence } == "A casa é grande." }
    }

    /** Opened for a card, as from the review screen's Edit flashcard, the browser shows that card's info. */
    @Test
    fun theBrowserOpensTheCardItWasOpenedFor() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(open = { livro })
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("browse-detail").fetchSemanticsNodes().isNotEmpty() }
        // The translation shows in the row and in the info; only the suspended card offers Resume.
        assertEquals(2, onAllNodesWithText("книга").fetchSemanticsNodes().size)
        onNodeWithText("Resume").assertExists()
    }

    /** Long lists come in pages of the size chosen, kept for the next time; selecting all takes every page's cards. */
    @Test
    fun cardsComeInPages() = runDesktopComposeUiTest(width = 1586, height = 3200) {
        val viewModel = show(before = {
            runBlocking {
                val id = settings.current.currentLanguageId
                (1..30).forEach { n -> terms.save(Term(languageId = id, text = "palavra$n", textLc = "palavra$n", status = TermStatus.NEW_1, translation = "слово", sentence = "Uma frase com palavra$n.")) }
            }
            // The cards of the new words are made when the browser loads.
        })
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.rows.size == 33 }
        waitForIdle()
        onNodeWithText("1–33 of 33").assertExists()

        onNodeWithText("50 per page").performClick()
        onNodeWithText("25 per page").performClick()
        waitForIdle()
        onNodeWithText("1–25 of 33").assertExists()
        assertEquals(25, onAllNodes(SemanticsMatcher("a card's row") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("browse-row-") == true }).fetchSemanticsNodes().size)
        assertEquals(25, settings.current.flashcardBrowsePageSize)
        save("BROWSE_PAGES_SCREENSHOT")

        onNodeWithContentDescription("Next page").performClick()
        waitForIdle()
        onNodeWithText("26–33 of 33").assertExists()
        assertEquals(8, viewModel.state.value.pageRows.size)

        onNodeWithTag("browse-select-all").performClick()
        waitForIdle()
        assertEquals(33, viewModel.state.value.selected.size)
        onNodeWithText("33 of 33 selected").assertExists()

        // A new search starts again from the first page.
        viewModel.setSearch("palavra")
        waitForIdle()
        onNodeWithText("1–25 of 30").assertExists()
    }

    /** A card is deleted from its info, after asking, with its word; the selection deletes several. */
    @Test
    fun cardsAreDeletedWithTheirWords() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("browse-row-$casa").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("detail-delete").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("detail-delete").performClick()
        onNodeWithText("Delete 1 flashcard?").assertExists()
        onNode(androidx.compose.ui.test.hasText("Delete") and androidx.compose.ui.test.hasClickAction() and androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.isDialog())).performClick()
        waitUntil(timeoutMillis = 5_000) { rows() == 2 }
        assertEquals(null, runBlocking { terms.getById(casa) })
        assertEquals(0, onAllNodesWithTag("browse-detail").fetchSemanticsNodes().size, "the deleted card's info closes")

        onNodeWithTag("browse-select-all").performClick()
        onNodeWithTag("delete-selected").performClick()
        onNodeWithText("Delete 2 flashcards?").assertExists()
        onNode(androidx.compose.ui.test.hasText("Delete") and androidx.compose.ui.test.hasClickAction() and androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.isDialog())).performClick()
        waitUntil(timeoutMillis = 5_000) { rows() == 0 }
        assertEquals(null, runBlocking { terms.getById(comprar) })
    }

    @Test
    fun selectedCardsAreSuspendedTogether() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("browse-select-all").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("selection-bar").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("3 selected").assertExists()
        onNodeWithText("Suspend").performClick()
        waitUntil(timeoutMillis = 5_000) { card(comprar).suspended && card(casa).suspended }
        assertTrue(card(livro).suspended)
        // The selection is cleared with the action done.
        assertEquals(0, onAllNodesWithTag("selection-bar").fetchSemanticsNodes().size)
        onNodeWithTag("browse-select-all").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Resume").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Resume").performClick()
        waitUntil(timeoutMillis = 5_000) { !card(livro).suspended && !card(comprar).suspended }
    }

    @Test
    fun selectedCardsGetADueDateOrMoveAmongTheNewCards() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        val before = card(casa).schedule.due
        onNodeWithTag("browse-select-all").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("set-due").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("set-due").performClick()
        onNodeWithText("Set the due date of 1 card").assertExists()
        onNodeWithContentDescription("Days from today").performTextClearance()
        onNodeWithContentDescription("Days from today").performTextInput("5-x")
        onNodeWithTag("set-due-confirm").assertIsNotEnabled()
        onNodeWithContentDescription("Days from today").performTextClearance()
        onNodeWithContentDescription("Days from today").performTextInput("20")
        save("SET_DUE_SCREENSHOT")
        onNodeWithTag("set-due-confirm").performClick()
        waitUntil(timeoutMillis = 5_000) { card(casa).schedule.due != before }
        assertTrue(card(casa).schedule.due > Clock.System.now() + 19.days)
        assertEquals(CardState.NEW, card(comprar).schedule.state, "a new card is left alone")

        // The new card among the selection moves to the end of the new cards.
        onNodeWithTag("browse-select-all").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("learn-last").fetchSemanticsNodes().isNotEmpty() }
        val saved = card(comprar).schedule.due
        onNodeWithTag("learn-last").performClick()
        waitUntil(timeoutMillis = 5_000) { card(comprar).schedule.due != saved }
    }

    /** A phone shows the Anki import and export as buttons, the three figures, no review, and the cards a page at a time. */
    @Test
    fun aPhonePagesTheCardsUnderTheAnkiButtons() = runDesktopComposeUiTest(width = 420, height = 900) {
        val reader = object : AnkiArchiveReader {
            override val available = true
            override suspend fun read(bytes: ByteArray) = AnkiArchive(Instant.parse("2026-02-14T04:00:00Z"), emptyList())
        }
        val dictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = DictionaryLookup(emptyList(), emptyList())
        }
        val translator = object : SentenceTranslator {
            override suspend fun translate(text: String, language: Language): String? = null
        }
        val notes = AnkiExportService(terms, languages, dictionary, translator, SentenceAudio(LocalSpeech(emptyList()), settings, MemorySpeechAudioCache()), settings, AnkiPackagerImpl())
        val viewModel = show(
            width = 420,
            importer = AnkiImportService(reader, cards, terms, languages, settings),
            exporter = AnkiDeckExportService(cards, terms, languages, settings, notes, AnkiPackagerImpl()),
            before = {
                runBlocking {
                    val id = settings.current.currentLanguageId
                    (1..30).forEach { n -> terms.save(Term(languageId = id, text = "palavra$n", textLc = "palavra$n", status = TermStatus.NEW_1, translation = "слово", sentence = "Uma frase com palavra$n.")) }
                }
            },
        )
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.rows.size == 33 }
        onNodeWithTag("import-anki").assertIsDisplayed()
        onNodeWithTag("export-anki").assertIsDisplayed()
        for (view in listOf("ALL", "DUE", "ADDED_TODAY")) onNodeWithTag("browse-figure-$view").assertIsDisplayed()
        onNodeWithTag("phone-review").assertDoesNotExist()
        save("BROWSE_PHONE_ANKI_SCREENSHOT")
        viewModel.setPageSize(25)
        waitForIdle()
        // The list makes only the cards in view, so the page is counted in the state.
        assertEquals(25, viewModel.state.value.pageRows.size)
        // The pager comes after the page's cards, at the foot of the list.
        onAllNodes(hasScrollToIndexAction())[0].performScrollToNode(hasContentDescription("Next page"))
        onNodeWithContentDescription("Next page").performClick()
        waitForIdle()
        assertEquals(8, viewModel.state.value.pageRows.size)
        // The new page starts at its first card, under the figures and groups.
        onNodeWithTag("browse-row-${viewModel.state.value.pageRows.first().termId}").assertIsDisplayed()
    }

    @Test
    fun aPhoneOpensTheCardOnAPageOfItsOwn() = runDesktopComposeUiTest(width = 420, height = 900) {
        show(width = 420)
        onNodeWithTag("phone-sort").assertExists()
        save("BROWSE_PHONE_LIST_SCREENSHOT")
        // The review is in the Flashcards tab's sheet, not here.
        onNodeWithTag("phone-review").assertDoesNotExist()
        // Select, from the bar's menu, puts checkboxes on the cards; a tap then picks the card instead of opening it.
        onNodeWithTag("phone-menu").performClick()
        onNodeWithTag("phone-select").performClick()
        onNodeWithTag("browse-row-$comprar").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("selection-bar").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("phone-select-done").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("selection-bar").fetchSemanticsNodes().isEmpty() }
        // A long press starts selecting with that card.
        onNodeWithTag("browse-row-$casa").performTouchInput { longClick() }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("1 of 3 selected").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("phone-select-done").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("selection-bar").fetchSemanticsNodes().isEmpty() }
        onNodeWithTag("browse-row-$casa").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("review-history").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("browse-detail").assertExists()
        onNodeWithText("Danger zone").assertExists()
        saveTop("BROWSE_PHONE_SCREENSHOT")
        onNodeWithTag("detail-back").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("browse-detail").fetchSemanticsNodes().isEmpty() }
        // Done closes it too, from the page's foot, in view however far the page is scrolled.
        onNodeWithTag("browse-row-$casa").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("detail-done").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("detail-done").assertIsDisplayed().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("browse-detail").fetchSemanticsNodes().isEmpty() }
    }
}
