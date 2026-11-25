package com.idiomiko.core.domain.fonts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FontFilesTest {
    private fun file(vararg head: Int) = ByteArray(64).also { head.forEachIndexed { i, b -> it[i] = b.toByte() } }

    @Test
    fun trueTypeAndOpenTypeFilesAreFontsAndOthersAreNot() {
        assertTrue(FontFiles.isFont(file(0x00, 0x01, 0x00, 0x00)), "TrueType")
        assertTrue(FontFiles.isFont(file('O'.code, 'T'.code, 'T'.code, 'O'.code)), "OpenType")
        assertTrue(FontFiles.isFont(file('t'.code, 'r'.code, 'u'.code, 'e'.code)), "Apple TrueType")
        assertFalse(FontFiles.isFont(file('t'.code, 't'.code, 'c'.code, 'f'.code)), "a collection of fonts")
        assertFalse(FontFiles.isFont(file('w'.code, 'O'.code, 'F'.code, '2'.code)), "a web font")
        assertFalse(FontFiles.isFont("Just some text, not a font.".encodeToByteArray()))
        assertFalse(FontFiles.isFont(ByteArray(3)))
    }

    @Test
    fun aFontIsNamedAfterItsFile() {
        assertEquals("MyFont", FontFiles.nameOf("MyFont-Regular.ttf"))
        assertEquals("Old Book Face Bold", FontFiles.nameOf("/fonts/Old_Book-Face-Bold.otf"))
        assertEquals("Merriweather", FontFiles.nameOf("Merriweather.ttf"))
    }
}
