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
import com.tayra.languages.core.domain.service.SetupFile
import com.tayra.languages.core.domain.service.SetupFileKind
import com.tayra.languages.core.domain.service.SetupItem
import com.tayra.languages.core.domain.service.SetupKind
import com.tayra.languages.core.domain.service.SetupState
import com.tayra.languages.core.domain.service.SetupStatus
import com.tayra.languages.core.ui.i18n.UiLanguage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** The downloads of a new language: ticked by default unless large, then shown with their progress. */
@OptIn(ExperimentalTestApi::class)
class LanguageSetupDialogTest {
    private val items = listOf(
        SetupItem("courses:courses-el", SetupKind.COURSES, "Greek", "Greek", listOf(SetupFile(SetupFileKind.COURSES, "Greek", 1_196_300)), recommended = true),
        SetupItem("dictionary:el-ru", SetupKind.DICTIONARY, "Greek", "Greek \u2192 Russian", listOf(SetupFile(SetupFileKind.DICTIONARY, "Greek \u2192 Russian", 1_429_346)), recommended = true),
        SetupItem(
            "voice:PIPER:el_GR-rapunzelina-medium", SetupKind.VOICE, "Greek", "Piper · Rapunzelina · medium · Greece",
            listOf(SetupFile(SetupFileKind.ENGINE, "Piper", 86_000_000, estimated = true), SetupFile(SetupFileKind.VOICE, "Rapunzelina · medium · Greece", 63_500_000)),
            recommended = false, switchesEngine = true,
        ),
        SetupItem(
            "translation:el-ru", SetupKind.TRANSLATION, "Greek", "Argos Translate · Greek \u2192 Russian",
            listOf(SetupFile(SetupFileKind.MODEL, "Greek \u2192 English", 72_161_252), SetupFile(SetupFileKind.MODEL, "English \u2192 Russian", 195_746_693)),
            recommended = true,
        ),
    )

    @Test
    fun theChosenDownloadsStartAndShowTheirProgress() = runDesktopComposeUiTest(width = 760, height = 1000) {
        var state by mutableStateOf(LanguageSetupUiState(1, items, items.filter { it.recommended }.map { it.id }.toSet()))
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
        onNodeWithText("Get ready to learn Greek").assertExists()
        onNodeWithText("3 downloads selected · 270.5 MB").assertExists()
        onNodeWithText("Piper engine · about 86.0 MB").assertExists()
        System.getenv("SETUP_SCREENSHOT")?.let { save(it) }
        onNodeWithTag("setup-VOICE").performClick()
        onNodeWithText("4 downloads selected · about 420.0 MB").assertExists()
        onNodeWithText("Download selected").performClick()
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

    @Test
    fun aLanguageWithNothingLeftOnlyOffersDone() = runDesktopComposeUiTest(width = 760, height = 1000) {
        val croatian = listOf(
            SetupItem("dictionary:installed", SetupKind.DICTIONARY, "Croatian", "Croatian \u2192 Russian", emptyList(), recommended = false, status = SetupStatus.INSTALLED),
            SetupItem("courses:unavailable", SetupKind.COURSES, "Croatian", "", emptyList(), recommended = false, status = SetupStatus.UNAVAILABLE),
            SetupItem("voice:unavailable", SetupKind.VOICE, "Croatian", "Piper", emptyList(), recommended = false, status = SetupStatus.UNAVAILABLE),
            SetupItem("translation:unavailable", SetupKind.TRANSLATION, "Croatian", "Argos Translate · Croatian \u2192 Russian", emptyList(), recommended = false, status = SetupStatus.UNAVAILABLE),
        )
        var closed = 0
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                LanguageSetupContent(LanguageSetupUiState(1, croatian), emptyMap(), {}, {}, {}, onClosed = { closed++ })
            }
        }
        onNodeWithText("Everything available for Croatian is already on this device.").assertExists()
        onNodeWithText("Download selected").assertDoesNotExist()
        System.getenv("SETUP_SCREENSHOT")?.let { save(it.replace(".png", "-croatian.png")) }
        onNodeWithTag("setup-done").performClick()
        assertEquals(1, closed)
    }

    @Test
    fun theRussianTextsFit() = runDesktopComposeUiTest(width = 760, height = 1000) {
        UiLanguage.set("ru")
        try {
            setContent {
                Hosted(SettingsRepositoryImpl(MapSettings())) {
                    LanguageSetupContent(LanguageSetupUiState(1, items, items.filter { it.recommended }.map { it.id }.toSet()), emptyMap(), {}, {}, {}, {})
                }
            }
            onNodeWithText("Начинаем учить греческий").assertExists()
            onNodeWithText("Выбрано 3 загрузки · 270.5 МБ").assertExists()
            System.getenv("SETUP_SCREENSHOT")?.let { save(it.replace(".png", "-ru.png")) }
        } finally {
            UiLanguage.set("en")
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
    }
}
