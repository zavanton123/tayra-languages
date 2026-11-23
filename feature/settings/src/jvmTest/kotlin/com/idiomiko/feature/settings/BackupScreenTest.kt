package com.idiomiko.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
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
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The Data screen makes, lists, restores and deletes backups, and clears the data on this device. */
@OptIn(ExperimentalTestApi::class)
class BackupScreenTest {

    private class Ticking(var now: Instant = Instant.parse("2026-10-03T12:00:00Z")) : Clock {
        override fun now(): Instant = now.also { now += 5.minutes }
    }

    @Test
    fun aBackupIsCreatedRestoredAndDeleted() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-backup-screen", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val book = runBlocking {
            val language = languages.save(Language(name = "Portuguese"))
            BookService(books, languages).create(BookDraft(languageId = language, title = "O lobo", text = "O lobo dorme."))
        }
        val repository = BackupRepositoryImpl(provider, settings, BackupFiles(Files.createTempDirectory("idiomiko-backup-screen").toFile()), Ticking(), timeZone = { TimeZone.UTC })
        var restored = false
        val viewModel = BackupViewModel(repository, clearDataService(provider, settings))
        setContent { Hosted(settings) { BackupScreen(onNavigate = {}, onBack = {}, onRestored = { restored = true }, onCleared = {}, viewModel = viewModel) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Create a backup to keep a copy of your data.")).fetchSemanticsNodes().isNotEmpty() }
        onAllNodes(hasText("Create backup") and hasClickAction()).onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Oct 3, 2026, 12:00:00")).fetchSemanticsNodes().isNotEmpty() }

        // The book is deleted after the backup; restoring brings it back.
        runBlocking { books.deleteBook(book) }
        onAllNodes(hasText("Restore") and hasClickAction())[0].performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Restore this backup?")).fetchSemanticsNodes().isNotEmpty() }
        onAllNodes(hasText("Restore") and hasClickAction()).onLast().performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Backup restored")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("O lobo"), runBlocking { books.getBooks() }.map { it.title })
        // The data replaced by the restore was backed up first.
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Oct 3, 2026, 12:05:00")).fetchSemanticsNodes().isNotEmpty() }
        onAllNodes(hasText("OK")).onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { restored }
        System.getenv("BACKUP_SCREENSHOT")?.let { save(it) }

        onNodeWithContentDescription("More for the backup of Oct 3, 2026, 12:05:00").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Delete")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Delete").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Delete this backup?")).fetchSemanticsNodes().isNotEmpty() }
        onAllNodes(hasText("Delete") and hasClickAction()).onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Oct 3, 2026, 12:05:00")).fetchSemanticsNodes().isEmpty() }
        onNodeWithText("Oct 3, 2026, 12:00:00").assertExists()
    }

    /** The history shows the three newest backups; More opens the older ones and Less puts them away. */
    @Test
    fun onlyTheThreeNewestBackupsShowUntilMore() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-backup-more", ".db").also { it.delete() }))
        val settings = SettingsRepositoryImpl(MapSettings())
        val repository = BackupRepositoryImpl(provider, settings, BackupFiles(Files.createTempDirectory("idiomiko-backup-more").toFile()), Ticking(), timeZone = { TimeZone.UTC })
        runBlocking { repeat(7) { repository.create() } }
        val viewModel = BackupViewModel(repository, clearDataService(provider, settings))
        setContent { Hosted(settings) { BackupScreen(onNavigate = {}, onBack = {}, onRestored = {}, onCleared = {}, viewModel = viewModel) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Oct 3, 2026, 12:30:00")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Oct 3, 2026, 12:20:00").assertExists()
        onNodeWithText("Oct 3, 2026, 12:15:00").assertDoesNotExist()
        onNodeWithTag("backups-more").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Oct 3, 2026, 12:00:00")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("backups-more").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Oct 3, 2026, 12:00:00")).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun anImportedBackupIsOfferedForRestoring() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-backup-import", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val book = runBlocking {
            val language = languages.save(Language(name = "Portuguese"))
            BookService(books, languages).create(BookDraft(languageId = language, title = "O lobo", text = "O lobo dorme."))
        }
        val repository = BackupRepositoryImpl(provider, settings, BackupFiles(Files.createTempDirectory("idiomiko-backup-import").toFile()), Ticking(), timeZone = { TimeZone.UTC })
        // A file exported earlier: made here, then forgotten by the device.
        val exported = runBlocking { repository.create().let { backup -> repository.read(backup.name).also { repository.delete(backup.name) } } }
        runBlocking { books.deleteBook(book) }
        val viewModel = BackupViewModel(repository, clearDataService(provider, settings))
        setContent { Hosted(settings) { BackupScreen(onNavigate = {}, onBack = {}, onRestored = {}, onCleared = {}, viewModel = viewModel) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Create a backup to keep a copy of your data.")).fetchSemanticsNodes().isNotEmpty() }

        // Declining keeps the file in the list and the data as it is.
        viewModel.import(exported)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Restore the imported backup?")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("No").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Restore the imported backup?")).fetchSemanticsNodes().isEmpty() }
        onNodeWithText("Oct 3, 2026, 12:00:00").assertExists()
        assertEquals(emptyList(), runBlocking { books.getBooks() })

        // Accepting restores it, backing the current data up first.
        viewModel.import(exported)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Restore the imported backup?")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Yes").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Backup restored")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("O lobo"), runBlocking { books.getBooks() }.map { it.title })
    }

    /** A phone has one card with both actions, a plain history list with a menu per backup, and what is included folded away. */
    @Test
    fun aPhoneCreatesAndDeletesABackupFromItsOwnLayout() = runDesktopComposeUiTest(width = 400, height = 1100) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-backup-phone", ".db").also { it.delete() }))
        val settings = SettingsRepositoryImpl(MapSettings())
        runBlocking { LanguageRepositoryImpl(provider).save(Language(name = "Portuguese")) }
        val repository = BackupRepositoryImpl(provider, settings, BackupFiles(Files.createTempDirectory("idiomiko-backup-phone").toFile()), Ticking(), timeZone = { TimeZone.UTC })
        val viewModel = BackupViewModel(repository, clearDataService(provider, settings))
        setContent { Hosted(settings) { BackupScreen(onNavigate = {}, onBack = {}, onRestored = {}, onCleared = {}, viewModel = viewModel) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Protect your data")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Create a backup").assertDoesNotExist()
        onNodeWithTag("phone-create-backup").performClick()
        // The phone row leaves the seconds out.
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Oct 3, 2026, 12:00")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("1 local backup", substring = true).assertDoesNotExist()
        System.getenv("BACKUP_SCREENSHOT")?.let { save(it.replace(".png", "-phone.png")) }

        onNodeWithContentDescription("More for the backup of Oct 3, 2026, 12:00:00").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Delete")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Delete").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Delete this backup?")).fetchSemanticsNodes().isNotEmpty() }
        onAllNodes(hasText("Delete") and hasClickAction()).onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Oct 3, 2026, 12:00")).fetchSemanticsNodes().isEmpty() }
        onNodeWithText("Create a backup to keep a copy of your data.").assertExists()
    }

    /** Clearing the data is at the foot of the Data page: confirmed, reported in place, and the backups stay listed. */
    @Test
    fun clearingTheDataAtTheFootOfThePageKeepsTheBackups() = runDesktopComposeUiTest(width = 1586, height = 2200) {
        clearsAndKeepsTheBackups("idiomiko-data-clear", time = "Oct 3, 2026, 12:00:00", screenshot = "")
    }

    /** A phone stacks the same block under the history. */
    @Test
    fun aPhoneClearsTheDataBelowTheHistory() = runDesktopComposeUiTest(width = 400, height = 2600) {
        clearsAndKeepsTheBackups("idiomiko-data-clear-phone", time = "Oct 3, 2026, 12:00", screenshot = "-phone")
    }

    private fun ComposeUiTest.clearsAndKeepsTheBackups(name: String, time: String, screenshot: String) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile(name, ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        runBlocking {
            val language = languages.save(Language(name = "Portuguese"))
            BookService(books, languages).create(BookDraft(languageId = language, title = "O lobo", text = "O lobo dorme."))
        }
        val repository = BackupRepositoryImpl(provider, settings, BackupFiles(Files.createTempDirectory(name).toFile()), Ticking(), timeZone = { TimeZone.UTC })
        runBlocking { repository.create() }
        var cleared = 0
        // Made once: clearing changes the settings, which recomposes the host.
        val viewModel = BackupViewModel(repository, clearDataService(provider, settings))
        setContent { Hosted(settings) { BackupScreen(onNavigate = {}, onBack = {}, onRestored = {}, onCleared = { cleared++ }, viewModel = viewModel) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(time)).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Data").assertExists()
        onNodeWithText("Will be removed").assertExists()
        onNodeWithText("Will remain").assertExists()
        System.getenv("DATA_SCREENSHOT")?.let { save(it.replace(".png", "$screenshot.png")) }

        onNodeWithTag("clear-all").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Clear all data?")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Clear everything").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Everything was cleared")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(emptyList(), runBlocking { books.getBooks() })
        onNodeWithText(time).assertExists()
        onNodeWithText("Create a backup to keep a copy of your data.").assertDoesNotExist()
        System.getenv("DATA_SCREENSHOT")?.let { save(it.replace(".png", "$screenshot-cleared.png")) }

        onNodeWithTag("clear-done").performScrollTo().performClick()
        assertEquals(1, cleared)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}
