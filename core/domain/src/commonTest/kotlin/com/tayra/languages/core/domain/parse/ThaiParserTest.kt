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
