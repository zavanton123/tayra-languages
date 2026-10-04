package com.tayra.languages.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** The daily limits, the wanted retention and the learning steps of the flashcards. */
@OptIn(ExperimentalTestApi::class)
class FlashcardSettingsScreenTest {

    @Test
    fun limitsAndStepsAreSavedAsTheyChange() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        setContent { Hosted(settings) { FlashcardSettingsScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings)) } }
        waitForIdle()
        System.getenv("FLASHCARD_SETTINGS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }

        onNodeWithContentDescription("Increase new cards a day").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardNewPerDay == 25 }

        // Reviews are unlimited until a limit is switched on, which starts at Anki's 200.
        assertEquals(0, settings.current.flashcardReviewsPerDay)
        assertEquals(0, onAllNodesWithText("Reviews a day").fetchSemanticsNodes().size)
        onAllNodes(isToggleable())[0].performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardReviewsPerDay == 200 }
        onNodeWithContentDescription("Decrease reviews a day").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardReviewsPerDay == 190 }

        // Steps are saved once they read as steps; anything else is flagged and left unsaved.
        onNodeWithContentDescription("Learning steps").performTextClearance()
        onNodeWithContentDescription("Learning steps").performTextInput("30s 5m 1h")
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardLearnSteps == "30s 5m 1h" }
        onNodeWithContentDescription("Relearning steps").performTextClearance()
        onNodeWithContentDescription("Relearning steps").performTextInput("soon")
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Use steps such as 1m 10m").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("", settings.current.flashcardRelearnSteps, "clearing the field saved no steps; the unreadable text was not saved")

        onNodeWithText("Reset to defaults").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardNewPerDay == 20 && settings.current.flashcardLearnSteps == "1m 10m" && settings.current.flashcardRelearnSteps == "10m" }
        assertEquals(0, settings.current.flashcardReviewsPerDay)
    }
}
