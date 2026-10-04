package com.tayra.languages.core.data.backup

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

/** Backups live in the app's Application Support folder. */
@OptIn(ExperimentalForeignApi::class)
actual class BackupFiles {
    private val directory: String =
        (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).firstOrNull() as? String ?: "") + "/backups"

    actual suspend fun list(): List<StoredBackupFile> = withContext(Dispatchers.IO) {
        val manager = NSFileManager.defaultManager
        manager.contentsOfDirectoryAtPath(directory, error = null).orEmpty().mapNotNull { name ->
            val attributes = manager.attributesOfItemAtPath("$directory/$name", error = null) ?: return@mapNotNull null
            StoredBackupFile(name as String, (attributes[NSFileSize] as? NSNumber)?.longLongValue ?: 0L)
        }
    }

    actual suspend fun read(name: String): ByteArray = withContext(Dispatchers.IO) {
        val data = NSData.dataWithContentsOfFile("$directory/$name") ?: error("Could not read $name")
        val size = data.length.toInt()
        ByteArray(size).also { bytes -> if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
    }

    actual suspend fun write(name: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        if (!data.writeToFile("$directory/$name", atomically = true)) error("Could not save $name")
    }

    actual suspend fun delete(name: String) {
        withContext(Dispatchers.IO) { NSFileManager.defaultManager.removeItemAtPath("$directory/$name", error = null) }
    }
}
