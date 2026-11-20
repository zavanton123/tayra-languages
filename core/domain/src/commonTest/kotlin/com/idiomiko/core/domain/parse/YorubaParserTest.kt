package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.language.LanguageDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YorubaParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Yoruba" }.language
    private val parser = language.parser

    private fun words(text: String) = parser.parse(text, language).filter { it.isWord }.map { it.token }

    @Test
    fun theLanguageIsReadWithThisParser() {
        assertTrue(parser is YorubaParser)
    }

    @Test
    fun aWordKeepsItsCombiningMarks() {
        // ẹ́ and ọ̀ have no single letter: ẹ or ọ and a combining acute or grave, inside the word.
        assertEquals(listOf("Ọ̀rẹ́", "mi", "ń", "lọ", "sí", "ọjà", "lọ́la"), words("Ọ̀rẹ́ mi ń lọ sí ọjà lọ́la."))
        assertEquals(listOf("ọkọ̀", "ọkọ", "ọkọ́", "àti", "oko"), words("ọkọ̀, ọkọ, ọkọ́ àti oko"))
        assertEquals(listOf("ǹjẹ́", "o", "rí", "i"), words("Ǹjẹ́ o rí i?").map { it.lowercase() })
    }

    @Test
    fun decomposedTextIsReadAsTheComposedWord() {
        val decomposed = "ọkọ̀ ẹ́ ṣe"
        assertEquals(listOf("ọkọ̀", "ẹ́", "ṣe"), words(decomposed))
        assertEquals("ọkọ̀", language.lowercase("ỌKỌ̀"))
    }

    @Test
    fun theLineBelowIsTheDotBelow() {
        assertEquals(listOf("ọmọ", "ẹ̀kọ́"), words("o̩mo̩ è̩kó̩"))
    }

    @Test
    fun aHyphenPartsWords() {
        assertEquals(listOf("orílẹ̀", "èdè", "gan", "an"), words("orílẹ̀-èdè gan-an"))
    }
}
