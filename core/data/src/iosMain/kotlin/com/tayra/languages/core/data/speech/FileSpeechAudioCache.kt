package com.tayra.languages.core.data.speech

import com.tayra.languages.core.data.network.Md5
import com.tayra.languages.core.domain.service.SpeechAudioCache
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSNumber
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

/**
 * Sentence audio as files in the app's Caches folder, named by a hash of their key. Past
 * [maxBytes] the files used longest ago are deleted; reading a file marks it as used.
 */
@OptIn(ExperimentalForeignApi::class)
class FileSpeechAudioCache(private val maxBytes: Long = SpeechAudioCache.MAX_BYTES_ON_DISK) : SpeechAudioCache {
    private val directory: String =
        (NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).firstOrNull() as? String ?: "") + "/speech-cache"

    private fun path(key: String) = "$directory/${Md5.hex(key.encodeToByteArray())}.wav"

    override suspend fun read(key: String): ByteArray? {
        val data = NSData.dataWithContentsOfFile(path(key)) ?: return null
        NSFileManager.defaultManager.setAttributes(mapOf<Any?, Any?>(NSFileModificationDate to NSDate()), ofItemAtPath = path(key), error = null)
        val size = data.length.toInt()
        if (size == 0) return ByteArray(0)
        return ByteArray(size).also { bytes -> bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
    }

    override suspend fun write(key: String, audio: ByteArray) {
        if (audio.isEmpty()) return
        NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
        val data = audio.usePinned { NSData.create(bytes = it.addressOf(0), length = audio.size.toULong()) }
        data.writeToFile(path(key), atomically = true)
        trim()
    }

    /** Deletes the files used longest ago until the rest fit in [maxBytes]. */
    private fun trim() {
        val manager = NSFileManager.defaultManager
        val files = manager.contentsOfDirectoryAtPath(directory, error = null).orEmpty().mapNotNull { name ->
            val path = "$directory/$name"
            val attributes = manager.attributesOfItemAtPath(path, error = null) ?: return@mapNotNull null
            val size = (attributes[NSFileSize] as? NSNumber)?.longLongValue ?: 0L
            val used = (attributes[NSFileModificationDate] as? NSDate)?.timeIntervalSince1970 ?: 0.0
            Triple(path, size, used)
        }.sortedBy { it.third }
        var total = files.sumOf { it.second }
        for ((path, size, _) in files.dropLast(1)) {
            if (total <= maxBytes) break
            total -= size
            manager.removeItemAtPath(path, error = null)
        }
    }

    override suspend fun clear() {
        NSFileManager.defaultManager.removeItemAtPath(directory, error = null)
    }
}
