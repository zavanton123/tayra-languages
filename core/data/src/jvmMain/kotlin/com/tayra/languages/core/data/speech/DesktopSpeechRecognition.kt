package com.tayra.languages.core.data.speech

import co.touchlab.kermit.Logger
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.practice.Heard
import com.tayra.languages.core.domain.practice.SpeechRecognition
import com.tayra.languages.core.domain.practice.SpeechRecognitionSetup
import com.tayra.languages.core.domain.practice.RecognitionModel
import com.tayra.languages.core.domain.practice.RecognitionQuality
import com.tayra.languages.core.domain.settings.SettingsRepository
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
import java.nio.file.Files
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.coroutines.coroutineContext

/**
 * Speech heard on the desktop: the microphone recorded until the learner pauses, then Whisper
 * (faster-whisper, in the speech worker) writes down what was said: its small model, quick, or its
 * large turbo one, slower but better at an accent and told what is expected; with no network once
 * one is downloaded from the Speech settings.
 */
class DesktopSpeechRecognition(
    private val python: ManagedPython,
    private val worker: TtsWorker,
    private val settings: SettingsRepository,
    private val models: File = File(DatabaseDriverFactory.dataDirectory(), "stt/whisper"),
) : SpeechRecognition, SpeechRecognitionSetup {
    private val progressState = MutableStateFlow<String?>(null)
    override val progress: StateFlow<String?> = progressState

    @Volatile
    private var stopping = false

    override suspend fun isAvailable(languageCode: String): Boolean = whisperCode(languageCode) != null && hearing() != null

    override suspend fun models(): List<RecognitionModel> {
        // A runtime set up for the other model only needs the model.
        val runtime = if (runtimeReady()) 0 else (if (python.isInstalled) 0 else python.downloadSize) + PIP_DOWNLOAD_SIZE
        return Whisper.entries.map { RecognitionModel(it.quality, runtime + it.size, withContext(Dispatchers.IO) { downloaded(it) }) }
    }

    override suspend fun install(quality: RecognitionQuality): Unit = withContext(Dispatchers.IO) {
        val whisper = Whisper.of(quality)
        try {
            if (!runtimeReady()) {
                python.ensure { progressState.value = it }
                python.pip(listOf("faster-whisper"), "Installing speech recognition") { progressState.value = it }
                worker.restart()
            }
            coroutineScope {
                // The model comes down inside the worker, which says nothing until it is done: the folder's growth is the progress.
                val watch = launch {
                    while (true) {
                        // The cache's snapshot links point at its blobs: counted once, as blobs.
                        val done = models.walkTopDown().filter { it.isFile && whisper.repo in it.path && !Files.isSymbolicLink(it.toPath()) }.sumOf { it.length() }
                        progressState.value = "Downloading the recognition model (${done / 1_000_000} of ${whisper.size / 1_000_000} MB)…"
                        delay(PROGRESS_EVERY_MS)
                    }
                }
                try {
                    worker.request(DOWNLOAD_TIMEOUT_MS, "cmd" to "prepare", *model(whisper))
                } finally {
                    watch.cancel()
                }
            }
        } finally {
            progressState.value = null
        }
    }

    override suspend fun remove(quality: RecognitionQuality): Unit = withContext(Dispatchers.IO) {
        val whisper = Whisper.of(quality)
        models.listFiles { file -> file.isDirectory && whisper.repo in file.name }?.forEach { it.deleteRecursively() }
    }

    override suspend fun listen(languageCode: String, hint: String?, onRecorded: () -> Unit): Heard = withContext(Dispatchers.IO) {
        val language = whisperCode(languageCode) ?: return@withContext Heard.Refused
        val whisper = hearing() ?: return@withContext Heard.Refused
        stopping = false
        when (val recording = record()) {
            Recording.Refused -> Heard.Refused
            Recording.Silent -> Heard.Nothing
            is Recording.Said -> {
                onRecorded()
                val wav = File.createTempFile("tayra-heard", ".wav")
                try {
                    AudioSystem.write(AudioInputStream(ByteArrayInputStream(recording.pcm), FORMAT, recording.pcm.size / 2L), AudioFileFormat.Type.WAVE, wav)
                    // Only the large model gains from the hint: the small one, given it, makes up words.
                    val prompt = if (whisper.hinted) hint.orEmpty() else ""
                    val text = worker.request(TRANSCRIBE_TIMEOUT_MS, "cmd" to "transcribe", "lang" to language, "audio" to wav.absolutePath, "prompt" to prompt, *model(whisper))["text"]?.jsonPrimitive?.content?.trim()
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

    private fun model(whisper: Whisper) = arrayOf("engine" to ENGINE, "model" to whisper.model, "models" to models.absolutePath)

    private fun downloaded(whisper: Whisper): Boolean = models.walkTopDown().any { it.name == "model.bin" && whisper.repo in it.path }

    private suspend fun runtimeReady(): Boolean = runCatching { worker.request(STATUS_TIMEOUT_MS, "cmd" to "status", "engine" to ENGINE) }.isSuccess

    /** The model that hears: the one chosen in the settings where it is downloaded, else the other; null with neither, or no runtime. */
    private suspend fun hearing(): Whisper? {
        val chosen = Whisper.entries.firstOrNull { it.quality.name == settings.current.speechRecognitionQuality } ?: Whisper.SMALL
        val there = withContext(Dispatchers.IO) { (listOf(chosen) + (Whisper.entries - chosen)).firstOrNull { downloaded(it) } }
        return there?.takeIf { runtimeReady() }
    }

    /** The two models offered, as faster-whisper names them and the folder of the Hugging Face cache it downloads them into. */
    private enum class Whisper(val quality: RecognitionQuality, val model: String, val repo: String, val size: Long, val hinted: Boolean) {
        /** Under a second for a phrase on an M4 Pro; told what is expected, it makes words up, so it is not. */
        SMALL(RecognitionQuality.MEDIUM, "small", "faster-whisper-small", 486_000_000L, hinted = false),

        /** About three seconds for a phrase; far better at a learner's accent, the more so told what is expected. */
        LARGE_TURBO(RecognitionQuality.HIGH, "large-v3-turbo", "faster-whisper-large-v3-turbo", 1_622_000_000L, hinted = true),
        ;

        companion object {
            fun of(quality: RecognitionQuality) = entries.first { it.quality == quality }
        }
    }

    private companion object {
        const val ENGINE = "whisper"
        val FORMAT = AudioFormat(16_000f, 16, 1, true, false)

        /** A tenth of a second of [FORMAT]. */
        const val CHUNK_BYTES = 3_200
        const val PIP_DOWNLOAD_SIZE = 90_000_000L
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
