package com.tayra.languages.core.data.speech

import com.tayra.languages.core.data.network.Md5
import com.tayra.languages.core.domain.service.SpeechAudioCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Sentence audio as WAV files in [directory], named by a hash of their key. */
class FileSpeechAudioCache(private val directory: File) : SpeechAudioCache {
    private fun file(key: String) = File(directory, Md5.hex(key.encodeToByteArray()) + ".wav")

    override suspend fun read(key: String): ByteArray? = withContext(Dispatchers.IO) {
        file(key).takeIf { it.isFile }?.readBytes()
    }

    override suspend fun write(key: String, audio: ByteArray) = withContext(Dispatchers.IO) {
        directory.mkdirs()
        // Written aside and renamed, so a reader never sees half a file.
        val part = File(directory, file(key).name + ".part")
        part.writeBytes(audio)
        part.renameTo(file(key))
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        directory.listFiles()?.forEach { it.delete() }
        Unit
    }
}
