package com.tayra.languages.core.ui.audio

import co.touchlab.kermit.Logger
import javazoom.jl.player.Player
import java.io.BufferedInputStream
import java.net.URI
import java.util.concurrent.Executors

/** Decodes MP3 with JLayer on a background thread; javax.sound has no built-in MP3 support. */
actual class AudioPlayer actual constructor() {
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "audio-player").apply { isDaemon = true } }

    @Volatile
    private var current: Player? = null

    actual fun play(url: String, onStarted: () -> Unit, onFinished: (failed: Boolean) -> Unit) {
        stop()
        executor.execute {
            var failed = false
            try {
                val stream = BufferedInputStream(URI(url).toURL().openStream())
                val player = Player(stream)
                current = player
                onStarted()
                player.play()
            } catch (e: Exception) {
                Logger.w(e) { "Could not play audio $url" }
                failed = true
            } finally {
                current = null
                onFinished(failed)
            }
        }
    }

    actual fun stop() {
        current?.close()
        current = null
    }

    actual fun release() {
        stop()
        executor.shutdownNow()
    }
}
