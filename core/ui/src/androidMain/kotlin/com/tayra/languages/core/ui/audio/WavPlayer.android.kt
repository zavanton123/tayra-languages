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

    private var done: (() -> Unit)? = null

    actual fun play(wav: ByteArray, onDone: () -> Unit) {
        stop()
        done = onDone
        try {
            file.writeBytes(wav)
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { finished(it) }
                setOnErrorListener { mp, _, _ -> finished(mp); true }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Logger.w(e) { "Could not play synthesized speech" }
            finish()
        }
    }

    private fun finished(mp: MediaPlayer) {
        mp.release()
        if (player === mp) player = null
        finish()
    }

    private fun finish() {
        done?.also { done = null }?.invoke()
    }

    actual fun pause(): Boolean {
        val playing = player ?: return false
        return runCatching { playing.pause() }.isSuccess
    }

    actual fun resume() {
        player?.let { runCatching { it.start() } }
    }

    actual fun stop() {
        player?.let { runCatching { it.stop(); it.release() } }
        player = null
        finish()
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
