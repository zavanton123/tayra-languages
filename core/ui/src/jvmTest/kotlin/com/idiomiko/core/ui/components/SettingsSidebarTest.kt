package com.idiomiko.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.idiomiko.core.ui.hotkeys.opensSettings
import com.idiomiko.core.ui.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The sidebar beside every settings page on a wide window, and the desktop's shortcut to the settings. */
@OptIn(ExperimentalTestApi::class)
class SettingsSidebarTest {

    /** A wide window lists every settings section, Account first, with the page's own lit; another one opens in its place. */
    @Test
    fun theSidebarLightsTheCurrentSectionAndOpensTheOthers() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { ProvideWindowWidth { Page(SettingsSection.SPEECH, onNavigate = { visited += it }) } } }
        onNodeWithTag("settings-sidebar").assertExists()
        assertEquals(SettingsSection.ACCOUNT, SettingsSection.entries.first())
        for (section in SettingsSection.entries) {
            val row = onNodeWithTag("settings-section-${section.name}")
            if (section == SettingsSection.SPEECH) row.assertIsSelected() else row.assertIsNotSelected()
        }
        // The sidebar's title is the only Settings left: the page's way back would repeat it.
        assertEquals(1, onAllNodesWithText("Settings").fetchSemanticsNodes().size)
        onNodeWithTag("settings-section-GENERAL").performClick()
        onNodeWithTag("settings-section-ACCOUNT").performClick()
        // The page shown already is not opened again.
        onNodeWithTag("settings-section-SPEECH").performClick()
        assertEquals(listOf<Route>(Route.Settings, Route.Account), visited)
    }

    /** Provided by the app, the switch that takes the shown page's place opens the section rather than a plain link. */
    @Test
    fun theAppsSwitchOpensTheSection() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val linked = mutableListOf<Route>()
        val switched = mutableListOf<Route>()
        setContent {
            MaterialTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalSettingsSwitch provides { switched += it }) { Page(SettingsSection.GENERAL, onNavigate = { linked += it }) }
                }
            }
        }
        onNodeWithTag("settings-section-BACKUPS").performClick()
        assertEquals(listOf<Route>(Route.Backups), switched)
        assertEquals(emptyList(), linked)
    }

    /** Below an expanded window there is no sidebar, and the page keeps its way back to Settings. */
    @Test
    fun aMediumWindowKeepsTheWayBackInstead() = runDesktopComposeUiTest(width = 800, height = 1000) {
        var back = 0
        setContent { MaterialTheme { ProvideWindowWidth { Page(SettingsSection.SPEECH, onNavigate = {}, onBack = { back++ }) } } }
        onNodeWithTag("settings-sidebar").assertDoesNotExist()
        onNodeWithText("Settings").performClick()
        assertEquals(1, back)
    }

    /** Beside the sidebar a page is told the width it has: a window just wide enough for the sidebar leaves it a medium one. */
    @Test
    fun thePageBesideTheSidebarIsToldTheWidthItHas() = runDesktopComposeUiTest(width = 1000, height = 1000) {
        var widths = emptyList<WindowWidth>()
        setContent {
            MaterialTheme {
                ProvideWindowWidth {
                    SettingsPane(SettingsSection.GENERAL, PaddingValues(0.dp), onNavigate = {}) { widths = widths + LocalWindowWidth.current }
                }
            }
        }
        onNodeWithTag("settings-sidebar").assertExists()
        waitForIdle()
        assertEquals(WindowWidth.MEDIUM, widths.last())
    }

    @Test
    fun commandCommaOnAMacAndControlCommaElsewhereOpenTheSettings() {
        fun press(key: Key = Key.Comma, ctrl: Boolean = false, meta: Boolean = false, alt: Boolean = false, shift: Boolean = false, macOs: Boolean) =
            opensSettings(key, ctrl, meta, alt, shift, macOs)
        assertTrue(press(meta = true, macOs = true))
        assertTrue(press(ctrl = true, macOs = false))
        assertFalse(press(ctrl = true, macOs = true), "Control is not the Mac's way")
        assertFalse(press(meta = true, macOs = false), "the Windows key is not the others'")
        assertFalse(press(macOs = true))
        assertFalse(press(meta = true, shift = true, macOs = true))
        assertFalse(press(ctrl = true, alt = true, macOs = false))
        assertFalse(press(key = Key.Period, meta = true, macOs = true))
    }

    @androidx.compose.runtime.Composable
    private fun Page(section: SettingsSection, onNavigate: (Route) -> Unit, onBack: () -> Unit = {}) {
        SettingsPane(section, PaddingValues(0.dp), onNavigate) { padding ->
            PageColumn(padding) { ScreenHeader(section.label, null, onBackToSettings = onBack) }
        }
    }
}
