package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.backup.BackupFiles
import com.tayra.languages.core.data.backup.BackupRepositoryImpl
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.DatabaseMaintenanceImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.BookService
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Every write the app makes to a synced table stamps the record it belongs to, and each deletion of
 * a record other devices may have leaves a tombstone, whatever code made the write; writes made
 * while a restore, a wipe or a pull is applied are not this device's changes.
 */
class SyncTrackingTest {
    private val file = File.createTempFile("tayra-sync", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)

    /** A connection of its own, as another writer would have, with references enforced. */
    private fun <T> sql(block: Connection.() -> T): T =
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}", Properties().apply { put("foreign_keys", "true") }).use(block)

    private fun Connection.long(query: String): Long = createStatement().executeQuery(query).use { it.next(); it.getLong(1) }
    private fun Connection.strings(query: String): List<String> = createStatement().executeQuery(query).use { r ->
        buildList { while (r.next()) add((1..r.metaData.columnCount).joinToString("|") { r.getString(it) ?: "" }) }
    }
    private fun Connection.run(statements: String) { statements.split(";").filter { it.isNotBlank() }.forEach { createStatement().execute(it) } }

    private fun tombstones() = sql { strings("SELECT kind, scope, key FROM sync_tombstones ORDER BY kind, scope, key") }

    private suspend fun portuguese() = languages.save(Language(name = "Portuguese"))

    @Test
    fun aSavedWordIsStampedAndOnlyItsOwnChangesStampItAgain() = runBlocking<Unit> {
        val pt = portuguese()
        val before = System.currentTimeMillis()
        val lobo = terms.save(Term(languageId = pt, text = "lobo", textLc = "lobo", status = TermStatus.NEW_1))
        val stamped = sql { long("SELECT updated_at FROM terms WHERE id = $lobo") }
        assertTrue(stamped in before..System.currentTimeMillis() + 1, "stamped when saved, in epoch milliseconds: $stamped")

        sql { run("UPDATE terms SET updated_at = 0 WHERE id = $lobo") }
        sql { run("UPDATE terms SET anki_exported_at = 1 WHERE id = $lobo") }
        assertEquals(0, sql { long("SELECT updated_at FROM terms WHERE id = $lobo") }, "sending a word to this device's Anki is not a change to sync")

        sql { run("UPDATE terms SET status_changed_at = 1 WHERE id = $lobo") }
        terms.save(terms.getById(lobo)!!.copy(status = TermStatus.NEW_2, translation = "wolf"))
        assertTrue(sql { long("SELECT updated_at FROM terms WHERE id = $lobo") } >= stamped, "a new status stamps it again")
        assertTrue(sql { long("SELECT status_changed_at FROM terms WHERE id = $lobo") } >= stamped, "and says when the status was set, which sync merges by")
    }

    @Test
    fun theWordsThingsStampTheWord() = runBlocking<Unit> {
        val pt = portuguese()
        val casa = terms.save(Term(languageId = pt, text = "casa", textLc = "casa", status = TermStatus.NEW_1))
        val casas = terms.save(Term(languageId = pt, text = "casas", textLc = "casas", status = TermStatus.NEW_1))
        fun reset() = sql { run("UPDATE terms SET updated_at = 0") }
        fun stamp(id: Long) = sql { long("SELECT updated_at FROM terms WHERE id = $id") }

        reset()
        sql { run("INSERT INTO term_parents(term_id, parent_id) VALUES ($casas, $casa)") }
        assertTrue(stamp(casas) > 0, "a word's parents are part of its record")
        assertEquals(0, stamp(casa))
        reset()
        sql { run("INSERT INTO level_terms(term_id) VALUES ($casa)") }
        assertTrue(stamp(casa) > 0, "so is being known through the vocabulary level")

        sql { run("INSERT INTO flashcards(term_id, due, seen_status) VALUES ($casa, 0, 1)") }
        assertTrue(sql { long("SELECT updated_at FROM flashcards WHERE term_id = $casa") } > 0)
        sql { run("UPDATE flashcards SET updated_at = 0; UPDATE flashcards SET reps = 3 WHERE term_id = $casa") }
        assertTrue(sql { long("SELECT updated_at FROM flashcards WHERE term_id = $casa") } > 0, "a review stamps the card")
    }

    @Test
    fun deletionsLeaveOneTombstoneForWhatTheyRemoved() = runBlocking<Unit> {
        val pt = portuguese()
        val lobo = terms.save(Term(languageId = pt, text = "Lobo", textLc = "lobo", status = TermStatus.NEW_1))
        val gato = terms.save(Term(languageId = pt, text = "gato", textLc = "gato", status = TermStatus.NEW_1))
        val unseen = terms.save(Term(languageId = pt, text = "de", textLc = "de", status = TermStatus.UNKNOWN))
        sql { run("INSERT INTO flashcards(term_id, due, seen_status) VALUES ($lobo, 0, 1), ($gato, 0, 1)") }

        sql { run("DELETE FROM flashcards WHERE term_id = $gato") }
        assertEquals(listOf("flashcard|Portuguese|gato"), tombstones(), "a card removed on its own")

        sql { run("DELETE FROM sync_tombstones; DELETE FROM terms WHERE id IN ($lobo, $unseen)") }
        assertEquals(listOf("term|Portuguese|lobo"), tombstones(), "the word's card goes with it; a word never saved leaves nothing")

        sql { run("DELETE FROM sync_tombstones; DELETE FROM languages WHERE id = $pt") }
        assertEquals(listOf("language|Portuguese|"), tombstones(), "removing a language is one record, not one per word")
    }

    @Test
    fun booksAndPlaylistsHaveTheSameIdOnEveryDevice() = runBlocking<Unit> {
        val pt = portuguese()
        val service = BookService(books, languages)
        val first = service.create(BookDraft(languageId = pt, title = "Lobo", text = "O lobo dorme.\n\nO gato come."))
        val second = service.create(BookDraft(languageId = pt, title = "Gato", text = "O gato dorme."))
        val ids = sql { strings("SELECT sync_id FROM books ORDER BY id") }
        assertEquals(2, ids.toSet().size)
        assertTrue(ids.all { it.matches(Regex("[0-9a-f]{32}")) }, ids.toString())

        sql { run("UPDATE books SET updated_at = 0; UPDATE pages SET updated_at = 0; UPDATE pages SET read_date = 5 WHERE book_id = $first") }
        assertEquals(0, sql { long("SELECT updated_at FROM books WHERE id = $first") })
        assertTrue(sql { long("SELECT min(updated_at) FROM pages WHERE book_id = $first AND read_date = 5") } > 0, "reading a page stamps the page")
        sql { run("INSERT INTO book_tags(text) VALUES ('a2'); INSERT INTO book_tag_map(book_id, tag_id) VALUES ($first, 1)") }
        assertTrue(sql { long("SELECT updated_at FROM books WHERE id = $first") } > 0, "a book's tags are part of its record")

        sql { run("INSERT INTO playlists(language_id, name, created_at) VALUES ($pt, 'Evening', 1)") }
        val playlist = sql { strings("SELECT sync_id, updated_at > 0 FROM playlists") }.single()
        assertTrue(playlist.matches(Regex("[0-9a-f]{32}\\|1")), playlist)
        sql { run("UPDATE playlists SET updated_at = 0; INSERT INTO playlist_items(playlist_id, book_id, position) VALUES (1, $second, 0)") }
        assertTrue(sql { long("SELECT updated_at FROM playlists") } > 0, "a playlist's texts are part of its record")

        sql { run("DELETE FROM books WHERE id = $first") }
        assertEquals(listOf("book||${ids.first()}"), tombstones(), "the book's pages go with it")
    }

    @Test
    fun writesAppliedFromElsewhereAreNotThisDevicesChanges() = runBlocking<Unit> {
        val pt = portuguese()
        sql {
            autoCommit = false
            run("UPDATE sync_state SET applying = 1")
            run("INSERT INTO terms(language_id, text, text_lc, status, created_at, status_changed_at, updated_at) VALUES ($pt, 'mar', 'mar', 1, 1, 1, 42)")
            run("INSERT INTO books(language_id, title) VALUES ($pt, 'Mar')")
            run("DELETE FROM terms WHERE text_lc = 'mar'")
            run("UPDATE sync_state SET applying = 0")
            commit()
        }
        assertEquals(emptyList(), tombstones())
        val book = sql { strings("SELECT sync_id IS NOT NULL, updated_at FROM books") }.single()
        assertEquals("1|0", book, "a book that came without an id still gets one, but no stamp")
    }

    @Test
    fun aRestoreAndAWipeStartSyncOver() = runBlocking<Unit> {
        val pt = portuguese()
        val lobo = terms.save(Term(languageId = pt, text = "lobo", textLc = "lobo", status = TermStatus.NEW_1))
        BookService(books, languages).create(BookDraft(languageId = pt, title = "Lobo", text = "O lobo dorme."))
        val device = sql { strings("SELECT device_id FROM sync_state") }.single()
        sql { run("UPDATE sync_state SET last_seq = 99, last_push_at = 5, full_push = 0") }
        val backups = BackupRepositoryImpl(provider, SettingsRepositoryImpl(MapSettings()), BackupFiles(Files.createTempDirectory("tayra-sync-backups").toFile()))
        val backup = backups.create()
        sql { run("DELETE FROM terms WHERE id = $lobo") }
        assertEquals(1, tombstones().size)
        val bookId = sql { strings("SELECT sync_id FROM books") }.single()

        backups.restore(backup.name)
        assertEquals(emptyList(), tombstones(), "the restore's deletions and the ones before it are not sent")
        assertEquals("$device|0||1", sql { strings("SELECT device_id, last_seq, last_push_at, full_push FROM sync_state") }.single(), "same device, everything sent and taken afresh")
        assertEquals(bookId, sql { strings("SELECT sync_id FROM books") }.single(), "the book keeps its id")
        assertNotEquals(0, sql { long("SELECT updated_at FROM terms WHERE text_lc = 'lobo'") }, "rows keep the times they had")

        sql { run("UPDATE sync_state SET last_seq = 7, full_push = 0") }
        DatabaseMaintenanceImpl(provider).wipeAllData()
        assertEquals(emptyList(), tombstones(), "emptying this device does not empty the account")
        assertEquals("0|1", sql { strings("SELECT last_seq, full_push FROM sync_state") }.single())
    }
}
