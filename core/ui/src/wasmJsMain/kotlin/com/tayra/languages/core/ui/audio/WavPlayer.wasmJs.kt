package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlin.io.encoding.Base64

/** A blob URL for WAV bytes given as base64: one call, rather than one per byte. */
private fun wavUrl(base64: String): String = js(
    """{
        var binary = atob(base64);
        var bytes = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        return URL.createObjectURL(new Blob([bytes], { type: 'audio/wav' }));
    }""",
)
private fun newWavAudio(url: String): JsAny = js("new Audio(url)")
private fun startWavAudio(audio: JsAny, onEnded: () -> Unit, onError: () -> Unit): Unit =
    js("{ audio.onended = onEnded; audio.onerror = onError; audio.play().catch(onError); }")
private fun pauseWavAudio(audio: JsAny): Unit = js("audio.pause()")
private fun resumeWavAudio(audio: JsAny): Unit = js("audio.play().catch(function () {})")
private fun stopWavAudio(audio: JsAny): Unit = js("{ audio.onended = null; audio.onerror = null; audio.pause(); audio.currentTime = 0; }")
private fun revokeWavUrl(url: String): Unit = js("URL.revokeObjectURL(url)")

/** Plays synthesized speech through an HTMLAudioElement, which can be held and resumed. */
actual class WavPlayer {
    private var current: JsAny? = null
    private var url: String? = null
    private var onDone: (() -> Unit)? = null

    actual fun play(wav: ByteArray, onDone: () -> Unit) {
        stop()
        val url = wavUrl(Base64.encode(wav))
        val audio = newWavAudio(url)
        current = audio
        this.url = url
        this.onDone = onDone
        startWavAudio(audio, onEnded = { if (current === audio) finish() }, onError = { if (current === audio) finish() })
    }

    actual fun pause(): Boolean {
        val audio = current ?: return false
        pauseWavAudio(audio)
        return true
    }

    actual fun resume() {
        current?.let { resumeWavAudio(it) }
    }

    actual fun stop() {
        current?.let { stopWavAudio(it) }
        finish()
    }

    private fun finish() {
        current = null
        url?.let { revokeWavUrl(it) }
        url = null
        onDone?.also { onDone = null }?.invoke()
    }

    actual fun release() = stop()
}

@Composable
actual fun rememberWavPlayer(): WavPlayer {
    val player = remember { WavPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
