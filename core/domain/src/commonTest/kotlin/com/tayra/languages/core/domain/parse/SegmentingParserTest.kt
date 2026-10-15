package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.model.Language
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SegmentingParserTest {
    private val parser = SegmentingParser("Test", "xx", joinUnknown = true)
    private val language = Language(name = "Test", wordCharacters = "a-z", regexpSplitSentences = ".", parserType = "test")

    @AfterTest
    fun forget() = Lexicons.set("xx", null)

    private fun words(text: String) = parser.parse(text, language).filter { it.isWord }.map { it.token }

    @Test
    fun cutsTextIntoTheLongestWordsTheLexiconKnows() {
        Lexicons.set("xx", Lexicon(listOf("the", "there", "is", "a", "cat", "cats", "here")))
        assertEquals(listOf("there", "is", "a", "cat"), words("thereisacat"))
        assertEquals(listOf("cats", "here"), words("catshere"))
    }

    @Test
    fun lettersNoWordCoversStayTogether() {
        Lexicons.set("xx", Lexicon(listOf("cat")))
        assertEquals(listOf("zz", "cat", "q"), words("zzcatq"))
        val tokens = parser.parse("cat. cat", language)
        assertEquals(listOf("cat", ". ", "cat"), tokens.map { it.token })
        assertEquals(true, tokens[1].isEndOfSentence)
    }

    @Test
    fun withoutALexiconEachRunIsOneWord() {
        assertEquals(listOf("thereisacat"), words("thereisacat"))
        assertEquals(listOf("t", "h", "e"), SegmentingParser("Test", "xx", joinUnknown = false).parse("the", language).map { it.token })
    }
}
