package com.idiomiko.core.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class ReadingFontsTest {

    /** Every bundled font loads and sets text in a face of its own, so none falls back to the system's. */
    @Test
    fun eachBundledFontSetsTextInItsOwnFace() = runDesktopComposeUiTest(width = 800, height = 1200) {
        val bundled = ReadingFont.entries.filter { it.bundled }
        setContent {
            Column {
                Text("Hamburgefonstiv", Modifier.testTag("system"), fontFamily = ReadingFont.SANS.fontFamily())
                for (font in bundled) Text("Hamburgefonstiv", Modifier.testTag(font.id), fontFamily = font.fontFamily())
            }
        }
        fun pixels(tag: String): List<Int> = onNodeWithTag(tag).captureToImage().toAwtImage().let { image ->
            List(image.width * image.height) { image.getRGB(it % image.width, it / image.width) }
        }
        val faces = bundled.associateWith { pixels(it.id) }
        val system = pixels("system")
        for ((font, face) in faces) assertNotEquals(system, face, "${font.label} is set in the system font")
        assertEquals(faces.size, faces.values.toSet().size, "each font draws the text in a face of its own")
    }

    @Test
    fun storedIdsFindTheirFont() {
        for (font in ReadingFont.entries) assertEquals(font, ReadingFont.byId(font.id))
    }
}
