package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

actual class WavPlayer {
    /** The clip sounding now; a clip no longer here is ignored when its STOP comes in. */
    @Volatile
    private var clip: Clip? = null

    private var wav: ByteArray? = null

    /** The current playback's onDone, until it has run. */
    private var done: (() -> Unit)? = null

    /** The frame a pause held playback at. */
    private var heldAt: Int? = null

    actual fun play(wav: ByteArray, onDone: () -> Unit) {
        stop()
        this.wav = wav
        synchronized(this) { done = onDone }
        start(0)
    }

    /**
     * A clip stopped and started again does not always play on: on a Mac with its lid closed it
     * never reaches its end, while a new clip plays as usual. So a pause closes the clip and
     * [resume] opens a new one at the frame it was held at.
     */
    actual fun pause(): Boolean {
        val playing = clip ?: return false
        clip = null
        heldAt = playing.framePosition
        runCatching {
            playing.stop()
            playing.close()
        }
        return true
    }

    actual fun resume() {
        val at = heldAt ?: return
        heldAt = null
        start(at)
    }

    actual fun stop() {
        val playing = clip
        clip = null
        heldAt = null
        wav = null
        playing?.let {
            runCatching {
                it.stop()
                it.close()
            }
        }
        finish(synchronized(this) { done })
    }

    actual fun release() = stop()

    private fun start(frame: Int) {
        val data = wav ?: return
        val onDone = synchronized(this) { done }
        try {
            val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(data))
            val next = AudioSystem.getClip()
            next.open(stream)
            next.framePosition = frame.coerceIn(0, next.frameLength)
            // STOP fires at the end of the clip, and when it is stopped early, which pause and stop see to themselves.
            next.addLineListener { event -> if (event.type == LineEvent.Type.STOP && clip === next) finish(onDone) }
            clip = next
            next.start()
        } catch (e: Exception) {
            Logger.w(e) { "Could not play synthesized speech" }
            clip = null
            finish(onDone)
        }
    }

    /** Runs [onDone] if it is still the current playback's and has not run yet. */
    private fun finish(onDone: (() -> Unit)?) {
        val run = synchronized(this) {
            if (onDone != null && done === onDone) {
                done = null
                true
            } else {
                false
            }
        }
        if (run) onDone?.invoke()
    }
}

@Composable
actual fun rememberWavPlayer(): WavPlayer {
    val player = remember { WavPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
