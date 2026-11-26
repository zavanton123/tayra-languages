package com.idiomiko.core.ui.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.min

// UIKit reads what the photo library gives, HEIC too, and draws it upright at no more than [maxSide]; Skia reads that.
@OptIn(ExperimentalForeignApi::class)
internal actual fun decodeUpright(bytes: ByteArray, maxSide: Int): ImageBitmap? {
    if (bytes.isEmpty()) return null
    val data = bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
    val photo = UIImage.imageWithData(data) ?: return null
    val (width, height) = photo.size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null
    val scale = min(1.0, maxSide / max(width, height))
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { setScale(1.0) }
    val upright = UIGraphicsImageRenderer(CGSizeMake(width * scale, height * scale), format).imageWithActions { _ ->
        photo.drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale))
    }
    val jpeg = UIImageJPEGRepresentation(upright, 0.95) ?: return null
    val out = ByteArray(jpeg.length.toInt())
    if (out.isEmpty()) return null
    out.usePinned { memcpy(it.addressOf(0), jpeg.bytes, jpeg.length) }
    return runCatching { Image.makeFromEncoded(out).toComposeImageBitmap() }.getOrNull()
}

actual fun ImageBitmap.encodeJpeg(quality: Int): ByteArray =
    Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, quality)?.bytes ?: error("The picture could not be written as a JPEG")
