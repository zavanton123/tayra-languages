package com.idiomiko.core.domain.term

import com.idiomiko.core.domain.TestLanguages
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
}
