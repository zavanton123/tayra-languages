package com.idiomiko.feature.settings

import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.domain.service.SpeechEngine
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
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.IdiomikoTheme
import androidx.compose.ui.test.performScrollTo
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Settings page changes the reading settings with its controls and puts them back with Reset to defaults. */
@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    @Test
    fun theControlsChangeTheSettingsAndResetPutsThemBack() = runDesktopComposeUiTest(width = 1580, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        runBlocking { settings.update { it.copy(readingFontScale = 1.2f) } }
        setContent { Hosted(settings) { SettingsScreen(onNavigate = {}, viewModel = SettingsViewModel(settings)) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(androidx.compose.ui.test.hasText("Reading preview")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("settings-section-GENERAL").assertIsSelected()
        System.getenv("SETTINGS_SCREENSHOT")?.let { save(it) }
        // The word statuses have their card; the daily goal has a page of its own.
        onNodeWithText("Display word statuses").assertExists()
        onNodeWithText("Show learning levels").assertExists()
        onNodeWithText("Behaviour").assertDoesNotExist()
        onNodeWithText("Daily goal").assertDoesNotExist()
        onNodeWithTag("daily-goal").assertDoesNotExist()
        // Motion follows the system's setting alone.
        onNodeWithText("Reduce motion").assertDoesNotExist()

        onNodeWithContentDescription("reading font size").performSemanticsAction(SemanticsActions.SetProgress) { it(1.3f) }
        waitUntil(timeoutMillis = 5_000) { settings.current.readingFontScale == 1.3f }
        onNodeWithText("130%").assertExists()
        onNodeWithContentDescription("Decrease book stats page sample size").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.statsSampleSize == 4 }

        // The reading font is chosen from a list whose entries are drawn in their own fonts.
        onNodeWithText("System serif").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Literata").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText("Literata").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.readingFont == "literata" }
        System.getenv("SETTINGS_SCREENSHOT")?.let { save(it.replace(".png", "-literata.png")) }

        onNodeWithText("Reset to defaults").performScrollTo().performClick()
        onNodeWithText("Reset").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.readingFontScale == 1.0f }
        assertEquals(5, settings.current.statsSampleSize)
        assertEquals("serif", settings.current.readingFont)
    }

    /** A window just wide enough for the sidebar keeps it, with the page in one column beside it; another section opens from it. */
    @Test
    fun aMediumWideWindowHasTheSidebarBesideOneColumn() = runDesktopComposeUiTest(width = 1000, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val visited = mutableListOf<Route>()
        setContent { Hosted(settings) { SettingsScreen(onNavigate = { visited += it }, viewModel = SettingsViewModel(settings)) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(androidx.compose.ui.test.hasText("Reading preview")).fetchSemanticsNodes().isNotEmpty() }
        val appearance = onNodeWithText("Appearance").fetchSemanticsNode().boundsInRoot
        val preview = onNodeWithText("Reading preview").fetchSemanticsNode().boundsInRoot
        assertTrue(preview.top > appearance.top && preview.left == appearance.left, "the cards stack in one column")
        System.getenv("SETTINGS_SCREENSHOT")?.let { save(it.replace(".png", "-medium.png")) }
        onNodeWithTag("settings-section-SPEECH").performClick()
        assertEquals(listOf<Route>(Route.Speech), visited)
    }

    /** A phone gets compact cards: the theme and font from bottom sheets, the sliders under their labels, Reset in the top bar. */
    @Test
    fun aPhoneGetsCompactCardsWithSheetPickers() = runDesktopComposeUiTest(width = 400, height = 1200) {
        val settings = SettingsRepositoryImpl(MapSettings())
        runBlocking { settings.update { it.copy(readingFontScale = 1.2f) } }
        setContent { Hosted(settings) { SettingsScreen(onNavigate = {}, viewModel = SettingsViewModel(settings)) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Appearance").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Reset to defaults").assertDoesNotExist()
        onNodeWithText("120%").assertExists()
        onNodeWithTag("word-statuses").assertExists()
        onNodeWithText("Behaviour").assertDoesNotExist()
        onNodeWithTag("daily-goal").assertDoesNotExist()
        // Motion follows the system's setting alone.
        onNodeWithText("Reduce motion").assertDoesNotExist()
        System.getenv("SETTINGS_SCREENSHOT")?.let { save(it.replace(".png", "-phone.png")) }

        onNodeWithTag("phone-slider-reading font size").performSemanticsAction(SemanticsActions.SetProgress) { it(1.3f) }
        waitUntil(timeoutMillis = 5_000) { settings.current.readingFontScale == 1.3f }
        onNodeWithText("130%").assertExists()

        onNodeWithTag("phone-theme").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Darcula").fetchSemanticsNodes().isNotEmpty() }
        // The light themes come first, so the dark ones are further down the sheet.
        onNodeWithText("Darcula").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.themeId == AppThemes.all.first { it.label == "Darcula" }.id }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Gruvbox Dark").fetchSemanticsNodes().isEmpty() }

        onNodeWithTag("phone-font").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Literata").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Literata").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.readingFont == "literata" }

        onNodeWithText("Reset").performClick()
        onAllNodesWithText("Reset").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.readingFontScale == 1.0f && settings.current.readingFont == "serif" }
    }

    /**
     * A phone's settings end with the pages the More sheet does not list, in the plan's groups, each
     * with its current value where it has one.
     */
    @Test
    fun aPhoneIndexesTheSettingsMoreLeavesOut() = runDesktopComposeUiTest(width = 400, height = 2600) {
        val settings = SettingsRepositoryImpl(MapSettings())
        runBlocking { settings.update { it.copy(speechEngine = SpeechEngine.PIPER, flashcardNewPerDay = 15) } }
        val visited = mutableListOf<Route>()
        setContent { Hosted(settings) { SettingsScreen(onNavigate = { visited += it }, viewModel = SettingsViewModel(settings)) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Preferences").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Preferences").assertExists()
        onNodeWithText("Data").assertDoesNotExist()
        // The current value beside the page: the cards a day.
        onNodeWithText("15 new a day").assertExists()
        for (route in listOf(Route.Goals, Route.FlashcardSettings, Route.VocabularySettings)) onNodeWithTag("settings-link-$route").assertExists()
        // More lists the offline resources, the languages, the backups and About; a phone has no keyboard shortcuts page.
        onNodeWithText("Offline resources").assertDoesNotExist()
        for (route in listOf(Route.Speech, Route.Languages, Route.Backups, Route.About, Route.Shortcuts)) onNodeWithTag("settings-link-$route").assertDoesNotExist()
        System.getenv("SETTINGS_SCREENSHOT")?.let { save(it.replace(".png", "-phone-index.png")) }
        // A phone has no sidebar, so its goals are reached from here.
        onNodeWithTag("settings-link-${Route.Goals}").performClick()
        assertEquals(listOf<Route>(Route.Goals), visited)
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

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
    }
}

/** The screen as the app hosts it: themed from the settings, with the window width and a learning language. */
@Composable
internal fun Hosted(settings: SettingsRepositoryImpl, content: @Composable () -> Unit) {
    val current by settings.settings.collectAsState()
    IdiomikoTheme(AppThemes.byId(current.themeId)) {
        ProvideWindowWidth {
            CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L) {}, content = content)
        }
    }
}
