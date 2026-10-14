package com.tayra.languages.core.domain.morphology

import kotlin.test.Test
import kotlin.test.assertTrue

/** The spellings each language's ported text processors give a word. */
class TextProcessorsTest {
    private fun spellings(code: String, word: String) = Morphology.forLanguage(code)!!.preprocess(word)

    @Test
    fun german() {
        assertTrue("Straße" in spellings("de", "Strasse"))
        assertTrue("Strasse" in spellings("de", "Straße"))
    }

    @Test
    fun french() {
        assertTrue("l\u2019homme" in spellings("fr", "l'homme"))
    }

    @Test
    fun greek() {
        assertTrue("πρόσωπο" in spellings("el", "πρόσωπό"))
    }
}
