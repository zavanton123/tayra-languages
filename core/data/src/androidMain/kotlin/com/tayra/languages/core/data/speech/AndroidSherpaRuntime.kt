package com.tayra.languages.core.data.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File

/** sherpa-onnx through its Java API, with models in the app's files. */
class AndroidSherpaRuntime(context: Context) : SherpaRuntime {
    private val files = context.applicationContext.filesDir
    private val cache = File(context.applicationContext.cacheDir, "tts-downloads")

    private val lock = Mutex()
    private var loadedKey: String? = null
    private var loadedModel: String? = null
    private var loaded: OfflineTts? = null

    override fun folder(engine: String): String = File(files, "tts/$engine").absolutePath

    override fun list(path: String): List<String>? = File(path).list()?.toList()

    override suspend fun installArchive(url: String, destination: String, onDownload: (Long, Long) -> Unit, onUnpack: () -> Unit) = withContext(Dispatchers.IO) {
        val target = File(destination)
        val archive = File(cache, "${target.name}.tar.bz2")
        val staging = File(target.parentFile, "${target.name}.unpacking")
        try {
            FileDownloads.download(url, archive, onDownload)
            onUnpack()
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
            target.deleteRecursively()
            if (!staging.renameTo(target)) error("could not store ${target.name}")
        } finally {
            archive.delete()
            staging.deleteRecursively()
        }
    }

    override suspend fun delete(path: String) {
        lock.withLock {
            withContext(Dispatchers.IO) {
                if (loadedModel?.startsWith(path) == true) { loaded?.release(); loaded = null; loadedKey = null; loadedModel = null }
                File(path).deleteRecursively()
            }
        }
    }

    override suspend fun synthesize(request: SherpaRequest): ByteArray = lock.withLock {
        withContext(Dispatchers.Default) {
            val tts = if (loadedKey == request.key) loaded!! else {
                loaded?.release()
                create(request).also { loaded = it; loadedKey = request.key; loadedModel = request.model }
            }
            val audio = tts.generate(text = request.text, sid = request.speaker, speed = request.speed)
            Wav.encode(audio.samples, audio.sampleRate)
        }
    }

    private fun create(r: SherpaRequest): OfflineTts {
        val model = if (r.kokoro) {
            OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(model = r.model, voices = r.voices, tokens = r.tokens, dataDir = r.dataDir, lexicon = r.lexicon, lang = r.lang, dictDir = r.dictDir),
                numThreads = 4, debug = false, provider = "cpu",
            )
        } else {
            OfflineTtsModelConfig(vits = OfflineTtsVitsModelConfig(model = r.model, tokens = r.tokens, dataDir = r.dataDir), numThreads = 2, debug = false, provider = "cpu")
        }
        return OfflineTts(config = OfflineTtsConfig(model = model))
    }
}
