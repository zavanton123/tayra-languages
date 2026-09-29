package com.tayra.languages.core.data.speech

import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What Piper and Kokoro share on phones: sherpa-onnx models downloaded as archives into one folder per engine. */
abstract class SherpaSpeechEngine(protected val runtime: SherpaRuntime, folderName: String) : LocalSpeechEngine {
    protected val root: String = runtime.folder(folderName)

    override val hasRuntimeSetup: Boolean = false

    protected val progressState = MutableStateFlow<String?>(null)
    override val progress: StateFlow<String?> = progressState

    override suspend fun isReady(): Boolean = true

    override suspend fun setUp(): String = status()

    protected fun path(vararg parts: String): String = (listOf(root) + parts).joinToString("/")

    protected suspend fun install(url: String, name: String, label: String) {
        try {
            runtime.installArchive(
                url, path(name),
                onDownload = { done, total -> progressState.value = "Downloading $label (${done / 1_000_000} of ${total / 1_000_000} MB)\u2026" },
                onUnpack = { progressState.value = "Unpacking $label\u2026" },
            )
        } finally {
            progressState.value = null
        }
    }

    protected fun onnx(folder: String): String? = runtime.list(folder)?.firstOrNull { it.endsWith(".onnx") }?.let { "$folder/$it" }

    protected companion object {
        const val MODELS = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"
    }
}

/** Piper on phones: the voices sherpa-onnx packages, in their compact copies. */
class SherpaPiperEngine(runtime: SherpaRuntime) : SherpaSpeechEngine(runtime, "piper") {

    override val engine: SpeechEngine = SpeechEngine.PIPER
    override val displayName: String = "Piper"
    override val description: String = "Piper is a neural speech engine that runs on this device with no network. It has voices for most of the languages the app teaches."
    override val packagesDescription: String = "One download per voice, about 20 MB each."

    private class Voice(val info: SherpaPiperVoice) {
        val key: String get() = info.key
        val quality: String = key.substringAfterLast('-').replace('_', ' ')
        val name: String = key.substringAfter('-').substringBeforeLast('-').replace('_', ' ').replaceFirstChar { it.uppercase() }

        /** The app's language code: Piper writes Norwegian as "no", the app uses "nb". */
        val appCode: String = key.substringBefore('_').let { if (it == "no") "nb" else it }
        val title: String get() = "$name \u00b7 $quality \u00b7 ${info.country}"
    }

    private val catalog: List<Voice> = SherpaPiperVoices.ALL.map(::Voice)
        .sortedWith(compareBy({ it.info.language }, { it.info.country }, { it.name }, { it.quality }))

    override suspend fun status(): String {
        val count = catalog.count { isInstalled(it.key) }
        return if (count == 0) "Piper is ready. No voices downloaded yet." else "Piper is ready with $count voice${if (count == 1) "" else "s"} downloaded."
    }

    override suspend fun packages(): List<SpeechPackage> =
        catalog.map { SpeechPackage(it.key, it.title, it.appCode, it.info.language, it.info.sizeBytes, isInstalled(it.key)) }

    override suspend fun installPackage(id: String) {
        val voice = catalog.firstOrNull { it.key == id } ?: error("unknown voice $id")
        install("$MODELS/vits-piper-${voice.key}-int8.tar.bz2", id, voice.name)
    }

    override suspend fun removePackage(id: String) = runtime.delete(path(id))

    override suspend fun voices(languageCode: String): List<SpeechVoice> =
        catalog.filter { it.appCode == languageCode && isInstalled(it.key) }
            .map { SpeechVoice(it.key, "${it.name} (${it.info.country}, ${it.quality})", languageCode) }

    override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
        val usable = voices(languageCode)
        val voice = usable.firstOrNull { it.id == voiceId } ?: usable.firstOrNull() ?: return null
        val folder = path(voice.id)
        val model = onnx(folder) ?: return null
        return runtime.synthesize(
            SherpaRequest(key = voice.id, kokoro = false, model = model, tokens = "$folder/tokens.txt", dataDir = "$folder/espeak-ng-data", speaker = 0, speed = speed, text = text),
        )
    }

    private fun isInstalled(key: String): Boolean = runtime.list(path(key))?.let { files -> "tokens.txt" in files && files.any { it.endsWith(".onnx") } } == true
}

/** Kokoro on phones: one model that speaks several languages, each with a handful of built-in voices. */
class SherpaKokoroEngine(runtime: SherpaRuntime) : SherpaSpeechEngine(runtime, "kokoro") {

