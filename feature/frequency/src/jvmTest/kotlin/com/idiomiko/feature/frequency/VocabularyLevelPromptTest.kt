package com.idiomiko.feature.frequency

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.CourseRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.VocabularyLevelRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.courses.Lesson
import com.idiomiko.core.domain.frequency.FrequencyLists
import com.idiomiko.core.domain.frequency.VocabularyLevelService
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.IdiomikoTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A language just chosen with no vocabulary level asks for one by level cards; putting it off keeps asking, choosing any level stops. */
@OptIn(ExperimentalTestApi::class)
class VocabularyLevelPromptTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-level-prompt", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val courses = CourseRepositoryImpl(provider)
    private val portuguese = runBlocking { languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) } } }
    private var closed = 0

    /** The service over the app's lists, or over their first [words] words only. */
    private fun service(words: Int? = null): VocabularyLevelService {
        val lists = ResourceFrequencyLists()
        val shortened = if (words == null) lists else object : FrequencyLists {
            override suspend fun list(languageCode: String) = lists.list(languageCode)?.let { it.copy(words = it.words.take(words)) }
        }
        return VocabularyLevelService(shortened, VocabularyLevelRepositoryImpl(provider), languages)
    }

    private val service = service()
    private var viewModel = VocabularySettingsViewModel(service, languages, settings, courses = courses)

    private fun ComposeUiTest.show() = setContent {
        var asking by mutableStateOf<Long?>(portuguese)
        IdiomikoTheme {
            ProvideWindowWidth {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    VocabularyLevelPrompt(asking, onClosed = { closed++; asking = null }, viewModel = viewModel)
                }
            }
        }
    }

    private fun ComposeUiTest.awaitCards() {
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { onAllNodesWithTag("level-card-Starter").fetchSemanticsNodes().isNotEmpty() }
        // The prompt picks Starter itself, through the view model, a moment after the cards show.
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { onAllNodes(hasTestTag("prompt-set-level") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.awaitText(text: String) = waitUntil(timeoutMillis = UI_WAIT_MILLIS) { onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private val levelCards = listOf("Starter", "A1", "A2", "B1", "B2", "C1", "C2")

    /** Starter is picked from the start, and Continue is the only way on: the prompt cannot be put off, to come back at the next switch. */
    @Test
    fun theCardsStartOnStarterWithNoWayToPutItOff() = runDesktopComposeUiTest(width = 1440, height = 900) {
        show()
        awaitCards()
        onNodeWithText("How much Portuguese do you know?").assertExists()
        onNodeWithText("Choose your level").assertExists()
        for (card in levelCards) onNodeWithTag("level-card-$card").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        onNodeWithTag("level-card-Starter").assertIsSelected()
        onNodeWithTag("level-card-B1").assertIsNotSelected()
        onNodeWithText("Starter level selected", substring = true).assertExists()
        onNodeWithText("mark no words as already known", substring = true).assertExists()
        screenshot("desktop-starter")
        onNodeWithText("Skip for now").assertDoesNotExist()
        onNodeWithContentDescription("Close").assertDoesNotExist()
        onNodeWithText("Continue with Starter").performClick()
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { closed == 1 }
        assertEquals(false, runBlocking { service.needsLevel(portuguese) })
    }

    @Test
    fun choosingACardNamesItsWordsAndContinueSavesThem() = runDesktopComposeUiTest(width = 1440, height = 900) {
        show()
        awaitCards()
        onNodeWithTag("level-card-B1").performClick()
        awaitText("Continue with B1")
        onNodeWithTag("level-card-B1").assertIsSelected()
        onNodeWithTag("level-card-Starter").assertIsNotSelected()
        onNodeWithText("B1 level selected", substring = true).assertExists()
        onNodeWithText("We'll mark the 2,000 most common words as known.", substring = true).assertExists()
        screenshot("desktop-b1")
        onNodeWithText("Continue with B1").performClick()
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { closed == 1 }
        assertEquals(false, runBlocking { service.needsLevel(portuguese) })
        assertEquals(2000, viewModel.state.value.level, "B1 reached: the words up to its end are known")
    }

    @Test
    fun starterSavesNoWords() = runDesktopComposeUiTest(width = 1440, height = 900) {
        show()
        awaitCards()
        onNodeWithText("Continue with Starter").performClick()
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { closed == 1 }
        assertEquals(false, runBlocking { service.needsLevel(portuguese) }, "Starter counts as a choice")
        assertEquals(0, viewModel.state.value.level)
    }

    @Test
    fun theLanguagesCoursesSetWhereALevelEnds() = runDesktopComposeUiTest(width = 1440, height = 900) {
        runBlocking { courses.seedSamples(portuguese, listOf(course("b1-a", CourseLevel.B1, 1800), course("b1-b", CourseLevel.B1, 2500))) }
        show()
        awaitCards()
        onNodeWithTag("level-card-B1").performClick()
        awaitText("We'll mark the 2,500 most common words as known.")
        onNodeWithText("Continue with B1").performClick()
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { closed == 1 }
        assertEquals(2500, viewModel.state.value.level)
    }

    @Test
    fun aShortListOffersOnlyTheLevelsItReaches() = runDesktopComposeUiTest(width = 1440, height = 900) {
        viewModel = VocabularySettingsViewModel(service(words = 300), languages, settings, courses = courses)
        show()
        awaitCards()
        onNodeWithTag("level-card-A1").assertExists()
        for (card in levelCards.drop(2)) assertTrue(onAllNodesWithTag("level-card-$card").fetchSemanticsNodes().isEmpty(), "$card is past a 300-word list")
        onNodeWithTag("level-card-A1").performClick()
        awaitText("Continue with A1")
        onNodeWithText("We'll mark the 300 most common words as known.", substring = true).assertExists()
    }

    @Test
    fun aMediumWindowPutsTheCardsOnTwoRows() = runDesktopComposeUiTest(width = 760, height = 900) {
        show()
        awaitCards()
        val top = onNodeWithTag("level-card-Starter").fetchSemanticsNode().boundsInRoot.top
        assertEquals(top, onNodeWithTag("level-card-B1").fetchSemanticsNode().boundsInRoot.top, "Starter to B1 on the first row")
        assertTrue(onNodeWithTag("level-card-B2").fetchSemanticsNode().boundsInRoot.top > top, "B2 to C2 on the second")
        screenshot("desktop-medium")
    }

    /** A phone asks on the whole screen, the levels one under another and the buttons at its foot. */
    @Test
    fun aPhoneGetsTheWholeScreenWithTheCards() = runDesktopComposeUiTest(width = 360, height = 800) {
        show()
        awaitCards()
        onNodeWithText("How much Portuguese do you know?").assertExists()
        val screen = onNodeWithTag("vocabulary-level-prompt").getBoundsInRoot()
        assertEquals(360.dp, screen.right - screen.left)
        assertEquals(800.dp, screen.bottom - screen.top)
        assertTrue(800.dp - onNodeWithText("Continue with Starter").getBoundsInRoot().bottom < 40.dp, "the buttons sit at the screen's foot")
        assertEquals(0, onAllNodesWithText("You can change this later", substring = true).fetchSemanticsNodes().size, "no line under the question")
        for (card in levelCards) onNodeWithTag("level-card-$card").assertExists()
        onNodeWithTag("level-card-A2").performClick()
        awaitText("Continue with A2")
        onNodeWithText("We'll mark the 1,000 most common words as known.", substring = true).assertExists()
        screenshot("phone-screen")
        onNodeWithTag("chosen-level").performScrollTo()
        screenshot("phone-screen-end")
        onNodeWithText("Continue with A2").performClick()
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { closed == 1 }
        assertEquals(1000, viewModel.state.value.level)
    }

    private fun course(id: String, level: CourseLevel, rankUpTo: Int) = Course(
        id = id, languageCode = "pt", title = id, description = "", level = level, topic = "", rankUpTo = rankUpTo,
        lessons = listOf(Lesson("$id-1", "Lição", "", "Uma casa.")),
    )

    /** Writes what is on screen to [name].png in the folder LEVEL_PROMPT_SCREENSHOTS names, when it is set. */
    private fun ComposeUiTest.screenshot(name: String) {
        System.getenv("LEVEL_PROMPT_SCREENSHOTS")?.let { folder ->
            waitForIdle()
            ImageIO.write(onAllNodes(isRoot()).fetchSemanticsNodes().indices.last().let { onAllNodes(isRoot())[it] }.captureToImage().toAwtImage(), "png", File(folder, "$name.png"))
        }
    }
}
