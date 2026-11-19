package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.language.LanguageDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MalteseParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Maltese" }.language
    private val parser = language.parser

    private fun words(text: String) = parser.parse(text, language).filter { it.isWord }.map { it.token }

    @Test
    fun theLanguageIsReadWithThisParser() {
        assertTrue(parser is MalteseParser)
    }

    @Test
    fun aWordCutShortBeforeAnotherIsAWordOfItsOwn() {
        assertEquals(listOf("Marija", "toqgħod", "f'", "Malta"), words("Marija toqgħod f'Malta."))
        assertEquals(listOf("Ġie", "b'", "mod", "sabiħ"), words("Ġie b'mod sabiħ."))
        assertEquals(listOf("M'", "hemmx", "ħobż"), words("M'hemmx ħobż."))
        assertEquals(listOf("X'", "inhu", "ismek"), words("X'inhu ismek?"))
        assertEquals(listOf("s'", "issa", "t'", "Għawdex"), words("s'issa t'Għawdex"))
    }

    @Test
    fun theCurlyApostropheIsReadAsTheStraightOne() {
        assertEquals(listOf("f'", "dan", "il", "każ"), words("f’dan il-każ"))
    }

    @Test
    fun anApostropheEndingAWordStaysWithIt() {
        assertEquals(listOf("il", "kelb", "ta'", "Pawlu", "ma'", "Anna"), words("il-kelb ta' Pawlu ma' Anna"))
        assertEquals(listOf("seta'", "jiġi"), words("seta' jiġi"))
    }

    @Test
    fun theArticleAndAPrepositionJoinedToItAreCutAtTheHyphen() {
        assertEquals(listOf("tad", "dar", "fis", "sena", "mill", "ġdid"), words("tad-dar fis-sena mill-ġdid"))
    }

    @Test
    fun otherWordsWithAnApostropheStayWhole() {
        assertEquals(listOf("d'Amato", "O'Brien"), words("d'Amato O'Brien"))
    }
}
