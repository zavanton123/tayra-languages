package com.tayra.languages.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
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
            recommended = false, switchesEngine = true, engine = "Piper",
        ),
        SetupItem(
            "voice:PIPER:el_GR-chreece-low", SetupKind.VOICE, "Greek", "Piper · Chreece · low · Greece",
            listOf(SetupFile(SetupFileKind.ENGINE, "Piper", 86_000_000, estimated = true), SetupFile(SetupFileKind.VOICE, "Chreece · low · Greece", 20_000_000)),
            recommended = false, switchesEngine = true, engine = "Piper",
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
        onNodeWithText("Greek voice · Piper").assertExists()
        onNodeWithText("Chreece · low · Greece").assertExists()
        System.getenv("SETUP_SCREENSHOT")?.let { save(it) }
        onNodeWithTag("setup-voice:PIPER:el_GR-rapunzelina-medium").performClick()
        onNodeWithText("4 downloads selected · about 420.0 MB").assertExists()
        onNodeWithTag("setup-voice:PIPER:el_GR-chreece-low").performClick()
        onNodeWithText("5 downloads selected · about 440.0 MB", substring = false).assertExists()
        onNodeWithTag("setup-voice:PIPER:el_GR-chreece-low").performClick()
        onNodeWithText("Download selected").performClick()
        assertEquals(1, started)
        progress = mapOf(
            items[0].id to SetupState.Done,
            items[1].id to SetupState.Failed("HTTP 503"),
            items[2].id to SetupState.Running(null),
            items[4].id to SetupState.Waiting,
        )
        waitForIdle()
        onNodeWithText("Downloads go on in the background if you close this.").assertExists()
        onNodeWithText("Download failed: HTTP 503").assertExists()
        onNodeWithText("Try again").assertDoesNotExist()
        System.getenv("SETUP_SCREENSHOT")?.let { save(it.replace(".png", "-running.png")) }
        progress = progress + mapOf(items[2].id to SetupState.Done, items[4].id to SetupState.Done)
        onNodeWithText("Try again").performClick()
        assertEquals(SetupState.Waiting, progress[items[1].id])
    }

    @Test
    fun aPhoneGetsABottomSheetSummingUpTheChoice() = runDesktopComposeUiTest(width = 400, height = 860) {
        var state by mutableStateOf(LanguageSetupUiState(1, items, items.filter { it.recommended }.map { it.id }.toSet()))
        var started = 0
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                PhoneSetupSheet(
                    state, emptyMap(),
                    onToggle = { id -> state = state.copy(selected = if (id in state.selected) state.selected - id else state.selected + id) },
                    onStart = { started++; state = state.copy(started = true) },
                    onRetry = {},
                    onClosed = {},
                )
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Get ready to learn Greek").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("3 selected · 270.5 MB").assertExists()
        onNodeWithText("Greek voice · Piper").assertExists()
        onNodeWithText("Chreece · low · Greece").assertExists()
        onNodeWithText("Piper engine · about 86.0 MB").assertDoesNotExist()
        System.getenv("SETUP_SCREENSHOT")?.let { save(it.replace(".png", "-phone.png")) }
        onNodeWithTag("setup-voice:PIPER:el_GR-rapunzelina-medium").performClick()
        onNodeWithText("4 selected · about 420.0 MB").assertExists()
        onNodeWithText("Download selected").performClick()
        assertEquals(1, started)
        onNodeWithText("Downloads go on in the background if you close this.").assertExists()
    }

    @Test
    fun kokoroVoicesAreChosenOneByOneAndShareTheirModel() = runDesktopComposeUiTest(width = 760, height = 720) {
        val model = SetupFile(SetupFileKind.VOICE_MODEL, "Kokoro model with 34 voices", 120_500_000)
        val voices = listOf("pf_dora" to "Dora (Brazilian, female)", "pm_alex" to "Alex (Brazilian, male)").map { (id, name) ->
            SetupItem("voice:KOKORO:kokoro-v1.0:$id", SetupKind.VOICE, "Portuguese", "Kokoro · $name", listOf(model), recommended = id == "pf_dora", engine = "Kokoro", voiceSizeBytes = 522_240)
        } + listOf("cadu" to "Cadu · medium · Brazil", "edresson" to "Edresson · low · Brazil", "faber" to "Faber · medium · Brazil", "jeff" to "Jeff · medium · Brazil").map { (id, name) ->
            SetupItem(
                "voice:PIPER:pt_BR-$id", SetupKind.VOICE, "Portuguese", "Piper · $name",
                listOf(SetupFile(SetupFileKind.VOICE, name, 63_200_000)), recommended = false, switchesEngine = true, engine = "Piper",
            )
        }
        var state by mutableStateOf(LanguageSetupUiState(1, voices, setOf(voices[0].id)))
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                LanguageSetupContent(
                    state, emptyMap(),
                    onToggle = { id -> state = state.copy(selected = if (id in state.selected) state.selected - id else state.selected + id) },
                    onStart = {}, onRetry = {}, onClosed = {},
                )
            }
        }
        onNodeWithText("Kokoro model").assertExists()
        assertEquals(2, onAllNodesWithText("120.5 MB").fetchSemanticsNodes().size, "the model row and the card total")
        onNodeWithText("Alex (Brazilian, male)").assertExists()
        assertEquals(2, onAllNodesWithText("522 kB").fetchSemanticsNodes().size, "each Kokoro voice shows its own size")
        onNodeWithTag("setup-total-Kokoro").assertTextEquals("120.5 MB")
        onNodeWithTag("setup-total-Piper").assertTextEquals("252.8 MB")
        System.getenv("SETUP_SCREENSHOT")?.let { save(it.replace(".png", "-kokoro.png")) }
        onNodeWithTag("setup-voice:KOKORO:kokoro-v1.0:pm_alex").performClick()
        onNodeWithText("2 downloads selected · 120.5 MB").assertExists()

        onNodeWithTag("setup-all-Piper").performClick()
        assertEquals(6, state.selected.size, "the Piper box ticks all its voices")
        onNodeWithTag("setup-all-Kokoro").performClick()
        assertEquals(4, state.selected.size, "every Kokoro voice was ticked, so its box clears them")
        onNodeWithTag("setup-all-Kokoro").performClick()
        onNodeWithTag("setup-voice:KOKORO:kokoro-v1.0:pm_alex").performClick()
        onNodeWithTag("setup-all-Kokoro").performClick()
        assertEquals(6, state.selected.size, "a partly ticked engine gets all its voices ticked")
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
