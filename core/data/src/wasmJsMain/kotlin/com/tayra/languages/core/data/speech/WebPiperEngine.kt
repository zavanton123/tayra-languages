package com.tayra.languages.core.data.speech

import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.js.Promise
import kotlin.math.roundToInt

// The library is loaded on first use, so the app starts without it and its ONNX runtime.

/** The voice catalog, as JSON: one entry per voice with the size of its files. */
private fun piperVoices(): Promise<JsAny?> = js(
    """import('@mintplex-labs/piper-tts-web').then(function (tts) { return tts.voices(); }).then(function (voices) {
        return JSON.stringify(voices.map(function (v) {
            var size = 0;
            for (var f in v.files) size += v.files[f].size_bytes;
            return { key: v.key, name: v.name, family: v.language.family, country: v.language.country_english, language: v.language.name_english, quality: v.quality, size: size };
        }));
    })""",
)

/** The ids of the voices kept in the browser, as JSON. */
private fun piperStored(): Promise<JsAny?> = js(
    """import('@mintplex-labs/piper-tts-web').then(function (tts) { return tts.stored(); }).then(function (ids) { return JSON.stringify(ids); })""",
)

private fun piperDownload(voiceId: String, onProgress: (Double) -> Unit): Promise<JsAny?> = js(
    """import('@mintplex-labs/piper-tts-web').then(function (tts) {
        return tts.download(voiceId, function (p) { if (p.total > 0) onProgress(p.loaded / p.total); });
    }).then(function () { return null; })""",
)

private fun piperRemove(voiceId: String): Promise<JsAny?> = js(
    """import('@mintplex-labs/piper-tts-web').then(function (tts) { return tts.remove(voiceId); }).then(function () { return null; })""",
)

/** WAV audio of [text] in [voiceId], as base64. */
private fun piperSpeak(voiceId: String, text: String): Promise<JsAny?> = js(
    """import('@mintplex-labs/piper-tts-web').then(function (tts) { return tts.predict({ voiceId: voiceId, text: text }); })
    .then(function (blob) { return blob.arrayBuffer(); })
    .then(function (buffer) {
        var bytes = new Uint8Array(buffer);
        var binary = '';
        for (var i = 0; i < bytes.length; i += 8192) binary += String.fromCharCode.apply(null, bytes.subarray(i, i + 8192));
        return btoa(binary);
    })""",
)

@Serializable
private class WebPiperVoice(val key: String, val name: String, val family: String, val country: String, val language: String, val quality: String, val size: Long) {
    /** The app's language code: Piper writes Norwegian as "no", the app uses "nb". */
    val appCode: String get() = if (family == "no") "nb" else family
    val label: String get() = name.replace('_', ' ').replaceFirstChar { it.uppercase() }
    val qualityLabel: String get() = quality.replace('_', ' ')
}

/**
 * Piper in the browser: the same voices as elsewhere, run by the ONNX runtime in WebAssembly.
 * Voices are downloaded once into the browser's private file storage.
 */
class WebPiperEngine : LocalSpeechEngine {
    override val engine: SpeechEngine = SpeechEngine.PIPER
    override val displayName: String = "Piper"
    override val description: String = "Piper is a neural speech engine that runs in the browser. Voices are downloaded once and kept by the browser; the first sentence takes a moment while the engine loads."
    override val packagesDescription: String = "One download per voice, 20 to 120 MB each."
    override val hasRuntimeSetup: Boolean = false
    override val supportsSpeed: Boolean = false
    override val progress = MutableStateFlow<String?>(null)

    private val json = Json { ignoreUnknownKeys = true }
    private var catalog: List<WebPiperVoice>? = null

    private suspend fun catalog(): List<WebPiperVoice> = catalog ?: json.decodeFromString<List<WebPiperVoice>>(piperVoices().await<JsAny?>().toString())
        .sortedWith(compareBy({ it.language }, { it.country }, { it.name }, { it.quality }))
        .also { catalog = it }

    private suspend fun stored(): Set<String> = json.decodeFromString<List<String>>(piperStored().await<JsAny?>().toString()).toSet()

    override suspend fun status(): String {
        val count = stored().size
        return if (count == 0) "Piper is ready. No voices downloaded yet." else "Piper is ready with $count voice${if (count == 1) "" else "s"} downloaded."
    }

    override suspend fun isReady(): Boolean = true

    override suspend fun setUp(): String = status()

    override suspend fun packages(): List<SpeechPackage> {
        val installed = stored()
        return catalog().map { SpeechPackage(it.key, "${it.label} · ${it.qualityLabel} · ${it.country}", it.appCode, it.language, it.size, it.key in installed) }
    }

    override suspend fun installPackage(id: String) {
        val voice = catalog().firstOrNull { it.key == id } ?: error("unknown voice $id")
        progress.value = "Downloading ${voice.label}..."
        try {
            piperDownload(id) { fraction -> progress.value = "Downloading ${voice.label}: ${(fraction * 100).roundToInt()}%" }.await<JsAny?>()
        } finally {
            progress.value = null
        }
    }

    override suspend fun removePackage(id: String) {
        piperRemove(id).await<JsAny?>()
    }

    override suspend fun voices(languageCode: String): List<SpeechVoice> {
        val installed = stored()
        return catalog().filter { it.appCode == languageCode && it.key in installed }
            .map { SpeechVoice(it.key, "${it.label} (${it.country}, ${it.qualityLabel})", languageCode) }
    }

    /** [speed] is not applied: the browser build of Piper reads at its voice's own pace. */
    override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
        val usable = voices(languageCode)
        val voice = usable.firstOrNull { it.id == voiceId } ?: usable.firstOrNull() ?: return null
        val base64 = piperSpeak(voice.id, text).await<JsAny?>()?.toString() ?: error("Piper gave no audio")
        return Base64.decode(base64)
    }
}
