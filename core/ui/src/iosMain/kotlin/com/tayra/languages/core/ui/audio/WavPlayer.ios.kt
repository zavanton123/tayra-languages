package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.Foundation.NSData
import platform.Foundation.create
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual class WavPlayer {
    private var player: AVAudioPlayer? = null
    private var done: (() -> Unit)? = null

    /** Reports the end of playback; kept here because the player holds it weakly. */
    private val delegate = object : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            if (player === this@WavPlayer.player) {
                this@WavPlayer.player = null
                finish()
            }
        }
    }

    private fun finish() {
        done?.also { done = null }?.invoke()
    }

    actual fun play(wav: ByteArray, onDone: () -> Unit) {
        stop()
        done = onDone
        if (wav.isEmpty()) return finish()
        val data = wav.usePinned { NSData.create(bytes = it.addressOf(0), length = wav.size.toULong()) }
        val next = AVAudioPlayer(data = data, error = null) ?: return finish()
        next.delegate = delegate
        player = next
        next.prepareToPlay()
        if (!next.play()) { player = null; finish() }
    }

    actual fun stop() {
        player?.stop()
        player = null
        finish()
    }

    actual fun release() = stop()
}

@Composable
actual fun rememberWavPlayer(): WavPlayer {
    val player = remember { WavPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
