package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.language.LanguageDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UzbekParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Uzbek" }.language
    private val parser = language.parser

    private fun words(text: String) = parser.parse(text, language).filter { it.isWord }.map { it.token }

    @Test
    fun theLanguageIsReadWithThisParser() {
        assertTrue(parser is UzbekParser)
    }

    @Test
    fun everyMarkAfterOAndGIsTheTurnedComma() {
        val words = listOf("Oʻzbekiston", "togʻri", "soʻz")
        assertEquals(words, words("Oʻzbekiston togʻri soʻz."))
        assertEquals(words, words("O'zbekiston tog'ri so'z."))
        assertEquals(words, words("O‘zbekiston tog‘ri so‘z."))
        assertEquals(words, words("O’zbekiston tog’ri so’z."))
        assertEquals(words, words("O`zbekiston tog`ri so`z."))
    }

    @Test
    fun aGEndingAWordKeepsItsMark() {
        assertEquals(listOf("bogʻ", "va", "togʻ"), words("bog' va tog‘."))
    }

    @Test
    fun aMarkBetweenOtherLettersIsTheTutuqBelgisi() {
        assertEquals(listOf("maʼno", "sanʼat", "taʼlim"), words("ma'no, san’at, taʼlim"))
    }

    @Test
    fun quotationMarksStayApart() {
        assertEquals(listOf("U", "keling", "dedi", "kino"), words("U ‘keling’ dedi. 'kino'"))
    }

    @Test
    fun theMarksAreLowercasedAsTheTextIsRead() {
        assertEquals("oʻzbek maʼno", parser.lowercase("O'zbek Ma’no"))
        assertEquals(UzbekParser.normalized("o'g'il").length, "o'g'il".length)
        assertEquals("oʻgʻil", UzbekParser.normalized("o'g'il"))
    }
}
