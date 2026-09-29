package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual class WavPlayer {
    private var player: AVAudioPlayer? = null

    actual fun play(wav: ByteArray) {
        stop()
        if (wav.isEmpty()) return
        val data = wav.usePinned { NSData.create(bytes = it.addressOf(0), length = wav.size.toULong()) }
        player = AVAudioPlayer(data = data, error = null)?.also { it.prepareToPlay(); it.play() }
    }

    actual fun stop() {
        player?.stop()
        player = null
    }

    actual fun release() = stop()
}

@Composable
actual fun rememberWavPlayer(): WavPlayer {
    val player = remember { WavPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
