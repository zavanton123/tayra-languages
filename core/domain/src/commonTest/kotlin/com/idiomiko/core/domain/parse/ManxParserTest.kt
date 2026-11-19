package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.language.LanguageDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManxParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Manx" }.language
    private val parser = language.parser

    private fun words(text: String) = parser.parse(text, language).filter { it.isWord }.map { it.token }

    @Test
    fun theLanguageIsReadWithThisParser() {
        assertTrue(parser is ManxParser)
    }

    @Test
    fun aWordWrittenOntoTheNextIsAWordOfItsOwn() {
        assertEquals(listOf("T'", "eh", "mie"), words("T'eh mie."))
        assertEquals(listOf("v'", "ad", "ayns", "dt'", "ennym"), words("v'ad ayns dt'ennym"))
        assertEquals(listOf("s'", "mie", "lhiam", "eh"), words("s'mie lhiam eh"))
    }

    @Test
    fun theAndAndWrittenOntoTheWordBeforeAreWordsOfTheirOwn() {
        assertEquals(listOf("Ta", "'n", "thie", "mooar"), words("Ta'n thie mooar"))
        assertEquals(listOf("lane", "jeh", "'n", "ushtey"), words("lane jeh'n ushtey"))
        assertEquals(listOf("chammah", "'s", "eshyn"), words("chammah's eshyn"))
        assertEquals(listOf("t'", "ou", "'n", "fer"), words("t'ou'n fer"))
    }

    @Test
    fun theCurlyApostropheIsTheStraightOne() {
        assertEquals(listOf("t'", "eh", "ta", "'n"), words("t’eh ta’n"))
    }

    @Test
    fun anApostropheAtAWordsEdgeIsNoPartOfIt() {
        assertEquals(listOf("Ta", "eh", "sy", "thie"), words("'Ta eh 'sy thie'"))
        assertEquals("'Ta eh 'sy thie'", parser.parse("'Ta eh 'sy thie'", language).joinToString("") { it.token })
    }

    @Test
    fun aMutationsLetterBeforeAVowelIsNoWord() {
        assertEquals(listOf("ny", "ellanyn", "yn", "ushtey"), words("ny h-ellanyn, yn t-ushtey"))
        assertEquals(listOf("Ellan", "Vannin", "as", "ny", "Ellanyn"), words("Ellan Vannin as ny h-Ellanyn"))
        assertEquals("ny h-ellanyn", parser.parse("ny h-ellanyn", language).joinToString("") { it.token })
    }

    @Test
    fun aHyphenPartsTwoWords() {
        assertEquals(listOf("ry", "akin", "ard", "valley"), words("ry-akin ard-valley"))
        assertEquals(listOf("h", "Pobblaght"), words("h-Pobblaght"))
    }

    @Test
    fun anApostropheInsideAWordKeepsItWhole() {
        assertEquals(listOf("c'red", "nee'm"), words("c'red nee'm"))
    }
}
