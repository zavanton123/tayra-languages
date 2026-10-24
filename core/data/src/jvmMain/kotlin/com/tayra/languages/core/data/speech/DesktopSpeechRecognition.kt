package com.tayra.languages.core.data.speech

import co.touchlab.kermit.Logger
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.practice.Heard
import com.tayra.languages.core.domain.practice.SpeechRecognition
import com.tayra.languages.core.domain.practice.SpeechRecognitionSetup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.coroutines.coroutineContext

/**
 * Speech heard on the desktop: the microphone recorded until the learner pauses, then Whisper's
 * small model (faster-whisper, in the speech worker) writes down what was said, with no network
 * once it is set up from the Speech settings.
 */
class DesktopSpeechRecognition(
    private val python: ManagedPython,
    private val worker: TtsWorker,
    private val models: File = File(DatabaseDriverFactory.dataDirectory(), "stt/whisper"),
) : SpeechRecognition, SpeechRecognitionSetup {
    private val progressState = MutableStateFlow<String?>(null)
    override val progress: StateFlow<String?> = progressState

    @Volatile
    private var stopping = false

    override suspend fun isAvailable(languageCode: String): Boolean = whisperCode(languageCode) != null && isReady()

    override suspend fun isReady(): Boolean = withContext(Dispatchers.IO) { modelDownloaded() } &&
        runCatching { worker.request(STATUS_TIMEOUT_MS, "cmd" to "status", "engine" to ENGINE) }.isSuccess

    override suspend fun downloadSize(): Long = (if (python.isInstalled) 0 else python.downloadSize) + PIP_DOWNLOAD_SIZE + MODEL_SIZE

    override suspend fun setUp(): Unit = withContext(Dispatchers.IO) {
        try {
            python.ensure { progressState.value = it }
            python.pip(listOf("faster-whisper"), "Installing speech recognition") { progressState.value = it }
            worker.restart()
            coroutineScope {
                // The model comes down inside the worker, which says nothing until it is done: the folder's growth is the progress.
                val watch = launch {
                    while (true) {
                        val done = models.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                        progressState.value = "Downloading the recognition model (${done / 1_000_000} of ${MODEL_SIZE / 1_000_000} MB)…"
                        delay(PROGRESS_EVERY_MS)
                    }
                }
                try {
                    worker.request(DOWNLOAD_TIMEOUT_MS, "cmd" to "prepare", *model())
                } finally {
                    watch.cancel()
                }
            }
        } finally {
            progressState.value = null
        }
    }

    override suspend fun listen(languageCode: String): Heard = withContext(Dispatchers.IO) {
        val language = whisperCode(languageCode) ?: return@withContext Heard.Refused
        stopping = false
        when (val recording = record()) {
            Recording.Refused -> Heard.Refused
            Recording.Silent -> Heard.Nothing
            is Recording.Said -> {
                val wav = File.createTempFile("tayra-heard", ".wav")
                try {
                    AudioSystem.write(AudioInputStream(ByteArrayInputStream(recording.pcm), FORMAT, recording.pcm.size / 2L), AudioFileFormat.Type.WAVE, wav)
                    val text = worker.request(TRANSCRIBE_TIMEOUT_MS, "cmd" to "transcribe", "lang" to language, "audio" to wav.absolutePath, *model())["text"]?.jsonPrimitive?.content?.trim()
                    if (text.isNullOrEmpty()) Heard.Nothing else Heard.Words(text)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.w(e) { "speech recognition failed" }
                    Heard.Refused
                } finally {
                    wav.delete()
                }
            }
        }
    }

    override fun stop() {
        stopping = true
    }

    private sealed interface Recording {
        object Refused : Recording
        object Silent : Recording
        class Said(val pcm: ByteArray) : Recording
    }

    /** The microphone until [SpeechEndpoint] or [stop] ends it; refused when there is none to open, or it gives only zeros. */
    private suspend fun record(): Recording {
        val line = try {
            AudioSystem.getTargetDataLine(FORMAT).apply {
                open(FORMAT)
                start()
            }
        } catch (e: Exception) {
            Logger.w(e) { "no microphone" }
            return Recording.Refused
        }
        val pcm = ByteArrayOutputStream()
        val endpoint = SpeechEndpoint(FORMAT.sampleRate.toInt())
        val chunk = ByteArray(CHUNK_BYTES)
        try {
            while (true) {
                coroutineContext.ensureActive()
                val read = line.read(chunk, 0, chunk.size)
                if (read <= 0) continue
                pcm.write(chunk, 0, read)
                val state = endpoint.add(chunk, read)
                when {
                    state == SpeechEndpoint.State.MUTED -> return Recording.Refused
                    state == SpeechEndpoint.State.SAID || (stopping && endpoint.spoke) -> return Recording.Said(pcm.toByteArray())
                    state == SpeechEndpoint.State.SILENT || stopping -> return Recording.Silent
                }
            }
        } finally {
            line.stop()
            line.close()
        }
    }

    private fun model() = arrayOf("engine" to ENGINE, "model" to MODEL, "models" to models.absolutePath)

    private fun modelDownloaded(): Boolean = models.walkTopDown().any { it.name == "model.bin" }

    private companion object {
        const val ENGINE = "whisper"
        const val MODEL = "small"
        val FORMAT = AudioFormat(16_000f, 16, 1, true, false)

        /** A tenth of a second of [FORMAT]. */
        const val CHUNK_BYTES = 3_200
        const val PIP_DOWNLOAD_SIZE = 90_000_000L
        const val MODEL_SIZE = 484_000_000L
        const val STATUS_TIMEOUT_MS = 60_000L
        const val TRANSCRIBE_TIMEOUT_MS = 120_000L
        const val DOWNLOAD_TIMEOUT_MS = 3_600_000L
        const val PROGRESS_EVERY_MS = 500L

        /** The languages Whisper writes down, by the app's codes; Norwegian Bokmål is Whisper's "no". */
        val LANGUAGES = setOf(
            "af", "am", "ar", "as", "az", "ba", "be", "bg", "bn", "bo", "br", "bs", "ca", "cs", "cy", "da", "de", "el", "en", "es", "et", "eu",
            "fa", "fi", "fo", "fr", "gl", "gu", "ha", "he", "hi", "hr", "ht", "hu", "hy", "id", "is", "it", "ja", "jw", "ka", "kk", "km", "kn",
            "ko", "la", "lb", "ln", "lo", "lt", "lv", "mg", "mi", "mk", "ml", "mn", "mr", "ms", "mt", "my", "ne", "nl", "nn", "no", "oc", "pa",
            "pl", "ps", "pt", "ro", "ru", "sa", "sd", "si", "sk", "sl", "sn", "so", "sq", "sr", "su", "sv", "sw", "ta", "te", "tg", "th", "tk",
            "tl", "tr", "tt", "uk", "ur", "uz", "vi", "yi", "yo", "zh", "yue",
        )

        fun whisperCode(code: String): String? = (if (code == "nb") "no" else code).takeIf { it in LANGUAGES }
    }
}
