package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.BookService
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A library from before sync is given what sync keeps, and comes out with the same tables, columns,
 * indexes and triggers as a new one; what was there counts as changed when it last visibly was.
 */
class SyncMigrationTest {

    private fun <T> sql(file: File, block: Connection.() -> T): T = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use(block)
    private fun Connection.strings(query: String): List<String> = createStatement().executeQuery(query).use { r ->
        buildList { while (r.next()) add((1..r.metaData.columnCount).joinToString("|") { r.getString(it) ?: "" }) }
    }

    /** Each table's columns in order with their types and defaults, and the names of the indexes and triggers. */
    private fun Connection.shape(): List<String> {
        val tables = strings("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name")
        return tables.flatMap { table -> strings("PRAGMA table_info($table)").map { "$table: $it" } } +
            strings("SELECT type || ' ' || name || ' on ' || tbl_name FROM sqlite_master WHERE type IN ('index', 'trigger') AND name NOT LIKE 'sqlite_%' ORDER BY type, name")
    }

    @Test
    fun aLibraryFromBeforeSyncIsGivenWhatSyncKeeps() = runBlocking<Unit> {
        val fresh = File.createTempFile("tayra-sync-fresh", ".db").also { it.delete() }
        DatabaseProvider(DatabaseDriverFactory(fresh)).database()
        val freshShape = sql(fresh) { shape() }

        // A library as version 27 left it: no sync columns, tables or triggers.
        val file = File.createTempFile("tayra-sync-27", ".db").also { it.delete() }
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val pt = LanguageRepositoryImpl(provider).save(Language(name = "Portuguese"))
        val terms = TermRepositoryImpl(provider)
        val lobo = terms.save(Term(languageId = pt, text = "lobo", textLc = "lobo", status = TermStatus.NEW_1))
        val books = BookRepositoryImpl(provider)
        BookService(books, LanguageRepositoryImpl(provider)).create(BookDraft(languageId = pt, title = "Lobo", text = "O lobo dorme."))
        BookService(books, LanguageRepositoryImpl(provider)).create(BookDraft(languageId = pt, title = "Gato", text = "O gato dorme."))
        sql(file) {
            createStatement().execute("UPDATE terms SET created_at = 1000, status_changed_at = 2000 WHERE id = $lobo")
            pretendVersion(27)
        }

        DatabaseProvider(DatabaseDriverFactory(file)).database()
        assertEquals(freshShape, sql(file) { shape() }, "a migrated library is shaped as a new one")
        sql(file) {
            assertEquals(listOf("2000"), strings("SELECT updated_at FROM terms WHERE id = $lobo"), "a word counts as changed when its status last was")
            val ids = strings("SELECT sync_id FROM books")
            assertEquals(2, ids.toSet().size)
            assertTrue(ids.all { it.matches(Regex("[0-9a-f]{32}")) }, ids.toString())
            assertEquals(listOf("0|1|"), strings("SELECT last_seq, full_push, last_push_at FROM sync_state"), "the first sync sends everything")
            assertEquals(listOf("47"), strings("SELECT count(*) FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'sync_%'"))
            // And the triggers work in it.
            createStatement().execute("PRAGMA foreign_keys = ON")
            createStatement().execute("DELETE FROM terms WHERE id = $lobo")
            assertEquals(listOf("term|Portuguese|lobo"), strings("SELECT kind, scope, key FROM sync_tombstones"))
        }
    }
}
