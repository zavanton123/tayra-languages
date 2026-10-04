package com.tayra.languages.core.data.backup

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class BackupFiles(private val directory: File = File(DatabaseDriverFactory.dataDirectory(), "backups")) {
    actual suspend fun list(): List<StoredBackupFile> = withContext(Dispatchers.IO) {
        directory.listFiles().orEmpty().filter { it.isFile && !it.name.endsWith(".partial") }.map { StoredBackupFile(it.name, it.length()) }
    }

    actual suspend fun read(name: String): ByteArray = withContext(Dispatchers.IO) { File(directory, name).readBytes() }

    actual suspend fun write(name: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val partial = File(directory, "$name.partial")
        partial.writeBytes(bytes)
        if (!partial.renameTo(File(directory, name).also { it.delete() })) error("Could not save $name")
    }

    actual suspend fun delete(name: String) {
        withContext(Dispatchers.IO) { File(directory, name).delete() }
    }
}
