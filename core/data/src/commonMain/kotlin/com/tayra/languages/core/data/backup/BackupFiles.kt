package com.tayra.languages.core.data.backup

/** A file in the app's backups folder. */
data class StoredBackupFile(val name: String, val sizeBytes: Long)

/** The folder that holds the backups on this platform. */
expect class BackupFiles {
    suspend fun list(): List<StoredBackupFile>
    suspend fun read(name: String): ByteArray
    suspend fun write(name: String, bytes: ByteArray)
    suspend fun delete(name: String)
}
