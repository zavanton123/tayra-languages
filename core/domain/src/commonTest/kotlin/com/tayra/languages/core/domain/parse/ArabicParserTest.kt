package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.language.LanguageDefinitions
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ArabicParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Arabic" }.language

    /** Ranks as the frequency list gives them: the letters in front first, then the words with their forms. */
    private val ranks = mapOf(
        "ال" to 1, "و" to 2, "في" to 3, "ب" to 5, "ل" to 7, "كان" to 9, "لل" to 15, "ف" to 21, "قد" to 28, "لكن" to 29,
        "بعد" to 25, "كل" to 19, "ك" to 36, "س" to 41, "قال" to 24, "قالت" to 24, "يوم" to 40, "سنة" to 65, "عالم" to 63,
        "هو" to 18, "به" to 5, "له" to 7, "كتب" to 147, "كتاب" to 164, "كتابه" to 164, "بيت" to 198, "ولد" to 195,
        "فيلم" to 144, "وصل" to 197, "يصل" to 197, "وقت" to 70, "سيد" to 211, "يد" to 289, "يكتب" to 147, "ليل" to 702,
        "مدرسة" to 208, "مدرستي" to 208, "على" to 6, "عليه" to 6, "جديد" to 64,
    )

    /** Words a dictionary pack adds, without a rank. */
    private val packWords = listOf("العالم", "الكتاب", "لبنان", "بنان", "لد", "صل", "ين", "عد", "كن", "لم", "يلم", "تب")

    @BeforeTest
    fun lexicon() = Lexicons.set("ar", Lexicon(ranks.keys + packWords, ranks))

    @AfterTest
    fun forget() = Lexicons.set("ar", null)

    private fun words(text: String) = language.parseTokens(text).filter { it.isWord }.map { it.token }

    @Test
    fun theLettersInFrontAreCutOff() {
        assertEquals(listOf("و", "ال", "كتاب"), words("والكتاب"))
        assertEquals(listOf("ب", "ال", "عالم"), words("بالعالم"))
        assertEquals(listOf("و", "كتب"), words("وكتب"))
        assertEquals(listOf("ف", "قالت"), words("فقالت"))
        assertEquals(listOf("ك", "ال", "بيت"), words("كالبيت"))
        assertEquals(listOf("و", "ب", "ال", "بيت"), words("وبالبيت"))
        assertEquals(listOf("ال", "عالم"), words("العالم"))
    }

    @Test
    fun theArticleWithLIsOnePiece() {
        assertEquals(listOf("لل", "كتاب"), words("للكتاب"))
        assertEquals(listOf("و", "لل", "بيت"), words("وللبيت"))
        // Before a word in ل the article's lam is written once: لليل is ل + ليل.
        assertEquals(listOf("ل", "ليل"), words("لليل"))
    }

    @Test
    fun theFutureIsCutOffAVerbsImperfectOnly() {
        assertEquals(listOf("س", "يكتب"), words("سيكتب"))
        assertEquals(listOf("و", "س", "يكتب"), words("وسيكتب"))
        assertEquals(listOf("سيد"), words("سيد"))
        assertEquals(listOf("سنة"), words("سنة"))
    }

    @Test
    fun aWordWithAPronounAfterItIsCutToo() {
        assertEquals(listOf("و", "كتابهم"), words("وكتابهم"))
        assertEquals(listOf("ب", "مدرستي"), words("بمدرستي"))
        assertEquals(listOf("كتابه"), words("كتابه"))
        assertEquals(listOf("عليه"), words("عليه"))
    }

    @Test
    fun wordsOfTheListStayWhole() {
        for (word in listOf("ولد", "بيت", "كان", "فيلم", "لكن", "بعد", "كل", "وصل", "وقت", "به", "له", "قد")) {
            assertEquals(listOf(word), words(word), word)
        }
    }

    @Test
    fun aWordOnlyADictionaryHasStaysWholeUnlessACutLeavesAWordOfTheList() {
        assertEquals(listOf("لبنان"), words("لبنان"))
        assertEquals(listOf("ال", "كتاب"), words("الكتاب"))
        // No word is left by ل in لم, nor by و in a word the lexicon lacks.
        assertEquals(listOf("لم"), words("لم"))
        assertEquals(listOf("وزارة"), words("وزارة"))
    }

    @Test
    fun vowelSignsStayOnTheirLettersAndAreNotMatched() {
        assertEquals(listOf("وَ", "الْ", "كِتَابُ"), words("وَالْكِتَابُ"))
        assertEquals(listOf("لِلْ", "بَيْتِ"), words("لِلْبَيْتِ"))
        assertEquals("كتاب", language.lowercase("كِتَابٌ"))
        assertEquals("أيضا", language.lowercase("أيضًا"))
        assertEquals("كتاب", language.lowercase("كتـــاب"))
    }

    @Test
    fun withoutALexiconWordsStayWhole() {
        Lexicons.set("ar", null)
        assertEquals(listOf("والكتاب"), words("والكتاب"))
    }

    @Test
    fun aSentenceIsCutWordByWord() {
        assertEquals(listOf("و", "قال", "ال", "ولد", "لل", "كتاب", "ال", "جديد"), words("وقال الولد للكتاب الجديد."))
    }
}
