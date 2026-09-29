package com.tayra.languages.core.data.speech

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.net.URLEncoder

/** One voice of the rhasspy/piper-voices catalog. */
class PiperVoice(
    val key: String,
    val name: String,
    val quality: String,
    /** Piper's language family code, such as `pt`. */
    val family: String,
    /** The locale, such as `pt_PT`. */
    val locale: String,
    val language: String,
    val country: String,
    /** Catalog paths of the model and its config. */
    val model: String,
    val config: String,
    val size: Long,
) {
    /** The app's language code: Piper writes Norwegian as "no", the app uses "nb". */
    val appCode: String get() = if (family == "no") "nb" else family

    val title: String get() = "$name \u00b7 $quality \u00b7 $country"
}

/** The Piper voice catalog, kept in [dir] for a week; an older copy still serves when the network is down. */
class PiperCatalog(private val dir: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private var voices: List<PiperVoice>? = null

    /** Blocks on the first call while the catalog downloads. */
    fun voices(): List<PiperVoice> = voices ?: run {
        val cached = File(dir, "voices.json")
        val stale = !cached.exists() || System.currentTimeMillis() - cached.lastModified() > TTL_MS
        if (stale) runCatching { FileDownloads.download("$BASE/voices.json", cached) }
        if (!cached.exists()) error("the voice catalog could not be downloaded")
        parse(cached.readText()).also { voices = it }
    }

    /** Catalog paths contain accented voice names, so each segment is percent-encoded. */
    fun url(path: String): String = "$BASE/" + path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }

    private fun parse(text: String): List<PiperVoice> = json.parseToJsonElement(text).jsonObject.values.mapNotNull { element ->
        val o = element.jsonObject
        val language = o["language"]?.jsonObject ?: return@mapNotNull null
        val files = o["files"]?.jsonObject ?: return@mapNotNull null
        val model = files.keys.firstOrNull { it.endsWith(".onnx") } ?: return@mapNotNull null
        val config = files.keys.firstOrNull { it.endsWith(".onnx.json") } ?: return@mapNotNull null
        PiperVoice(
            key = o["key"]?.jsonPrimitive?.content ?: return@mapNotNull null,
            name = o["name"]?.jsonPrimitive?.content.orEmpty().replaceFirstChar { it.uppercase() },
            quality = o["quality"]?.jsonPrimitive?.content.orEmpty().replace('_', ' '),
            family = language["family"]?.jsonPrimitive?.content.orEmpty(),
            locale = language["code"]?.jsonPrimitive?.content.orEmpty(),
            language = language["name_english"]?.jsonPrimitive?.content.orEmpty(),
            country = language["country_english"]?.jsonPrimitive?.content.orEmpty(),
            model = model,
            config = config,
            size = files[model]?.jsonObject?.get("size_bytes")?.jsonPrimitive?.longOrNull ?: 0L,
        )
    }.sortedWith(compareBy({ it.language }, { it.country }, { it.name }, { it.quality }))

    private companion object {
        const val BASE = "https://huggingface.co/rhasspy/piper-voices/resolve/main"
        const val TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
