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

    /** An own font file sets text in itself; one that is gone, or whose file cannot be read, gives way to the default. */
    @Test
    fun ownFontsSetTextInThemselvesAndFallBackToTheDefault() = runDesktopComposeUiTest(width = 800, height = 600) {
        val lora = java.io.File("src/commonMain/composeResources/font/lora.ttf").readBytes()
        val broken = ByteArray(4096).also { it[1] = 1 }
        val fonts = FakeOwnFonts(mapOf(1L to ("Mine" to lora), 2L to ("Broken" to broken)))
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalOwnFonts provides fonts) {
                Column {
                    Text("Hamburgefonstiv", Modifier.testTag("default"), fontFamily = readingFontFamily(ReadingFont.SERIF.id))
                    Text("Hamburgefonstiv", Modifier.testTag("lora"), fontFamily = ReadingFont.LORA.fontFamily())
                    Text("Hamburgefonstiv", Modifier.testTag("own"), fontFamily = readingFontFamily("own:1"))
                    Text("Hamburgefonstiv", Modifier.testTag("gone"), fontFamily = readingFontFamily("own:9"))
                    Text("Hamburgefonstiv", Modifier.testTag("broken"), fontFamily = readingFontFamily("own:2"))
                }
            }
        }
        fun pixels(tag: String): List<Int> = onNodeWithTag(tag).captureToImage().toAwtImage().let { image ->
            List(image.width * image.height) { image.getRGB(it % image.width, it / image.width) }
        }
        waitUntil(timeoutMillis = 5_000) { pixels("own") == pixels("lora") }
        assertEquals(pixels("default"), pixels("gone"))
        assertEquals(pixels("default"), pixels("broken"))
        assertEquals(FontChoice.Own(com.idiomiko.core.domain.fonts.OwnFont(1, "Mine")).id, "own:1")
        assertEquals(1L, FontChoice.ownFontId("own:1"))
        assertEquals(null, FontChoice.ownFontId("lora"))
    }
}

/** Own fonts held in memory: id to name and file. */
internal class FakeOwnFonts(initial: Map<Long, Pair<String, ByteArray>>) : com.idiomiko.core.domain.fonts.OwnFontRepository {
    private val files = kotlinx.coroutines.flow.MutableStateFlow(initial)
    override val fonts = kotlinx.coroutines.flow.MutableStateFlow(initial.map { (id, f) -> com.idiomiko.core.domain.fonts.OwnFont(id, f.first) })
    override suspend fun add(fileName: String, bytes: ByteArray) = null
    override suspend fun remove(id: Long) {}
    override suspend fun data(id: Long): ByteArray? = files.value[id]?.second
}
