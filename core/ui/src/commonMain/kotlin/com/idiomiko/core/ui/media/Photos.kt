package com.idiomiko.core.ui.media

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.idiomiko.core.ui.theme.AppAccents
import kotlin.math.max
import kotlin.math.roundToInt

/** The longest side a chosen photo is kept at while it is cropped: enough for any picture made from it. */
const val PHOTO_MAX_SIDE = 2048

/** A photo's file (PNG, JPEG, and HEIC where the platform reads it) as an upright bitmap, its longest side at most [maxSide]. */
fun decodePhoto(bytes: ByteArray, maxSide: Int = PHOTO_MAX_SIDE): ImageBitmap? = decodeUpright(bytes, maxSide)?.let { shrunk(it, maxSide) }

/** The image in [bytes] turned as its orientation says, at about [maxSide] where the platform can decode it smaller; null when it cannot be read. */
internal expect fun decodeUpright(bytes: ByteArray, maxSide: Int): ImageBitmap?

/** The bitmap as a JPEG of [quality], 0 to 100. */
expect fun ImageBitmap.encodeJpeg(quality: Int): ByteArray

private fun shrunk(image: ImageBitmap, maxSide: Int): ImageBitmap {
    val longest = max(image.width, image.height)
    if (longest <= maxSide) return image
    val scale = maxSide.toFloat() / longest
    val size = IntSize((image.width * scale).roundToInt().coerceAtLeast(1), (image.height * scale).roundToInt().coerceAtLeast(1))
    return ImageBitmap(size.width, size.height).also { out ->
        Canvas(out).drawImageRect(image, IntOffset.Zero, IntSize(image.width, image.height), IntOffset.Zero, size, Paint().apply { filterQuality = FilterQuality.High })
    }
}

/**
 * Where a photo sits behind a square: [zoom] times the scale at which it just covers the square, from 1 to [MAX_ZOOM],
 * and its centre moved by [offset] from the square's, in sides of the square.
 */
data class PhotoCrop(val zoom: Float = 1f, val offset: Offset = Offset.Zero) {
    /** How many pixels of a square [side] pixels wide one pixel of [image] takes. */
    fun scale(image: ImageBitmap, side: Float): Float = max(side / image.width, side / image.height) * zoom

    /** This crop with [zoom] in range and [offset] kept where the photo still covers the whole square. */
    fun within(image: ImageBitmap): PhotoCrop {
        val zoom = zoom.coerceIn(1f, MAX_ZOOM)
        val scale = PhotoCrop(zoom).scale(image, 1f)
        val maxX = (image.width * scale - 1f) / 2f
        val maxY = (image.height * scale - 1f) / 2f
        return PhotoCrop(zoom, Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY)))
    }

    /** The square of [image] this crop shows, drawn [size] pixels a side on white, as a photo has no transparency. */
    fun cropped(image: ImageBitmap, size: Int): ImageBitmap {
        val crop = within(image)
        val scale = crop.scale(image, size.toFloat())
        val width = image.width * scale
        val height = image.height * scale
        val left = size / 2f + crop.offset.x * size - width / 2f
        val top = size / 2f + crop.offset.y * size - height / 2f
        return ImageBitmap(size, size).also { out ->
            val canvas = Canvas(out)
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), Paint().apply { color = AppAccents.onAccent })
            canvas.drawImageRect(
                image, IntOffset.Zero, IntSize(image.width, image.height),
                IntOffset(left.roundToInt(), top.roundToInt()), IntSize(width.roundToInt(), height.roundToInt()),
                Paint().apply { filterQuality = FilterQuality.High },
            )
        }
    }

    companion object {
        const val MAX_ZOOM = 4f
    }
}
