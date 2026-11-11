package com.tayra.languages.feature.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ExperimentalFeaturesScreenTest {

    @Test
    fun videosStartOnAndTurningThemOffIsKept() = runDesktopComposeUiTest(width = 1200, height = 800) {
        val store = MapSettings()
        val settings = SettingsRepositoryImpl(store)
        assertTrue(settings.current.experimentalVideos)
        setContent { ExperimentalFeaturesScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings), videosAvailable = true) }

        onNodeWithTag("experimental-videos").assertIsOn().performClick()
        waitUntil(timeoutMillis = 5_000) { !settings.current.experimentalVideos }
        onNodeWithTag("experimental-videos").assertIsOff()
        assertFalse(SettingsRepositoryImpl(store).current.experimentalVideos)
    }

    @Test
    fun withoutAPlayerTheToggleIsOffAndSaysWhy() = runDesktopComposeUiTest(width = 1200, height = 800) {
        val settings = SettingsRepositoryImpl(MapSettings())
        setContent { ExperimentalFeaturesScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings), videosAvailable = false) }

        onNodeWithTag("experimental-videos").assertIsOff().assertIsNotEnabled()
        onNodeWithText("Videos aren't available on the web yet.").assertExists()
        onNodeWithTag("experimental-videos").performClick()
        waitForIdle()
        assertEquals(true, settings.current.experimentalVideos, "the switch is disabled, so the choice stays as it was")
    }
}
