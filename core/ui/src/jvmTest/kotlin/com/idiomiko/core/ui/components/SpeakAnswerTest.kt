package com.idiomiko.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SpeakAnswerTest {
    private var listening by mutableStateOf(false)
    private var started = 0
    private var stopped = 0
    private val ready = MutableStateFlow(false)

    private fun androidx.compose.ui.test.ComposeUiTest.answer() = setContent {
        MaterialTheme {
            SpeakAnswer(
                listening = listening, recognising = false, heard = "", retry = null, answered = false,
                onListen = { started++; listening = true }, onStop = { stopped++; listening = false; ready.value = false },
                onTypeInstead = {}, tag = "answer", ready = ready,
            )
        }
    }

    /** Held, the microphone waits to open, says when to speak, and ends with the hold. */
    @Test
    fun holdingSpeaksUntilLetGo() = runDesktopComposeUiTest(width = 400, height = 600) {
        answer()
        onNodeWithTag("microphone-label", useUnmergedTree = true).assertTextEquals("Hold to speak")
        onNodeWithTag("answer-speak").performTouchInput { down(center) }
        mainClock.advanceTimeBy(600)
        assertEquals(1, started)
        onNodeWithTag("microphone-label", useUnmergedTree = true).assertTextEquals("Get ready…")
        ready.value = true
        mainClock.advanceTimeBy(100)
        onNodeWithTag("microphone-label", useUnmergedTree = true).assertTextEquals("Release when you're done")
        assertEquals(0, stopped)
        onNodeWithTag("answer-speak").performTouchInput { up() }
        mainClock.advanceTimeBy(100)
        assertEquals(1, stopped)
        onNodeWithTag("microphone-label", useUnmergedTree = true).assertTextEquals("Hold to speak")
    }

    /** Tapped, it keeps listening once the finger is up, and a second tap finishes. */
    @Test
    fun aTapListensUntilTheNext() = runDesktopComposeUiTest(width = 400, height = 600) {
        ready.value = true
        answer()
        onNodeWithTag("answer-speak").performClick()
        waitForIdle()
        assertEquals(1, started)
        assertEquals(0, stopped)
        onNodeWithTag("microphone-label", useUnmergedTree = true).assertTextEquals("Finish")
        onNodeWithTag("answer-speak").performClick()
        waitForIdle()
        assertEquals(1, stopped)
    }
}
