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
import androidx.compose.ui.test.performScrollTo
import com.tayra.languages.core.domain.service.CommandLineStatus
import com.tayra.languages.core.domain.service.CommandLineTool
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

    /** The theme list shows each theme with its colours; a theme that was removed opens as the one standing in for it. */
    @Test
    fun aThemeIsPickedFromTheList() = runDesktopComposeUiTest(width = 1580, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        runBlocking { settings.update { it.copy(themeId = "dark_slate") } }
        setContent { Hosted(settings) { SettingsScreen(onNavigate = {}, viewModel = SettingsViewModel(settings)) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Darcula").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Darcula").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Gruvbox Dark").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("THEMES_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onAllNodes(androidx.compose.ui.test.isRoot()).onLast().captureToImage().toAwtImage(), "png", File(path))
        }
        onNodeWithText("Gruvbox Dark").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.themeId == "gruvbox_dark" }
        System.getenv("THEMES_SCREENSHOT")?.let { save(it.replace(".png", "-gruvbox.png")) }
    }

    /** On the desktop the command-line tool is put on the PATH from its card, which then says where. */
    @Test
    fun theCommandLineToolIsInstalledFromItsCard() = runDesktopComposeUiTest(width = 1580, height = 1300) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val tool = object : CommandLineTool {
            var installed = false
            override val bundled = true
            override suspend fun status() = CommandLineStatus(installed, "/Users/me/.local/bin/tayra")
            override suspend fun install(): CommandLineStatus {
                installed = true
                return status().copy(pathChangedIn = "/Users/me/.zshrc")
            }
            override suspend fun uninstall(): CommandLineStatus {
                installed = false
                return status()
            }
        }
        setContent { Hosted(settings) { SettingsScreen(onNavigate = {}, viewModel = SettingsViewModel(settings, tool)) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Not installed").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("CLI_SCREENSHOT")?.let { save(it) }
        onNodeWithText("Install command-line tool").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Installed").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Added ~/.local/bin to your PATH in /Users/me/.zshrc. Open a new terminal window to use it.").assertExists()
        System.getenv("CLI_SCREENSHOT")?.let { save(it.replace(".png", "-installed.png")) }
        onNodeWithText("Remove").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Not installed").fetchSemanticsNodes().isNotEmpty() }
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
