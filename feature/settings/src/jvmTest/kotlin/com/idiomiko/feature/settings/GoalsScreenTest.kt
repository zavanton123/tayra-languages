package com.idiomiko.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.russhwolf.settings.MapSettings
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** The Goals page sets the daily goal Home measures the day against, and the book stats sample size. */
@OptIn(ExperimentalTestApi::class)
class GoalsScreenTest {

    @Test
    fun theStepperSavesTheDailyGoalFiveMinutesAtATime() = runDesktopComposeUiTest(width = 1580, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        setContent { Hosted(settings) { GoalsScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings)) } }

        onNodeWithTag("settings-section-GOALS").assertIsSelected()
        onNodeWithTag("settings-section-GENERAL").assertIsNotSelected()
        onNodeWithText("Set goals").assertExists()
        onNodeWithTag("daily-goal").assertExists()
        System.getenv("GOALS_SCREENSHOT")?.let { save(it) }

        val start = settings.current.dailyGoalMinutes
        onNodeWithContentDescription("Increase daily goal").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.dailyGoalMinutes == start + 5 }
        onNodeWithContentDescription("Decrease daily goal").performClick()
        onNodeWithContentDescription("Decrease daily goal").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.dailyGoalMinutes == start - 5 }

        onNodeWithTag("stats-sample-size").assertExists()
        onNodeWithContentDescription("Decrease book stats page sample size").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.statsSampleSize == 4 }
    }

    /** On a phone the page has no sidebar, and the bar's arrow goes back. */
    @Test
    fun aPhoneGoesBackWithTheBarsArrow() = runDesktopComposeUiTest(width = 400, height = 900) {
        val settings = SettingsRepositoryImpl(MapSettings())
        var back = 0
        setContent { Hosted(settings) { GoalsScreen(onNavigate = {}, onBack = { back++ }, viewModel = SettingsViewModel(settings)) } }

        onNodeWithTag("settings-sidebar").assertDoesNotExist()
        onNodeWithTag("daily-goal").assertExists()
        System.getenv("GOALS_SCREENSHOT")?.let { save(it.replace(".png", "-phone.png")) }
        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, back)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
    }
}
