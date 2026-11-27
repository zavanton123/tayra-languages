package com.idiomiko.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import org.junit.Test
import kotlin.test.assertTrue

class ToastTest {
    /** On a phone a long message wraps and its button stays one line, not a column of letters. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aLongMessageLeavesTheButtonOnOneLine() = runDesktopComposeUiTest(width = 400, height = 800) {
        val state = ToastState()
        setContent { MaterialTheme { ToastHost(state) } }
        state.show("254 слова отмечены как знакомые", "Отменить") {}
        waitForIdle()
        val button = onNodeWithTag("toast-action").getUnclippedBoundsInRoot()
        val message = onNodeWithText("254 слова отмечены как знакомые").getUnclippedBoundsInRoot()
        assertTrue(button.width > button.height * 2, "the button is a word wide: $button")
        assertTrue(button.height < message.height || button.height.value < 40f, "the button is one line high: $button, message $message")
    }
}
