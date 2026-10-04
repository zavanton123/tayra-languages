package com.tayra.languages.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.AppThemes
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** The Settings page changes the reading settings with its controls and puts them back with Reset to defaults. */
@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    @Test
    fun theControlsChangeTheSettingsAndResetPutsThemBack() = runDesktopComposeUiTest(width = 1580, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        runBlocking { settings.update { it.copy(readingFontScale = 1.2f, showStreakOnHome = true) } }
        setContent { Hosted(settings) { SettingsScreen(onNavigate = {}, viewModel = SettingsViewModel(settings)) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(androidx.compose.ui.test.hasText("Reading preview")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("SETTINGS_SCREENSHOT")?.let { save(it) }

        onNodeWithContentDescription("Increase reading font size").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.readingFontScale == 1.3f }
        onNodeWithText("130%").assertExists()
        onNodeWithContentDescription("Decrease book stats page sample size").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.statsSampleSize == 4 }

        // The reading font is chosen from a list whose entries are drawn in their own fonts.
        onNodeWithText("System serif").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Literata").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText("Literata").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.readingFont == "literata" }
        System.getenv("SETTINGS_SCREENSHOT")?.let { save(it.replace(".png", "-literata.png")) }

        onNodeWithText("Reset to defaults").performClick()
        onNodeWithText("Reset").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.readingFontScale == 1.0f }
        assertEquals(5, settings.current.statsSampleSize)
        assertEquals(false, settings.current.showStreakOnHome)
        assertEquals("serif", settings.current.readingFont)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
    }
}

/** The screen as the app hosts it: themed from the settings, with the window width and a learning language. */
@Composable
internal fun Hosted(settings: SettingsRepositoryImpl, content: @Composable () -> Unit) {
    val current by settings.settings.collectAsState()
    TayraTheme(AppThemes.byId(current.themeId)) {
        ProvideWindowWidth {
            CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L) {}, content = content)
        }
    }
}
