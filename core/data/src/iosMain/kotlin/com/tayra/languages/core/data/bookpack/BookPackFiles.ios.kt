package com.tayra.languages.core.data.bookpack

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.tayra.languages.core.data.db.atFormat
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.gunzip
import com.tayra.languages.core.domain.books.BookPack
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
actual class BookPackFiles(private val downloader: DictionaryDownloader) {
    private val directory: String by lazy {
        val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
        "$support/book-packs"
    }

    private fun fileName(pack: BookPack) = "${pack.id}.sqlite"
    private fun path(pack: BookPack) = "$directory/${fileName(pack)}"
    private fun stampPath(pack: BookPack) = "${path(pack)}.format"

    actual suspend fun installedSize(pack: BookPack): Long? {
        val stamp = NSString.stringWithContentsOfFile(stampPath(pack), encoding = NSUTF8StringEncoding, error = null)
        if (stamp?.trim() != BookPack.FORMAT.toString()) return null
        val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(path(pack), error = null) ?: return null
        return (attributes["NSFileSize"] as? Number)?.toLong()
    }

    actual suspend fun install(pack: BookPack, onProgress: (Float?) -> Unit) {
        val bytes = gunzip(downloader.download(pack.url, onProgress))
        NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
        val data = bytes.usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong()) }
        check(data.writeToFile(path(pack), atomically = true)) { "Could not write ${path(pack)}" }
        (BookPack.FORMAT.toString() as NSString).writeToFile(stampPath(pack), atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    actual suspend fun remove(pack: BookPack) {
        NSFileManager.defaultManager.removeItemAtPath(stampPath(pack), error = null)
        NSFileManager.defaultManager.removeItemAtPath(path(pack), error = null)
    }

    actual suspend fun openDriver(pack: BookPack): SqlDriver? {
        if (installedSize(pack) == null) return null
        return NativeSqliteDriver(
            schema = BookPackDatabase.Schema.synchronous().atFormat(BookPack.FORMAT),
            name = fileName(pack),
            onConfiguration = { config: DatabaseConfiguration ->
                config.copy(extendedConfig = DatabaseConfiguration.Extended(basePath = directory))
            },
        )
    }
}
