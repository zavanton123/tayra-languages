package com.tayra.languages.speech

import com.tayra.languages.core.data.speech.SherpaRequest
import com.tayra.languages.core.data.speech.SherpaRuntime
import com.tayra.languages.core.data.speech.SherpaTranscription
import com.tayra.languages.core.data.speech.WhisperFiles
import com.tayra.languages.core.domain.media.HeardSegment
import com.tayra.languages.core.domain.media.TranscriptionException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One synthesis, as the Swift side receives it; mirrors [SherpaRequest]. */
class SpeechSynthesisRequest(
    val key: String,
    val kokoro: Boolean,
    val model: String,
    val tokens: String,
    val dataDir: String,
    val voices: String,
    val lexicon: String,
    val lang: String,
    val ruleFsts: String,
    val speaker: Int,
    val speed: Float,
    val text: String,
)

/** One transcription, as the Swift side receives it: the Whisper model's files, the voice detector and the samples to hear. */
class WhisperRequest(val encoder: String, val decoder: String, val tokens: String, val vad: String, val language: String, val pcm: String)

/**
 * What the Swift side implements with sherpa-onnx's C API. Callbacks rather than suspend
 * functions, because Kotlin cannot call a suspend function that Swift implements. Every
 * completion receives null on success or an error message.
 */
interface OnDeviceSpeechBridge {
    /** Downloads [url] to the file [destination], reporting bytes done and total. */
    fun download(url: String, destination: String, progress: (Long, Long) -> Unit, completion: (String?) -> Unit)

    /** Unpacks a `.tar.bz2` [archive] into the folder [destination], dropping the archive's top folder. */
    fun extractTarBz2(archive: String, destination: String, completion: (String?) -> Unit)

    /** Synthesizes [request] into a WAV file at [output]. */
    fun synthesize(request: SpeechSynthesisRequest, output: String, completion: (String?) -> Unit)

    /** Releases a loaded model whose files live under [path]. */
    fun unload(path: String)

    /** Writes the sound of the media at [url] as 16 kHz mono 16-bit little-endian samples into the file [output]. */
    fun decodeAudio(url: String, output: String, progress: (Float?) -> Unit, completion: (String?) -> Unit)

    /** Hears the samples of [request], handing over each stretch of speech: begin and end in milliseconds, and its text. */
    fun transcribe(request: WhisperRequest, progress: (Float) -> Unit, heard: (Long, Long, String) -> Unit, completion: (String?) -> Unit)

    /** Stops the running [decodeAudio] or [transcribe]. */
    fun cancelTranscription()
}

/** The sherpa-onnx engines on iOS: files through Foundation, everything native through [bridge]. */
@OptIn(ExperimentalForeignApi::class)
class IosSherpaRuntime(private val bridge: OnDeviceSpeechBridge) : SherpaRuntime {
    private val files = NSFileManager.defaultManager

    private val base: String =
        (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).firstOrNull() as? String)
            ?: error("no Application Support folder")

    override fun folder(engine: String): String = "$base/tts/$engine".also { files.createDirectoryAtPath(it, true, null, null) }

    override fun list(path: String): List<String>? = files.contentsOfDirectoryAtPath(path, null)?.map { it as String }

    override suspend fun installArchive(url: String, destination: String, onDownload: (Long, Long) -> Unit, onUnpack: () -> Unit) {
        val name = destination.substringAfterLast('/')
        val archive = "${NSTemporaryDirectory()}tayra-$name.tar.bz2"
        val staging = "$destination.unpacking"
        try {
            call { done -> bridge.download(url, archive, onDownload, done) }
            onUnpack()
            files.removeItemAtPath(staging, null)
            call { done -> bridge.extractTarBz2(archive, staging, done) }
            files.removeItemAtPath(destination, null)
            if (!files.moveItemAtPath(staging, destination, null)) error("could not store $name")
        } finally {
            files.removeItemAtPath(archive, null)
            files.removeItemAtPath(staging, null)
        }
    }

    override suspend fun delete(path: String) {
        bridge.unload(path)
        files.removeItemAtPath(path, null)
    }

    override suspend fun synthesize(request: SherpaRequest): ByteArray {
        val output = "${NSTemporaryDirectory()}tayra-speech.wav"
        val r = request
        call { done ->
            bridge.synthesize(
                SpeechSynthesisRequest(r.key, r.kokoro, r.model, r.tokens, r.dataDir, r.voices, r.lexicon, r.lang, r.ruleFsts, r.speaker, r.speed, r.text),
                output, done,
            )
        }
        val data = NSData.dataWithContentsOfFile(output) ?: error("no audio was written")
        files.removeItemAtPath(output, null)
        val bytes = ByteArray(data.length.toInt())
        if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        return bytes
    }

    /** Waits for a bridge call whose completion carries an error message or null. */
    private suspend fun call(start: (done: (String?) -> Unit) -> Unit) = suspendCancellableCoroutine { cont ->
        start { error -> if (error == null) cont.resume(Unit) else cont.resumeWithException(IllegalStateException(error)) }
    }
}

/** Video transcription on iOS: files through Foundation, the sound and Whisper through [bridge], whose messages are shown as they are. */
@OptIn(ExperimentalForeignApi::class)
class IosSherpaTranscription(private val bridge: OnDeviceSpeechBridge) : SherpaTranscription {
    private val files = NSFileManager.defaultManager

    override suspend fun download(url: String, destination: String, onDownload: (Long, Long) -> Unit) = suspendCancellableCoroutine { cont ->
        bridge.download(url, destination, onDownload) { error ->
            if (error == null) cont.resume(Unit) else cont.resumeWithException(IllegalStateException(error))
        }
    }

    override suspend fun decodeAudio(url: String, output: String, onProgress: (Float?) -> Unit) = call { done ->
        bridge.decodeAudio(url, output, onProgress, done)
    }

    override suspend fun recognize(model: WhisperFiles, pcm: String, onProgress: (Float) -> Unit): List<HeardSegment> {
        val heard = mutableListOf<HeardSegment>()
        call { done ->
            bridge.transcribe(
                WhisperRequest(model.encoder, model.decoder, model.tokens, model.vad, model.language, pcm),
                onProgress,
                { begin, end, text -> heard += HeardSegment(begin, end, text) },
                done,
            )
        }
        return heard
    }

    override fun temporaryFile(name: String): String = "${NSTemporaryDirectory()}$name"

    override fun deleteFile(path: String) {
        files.removeItemAtPath(path, null)
    }

    override fun moveFile(from: String, to: String) {
        files.removeItemAtPath(to, null)
        if (!files.moveItemAtPath(from, to, null)) error("could not move $from")
    }

    private suspend fun call(start: (done: (String?) -> Unit) -> Unit) = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { bridge.cancelTranscription() }
        start { error -> if (error == null) cont.resume(Unit) else cont.resumeWithException(TranscriptionException(error)) }
    }
}
