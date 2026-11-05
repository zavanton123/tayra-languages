package com.tayra.languages.core.data.speech

import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.CueNormalizer
import com.tayra.languages.core.domain.media.HeardSegment
import com.tayra.languages.core.domain.media.TranscriptCues
import com.tayra.languages.core.domain.media.TranscriptionException
import com.tayra.languages.core.domain.media.TranscriptionModel
import com.tayra.languages.core.domain.media.TranscriptionStage
import com.tayra.languages.core.domain.media.VideoTranscriber
import com.tayra.languages.core.domain.media.WhisperLanguages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The files of one Whisper model in sherpa-onnx, with the voice detector that cuts the sound into what is said. */
data class WhisperFiles(val encoder: String, val decoder: String, val tokens: String, val vad: String, val language: String)

/**
 * What a phone does natively for [SherpaVideoTranscriber]: Android through sherpa-onnx's Java API and
 * its media codecs, iOS through Swift. Failures throw [TranscriptionException] with a message to show.
 */
interface SherpaTranscription {
    /** Downloads [url] to the file [destination], reporting bytes done and total. */
    suspend fun download(url: String, destination: String, onDownload: (Long, Long) -> Unit)

    /** Writes the sound of the media at [url] as 16 kHz mono 16-bit little-endian samples into the file [output]. */
    suspend fun decodeAudio(url: String, output: String, onProgress: (Float?) -> Unit)

    /** What Whisper hears in the samples of [pcm], cut where the voice detector hears speech end. */
    suspend fun recognize(model: WhisperFiles, pcm: String, onProgress: (Float) -> Unit): List<HeardSegment>

    /** A path for a temporary file named [name]. */
    fun temporaryFile(name: String): String

    fun deleteFile(path: String)

    /** Moves the file [from] to [to], replacing what is there. */
    fun moveFile(from: String, to: String)
}

/**
 * Video transcription on a phone: Whisper's base or small model in sherpa-onnx, downloaded on demand
 * beside the speech voices, with Silero's voice detector to cut the sound into lines.
 */
class SherpaVideoTranscriber(private val runtime: SherpaRuntime, private val platform: SherpaTranscription) : VideoTranscriber {
    private val progressState = MutableStateFlow<String?>(null)
    override val installProgress: StateFlow<String?> = progressState

    private val folder: String get() = runtime.folder(ENGINE)

    private enum class Model(val id: String, val title: String, val archiveSize: Long, val note: String) {
        BASE("base", "Whisper base", 207_557_382L, "Quicker"),
        SMALL("small", "Whisper small", 639_387_718L, "More accurate"),
    }

    private fun installed(model: Model): Boolean {
        val files = runtime.list("$folder/${model.id}").orEmpty()
        return model.files().all { it in files } && runtime.list(folder).orEmpty().contains(VAD_FILE)
    }

    private fun Model.files() = listOf("$id-encoder.int8.onnx", "$id-decoder.int8.onnx", "$id-tokens.txt")

    override suspend fun models(): List<TranscriptionModel> {
        val vad = if (runtime.list(folder).orEmpty().contains(VAD_FILE)) 0 else VAD_SIZE
        return Model.entries.map { TranscriptionModel(it.id, it.title, it.archiveSize + vad, installed(it), it.note) }
    }

    override suspend fun install(model: TranscriptionModel) {
        val chosen = Model.entries.first { it.id == model.id }
        try {
            if (!runtime.list(folder).orEmpty().contains(VAD_FILE)) {
                progressState.value = "Downloading the voice detector…"
                // Fetched beside its place first, so a broken download never counts as the detector.
                val partial = "$folder/$VAD_FILE.part"
                platform.download(VAD_URL, partial) { _, _ -> }
                platform.moveFile(partial, "$folder/$VAD_FILE")
            }
            runtime.installArchive(
                "$MODELS/sherpa-onnx-whisper-${chosen.id}.tar.bz2",
                "$folder/${chosen.id}",
                onDownload = { done, total -> progressState.value = "Downloading Whisper (${done / 1_000_000} of ${(if (total > 0) total else chosen.archiveSize) / 1_000_000} MB)…" },
                onUnpack = { progressState.value = "Unpacking Whisper…" },
            )
            // The archive also holds the full-precision model and test recordings: only the int8 files are used.
            val keep = chosen.files().toSet()
            runtime.list("$folder/${chosen.id}").orEmpty().filter { it !in keep }.forEach { runtime.delete("$folder/${chosen.id}/$it") }
        } finally {
            progressState.value = null
        }
    }

    override suspend fun remove(model: TranscriptionModel) {
        runtime.delete("$folder/${model.id}")
    }

    override suspend fun transcribe(url: String, languageCode: String, onProgress: (TranscriptionStage, Float?) -> Unit): List<Cue> {
        val language = WhisperLanguages.code(languageCode) ?: throw TranscriptionException("Whisper does not write down this language.")
        // The better model when both are there.
        val model = Model.entries.reversed().firstOrNull { installed(it) } ?: throw TranscriptionException("Download a Whisper model first.")
        val dir = "$folder/${model.id}"
        val files = WhisperFiles("$dir/${model.id}-encoder.int8.onnx", "$dir/${model.id}-decoder.int8.onnx", "$dir/${model.id}-tokens.txt", "$folder/$VAD_FILE", language)
        val pcm = platform.temporaryFile("tayra-transcription.pcm")
        try {
            onProgress(TranscriptionStage.AUDIO, null)
            platform.decodeAudio(url, pcm) { onProgress(TranscriptionStage.AUDIO, it) }
            onProgress(TranscriptionStage.SPEECH, 0f)
            val heard = platform.recognize(files, pcm) { onProgress(TranscriptionStage.SPEECH, it) }
            return CueNormalizer.normalize(TranscriptCues.of(heard))
        } finally {
            platform.deleteFile(pcm)
        }
    }

    private companion object {
        const val ENGINE = "whisper"
        const val MODELS = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models"
        const val VAD_URL = "$MODELS/silero_vad.onnx"
        const val VAD_FILE = "silero_vad.onnx"
        const val VAD_SIZE = 643_854L
    }
}
