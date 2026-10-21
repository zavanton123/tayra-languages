package com.tayra.languages.core.domain.parse

import com.tayra.languages.core.domain.TestLanguages
import com.tayra.languages.core.domain.language.LanguageDefinitions
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberSentencesTest {

    private fun language(name: String) = LanguageDefinitions.all.first { it.name == name }.language

    private fun sentences(text: String, name: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        for (token in language(name).parseTokens(text)) {
            current.append(token.token)
            if (token.isEndOfSentence) {
                out += current.toString().trim()
                current.clear()
            }
        }
        if (current.isNotBlank()) out += current.toString().trim()
        return out
    }

    @Test
    fun portugueseAmountsStayInTheirSentence() {
        assertEquals(listOf("Em maio, o Pedro tinha 38.000 reais."), sentences("Em maio, o Pedro tinha 38.000 reais.", "Portuguese"))
        assertEquals(listOf("A casa custa 1.250.000 reais.", "É cara."), sentences("A casa custa 1.250.000 reais. É cara.", "Portuguese"))
        assertEquals(listOf("Ele nasceu em 2020.", "Depois mudou-se."), sentences("Ele nasceu em 2020. Depois mudou-se.", "Portuguese"))
        assertEquals(listOf("Ele chegou às 10.30 da manhã."), sentences("Ele chegou às 10.30 da manhã.", "Portuguese"))
    }

    @Test
    fun germanNumbersAndDatesStayInTheirSentence() {
        assertEquals(listOf("Das Auto kostet 1.400 Euro.", "Das ist teuer."), sentences("Das Auto kostet 1.400 Euro. Das ist teuer.", "German"))
        assertEquals(listOf("Am 3.10.2024 war Feiertag."), sentences("Am 3.10.2024 war Feiertag.", "German"))
        assertEquals(listOf("Das ist das Ende von Teil 3.", "Weiter."), sentences("Das ist das Ende von Teil 3. Weiter.", "German"))
    }

    @Test
    fun englishDecimalsStayInTheirSentence() {
        assertEquals(listOf("The rope is 2.5 metres long.", "It is enough."), sentences("The rope is 2.5 metres long. It is enough.", "English"))
        assertEquals(listOf("It costs 38,000 dollars."), sentences("It costs 38,000 dollars.", "English"))
        assertEquals(listOf("We left at 10:30.", "Then we ate."), sentences("We left at 10:30. Then we ate.", "English"))
    }

    @Test
    fun russianNumbersStayInTheirSentence() {
        assertEquals(listOf("У него было 38 000 рублей.", "Это много."), sentences("У него было 38 000 рублей. Это много.", "Russian"))
        assertEquals(listOf("Вес 2.5 кг.", "Конец."), sentences("Вес 2.5 кг. Конец.", "Russian"))
        assertEquals(listOf("Это было в 1999.", "Потом всё изменилось."), sentences("Это было в 1999. Потом всё изменилось.", "Russian"))
    }

    @Test
    fun segmentedLanguagesKeepDecimals() {
        assertEquals(listOf("りんごは3.5キロです。", "安い。"), sentences("りんごは3.5キロです。安い。", "Japanese"))
        assertEquals(listOf("사과가 3.5킬로예요.", "싸요."), sentences("사과가 3.5킬로예요. 싸요.", "Korean"))
    }

    @Test
    fun classicalChineseKeepsDecimals() {
        val tokens = ClassicalChineseParser().parse("三3.5。之", TestLanguages.classicalChinese)
        assertEquals(listOf("三", "3", ".", "5", "。", "之"), tokens.map { it.token })
        assertEquals(listOf(false, false, false, false, true, false), tokens.map { it.isEndOfSentence })
    }
}
