package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.language.LanguageDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals

class PersianParserTest {
    private val language = LanguageDefinitions.all.first { it.name == "Farsi" }.language

    private fun words(text: String) = language.parseTokens(text).filter { it.isWord }.map { it.token }

    @Test
    fun theNonJoinerStaysInsideAWord() {
        assertEquals(listOf("من", "می‌روم"), words("من می‌روم."))
        assertEquals(listOf("کتاب‌ها", "را", "خانه‌اش", "دید"), words("کتاب‌ها را خانه‌اش دید."))
        assertEquals(listOf("نمی‌شود"), words("نمی‌شود؟"))
    }

    @Test
    fun persianDigitsAreNoWord() {
        assertEquals(listOf("سینا", "سال", "دارد"), words("سینا ۱۰ سال دارد."))
    }

    @Test
    fun endingsStayOnTheirWord() {
        assertEquals(listOf("کتابش", "دوستم", "کتابتان"), words("کتابش، دوستم، کتابتان"))
    }

    @Test
    fun wordsAreMatchedInPersianLetters() {
        assertEquals("کتاب", language.lowercase("كتاب"))
        assertEquals("ایران", language.lowercase("ايران"))
        assertEquals("موسی", language.lowercase("موسى"))
        assertEquals("میکند", language.lowercase("مي‌كند"))
    }

    @Test
    fun aWordIsMatchedWithOrWithoutItsNonJoiner() {
        assertEquals(language.lowercase("میروم"), language.lowercase("می‌روم"))
        assertEquals("کتابها", language.lowercase("کتاب‌ها"))
    }

    @Test
    fun wordsAreMatchedWithoutMarks() {
        assertEquals("واقعا", language.lowercase("واقعاً"))
        assertEquals("خانه", language.lowercase("خانهٔ"))
        assertEquals("خانه", language.lowercase("خانۀ"))
        assertEquals("پدر", language.lowercase("پدرِ"))
        assertEquals("پدر", language.lowercase("پَدَر"))
    }

    @Test
    fun aLetterWrittenWithACombiningSignStaysThatLetter() {
        assertEquals("آب", language.lowercase("آب"))
        assertEquals("مسئله", language.lowercase("مسیٔله"))
        assertEquals("مؤسسه", language.lowercase("مؤسسه"))
    }

    @Test
    fun aNonJoinerAtTheEdgeOfAWordIsNoPartOfTheMatch() {
        assertEquals("کتاب", language.lowercase("کتاب‌"))
    }

    @Test
    fun theFormAPackWritesHasNoNonJoiner() {
        assertEquals("میکند", PersianParser.packForm("مي‌كند"))
    }
}
