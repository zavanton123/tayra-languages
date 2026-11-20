package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.language.LanguageDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HausaParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Hausa" }.language
    private val parser = language.parser

    private fun words(text: String) = parser.parse(text, language).filter { it.isWord }.map { it.token }

    private fun keys(text: String) = words(text).map { language.lowercase(it) }

    @Test
    fun theLanguageIsReadWithThisParser() {
        assertTrue(parser is HausaParser)
    }

    @Test
    fun hookedLettersStayInsideTheirWords() {
        assertEquals(listOf("Ɗan", "ƙasa", "ya", "ɓata", "ƴaƴansa"), words("Ɗan ƙasa ya ɓata ƴaƴansa."))
        assertEquals(listOf("ɗaya", "ƙwarai", "ɓera"), words("ɗaya, ƙwarai; ɓera!"))
    }

    @Test
    fun theApostropheSpellingsAreTheHookedLetters() {
        assertEquals(listOf("ƙasa", "ɗan", "ɓata", "ƴaƴa", "ƴan"), keys("k'asa 'dan 'bata 'ya'ya 'yan"))
        assertEquals(listOf("ƙasa", "ƴar", "ƙyalli"), keys("K'asa 'Yar k'yalli"))
        assertEquals(listOf("ƴan", "ɗan"), keys("’yan ʼdan"))
        assertEquals(listOf("ƴan", "ƙasa"), keys("ƴan ƙasa"))
    }

    @Test
    fun theGlottalStopStaysInItsWord() {
        assertEquals(listOf("sa'a", "Jumma'a", "jama'a", "al'ada"), words("sa'a Jumma'a jama'a al'ada"))
        assertEquals(listOf("sa'a", "jumma'a"), keys("Sa’a Jumma’a"))
    }

    @Test
    fun aQuotationMarkIsNoPartOfAWord() {
        assertEquals("'[gida]' [ne]", parser.parse("'gida' ne", language).joinToString("") { if (it.isWord) "[${it.token}]" else it.token })
        assertEquals("'['yan]' [ne]", parser.parse("''yan' ne", language).joinToString("") { if (it.isWord) "[${it.token}]" else it.token })
        assertEquals(listOf("Isak"), words("Isak' "))
    }

    @Test
    fun foldingLeavesOtherTextAlone() {
        assertEquals("ina kwana", HausaParser.fold("ina kwana"))
        assertEquals("k' ", HausaParser.fold("k' "))
    }
}
