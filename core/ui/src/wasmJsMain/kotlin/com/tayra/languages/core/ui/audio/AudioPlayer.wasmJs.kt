package com.tayra.languages.core.ui.audio

private fun createAudio(url: String): JsAny = js("new Audio(url)")
private fun playAudio(audio: JsAny, onPlaying: () -> Unit, onEnded: () -> Unit, onError: () -> Unit): Unit =
    js("{ audio.onplaying = onPlaying; audio.onended = onEnded; audio.onerror = onError; audio.play().catch(onError); }")
private fun stopAudio(audio: JsAny): Unit = js("{ audio.onplaying = null; audio.onended = null; audio.onerror = null; audio.pause(); audio.currentTime = 0; }")

private fun newBytes(size: Int): JsAny = js("new Uint8Array(size)")
private fun setByte(array: JsAny, index: Int, value: Int): Unit = js("array[index] = value")
private fun blobUrl(array: JsAny): String = js("URL.createObjectURL(new Blob([array], { type: 'audio/mpeg' }))")
private fun revokeUrl(url: String): Unit = js("URL.revokeObjectURL(url)")

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

    /** A clip in memory plays from a blob URL, released once it has finished. */
    actual fun play(audio: ByteArray, onStarted: () -> Unit, onFinished: (failed: Boolean) -> Unit) {
        val array = newBytes(audio.size)
        audio.forEachIndexed { index, byte -> setByte(array, index, byte.toInt() and 0xFF) }
        val url = blobUrl(array)
        play(url, onStarted) { failed ->
            revokeUrl(url)
            onFinished(failed)
        }
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
