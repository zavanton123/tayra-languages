package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.language.LanguageDefinitions
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JapaneseParserTest {
    private val japanese = LanguageDefinitions.all.first { it.name == "Japanese" }.language

    @AfterTest
    fun forget() = Lexicons.set("ja", null)

    @Test
    fun inflectionsOfTheLexiconsWordsAreOneWord() {
        Lexicons.set("ja", Lexicon(listOf("私", "は", "寿司", "を", "食べる", "ラーメン")))
        val words = japanese.parseTokens("私は寿司を食べました。ラーメンが").filter { it.isWord }.map { it.token }
        assertEquals(listOf("私", "は", "寿司", "を", "食べました", "ラーメン", "が"), words)
    }
}
