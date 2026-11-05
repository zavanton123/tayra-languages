package com.tayra.languages.core.data.speech

import co.touchlab.kermit.Logger
import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.media.Cue
import com.tayra.languages.core.domain.media.CueNormalizer
import com.tayra.languages.core.domain.media.HeardSegment
import com.tayra.languages.core.domain.media.HeardWord
import com.tayra.languages.core.domain.media.TranscriptCues
import com.tayra.languages.core.domain.media.TranscriptionException
import com.tayra.languages.core.domain.media.TranscriptionModel
import com.tayra.languages.core.domain.media.TranscriptionStage
import com.tayra.languages.core.domain.media.VideoTranscriber
import com.tayra.languages.core.domain.media.WhisperLanguages
import com.tayra.languages.core.domain.practice.RecognitionQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File

/**
 * Video transcription on the desktop: faster-whisper, with the models of the Speech settings, in a
 * speech worker of its own (so reading aloud goes on meanwhile), reading the video's sound from its
 * address through PyAV. Progress and cancelling go through two small files beside each other.
 */
class DesktopVideoTranscriber(
    private val python: ManagedPython,
    private val recognition: DesktopSpeechRecognition,
    private val worker: TtsWorker = TtsWorker(python),
) : VideoTranscriber {
    override val installProgress: StateFlow<String?> = recognition.progress

    override suspend fun models(): List<TranscriptionModel> = recognition.models().map {
        TranscriptionModel(it.quality.name, "Whisper ${it.name}", it.downloadSize, it.installed, if (it.quality == RecognitionQuality.HIGH) "More accurate" else "Quicker")
    }

    override suspend fun install(model: TranscriptionModel) {
        recognition.install(RecognitionQuality.valueOf(model.id))
        // Started before the packages were there, the worker would not find them.
        worker.restart()
    }

    override suspend fun remove(model: TranscriptionModel) = recognition.remove(RecognitionQuality.valueOf(model.id))

    override suspend fun transcribe(url: String, languageCode: String, onProgress: (TranscriptionStage, Float?) -> Unit): List<Cue> {
        val language = WhisperLanguages.code(languageCode) ?: throw TranscriptionException("Whisper does not write down this language.")
        val (model, folder) = recognition.hearingModel()?.takeIf { python.isInstalled } ?: throw TranscriptionException("Download a Whisper model first.")
        val progress = withContext(Dispatchers.IO) { File.createTempFile("tayra-transcription", ".progress") }
        val cancel = File(progress.path + ".cancel")
        onProgress(TranscriptionStage.AUDIO, null)
        val reply = try {
            coroutineScope {
                var finished = false
                val watch = launch(Dispatchers.IO) {
                    try {
                        while (true) {
                            delay(PROGRESS_EVERY_MS)
                            val (stage, fraction) = progress.readText().split(' ').takeIf { it.size == 2 } ?: continue
                            onProgress(if (stage == "speech") TranscriptionStage.SPEECH else TranscriptionStage.AUDIO, fraction.toFloatOrNull())
                        }
                    } finally {
                        // Left waiting, this was cancelled: the worker stops at its next look at the file.
                        if (!finished) cancel.createNewFile()
                    }
                }
                try {
                    worker.request(
                        TIMEOUT_MS, "cmd" to "transcribe_media", "engine" to "whisper", "model" to model, "models" to folder.absolutePath,
                        "lang" to language, "media" to url, "progress" to progress.absolutePath,
                    )
                } finally {
                    finished = true
                    watch.cancel()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "transcription failed" }
            val message = e.message.orEmpty()
            throw TranscriptionException(
                when {
                    "has no sound" in message -> "The video has no sound."
                    "could not be opened" in message -> "The video could not be read from its address."
                    "No module named" in message -> "Download a Whisper model first."
                    else -> "The transcription failed."
                },
            )
        } finally {
            withContext(Dispatchers.IO) {
                progress.delete()
                File(progress.path + ".part").delete()
                cancel.delete()
            }
        }
        return CueNormalizer.normalize(TranscriptCues.of(segments(reply)))
    }

    private companion object {
        const val PROGRESS_EVERY_MS = 300L

        /** A film of a few hours, heard by the large model. */
        const val TIMEOUT_MS = 6 * 3_600_000L

        fun segments(reply: JsonObject): List<HeardSegment> = reply["segments"]?.jsonArray.orEmpty().map { element ->
            val o = element as JsonObject
            HeardSegment(
                beginMs = o["start"]!!.jsonPrimitive.long,
                endMs = o["end"]!!.jsonPrimitive.long,
                text = o["text"]?.jsonPrimitive?.content.orEmpty(),
                words = (o["words"] as? JsonArray).orEmpty().map { w ->
                    val word = w.jsonArray
                    HeardWord(word[0].jsonPrimitive.long, word[1].jsonPrimitive.long, word[2].jsonPrimitive.content)
                },
            )
        }
    }
}
