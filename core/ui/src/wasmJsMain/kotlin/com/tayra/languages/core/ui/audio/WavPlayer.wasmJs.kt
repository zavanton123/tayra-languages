package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** The browser has no local speech engines yet, so there is never audio to play. */
actual class WavPlayer {
    actual fun play(wav: ByteArray, onDone: () -> Unit) = onDone()
    actual fun pause(): Boolean = false
    actual fun resume() {}
    actual fun stop() {}
    actual fun release() {}
}

@Composable
actual fun rememberWavPlayer(): WavPlayer = remember { WavPlayer() }
