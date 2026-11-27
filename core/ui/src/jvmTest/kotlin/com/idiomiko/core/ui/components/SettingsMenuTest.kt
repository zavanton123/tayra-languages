package com.idiomiko.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.ui.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals

/** A phone's Settings page lists the settings pages in the sidebar's order, less those the app hides. */
@OptIn(ExperimentalTestApi::class)
class SettingsMenuTest {
    @Test
    fun theSettingsPageListsThePagesInTheSidebarsOrder() = runDesktopComposeUiTest(width = 400, height = 1000) {
        val visited = mutableListOf<Route>()
        var back = 0
        setContent {
            MaterialTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(
                        LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Croatian"), 1L) {},
                        LocalHiddenSettingsSections provides setOf(SettingsSection.UPDATES, SettingsSection.EXPERIMENTS),
                    ) {
                        SettingsMenuScreen(onNavigate = { visited += it }, onBack = { back++ })
                    }
                }
            }
        }
        val shown = listOf("General", "Languages", "Goals", "Flashcard settings", "Vocabulary settings", "Speech", "Playlist settings", "Translation", "Backups", "Clear data")
        val tops = shown.map { onNodeWithTag("settings-menu-$it").fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops.sorted(), tops, "in the sidebar's order")
        for (gone in listOf("Updates", "Experiments", "Hotkeys")) assertEquals(0, onAllNodesWithTag("settings-menu-$gone").fetchSemanticsNodes().size, gone)
        onNodeWithText("Croatian").assertExists()
        System.getenv("SETTINGS_MENU_SCREENSHOT")?.let { path ->
            waitForIdle()
            javax.imageio.ImageIO.write(onAllNodes(isRoot()).onFirst().captureToImage().toAwtImage(), "png", java.io.File(path))
        }
        onNodeWithTag("settings-menu-Speech").performClick()
        onNodeWithContentDescription("Back").performClick()
        assertEquals(listOf<Route>(Route.Speech), visited)
        assertEquals(1, back)
    }
}
