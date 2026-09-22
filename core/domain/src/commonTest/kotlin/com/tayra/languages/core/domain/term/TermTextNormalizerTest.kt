package com.tayra.languages.core.domain.term

import com.tayra.languages.core.domain.TestLanguages
import kotlin.test.Test
import kotlin.test.assertEquals

class TermTextNormalizerTest {
    @Test
    fun multiwordTextIsTokenised() {
        val term = TermTextNormalizer.termFromText(TestLanguages.english, "  A Dog\n ")
        assertEquals("A​ ​Dog", term.text)
        assertEquals("a​ ​dog", term.textLc)
        assertEquals(3, term.tokenCount)
    }

    @Test
    fun csvRoundTrip() {
        val rows = listOf(listOf("a", "b,c", "d\"e"), listOf("1", "", "x"))
        assertEquals(rows, Csv.parse(Csv.format(rows)))
    }
}
