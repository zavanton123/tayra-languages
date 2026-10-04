package com.tayra.languages.core.domain.backup

import kotlinx.datetime.LocalDateTime

/** A backup kept by the app; [createdAt] is in local time, null for a file whose name does not tell. */
data class Backup(val name: String, val createdAt: LocalDateTime?, val sizeBytes: Long)

/** A file that is not a backup, or one this version of the app cannot restore. */
class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Complete copies of the user's data: every table of the database and the settings that make
 * sense on another device, in one SQLite file. Audio files, downloaded dictionaries, voices and
 * translation models are not included, nor are API keys.
 */
interface BackupRepository {
    /** The backups kept by the app, newest first. */
    suspend fun list(): List<Backup>

    suspend fun create(): Backup

    suspend fun delete(name: String)

    /** The backup's file, to be saved elsewhere. */
    suspend fun read(name: String): ByteArray

    /** Keeps a backup file saved elsewhere among the app's backups; throws [BackupException] if it is not one. */
    suspend fun import(file: ByteArray): Backup

    /**
     * Replaces all data and the backed-up settings with those of the backup. The current data is
     * backed up first, so a restore can be undone; that backup is returned.
     */
    suspend fun restore(name: String): Backup
}
