package com.tayra.languages.feature.flashcards

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
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
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The browser lists every card, narrows them by search and group, opens one for its history, and suspends or resets a selection. */
@OptIn(ExperimentalTestApi::class)
class BrowseFlashcardsScreenTest {

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-browse", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val cards = FlashcardRepositoryImpl(provider)
    private val service = FlashcardService(cards, terms, settings)
    private var comprar = 0L
    private var casa = 0L
    private var livro = 0L

    /** Three cards: one new, one answered Easy and so in review days away, one suspended. */
    private fun ComposeUiTest.show(width: Int = 1586): BrowseFlashcardsViewModel {
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
        val viewModel = BrowseFlashcardsViewModel(service, languages, settings, terms)
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(
                        LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {},
                        LocalFlashcardsDue provides 0,
                    ) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            BrowseFlashcardsScreen(onNavigate = {}, onBack = {}, viewModel = viewModel)
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

    private fun ComposeUiTest.rows(): Int = listOf(comprar, casa, livro).count { onAllNodesWithTag("browse-row-$it").fetchSemanticsNodes().isNotEmpty() }

    private fun card(id: Long) = runBlocking { cards.get(id)!! }

    @Test
    fun theListNarrowsBySearchAndGroup() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        assertEquals(3, rows())
        onNodeWithText("3 cards").assertExists()
        // The review card shows where it stands (the group chip says Review too); the new and suspended cards have no time to come yet.
        assertEquals(2, onAllNodesWithText("Review").fetchSemanticsNodes().size)
        assertEquals(2, onAllNodesWithText("Not shown yet").fetchSemanticsNodes().size)
        save("BROWSE_SCREENSHOT")

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
        onNodeWithTag("browse-row-$casa").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("review-history").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("browse-detail").assertExists()
        save("BROWSE_PHONE_SCREENSHOT")
    }
}
