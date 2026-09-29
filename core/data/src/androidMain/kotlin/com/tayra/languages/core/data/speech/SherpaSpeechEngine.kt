package com.tayra.languages.core.data.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * What Piper and Kokoro share on Android: both run in sherpa-onnx from model folders that are
 * downloaded as `.tar.bz2` archives and unpacked into the app's files.
 */
abstract class SherpaSpeechEngine(context: Context, folder: String) : LocalSpeechEngine {
    protected val dir = File(context.applicationContext.filesDir, "tts/$folder")
    private val cache = File(context.applicationContext.cacheDir, "tts-downloads")

    override val hasRuntimeSetup: Boolean = false

    protected val progressState = MutableStateFlow<String?>(null)
    override val progress: StateFlow<String?> = progressState

    private val lock = Mutex()
    private var loadedKey: String? = null
    private var loaded: OfflineTts? = null

    override suspend fun isReady(): Boolean = true

    override suspend fun setUp(): String = status()

    /** Downloads [url] and unpacks it as [name] under [dir]; the archive's own top folder is dropped. */
    protected suspend fun installArchive(url: String, name: String, label: String) = withContext(Dispatchers.IO) {
        val archive = File(cache, "$name.tar.bz2")
        val staging = File(dir, "$name.unpacking")
        try {
            FileDownloads.download(url, archive) { done, total ->
                progressState.value = "Downloading $label (${done / 1_000_000} of ${total / 1_000_000} MB)\u2026"
            }
            progressState.value = "Unpacking $label\u2026"
            staging.deleteRecursively(); staging.mkdirs()
            TarArchiveInputStream(BZip2CompressorInputStream(archive.inputStream().buffered())).use { tar ->
                while (true) {
                    val entry = tar.nextEntry ?: break
                    val relative = entry.name.substringAfter('/', "")
                    if (relative.isEmpty() || relative.contains("..")) continue
                    val out = File(staging, relative)
                    if (entry.isDirectory) out.mkdirs() else { out.parentFile?.mkdirs(); out.outputStream().use { tar.copyTo(it) } }
                }
            }
            File(dir, name).deleteRecursively()
            if (!staging.renameTo(File(dir, name))) error("could not store $label")
        } finally {
            archive.delete()
            staging.deleteRecursively()
            progressState.value = null
        }
    }

    protected suspend fun remove(name: String) {
        lock.withLock {
            withContext(Dispatchers.IO) {
                if (loadedKey?.startsWith(name) == true) { loaded?.release(); loaded = null; loadedKey = null }
                File(dir, name).deleteRecursively()
            }
        }
    }

    /** Synthesizes with the engine for [key], building it with [create] when another one is loaded; one at a time keeps memory low. */
    protected suspend fun speak(key: String, text: String, speaker: Int, speed: Float, create: () -> OfflineTts): ByteArray = lock.withLock {
        withContext(Dispatchers.Default) {
            val tts = if (loadedKey == key) loaded!! else {
                loaded?.release()
                create().also { loaded = it; loadedKey = key }
            }
            wav(tts.generate(text = text, sid = speaker, speed = speed))
        }
    }

    /** 16-bit mono WAV from sherpa's float samples. */
    private fun wav(audio: GeneratedAudio): ByteArray {
        val samples = audio.samples
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) data.putShort((s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt(36 + samples.size * 2).put("WAVE".toByteArray())
        header.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(audio.sampleRate).putInt(audio.sampleRate * 2).putShort(2).putShort(16)
        header.put("data".toByteArray()).putInt(samples.size * 2)
        return ByteArrayOutputStream(44 + samples.size * 2).apply { write(header.array()); write(data.array()) }.toByteArray()
    }

    protected companion object {
        const val MODELS = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"
    }
}
