package com.tayra.languages.core.data

import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.backup.BackupFiles
import com.tayra.languages.core.data.backup.BackupRepositoryImpl
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.backup.BackupException
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** A backup holds all data and the portable settings; restoring it replaces what is there. */
class BackupTest {

    /** Each backup is a second later than the one before, so their names differ. */
    private class Ticking(var now: Instant = Instant.parse("2026-10-03T12:00:00Z")) : Clock {
        override fun now(): Instant = now.also { now += kotlin.time.Duration.parse("1s") }
    }

    private class Env {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-backup", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val service = TermService(terms, languages)
        val settings = SettingsRepositoryImpl(MapSettings())
        val folder: File = Files.createTempDirectory("tayra-backups").toFile()
        val backups = BackupRepositoryImpl(provider, settings, BackupFiles(folder), Ticking(), timeZone = { TimeZone.UTC })
    }

    @Test
    fun aBackupCarriesTheFlashcardsAndTheirSettings() = runBlocking<Unit> {
        val env = Env()
        val language = env.languages.save(Language(name = "Portuguese"))
        val lobo = env.terms.save(Term(languageId = language, text = "lobo", textLc = "lobo", status = TermStatus.NEW_1, translation = "wolf"))
        val flashcards = FlashcardService(FlashcardRepositoryImpl(env.provider), env.terms, env.settings)
        env.settings.update { it.copy(flashcardNewPerDay = 7, flashcardLearnSteps = "5m") }
        flashcards.answer(flashcards.next(language).card!!, Rating.EASY)
        val answered = flashcards.cardFor(lobo)!!
        assertEquals(TermStatus.LEARNING_3, env.terms.getById(lobo)!!.status)
        val backup = env.backups.create()

        // After the backup the card is started over and the settings change.
        flashcards.restart(lobo)
        env.settings.update { it.copy(flashcardNewPerDay = 20, flashcardLearnSteps = "1m 10m") }

        env.backups.restore(backup.name)
        assertEquals(answered, flashcards.cardFor(lobo))
        assertEquals(7, env.settings.current.flashcardNewPerDay)
        assertEquals("5m", env.settings.current.flashcardLearnSteps)
        // The answer given before the backup still counts against the day's new cards.
        assertEquals(0, flashcards.counts(language).total)
    }

    @Test
    fun restoringABackupBringsBackTheDataAndSettings() = runBlocking<Unit> {
        val env = Env()
        val language = env.languages.save(Language(name = "Portuguese"))
        val book = BookService(env.books, env.languages).create(BookDraft(languageId = language, title = "Lobo d'água", text = "O lobo d'água dorme."))
        val lobo = env.service.save(env.service.findOrNew(language, "lobo").copy(status = TermStatus.NEW_1, statusExplicitlySet = true, translation = "wolf"), sentence = "O lobo d'água dorme.")
        env.settings.update { it.copy(currentLanguageId = language, readingFontScale = 1.3f, themeId = "sepia", googleTranslateApiKey = "secret-key", argosPython = "/usr/bin/python3") }

        val backup = env.backups.create()
        assertEquals(LocalDateTime(2026, 10, 3, 12, 0, 0), backup.createdAt)
        assertEquals(listOf(backup.name), env.backups.list().map { it.name })
        val file = env.backups.read(backup.name)
        assertFalse(file.decodeToString().contains("secret-key"), "API keys stay out of backups")
        assertFalse(file.decodeToString().contains("/usr/bin/python3"), "the Python path stays out of backups")

        // Everything changes after the backup.
        env.books.deleteBook(book)
        env.service.save(env.service.findOrNew(language, "gato").copy(status = TermStatus.NEW_2, statusExplicitlySet = true))
        env.service.setStatus(listOf(lobo), TermStatus.WELL_KNOWN)
        env.settings.update { it.copy(readingFontScale = 2f, themeId = "default", googleTranslateApiKey = "new-key", argosPython = "/opt/python") }

        val undo = env.backups.restore(backup.name)
        assertEquals(listOf(undo.name, backup.name), env.backups.list().map { it.name }, "the data before the restore is backed up first, newest first")

        assertEquals(listOf("Lobo d'água"), env.books.getBooks().map { it.title })
        assertEquals("O lobo d'água dorme.", env.books.getPages(book).single().text)
        val restored = env.terms.getById(lobo)!!
        assertEquals(TermStatus.NEW_1, restored.status)
        assertEquals("wolf", restored.translation)
        assertEquals("O lobo d'água dorme.", restored.sentence)
        assertNull(env.terms.findByTextLc(language, "gato"))
        assertEquals(1.3f, env.settings.current.readingFontScale)
        assertEquals("sepia", env.settings.current.themeId)
        assertEquals(language, env.settings.current.currentLanguageId)
        assertEquals("new-key", env.settings.current.googleTranslateApiKey, "this device's API key is kept")
        assertEquals("/opt/python", env.settings.current.argosPython, "this device's Python path is kept")

        // New rows still get ids of their own.
        val gato = env.service.save(env.service.findOrNew(language, "gato").copy(status = TermStatus.NEW_1, statusExplicitlySet = true))
        assertTrue(gato > lobo)

        // Restoring the automatic backup undoes the restore.
        env.backups.restore(undo.name)
        assertTrue(env.books.getBooks().isEmpty())
        assertEquals(TermStatus.NEW_2, env.terms.findByTextLc(language, "gato")?.status)
        assertEquals(2f, env.settings.current.readingFontScale)
    }

