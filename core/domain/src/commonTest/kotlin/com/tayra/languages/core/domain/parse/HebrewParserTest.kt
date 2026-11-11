package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.language.LanguageDefinitions
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HebrewParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Hebrew" }.language

    /** Ranks as the frequency list gives them: the letters in front first, then the words with their forms. */
    private val ranks = mapOf(
        "ה" to 1, "ב" to 2, "בהם" to 2, "ל" to 3, "לכם" to 3, "להם" to 3, "ו" to 4, "ש" to 5, "של" to 6, "שלי" to 6, "שלו" to 6,
        "שלה" to 6, "לא" to 8, "מ" to 10, "זה" to 11, "היה" to 12, "להיות" to 12, "כל" to 13, "הוא" to 14, "אני" to 15,
        "יש" to 19, "היא" to 21, "בית" to 23, "ביתו" to 23, "ביתה" to 23, "שם" to 24, "שנה" to 25, "כ" to 26, "הם" to 32,
        "עשה" to 40, "לעשות" to 40, "דרך" to 52, "דרכי" to 52, "יום" to 61, "גדול" to 63, "הגיע" to 65, "זמן" to 66,
        "ספר" to 68, "ספרים" to 68, "לפני" to 73, "אדם" to 75, "כבר" to 78, "מקום" to 79, "ילד" to 80, "ילדים" to 80,
        "הלך" to 87, "חדש" to 90, "ים" to 97, "עיר" to 100, "הרבה" to 110, "מספר" to 113, "היום" to 115, "כש" to 116,
        "משפחה" to 135, "משהו" to 136, "הבין" to 146, "שמע" to 161, "הפך" to 175, "שלום" to 187, "למד" to 215, "כסף" to 222,
        "מים" to 225, "הרגיש" to 226, "שאל" to 246, "מלך" to 257, "שלח" to 284, "בדק" to 448, "לבד" to 529, "ועדה" to 654,
        "כלב" to 745, "מכתב" to 752, "הביתה" to 845, "ברכה" to 879, "שולחן" to 943, "לחם" to 1209, "בכה" to 1317,
        "ג׳ינס" to 5141, "אמינות" to 300, "האמין" to 347, "מאמינות" to 347, "בא" to 400, "באתי" to 400, "הביא" to 640,
        "הבאתי" to 640,
    )

    /** The headwords among them, with their own ranks: ביתו and ביתה are בית's forms. */
    private val heads = listOf("בית", "דרך", "יום", "ילד", "שנה", "זמן", "של").associateWith { ranks.getValue(it) }

    /** Words a dictionary pack adds, without a rank. */
    private val packWords = listOf("ייבא", "לייבא", "מע", "שמעו", "צה״ל")

    @BeforeTest
    fun lexicon() = Lexicons.set("he", Lexicon(ranks.keys + packWords, ranks, heads))

    @AfterTest
    fun forget() = Lexicons.set("he", null)

    private fun words(text: String) = language.parseTokens(text).filter { it.isWord }.map { it.token }

    @Test
    fun theLettersInFrontAreCutOff() {
        assertEquals(listOf("ו", "ב", "בית"), words("ובבית"))
        assertEquals(listOf("מ", "ה", "בית"), words("מהבית"))
        assertEquals(listOf("ש", "ה", "ילד"), words("שהילד"))
        assertEquals(listOf("ו", "כש", "הוא"), words("וכשהוא"))
        assertEquals(listOf("ש", "ב", "בית"), words("שבבית"))
        assertEquals(listOf("כש", "היה"), words("כשהיה"))
    }

    @Test
    fun theArticleIsNotWrittenAfterBKAndL() {
        // בבית is "in the house", לילד "to the boy": nothing of the article is left to cut.
        assertEquals(listOf("ב", "בית"), words("בבית"))
        assertEquals(listOf("ל", "ילד"), words("לילד"))
        assertEquals(listOf("כ", "ילד"), words("כילד"))
    }

    @Test
    fun wordsTheListWronglyKeptWholeAreCut() {
        val cuts = mapOf(
            "הזמן" to listOf("ה", "זמן"), "הדרך" to listOf("ה", "דרך"), "הכל" to listOf("ה", "כל"), "האדם" to listOf("ה", "אדם"),
            "המלך" to listOf("ה", "מלך"), "הגדול" to listOf("ה", "גדול"), "החדש" to listOf("ה", "חדש"), "בשם" to listOf("ב", "שם"),
            "ביום" to listOf("ב", "יום"), "בים" to listOf("ב", "ים"), "בשנה" to listOf("ב", "שנה"), "לבית" to listOf("ל", "בית"),
            "מבית" to listOf("מ", "בית"), "שהם" to listOf("ש", "הם"), "שהיה" to listOf("ש", "היה"), "שיש" to listOf("ש", "יש"),
        )
        for ((word, pieces) in cuts) assertEquals(pieces, words(word), word)
    }

    @Test
    fun wordsWhoseFirstLetterIsTheirOwnStayWhole() {
        val whole = listOf(
            "הלך", "הגיע", "הבין", "הרגיש", "הפך", "הרבה", "ביתה", "בית", "בדק", "בכה", "ברכה", "לקח", "למד", "לחם", "לבד",
            "לפני", "מים", "מלך", "מקום", "משפחה", "מספר", "מכתב", "משהו", "שלום", "שלח", "שמע", "שאל", "שולחן", "כלב", "כסף",
            "כבר", "שלי", "שלו", "שלה", "לכם", "להם", "בהם", "היום", "הביתה", "להיות", "לעשות",
        )
        for (word in whole) assertEquals(listOf(word), words(word), word)
    }

    @Test
    fun aFormOfTheListStaysWholeThoughACutLeavesACommonerWord() {
        // מאמינות "they believe" is האמין's, no מ + אמינות "credibility"; הבאתי "I brought" הביא's, no ה + באתי "I came".
        assertEquals(listOf("מאמינות"), words("מאמינות"))
        assertEquals(listOf("הבאתי"), words("הבאתי"))
        assertEquals(listOf("ב", "אמינות"), words("באמינות"))
        assertEquals(listOf("ש", "באתי"), words("שבאתי"))
    }

    @Test
    fun aWordOnlyADictionaryHasStaysWholeUnlessACutLeavesAWordOfTheList() {
        // לייבא "to import" is an infinitive the pack has: no ל + ייבא, whose ייבא only the pack has. שמעו "they heard" is no
        // ש + מעו either.
        assertEquals(listOf("לייבא"), words("לייבא"))
        assertEquals(listOf("שמעו"), words("שמעו"))
    }

    @Test
    fun aPronounAfterAWordStaysOnIt() {
        assertEquals(listOf("ל", "ביתו"), words("לביתו"))
        assertEquals(listOf("ב", "דרכי"), words("בדרכי"))
        // A headword's form with a pronoun the list lacks is the headword's: ביתנו "our house", דרכו "his way".
        assertEquals(listOf("ביתנו"), words("ביתנו"))
        assertEquals(listOf("ו", "דרכו"), words("ודרכו"))
    }

    @Test
    fun afterTheConjunctionAWordOfTheListIsCutNoFurther() {
        assertEquals(listOf("ו", "לפני"), words("ולפני"))
        assertEquals(listOf("ו", "שלום"), words("ושלום"))
    }

    @Test
    fun aWordInVavDoublesItAfterTheLettersInFront() {
        assertEquals(listOf("ה", "וועדה"), words("הוועדה"))
        assertEquals(listOf("ב", "וועדה"), words("בוועדה"))
    }

    @Test
    fun theGereshAndGershayimAreLettersOfTheWord() {
        assertEquals(listOf("ג׳ינס"), words("ג'ינס"))
        assertEquals(listOf("ג׳ינס"), words("ג’ינס"))
        assertEquals(listOf("ה", "ג׳ינס"), words("הג׳ינס"))
        assertEquals(listOf("צה״ל"), words("צה\"ל"))
        assertEquals(listOf("ב", "צה״ל"), words("בצה״ל"))
        assertEquals("ג׳ינס", language.lowercase("ג'ינס"))
        // Quotation marks around a word stay outside it.
        assertEquals(listOf("שלום"), words("\"שלום\""))
    }

    @Test
    fun theMaqafJoinsTwoWords() {
        assertEquals(listOf("בית", "ספר"), words("בית־ספר"))
        assertEquals(listOf("ב"), words("ב־2020"))
    }

    @Test
    fun pointsStayOnTheirLettersAndAreNotMatched() {
        assertEquals(listOf("בְּ", "בַיִת"), words("בְּבַיִת"))
        assertEquals(listOf("וְ", "הַ", "יֶּלֶד"), words("וְהַיֶּלֶד"))
        assertEquals("בית", language.lowercase("בַּיִת"))
        // Cantillation marks are no letters either: they do not part a word.
        assertEquals(listOf("בְּ֭", "בַיִת"), words("בְּ֭בַיִת"))
    }

    @Test
    fun withoutALexiconWordsStayWhole() {
        Lexicons.set("he", null)
        assertEquals(listOf("ובבית"), words("ובבית"))
    }

    @Test
    fun aSentenceIsCutWordByWord() {
        assertEquals(listOf("ו", "ה", "ילד", "הלך", "ל", "בית", "של", "ה", "מלך", "ב", "יום", "ה", "גדול"), words("והילד הלך לבית של המלך ביום הגדול."))
    }
}
