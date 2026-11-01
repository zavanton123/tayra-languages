package com.tayra.languages.feature.flashcards

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.flashcards.AnkiArchive
import com.tayra.languages.core.domain.flashcards.AnkiArchiveCard
import com.tayra.languages.core.domain.flashcards.AnkiArchiveNote
import com.tayra.languages.core.domain.flashcards.AnkiArchiveReader
import com.tayra.languages.core.domain.flashcards.AnkiImportService
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalFlashcardsDue
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.DriverManager
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The browser lists every card, narrows them by search and group, opens one for its history, and suspends or resets a selection. */
@OptIn(ExperimentalTestApi::class)
class BrowseFlashcardsScreenTest {

    private val file = File.createTempFile("tayra-browse", ".db").also { it.delete() }
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
    private fun ComposeUiTest.show(width: Int = 1586, open: (() -> Long)? = null, importer: AnkiImportService? = null, before: () -> Unit = {}): BrowseFlashcardsViewModel {
        val languageId = runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = id, nativeLanguage = "ru") }
            comprar = terms.save(Term(languageId = id, text = "comprar", textLc = "comprar", status = TermStatus.NEW_1, translation = "купить", sentence = "Vou comprar pão."))
            casa = terms.save(Term(languageId = id, text = "casa", textLc = "casa", status = TermStatus.NEW_1, translation = "дом"))
            livro = terms.save(Term(languageId = id, text = "livro", textLc = "livro", status = TermStatus.NEW_1, translation = "книга"))
            service.answer(service.next(id, prefer = casa).card!!, Rating.EASY)
            service.setSuspended(livro, true)
            id
        }
        before()
        val viewModel = BrowseFlashcardsViewModel(service, languages, settings, terms, importer)
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(
                        LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {},
                        LocalFlashcardsDue provides 0,
                    ) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            BrowseFlashcardsScreen(onNavigate = {}, onBack = {}, termId = open?.invoke(), viewModel = viewModel)
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

    /** An Anki package's cards come in from the button beside Review cards, and the import says what it did. */
    @Test
    fun ankiCardsComeInAndTheImportSaysWhatItDid() = runDesktopComposeUiTest(width = 1500, height = 900) {
        val card = AnkiArchiveCard("Tayra Languages::Portuguese", 2, 2, 400, null, 30, 3, 0, 0, 40.5, 5.25, null, emptyList())
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
        save("BROWSE_CREATED_SCREENSHOT")
        onNodeWithText("Created").performClick()
        waitForIdle()
        assertEquals(listOf(livro, comprar, casa), viewModel.state.value.rows.map { it.termId })
    }

    @Test
    fun aCardOpensWithItsHistoryAndCanBeReset() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("browse-row-$casa").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("review-history").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("browse-detail").assertExists()
        onNodeWithText("Easy").assertExists()
        save("BROWSE_DETAIL_SCREENSHOT")

        onNodeWithText("Reset progress").performClick()
        onNodeWithText("Reset 1 flashcard?").assertExists()
        onNodeWithText("Reset").performClick()
        waitUntil(timeoutMillis = 5_000) { card(casa).schedule.state == CardState.NEW }
        // A new card has nothing to reset; its history stays, as in Anki.
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Reset progress").fetchSemanticsNodes().isEmpty() }
        onNodeWithText("Easy").assertExists()
    }

    /** The sentence the card is built on can be changed in the card info, in place. */
    @Test
    fun theSentenceIsEditedInTheCardInfo() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("browse-row-$casa").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("sentence-edit").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Add a sentence").performClick()
        onNodeWithTag("sentence-field").performTextInput("A casa é grande.")
        onNodeWithTag("sentence-save").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { terms.getById(casa)!!.sentence } == "A casa é grande." }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("A casa é grande.").fetchSemanticsNodes().size == 2 }
        // The pencil now opens the sentence for changing.
        onNodeWithTag("sentence-edit").performClick()
        onNodeWithText("Cancel").performClick()
        assertEquals(0, onAllNodesWithTag("sentence-field").fetchSemanticsNodes().size)
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
                (1..30).forEach { n -> terms.save(Term(languageId = id, text = "palavra$n", textLc = "palavra$n", status = TermStatus.NEW_1, translation = "слово")) }
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
    fun aPhoneOpensTheCardInASheet() = runDesktopComposeUiTest(width = 420, height = 900) {
        show(width = 420)
        onNodeWithTag("phone-sort").assertExists()
        save("BROWSE_PHONE_LIST_SCREENSHOT")
        // Select puts checkboxes on the rows; a tap then picks the card instead of opening it.
        onNodeWithTag("phone-select").performClick()
        onNodeWithTag("browse-row-$comprar").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("selection-bar").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Done").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("selection-bar").fetchSemanticsNodes().isEmpty() }
        onNodeWithTag("browse-row-$casa").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("review-history").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("browse-detail").assertExists()
        save("BROWSE_PHONE_SCREENSHOT")
    }
}
