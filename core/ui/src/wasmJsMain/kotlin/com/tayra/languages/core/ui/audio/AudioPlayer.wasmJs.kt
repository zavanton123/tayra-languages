package com.tayra.languages.core.ui.audio

private fun createAudio(url: String): JsAny = js("new Audio(url)")
private fun playAudio(audio: JsAny): Unit = js("{ audio.play().catch(function() {}); }")
private fun stopAudio(audio: JsAny): Unit = js("{ audio.pause(); audio.currentTime = 0; }")

/** Uses the browser's HTMLAudioElement. */
actual class AudioPlayer actual constructor() {
    private var current: JsAny? = null

    actual fun play(url: String) {
        stop()
        current = createAudio(url).also { playAudio(it) }
    }

    actual fun stop() {
        current?.let { stopAudio(it) }
        current = null
    }

    actual fun release() = stop()
}
