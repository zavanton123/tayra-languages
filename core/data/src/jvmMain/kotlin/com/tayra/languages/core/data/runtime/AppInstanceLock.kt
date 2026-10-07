package com.tayra.languages.core.data.runtime

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption

/**
 * A lock the desktop app holds on a file in the data folder while it is open, so the `tayra`
 * command can tell: the app shows the command's changes only after it is opened again, and a
 * restore under it would be overwritten by what the app still holds.
 */
object AppInstanceLock {
    private var held: FileLock? = null

    private fun file() = File(DatabaseDriverFactory.dataDirectory(), "app.lock")

    private fun channel(): FileChannel {
        val file = file().also { it.parentFile?.mkdirs() }
        return FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    }

    /** Takes the lock for the life of this process; false when another app already holds it. */
    @Synchronized
    fun acquire(): Boolean {
        if (held != null) return true
        held = runCatching { channel().tryLock() }.getOrNull()
        return held != null
    }

    /** Whether the app is open, in another process. */
    fun isAppOpen(): Boolean {
        if (held != null) return false
        return try {
            channel().use { channel ->
                val lock = channel.tryLock() ?: return true
                lock.release()
                false
            }
        } catch (_: OverlappingFileLockException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
