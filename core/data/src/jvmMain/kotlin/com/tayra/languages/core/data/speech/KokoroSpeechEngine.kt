package com.tayra.languages.core.data.speech

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Kokoro: one model that speaks several languages, each with a handful of built-in voices. */
class KokoroSpeechEngine(
    python: ManagedPython,
    worker: TtsWorker,
    private val dir: File = File(DatabaseDriverFactory.dataDirectory(), "tts/kokoro"),
) : PythonSpeechEngine(python, worker, "kokoro", listOf("kokoro-onnx", "soundfile"), pipDownloadSize = 39_000_000) {

    override val engine: SpeechEngine = SpeechEngine.KOKORO
    override val displayName: String = "Kokoro"
    override val description: String = "Kokoro is a high-quality neural voice that runs on this computer with no network. It speaks English, Spanish, French, Italian and Portuguese; other languages fall back to the system voice."
    override val packagesDescription: String = "One download holds the model and all its voices."

    override suspend fun installedSummary(): String = if (isInstalled()) "Model downloaded" else "Model not downloaded yet"

    override suspend fun packages(): List<SpeechPackage> = listOf(
        SpeechPackage(PACKAGE, "Kokoro model with ${VOICES.size} voices", languageCode = null, group = "English, Spanish, French, Italian, Portuguese", sizeBytes = MODEL_BYTES + VOICES_BYTES, installed = isInstalled()),
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

    override suspend fun voices(languageCode: String): List<SpeechVoice> =
        if (!isInstalled()) emptyList() else VOICES.filter { it.language == languageCode }.map { SpeechVoice(it.id, it.label, languageCode) }

    override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
        if (!isInstalled()) return null
        val usable = VOICES.filter { it.language == languageCode }
        val voice = usable.firstOrNull { it.id == voiceId } ?: usable.firstOrNull() ?: return null
        return synthesizeWith(
            "model" to File(dir, MODEL_FILE).absolutePath, "voices" to File(dir, VOICES_FILE).absolutePath,
            "voice" to voice.id, "lang" to voice.accent, "text" to text, "speed" to speed.toString(),
        )
    }

    private fun isInstalled(): Boolean = File(dir, MODEL_FILE).exists() && File(dir, VOICES_FILE).exists()

    /** [accent] is the phonemizer language Kokoro expects for the voice. */
    private class Voice(val id: String, val language: String, val accent: String, val label: String)

    private companion object {
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
            "ef_dora", "em_alex", "ff_siwis", "if_sara", "im_nicola", "pf_dora", "pm_alex",
        ).map { id ->
            val (language, accent, region) = when (id[0]) {
                'a' -> Triple("en", "en-us", "American")
                'b' -> Triple("en", "en-gb", "British")
                'e' -> Triple("es", "es", "Spanish")
                'f' -> Triple("fr", "fr-fr", "French")
                'i' -> Triple("it", "it", "Italian")
                else -> Triple("pt", "pt-br", "Brazilian")
            }
            val name = id.substringAfter('_').replaceFirstChar { it.uppercase() }
            Voice(id, language, accent, "$name ($region, ${if (id[1] == 'f') "female" else "male"})")
        }
    }
}
