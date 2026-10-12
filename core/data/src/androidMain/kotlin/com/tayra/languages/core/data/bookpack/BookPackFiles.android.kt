package com.tayra.languages.core.data.bookpack

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.tayra.languages.core.data.db.atFormat
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.PackFiles
import com.tayra.languages.core.domain.books.BookPack
import java.io.File

actual class BookPackFiles(private val context: Context, downloader: DictionaryDownloader) {
    private val files = PackFiles(File(context.filesDir, "book-packs"), downloader)

    actual suspend fun installedSize(pack: BookPack): Long? = files.installedSize(pack.id, BookPack.FORMAT)

    actual suspend fun install(pack: BookPack, onProgress: (Float?) -> Unit) = files.install(pack.id, pack.url, BookPack.FORMAT, onProgress)

    actual suspend fun remove(pack: BookPack) = files.remove(pack.id)

    actual suspend fun openDriver(pack: BookPack): SqlDriver? {
        if (files.installedSize(pack.id, BookPack.FORMAT) == null) return null
        return AndroidSqliteDriver(schema = BookPackDatabase.Schema.synchronous().atFormat(BookPack.FORMAT), context = context, name = files.file(pack.id).absolutePath)
    }
}
