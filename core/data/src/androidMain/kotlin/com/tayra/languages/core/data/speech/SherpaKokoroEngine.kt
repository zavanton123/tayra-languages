package com.tayra.languages.core.data.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import java.io.File

/** Kokoro on Android: one model that speaks several languages, each with a handful of built-in voices. */
class SherpaKokoroEngine(context: Context) : SherpaSpeechEngine(context, "kokoro") {

    override val engine: SpeechEngine = SpeechEngine.KOKORO
    override val displayName: String = "Kokoro"
    override val description: String = "Kokoro is a high-quality neural voice that runs on this phone with no network. It speaks English, Spanish, French, Italian and Portuguese; other languages fall back to the system voice. It needs a recent phone to answer quickly."
    override val packagesDescription: String = "One download holds the model and all its voices."

    private val folder get() = File(dir, PACKAGE)

    override suspend fun status(): String = if (isInstalled()) "Kokoro is ready. The model is downloaded." else "Kokoro is ready. The model is not downloaded yet."

    override suspend fun packages(): List<SpeechPackage> = listOf(
        SpeechPackage(PACKAGE, "Kokoro model with ${VOICES.size} voices", languageCode = null, group = "English, Spanish, French, Italian, Portuguese", sizeBytes = ARCHIVE_BYTES, installed = isInstalled()),
    )

    override suspend fun installPackage(id: String) = installArchive("$MODELS/kokoro-int8-multi-lang-v1_0.tar.bz2", PACKAGE, "the Kokoro model")

    override suspend fun removePackage(id: String) = remove(PACKAGE)

    override suspend fun voices(languageCode: String): List<SpeechVoice> =
        if (!isInstalled()) emptyList() else VOICES.filter { it.language == languageCode }.map { SpeechVoice(it.id, it.label, languageCode) }

    override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
        if (!isInstalled()) return null
        val usable = VOICES.filter { it.language == languageCode }
        val voice = usable.firstOrNull { it.id == voiceId } ?: usable.firstOrNull() ?: return null
        val model = model() ?: return null
        // The phonemizer language is part of the engine's configuration, so each accent has its own engine.
        return speak("$PACKAGE:${voice.accent}", text, speaker = voice.speaker, speed = speed) {
            // English comes in two lexicons; each accent loads its own, next to the Chinese one the model expects.
            val english = if (voice.accent == "en-gb") "lexicon-gb-en.txt" else "lexicon-us-en.txt"
            val lexicons = listOf(english, "lexicon-zh.txt").map { File(folder, it) }.filter { it.exists() }.joinToString(",") { it.absolutePath }
            val kokoro = OfflineTtsKokoroModelConfig(
                model = model.absolutePath,
                voices = File(folder, "voices.bin").absolutePath,
                tokens = File(folder, "tokens.txt").absolutePath,
                dataDir = File(folder, "espeak-ng-data").absolutePath,
                lexicon = lexicons,
                lang = voice.accent,
                dictDir = File(folder, "dict").takeIf { it.exists() }?.absolutePath.orEmpty(),
            )
            OfflineTts(config = OfflineTtsConfig(model = OfflineTtsModelConfig(kokoro = kokoro, numThreads = 4, debug = false, provider = "cpu")))
        }
    }

    private fun isInstalled(): Boolean = model() != null && File(folder, "voices.bin").exists()

    private fun model(): File? = folder.listFiles()?.firstOrNull { it.name.endsWith(".onnx") }

    /** [speaker] is the voice's index in the model; [accent] the phonemizer language for it. */
    private class Voice(val id: String, val speaker: Int, val language: String, val accent: String, val label: String)

    private companion object {
        const val PACKAGE = "kokoro-v1.0"
        const val ARCHIVE_BYTES = 132_000_000L

        /** The model's speakers in its own order; only the languages the app teaches are offered. */
        private val SPEAKERS = listOf(
            "af_alloy", "af_aoede", "af_bella", "af_heart", "af_jessica", "af_kore", "af_nicole", "af_nova", "af_river", "af_sarah", "af_sky",
            "am_adam", "am_echo", "am_eric", "am_fenrir", "am_liam", "am_michael", "am_onyx", "am_puck", "am_santa",
            "bf_alice", "bf_emma", "bf_isabella", "bf_lily", "bm_daniel", "bm_fable", "bm_george", "bm_lewis",
            "ef_dora", "em_alex", "ff_siwis", "hf_alpha", "hf_beta", "hm_omega", "hm_psi", "if_sara", "im_nicola",
            "jf_alpha", "jf_gongitsune", "jf_nezumi", "jf_tebukuro", "jm_kumo", "pf_dora", "pm_alex", "pm_santa",
            "zf_xiaobei", "zf_xiaoni", "zf_xiaoxiao", "zf_xiaoyi", "zm_yunjian", "zm_yunxi", "zm_yunxia", "zm_yunyang",
        )
        private val PREFERRED = listOf("af_heart", "bf_emma", "ef_dora", "ff_siwis", "if_sara", "pf_dora")

        val VOICES: List<Voice> = SPEAKERS.mapIndexedNotNull { index, id ->
            val (language, accent, region) = when (id[0]) {
                'a' -> Triple("en", "en-us", "American")
                'b' -> Triple("en", "en-gb", "British")
                'e' -> Triple("es", "es", "Spanish")
                'f' -> Triple("fr", "fr-fr", "French")
                'i' -> Triple("it", "it", "Italian")
                'p' -> Triple("pt", "pt-br", "Brazilian")
                else -> return@mapIndexedNotNull null
            }
            if (id.endsWith("_santa")) return@mapIndexedNotNull null
            val name = id.substringAfter('_').replaceFirstChar { it.uppercase() }
            Voice(id, index, language, accent, "$name ($region, ${if (id[1] == 'f') "female" else "male"})")
        }.sortedBy { if (it.id in PREFERRED) 0 else 1 }
    }
}
