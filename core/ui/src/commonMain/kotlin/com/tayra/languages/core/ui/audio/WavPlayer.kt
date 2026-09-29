package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable

/** Plays a WAV file held in memory, one at a time. */
expect class WavPlayer {
    /** Plays [wav], interrupting any earlier playback. */
    fun play(wav: ByteArray)
    fun stop()
    fun release()
}

/** A player scoped to the composition: stopped and released when the composable leaves. */
@Composable
expect fun rememberWavPlayer(): WavPlayer