    @Test
    fun aBackupFileSavedElsewhereCanBeImportedAndRestored() = runBlocking<Unit> {
        val source = Env()
        val language = source.languages.save(Language(name = "Czech"))
        source.service.save(source.service.findOrNew(language, "kočka").copy(status = TermStatus.LEARNING_3, statusExplicitlySet = true, translation = "cat"))
        val file = source.backups.read(source.backups.create().name)

        val other = Env()
        val imported = other.backups.import(file)
        assertEquals(LocalDateTime(2026, 10, 3, 12, 0, 0), imported.createdAt, "an imported backup keeps the time it was made")
        assertEquals(listOf(imported.name), other.backups.list().map { it.name })
        other.backups.restore(imported.name)
        val czech = other.languages.getAll().single { it.name == "Czech" }
        assertEquals("cat", other.terms.findByTextLc(czech.id, "kočka")?.translation)
    }

    @Test
    fun filesThatAreNotBackupsAreRefused() = runBlocking<Unit> {
        val env = Env()
        assertFailsWith<BackupException> { env.backups.import("not a database".encodeToByteArray()) }
        assertFailsWith<BackupException> { env.backups.import(ByteArray(0)) }

        // An ordinary SQLite database is not a backup either.
        val plain = File.createTempFile("plain", ".db")
        DriverManager.getConnection("jdbc:sqlite:${plain.absolutePath}").use { it.createStatement().execute("CREATE TABLE t (a INTEGER)") }
        assertFailsWith<BackupException> { env.backups.import(plain.readBytes()) }
        assertTrue(env.backups.list().isEmpty())
    }

    @Test
    fun backupsFromNewerVersionsAreRefusedAndOlderOnesRestored() = runBlocking<Unit> {
        val env = Env()
        val language = env.languages.save(Language(name = "Polish"))
        env.service.save(env.service.findOrNew(language, "kot").copy(status = TermStatus.NEW_1, statusExplicitlySet = true, translation = "cat"))
        val file = env.backups.read(env.backups.create().name)

        val newer = edited(file, "UPDATE backup_info SET value = '999' WHERE key = 'schema_version'")
        val error = assertFailsWith<BackupException> { env.backups.import(newer) }
        assertTrue(error.message!!.contains("newer version"))

        // A backup made before a column existed restores the columns it has.
        val older = edited(file, "ALTER TABLE terms DROP COLUMN anki_exported_at", "UPDATE backup_info SET value = '8' WHERE key = 'schema_version'", "UPDATE backup_info SET value = '0' WHERE key = 'created_at'")
        val imported = env.backups.import(older)
        env.backups.restore(imported.name)
        val kot = assertNotNull(env.terms.findByTextLc(language, "kot"))
        assertEquals("cat", kot.translation)
    }

    private fun edited(file: ByteArray, vararg statements: String): ByteArray {
        val copy = File.createTempFile("tayra-edited", ".sqlite")
        copy.writeBytes(file)
        DriverManager.getConnection("jdbc:sqlite:${copy.absolutePath}").use { connection ->
            connection.createStatement().use { statement -> statements.forEach { statement.execute(it) } }
        }
        return copy.readBytes().also { copy.delete() }
    }
}
