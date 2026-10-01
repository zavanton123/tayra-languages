package com.tayra.languages.core.data.speech

import com.tayra.languages.core.data.network.Md5
import com.tayra.languages.core.domain.service.SpeechAudioCache
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

/** Sentence audio as WAV files in the app's Caches folder, named by a hash of their key. */
@OptIn(ExperimentalForeignApi::class)
class FileSpeechAudioCache : SpeechAudioCache {
    private val directory: String =
        (NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).firstOrNull() as? String ?: "") + "/speech-cache"

    private fun path(key: String) = "$directory/${Md5.hex(key.encodeToByteArray())}.wav"

    override suspend fun read(key: String): ByteArray? {
        val data = NSData.dataWithContentsOfFile(path(key)) ?: return null
        val size = data.length.toInt()
        if (size == 0) return ByteArray(0)
        return ByteArray(size).also { bytes -> bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
    }

    override suspend fun write(key: String, audio: ByteArray) {
        if (audio.isEmpty()) return
        NSFileManager.defaultManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
        val data = audio.usePinned { NSData.create(bytes = it.addressOf(0), length = audio.size.toULong()) }
        data.writeToFile(path(key), atomically = true)
    }

    override suspend fun clear() {
        NSFileManager.defaultManager.removeItemAtPath(directory, error = null)
    }
}
