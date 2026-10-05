package com.tayra.languages.feature.flashcards

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalFlashcardsDue
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** A card is shown front first, the answer on request, and answering it moves on and moves the word's status. */
@OptIn(ExperimentalTestApi::class)
class FlashcardsScreenTest {

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-flash", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = FlashcardService(FlashcardRepositoryImpl(provider), terms, settings)
    private val sentence = "Quando precisamos comprar alguma coisa, passamos no supermercado."
    private var termId = 0L

    private fun ComposeUiTest.show(): FlashcardsViewModel {
        val languageId = runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = id, nativeLanguage = "ru") }
            termId = terms.save(Term(languageId = id, text = "comprar", textLc = "comprar", status = TermStatus.NEW_1, translation = "купить, покупать", sentence = sentence))
            id
        }
        val noDictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val translator = object : SentenceTranslator {
            override suspend fun translate(text: String, language: Language): String? = "Когда нам нужно что-то купить, мы заходим в супермаркет."
        }
        val suggestions = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        val viewModel = FlashcardsViewModel(service, languages, settings, translator, noDictionary, WordTranslationService(terms, noDictionary, suggestions, settings))
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(
                        LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {},
                        LocalFlashcardsDue provides 1,
                    ) {
                        val state by viewModel.state.collectAsState()
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column {
                                AppTopBar(title = "Flashcards", onNavigate = {}, section = NavSection.FLASHCARDS)
                                FlashcardsContent(
                                    state,
                                    speaker = null,
                                    actions = FlashcardActions(onReveal = viewModel::reveal, onAnswer = viewModel::answer, onUndo = viewModel::undo, onSuspend = viewModel::suspendCard),
                                )
                            }
                        }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard").fetchSemanticsNodes().isNotEmpty() }
        return viewModel
    }

    private fun ComposeUiTest.save(env: String) {
        System.getenv(env)?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }

    private fun status() = runBlocking { terms.getById(termId)!!.status }

    @Test
    fun aCardHidesItsWordUntilTheAnswerIsShown() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Когда нам нужно что-то купить, мы заходим в супермаркет.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Quando precisamos [...] alguma coisa, passamos no supermercado.").assertExists()
        assertEquals(0, onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithText("купить, покупать").fetchSemanticsNodes().size)
        onNodeWithText("Card 1 of 1").assertExists()
        save("FLASHCARD_FRONT_SCREENSHOT")

        onNodeWithText("Show answer").performClick()
        onNodeWithText(sentence).assertExists()
        onNodeWithText("купить, покупать").assertExists()
        // What each answer does to a new card: the learning steps, or days away for Easy.
        onNodeWithText("<1m").assertExists()
        onNodeWithText("<6m").assertExists()
        onNodeWithText("<10m").assertExists()
        // About eight days for Easy, moved a day either way by the interval fuzz.
        assertEquals(1, listOf("7d", "8d", "9d").count { onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() })
        save("FLASHCARD_BACK_SCREENSHOT")
    }

    @Test
    fun answeringMovesOnAndRaisesTheStatus() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("answer-Good").performClick()
        // Ten minutes to wait and nothing else to show: the card comes straight back, as in Anki.
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("LEARNING").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(TermStatus.NEW_1, status())
        // One answer given, one card still waiting.
        onNodeWithText("Card 2 of 2").assertExists()

        // Space shows the answer, and Space again answers Good.
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("All done for today").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(TermStatus.NEW_2, status(), "the card now waits a day or more")
        onNodeWithText("2 of 2 answered").assertExists()
        save("FLASHCARD_DONE_SCREENSHOT")

        onNodeWithText("Undo last answer").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(TermStatus.NEW_1, status())
    }

    @Test
    fun easyKeySendsANewCardDaysAway() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Four) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("All done for today").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(TermStatus.LEARNING_3, status())
    }
}
