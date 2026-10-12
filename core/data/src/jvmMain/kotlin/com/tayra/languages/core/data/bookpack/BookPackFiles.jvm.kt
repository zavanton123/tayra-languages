package com.tayra.languages.core.data.bookpack

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.PackFiles
import com.tayra.languages.core.domain.books.BookPack
import java.io.File
import java.util.Properties

actual class BookPackFiles(
    downloader: DictionaryDownloader,
    directory: File = File(DatabaseDriverFactory.dataDirectory(), "book-packs"),
) {
    private val files = PackFiles(directory, downloader)

    actual suspend fun installedSize(pack: BookPack): Long? = files.installedSize(pack.id, BookPack.FORMAT)

    actual suspend fun install(pack: BookPack, onProgress: (Float?) -> Unit) = files.install(pack.id, pack.url, BookPack.FORMAT, onProgress)

    actual suspend fun remove(pack: BookPack) = files.remove(pack.id)

    actual suspend fun openDriver(pack: BookPack): SqlDriver? {
        if (files.installedSize(pack.id, BookPack.FORMAT) == null) return null
        val properties = Properties().apply { put("open_mode", "1") } // SQLITE_OPEN_READONLY
        return JdbcSqliteDriver("jdbc:sqlite:${files.file(pack.id).absolutePath}", properties)
    }
}
