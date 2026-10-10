package com.tayra.languages.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.courses.CoursePackStatus
import com.tayra.languages.core.domain.courses.CoursePacks
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.ui.components.PageColumn
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** The course pack is offered for download, and once installed it can be removed after a confirmation. */
@OptIn(ExperimentalTestApi::class)
class CoursePacksScreenTest {
    private val pack = CoursePacks.forLanguage("pt").single()

    @Test
    fun aPackIsDownloadedAndRemovedAfterConfirming() = runDesktopComposeUiTest(width = 1400, height = 900) {
        var state by mutableStateOf<PackState>(PackState.NotInstalled)
        var removed = 0
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                PageColumn(PaddingValues()) {
                    CoursePacksContent(
                        listOf(CoursePackStatus(pack, state)),
                        onDownload = { state = PackState.Installed(4_049_920) },
                        onRemove = { removed++; state = PackState.NotInstalled },
                    )
                }
            }
        }
        onNodeWithText("Portuguese (Brazil)").assertExists()
        onNodeWithText("Not downloaded").assertExists()
        onNodeWithText("1.2 MB to download").assertExists()
        System.getenv("COURSE_PACKS_SCREENSHOT")?.let { save(it) }

        onNodeWithTag("download-courses-pt").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("remove-courses-pt").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Installed").assertExists()
        onNodeWithText("4.0 MB on this device").assertExists()
        System.getenv("COURSE_PACKS_SCREENSHOT")?.let { save(it.replace(".png", "-installed.png")) }

        onNodeWithTag("remove-courses-pt").performClick()
        onNodeWithText("Remove the Portuguese (Brazil) courses?").assertExists()
        assertEquals(0, removed, "nothing is removed before confirming")
        onNodeWithTag("confirm-remove").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("download-courses-pt").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, removed)
    }

    @Test
    fun aFailedDownloadCanBeTriedAgain() = runDesktopComposeUiTest(width = 1400, height = 900) {
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                Column { CoursePacksContent(listOf(CoursePackStatus(pack, PackState.Failed("Server answered 404"))), onDownload = {}, onRemove = {}) }
            }
        }
        onNodeWithText("Server answered 404").assertExists()
        onNodeWithText("Try again").assertExists()
    }

    /** Only the pack of the language being learned is listed; a language without one says so. */
    @Test
    fun aLanguageWithoutAPackSaysSo() = runDesktopComposeUiTest(width = 1400, height = 900) {
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                Column { CoursePacksContent(emptyList(), onDownload = {}, onRemove = {}, languageName = "Finnish") }
            }
        }
        onNodeWithText("There are no ready-made courses for Finnish yet.").assertExists()
        onNodeWithText("Download").assertDoesNotExist()
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
    }
}
