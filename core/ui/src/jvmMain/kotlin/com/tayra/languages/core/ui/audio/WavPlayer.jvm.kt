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
    @Volatile
    private var clip: Clip? = null

    /** Set while paused: stopping the clip to pause also fires STOP, which must not end playback. */
    @Volatile
    private var paused = false

    actual fun play(wav: ByteArray, onDone: () -> Unit) {
        stop()
        try {
            val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(wav))
            val next = AudioSystem.getClip()
            next.open(stream)
            // STOP fires at the end of the clip and when it is stopped early.
            next.addLineListener { event -> if (event.type == LineEvent.Type.STOP && !paused) onDone() }
            clip = next
            clipDone = onDone
            next.start()
        } catch (e: Exception) {
            Logger.w(e) { "Could not play synthesized speech" }
            onDone()
        }
    }

    actual fun pause(): Boolean {
        val playing = clip ?: return false
        paused = true
        playing.stop()
        return true
    }

    actual fun resume() {
        val held = clip ?: return
        paused = false
        held.start()
    }

    actual fun stop() {
        val wasPaused = paused
        paused = false
        clip?.let {
            runCatching {
                it.stop()
                it.close()
            }
        }
        // A paused clip fires no STOP when closed, so its end is reported here.
        if (wasPaused) clipDone?.invoke()
        clip = null
        clipDone = null
    }

    private var clipDone: (() -> Unit)? = null

    actual fun release() = stop()
}

@Composable
actual fun rememberWavPlayer(): WavPlayer {
    val player = remember { WavPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
