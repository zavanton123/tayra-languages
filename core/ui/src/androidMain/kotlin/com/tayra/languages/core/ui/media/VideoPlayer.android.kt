package com.tayra.languages.core.ui.media

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import java.io.File

actual val videoSupported: Boolean = true

@Composable
actual fun VideoPlayer(source: VideoSource, onController: (VideoController) -> Unit, modifier: Modifier) {
    when (source) {
        is VideoSource.File -> FileVideo(source.uri, onController, modifier)
        is VideoSource.YouTube -> YouTubeVideo(source, onController, modifier)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun FileVideo(address: String, onController: (VideoController) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val controller = remember(address) { ExoVideoController(context, address) }
    DisposableEffect(controller) {
        onController(controller)
        onDispose { controller.release() }
    }
    // The place is read every frame while it plays, so a line's end is caught on time; a paused player is not polled.
    LaunchedEffect(controller) {
        controller.playing.collectLatest { playing ->
            if (playing) while (isActive) withFrameMillis { controller.tick() }
        }
    }
    // Going to the background stops the picture; the sound stops with it.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { controller.pause() }
    AndroidView(
        factory = { PlayerView(it).apply { useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; player = controller.player } },
        modifier = modifier,
        onRelease = { it.player = null },
    )
}

/** ExoPlayer as a [VideoController]: its place, whether it plays and how long the video is, as flows. */
private class ExoVideoController(context: Context, address: String) : VideoController {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    override val position = MutableStateFlow(0L)
    override val playing = MutableStateFlow(false)
    override val durationMs = MutableStateFlow(0L)
    override val rate = MutableStateFlow(1f)
    override val problem = MutableStateFlow<VideoProblem?>(null)
    override val latencyMs: Long get() = 40

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing.value = isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING)
                tick()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) durationMs.value = player.duration.coerceAtLeast(0)
                if (state == Player.STATE_ENDED) playing.value = false
            }

            override fun onPlayerError(error: PlaybackException) {
                problem.value = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND, PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> VideoProblem.FileMissing
                    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED, PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> VideoProblem.Unsupported
                    else -> VideoProblem.Failed(error.message ?: error.errorCodeName)
                }
                playing.value = false
            }
        })
        val uri = if (address.contains("://")) Uri.parse(address) else Uri.fromFile(File(address))
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    fun tick() {
        position.value = player.currentPosition
    }

    override fun play() {
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        player.play()
        playing.value = true
    }

    override fun pause() {
        player.pause()
        playing.value = false
        tick()
    }

    override fun seekTo(ms: Long) {
        player.seekTo(ms.coerceAtLeast(0))
        position.value = ms.coerceAtLeast(0)
    }

    override fun setRate(rate: Float) {
        player.playbackParameters = PlaybackParameters(rate)
        this.rate.value = rate
    }

    override fun release() {
        player.release()
    }
}
