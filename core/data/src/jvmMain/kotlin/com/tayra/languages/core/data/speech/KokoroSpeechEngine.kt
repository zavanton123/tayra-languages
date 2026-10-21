package com.tayra.languages.core.data.speech

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/** Kokoro: one model that speaks several languages, each with a handful of built-in voices. */
class KokoroSpeechEngine(
    python: ManagedPython,
    worker: TtsWorker,
    private val dir: File = File(DatabaseDriverFactory.dataDirectory(), "tts/kokoro"),
) : PythonSpeechEngine(
    python, worker, "kokoro", listOf("kokoro-onnx", "soundfile") + READER_PACKAGES,
    pipDownloadSize = 39_000_000 + READERS_BYTES, pipNotes = READER_NOTES,
) {

    override val engine: SpeechEngine = SpeechEngine.KOKORO
    // The worker pads the clips with silence since this version.
    override val cacheVersion: String = "padded"
    override val displayName: String = "Kokoro"
    override val description: String = "Kokoro is a high-quality neural voice that runs on this computer with no network. It speaks English, Spanish, French, Italian, Portuguese, Hindi, Japanese and Mandarin Chinese; other languages fall back to the system voice."
    override val packagesDescription: String = "One download holds the model and all its voices."

    override suspend fun installedSummary(): String = if (isInstalled()) "Model downloaded" else "Model not downloaded yet"

    override fun missing(reply: JsonObject): String? = missingKokoroReaders(reply)

    override val missingDownloadSize: Long = READERS_BYTES

    override suspend fun packages(): List<SpeechPackage> = listOf(
        SpeechPackage(PACKAGE, "Kokoro model with ${VOICES.size} voices", languageCode = null, group = "English, Spanish, French, Italian, Portuguese, Hindi, Japanese, Mandarin Chinese", sizeBytes = MODEL_BYTES + VOICES_BYTES, installed = isInstalled()),
    )

    override suspend fun installPackage(id: String) = withContext(Dispatchers.IO) {
        try {
            FileDownloads.download("$BASE/voices-v1.0.bin", File(dir, VOICES_FILE)) { done, total ->
                progressState.value = "Downloading the Kokoro voices (${megabytes(done)} of ${megabytes(total)})\u2026"
            }
            FileDownloads.download("$BASE/kokoro-v1.0.int8.onnx", File(dir, MODEL_FILE)) { done, total ->
                progressState.value = "Downloading the Kokoro model (${megabytes(done)} of ${megabytes(total)})\u2026"
            }
        } finally {
            progressState.value = null
        }
    }

    override suspend fun removePackage(id: String) {
        withContext(Dispatchers.IO) { dir.deleteRecursively() }
        worker.restart()
    }

    override fun packageVoices(id: String, languageCode: String): List<SpeechVoice> =
        VOICES.filter { it.language == languageCode }.map { SpeechVoice(it.id, it.label, languageCode, KOKORO_VOICE_BYTES) }

    override suspend fun voices(languageCode: String): List<SpeechVoice> =
        if (!isInstalled()) emptyList() else VOICES.filter { it.language == languageCode }.map { SpeechVoice(it.id, it.label, languageCode) }

    override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
        if (!isInstalled()) return null
        val fields = synthesisFields(text, languageCode, voiceId, speed) ?: return null
        return synthesizeWith(*fields.toTypedArray())
    }

    /** The worker's request for [text], or null when no voice speaks [languageCode]. */
    internal fun synthesisFields(text: String, languageCode: String, voiceId: String?, speed: Float): List<Pair<String, String>>? {
        val usable = VOICES.filter { it.language == languageCode }
        val voice = usable.firstOrNull { it.id == voiceId } ?: usable.firstOrNull() ?: return null
        return listOf(
            "model" to File(dir, MODEL_FILE).absolutePath, "voices" to File(dir, VOICES_FILE).absolutePath,
            "voice" to voice.id, "lang" to voice.accent, "text" to text, "speed" to speed.toString(),
        )
    }

    private fun isInstalled(): Boolean = File(dir, MODEL_FILE).exists() && File(dir, VOICES_FILE).exists()

    /** [accent] is the phonemizer language Kokoro expects for the voice; the worker reads "zh" and "ja" with misaki. */
    private class Voice(val id: String, val language: String, val accent: String, val label: String)

    private companion object {
        /**
         * misaki's Chinese and Japanese readers, as Kokoro's own pipeline sets them up for this model: jieba and
         * pypinyin for Chinese; cutlet on fugashi for Japanese, with UniDic Lite (47 MB) as its dictionary, since
         * the full UniDic is a separate 526 MB download.
         */
        val READER_PACKAGES = listOf("misaki", "ordered-set", "jieba", "pypinyin", "cn2an", "fugashi", "jaconv", "mojimoji", "unidic-lite")
        val READER_NOTES = mapOf("unidic-lite" to "the Japanese dictionary, 47 MB", "jieba" to "the Chinese dictionary, 19 MB")
        const val READERS_BYTES = 74_000_000L

        const val BASE = "https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0"
        const val PACKAGE = "kokoro-v1.0"
        const val MODEL_FILE = "kokoro.onnx"
        const val VOICES_FILE = "voices.bin"
        const val MODEL_BYTES = 92_361_271L
        const val VOICES_BYTES = 28_214_398L

        /** Voice ids start with the accent letter and f or m; the best-rated voice of each language leads. */
        val VOICES: List<Voice> = listOf(
            "af_heart", "af_bella", "af_nicole", "af_sarah", "af_aoede", "af_kore", "af_alloy", "af_jessica", "af_nova", "af_river", "af_sky",
            "am_michael", "am_fenrir", "am_puck", "am_adam", "am_echo", "am_eric", "am_liam", "am_onyx",
            "bf_emma", "bf_isabella", "bf_alice", "bf_lily", "bm_george", "bm_fable", "bm_daniel", "bm_lewis",
            "ef_dora", "em_alex", "ff_siwis", "hf_alpha", "hf_beta", "hm_omega", "hm_psi", "if_sara", "im_nicola",
            "jf_alpha", "jf_gongitsune", "jf_tebukuro", "jf_nezumi", "jm_kumo", "pf_dora", "pm_alex",
            "zf_xiaobei", "zf_xiaoni", "zf_xiaoxiao", "zf_xiaoyi", "zm_yunjian", "zm_yunxi", "zm_yunxia", "zm_yunyang",
        ).map { id ->
            val (language, accent, region) = when (id[0]) {
                'a' -> Triple("en", "en-us", "American")
                'b' -> Triple("en", "en-gb", "British")
                'e' -> Triple("es", "es", "Spanish")
                'f' -> Triple("fr", "fr-fr", "French")
                'h' -> Triple("hi", "hi", "Hindi")
                'i' -> Triple("it", "it", "Italian")
                'j' -> Triple("ja", "ja", "Japanese")
                'p' -> Triple("pt", "pt-br", "Brazilian")
                else -> Triple("zh", "zh", "Mandarin")
            }
            val name = id.substringAfter('_').replaceFirstChar { it.uppercase() }
            Voice(id, language, accent, "$name ($region, ${if (id[1] == 'f') "female" else "male"})")
        }
    }
}

/** The readers the worker's status lists as failing to load, as installations made before they were added lack them. */
internal fun missingKokoroReaders(reply: JsonObject): String? {
    val names = reply["missing"]?.jsonArray.orEmpty().mapNotNull {
        when (it.jsonPrimitive.content) {
            "zh" -> "Chinese"
            "ja" -> "Japanese"
            else -> null
        }
    }
    return if (names.isEmpty()) null else "${names.joinToString(" and ")} reader${if (names.size == 1) "" else "s"} not installed yet"
}
