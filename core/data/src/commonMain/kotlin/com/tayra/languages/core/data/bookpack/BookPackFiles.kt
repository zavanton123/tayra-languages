package com.tayra.languages.core.data.bookpack

import app.cash.sqldelight.db.SqlDriver
import com.tayra.languages.core.domain.books.BookPack

/** Platform storage for downloaded book packs, which also opens them as read-only SQLite drivers. */
expect class BookPackFiles {
    /** Size of the installed pack in bytes, or null when it is not installed. */
    suspend fun installedSize(pack: BookPack): Long?
    suspend fun install(pack: BookPack, onProgress: (Float?) -> Unit)
    suspend fun remove(pack: BookPack)
    /** A driver over the installed pack, or null when it is not installed. */
    suspend fun openDriver(pack: BookPack): SqlDriver?
}
