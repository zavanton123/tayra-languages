package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.TestLanguages
import com.tayra.languages.core.domain.model.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceDelimitedParserTest {

    private val parser = SpaceDelimitedParser()

    private fun tokensAsString(text: String, language: Language): String =
        parser.parse(text, language).joinToString("") { if (it.isWord) "[${it.token}]" else it.token }

    private fun tokenTriples(text: String, language: Language) =
        parser.parse(text, language).map { Triple(it.token, it.isWord, it.isEndOfSentence) }

    @Test
    fun endOfSentenceIsMarked() {
        val expected = listOf(
            Triple("Tengo", true, false), Triple(" ", false, false), Triple("un", true, false),
            Triple(" ", false, false), Triple("gato", true, false), Triple(".", false, true),
            Triple("¶", false, true),
            Triple("Tengo", true, false), Triple(" ", false, false), Triple("dos", true, false),
            Triple(".", false, true),
        )
        assertEquals(expected, tokenTriples("Tengo un gato.\nTengo dos.", TestLanguages.spanish))
    }

    @Test
    fun exceptionsDoNotSplitSentences() {
        val expected = listOf(
            Triple("1. ", false, true), Triple("Mrs.", true, false), Triple(" ", false, false),
            Triple("Jones", true, false), Triple(" ", false, false), Triple("is", true, false),
            Triple(" ", false, false), Triple("here", true, false), Triple(".", false, true),
        )
        assertEquals(expected, tokenTriples("1. Mrs. Jones is here.", TestLanguages.english))
    }

    @Test
    fun multiDotExceptionIsOneToken() {
        val spanish = TestLanguages.spanish.copy(exceptionsSplitSentences = "EE.UU.")
        assertEquals("[Estamos] [en] [EE.UU.] [hola].", tokensAsString("Estamos en EE.UU. hola.", spanish))
        assertEquals("[EE.UU.]", tokensAsString("EE.UU.", spanish))
    }

    @Test
    fun quickChecks() {
        val english = TestLanguages.english
        assertEquals("[test]", tokensAsString("test", english))
        assertEquals("[test].", tokensAsString("test.", english))
        assertEquals("\"[test].\"", tokensAsString("\"test.\"", english))
        assertEquals("[Hi] [there].", tokensAsString("Hi there.", english))
        assertEquals("[Hi] [there]. [Goodbye].", tokensAsString("Hi there.  Goodbye.", english))
        assertEquals("[Hi].¶[Goodbye].", tokensAsString("Hi.\nGoodbye.", english))
        assertEquals("[He]123[llo].", tokensAsString("He123llo.", english))
        assertEquals("1234", tokensAsString("1234", english))
        assertEquals("1234.[Hello]", tokensAsString("1234.Hello", english))
        assertEquals("[Tengo] [que] [y] [qué].", tokensAsString("Tengo que y qué.", TestLanguages.spanish))
    }

    @Test
    fun characterSubstitutionsApplied() {
        assertEquals("[l]'[homme]…", tokensAsString("l’homme...", TestLanguages.english))
    }

    @Test
    fun zeroWidthNonJoinerRetainedWhenWordCharacter() {
        assertEquals("[Brot‌zeit]", tokensAsString("Brot‌zeit", TestLanguages.german))
    }

    @Test
    fun defaultWordPatternHandlesUnicodeLetters() {
        val generic = TestLanguages.generic
        assertEquals("[Saint] [Denis] [be] [my] [speed]!—[donc] [vôtre] [est] [France], [et] [vous] [êtes] [mienne].",
            tokensAsString("Saint Denis be my speed!—donc vôtre est France, et vous êtes mienne.", generic))
        assertEquals("[Встреча] [с] [медведем].", tokensAsString("Встреча с медведем.", generic))
        assertEquals("[नमस‍ते]", tokensAsString("नमस‍ते", generic))
        assertEquals("[日本語]。", tokensAsString("日本語。", generic))
    }

    @Test
    fun legacyHexEscapesAreConverted() {
        val arabic = TestLanguages.generic.copy(wordCharacters = "\\x{0600}-\\x{06FF}")
        assertEquals("[مرحبا] [بكم]", tokensAsString("مرحبا بكم", arabic))
    }

    @Test
    fun turkishLowercase() {
        assertEquals("ırmak iyi", TurkishParser().lowercase("IRMAK İYİ"))
    }

    @Test
    fun numberedAssignsSentences() {
        val tokens = parser.parse("Hi there. Bye.", TestLanguages.english).numbered()
        assertEquals(listOf("Hi", " ", "there", ". ", "Bye", "."), tokens.map { it.token })
        assertEquals(listOf(0, 0, 0, 0, 1, 1), tokens.map { it.sentenceNumber })
        assertEquals((1..6).toList(), tokens.map { it.order })
    }
}
