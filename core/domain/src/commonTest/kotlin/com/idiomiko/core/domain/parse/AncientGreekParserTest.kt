package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.language.LanguageDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AncientGreekParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Ancient Greek" }.language
    private val parser = language.parser

    private fun words(text: String) = parser.parse(text, language).filter { it.isWord }.map { it.token }

    @Test
    fun theLanguageIsReadWithThisParser() {
        assertTrue(parser is AncientGreekParser)
    }

    @Test
    fun theGraveIsMatchedAsTheAcute() {
        assertEquals("καλός", parser.lowercase("καλὸς"))
        assertEquals("ἀγοράν", parser.lowercase("ἀγορὰν"))
        assertEquals("καί", parser.lowercase("καὶ"))
        assertEquals(parser.lowercase("τοὺς"), parser.lowercase("τούς"))
        assertEquals("αὐτή", parser.lowercase("αὐτὴ"))
    }

    @Test
    fun theSecondAccentOfAnEncliticIsLeftOut() {
        assertEquals("ἄνθρωπος", parser.lowercase("ἄνθρωπός"))
        assertEquals("εἶπεν", parser.lowercase("εἶπέν"))
        assertEquals("σῶμα", parser.lowercase("σῶμά"))
        assertEquals("τοῦτο", parser.lowercase("τοῦτό"))
    }

    @Test
    fun breathingsCircumflexAndIotaSubscriptStay() {
        assertEquals("ἡ", parser.lowercase("Ἡ"))
        assertNotEquals(parser.lowercase("ἤ"), parser.lowercase("ἡ"))
        assertEquals("ἀγορᾷ", parser.lowercase("ἀγορᾷ"))
        assertEquals("τῷ", parser.lowercase("τῷ"))
        assertEquals("ἐστιν", parser.lowercase("ἐστιν"))
        assertEquals("ἔστιν", parser.lowercase("Ἔστιν"))
    }

    @Test
    fun theOxiaIsTheTonos() {
        assertEquals("καλ\u03CCς", parser.lowercase("καλ\u1F79ς"))
    }

    @Test
    fun aPhraseIsMatchedWordByWord() {
        assertEquals("καλός\u200Bἀνήρ", parser.lowercase("Καλὸς\u200Bἀνήρ"))
        assertEquals("ἄνθρωπος τις", parser.lowercase("ἄνθρωπός τις"))
    }

    @Test
    fun wordsAreCutAtGreekPunctuation() {
        assertEquals(listOf("τί", "λέγεις", "ὁ", "πατὴρ", "ἐν", "τῇ", "ἀγορᾷ", "ἐστιν"), words("Τί λέγεις; ὁ πατὴρ ἐν τῇ ἀγορᾷ ἐστιν·").map { it.lowercase() })
    }
}
