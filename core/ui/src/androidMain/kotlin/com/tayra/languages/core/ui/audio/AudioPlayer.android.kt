package com.tayra.languages.core.ui.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import co.touchlab.kermit.Logger

actual class AudioPlayer actual constructor() {
    private var current: MediaPlayer? = null

    actual fun play(url: String) {
        stop()
        current = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build())
            setOnPreparedListener { it.start() }
            setOnCompletionListener { it.release(); if (current === it) current = null }
            setOnErrorListener { player, what, extra ->
                Logger.w { "Audio playback failed ($what/$extra) for $url" }
                player.release()
                if (current === player) current = null
                true
            }
            try {
                setDataSource(url)
                prepareAsync()
            } catch (e: Exception) {
                Logger.w(e) { "Could not load audio $url" }
                release()
                current = null
            }
        }
    }

    actual fun stop() {
        current?.let { runCatching { it.stop() }; it.release() }
        current = null
    }

    actual fun release() = stop()
}
