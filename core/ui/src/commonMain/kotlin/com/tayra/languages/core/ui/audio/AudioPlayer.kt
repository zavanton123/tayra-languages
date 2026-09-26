package com.tayra.languages.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/** Plays one audio clip at a time from a URL. */
expect class AudioPlayer() {
    /** Starts playing the clip, stopping any clip that is still playing. */
    fun play(url: String)
    fun stop()
    /** Releases platform resources; the player must not be used afterwards. */
    fun release()
}

/** A player scoped to the composition: stopped and released when the composable leaves. */
@Composable
fun rememberAudioPlayer(): AudioPlayer {
    val player = remember { AudioPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
