package com.idiomiko.core.ui.components

import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toAwtImage
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The crop dialog: the photo dragged within the circle, and the square behind it saved as a JPEG. */
@OptIn(ExperimentalTestApi::class)
class PhotoCropDialogTest {
    private val photo = BufferedImage(900, 300, BufferedImage.TYPE_INT_RGB).apply {
        val g = createGraphics()
        listOf(Color.RED, Color.GREEN, Color.BLUE).forEachIndexed { i, colour -> g.color = colour; g.fillRect(i * 300, 0, 300, 300) }
        g.dispose()
    }.toComposeImageBitmap()

    @Test
    fun theSquareShownIsSaved() = runDesktopComposeUiTest(width = 900, height = 900) {
        var saved: ByteArray? = null
        var cancelled = false
        setContent { IdiomikoTheme { PhotoCropDialog(photo, saving = false, problem = null, onCancel = { cancelled = true }, onSave = { saved = it }) } }
        onNodeWithText("Your photo").assertExists()
        System.getenv("PHOTO_CROP_SCREENSHOT")?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[1].captureToImage().toAwtImage(), "png", File(it)) }
        // Dragged right, the photo's left third comes into the circle.
        onNodeWithTag("photo-crop-area").performTouchInput { swipeRight(startX = centerX - 100f, endX = right) }
        onNodeWithTag("photo-save").performClick()
        waitUntil(timeoutMillis = 5_000) { saved != null }
        val image = ImageIO.read(ByteArrayInputStream(assertNotNull(saved)))
        assertEquals(CROPPED_PHOTO_SIZE, image.width)
        assertEquals(CROPPED_PHOTO_SIZE, image.height)
        val middle = Color(image.getRGB(CROPPED_PHOTO_SIZE / 2, CROPPED_PHOTO_SIZE / 2))
        assertTrue(middle.red > 200 && middle.green < 80, "the left third is saved, not $middle")
        onNodeWithText("Cancel").performClick()
        assertTrue(cancelled)
    }
}
