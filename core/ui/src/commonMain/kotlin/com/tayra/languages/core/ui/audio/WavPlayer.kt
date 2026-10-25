package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable

/** Plays a WAV file held in memory, one at a time. */
expect class WavPlayer {
    /**
     * Plays [wav] at [speed] (1 is normal), the pitch kept, interrupting any earlier playback.
     * [onDone] runs once when playback ends, is stopped or fails, possibly on another thread.
     */
    fun play(wav: ByteArray, speed: Float = 1f, onDone: () -> Unit = {})

    /** Holds playback where it is, without calling onDone; false when this platform cannot. */
    fun pause(): Boolean

    /** Goes on from where [pause] held it. */
    fun resume()
    fun stop()
    fun release()
}

/** A player scoped to the composition: stopped and released when the composable leaves. */
@Composable
expect fun rememberWavPlayer(): WavPlayer
