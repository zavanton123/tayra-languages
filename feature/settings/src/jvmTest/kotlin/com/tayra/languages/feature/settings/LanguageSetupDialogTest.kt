package com.tayra.languages.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.service.SetupItem
import com.tayra.languages.core.domain.service.SetupKind
import com.tayra.languages.core.domain.service.SetupState
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** The downloads of a new language: ticked by default unless large, then shown with their progress. */
@OptIn(ExperimentalTestApi::class)
class LanguageSetupDialogTest {
    private val items = listOf(
        SetupItem("courses:courses-fr", SetupKind.COURSES, "French", "French", 1_196_300, recommended = true),
        SetupItem("dictionary:fr-en", SetupKind.DICTIONARY, "French", "French → English", null, recommended = true),
        SetupItem("voice:PIPER:fr_FR-siwis-medium", SetupKind.VOICE, "French", "Piper: Siwis (medium)", 63_000_000, recommended = false, includesRuntime = true, switchesEngine = true),
        SetupItem("translation:fr-en", SetupKind.TRANSLATION, "French", "Argos Translate", 92_000_000, recommended = true),
    )

    @Test
    fun theChosenDownloadsStartAndShowTheirProgress() = runDesktopComposeUiTest(width = 900, height = 760) {
        var state by mutableStateOf(LanguageSetupUiState(items, items.filter { it.recommended }.map { it.id }.toSet()))
        var progress by mutableStateOf<Map<String, SetupState>>(emptyMap())
        var started = 0
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                LanguageSetupContent(
                    state, progress,
                    onToggle = { id -> state = state.copy(selected = if (id in state.selected) state.selected - id else state.selected + id) },
                    onStart = { started++; state = state.copy(started = true) },
                    onRetry = { progress = progress + (items[1].id to SetupState.Waiting) },
                    onClosed = {},
                )
            }
        }
        onNodeWithText("Get ready to learn French").assertExists()
        onNodeWithText("Download (3)").assertExists()
        System.getenv("SETUP_SCREENSHOT")?.let { save(it) }
        onNodeWithTag("setup-VOICE").performClick()
        onNodeWithText("Download (4)").performClick()
        assertEquals(1, started)
        progress = mapOf(
            items[0].id to SetupState.Done,
            items[1].id to SetupState.Failed("HTTP 503"),
            items[2].id to SetupState.Running(null),
            items[3].id to SetupState.Waiting,
        )
        waitForIdle()
        onNodeWithText("Downloads go on in the background if you close this.").assertExists()
        onNodeWithText("Download failed: HTTP 503").assertExists()
        onNodeWithText("Try again").assertDoesNotExist()
        System.getenv("SETUP_SCREENSHOT")?.let { save(it.replace(".png", "-running.png")) }
        progress = progress + mapOf(items[2].id to SetupState.Done, items[3].id to SetupState.Done)
        onNodeWithText("Try again").performClick()
        assertEquals(SetupState.Waiting, progress[items[1].id])
    }

    private fun androidx.compose.ui.test.ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
    }
}
