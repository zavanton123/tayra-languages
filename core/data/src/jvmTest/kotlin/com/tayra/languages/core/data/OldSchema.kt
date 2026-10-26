package com.tayra.languages.core.data

import java.sql.Connection

/**
 * Makes a library of the current shape pass for one [version] left, for tests of the migrations
 * after it: what migration 27 adds (sync's columns, tables, indexes and triggers) is taken out of
 * libraries older than 28, which never had it, so the migration can add it again.
 */
internal fun Connection.pretendVersion(version: Int) {
    if (version <= 27) removeSyncTracking()
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
