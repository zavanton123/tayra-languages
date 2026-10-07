package com.tayra.languages.core.ui.audio

import com.tayra.languages.core.ui.i18n.tr
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

/** Plays one audio clip at a time from a URL. */
expect class AudioPlayer() {
    /**
     * Starts playing the clip, stopping any clip that is still playing. [onStarted] runs when
     * sound begins, after the download; [onFinished] runs once when the clip ends, fails
     * ([failed] true), or is stopped. Both may be called from any thread.
     */
    fun play(url: String, onStarted: () -> Unit, onFinished: (failed: Boolean) -> Unit)

    /** Plays a clip already in memory (an MP3 file's bytes), with the same callbacks. */
    fun play(audio: ByteArray, onStarted: () -> Unit, onFinished: (failed: Boolean) -> Unit)
    fun stop()
    /** Releases platform resources; the player must not be used afterwards. */
    fun release()
}

/** Playback state for the composition: which URL is playing, so buttons can show play or stop. */
class AudioPlayback internal constructor(private val player: AudioPlayer) {
    var playingUrl: String? by mutableStateOf(null)
        private set

    /** The clip that was asked for but is still downloading. */
    var loadingUrl: String? by mutableStateOf(null)
        private set

    fun isPlaying(url: String): Boolean = playingUrl == url

    fun isLoading(url: String): Boolean = loadingUrl == url

    /**
     * Plays [url], or stops it when it is the clip currently playing. [onFailed] runs, on the
     * player's thread, when the clip cannot be played.
     */
    fun toggle(url: String, onFailed: (() -> Unit)? = null) {
        if (playingUrl == url) {
            stop()
            return
        }
        playingUrl = url
        loadingUrl = url
        player.play(
            url,
            onStarted = { if (loadingUrl == url) loadingUrl = null },
            onFinished = { failed ->
                if (playingUrl == url) playingUrl = null
                if (loadingUrl == url) loadingUrl = null
                if (failed) onFailed?.invoke()
            },
        )
    }

    /**
     * Plays the clip [key] once [load] has it (shown as loading until then), or stops it when it
     * is the clip playing. [onFailed] runs, on [scope], when there is nothing to play or it fails.
     */
    fun play(key: String, scope: CoroutineScope, load: suspend () -> ByteArray?, onFailed: () -> Unit) {
        if (playingUrl == key) {
            stop()
            return
        }
        stop()
        playingUrl = key
        loadingUrl = key
        scope.launch {
            val audio = load()
            if (playingUrl != key) return@launch
            if (audio == null) {
                stop()
                onFailed()
                return@launch
            }
            player.play(
                audio,
                onStarted = { if (loadingUrl == key) loadingUrl = null },
                onFinished = { failed ->
                    if (playingUrl == key) playingUrl = null
                    if (loadingUrl == key) loadingUrl = null
                    if (failed) scope.launch { onFailed() }
                },
            )
        }
    }

    fun stop() {
        player.stop()
        playingUrl = null
        loadingUrl = null
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
                    .semantics { contentDescription = tr("Stop recording") },
            )
        } else {
            Icon(Icons.Default.PlayArrow, contentDescription = tr("Play recording"), tint = MaterialTheme.colorScheme.primary)
        }
    }
}
