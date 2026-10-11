package com.tayra.languages.feature.frequency

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.VocabularyLevelRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.frequency.VocabularyLevelService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A language just chosen with no vocabulary level asks for one; putting it off keeps asking, choosing any level stops. */
@OptIn(ExperimentalTestApi::class)
class VocabularyLevelPromptTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-level-prompt", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val portuguese = runBlocking { languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) } } }
    private val service = VocabularyLevelService(ResourceFrequencyLists(), VocabularyLevelRepositoryImpl(provider), languages)
    private val viewModel = VocabularySettingsViewModel(service, languages, settings)
    private var closed = 0

    private fun ComposeUiTest.show() = setContent {
        var asking by mutableStateOf<Long?>(portuguese)
        TayraTheme {
            ProvideWindowWidth {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    VocabularyLevelPrompt(asking, onClosed = { closed++; asking = null }, viewModel = viewModel)
                }
            }
        }
    }

    @Test
    fun puttingItOffClosesWithoutALevel() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("level-slider").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("How much Portuguese do you know?").assertExists()
        assertEquals(2, onAllNodesWithText("Starting out").fetchSemanticsNodes().size, "the level's name and the slider's start")
        onNodeWithText("Start from scratch").assertExists()
        onNodeWithText("Skip for now").performClick()
        waitForIdle()
        assertEquals(1, closed)
        assertTrue(onAllNodesWithTag("vocabulary-level-prompt").fetchSemanticsNodes().isEmpty())
        assertTrue(runBlocking { service.needsLevel(portuguese) }, "asked again next time")
    }

    @Test
    fun movingTheSliderPicksALevelAndStartingSavesIt() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("level-slider").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("level-slider").performSemanticsAction(SemanticsActions.SetProgress) { it(3f) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Start with 300 words").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Early beginner").assertExists()
        onNodeWithText("Words around this level").assertExists()
        System.getenv("LEVEL_PROMPT_SCREENSHOT")?.let { save(it) }
        onNodeWithText("Start with 300 words").performClick()
        waitUntil(timeoutMillis = 10_000) { closed == 1 }
        assertEquals(false, runBlocking { service.needsLevel(portuguese) })
        assertEquals(300, viewModel.state.value.level)
    }

    @Test
    fun startingFromScratchStopsTheQuestion() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("level-slider").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("prompt-set-level").assertIsEnabled()
        onNodeWithText("Start from scratch").performClick()
        waitUntil(timeoutMillis = 10_000) { closed == 1 }
        assertEquals(false, runBlocking { service.needsLevel(portuguese) })
    }

    @Test
    fun aPhoneGetsABottomSheetWithTheSampleText() = runDesktopComposeUiTest(width = 400, height = 860) {
        show()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("level-slider").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("How much Portuguese do you know?").assertExists()
        onNodeWithText("Example text").assertExists()
        assertTrue(onAllNodesWithText("Your first words").fetchSemanticsNodes().isEmpty(), "the sheet keeps to the sample text")
        assertTrue(onAllNodesWithText("Reading preview").fetchSemanticsNodes().isEmpty())
        onNodeWithTag("level-slider").performSemanticsAction(SemanticsActions.SetProgress) { it(7f) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Start with 1,000 words").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Elementary").assertExists()
        System.getenv("LEVEL_SHEET_SCREENSHOT")?.let { save(it) }
        onNodeWithText("Start with 1,000 words").performClick()
        waitUntil(timeoutMillis = 10_000) { closed == 1 }
        assertEquals(1000, viewModel.state.value.level)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}
