package com.tayra.languages.core.data.speech

import co.touchlab.kermit.Logger
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.media.WhisperLanguages
import com.tayra.languages.core.domain.practice.Heard
import com.tayra.languages.core.domain.practice.SpeechRecognition
import com.tayra.languages.core.domain.practice.SpeechRecognitionSetup
import com.tayra.languages.core.domain.practice.levelOfDecibels
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
import kotlin.math.log10
import kotlin.math.sqrt

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

    private val levelState = MutableStateFlow(0f)
    override val level: StateFlow<Float> = levelState

    override suspend fun isAvailable(languageCode: String): Boolean = whisperCode(languageCode) != null && downloadedModel() != null && runtimeReady()

    override suspend fun models(): List<RecognitionModel> {
        // A runtime set up for the other model only needs the model.
        val runtime = if (runtimeReady()) 0 else (if (python.isInstalled) 0 else python.downloadSize) + PIP_DOWNLOAD_SIZE
        return Whisper.entries.map { RecognitionModel(it.quality, runtime + it.size, withContext(Dispatchers.IO) { downloaded(it) }, runtime, it.model) }
    }

    override fun supports(languageCode: String): Boolean = whisperCode(languageCode) != null

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

    override suspend fun listen(languageCode: String, hint: String?, longPauses: Boolean, onRecorded: () -> Unit): Heard = withContext(Dispatchers.IO) {
        val language = whisperCode(languageCode) ?: return@withContext Heard.Refused
        val whisper = downloadedModel() ?: return@withContext Heard.Refused
        stopping = false
        // The microphone opens first: the worker, which also reads aloud, may be busy, and the first words must not wait for it.
        when (val recording = record()) {
            Recording.Refused -> Heard.Refused
            Recording.Muted -> {
                Logger.w { "the microphone gave only silence" }
                Heard.Muted
            }
            is Recording.Said -> {
                onRecorded()
                // It can be restarting: worth another try, not a refusal.
                if (!runtimeReady()) {
                    Logger.w { "speech recognition is not ready" }
                    return@withContext Heard.Failed
                }
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
                    Heard.Failed
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
        object Muted : Recording
        class Said(val pcm: ByteArray) : Recording
    }

    /** The microphone until [SpeechEndpoint] or [stop] ends it; refused when there is none to open, muted when it gives only zeros. */
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
                levelState.value = levelOf(chunk, read)
                pcm.write(chunk, 0, read)
                val state = endpoint.add(chunk, read)
                when {
                    state == SpeechEndpoint.State.MUTED -> return Recording.Muted
                    state == SpeechEndpoint.State.FULL || stopping -> return Recording.Said(pcm.toByteArray())
                }
            }
        } finally {
            levelState.value = 0f
            line.stop()
            line.close()
        }
    }

    /** How loud [read] bytes of 16-bit little-endian samples are, as a [level]. */
    private fun levelOf(chunk: ByteArray, read: Int): Float {
        var sum = 0.0
        val samples = read / 2
        for (i in 0 until samples) {
            val sample = (chunk[2 * i].toInt() and 0xFF) or (chunk[2 * i + 1].toInt() shl 8)
            sum += sample.toDouble() * sample
        }
        val rms = sqrt(sum / samples.coerceAtLeast(1)) / Short.MAX_VALUE
        return levelOfDecibels(if (rms > 0) (20 * log10(rms)).toFloat() else -100f)
    }

    /** The faster-whisper name of the model that hears (see [downloadedModel]), and the folder of the models, for the video transcriber. */
    internal suspend fun hearingModel(): Pair<String, File>? = downloadedModel()?.let { it.model to models }

    private fun model(whisper: Whisper) = arrayOf("engine" to ENGINE, "model" to whisper.model, "models" to models.absolutePath)

    private fun downloaded(whisper: Whisper): Boolean = models.walkTopDown().any { it.name == "model.bin" && whisper.repo in it.path }

    private suspend fun runtimeReady(): Boolean = runCatching { worker.request(STATUS_TIMEOUT_MS, "cmd" to "status", "engine" to ENGINE) }.isSuccess

    /** The model that hears: the one chosen in the settings where it is downloaded, else the other; null with neither. */
    private suspend fun downloadedModel(): Whisper? {
        val chosen = Whisper.entries.firstOrNull { it.quality.name == settings.current.speechRecognitionQuality } ?: Whisper.SMALL
        return withContext(Dispatchers.IO) { (listOf(chosen) + (Whisper.entries - chosen)).firstOrNull { downloaded(it) } }
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

        fun whisperCode(code: String): String? = WhisperLanguages.code(code)
    }
}
