package com.tayra.languages.core.data.bookpack

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import co.touchlab.kermit.Logger
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.core.domain.books.BookPack
import com.tayra.languages.core.domain.books.BookPackStore
import com.tayra.languages.core.domain.books.PackedBook
import kotlinx.coroutines.withContext

/** Reads the books of the installed packs; a file of another format reads as empty. */
class BookPackRepository(private val files: BookPackFiles) : BookPackStore {
    override suspend fun installedSize(pack: BookPack): Long? = files.installedSize(pack)

    override suspend fun install(pack: BookPack, onProgress: (Float?) -> Unit) = files.install(pack, onProgress)

    override suspend fun remove(pack: BookPack) = files.remove(pack)

    override suspend fun books(pack: BookPack): List<PackedBook> = read(pack) { database ->
        database.bookPackQueries.selectBooks().awaitAsList().map { row ->
            PackedBook(
                id = row.id,
                level = row.level,
                title = row.title,
                author = row.author,
                summary = row.summary,
                sourceUrl = row.source_url,
                license = row.license,
                tags = row.tags.split(' ').filter { it.isNotBlank() },
                text = row.text,
            )
        }
    }.orEmpty()

    private suspend fun <T> read(pack: BookPack, block: suspend (BookPackDatabase) -> T): T? = withContext(databaseDispatcher) {
        val driver = try {
            files.openDriver(pack)
        } catch (e: Exception) {
            Logger.w(e) { "Could not open book pack ${pack.id}" }
            null
        } ?: return@withContext null
        try {
            val database = BookPackDatabase(driver)
            val format = database.bookPackQueries.selectMeta("format").awaitAsOneOrNull()?.toIntOrNull()
            if (format == BookPack.FORMAT) {
                block(database)
            } else {
                Logger.w { "Book pack ${pack.id} has format $format, expected ${BookPack.FORMAT}" }
                null
            }
        } catch (e: Exception) {
            Logger.w(e) { "Book pack ${pack.id} is not readable" }
            null
        } finally {
            driver.close()
        }
    }
}
