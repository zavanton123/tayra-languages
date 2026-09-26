package com.tayra.languages.core.ui.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import co.touchlab.kermit.Logger

actual class AudioPlayer actual constructor() {
    private var current: MediaPlayer? = null
    private var onFinished: (() -> Unit)? = null

    actual fun play(url: String, onFinished: () -> Unit) {
        stop()
        this.onFinished = onFinished
        current = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build())
            setOnPreparedListener { it.start() }
            setOnCompletionListener { finish(it) }
            setOnErrorListener { player, what, extra ->
                Logger.w { "Audio playback failed ($what/$extra) for $url" }
                finish(player)
                true
            }
            try {
                setDataSource(url)
                prepareAsync()
            } catch (e: Exception) {
                Logger.w(e) { "Could not load audio $url" }
                finish(this)
            }
        }
    }

    private fun finish(player: MediaPlayer) {
        player.release()
        if (current === player) {
            current = null
            onFinished?.also { onFinished = null }?.invoke()
        }
    }

    actual fun stop() {
        current?.let { runCatching { it.stop() }; finish(it) }
    }

    actual fun release() = stop()
}
