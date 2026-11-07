package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.language.LanguageDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArmenianParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Armenian" }.language
    private val parser = language.parser

    private fun words(text: String) = parser.parse(text, language).filter { it.isWord }.map { it.token }

    @Test
    fun theLanguageIsReadWithThisParser() {
        assertTrue(parser is ArmenianParser)
    }

    @Test
    fun aMarkInsideAWordKeepsItWhole() {
        assertEquals(listOf("Ինչո՞ւ", "է", "նա", "շտապում"), words("Ինչո՞ւ է նա շտապում։"))
        assertEquals(listOf("Ո՞վ", "է", "Արամը"), words("Ո՞վ է Արամը։"))
        assertEquals(listOf("Արի՛", "գնա՛նք"), words("Արի՛, գնա՛նք։"))
        assertEquals(listOf("Ա՜խ", "ինչ", "լավ", "է"), words("Ա՜խ, ինչ լավ է։"))
        assertEquals(listOf("Պրն՟", "Արամ"), words("Պրն՟ Արամ"))
    }

    @Test
    fun aMarkEndsNoSentence() {
        val ends = parser.parse("Ինչո՞ւ է նա շտապում։ Արի՛, գնա՛նք։", language).filter { it.isEndOfSentence }
        assertEquals(listOf("։ ", "։"), ends.map { it.token })
    }

    @Test
    fun theArmenianCommaAndFullStopAreNoPartOfAWord() {
        assertEquals(listOf("Գիրքը", "սեղանի", "վրա", "է", "դռան", "մոտ"), words("Գիրքը սեղանի վրա է՝ դռան մոտ։"))
    }

    @Test
    fun aWordIsMatchedWithoutItsMarks() {
        assertEquals("ինչու", parser.lowercase("Ինչո՞ւ"))
        assertEquals("ով", parser.lowercase("Ո՞վ"))
        assertEquals("գնանք", parser.lowercase("գնա՛նք"))
        assertEquals("ախ", parser.lowercase("Ա՜խ"))
    }

    @Test
    fun yechAndYiwnAreMatchedAsTheOneLetter() {
        assertEquals("նաև", parser.lowercase("նաեւ"))
        assertEquals(parser.lowercase("կարևոր"), parser.lowercase("կարեւոր"))
    }

    @Test
    fun aMarkStandingAloneIsNoWord() {
        val alone = parser.parse("Այո ՞ այո", language).single { it.token == "՞" }
        assertFalse(alone.isWord)
    }
}
