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

    /** The negator, ก็ and ตอน join no word after them, ว่า, อยู่ and the pronouns none before them, however common the pair. */
    @Test
    fun grammaticalWordsStandApart() {
        val ranks = mapOf(
            "ไม่" to 25, "ดี" to 143, "ไม่ดี" to 3329, "มี" to 5, "ใคร" to 300, "ไม่มีใคร" to 2146, "ก็" to 68, "ก็ดี" to 1604,
            "ตอน" to 287, "เย็น" to 968, "ตอนเย็น" to 4520, "คิด" to 366, "ว่า" to 6, "คิดว่า" to 975, "ของ" to 9, "เรา" to 192,
            "ของเรา" to 2033, "อาศัย" to 1083, "อยู่" to 40, "อาศัยอยู่" to 1714, "สนาม" to 513, "บิน" to 982, "สนามบิน" to 1717,
            "ต้อง" to 67, "การ" to 18, "ต้องการ" to 891, "มาก" to 139, "กว่า" to 150, "มากกว่า" to 400,
        )
        Lexicons.set("th", Lexicon(ranks.keys + "ก", ranks))
        assertEquals(listOf("ไม่", "ดี", "ก็", "ดี"), words("ไม่ดีก็ดี"))
        assertEquals(listOf("ไม่", "มี", "ใคร"), words("ไม่มีใคร"))
        assertEquals(listOf("ตอน", "เย็น", "เรา", "คิด", "ว่า"), words("ตอนเย็นเราคิดว่า"))
        assertEquals(listOf("ของ", "เรา", "อาศัย", "อยู่"), words("ของเราอาศัยอยู่"))
        // Words that are none of them stay one word as the lexicon has them, and a letter alone is no word to cut into:
        // มากกว่า is not มากก + ว่า, as มากก is มาก and a letter.
        assertEquals(listOf("สนามบิน", "ต้องการ", "มากกว่า"), words("สนามบินต้องการมากกว่า"))
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
