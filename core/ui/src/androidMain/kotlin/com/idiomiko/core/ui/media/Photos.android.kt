package com.idiomiko.core.ui.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max

internal actual fun decodeUpright(bytes: ByteArray, maxSide: Int): ImageBitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) decoded(bytes, maxSide) else factoryDecoded(bytes, maxSide)
}.getOrNull()?.asImageBitmap()

// The decoder turns the photo upright and reads HEIC; a software bitmap, as the crop draws it on a canvas of its own.
@android.annotation.TargetApi(Build.VERSION_CODES.P)
private fun decoded(bytes: ByteArray, maxSide: Int): Bitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetSampleSize(sample(max(info.size.width, info.size.height), maxSide))
    }

// Before Android 9 the photo is turned by hand, as its Exif orientation says.
private fun factoryDecoded(bytes: ByteArray, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val options = BitmapFactory.Options().apply { inSampleSize = sample(max(bounds.outWidth, bounds.outHeight), maxSide) }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
    val turn = when (ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(turn) }, true)
}

/** The largest power of two to decode [longest] pixels by and still have at least [maxSide]. */
private fun sample(longest: Int, maxSide: Int): Int {
    var sample = 1
    while (longest / (sample * 2) >= maxSide) sample *= 2
    return sample
}

actual fun ImageBitmap.encodeJpeg(quality: Int): ByteArray =
    ByteArrayOutputStream().also { asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
