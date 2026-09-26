package com.tayra.languages.core.ui.audio

private fun createAudio(url: String): JsAny = js("new Audio(url)")
private fun playAudio(audio: JsAny, onEnded: () -> Unit): Unit =
    js("{ audio.onended = onEnded; audio.onerror = onEnded; audio.play().catch(onEnded); }")
private fun stopAudio(audio: JsAny): Unit = js("{ audio.onended = null; audio.onerror = null; audio.pause(); audio.currentTime = 0; }")

/** Uses the browser's HTMLAudioElement. */
actual class AudioPlayer actual constructor() {
    private var current: JsAny? = null
    private var onFinished: (() -> Unit)? = null

    actual fun play(url: String, onFinished: () -> Unit) {
        stop()
        this.onFinished = onFinished
        val audio = createAudio(url)
        current = audio
        playAudio(audio) { if (current === audio) finish() }
    }

    private fun finish() {
        current = null
        onFinished?.also { onFinished = null }?.invoke()
    }

    actual fun stop() {
        current?.let { stopAudio(it) }
        finish()
    }

    actual fun release() = stop()
}
