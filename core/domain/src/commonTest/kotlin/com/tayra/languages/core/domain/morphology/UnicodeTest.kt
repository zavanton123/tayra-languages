package com.tayra.languages.core.domain.morphology

import kotlin.test.Test
import kotlin.test.assertEquals

class UnicodeTest {
    @Test
    fun decomposesAndComposesAsJavaScriptDoes() {
        assertEquals("é", Unicode.nfd("é"))
        assertEquals("é", Unicode.nfc("é"))
        assertEquals("ἄ", Unicode.nfd("ἄ"))
        assertEquals("ἄ", Unicode.nfc("ἄ"))
        // Marks are put in canonical order, so either order composes to the same letter.
        assertEquals("ệ", Unicode.nfd("ệ"))
        assertEquals("ệ", Unicode.nfc("ệ"))
        assertEquals("й", Unicode.nfc("й"))
        assertEquals("Haus", Unicode.nfd("Haus"))
    }

    @Test
    fun hangulSyllablesSplitIntoJamoAndBack() {
        assertEquals("한", Unicode.nfd("한"))
        assertEquals("한국어", Unicode.nfc(Unicode.nfd("한국어")))
    }

    @Test
    fun compatibilityFormsBecomeTheLettersTheyStandFor() {
        assertEquals("\u064a", Unicode.nfkc("\ufef4"))
        assertEquals("\u4e00", Unicode.nfkd("\u2f00"))
        assertEquals("A1", Unicode.nfkc("\uff21\uff11"))
    }
}
