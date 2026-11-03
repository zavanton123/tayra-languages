package com.tayra.languages.core.data

import java.sql.Connection

/**
 * Makes a library of the current shape pass for one [version] left, for tests of the migrations
 * after it: what migration 27 adds (sync's columns, tables, indexes and triggers) is taken out of
 * libraries older than 28, which never had it, so the migration can add it again; so are later migrations' additions.
 */
internal fun Connection.pretendVersion(version: Int) {
    if (version <= 28) createStatement().execute("DROP TABLE IF EXISTS sync_pending")
    if (version <= 27) removeSyncTracking()
    // After the sync triggers go, as a test may have dropped tables they name, or the flashcards themselves.
    if (version <= 29 && createStatement().executeQuery("SELECT count(*) FROM pragma_table_info('flashcards') WHERE name = 'created_at'").use { it.getInt(1) } > 0) {
        createStatement().execute("ALTER TABLE flashcards DROP COLUMN created_at")
    }
    if (version <= 34 && createStatement().executeQuery("SELECT count(*) FROM pragma_table_info('flashcard_reviews') WHERE name = 'taken_ms'").use { it.getInt(1) } > 0) {
        createStatement().execute("ALTER TABLE flashcard_reviews DROP COLUMN taken_ms")
    }
    if (version <= 33) {
        createStatement().execute("DROP TRIGGER IF EXISTS sync_fsrs_parameters_insert")
        createStatement().execute("DROP TRIGGER IF EXISTS sync_fsrs_parameters_delete")
        createStatement().execute("DROP TABLE IF EXISTS fsrs_parameters")
    }
    if (version <= 32 && createStatement().executeQuery("SELECT count(*) FROM pragma_table_info('flashcard_reviews') WHERE name = 'answered_by'").use { it.getInt(1) } > 0) {
        createStatement().execute("ALTER TABLE flashcard_reviews DROP COLUMN answered_by")
    }
    if (version <= 31 && createStatement().executeQuery("SELECT count(*) FROM pragma_table_info('terms') WHERE name = 'sentence_translation'").use { it.getInt(1) } > 0) {
        createStatement().execute("DROP TRIGGER IF EXISTS terms_sentence_translation_stale")
        if (version >= 28) {
            createStatement().execute("DROP TRIGGER IF EXISTS sync_terms_update")
            createStatement().execute(
                """
                CREATE TRIGGER sync_terms_update
                AFTER UPDATE OF text, text_lc, status, translation, romanization, token_count, flash_message, translation_language, sentence, card_form ON terms
                WHEN (SELECT applying FROM sync_state WHERE id = 1) IS NOT 1
                BEGIN
                    UPDATE terms SET updated_at = CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER) WHERE id = new.id;
                END
                """.trimIndent(),
            )
        }
        createStatement().execute("ALTER TABLE terms DROP COLUMN sentence_translation")
    }
    if (version <= 30 && createStatement().executeQuery("SELECT count(*) FROM pragma_table_info('terms') WHERE name = 'card_form'").use { it.getInt(1) } > 0) {
        // The trigger names the column, so it goes back to the one before it first.
        if (version >= 28) {
            createStatement().execute("DROP TRIGGER IF EXISTS sync_terms_update")
            createStatement().execute(
                """
                CREATE TRIGGER sync_terms_update
                AFTER UPDATE OF text, text_lc, status, translation, romanization, token_count, flash_message, translation_language, sentence ON terms
                WHEN (SELECT applying FROM sync_state WHERE id = 1) IS NOT 1
                BEGIN
                    UPDATE terms SET updated_at = CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER) WHERE id = new.id;
                END
                """.trimIndent(),
            )
        }
        createStatement().execute("ALTER TABLE terms DROP COLUMN card_form")
    }
    createStatement().execute("PRAGMA user_version = $version")
}

private fun Connection.removeSyncTracking() {
    fun names(query: String) = createStatement().executeQuery(query).use { r -> buildList { while (r.next()) add(r.getString(1)) } }
    for (trigger in names("SELECT name FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'sync_%'")) createStatement().execute("DROP TRIGGER $trigger")
    for (index in listOf("terms_updated_at", "pages_updated_at", "books_sync_id", "playlists_sync_id")) createStatement().execute("DROP INDEX IF EXISTS $index")
    createStatement().execute("DROP TABLE IF EXISTS sync_state")
    createStatement().execute("DROP TABLE IF EXISTS sync_tombstones")
    val added = listOf("terms", "flashcards", "flashcard_reviews", "practice_progress", "practice_examples", "vocabulary_levels", "languages", "books", "pages", "page_bookmarks", "words_read", "playlists")
        .map { it to "updated_at" } + listOf("books" to "sync_id", "playlists" to "sync_id")
    // A test may have dropped a table already, to look older still.
    for ((table, column) in added) {
        if (column in names("SELECT name FROM pragma_table_info('$table')")) createStatement().execute("ALTER TABLE $table DROP COLUMN $column")
    }
}
