package com.tayra.languages.core.ui.audio

private fun createAudio(url: String): JsAny = js("new Audio(url)")
private fun playAudio(audio: JsAny, onPlaying: () -> Unit, onEnded: () -> Unit, onError: () -> Unit): Unit =
    js("{ audio.onplaying = onPlaying; audio.onended = onEnded; audio.onerror = onError; audio.play().catch(onError); }")
private fun stopAudio(audio: JsAny): Unit = js("{ audio.onplaying = null; audio.onended = null; audio.onerror = null; audio.pause(); audio.currentTime = 0; }")

/** Uses the browser's HTMLAudioElement. */
actual class AudioPlayer actual constructor() {
    private var current: JsAny? = null
    private var onFinished: ((Boolean) -> Unit)? = null

    actual fun play(url: String, onStarted: () -> Unit, onFinished: (failed: Boolean) -> Unit) {
        stop()
        this.onFinished = onFinished
        val audio = createAudio(url)
        current = audio
        playAudio(
            audio,
            onPlaying = { if (current === audio) onStarted() },
            onEnded = { if (current === audio) finish() },
            onError = { if (current === audio) finish(failed = true) },
        )
    }

    private fun finish(failed: Boolean = false) {
        current = null
        onFinished?.also { onFinished = null }?.invoke(failed)
    }

    actual fun stop() {
        current?.let { stopAudio(it) }
        finish()
    }

    actual fun release() = stop()
}
