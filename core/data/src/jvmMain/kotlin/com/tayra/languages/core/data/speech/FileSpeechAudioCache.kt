package com.tayra.languages.core.data.speech

import com.tayra.languages.core.data.network.Md5
import com.tayra.languages.core.domain.service.SpeechAudioCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Sentence audio as files in [directory], named by a hash of their key. Past [maxBytes] the files
 * used longest ago are deleted; reading a file marks it as used.
 */
class FileSpeechAudioCache(
    private val directory: File,
    private val maxBytes: Long = SpeechAudioCache.MAX_BYTES_ON_DISK,
) : SpeechAudioCache {
    private fun file(key: String) = File(directory, Md5.hex(key.encodeToByteArray()) + ".wav")

    override suspend fun read(key: String): ByteArray? = withContext(Dispatchers.IO) {
        file(key).takeIf { it.isFile }?.let { found ->
            found.setLastModified(System.currentTimeMillis())
            found.readBytes()
        }
    }

    override suspend fun write(key: String, audio: ByteArray) = withContext(Dispatchers.IO) {
        directory.mkdirs()
        // Written aside and renamed, so a reader never sees half a file.
        val part = File(directory, file(key).name + ".part")
        part.writeBytes(audio)
        part.renameTo(file(key))
        trim()
    }

    /** Deletes the files used longest ago until the rest fit in [maxBytes]. */
    private fun trim() {
        val files = directory.listFiles { candidate -> candidate.isFile && candidate.name.endsWith(".wav") }.orEmpty().sortedBy { it.lastModified() }
        var total = files.sumOf { it.length() }
        for (oldest in files.dropLast(1)) {
            if (total <= maxBytes) break
            total -= oldest.length()
            oldest.delete()
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        directory.listFiles()?.forEach { it.delete() }
        Unit
    }
}
