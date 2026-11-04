package com.tayra.languages.core.data.media

import com.tayra.languages.core.domain.media.VideoClips
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

/**
 * VLC cuts a line's sound into an M4A file and takes a picture of it, when VLC is installed (the
 * desktop app plays video files with it too). Without VLC the cards keep the read-aloud sound.
 */
class VlcVideoClips : VideoClips {
    private val factory: MediaPlayerFactory? by lazy {
        if (runCatching { NativeDiscovery().discover() }.getOrDefault(false)) MediaPlayerFactory("--quiet", "--no-video-title-show") else null
    }

    override suspend fun sound(media: String, fromMs: Long, toMs: Long): Pair<ByteArray, String>? = withContext(Dispatchers.IO) {
        val vlc = factory ?: return@withContext null
        if (!File(media).isFile || toMs <= fromMs) return@withContext null
        val out = File.createTempFile("tayra-clip", ".wav")
        val player = vlc.mediaPlayers().newMediaPlayer()
        try {
            val done = CompletableDeferred<Unit>()
            player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun finished(mediaPlayer: MediaPlayer) { done.complete(Unit) }
                override fun stopped(mediaPlayer: MediaPlayer) { done.complete(Unit) }
                override fun error(mediaPlayer: MediaPlayer) { done.complete(Unit) }
            })
            val started = player.media().start(
                media,
                ":start-time=${fromMs / 1000.0}",
                ":stop-time=${toMs / 1000.0}",
                ":no-sout-video",
                ":sout=#transcode{vcodec=none,acodec=s16l,channels=1,samplerate=$RATE}:std{access=file,mux=wav,dst='${out.absolutePath}'}",
            )
            if (!started) return@withContext null
            withTimeoutOrNull(CUT_TIMEOUT_MS) { done.await() }
            // The file is finished only once VLC stops writing it.
            player.controls().stop()
            Wav.trim(out.readBytes(), toMs - fromMs)?.let { it to "wav" }
        } finally {
            player.release()
            out.delete()
        }
    }

    override suspend fun picture(media: String, atMs: Long, maxWidth: Int): ByteArray? = withContext(Dispatchers.IO) {
        val vlc = factory ?: return@withContext null
        if (!File(media).isFile) return@withContext null
        val player = vlc.mediaPlayers().newEmbeddedMediaPlayer()
        val frame = CompletableDeferred<BufferedImage>()
        var width = 0
        var height = 0
        player.videoSurface().set(
            vlc.videoSurfaces().newVideoSurface(
                object : BufferFormatCallback {
                    override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                        width = sourceWidth
                        height = sourceHeight
                        return RV32BufferFormat(sourceWidth, sourceHeight)
                    }

                    override fun allocatedBuffers(buffers: Array<out ByteBuffer>) {}
                },
                object : RenderCallback {
                    override fun display(mediaPlayer: MediaPlayer, nativeBuffers: Array<out ByteBuffer>, bufferFormat: BufferFormat) {
                        if (frame.isCompleted || width == 0) return
                        val pixels = IntArray(width * height)
                        nativeBuffers[0].rewind()
                        nativeBuffers[0].order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(pixels, 0, minOf(pixels.size, nativeBuffers[0].remaining() / 4))
                        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                        image.setRGB(0, 0, width, height, pixels, 0, width)
                        frame.complete(image)
                    }
                },
                true,
            ),
        )
        try {
            if (!player.media().start(media, ":start-time=${atMs / 1000.0}", ":no-audio")) return@withContext null
            val image = withTimeoutOrNull(PICTURE_TIMEOUT_MS) { frame.await() } ?: return@withContext null
            jpeg(scaled(image, maxWidth))
        } finally {
            player.submit { player.controls().stop(); player.release() }
        }
    }

    private fun scaled(image: BufferedImage, maxWidth: Int): BufferedImage {
        if (image.width <= maxWidth) return image
        val height = image.height * maxWidth / image.width
        return BufferedImage(maxWidth, height, BufferedImage.TYPE_INT_RGB).also { out ->
            val g = out.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(image, 0, 0, maxWidth, height, null)
            g.dispose()
        }
    }

    private fun jpeg(image: BufferedImage): ByteArray = ByteArrayOutputStream().also { ImageIO.write(image, "jpg", it) }.toByteArray()

    private companion object {
        const val CUT_TIMEOUT_MS = 30_000L
        const val PICTURE_TIMEOUT_MS = 10_000L

        /** The sample rate of the clips: enough for speech, and small. */
        const val RATE = 22_050
    }
}

/** 16-bit mono WAV files as VLC writes them. */
internal object Wav {
    /**
     * The last [keepMs] of [wav]'s sound, as a WAV of its own. VLC may begin a cut at the start of
     * the file rather than at the time asked, so whatever comes before the line is dropped here.
     */
    fun trim(wav: ByteArray, keepMs: Long): ByteArray? {
        val data = chunk(wav, "data") ?: return null
        val format = chunk(wav, "fmt ") ?: return null
        val buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        val channels = buffer.getShort(format.first + 2).toInt()
        val rate = buffer.getInt(format.first + 4)
        val bytesPerFrame = channels * 2
        val frames = minOf(data.second, wav.size - data.first) / bytesPerFrame
        if (frames == 0 || rate <= 0) return null
        val keep = minOf(frames.toLong(), keepMs * rate / 1000).toInt()
        val start = data.first + (frames - keep) * bytesPerFrame
        val pcm = wav.copyOfRange(start, start + keep * bytesPerFrame)
        return header(pcm.size, channels, rate) + pcm
    }

    /** Where [id]'s data begins and how long it is. */
    private fun chunk(wav: ByteArray, id: String): Pair<Int, Int>? {
        val buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        var at = 12
        while (at + 8 <= wav.size) {
            val name = String(wav, at, 4, Charsets.US_ASCII)
            val size = buffer.getInt(at + 4)
            if (name == id) return at + 8 to (if (size <= 0) wav.size - at - 8 else size)
            at += 8 + size + (size and 1)
        }
        return null
    }

    private fun header(dataSize: Int, channels: Int, rate: Int): ByteArray = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVE".toByteArray())
        put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(channels.toShort()); putInt(rate); putInt(rate * channels * 2); putShort((channels * 2).toShort()); putShort(16)
        put("data".toByteArray()); putInt(dataSize)
    }.array()
}
