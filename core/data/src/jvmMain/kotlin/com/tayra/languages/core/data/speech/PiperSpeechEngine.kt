package com.tayra.languages.core.data.speech

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Piper on the desktop: one ONNX model per voice, downloaded from the rhasspy/piper-voices catalog. */
class PiperSpeechEngine(
    python: ManagedPython,
    worker: TtsWorker,
    private val dir: File = File(DatabaseDriverFactory.dataDirectory(), "tts/piper"),
) : PythonSpeechEngine(python, worker, "piper", listOf("piper-tts")) {

    override val engine: SpeechEngine = SpeechEngine.PIPER
    override val displayName: String = "Piper"
    override val description: String = "Piper is a neural speech engine that runs on this computer with no network. It has voices for most of the languages the app teaches."
    override val packagesDescription: String = "One download per voice, 20 to 120 MB each. Higher quality means a larger file and a slightly slower voice."

    private val catalog = PiperCatalog(dir)

    override suspend fun installedSummary(): String {
        val count = dir.listFiles()?.count { isInstalled(it.name) } ?: 0
        return if (count == 0) "No voices downloaded yet." else "$count voice${if (count == 1) "" else "s"} downloaded."
    }

    override suspend fun packages(): List<SpeechPackage> = voices().map { SpeechPackage(it.key, it.title, it.appCode, it.language, it.size, isInstalled(it.key)) }

    override suspend fun installPackage(id: String) = withContext(Dispatchers.IO) {
        val voice = voices().firstOrNull { it.key == id } ?: error("unknown voice $id")
        val target = folder(id)
        try {
            FileDownloads.download(catalog.url(voice.config), File(target, CONFIG))
            FileDownloads.download(catalog.url(voice.model), File(target, MODEL)) { done, total ->
                progressState.value = "Downloading ${voice.name} (${megabytes(done)} of ${megabytes(total)})\u2026"
            }
        } catch (e: Exception) {
            target.deleteRecursively()
            throw e
        } finally {
            progressState.value = null
        }
    }

    override suspend fun removePackage(id: String) {
        withContext(Dispatchers.IO) { folder(id).deleteRecursively() }
    }

    override suspend fun voices(languageCode: String): List<SpeechVoice> =
        voices().filter { it.appCode == languageCode && isInstalled(it.key) }.map { SpeechVoice(it.key, "${it.name} (${it.country}, ${it.quality})", languageCode) }

    override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
        val usable = voices(languageCode)
        val voice = usable.firstOrNull { it.id == voiceId } ?: usable.firstOrNull() ?: return null
        return synthesizeWith("model" to File(folder(voice.id), MODEL).absolutePath, "text" to text, "speed" to speed.toString())
    }

    private suspend fun voices(): List<PiperVoice> = withContext(Dispatchers.IO) { catalog.voices() }

    private fun isInstalled(key: String): Boolean = File(folder(key), MODEL).exists() && File(folder(key), CONFIG).exists()

    private fun folder(key: String) = File(dir, key)

    private companion object {
        const val MODEL = "voice.onnx"
        const val CONFIG = "voice.onnx.json"
    }
}
