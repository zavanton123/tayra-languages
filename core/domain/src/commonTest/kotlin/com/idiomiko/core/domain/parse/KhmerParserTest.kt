package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.language.LanguageDefinitions
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KhmerParserTest {
    private val khmer = LanguageDefinitions.all.single { it.language.name == "Khmer" }.language

    @AfterTest
    fun forget() = Lexicons.set("km", null)

    private fun words(text: String) = khmer.parseTokens(text).filter { it.isWord }.map { it.token }

    @Test
    fun khmerHasItsOwnParser() {
        assertTrue(khmer.parser is KhmerParser)
    }

    /** កម "scabies" ends before a coeng, ក្ដ before a vowel sign: neither is read inside the word it starts. */
    @Test
    fun aWordNeverEndsInsideASyllable() {
        Lexicons.set("km", Lexicon(listOf("កម", "កម្រិត", "ខ្ពស់", "សេចក្តី", "ក្ដ", "ស្រឡាញ់", "ម្ដាយ", "ទៅ", "ផ្សារ", "ក", "ម", "រ")))
        assertEquals(listOf("កម្រិត", "ខ្ពស់"), words("កម្រិតខ្ពស់"))
        assertEquals(listOf("សេចក្ដី", "ស្រឡាញ់"), words("សេចក្ដីស្រឡាញ់"))
        // A word the lexicon lacks stays one word: no consonant alone is read out of it, nor កម before its coeng.
        assertEquals(listOf("កម្ពុជា"), words("កម្ពុជា"))
        assertEquals(listOf("ម្ដាយ", "ទៅ", "ផ្សារ"), words("ម្ដាយទៅផ្សារ"))
    }

    /** Subscript ដ and ត look alike: the lexicon's word is found in either spelling and saved in the one with ត. */
    @Test
    fun subscriptDaAndTaAreTheSame() {
        Lexicons.set("km", Lexicon(listOf("សប្តាហ៍", "មិត្តភក្ដិ", "របស់", "នាង")))
        assertEquals(listOf("សប្ដាហ៍", "មិត្តភក្តិ", "របស់", "នាង"), words("សប្ដាហ៍មិត្តភក្តិរបស់នាង"))
        assertEquals("សប្តាហ៍", khmer.lowercase("សប្ដាហ៍"))
        assertEquals(khmer.lowercase("មិត្តភក្ដិ"), khmer.lowercase("មិត្តភក្តិ"))
        assertEquals(listOf("មិត្តភក្ដិ", "មិត្តភក្តិ", "មិត្ដភក្តិ", "មិត្ដភក្ដិ").sorted(), KhmerParser.spellings("មិត្តភក្ដិ").sorted())
    }

    /** A subscript រ is written before or after the subscript beside it: the word is found either way, and saved with it after. */
    @Test
    fun subscriptRoIsMatchedInEitherOrder() {
        Lexicons.set("km", Lexicon(listOf("សាស្ត្រ", "សង្គ្រាម", "និង")))
        assertEquals(listOf("សាស្រ្ត", "និង", "សង្គ្រាម"), words("សាស្រ្តនិងសង្គ្រាម"))
        assertEquals("សាស្ត្រ", khmer.lowercase("សាស្រ្ត"))
        assertEquals("ស្ត្រី", khmer.lowercase("ស្ត្រី"))
    }

    /** ។ and ៕ are no words and end sentences; ៖, ៗ and the digits are no words either. */
    @Test
    fun punctuationAndDigitsAreNoWords() {
        Lexicons.set("km", Lexicon(listOf("ខ្ញុំ", "ទៅ", "ផ្សារ", "ក្មេង", "លេង", "ឆ្នាំ", "។")))
        val tokens = khmer.parseTokens("ខ្ញុំទៅផ្សារ។ ក្មេងៗលេង៖ ឆ្នាំ២០២៤។")
        assertEquals(listOf("ខ្ញុំ", "ទៅ", "ផ្សារ", "ក្មេង", "លេង", "ឆ្នាំ"), tokens.filter { it.isWord }.map { it.token })
        assertEquals(listOf(0, 0, 0, 1, 1, 1), tokens.filter { it.isWord }.map { it.sentenceNumber })
        assertTrue(tokens.single { it.token.startsWith("។ ") }.isEndOfSentence)
    }
}
