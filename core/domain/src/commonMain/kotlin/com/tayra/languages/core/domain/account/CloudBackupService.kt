package com.tayra.languages.core.domain.account

import com.tayra.languages.core.domain.backup.Backup
import com.tayra.languages.core.domain.backup.BackupRepository

/**
 * The library kept in the cloud as the same file a backup on the device is: backing up makes a
 * backup here and sends it; restoring takes one back, keeps it among this device's backups, and
 * restores from it, the library as it was backed up on the device first so the restore can be undone.
 */
class CloudBackupService(private val local: BackupRepository, private val cloud: CloudBackupStore) {

    suspend fun list(): List<CloudBackupInfo> = cloud.list()

    suspend fun backUp(): CloudBackupInfo {
        val backup = local.create()
        return cloud.upload(backup.name, local.read(backup.name))
    }

    /** Replaces the library with the cloud backup [id]; returns the backup of the library as it was. */
    suspend fun restore(id: String): Backup {
        val taken = local.import(cloud.download(id))
        return local.restore(taken.name)
    }

    suspend fun delete(id: String) = cloud.delete(id)
}
