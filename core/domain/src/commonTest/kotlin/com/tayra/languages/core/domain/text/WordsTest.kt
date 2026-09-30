package com.tayra.languages.core.domain.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WordsTest {
    private fun wordAt(text: String, offset: Int): String? = Words.rangeAt(text, offset)?.let { text.substring(it.first, it.last + 1) }

    @Test
    fun findsTheWordAroundAnOffset() {
        val s = "Dizem no meu país que existe uma maldição."
        assertEquals("Dizem", wordAt(s, 0))
        assertEquals("Dizem", wordAt(s, 4))
        assertEquals("Dizem", wordAt(s, 5))
        assertEquals("país", wordAt(s, 15))
        assertEquals("maldição", wordAt(s, 36))
        assertEquals("maldição", wordAt(s, s.length - 1))
    }

    @Test
    fun keepsInnerApostrophesAndHyphensButNotOuterOnes() {
        assertEquals("aujourd'hui", wordAt("C'est aujourd'hui.", 9))
        assertEquals("well-known", wordAt("a well-known 'fact'", 5))
        assertEquals("fact", wordAt("a well-known 'fact'", 15))
    }

    @Test
    fun ignoresSpacesPunctuationAndNumbers() {
        assertNull(wordAt("a  b", 2))
        assertNull(wordAt("— 1999 —", 3))
        assertNull(wordAt("", 0))
    }

    @Test
    fun handlesCyrillicAndCombiningMarks() {
        assertEquals("Лобисомем", wordAt("Это Лобисомем.", 6))
        val decomposed = "cafe\u0301 noir"
        assertEquals("cafe\u0301", wordAt(decomposed, 2))
    }
}
