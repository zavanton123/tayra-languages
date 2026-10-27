package com.tayra.languages.core.ui.audio

import java.util.concurrent.Executors

/**
 * Closes audio lines away from the caller. On a Mac closing a line now and then holds its thread
 * for two seconds, which on the UI thread froze the window when a sound was stopped.
 */
internal object AudioRelease {
    private val closer = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "audio-release").apply { isDaemon = true } }

    fun later(close: () -> Unit) {
        closer.execute { runCatching(close) }
    }
}
