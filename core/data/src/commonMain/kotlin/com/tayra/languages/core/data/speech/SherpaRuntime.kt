package com.tayra.languages.core.data.speech

/** One synthesis in sherpa-onnx. Paths are absolute; empty strings mean "not used". */
data class SherpaRequest(
    /** Names the model configuration; a runtime keeps the last configuration it loaded. */
    val key: String,
    val kokoro: Boolean,
    val model: String,
    val tokens: String,
    val dataDir: String,
    val voices: String = "",
    val lexicon: String = "",
    val lang: String = "",
    val dictDir: String = "",
    val speaker: Int,
    val speed: Float,
    val text: String,
)

/**
 * What a platform provides for the sherpa-onnx speech engines: files, the model downloads, and
 * the synthesis itself. Android runs sherpa-onnx through its Java API; iOS through a Swift
 * bridge to its C API.
 */
interface SherpaRuntime {
    /** The absolute folder that holds an engine's models, such as `<app files>/tts/piper`. */
    fun folder(engine: String): String

    /** The file names in [path], or null when it does not exist. */
    fun list(path: String): List<String>?

    /**
     * Downloads a `.tar.bz2` model archive and unpacks it as [destination], dropping the archive's
     * top folder. [onDownload] reports bytes done and total; [onUnpack] marks the switch to unpacking.
     */
    suspend fun installArchive(url: String, destination: String, onDownload: (Long, Long) -> Unit, onUnpack: () -> Unit)

    /** Deletes [path], first releasing a loaded model that lives inside it. */
    suspend fun delete(path: String)

    /** WAV audio for [request]. Throws on failure. */
    suspend fun synthesize(request: SherpaRequest): ByteArray
}

/** 16-bit mono WAV from float samples in [-1, 1]. */
object Wav {
    fun encode(samples: FloatArray, sampleRate: Int): ByteArray {
        val dataBytes = samples.size * 2
        val out = ByteArray(44 + dataBytes)
        var i = 0
        fun ascii(s: String) { for (c in s) out[i++] = c.code.toByte() }
        fun int(v: Int) { for (b in 0 until 4) out[i++] = (v ushr (8 * b)).toByte() }
        fun short(v: Int) { out[i++] = v.toByte(); out[i++] = (v ushr 8).toByte() }
        ascii("RIFF"); int(36 + dataBytes); ascii("WAVE")
        ascii("fmt "); int(16); short(1); short(1); int(sampleRate); int(sampleRate * 2); short(2); short(16)
        ascii("data"); int(dataBytes)
        for (s in samples) short((s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt())
        return out
    }
}
