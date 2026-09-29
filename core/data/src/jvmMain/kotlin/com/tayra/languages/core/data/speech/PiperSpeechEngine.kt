package com.tayra.languages.core.data.speech

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.net.URLEncoder

/** Piper: one ONNX model per voice, downloaded from the rhasspy/piper-voices catalog. */
class PiperSpeechEngine(
    python: ManagedPython,
    worker: TtsWorker,
    private val dir: File = File(DatabaseDriverFactory.dataDirectory(), "tts/piper"),
) : PythonSpeechEngine(python, worker, "piper", listOf("piper-tts")) {

    override val engine: SpeechEngine = SpeechEngine.PIPER
    override val displayName: String = "Piper"
    override val description: String = "Piper is a neural speech engine that runs on this computer with no network. It has voices for most of the languages the app teaches."
    override val packagesDescription: String = "One download per voice, 20 to 120 MB each. Higher quality means a larger file and a slightly slower voice."

    private class Voice(val key: String, val name: String, val quality: String, val family: String, val language: String, val country: String, val model: String, val config: String, val size: Long)

    private val json = Json { ignoreUnknownKeys = true }
    private var catalog: List<Voice>? = null

    override suspend fun installedSummary(): String {
        val voices = installed()
        return if (voices.isEmpty()) "No voices downloaded yet." else "${voices.size} voice${if (voices.size == 1) "" else "s"} downloaded."
    }

    override suspend fun packages(): List<SpeechPackage> = catalog().map { it.toPackage() }

    override suspend fun installPackage(id: String) = withContext(Dispatchers.IO) {
        val voice = catalog().firstOrNull { it.key == id } ?: error("unknown voice $id")
        val target = folder(id)
        try {
            python.download(url(voice.config), File(target, CONFIG)) { _, _ -> }
            python.download(url(voice.model), File(target, MODEL)) { done, total ->
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
        catalog().filter { appCode(it.family) == languageCode && isInstalled(it.key) }
            .map { SpeechVoice(it.key, "${it.name} (${it.country}, ${it.quality})", languageCode) }

    override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
        val usable = voices(languageCode)
        val voice = usable.firstOrNull { it.id == voiceId } ?: usable.firstOrNull() ?: return null
        return synthesizeWith("model" to File(folder(voice.id), MODEL).absolutePath, "text" to text, "speed" to speed.toString())
    }

    private fun Voice.toPackage() = SpeechPackage(
        id = key,
        title = "$name \u00b7 $quality \u00b7 $country",
        languageCode = appCode(family),
        group = language,
        sizeBytes = size,
        installed = isInstalled(key),
    )

    private fun installed(): List<String> = dir.listFiles()?.filter { File(it, MODEL).exists() && File(it, CONFIG).exists() }?.map { it.name }.orEmpty()

    private fun isInstalled(key: String): Boolean = File(folder(key), MODEL).exists() && File(folder(key), CONFIG).exists()

    private fun folder(key: String) = File(dir, key)

    /** The catalog, from a copy kept for a week; an older copy still serves when the network is down. */
    private suspend fun catalog(): List<Voice> = catalog ?: withContext(Dispatchers.IO) {
        val cached = File(dir, "voices.json")
        val stale = !cached.exists() || System.currentTimeMillis() - cached.lastModified() > CATALOG_TTL_MS
        if (stale) runCatching { python.download("$BASE/voices.json", cached) { _, _ -> } }
        if (!cached.exists()) error("the voice catalog could not be downloaded")
        parse(cached.readText()).also { catalog = it }
    }

    private fun parse(text: String): List<Voice> = json.parseToJsonElement(text).jsonObject.values.mapNotNull { element ->
        val o = element.jsonObject
        val language = o["language"]?.jsonObject ?: return@mapNotNull null
        val files = o["files"]?.jsonObject ?: return@mapNotNull null
        val model = files.keys.firstOrNull { it.endsWith(".onnx") } ?: return@mapNotNull null
        val config = files.keys.firstOrNull { it.endsWith(".onnx.json") } ?: return@mapNotNull null
        Voice(
            key = o["key"]?.jsonPrimitive?.content ?: return@mapNotNull null,
            name = o["name"]?.jsonPrimitive?.content.orEmpty().replaceFirstChar { it.uppercase() },
            quality = o["quality"]?.jsonPrimitive?.content.orEmpty().replace('_', ' '),
            family = language["family"]?.jsonPrimitive?.content.orEmpty(),
            language = language["name_english"]?.jsonPrimitive?.content.orEmpty(),
            country = language["country_english"]?.jsonPrimitive?.content.orEmpty(),
            model = model,
            config = config,
            size = files[model]?.jsonObject?.get("size_bytes")?.jsonPrimitive?.longOrNull ?: 0L,
        )
    }.sortedWith(compareBy({ it.language }, { it.country }, { it.name }, { it.quality }))

    /** Catalog paths contain accented voice names, so each segment is percent-encoded. */
    private fun url(path: String): String = "$BASE/" + path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }

    /** Piper writes Norwegian as "no"; the app uses "nb". */
    private fun appCode(family: String): String = if (family == "no") "nb" else family

    private companion object {
        const val BASE = "https://huggingface.co/rhasspy/piper-voices/resolve/main"
        const val MODEL = "voice.onnx"
        const val CONFIG = "voice.onnx.json"
        const val CATALOG_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
