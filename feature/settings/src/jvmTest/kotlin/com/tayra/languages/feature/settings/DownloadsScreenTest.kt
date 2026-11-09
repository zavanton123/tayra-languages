package com.tayra.languages.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.foundation.layout.PaddingValues
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.books.BookPackStatus
import com.tayra.languages.core.domain.books.BookPacks
import com.tayra.languages.core.domain.courses.CoursePackStatus
import com.tayra.languages.core.domain.courses.CoursePacks
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.ocr.OcrReadiness
import com.tayra.languages.core.domain.service.SetupFile
import com.tayra.languages.core.domain.service.SetupFileKind
import com.tayra.languages.core.domain.service.SetupItem
import com.tayra.languages.core.domain.service.SetupKind
import com.tayra.languages.core.domain.service.SetupStatus
import com.tayra.languages.core.ui.components.PageColumn
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** Every download of the language being learned on one page, each fetched or removed in place. */
@OptIn(ExperimentalTestApi::class)
class DownloadsScreenTest {
    private val courses = CoursePacks.forLanguage("pt").single()
    private val books = BookPacks.forLanguage("pt").first()
    private val dictionary = DictionaryPacks.find("pt", "en")!!

    private val piper = SetupItem(
        "voice:PIPER:pt_BR-faber", SetupKind.VOICE, "Portuguese", "Piper · faber", emptyList(), recommended = false,
        status = SetupStatus.INSTALLED, engine = "Piper", onDeviceBytes = 63_000_000,
    )
    private val piperMissing = SetupItem(
        "voice:PIPER:pt_PT-tugao", SetupKind.VOICE, "Portuguese", "Piper · tugão", listOf(SetupFile(SetupFileKind.VOICE, "tugão", 63_000_000)), recommended = true,
        engine = "Piper",
    )
    private val translation = SetupItem(
        "translation:pt-en", SetupKind.TRANSLATION, "Portuguese", "Argos Translate · Portuguese → English",
        listOf(SetupFile(SetupFileKind.MODEL, "Portuguese → English", 50_000_000)), recommended = true,
    )
    private val recognition = SetupItem(
        "recognition:medium", SetupKind.RECOGNITION, "Portuguese", "MEDIUM", emptyList(), recommended = false,
        status = SetupStatus.INSTALLED, onDeviceBytes = 540_000_000,
    )

    private fun state(course: PackState, removed: Boolean) = DownloadsUiState(
        language = Language(1, "Portuguese"),
        code = "pt",
        native = "en",
        courses = listOf(CoursePackStatus(courses, course)),
        books = listOf(BookPackStatus(books, PackState.Downloading(0.4f))),
        dictionaries = listOf(PackStatus(dictionary, PackState.Installed(12_000_000))),
        tools = listOfNotNull(piper.takeUnless { removed }, piperMissing, translation, recognition),
        ocr = OcrDownloads(readiness = OcrReadiness.NeedsDownload(15_000_000)),
    )

    @Test
    fun theLanguagesDownloadsAreFetchedAndRemovedInPlace() = show(width = 1586, height = 1400, shot = "")

    @Test
    fun aPhoneListsTheSameDownloads() = show(width = 400, height = 2600, shot = "-phone")

    private fun show(width: Int, height: Int, shot: String) = runDesktopComposeUiTest(width = width, height = height) {
        var course by mutableStateOf<PackState>(PackState.NotInstalled)
        var voiceRemoved by mutableStateOf(false)
        val installed = mutableListOf<String>()
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                PageColumn(PaddingValues()) {
                    DownloadsContent(
                        state(course, voiceRemoved),
                        progress = emptyMap(),
                        actions = DownloadActions(
                            downloadCourses = { course = PackState.Installed(4_049_920) },
                            removeCourses = { course = PackState.NotInstalled },
                            install = { installed += it.id },
                            canRemove = { it.status == SetupStatus.INSTALLED },
                            remove = { if (it.id == piper.id) voiceRemoved = true },
                        ),
                        onNavigate = {},
                    )
                }
            }
        }
        onNodeWithText("3 downloaded", substring = true).assertExists()
        onNodeWithText("Courses").assertExists()
        onNodeWithText("Voices").performScrollTo()
        onNodeWithText("Offline translation").assertExists()
        onNodeWithText("Speech recognition").assertExists()
        onNodeWithText("Text recognition").assertExists()
        System.getenv("DOWNLOADS_SCREENSHOT")?.let { save(it.replace(".png", "$shot.png")) }

        onNodeWithTag("download-courses-${courses.id}").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("remove-courses-${courses.id}").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("4.0 MB on this device").assertExists()

        onNodeWithTag("download-translation").performScrollTo().performClick()
        assertEquals(listOf(translation.id), installed)

        onNodeWithTag("remove-voice-${piper.id}").performScrollTo().performClick()
        onNodeWithTag("confirm-remove").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("remove-voice-${piper.id}").fetchSemanticsNodes().isEmpty() }
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
    }
}
