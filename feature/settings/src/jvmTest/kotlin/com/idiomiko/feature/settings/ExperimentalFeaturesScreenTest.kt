package com.idiomiko.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ExperimentalFeaturesScreenTest {

    @Test
    fun videosStartOffAndTurningThemOnIsKept() = runDesktopComposeUiTest(width = 1200, height = 800) {
        // A device that saved its settings before videos were opt-in still has them off.
        val store = MapSettings().apply { putBoolean("experimental_videos", true) }
        val settings = SettingsRepositoryImpl(store)
        assertFalse(settings.current.experimentalVideos)
        setContent { IdiomikoTheme { ExperimentalFeaturesScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings), videosAvailable = true) } }

        onNodeWithTag("experimental-videos").assertIsOff().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.experimentalVideos }
        onNodeWithTag("experimental-videos").assertIsOn()
        assertTrue(SettingsRepositoryImpl(store).current.experimentalVideos)
        System.getenv("EXPERIMENTS_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
        }
    }

    @Test
    fun withoutAPlayerTheToggleIsOffAndSaysWhy() = runDesktopComposeUiTest(width = 1200, height = 800) {
        val settings = SettingsRepositoryImpl(MapSettings())
        setContent { ExperimentalFeaturesScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings), videosAvailable = false) }

        onNodeWithTag("experimental-videos").assertIsOff().assertIsNotEnabled()
        onNodeWithText("Videos aren't available on the web yet.").assertExists()
        onNodeWithTag("experimental-videos").performClick()
        waitForIdle()
        assertEquals(false, settings.current.experimentalVideos, "the switch is disabled, so the choice stays as it was")
    }
}
