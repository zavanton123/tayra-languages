package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.language.LanguageDefinitions
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ThaiParserTest {
    private val thai = LanguageDefinitions.all.single { it.language.name == "Thai" }.language

    @AfterTest
    fun forget() = Lexicons.set("th", null)

    private fun words(text: String) = thai.parseTokens(text).filter { it.isWord }.map { it.token }

    /** The longest word that ends where a syllable does: ต่อม "gland" and มีน "March" end inside the next one. */
    @Test
    fun aWordNeverEndsInsideASyllable() {
        Lexicons.set("th", Lexicon(listOf("ต่อ", "ต่อม", "มา", "เขา", "กลับ", "บ้าน", "นี้", "มี", "มีน", "น้ำ", "คน", "คนที", "ที่", "นักเรียน")))
        assertEquals(listOf("ต่อ", "มา", "เขา", "กลับ", "บ้าน"), words("ต่อมาเขากลับบ้าน"))
        assertEquals(listOf("บ้าน", "นี้", "มี", "น้ำ"), words("บ้านนี้มีน้ำ"))
        assertEquals(listOf("คน", "ที่", "มา"), words("คนที่มา"))
        assertEquals(listOf("มี", "นักเรียน"), words("มีนักเรียน"))
        // Where the longest word ends a syllable, it is still the one taken.
        assertEquals(listOf("ต่อม", "มี"), words("ต่อมมี"))
    }

    /** By how common they are, two common words are not read as a rarer one and a piece: มี + มติ, not มีม + ติ. */
    @Test
    fun byFrequencyTheLikeliestWordsWinBetweenSyllables() {
        val ranks = mapOf("มี" to 5, "ว่า" to 6, "มติ" to 300, "มีม" to 641, "ติ" to 389, "ว่าง" to 900, "งบประมาณ" to 700, "ต่อม" to 2000, "มา" to 30, "ไป" to 20)
        Lexicons.set("th", Lexicon(ranks.keys + "ต่อ" + "บ" + "ประมาณ", ranks))
        assertEquals(listOf("มี", "มติ"), words("มีมติ"))
        assertEquals(listOf("ว่า", "งบประมาณ"), words("ว่างบประมาณ"))
        // Never inside a syllable, however rare the words around it: ต่อ, a word of the dictionary alone, and มา.
        assertEquals(listOf("ต่อ", "มา"), words("ต่อมา"))
        // Letters no word covers go a syllable at a time, and are one word together.
        assertEquals(listOf("สมศรี", "ไป"), words("สมศรีไป"))
        // A vowel written first and its silent ห go with the letter the ห leads: ไหร่ is one syllable, no ไห "jar" + ร่.
        Lexicons.set("th", Lexicon(listOf("เท่า", "ไห", "ร"), mapOf("เท่า" to 400, "ไห" to 8000)))
        assertEquals(listOf("เท่า", "ไหร่"), words("เท่าไหร่"))
    }

    /** Letters no word covers are passed over a syllable at a time, so no word starts after a vowel written first. */
    @Test
    fun lettersNoWordCoversGoBySyllables() {
        Lexicons.set("th", Lexicon(listOf("ก", "ไป")))
        assertEquals(listOf("เก", "ไป"), words("เกไป"))
        assertEquals(listOf("สมศรี", "ไป"), words("สมศรีไป"))
    }

    @Test
    fun theRepeatMarkAndThaiDigitsAreNoWords() {
        Lexicons.set("th", Lexicon(listOf("เด็ก", "เล่น", "คน")))
        val tokens = thai.parseTokens("เด็กๆ เล่น ๕ คน")
        assertEquals(listOf("เด็ก" to true, "ๆ " to false, "เล่น" to true, " ๕ " to false, "คน" to true), tokens.map { it.token to it.isWord })
    }

    @Test
    fun withoutALexiconEachStretchIsOneWord() {
        assertEquals(listOf("ต่อมาเขา", "เล่น"), words("ต่อมาเขาๆเล่น"))
    }
}
