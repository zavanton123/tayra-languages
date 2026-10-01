package com.tayra.languages.core.ui.audio

private fun createAudio(url: String): JsAny = js("new Audio(url)")
private fun playAudio(audio: JsAny, onEnded: () -> Unit, onError: () -> Unit): Unit =
    js("{ audio.onended = onEnded; audio.onerror = onError; audio.play().catch(onError); }")
private fun stopAudio(audio: JsAny): Unit = js("{ audio.onended = null; audio.onerror = null; audio.pause(); audio.currentTime = 0; }")

/** Uses the browser's HTMLAudioElement. */
actual class AudioPlayer actual constructor() {
    private var current: JsAny? = null
    private var onFinished: ((Boolean) -> Unit)? = null

    actual fun play(url: String, onFinished: (failed: Boolean) -> Unit) {
        stop()
        this.onFinished = onFinished
        val audio = createAudio(url)
        current = audio
        playAudio(audio, onEnded = { if (current === audio) finish() }, onError = { if (current === audio) finish(failed = true) })
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
