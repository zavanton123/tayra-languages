package com.tayra.languages.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A shortcut is changed by selecting it and pressing keys; clashes are shown, and Backspace clears one. */
@OptIn(ExperimentalTestApi::class)
class ShortcutsScreenTest {

    @Test
    fun pressingKeysChangesAShortcutAndClashesAreShown() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        setContent { Hosted(settings) { ShortcutsScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings)) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("No conflicts", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("SHORTCUTS_SCREENSHOT")?.let { save(it) }

        // Selecting a shortcut listens for keys; Ctrl+J becomes the new one.
        onNodeWithContentDescription("Change shortcut for Move to previous word").performClick()
        waitUntil(timeoutMillis = 5_000) { listening() }
        System.getenv("SHORTCUTS_SCREENSHOT")?.let { save(it.replace(".png", "-editing.png")) }
        onNodeWithContentDescription("Press a key combination").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.J) } }
        waitUntil(timeoutMillis = 5_000) { settings.current.hotkeys[HotkeyAction.PREV_WORD] == Hotkey("J", ctrl = true) }

        // Giving status 1 the Known key clashes with it.
        onNodeWithContentDescription("Change shortcut for Set status to 1").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { listening() }
        onNodeWithContentDescription("Press a key combination").performKeyInput { pressKey(Key.K) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("2 in conflict", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Also used for: set status to known").assertExists()

        // Backspace clears it, which ends the clash.
        onNodeWithContentDescription("Change shortcut for Set status to 1").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { listening() }
        onNodeWithContentDescription("Press a key combination").performKeyInput { pressKey(Key.Backspace) }
        waitUntil(timeoutMillis = 5_000) { settings.current.hotkeys[HotkeyAction.STATUS_1] == null }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("No conflicts", substring = true)).fetchSemanticsNodes().isNotEmpty() }

        // Search narrows the list to matching actions.
        onNodeWithText("Search shortcuts").performScrollTo().performTextInput("bookmark")
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Read the next sentence")).fetchSemanticsNodes().isEmpty() }
        onNodeWithText("Bookmark the current page").assertExists()
        assertNull(settings.current.hotkeys[HotkeyAction.STATUS_1])
        assertEquals(Hotkey("J", ctrl = true), settings.current.hotkeys[HotkeyAction.PREV_WORD])
    }

    private fun ComposeUiTest.listening() = onAllNodes(hasContentDescription("Press a key combination")).fetchSemanticsNodes().isNotEmpty()

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}
