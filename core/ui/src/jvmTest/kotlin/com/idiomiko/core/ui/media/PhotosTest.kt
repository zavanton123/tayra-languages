package com.idiomiko.core.ui.media

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A chosen photo is read upright and no larger than needed, and a crop takes the square it shows. */
class PhotosTest {
    /** Thirds of red, green and blue side by side, [width] wide and a third as high. */
    private fun stripes(width: Int) = BufferedImage(width, width / 3, BufferedImage.TYPE_INT_RGB).apply {
        val g = createGraphics()
        listOf(Color.RED, Color.GREEN, Color.BLUE).forEachIndexed { i, colour -> g.color = colour; g.fillRect(i * width / 3, 0, width / 3, height) }
        g.dispose()
    }

    private fun encoded(image: BufferedImage, format: String) = ByteArrayOutputStream().also { ImageIO.write(image, format, it) }.toByteArray()

    private fun near(expected: Color, rgb: Int) {
        val actual = Color(rgb)
        assertTrue(abs(expected.red - actual.red) < 50 && abs(expected.green - actual.green) < 50 && abs(expected.blue - actual.blue) < 50, "$actual is not near $expected")
    }

    private fun ImageBitmap.rgb(x: Int, y: Int) = toAwtImage().getRGB(x, y)

    /** [jpeg] with an Exif block saying the camera was turned a quarter clockwise (orientation 6), as a phone writes it. */
    private fun turnedQuarter(jpeg: ByteArray): ByteArray {
        val tiff = ByteBuffer.allocate(26).put("MM".toByteArray()).putShort(42).putInt(8)
            .putShort(1).putShort(0x0112).putShort(3).putInt(1).putShort(6).putShort(0).putInt(0).array()
        val body = "Exif\u0000\u0000".toByteArray() + tiff
        val app1 = byteArrayOf(0xFF.toByte(), 0xE1.toByte()) + ByteBuffer.allocate(2).putShort((body.size + 2).toShort()).array() + body
        return jpeg.copyOfRange(0, 2) + app1 + jpeg.copyOfRange(2, jpeg.size)
    }

    @Test
    fun aPhotoTakenWithTheCameraTurnedIsReadUpright() {
        val photo = assertNotNull(decodePhoto(turnedQuarter(encoded(stripes(600), "jpeg"))))
        assertEquals(200, photo.width)
        assertEquals(600, photo.height)
    }

    @Test
    fun aLargePhotoIsReadAtTheLongestSideKept() {
        val photo = assertNotNull(decodePhoto(encoded(stripes(3000), "png")))
        assertEquals(PHOTO_MAX_SIDE, photo.width)
        assertEquals(683, photo.height)
        assertNull(decodePhoto("not a picture".toByteArray()))
        assertNull(decodePhoto(ByteArray(0)))
    }

    @Test
    fun aCropTakesTheSquareItShows() {
        val photo = stripes(900).toComposeImageBitmap()
        // At first the middle of the photo fills the square.
        val middle = PhotoCrop().cropped(photo, 300)
        assertEquals(300, middle.width)
        near(Color.GREEN, middle.rgb(5, 150))
        near(Color.GREEN, middle.rgb(295, 150))
        // Moved right as far as it goes, the photo's left third shows; it cannot be moved past its edge.
        val left = PhotoCrop(offset = Offset(5f, 0f)).within(photo)
        assertEquals(1f, left.offset.x)
        near(Color.RED, left.cropped(photo, 300).rgb(150, 150))
        // Zoomed in twice, the square holds half of the middle third.
        val zoomed = PhotoCrop(zoom = 2f, offset = Offset(0f, 0.5f)).within(photo)
        assertEquals(0.5f, zoomed.offset.y)
        near(Color.GREEN, zoomed.cropped(photo, 300).rgb(5, 5))
        assertEquals(PhotoCrop.MAX_ZOOM, PhotoCrop(zoom = 9f).within(photo).zoom)
        assertEquals(1f, PhotoCrop(zoom = 0.5f).within(photo).zoom)
    }

    @Test
    fun aCropIsWrittenAsAJpeg() {
        val jpeg = PhotoCrop().cropped(stripes(900).toComposeImageBitmap(), 512).encodeJpeg(90)
        assertEquals(0xFF.toByte(), jpeg[0])
        assertEquals(0xD8.toByte(), jpeg[1])
        val read = ImageIO.read(ByteArrayInputStream(jpeg))
        assertEquals(512, read.width)
        near(Color.GREEN, read.getRGB(256, 256))
    }
}
