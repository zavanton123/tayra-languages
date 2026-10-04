package com.tayra.languages.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
import org.junit.Rule
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The Backups screen makes, lists, restores and deletes backups. */
class BackupScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private class Ticking(var now: Instant = Instant.parse("2026-10-03T12:00:00Z")) : Clock {
        override fun now(): Instant = now.also { now += 5.minutes }
    }

    @Test
    fun aBackupIsCreatedRestoredAndDeleted() {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-backup-screen", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val book = runBlocking {
            val language = languages.save(Language(name = "Portuguese"))
            BookService(books, languages).create(BookDraft(languageId = language, title = "O lobo", text = "O lobo dorme."))
        }
        val repository = BackupRepositoryImpl(
            provider,
            SettingsRepositoryImpl(MapSettings()),
            BackupFiles(Files.createTempDirectory("tayra-backup-screen").toFile()),
            Ticking(),
            timeZone = { TimeZone.UTC },
        )
        var restored = false
        rule.setContent { BackupScreen(onNavigate = {}, onBack = {}, onRestored = { restored = true }, viewModel = BackupViewModel(repository)) }

        rule.waitUntil(5_000) { rule.onAllNodesWithText("No backups yet.").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("Create backup").onLast().performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("3 Oct 2026, 12:00:00").fetchSemanticsNodes().isNotEmpty() }

        // The book is deleted after the backup; restoring brings it back.
        runBlocking { books.deleteBook(book) }
        rule.onAllNodes(hasText("Restore") and hasClickAction())[0].performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Restore this backup?").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasText("Restore") and hasClickAction()).onLast().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Backup restored").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("O lobo"), runBlocking { books.getBooks() }.map { it.title })
        // The data replaced by the restore was backed up first.
        rule.waitUntil(5_000) { rule.onAllNodesWithText("3 Oct 2026, 12:05:00").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("OK").onLast().performClick()
        rule.waitUntil(5_000) { restored }
        System.getenv("BACKUP_SCREENSHOT")?.let { save(it) }

        rule.onAllNodesWithContentDescription("Delete backup")[0].performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Delete this backup?").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasText("Delete") and hasClickAction()).onLast().performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("3 Oct 2026, 12:05:00").fetchSemanticsNodes().isEmpty() }
        assertTrue(rule.onAllNodesWithText("3 Oct 2026, 12:00:00").fetchSemanticsNodes().isNotEmpty())
        System.getenv("BACKUP_SCREENSHOT")?.let { save(it.replace(".png", "-after-delete.png")) }
    }

    private fun save(path: String) {
        rule.waitForIdle()
        ImageIO.write(rule.onRoot().captureToImage().toAwtImage(), "png", File(path))
    }
}
