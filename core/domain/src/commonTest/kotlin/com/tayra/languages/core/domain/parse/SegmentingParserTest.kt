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

    /** Cut by frequency, two common words are not read as a rarer one that spans them. */
    @Test
    fun byFrequencyTheLikeliestWordsWin() {
        val chinese = SegmentingParser("Chinese", "xx", joinUnknown = false, byFrequency = true)
        val language = Language(name = "Chinese", wordCharacters = "一-龥", regexpSplitSentences = "。", parserType = "test")
        val ranks = mapOf("我" to 5, "不" to 8, "知道" to 90, "不知" to 3000, "道" to 800, "从" to 60, "中国" to 31, "从中" to 2500, "国" to 900, "他" to 15, "来" to 29)
        Lexicons.set("xx", Lexicon(ranks.keys, ranks))
        fun cut(text: String) = chinese.parse(text, language).filter { it.isWord }.map { it.token }
        assertEquals(listOf("我", "不", "知道"), cut("我不知道"))
        assertEquals(listOf("他", "从", "中国", "来"), cut("他从中国来"))
        // A character no word covers is a word of its own; a word the lexicon has without a rank still counts.
        Lexicons.set("xx", Lexicon(ranks.keys + "猫咪", ranks))
        assertEquals(listOf("他", "猫咪", "好"), cut("他猫咪好"))
    }

    @Test
    fun withoutALexiconEachRunIsOneWord() {
        assertEquals(listOf("thereisacat"), words("thereisacat"))
        assertEquals(listOf("t", "h", "e"), SegmentingParser("Test", "xx", joinUnknown = false).parse("the", language).map { it.token })
    }
}
