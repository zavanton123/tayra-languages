package com.idiomiko.core.domain.parse

import com.idiomiko.core.domain.language.LanguageDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UrduParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Urdu" }.language

    private fun words(text: String) = language.parseTokens(text).filter { it.isWord }.map { it.token }

    @Test
    fun urduIsReadRightToLeftByTheUrduParser() {
        assertTrue(language.rightToLeft)
        assertEquals("urdu", language.parserType)
    }

    @Test
    fun theUrduLettersAreWordLetters() {
        assertEquals(listOf("بچے", "گھر", "میں", "ٹماٹر", "اور", "ڈبل", "روٹی", "کھا", "رہے", "ہیں"), words("بچے گھر میں ٹماٹر اور ڈبل روٹی کھا رہے ہیں۔"))
        assertEquals(listOf("وہ", "پہاڑ", "پر", "چڑھا", "نہیں"), words("وہ پہاڑ پر چڑھا؟ نہیں!"))
    }

    @Test
    fun theUrduFullStopEndsASentence() {
        val tokens = language.parseTokens("میں آیا۔ وہ گیا۔")
        assertEquals(listOf(0, 0, 1, 1), tokens.filter { it.isWord }.map { it.sentenceNumber })
    }

    @Test
    fun digitsAndCommasAreNoWord() {
        assertEquals(listOf("علی", "سال", "کا", "ہے", "اور", "خوش", "ہے"), words("علی ۱۰ سال کا ہے، اور خوش ہے۔"))
    }

    @Test
    fun doChashmiHeAndGoalHeStayApart() {
        assertEquals("بھی", language.lowercase("بھی"))
        assertEquals("تھا", language.lowercase("تھا"))
        assertEquals("ہم", language.lowercase("ہم"))
        assertTrue(language.lowercase("بھی") != language.lowercase("بہی"))
    }

    @Test
    fun arabicLettersAreMatchedAsUrduLetters() {
        assertEquals("کتاب", language.lowercase("كتاب"))
        assertEquals("یہی", language.lowercase("يهي"))
        assertEquals("موسی", language.lowercase("موسى"))
    }

    @Test
    fun wordsAreMatchedWithoutMarksOrTheNonJoiner() {
        assertEquals("لیے", language.lowercase("لِیے"))
        assertEquals("فورا", language.lowercase("فوراً"))
        assertEquals("ذخیرہ", language.lowercase("\u0630\u062E\u06CC\u0631\u06C2"))
        assertEquals("ذخیرہ", language.lowercase("\u0630\u062E\u06CC\u0631\u06C1\u0654"))
        assertEquals("نشانزد", language.lowercase("\u0646\u0634\u0627\u0646\u200C\u0632\u062F"))
    }

    @Test
    fun hamzaLettersAreOneLetter() {
        assertEquals("\u0622\u067E", language.lowercase("\u0627\u0653\u067E"))
        assertEquals("\u062C\u0627\u0626\u06D2", language.lowercase("\u062C\u0627\u06D2\u0654"))
        assertEquals("\u062C\u0627\u0626\u06D2", language.lowercase("\u062C\u0627\u06D3"))
        assertEquals("\u0645\u0633\u0626\u0644\u06C1", language.lowercase("\u0645\u0633\u06CC\u0654\u0644\u06C1"))
    }
}
