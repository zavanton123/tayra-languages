package com.tayra.languages.core.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** A dropdown tells of each option the pointer comes onto, to preview it, and of none when it closes without a choice. */
@OptIn(ExperimentalTestApi::class)
class DropdownHoverTest {
    @Test
    fun theOptionUnderThePointerIsPreviewed() = runDesktopComposeUiTest(width = 600, height = 600) {
        val hovered = mutableListOf<String?>()
        var selected by mutableStateOf("Default")
        setContent {
            TayraTheme {
                Dropdown(listOf("Default", "Sepia", "Teal"), selected, onSelect = { selected = it }, label = null, optionLabel = { it }, onHover = { hovered += it })
            }
        }
        onNodeWithText("Default").performClick()
        waitForIdle()
        onNodeWithText("Sepia").performMouseInput { enter(center) }
        onNodeWithText("Teal").performMouseInput { moveTo(center) }
        waitForIdle()
        assertEquals(listOf("Sepia", "Teal"), hovered.drop(hovered.indexOf("Sepia")))

        onAllNodesWithText("Default").onLast().performMouseInput { moveTo(center) }
        hovered.clear()
        onNodeWithText("Sepia").performClick()
        waitForIdle()
        assertEquals("Sepia", selected)
        assertEquals("Sepia", hovered.last(), "after a choice, the chosen option stays shown until the setting takes over")
    }

    @Test
    fun theClosingMenuSlidingUnderThePointerIsNotPreviewed() = runDesktopComposeUiTest(width = 600, height = 600) {
        val hovered = mutableListOf<String?>()
        var selected by mutableStateOf("Default")
        setContent {
            TayraTheme {
                Dropdown(listOf("Default", "Sepia", "Teal"), selected, onSelect = { selected = it }, label = null, optionLabel = { it }, onHover = { hovered += it })
            }
        }
        onNodeWithText("Default").performClick()
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithText("Sepia").performClick()
        mainClock.advanceTimeByFrame()
        // The menu shrinks towards its top as it goes, so the option after the chosen one passes under the pointer.
        onNodeWithText("Teal").performMouseInput { enter(center) }
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals("Sepia", selected)
        assertEquals("Sepia", hovered.last())
    }

    @Test
    fun closingWithoutAChoiceGoesBack() = runDesktopComposeUiTest(width = 600, height = 600) {
        val hovered = mutableListOf<String?>()
        setContent {
            TayraTheme {
                Dropdown(listOf("Default", "Sepia"), "Default", onSelect = {}, label = null, optionLabel = { it }, onHover = { hovered += it })
            }
        }
        onNodeWithText("Default").performClick()
        waitForIdle()
        onNodeWithText("Sepia").performMouseInput { enter(center) }
        waitForIdle()
        assertEquals("Sepia", hovered.last())
        // A click on the field again closes the menu.
        onAllNodesWithText("Default").onFirst().performClick()
        waitForIdle()
        assertEquals(null, hovered.last())
    }
}
