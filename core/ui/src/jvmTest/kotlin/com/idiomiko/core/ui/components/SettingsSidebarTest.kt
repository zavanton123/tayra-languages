package com.idiomiko.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    /** A wide window lists every settings section, General first, with the page's own lit; another one opens in its place. */
    @Test
    fun theSidebarLightsTheCurrentSectionAndOpensTheOthers() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { ProvideWindowWidth { Page(SettingsSection.SPEECH, onNavigate = { visited += it }) } } }
        onNodeWithTag("settings-sidebar").assertExists()
        assertEquals(SettingsSection.GENERAL, SettingsSection.entries.first())
        for (section in SettingsSection.entries) {
            val row = onNodeWithTag("settings-section-${section.name}")
            if (section == SettingsSection.SPEECH) row.assertIsSelected() else row.assertIsNotSelected()
        }
        // The sidebar's title is the only Settings left: the page's way back would repeat it.
        assertEquals(1, onAllNodesWithText("Settings").fetchSemanticsNodes().size)
        onNodeWithTag("settings-section-GENERAL").performClick()
        onNodeWithTag("settings-section-BACKUPS").performClick()
        // The page shown already is not opened again.
        onNodeWithTag("settings-section-SPEECH").performClick()
        assertEquals(listOf<Route>(Route.Settings, Route.Backups), visited)
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
        onNodeWithTag("settings-section-CLEAR_DATA").performClick()
        assertEquals(listOf<Route>(Route.Backups, Route.ClearData), switched)
        assertEquals(emptyList(), linked)
    }

    /** Clearing the data has its own section right below Backups, lit while its page is open. */
    @Test
    fun clearDataFollowsBackupsAndIsLitOnItsPage() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { ProvideWindowWidth { Page(SettingsSection.CLEAR_DATA, onNavigate = { visited += it }) } } }
        assertEquals(
            listOf("General", "Goals", "Languages", "Flashcards", "Vocabulary", "Hotkeys", "Speech", "Translation", "Backups", "Clear data", "Updates", "Experiments"),
            SettingsSection.entries.map { it.label },
        )
        onNodeWithTag("settings-section-CLEAR_DATA").assertIsSelected()
        onNodeWithTag("settings-section-BACKUPS").assertIsNotSelected()
        val backups = onNodeWithTag("settings-section-BACKUPS").fetchSemanticsNode().boundsInRoot
        val clear = onNodeWithTag("settings-section-CLEAR_DATA").fetchSemanticsNode().boundsInRoot
        assertTrue(clear.top >= backups.bottom, "Clear data sits below Backups")
        onNodeWithTag("settings-section-BACKUPS").performClick()
        assertEquals(listOf<Route>(Route.Backups), visited)
    }

    /** Goals follows General, lit while its page is open, and opens from the others. */
    @Test
    fun goalsFollowsGeneralAndIsLitOnItsPage() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { ProvideWindowWidth { Page(SettingsSection.GOALS, onNavigate = { visited += it }) } } }
        assertEquals(SettingsSection.GOALS, SettingsSection.entries[1])
        onNodeWithTag("settings-section-GOALS").assertIsSelected()
        onNodeWithTag("settings-section-GENERAL").assertIsNotSelected()
        val general = onNodeWithTag("settings-section-GENERAL").fetchSemanticsNode().boundsInRoot
        val goals = onNodeWithTag("settings-section-GOALS").fetchSemanticsNode().boundsInRoot
        val languages = onNodeWithTag("settings-section-LANGUAGES").fetchSemanticsNode().boundsInRoot
        assertTrue(goals.top >= general.bottom && languages.top >= goals.bottom, "Goals sits between General and Languages")
        onNodeWithTag("settings-section-GENERAL").performClick()
        assertEquals(listOf<Route>(Route.Settings), visited)
    }

    /** Updates sits between Clear data and Experiments, and is left out where the app provides it as hidden. */
    @Test
    fun updatesFollowsClearDataAndCanBeLeftOut() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        var hidden by mutableStateOf(emptySet<SettingsSection>())
        setContent {
            MaterialTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalHiddenSettingsSections provides hidden) { Page(SettingsSection.UPDATES, onNavigate = {}) }
                }
            }
        }
        onNodeWithTag("settings-section-UPDATES").assertIsSelected()
        val clear = onNodeWithTag("settings-section-CLEAR_DATA").fetchSemanticsNode().boundsInRoot
        val updates = onNodeWithTag("settings-section-UPDATES").fetchSemanticsNode().boundsInRoot
        val experiments = onNodeWithTag("settings-section-EXPERIMENTS").fetchSemanticsNode().boundsInRoot
        assertTrue(updates.top >= clear.bottom && experiments.top >= updates.bottom, "Updates sits between Clear data and Experiments")

        hidden = setOf(SettingsSection.UPDATES)
        onNodeWithTag("settings-section-UPDATES").assertDoesNotExist()
        onNodeWithTag("settings-section-EXPERIMENTS").assertExists()
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
