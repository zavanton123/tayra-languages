package com.tayra.languages.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.backup.BackupFiles
import com.tayra.languages.core.data.backup.BackupRepositoryImpl
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
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

/** The Backups screen makes, lists, restores and deletes backups. */
@OptIn(ExperimentalTestApi::class)
class BackupScreenTest {

    private class Ticking(var now: Instant = Instant.parse("2026-10-03T12:00:00Z")) : Clock {
        override fun now(): Instant = now.also { now += 5.minutes }
    }

    @Test
    fun aBackupIsCreatedRestoredAndDeleted() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-backup-screen", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val book = runBlocking {
            val language = languages.save(Language(name = "Portuguese"))
            BookService(books, languages).create(BookDraft(languageId = language, title = "O lobo", text = "O lobo dorme."))
        }
        val repository = BackupRepositoryImpl(provider, settings, BackupFiles(Files.createTempDirectory("tayra-backup-screen").toFile()), Ticking(), timeZone = { TimeZone.UTC })
        var restored = false
        val viewModel = BackupViewModel(repository)
        setContent { Hosted(settings) { BackupScreen(onNavigate = {}, onBack = {}, onRestored = { restored = true }, viewModel = viewModel) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("No backups yet", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        onAllNodes(hasText("Create backup") and hasClickAction()).onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("3 Oct 2026, 12:00:00")).fetchSemanticsNodes().isNotEmpty() }

        // The book is deleted after the backup; restoring brings it back.
        runBlocking { books.deleteBook(book) }
        onAllNodes(hasText("Restore") and hasClickAction())[0].performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Restore this backup?")).fetchSemanticsNodes().isNotEmpty() }
        onAllNodes(hasText("Restore") and hasClickAction()).onLast().performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Backup restored")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("O lobo"), runBlocking { books.getBooks() }.map { it.title })
        // The data replaced by the restore was backed up first.
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("3 Oct 2026, 12:05:00")).fetchSemanticsNodes().isNotEmpty() }
        onAllNodes(hasText("OK")).onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { restored }
        System.getenv("BACKUP_SCREENSHOT")?.let { save(it) }

        onNodeWithContentDescription("More for the backup of 3 Oct 2026, 12:05:00").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Delete")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Delete").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Delete this backup?")).fetchSemanticsNodes().isNotEmpty() }
        onAllNodes(hasText("Delete") and hasClickAction()).onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("3 Oct 2026, 12:05:00")).fetchSemanticsNodes().isEmpty() }
        onNodeWithText("3 Oct 2026, 12:00:00").assertExists()
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}
