package com.idiomiko.feature.frequency

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.data.repository.VocabularyLevelRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.frequency.VocabularyLevelService
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.IdiomikoTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The vocabulary settings show the level set on the level cards, set another from them or from an exact number of words, and save its words as known. */
@OptIn(ExperimentalTestApi::class)
class VocabularySettingsScreenTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-vocabulary-settings", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val portuguese = runBlocking { languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) } } }
    private val service = VocabularyLevelService(ResourceFrequencyLists(), VocabularyLevelRepositoryImpl(provider), languages)
    private val viewModel = VocabularySettingsViewModel(service, languages, settings)

    private fun ComposeUiTest.show() {
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) { VocabularySettingsScreen(onNavigate = {}, viewModel = viewModel) }
                }
            }
        }
        // The screen, not the view model: its state reaches the screen a moment after it changes.
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { onAllNodesWithTag("level-card-Starter").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.awaitText(text: String) = waitUntil(timeoutMillis = UI_WAIT_MILLIS) { onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private val levelCard = SemanticsMatcher("is a level card") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("level-card-") == true }

    private fun ComposeUiTest.assertNoCardSelected() =
        assertTrue(onAllNodes(levelCard and isSelected()).fetchSemanticsNodes().isEmpty(), "no card is selected")

    @Test
    fun theSavedLevelsCardIsSelected() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        runBlocking { service.setLevel(portuguese, 2000) }
        show()
        awaitText("Your level: B1")
        onNodeWithTag("level-card-B1").assertIsSelected()
        onNodeWithTag("level-card-A2").assertIsNotSelected()
        onNodeWithText("Current").assertExists()
        onNodeWithText("The 2,000 most common words are marked as known.", substring = true).assertExists()
        onNodeWithText("Set level").assertExists()
        onNodeWithTag("set-vocabulary-level").assertIsNotEnabled()
        // The exact number of words starts folded away.
        assertTrue(onAllNodesWithTag("level-slider").fetchSemanticsNodes().isEmpty())
        onNodeWithText("Word frequency").assertExists()
        screenshot("desktop-saved-b1")
    }

    @Test
    fun aLevelBetweenTwoCardsIsDescribedInTheStrip() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        runBlocking { service.setLevel(portuguese, 1500) }
        show()
        awaitText("Your level: 1,500 words, between A2 and B1")
        assertNoCardSelected()
        onNodeWithText("The 1,500 most common words are marked as known.", substring = true).assertExists()
        onNodeWithTag("set-vocabulary-level").assertIsNotEnabled()
        screenshot("desktop-between")
        onNodeWithTag("level-card-B1").performClick()
        awaitText("Set to B1")
        onNodeWithText("B1 level selected", substring = true).assertExists()
        onNodeWithText("We'll mark the 2,000 most common words as known.", substring = true).assertExists()
    }

    @Test
    fun pickingB1AndSettingItSavesItsWordsAsKnown() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        show()
        awaitText("No level set yet")
        assertNoCardSelected()
        onNodeWithTag("set-vocabulary-level").assertIsNotEnabled()
        assertTrue(viewModel.state.value.example.count { it.isWord } > 50, "the example is a sample story")
        onNodeWithTag("level-card-B1").performClick()
        awaitText("Set to B1")
        onNodeWithTag("level-card-B1").assertIsSelected()
        onNodeWithText("We'll mark the 2,000 most common words as known.", substring = true).assertExists()
        onNodeWithTag("set-vocabulary-level").assertIsEnabled()
        screenshot("desktop-collapsed")
        onNodeWithTag("set-vocabulary-level").performClick()
        onNodeWithText("Set your vocabulary level to 2,000?", substring = true).assertExists()
        onNodeWithText("Set level").performClick()
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { viewModel.state.value.level == 2000 && !viewModel.state.value.saving }
        assertEquals(TermStatus.WELL_KNOWN, runBlocking { terms.findByTextLc(portuguese, "dizer") }?.status)
        awaitText("Your level: B1")
        onNodeWithTag("level-card-B1").assertIsSelected()
        onNodeWithText("Current").assertExists()
        onNodeWithTag("set-vocabulary-level").assertIsNotEnabled()
    }

    @Test
    fun theExactNumberSectionExpandsAndItsSliderSavesAnExactNumber() = runDesktopComposeUiTest(width = 1586, height = 1400) {
        show()
        onNodeWithTag("exact-level-toggle").performClick()
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { onAllNodesWithTag("level-slider").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Reading preview").assertExists()
        // The slider's stops run 0, 100, 200, 300, 400, 500, …: the sixth is 500, between A1's 300 and A2's 1,000.
        onNodeWithTag("level-slider").performSemanticsAction(SemanticsActions.SetProgress) { it(5f) }
        awaitText("500 words selected, between A1 and A2")
        assertNoCardSelected()
        onNodeWithText("Words around this level").assertExists()
        onNodeWithText("Set to 500").assertExists()
        screenshot("desktop-expanded")
        onNodeWithTag("set-vocabulary-level").performScrollTo().performClick()
        onNodeWithText("Set your vocabulary level to 500?", substring = true).assertExists()
        onNodeWithText("Set level").performClick()
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { viewModel.state.value.level == 500 && !viewModel.state.value.saving }
        assertEquals(TermStatus.WELL_KNOWN, runBlocking { terms.findByTextLc(portuguese, "disse") }?.status)
        awaitText("Your level: 500 words, between A1 and A2")
    }

    /** A phone has the levels one to a row, the line under them, one button to set the level and the way to the word frequency page. */
    @Test
    fun aPhoneSetsTheLevelFromItsOwnPage() = runDesktopComposeUiTest(width = 400, height = 1400) {
        show()
        onNodeWithText("Explore word frequency").assertExists()
        val starter = onNodeWithTag("level-card-Starter").fetchSemanticsNode().boundsInRoot
        assertTrue(onNodeWithTag("level-card-A1").fetchSemanticsNode().boundsInRoot.top >= starter.bottom, "one level to a row")
        onNodeWithTag("set-vocabulary-level").assertIsNotEnabled()
        onNodeWithTag("level-card-B1").performScrollTo().performClick()
        awaitText("Set to B1")
        screenshot("phone")
        onNodeWithTag("exact-level-toggle").performScrollTo().performClick()
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { onAllNodesWithTag("level-slider").fetchSemanticsNodes().isNotEmpty() }
        waitForIdle()
        screenshot("phone-expanded")
        onNodeWithTag("set-vocabulary-level").performScrollTo().performClick()
        onNodeWithText("Set your vocabulary level to 2,000?", substring = true).assertExists()
        onNodeWithText("Set level").performClick()
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { viewModel.state.value.level == 2000 && !viewModel.state.value.saving }
        waitUntil(timeoutMillis = UI_WAIT_MILLIS) { onAllNodes(hasTestTag("set-vocabulary-level") and isNotEnabled()).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Writes what is on screen to [name].png in the folder VOCABULARY_SETTINGS_SCREENSHOTS names, when it is set. */
    private fun ComposeUiTest.screenshot(name: String) {
        System.getenv("VOCABULARY_SETTINGS_SCREENSHOTS")?.let { folder ->
            waitForIdle()
            ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(folder, "$name.png"))
        }
    }
}
