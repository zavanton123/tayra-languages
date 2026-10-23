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
        "مدرسة" to 208, "مدرستي" to 208, "على" to 6, "عليه" to 6, "عليك" to 6, "جديد" to 64, "لنا" to 7, "أن" to 8,
        "لأن" to 83, "باب" to 430, "أب" to 106, "ابي" to 106, "لدى" to 91, "لدي" to 91, "رد" to 479, "برد" to 1942,
        "بن" to 82, "لبن" to 574, "بنك" to 827, "فعل" to 101, "فعلي" to 101, "فقد" to 637, "هم" to 123, "فهم" to 521,
        "لا" to 12, "قالوا" to 24, "درس" to 900, "ولدي" to 195, "ليس" to 60, "لست" to 60, "ليست" to 60, "ست" to 670,
        "حد" to 154, "وحد" to 720, "وحدها" to 720, "وفى" to 960, "وفيت" to 960, "فيت" to 2060, "أخذ" to 192, "آخذ" to 192,
        "لجنة" to 484, "جنة" to 1208, "لعب" to 266, "عب" to 3474, "صديق" to 298, "أصدقاء" to 298, "ماء" to 201,
        "كلف" to 2277, "كلفا" to 2277,
    )

    /** The headwords among them, with their own ranks: فعلي is فعل's form, not a headword. */
    private val heads = listOf("كل", "باب", "ولد", "فعل", "برد", "لبن", "كتاب", "بيت").associateWith { ranks.getValue(it) }

    /** Words a dictionary pack adds, without a rank. */
    private val packWords = listOf("العالم", "الكتاب", "لبنان", "بنان", "لد", "صل", "ين", "عد", "كن", "لم", "يلم", "تب", "لفا")

    @BeforeTest
    fun lexicon() = Lexicons.set("ar", Lexicon(ranks.keys + packWords, ranks, heads))

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
        // The first person's أ on a verb in أ is written آ after it: سآخذ "I will take".
        assertEquals(listOf("س", "آخذ"), words("سآخذ"))
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
    fun aHeadwordWithAPronounAfterItStaysWholeWhereThatIsLikelier() {
        // كل + نا "all of us", باب + ي "my door": not ك + لنا, ب + أبي. The list has ولدي "my child" under ولد, not و + لدى.
        assertEquals(listOf("كلنا"), words("كلنا"))
        assertEquals(listOf("بابي"), words("بابي"))
        assertEquals(listOf("ولدي"), words("ولدي"))
        // فعلي is a form of فعل, so فعليك is ف + عليك, not فعلي with a pronoun.
        assertEquals(listOf("ف", "عليك"), words("فعليك"))
    }

    @Test
    fun wordsTheListHasStayWholeAgainstACommonerCut() {
        assertEquals(listOf("برد"), words("برد"))
        assertEquals(listOf("لبن"), words("لبن"))
        assertEquals(listOf("و", "برد"), words("وبرد"))
    }

    @Test
    fun afterTheConjunctionAWordOfTheListIsCutNoFurther() {
        assertEquals(listOf("و", "لأن"), words("ولأن"))
        assertEquals(listOf("لأن"), words("لأن"))
    }

    @Test
    fun afterLlAWordOfTheListIsTheArticles() {
        assertEquals(listOf("لل", "بنك"), words("للبنك"))
    }

    @Test
    fun afterLAWordOfTheListInLIsCutByTheCosts() {
        // لجنة "committee" and لعب "to play" are words of the list: للجنة is ل + لجنة, not لل + جنة "paradise".
        assertEquals(listOf("ل", "لجنة"), words("للجنة"))
        assertEquals(listOf("ل", "لعب"), words("للعب"))
    }

    @Test
    fun aWordOfTheListThatBeginsAsTheLettersInFrontStaysWhole() {
        // لست "I am not" is ليس's, وحدها "she alone" وحد's, وفيت "I fulfilled" وفى's: no ل + ست, و + حدها, و + فيت.
        assertEquals(listOf("لست"), words("لست"))
        assertEquals(listOf("و", "لست"), words("ولست"))
        assertEquals(listOf("وحدها"), words("وحدها"))
        assertEquals(listOf("وفيت"), words("وفيت"))
    }

    @Test
    fun aHamzaOnASeatBeforeAPronounIsTheWordsHamza() {
        // أصدقاء "friends" writes its hamza on a seat before a pronoun: أصدقائه, أصدقاؤه; ماء "water" مائه.
        assertEquals(listOf("ب", "أصدقائه"), words("بأصدقائه"))
        assertEquals(listOf("و", "أصدقاؤه"), words("وأصدقاؤه"))
        assertEquals(listOf("ب", "مائه"), words("بمائه"))
    }

    @Test
    fun aFormOfTheListWithAPronounIsNoCutLeavingOnlyADictionarysWord() {
        // كلفا "he charged ... two" with نا "us": no ك + لفانا, whose لفا only a dictionary has.
        assertEquals(listOf("كلفانا"), words("كلفانا"))
        // After the conjunction it is the word the costs choose, as the list has its form.
        assertEquals(listOf("و", "كلفانا"), words("وكلفانا"))
    }

    @Test
    fun faqadAndFahumGoByTheWordAfterThem() {
        assertEquals(listOf("ف", "قد", "قالوا"), words("فقد قالوا"))
        assertEquals(listOf("فقد", "ال", "كتاب"), words("فقد الكتاب"))
        assertEquals(listOf("فقد"), words("فقد"))
        assertEquals(listOf("فقد"), words("فقد."))
        assertEquals(listOf("ف", "هم", "لا"), words("فهم لا"))
        assertEquals(listOf("ف", "هم", "يعرفون"), words("فهم يعرفون"))
        assertEquals(listOf("فهم", "ال", "درس"), words("فهم الدرس"))
        assertEquals(listOf("فهم"), words("فهم"))
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
