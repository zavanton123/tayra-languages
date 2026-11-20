package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.language.LanguageDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PashtoParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Pashto" }.language

    private fun words(text: String) = language.parseTokens(text).filter { it.isWord }.map { it.token }

    @Test
    fun pashtoIsReadRightToLeftByThePashtoParser() {
        assertTrue(language.rightToLeft)
        assertEquals("pashto", language.parserType)
    }

    @Test
    fun thePashtoLettersAreWordLetters() {
        assertEquals(listOf("ښځې", "او", "نجونې", "ډوډۍ", "پخوي", "ځوان", "ټوپ", "څېړي"), words("ښځې او نجونې ډوډۍ پخوي، ځوان ټوپ څېړي."))
        assertEquals(listOf("ګڼ", "ږغ", "ړوند", "مڼه", "شئ"), words("ګڼ ږغ؟ ړوند مڼه شئ!"))
    }

    @Test
    fun theFullStopsAndQuestionMarkEndASentence() {
        val tokens = language.parseTokens("زه راغلم. هغه لاړ؟ موږ پاتې شو۔ بس")
        assertEquals(listOf(0, 0, 1, 1, 2, 2, 2, 3), tokens.filter { it.isWord }.map { it.sentenceNumber })
    }

    @Test
    fun digitsAndCommasAreNoWord() {
        assertEquals(listOf("احمد", "کلن", "دی", "او", "خوښ", "دی"), words("احمد ۱۰ کلن دی، او 12 خوښ دی."))
    }

    @Test
    fun theYesOfAfghanAndPakistaniSpellingAreOneLetter() {
        assertEquals(language.lowercase("چې"), language.lowercase("چي"))
        assertEquals(language.lowercase("چې"), language.lowercase("چی"))
        assertEquals(language.lowercase("ډېر"), language.lowercase("ډير"))
        assertEquals(language.lowercase("کېږي"), language.lowercase("کیږی"))
        assertEquals(language.lowercase("سړی"), language.lowercase("سړي"))
        assertEquals(language.lowercase("نجلۍ"), language.lowercase("نجلئ"))
        assertEquals(language.lowercase("وې"), language.lowercase("وے"))
        assertEquals("چي", language.lowercase("چې"))
    }

    @Test
    fun arabicPersianAndUrduLettersAreMatchedAsPashtoOnes() {
        assertEquals("کتاب", language.lowercase("كتاب"))
        assertEquals(language.lowercase("ګوره"), language.lowercase("گوره"))
        assertEquals(language.lowercase("زړه"), language.lowercase("زړۀ"))
        assertEquals(language.lowercase("ټوپ"), language.lowercase("ٹوپ"))
    }

    @Test
    fun pashtosOwnLettersStayApart() {
        assertTrue(language.lowercase("ښار") != language.lowercase("شار"))
        assertTrue(language.lowercase("ږغ") != language.lowercase("ژغ"))
        assertTrue(language.lowercase("ځای") != language.lowercase("زای"))
        assertTrue(language.lowercase("ړوند") != language.lowercase("روند"))
        assertTrue(language.lowercase("ڼ") != language.lowercase("ن"))
    }

    @Test
    fun wordsAreMatchedWithoutMarksOrTheNonJoiner() {
        assertEquals(language.lowercase("لاس"), language.lowercase("لَاس"))
        assertEquals(language.lowercase("فورا"), language.lowercase("فوراً"))
        assertEquals(language.lowercase("نړيوال"), language.lowercase("نړي‌وال"))
        assertEquals("آب", language.lowercase("آب"))
    }
}
