package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.language.LanguageDefinitions
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
        // After a number, a measure word goes with it, not with the word after it.
        Lexicons.set("xx", Lexicon(listOf("两", "个", "人", "个人"), mapOf("两" to 220, "个" to 37, "人" to 9, "个人" to 600)))
        assertEquals(listOf("两", "个", "人"), cut("两个人"))
        assertEquals(listOf("个人"), cut("个人"))
        // A character no word covers is a word of its own; a word the lexicon has without a rank still counts.
        Lexicons.set("xx", Lexicon(ranks.keys + "猫咪", ranks))
        assertEquals(listOf("他", "猫咪", "好"), cut("他猫咪好"))
    }

    private val cantonese get() = LanguageDefinitions.all.single { it.language.name == "Cantonese" }.language

    private fun cantoneseWords(text: String) = cantonese.parseTokens(text).filter { it.isWord }.map { it.token }

    /** 𨋢 lies beyond the Basic Multilingual Plane, in two UTF-16 units; in Cantonese it is a word like any other. */
    @Test
    fun aCantoneseCharacterBeyondTheBasicPlaneIsAWord() {
        val ranks = mapOf("我哋" to 2685, "搭" to 900, "𨋢" to 2888, "上去" to 700)
        try {
            Lexicons.set("yue", Lexicon(ranks.keys, ranks))
            val tokens = cantonese.parseTokens("我哋搭𨋢上去。")
            assertEquals(listOf("我哋", "搭", "𨋢", "上去", "。"), tokens.map { it.token })
            assertEquals(listOf(true, true, true, true, false), tokens.map { it.isWord })
            // One no word covers stays whole, and is a word still.
            Lexicons.set("yue", Lexicon(ranks.keys - "𨋢", ranks - "𨋢"))
            assertEquals(listOf("我哋", "搭", "𨋢", "上去"), cantoneseWords("我哋搭𨋢上去"))
            assertEquals(listOf("𨋢"), cantoneseWords("𨋢"))
            // Under a lexicon of one-unit words, one with no ranks, and none at all.
            Lexicons.set("yue", Lexicon(listOf("搭"), mapOf("搭" to 900)))
            assertEquals(listOf("搭", "𨋢", "搭"), cantoneseWords("搭𨋢搭"))
            Lexicons.set("yue", Lexicon(listOf("搭")))
            assertEquals(listOf("搭", "𨋢"), cantoneseWords("搭𨋢"))
            Lexicons.set("yue", null)
            assertEquals(listOf("搭", "𨋢"), cantoneseWords("搭𨋢"))
        } finally {
            Lexicons.set("yue", null)
        }
    }

    /** An emoji is two units as well and stays no word, and other languages keep such characters out of their words. */
    @Test
    fun onlyHanInCantoneseBecomesAWord() {
        val tokens = cantonese.parseTokens("搭😀𨋢")
        assertEquals(listOf("搭", "😀", "𨋢"), tokens.map { it.token })
        assertEquals(listOf(true, false, true), tokens.map { it.isWord })
        val mandarin = SegmentingParser("Chinese", "xx", joinUnknown = false, byFrequency = true)
        val han = Language(name = "Chinese", wordCharacters = "一-龥", regexpSplitSentences = "。", parserType = "test")
        assertEquals(listOf("搭" to true, "𨋢" to false), mandarin.parse("搭𨋢", han).map { it.token to it.isWord })
    }

    @Test
    fun withoutALexiconEachRunIsOneWord() {
        assertEquals(listOf("thereisacat"), words("thereisacat"))
        assertEquals(listOf("t", "h", "e"), SegmentingParser("Test", "xx", joinUnknown = false).parse("the", language).map { it.token })
    }
}
