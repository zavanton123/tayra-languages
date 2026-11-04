package com.tayra.languages.core.data.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.tayra.languages.core.domain.media.VideoClips
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

/**
 * Cuts a line's sound from a video file without decoding it: its AAC samples are copied into an M4A
 * of their own. A video whose sound is another codec (Opus in WebM, say) gives none, and its card is read aloud.
 */
class AndroidVideoClips(private val context: Context) : VideoClips {

    private fun uri(media: String): Uri = if ("://" in media) Uri.parse(media) else Uri.fromFile(File(media))

    override suspend fun sound(media: String, fromMs: Long, toMs: Long): Pair<ByteArray, String>? = withContext(Dispatchers.IO) {
        if (media.isBlank() || toMs <= fromMs) return@withContext null
        val extractor = MediaExtractor()
        val out = File.createTempFile("tayra-clip", ".m4a", context.cacheDir)
        try {
            extractor.setDataSource(context, uri(media), null)
            val track = (0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_AUDIO_AAC }
                ?: return@withContext null
            val format = extractor.getTrackFormat(track)
            extractor.selectTrack(track)
            extractor.seekTo(fromMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val target = muxer.addTrack(format)
            muxer.start()
            val size = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else DEFAULT_SAMPLE_BUFFER
            val buffer = ByteBuffer.allocate(size)
            val info = MediaCodec.BufferInfo()
            var first = -1L
            var written = 0
            while (true) {
                val read = extractor.readSampleData(buffer, 0)
                if (read < 0) break
                val time = extractor.sampleTime
                if (time > toMs * 1000) break
                if (time >= fromMs * 1000) {
                    if (first < 0) first = time
                    info.set(0, read, time - first, MediaCodec.BUFFER_FLAG_KEY_FRAME)
                    muxer.writeSampleData(target, buffer, info)
                    written++
                }
                extractor.advance()
            }
            muxer.stop()
            muxer.release()
            if (written == 0) null else out.readBytes() to "m4a"
        } catch (e: Exception) {
            null
        } finally {
            extractor.release()
            out.delete()
        }
    }

    override suspend fun picture(media: String, atMs: Long, maxWidth: Int): ByteArray? = withContext(Dispatchers.IO) {
        if (media.isBlank()) return@withContext null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri(media))
            val frame = retriever.getFrameAtTime(atMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST) ?: return@withContext null
            val scaled = if (frame.width > maxWidth) Bitmap.createScaledBitmap(frame, maxWidth, frame.height * maxWidth / frame.width, true) else frame
            ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val DEFAULT_SAMPLE_BUFFER = 256 * 1024
        const val JPEG_QUALITY = 85
    }
}
