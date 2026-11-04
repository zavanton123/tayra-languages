package com.tayra.languages.core.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.io.File
import java.nio.ByteBuffer
import kotlin.time.TimeSource

/** VLC, when it is installed: found once, in its usual places on each system. */
internal object VlcEngine {
    val available: Boolean by lazy { runCatching { NativeDiscovery().discover() }.getOrDefault(false) }

    val factory: MediaPlayerFactory by lazy { MediaPlayerFactory("--quiet", "--no-video-title-show") }
}

/** A file played by VLC, its pictures drawn by Compose, so nothing of Swing sits over the screen. */
@Composable
internal fun VlcVideo(address: String, onController: (VideoController) -> Unit, modifier: Modifier) {
    val controller = remember(address) { VlcVideoController(address) }
    DisposableEffect(controller) {
        onController(controller)
        onDispose { controller.release() }
    }
    LaunchedEffect(controller) {
        controller.playing.collectLatest { playing ->
            if (playing) while (isActive) withFrameMillis { controller.tick() }
        }
    }
    val frame by controller.frame.collectAsState()
    Box(modifier.background(MaterialTheme.colorScheme.inverseSurface), contentAlignment = Alignment.Center) {
        frame?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
    }
}

private class VlcVideoController(address: String) : VideoController {
    override val position = MutableStateFlow(0L)
    override val playing = MutableStateFlow(false)
    override val durationMs = MutableStateFlow(0L)
    override val rate = MutableStateFlow(1f)
    override val problem = MutableStateFlow<VideoProblem?>(null)
    override val latencyMs: Long get() = 40

    val frame = MutableStateFlow<ImageBitmap?>(null)

    private val player: EmbeddedMediaPlayer = VlcEngine.factory.mediaPlayers().newEmbeddedMediaPlayer()

    /** VLC tells its time a few times a second; between, it is counted on from the last it told. */
    @Volatile private var told = 0L
    @Volatile private var toldAt = TimeSource.Monotonic.markNow()
    @Volatile private var pendingSeek: Long? = null
    @Volatile private var ready = false

    private var width = 0
    private var height = 0
    private var pixels = ByteArray(0)

    init {
        player.videoSurface().set(
            VlcEngine.factory.videoSurfaces().newVideoSurface(
                object : BufferFormatCallback {
                    override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                        width = sourceWidth
                        height = sourceHeight
                        pixels = ByteArray(sourceWidth * sourceHeight * 4)
                        return RV32BufferFormat(sourceWidth, sourceHeight)
                    }

                    override fun allocatedBuffers(buffers: Array<out ByteBuffer>) {}
                },
                object : RenderCallback {
                    override fun display(mediaPlayer: MediaPlayer, nativeBuffers: Array<out ByteBuffer>, bufferFormat: BufferFormat) {
                        val buffer = nativeBuffers.firstOrNull() ?: return
                        if (width == 0 || pixels.isEmpty()) return
                        buffer.rewind()
                        buffer.get(pixels, 0, minOf(pixels.size, buffer.remaining()))
                        // RV32 is blue, green, red and an unused byte, which Skia reads as BGRA with no alpha.
                        val image = org.jetbrains.skia.Image.makeRaster(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE), pixels, width * 4)
                        frame.value = image.toComposeImageBitmap()
                    }
                },
                true,
            ),
        )
        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                ready = true
                playing.value = true
                mark(mediaPlayer.status().time())
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                ready = true
                playing.value = false
                mark(mediaPlayer.status().time())
                pendingSeek?.let { at -> pendingSeek = null; mediaPlayer.submit { mediaPlayer.controls().setTime(at) } }
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                playing.value = false
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                playing.value = false
                position.value = durationMs.value
            }

            override fun error(mediaPlayer: MediaPlayer) {
                playing.value = false
                problem.value = VideoProblem.Unsupported
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                durationMs.value = newLength.coerceAtLeast(0)
            }

            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                mark(newTime)
            }
        })
        if (!File(address).isFile) {
            problem.value = VideoProblem.FileMissing
        } else {
            // Opened at its start and held there, so the first frame shows and a resumed place can be sought.
            player.media().startPaused(address)
        }
    }

    private fun mark(time: Long) {
        if (time < 0) return
        told = time
        toldAt = TimeSource.Monotonic.markNow()
        position.value = time
    }

    /** Counts the place on from the last VLC told, as the screen draws a frame. */
    fun tick() {
        if (!playing.value) return
        val counted = told + (toldAt.elapsedNow().inWholeMilliseconds * rate.value).toLong()
        val end = durationMs.value
        position.value = if (end > 0) counted.coerceAtMost(end) else counted
    }

    override fun play() {
        if (problem.value != null) return
        // Counted as playing once VLC says it plays, so the counting starts with the sound and not before it.
        player.submit { player.controls().play() }
    }

    override fun pause() {
        playing.value = false
        player.submit {
            player.controls().setPause(true)
            mark(player.status().time())
        }
    }

    override fun seekTo(ms: Long) {
        val at = ms.coerceAtLeast(0)
        mark(at)
        if (!ready) {
            pendingSeek = at
            return
        }
        player.submit { player.controls().setTime(at) }
    }

    override fun setRate(rate: Float) {
        this.rate.value = rate
        mark(position.value)
        player.submit { player.controls().setRate(rate) }
    }

    override fun release() {
        playing.value = false
        player.submit {
            player.controls().stop()
            player.release()
        }
    }
}