    override val engine: SpeechEngine = SpeechEngine.KOKORO
    override val displayName: String = "Kokoro"
    override val description: String = "Kokoro is a high-quality neural voice that runs on this device with no network. It speaks English, Spanish, French, Italian and Portuguese; other languages fall back to the system voice. It needs a recent device to answer quickly."
    override val packagesDescription: String = "One download holds the model and all its voices."

    private val folder: String get() = path(PACKAGE)

    override suspend fun status(): String = if (isInstalled()) "Kokoro is ready. The model is downloaded." else "Kokoro is ready. The model is not downloaded yet."

    override suspend fun packages(): List<SpeechPackage> = listOf(
        SpeechPackage(PACKAGE, "Kokoro model with ${KokoroVoices.ALL.size} voices", languageCode = null, group = "English, Spanish, French, Italian, Portuguese", sizeBytes = ARCHIVE_BYTES, installed = isInstalled()),
    )

    override suspend fun installPackage(id: String) = install("$MODELS/kokoro-int8-multi-lang-v1_0.tar.bz2", PACKAGE, "the Kokoro model")

    override suspend fun removePackage(id: String) = runtime.delete(folder)

    override suspend fun voices(languageCode: String): List<SpeechVoice> =
        if (!isInstalled()) emptyList() else KokoroVoices.ALL.filter { it.language == languageCode }.map { SpeechVoice(it.id, it.label, languageCode) }

    override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? {
        if (!isInstalled()) return null
        val usable = KokoroVoices.ALL.filter { it.language == languageCode }
        val voice = usable.firstOrNull { it.id == voiceId } ?: usable.firstOrNull() ?: return null
        val model = onnx(folder) ?: return null
        val files = runtime.list(folder).orEmpty()
        // English comes in two lexicons; each accent loads its own, next to the Chinese one the model expects.
        val english = if (voice.accent == "en-gb") "lexicon-gb-en.txt" else "lexicon-us-en.txt"
        val lexicons = listOf(english, "lexicon-zh.txt").filter { it in files }.joinToString(",") { "$folder/$it" }
        // The phonemizer language is part of the engine's configuration, so each accent has its own engine.
        return runtime.synthesize(
            SherpaRequest(
                key = "$PACKAGE:${voice.accent}", kokoro = true, model = model, tokens = "$folder/tokens.txt",
                dataDir = "$folder/espeak-ng-data", voices = "$folder/voices.bin", lexicon = lexicons, lang = voice.accent,
                dictDir = if ("dict" in files) "$folder/dict" else "", speaker = voice.speaker, speed = speed, text = text,
            ),
        )
    }

    private fun isInstalled(): Boolean = runtime.list(folder)?.let { files -> "voices.bin" in files && files.any { it.endsWith(".onnx") } } == true

    private companion object {
        const val PACKAGE = "kokoro-v1.0"
        const val ARCHIVE_BYTES = 132_000_000L
    }
}

/** A Kokoro voice: [speaker] is its index in the model, [accent] the phonemizer language for it. */
class KokoroVoice(val id: String, val speaker: Int, val language: String, val accent: String, val label: String)

/** The voices of the multilingual Kokoro model that speak languages the app teaches. */
object KokoroVoices {
    /** The model's speakers in its own order. */
    private val SPEAKERS = listOf(
        "af_alloy", "af_aoede", "af_bella", "af_heart", "af_jessica", "af_kore", "af_nicole", "af_nova", "af_river", "af_sarah", "af_sky",
        "am_adam", "am_echo", "am_eric", "am_fenrir", "am_liam", "am_michael", "am_onyx", "am_puck", "am_santa",
        "bf_alice", "bf_emma", "bf_isabella", "bf_lily", "bm_daniel", "bm_fable", "bm_george", "bm_lewis",
        "ef_dora", "em_alex", "ff_siwis", "hf_alpha", "hf_beta", "hm_omega", "hm_psi", "if_sara", "im_nicola",
        "jf_alpha", "jf_gongitsune", "jf_nezumi", "jf_tebukuro", "jm_kumo", "pf_dora", "pm_alex", "pm_santa",
        "zf_xiaobei", "zf_xiaoni", "zf_xiaoxiao", "zf_xiaoyi", "zm_yunjian", "zm_yunxi", "zm_yunxia", "zm_yunyang",
    )
    private val PREFERRED = listOf("af_heart", "bf_emma", "ef_dora", "ff_siwis", "if_sara", "pf_dora")

    val ALL: List<KokoroVoice> = SPEAKERS.mapIndexedNotNull { index, id ->
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
        KokoroVoice(id, index, language, accent, "$name ($region, ${if (id[1] == 'f') "female" else "male"})")
    }.sortedBy { if (it.id in PREFERRED) 0 else 1 }
}
