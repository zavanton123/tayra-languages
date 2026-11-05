package com.tayra.languages.core.data.speech

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import co.touchlab.kermit.Logger
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import com.tayra.languages.core.domain.media.HeardSegment
import com.tayra.languages.core.domain.media.TranscriptionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Video transcription natively on Android: Media3's Transformer reads the video from its address (any
 * stream ExoPlayer plays, HLS too), its audio processors mix the sound down to 16 kHz mono and hand
 * the samples over; sherpa-onnx's Silero detector and Whisper hear them.
 */
@OptIn(UnstableApi::class)
class AndroidSherpaTranscription(context: Context) : SherpaTranscription {
    private val app = context.applicationContext

    override suspend fun download(url: String, destination: String, onDownload: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        File(destination).parentFile?.mkdirs()
        FileDownloads.download(url, File(destination), onDownload)
    }

    override fun temporaryFile(name: String): String = File(app.cacheDir, name).absolutePath

    override fun deleteFile(path: String) {
        File(path).delete()
    }

    override fun moveFile(from: String, to: String) {
        val target = File(to)
        target.delete()
        if (!File(from).renameTo(target)) error("could not move $from")
    }

    override suspend fun decodeAudio(url: String, output: String, onProgress: (Float?) -> Unit) {
        // The Transformer still writes an AAC file of the sound; only the samples on their way there are kept.
        val encoded = File(app.cacheDir, "tayra-transcription.m4a")
        val samples = BufferedOutputStream(FileOutputStream(output), 1 shl 16)
        try {
            coroutineScope {
                withContext(Dispatchers.Main) {
                    val transformer = Transformer.Builder(app).build()
                    val mix = ChannelMixingAudioProcessor().apply {
                        // Every channel at an equal share: Media3's own tables stop at a few layouts.
                        for (channels in 1..8) putChannelMixingMatrix(ChannelMixingMatrix(channels, 1, FloatArray(channels) { 1f / channels }))
                    }
                    val resample = SonicAudioProcessor().apply { setOutputSampleRateHz(SAMPLE_RATE) }
                    val item = EditedMediaItem.Builder(MediaItem.fromUri(url))
                        .setRemoveVideo(true)
                        .setEffects(Effects(listOf<AudioProcessor>(mix, resample, PcmCapture(samples)), emptyList()))
                        .build()
                    val watch = launch {
                        val holder = ProgressHolder()
                        while (true) {
                            delay(PROGRESS_EVERY_MS)
                            val known = transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE
                            onProgress(if (known) holder.progress / 100f else null)
                        }
                    }
                    try {
                        suspendCancellableCoroutine { cont ->
                            transformer.addListener(object : Transformer.Listener {
                                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                    cont.resume(Unit)
                                }

                                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                    Logger.w(exportException) { "the video's sound could not be read" }
                                    cont.resumeWithException(TranscriptionException(problemOf(exportException)))
                                }
                            })
                            cont.invokeOnCancellation { transformer.cancel() }
                            transformer.start(item, encoded.absolutePath)
                        }
                    } finally {
                        watch.cancel()
                    }
                }
            }
        } finally {
            withContext(Dispatchers.IO) {
                samples.close()
                encoded.delete()
            }
        }
        if (File(output).length() == 0L) throw TranscriptionException("The video has no sound.")
    }

    override suspend fun recognize(model: WhisperFiles, pcm: String, onProgress: (Float) -> Unit): List<HeardSegment> = withContext(Dispatchers.Default) {
        val vad = Vad(
            null,
            VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = model.vad, threshold = 0.5f, minSilenceDuration = 0.4f, minSpeechDuration = 0.25f, windowSize = 512, maxSpeechDuration = MAX_LINE_SECONDS,
                ),
                sampleRate = SAMPLE_RATE, numThreads = 1,
            ),
        )
        val recognizer = OfflineRecognizer(
            null,
            OfflineRecognizerConfig(
                modelConfig = OfflineModelConfig(
                    whisper = OfflineWhisperModelConfig(encoder = model.encoder, decoder = model.decoder, language = model.language, task = "transcribe"),
                    tokens = model.tokens, numThreads = THREADS, modelType = "whisper",
                ),
            ),
        )
        val heard = mutableListOf<HeardSegment>()
        fun drain() {
            while (!vad.empty()) {
                val segment = vad.front()
                val stream = recognizer.createStream()
                try {
                    stream.acceptWaveform(segment.samples, SAMPLE_RATE)
                    recognizer.decode(stream)
                    val text = recognizer.getResult(stream).text.trim()
                    val begin = segment.start * 1000L / SAMPLE_RATE
                    heard += HeardSegment(begin, begin + segment.samples.size * 1000L / SAMPLE_RATE, text)
                } finally {
                    stream.release()
                }
                vad.pop()
            }
        }
        try {
            val file = File(pcm)
            val total = file.length().coerceAtLeast(1)
            var read = 0L
            file.inputStream().buffered(1 shl 16).use { input ->
                val bytes = ByteArray(CHUNK_SAMPLES * 2)
                while (true) {
                    ensureActive()
                    var n = 0
                    while (n < bytes.size) {
                        val got = input.read(bytes, n, bytes.size - n)
                        if (got < 0) break
                        n += got
                    }
                    if (n <= 0) break
                    read += n
                    val shorts = ByteBuffer.wrap(bytes, 0, n - n % 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    vad.acceptWaveform(FloatArray(shorts.remaining()) { shorts.get(it) / 32768f })
                    drain()
                    onProgress(read.toFloat() / total)
                }
            }
            vad.flush()
            drain()
            heard
        } finally {
            vad.release()
            recognizer.release()
        }
    }

    /** Passes the samples on unchanged and keeps a copy of them in [out]. */
    private class PcmCapture(private val out: BufferedOutputStream) : BaseAudioProcessor() {
        override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            return inputAudioFormat
        }

        override fun queueInput(inputBuffer: ByteBuffer) {
            val size = inputBuffer.remaining()
            if (size == 0) return
            val copy = ByteArray(size)
            inputBuffer.duplicate().get(copy)
            out.write(copy)
            replaceOutputBuffer(size).put(inputBuffer).flip()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val CHUNK_SAMPLES = 16_000
        const val THREADS = 4
        const val MAX_LINE_SECONDS = 12f
        const val PROGRESS_EVERY_MS = 300L

        fun problemOf(e: ExportException): String = when (e.errorCode) {
            ExportException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, ExportException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            ExportException.ERROR_CODE_IO_BAD_HTTP_STATUS, ExportException.ERROR_CODE_IO_FILE_NOT_FOUND,
            ExportException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            -> "The video could not be read from its address."
            else -> if (e.message.orEmpty().contains("audio", ignoreCase = true) && e.message.orEmpty().contains("track", ignoreCase = true)) "The video has no sound." else "The transcription failed."
        }
    }
}
