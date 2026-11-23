package com.idiomiko.feature.settings

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
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.backup.BackupFiles
import com.idiomiko.core.data.backup.BackupRepositoryImpl
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.BookRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.model.BookDraft
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.service.BookService
import com.idiomiko.core.domain.service.ClearStep
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

/** The Clear data page asks before it removes anything, then shows each step going and what failed. */
@OptIn(ExperimentalTestApi::class)
class ClearDataScreenTest {

    private object Fixed : Clock {
        override fun now(): Instant = Instant.parse("2026-10-03T12:00:00Z")
    }

    /** A device with a book on it, and the page's view model, made once: clearing changes the settings, which recomposes the host. */
    private class Device(name: String) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile(name, ".db").also { it.delete() }))
        val books = BookRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val backups = BackupRepositoryImpl(provider, settings, BackupFiles(Files.createTempDirectory(name).toFile()), Fixed, timeZone = { TimeZone.UTC })
        val viewModel = ClearDataViewModel(clearDataService(provider, settings), backups)

        init {
            runBlocking {
                val languages = LanguageRepositoryImpl(provider)
                val language = languages.save(Language(name = "Portuguese"))
                BookService(books, languages).create(BookDraft(languageId = language, title = "O lobo", text = "O lobo dorme."))
            }
        }
    }

    /** Beside the settings sidebar, with Clear data lit, the page backs up, confirms, clears and reports. */
    @Test
    fun thePageBacksUpConfirmsAndClears() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val device = Device("idiomiko-clear-page")
        var cleared = 0
        setContent { Hosted(device.settings) { ClearDataScreen(onNavigate = {}, onBack = {}, onCleared = { cleared++ }, viewModel = device.viewModel) } }

        onNodeWithTag("settings-section-CLEAR_DATA").assertIsSelected()
        onNodeWithTag("settings-section-BACKUPS").assertIsNotSelected()
        for (text in listOf(
            "Clear local data", "Remove your learning data and downloaded files from this device. Your preferences stay.",
            "Will be removed", "Learning data", "Offline content", "Cached audio",
            "Will remain", "Backup files", "Installed engines",
            "Back up before clearing", "This action is permanent", "Cancel", "Clear all data",
        )) onNodeWithText(text).assertExists()
        save("CLEAR_SCREENSHOT")

        onNodeWithTag("clear-backup").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { device.backups.list() }.size == 1 }

        onNodeWithTag("clear-all").performScrollTo().performClick()
        onNodeWithText("Clear all data?").assertExists()
        // The dialog's Cancel, not the page's.
        onAllNodesWithText("Cancel").onLast().performClick()
        assertEquals(1, runBlocking { device.books.getBooks() }.size, "cancelling the dialog removes nothing")

        onNodeWithTag("clear-all").performClick()
        onNodeWithText("Clear everything").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Everything was cleared")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(emptyList(), runBlocking { device.books.getBooks() })
        assertEquals(1, runBlocking { device.backups.list() }.size, "the backups stay")
        onNodeWithText("Cancel").assertDoesNotExist()
        save("CLEAR_SCREENSHOT", "-cleared")
        onNodeWithTag("clear-done").performScrollTo().performClick()
        assertEquals(1, cleared)
    }

    /** Each group shows how far the clearing is, then what could not be removed; Cancel goes back. */
    @Test
    fun theStepsAreReportedAndCancelGoesBack() = runDesktopComposeUiTest(width = 1400, height = 1000) {
        var state by mutableStateOf(ClearDataUiState())
        var back = 0
        var done = 0
        setContent {
            Hosted(SettingsRepositoryImpl(MapSettings())) {
                // Laid out as the page column lays it out on the screen.
                Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ClearDataContent(state, onClear = {}, onBackup = {}, onDone = { done++ }, onBack = { back++ })
                }
            }
        }
        onNodeWithTag("clear-cancel").performClick()
        assertEquals(1, back)

        state = ClearDataUiState(started = true, running = ClearStep.DICTIONARIES)
        waitForIdle()
        onNodeWithText("This takes a moment; the items above show how far it is.").assertExists()
        save("CLEAR_SCREENSHOT", "-running")

        state = ClearDataUiState(started = true, done = true, problems = mapOf(ClearStep.VOICES to "Piper: the folder is in use"))
        waitForIdle()
        onNodeWithText("Some things could not be removed").assertExists()
        onNodeWithText("Could not remove: Piper: the folder is in use").assertExists()
        onNodeWithTag("clear-done").performClick()
        assertEquals(1, done)
    }

    /** A phone stacks the two cards, the backup reminder and the permanent-action box, each with a full-width button. */
    @Test
    fun aPhoneConfirmsAndClearsFromItsOwnPage() = runDesktopComposeUiTest(width = 400, height = 1500) {
        val device = Device("idiomiko-clear-phone")
        var cleared = 0
        setContent { Hosted(device.settings) { ClearDataScreen(onNavigate = {}, onBack = {}, onCleared = { cleared++ }, viewModel = device.viewModel) } }
        onNodeWithTag("settings-sidebar").assertDoesNotExist()
        for (text in listOf("Clear local data", "Will be removed", "Will remain", "Back up before clearing", "Clear all data")) onNodeWithText(text).assertExists()
        save("CLEAR_SCREENSHOT", "-phone")

        onNodeWithTag("clear-all").performScrollTo().performClick()
        onNodeWithText("Clear all data?").assertExists()
        onAllNodesWithText("Cancel").onLast().performClick()
        assertEquals(1, runBlocking { device.books.getBooks() }.size, "cancelling the dialog removes nothing")
        onNodeWithTag("clear-all").performClick()
        onNodeWithText("Clear everything").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Everything was cleared")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(emptyList(), runBlocking { device.books.getBooks() })
        onNodeWithTag("clear-done").performScrollTo().performClick()
        assertEquals(1, cleared)
    }

    private fun ComposeUiTest.save(env: String, suffix: String = "") {
        val path = System.getenv(env) ?: return
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path.replace(".png", "$suffix.png")))
    }
}
