package com.tayra.languages.core.data.dictionary

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.tayra.languages.core.data.db.atFormat
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
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
actual class DictionaryPackStorage(private val downloader: DictionaryDownloader) : DictionaryPackStore {
    private val directory: String by lazy {
        val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
        "$support/dictionaries"
    }

    private fun fileName(pack: DictionaryPack) = "${pack.id.name}.sqlite"
    private fun path(pack: DictionaryPack) = "$directory/${fileName(pack)}"
    private fun stampPath(pack: DictionaryPack) = "${path(pack)}.format"

    actual override suspend fun installedSize(pack: DictionaryPack): Long? {
        val manager = NSFileManager.defaultManager
        val stamp = NSString.stringWithContentsOfFile(stampPath(pack), encoding = NSUTF8StringEncoding, error = null)
        if (stamp?.trim() != DictionaryId.FORMAT.toString()) return null
        val attributes = manager.attributesOfItemAtPath(path(pack), error = null) ?: return null
        return (attributes["NSFileSize"] as? Number)?.toLong()
    }

    actual override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) {
        val bytes = gunzip(downloader.download(pack.url, onProgress))
        NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
        val data = bytes.usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong()) }
        check(data.writeToFile(path(pack), atomically = true)) { "Could not write ${path(pack)}" }
        (DictionaryId.FORMAT.toString() as NSString).writeToFile(stampPath(pack), atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    actual override suspend fun remove(pack: DictionaryPack) {
        val manager = NSFileManager.defaultManager
        manager.removeItemAtPath(stampPath(pack), error = null)
        manager.removeItemAtPath(path(pack), error = null)
    }

    actual suspend fun openDriver(pack: DictionaryPack): SqlDriver? {
        if (installedSize(pack) == null) return null
        return NativeSqliteDriver(
            schema = DictionaryDatabase.Schema.synchronous().atFormat(DictionaryId.FORMAT),
            name = fileName(pack),
            onConfiguration = { config: DatabaseConfiguration ->
                config.copy(extendedConfig = DatabaseConfiguration.Extended(basePath = directory))
            },
        )
    }
}
