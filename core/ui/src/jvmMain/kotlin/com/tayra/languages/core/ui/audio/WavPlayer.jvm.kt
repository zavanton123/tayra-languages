package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

actual class WavPlayer {
    @Volatile
    private var clip: Clip? = null

    actual fun play(wav: ByteArray) {
        stop()
        try {
            val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(wav))
            val next = AudioSystem.getClip()
            next.open(stream)
            clip = next
            next.start()
        } catch (e: Exception) {
            Logger.w(e) { "Could not play synthesized speech" }
        }
    }

    actual fun stop() {
        clip?.let { runCatching { it.stop(); it.close() } }
        clip = null
    }

    actual fun release() = stop()
}

@Composable
actual fun rememberWavPlayer(): WavPlayer {
    val player = remember { WavPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
