package com.tayra.languages.core.data.media

import com.tayra.languages.core.domain.media.VideoClips
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readValue
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAssetExportPresetAppleM4A
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVAssetExportSessionStatusCompleted
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVFileTypeAppleM4A
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.timeRange
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.CMTimeRangeMake
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.coroutines.resume

/** A line's sound through AVFoundation's M4A export, and a picture of it through its image generator. */
@OptIn(ExperimentalForeignApi::class)
class IosVideoClips : VideoClips {

    /** A stored video address: a copy in the app's documents ("videos/…"), or a whole path. */
    private fun url(media: String): NSURL? {
        if (media.isBlank()) return null
        if (media.startsWith("/")) return NSURL.fileURLWithPath(media)
        val documents = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).firstOrNull() as? NSURL ?: return null
        return documents.URLByAppendingPathComponent(media)
    }

    override suspend fun sound(media: String, fromMs: Long, toMs: Long): Pair<ByteArray, String>? {
        val source = url(media) ?: return null
        if (toMs <= fromMs) return null
        val session = AVAssetExportSession(asset = AVURLAsset(uRL = source, options = null), presetName = AVAssetExportPresetAppleM4A) ?: return null
        val out = NSURL.fileURLWithPath(NSTemporaryDirectory() + "tayra-clip-" + NSUUID().UUIDString + ".m4a")
        session.outputURL = out
        session.outputFileType = AVFileTypeAppleM4A
        session.timeRange = CMTimeRangeMake(CMTimeMakeWithSeconds(fromMs / 1000.0, TIMESCALE), CMTimeMakeWithSeconds((toMs - fromMs) / 1000.0, TIMESCALE))
        val done = suspendCancellableCoroutine { cont -> session.exportAsynchronouslyWithCompletionHandler { cont.resume(session.status == AVAssetExportSessionStatusCompleted) } }
        val bytes = if (done) NSData.dataWithContentsOfURL(out)?.toByteArray() else null
        NSFileManager.defaultManager.removeItemAtURL(out, null)
        return bytes?.let { it to "m4a" }
    }

    override suspend fun picture(media: String, atMs: Long, maxWidth: Int): ByteArray? {
        val source = url(media) ?: return null
        val generator = AVAssetImageGenerator(asset = AVURLAsset(uRL = source, options = null))
        generator.appliesPreferredTrackTransform = true
        generator.requestedTimeToleranceBefore = kCMTimeZero.readValue()
        generator.requestedTimeToleranceAfter = kCMTimeZero.readValue()
        generator.maximumSize = CGSizeMake(maxWidth.toDouble(), 0.0)
        val image = generator.copyCGImageAtTime(CMTimeMakeWithSeconds(atMs / 1000.0, TIMESCALE), actualTime = null, error = null) ?: return null
        return UIImageJPEGRepresentation(UIImage.imageWithCGImage(image), JPEG_QUALITY)?.toByteArray()
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        val out = ByteArray(size)
        if (size > 0) out.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
        return out
    }

    private companion object {
        const val TIMESCALE = 600
        const val JPEG_QUALITY = 0.85
    }
}
