package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.service.ClearStep
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** The Clear page asks before it removes anything, then shows each step going and what failed. */
@OptIn(ExperimentalTestApi::class)
class ClearDataScreenTest {
    @Test
    fun clearingIsConfirmedFirstAndThenReported() = runDesktopComposeUiTest(width = 1400, height = 1000) {
        var state by mutableStateOf(ClearDataUiState())
        var cleared = 0
        var done = 0
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                // Laid out as the page column lays it out on the screen.
                Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ClearDataContent(state, onClear = { cleared++ }, onBackups = {}, onDone = { done++ }, onBack = {})
                }
            }
        }
        onNodeWithText("Will be removed").assertExists()
        save("CLEAR_SCREENSHOT")

        onNodeWithTag("clear-all").performClick()
        onNodeWithText("Clear all data?").assertExists()
        onNodeWithText("This action is permanent").assertExists()
        // The dialog's Cancel, not the page's.
        onAllNodesWithText("Cancel").onLast().performClick()
        assertEquals(0, cleared, "cancelling the dialog removes nothing")

        onNodeWithTag("clear-all").performClick()
        onNodeWithText("Clear everything").performClick()
        assertEquals(1, cleared)

        state = ClearDataUiState(started = true, running = ClearStep.DICTIONARIES)
        waitForIdle()
        onNodeWithText("This takes a moment; the items above show how far it is.").assertExists()
        save("CLEAR_SCREENSHOT", "-running")

        state = ClearDataUiState(started = true, done = true, problems = mapOf(ClearStep.VOICES to "Piper: the folder is in use"))
        waitForIdle()
        onNodeWithText("Could not remove: Piper: the folder is in use").assertExists()
        save("CLEAR_SCREENSHOT", "-done")
        onNodeWithTag("clear-done").performClick()
        assertEquals(1, done)
    }

    private fun ComposeUiTest.save(env: String, suffix: String = "") {
        val path = System.getenv(env) ?: return
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path.replace(".png", "$suffix.png")))
    }
}
