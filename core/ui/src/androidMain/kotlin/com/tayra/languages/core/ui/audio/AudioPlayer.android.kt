package com.tayra.languages.core.ui.audio

import android.media.AudioAttributes
import android.media.MediaDataSource
import android.media.MediaPlayer
import co.touchlab.kermit.Logger

actual class AudioPlayer actual constructor() {
    private var current: MediaPlayer? = null
    private var onFinished: ((Boolean) -> Unit)? = null

    actual fun play(url: String, onStarted: () -> Unit, onFinished: (failed: Boolean) -> Unit) =
        start(url, { setDataSource(url) }, onStarted, onFinished)

    actual fun play(audio: ByteArray, onStarted: () -> Unit, onFinished: (failed: Boolean) -> Unit) =
        start("a recording in memory", { setDataSource(ByteArrayDataSource(audio)) }, onStarted, onFinished)

    private fun start(what: String, source: MediaPlayer.() -> Unit, onStarted: () -> Unit, onFinished: (failed: Boolean) -> Unit) {
        stop()
        this.onFinished = onFinished
        current = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build())
            setOnPreparedListener {
                it.start()
                onStarted()
            }
            setOnCompletionListener { finish(it) }
            setOnErrorListener { player, code, extra ->
                Logger.w { "Audio playback failed ($code/$extra) for $what" }
                finish(player, failed = true)
                true
            }
            try {
                source()
                prepareAsync()
            } catch (e: Exception) {
                Logger.w(e) { "Could not load audio $what" }
                finish(this, failed = true)
            }
        }
    }

    private fun finish(player: MediaPlayer, failed: Boolean = false) {
        player.release()
        if (current === player) {
            current = null
            onFinished?.also { onFinished = null }?.invoke(failed)
        }
    }

    actual fun stop() {
        current?.let { runCatching { it.stop() }; finish(it) }
    }

    actual fun release() = stop()
}

/** Serves a clip held in memory to MediaPlayer. */
private class ByteArrayDataSource(private val audio: ByteArray) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= audio.size) return -1
        val count = minOf(size.toLong(), audio.size - position).toInt()
        System.arraycopy(audio, position.toInt(), buffer, offset, count)
        return count
    }

    override fun getSize(): Long = audio.size.toLong()

    override fun close() {}
}
