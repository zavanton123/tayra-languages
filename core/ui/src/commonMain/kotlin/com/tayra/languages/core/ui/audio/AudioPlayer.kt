package com.tayra.languages.core.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Plays one audio clip at a time from a URL. */
expect class AudioPlayer() {
    /**
     * Starts playing the clip, stopping any clip that is still playing. [onFinished] runs once
     * when the clip ends, fails, or is stopped; it may be called from any thread.
     */
    fun play(url: String, onFinished: () -> Unit)
    fun stop()
    /** Releases platform resources; the player must not be used afterwards. */
    fun release()
}

/** Playback state for the composition: which URL is playing, so buttons can show play or stop. */
class AudioPlayback internal constructor(private val player: AudioPlayer) {
    var playingUrl: String? by mutableStateOf(null)
        private set

    fun isPlaying(url: String): Boolean = playingUrl == url

    /** Plays [url], or stops it when it is the clip currently playing. */
    fun toggle(url: String) {
        if (playingUrl == url) {
            player.stop()
            playingUrl = null
            return
        }
        playingUrl = url
        player.play(url) { if (playingUrl == url) playingUrl = null }
    }

    internal fun release() = player.release()
}

/** Playback scoped to the composition: stopped and released when the composable leaves. */
@Composable
fun rememberAudioPlayback(): AudioPlayback {
    val playback = remember { AudioPlayback(AudioPlayer()) }
    DisposableEffect(playback) { onDispose { playback.release() } }
    return playback
}

/** A play button that turns into a stop square while [url] is playing. */
@Composable
fun PlayButton(url: String, playback: AudioPlayback, modifier: Modifier = Modifier) {
    val playing = playback.isPlaying(url)
    IconButton(onClick = { playback.toggle(url) }, modifier = modifier) {
        if (playing) {
            Box(
                Modifier.size(14.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    .semantics { contentDescription = "Stop recording" },
            )
        } else {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play recording", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
