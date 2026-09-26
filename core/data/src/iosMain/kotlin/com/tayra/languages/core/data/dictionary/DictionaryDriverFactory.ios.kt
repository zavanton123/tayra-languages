package com.tayra.languages.core.data.dictionary

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryId
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

actual class DictionaryDriverFactory {
    private val directory: String by lazy {
        val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
        "$support/dictionaries"
    }

    actual suspend fun open(dictionary: DictionaryId, assets: DictionaryAssets): SqlDriver? {
        val name = "${dictionary.name}.sqlite"
        if (!ensureFile("$directory/$name", dictionary, assets)) return null
        return NativeSqliteDriver(
            schema = DictionaryDatabase.Schema.synchronous(),
            name = name,
            onConfiguration = { config: DatabaseConfiguration ->
                config.copy(extendedConfig = DatabaseConfiguration.Extended(basePath = directory))
            },
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun ensureFile(path: String, dictionary: DictionaryId, assets: DictionaryAssets): Boolean {
        val manager = NSFileManager.defaultManager
        val stampPath = "$path.format"
        val stamp = NSString.stringWithContentsOfFile(stampPath, encoding = NSUTF8StringEncoding, error = null)
        if (manager.fileExistsAtPath(path) && stamp?.trim() == DictionaryId.FORMAT.toString()) return true
        val bytes = assets.readBytes(dictionary) ?: return false
        manager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
        val data = bytes.usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong()) }
        check(data.writeToFile(path, atomically = true)) { "Could not write $path" }
        (DictionaryId.FORMAT.toString() as NSString).writeToFile(stampPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        return true
    }
}
