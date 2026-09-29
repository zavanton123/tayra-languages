package com.tayra.languages.core.ui.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import java.io.File

actual class WavPlayer(context: Context) {
    private val file = File(context.applicationContext.cacheDir, "speech.wav")
    private var player: MediaPlayer? = null

    actual fun play(wav: ByteArray) {
        stop()
        try {
            file.writeBytes(wav)
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { it.release(); if (player === it) player = null }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Logger.w(e) { "Could not play synthesized speech" }
        }
    }

    actual fun stop() {
        player?.let { runCatching { it.stop(); it.release() } }
        player = null
    }

    actual fun release() = stop()
}

@Composable
actual fun rememberWavPlayer(): WavPlayer {
    val context = LocalContext.current
    val player = remember(context) { WavPlayer(context) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
