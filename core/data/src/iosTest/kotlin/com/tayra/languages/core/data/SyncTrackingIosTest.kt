package com.tayra.languages.core.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseFileContext
import com.tayra.languages.core.data.db.TayraDatabase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Sync's triggers and migration on iOS's own SQLite, through the driver the app uses: a new library
 * tracks changes, and a library from version 27 is given what sync keeps.
 */
class SyncTrackingIosTest {
    private val name = "tayra-sync-ios-test.db"

    private fun open(): SqlDriver = NativeSqliteDriver(
        schema = TayraDatabase.Schema.synchronous(),
        name = name,
        onConfiguration = { config: DatabaseConfiguration -> config.copy(extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true)) },
    )

    @AfterTest
    fun delete() = DatabaseFileContext.deleteDatabase(name)

    private fun SqlDriver.run(statements: String) = statements.split(";").filter { it.isNotBlank() }.forEach { execute(null, it, 0) }

    private fun SqlDriver.strings(query: String): List<String> = executeQuery(null, query, { cursor ->
        val rows = mutableListOf<String>()
        while (cursor.next().value) rows += cursor.getString(0) ?: ""
        QueryResult.Value(rows)
    }, 0).value

    private fun SqlDriver.addWords() = run(
        "INSERT INTO languages(name, character_substitutions, regexp_split_sentences, exceptions_split_sentences, word_characters) VALUES ('Portuguese', '', '', '', '');" +
            "INSERT INTO terms(language_id, text, text_lc, status, created_at, status_changed_at) VALUES (1, 'lobo', 'lobo', 1, 1000, 2000);" +
            "INSERT INTO books(language_id, title) VALUES (1, 'Lobo')",
    )

    @Test
    fun aNewLibraryTracksChanges() {
        DatabaseFileContext.deleteDatabase(name)
        val driver = open()
        driver.addWords()
        assertTrue(driver.strings("SELECT updated_at FROM terms").single().toLong() > 1_700_000_000_000, "stamped in epoch milliseconds")
        assertTrue(driver.strings("SELECT sync_id FROM books").single().matches(Regex("[0-9a-f]{32}")))
        driver.run("DELETE FROM terms WHERE text_lc = 'lobo'")
        assertEquals(listOf("term Portuguese lobo"), driver.strings("SELECT kind || ' ' || scope || ' ' || key FROM sync_tombstones"))
        driver.run("DELETE FROM sync_tombstones; DELETE FROM languages")
        assertEquals(listOf("language Portuguese "), driver.strings("SELECT kind || ' ' || scope || ' ' || key FROM sync_tombstones"), "one record for the language, none for its book")
        driver.close()
    }

    @Test
    fun aLibraryFromVersion27IsGivenWhatSyncKeeps() {
        DatabaseFileContext.deleteDatabase(name)
        val fresh = open()
        val freshShape = fresh.strings("SELECT type || ' ' || name FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type, name")
        fresh.addWords()
        // Back to how version 27 left it.
        val triggers = fresh.strings("SELECT name FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'sync_%'")
        fresh.run(
            triggers.joinToString(";") { "DROP TRIGGER $it" } + ";" +
                "DROP INDEX terms_updated_at; DROP INDEX pages_updated_at; DROP INDEX books_sync_id; DROP INDEX playlists_sync_id;" +
                "DROP TABLE sync_state; DROP TABLE sync_tombstones; DROP TABLE sync_pending;" +
                listOf("terms", "flashcards", "flashcard_reviews", "practice_progress", "practice_examples", "vocabulary_levels", "languages", "books", "pages", "page_bookmarks", "words_read", "playlists")
                    .joinToString(";") { "ALTER TABLE $it DROP COLUMN updated_at" } + ";" +
                "ALTER TABLE books DROP COLUMN sync_id; ALTER TABLE playlists DROP COLUMN sync_id; PRAGMA user_version = 27",
        )
        fresh.close()

        val migrated = open()
        assertEquals(freshShape, migrated.strings("SELECT type || ' ' || name FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type, name"))
        assertEquals(listOf("2000"), migrated.strings("SELECT updated_at FROM terms"), "a word counts as changed when its status last was")
        assertTrue(migrated.strings("SELECT sync_id FROM books").single().matches(Regex("[0-9a-f]{32}")))
        assertEquals(listOf("29"), migrated.strings("PRAGMA user_version"))
        migrated.run("DELETE FROM terms")
        assertEquals(listOf("lobo"), migrated.strings("SELECT key FROM sync_tombstones"))
        migrated.close()
    }
}
