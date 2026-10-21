package com.tayra.languages.core.domain.service

import kotlin.test.Test
import kotlin.test.assertEquals

class SpokenNumbersTest {

    private fun spoken(text: String, language: String?) = SpokenNumbers.normalize(text, language)

    @Test
    fun pointGroupedThousandsLoseTheirPoints() {
        assertEquals("Em maio, o Pedro tinha 38000 reais.", spoken("Em maio, o Pedro tinha 38.000 reais.", "pt"))
        assertEquals("Custa 1400 reais.", spoken("Custa 1.400 reais.", "pt"))
        assertEquals("A casa custa 1250000 reais.", spoken("A casa custa 1.250.000 reais.", "pt-BR"))
        assertEquals("Das Auto kostet 1400 Euro.", spoken("Das Auto kostet 1.400 Euro.", "de"))
        assertEquals("Tiene 38000 habitantes.", spoken("Tiene 38.000 habitantes.", "es"))
        assertEquals("Ha 2500 euro.", spoken("Ha 2.500 euro.", "it"))
        assertEquals("Det koster 12000 kroner.", spoken("Det koster 12.000 kroner.", "da"))
    }

    @Test
    fun anAmountAtTheEndOfASentenceKeepsTheFullStop() {
        assertEquals("O total foi 38000.", spoken("O total foi 38.000.", "pt"))
        assertEquals("Итого 38000.", spoken("Итого 38 000.", "ru"))
        assertEquals("The total was 38000.", spoken("The total was 38,000.", "en"))
    }

    @Test
    fun decimalCommasYearsDatesAndTimesStay() {
        assertEquals("Tem 2,5 metros.", spoken("Tem 2,5 metros.", "pt"))
        assertEquals("Custa 1250,50 reais.", spoken("Custa 1.250,50 reais.", "pt"))
        assertEquals("Ele nasceu em 2020. Depois mudou-se.", spoken("Ele nasceu em 2020. Depois mudou-se.", "pt"))
        assertEquals("Am 3.10.2024 war Feiertag.", spoken("Am 3.10.2024 war Feiertag.", "de"))
        assertEquals("Chegou às 10.30 e saiu às 11:45.", spoken("Chegou às 10.30 e saiu às 11:45.", "pt"))
        assertEquals("Ligue para 3.456-7890.", spoken("Ligue para 3.456-7890.", "pt"))
        assertEquals("Em 1999 100 pessoas vieram.", spoken("Em 1999 100 pessoas vieram.", "pt"))
    }

    @Test
    fun englishDropsCommasAndSaysThePoint() {
        assertEquals("The rope is 2 point 5 metres long.", spoken("The rope is 2.5 metres long.", "en"))
        assertEquals("Pi is about 3 point 1 4.", spoken("Pi is about 3.14.", "en"))
        assertEquals("It cost 1250000 dollars.", spoken("It cost 1,250,000 dollars.", "en"))
        assertEquals("It cost 38000 point 5 0 dollars.", spoken("It cost 38,000.50 dollars.", "en"))
        assertEquals("It weighs 1 point 4 0 0 kilos.", spoken("It weighs 1.400 kilos.", "en"))
        assertEquals("In 2020. Then 3.10.2024 and 10:30.", spoken("In 2020. Then 3.10.2024 and 10:30.", "en"))
        assertEquals("Call 555-1234 or 1,2 or 3.", spoken("Call 555-1234 or 1,2 or 3.", "en"))
    }

    @Test
    fun spaceGroupedThousandsLoseTheirSpaces() {
        assertEquals("У него было 38000 рублей.", spoken("У него было 38 000 рублей.", "ru"))
        assertEquals("Это стоит 1250000 рублей.", spoken("Это стоит 1 250 000 рублей.", "ru"))
        assertEquals("Il a 38000 euros.", spoken("Il a 38 000 euros.", "fr"))
        assertEquals("Mám 2500 korun.", spoken("Mám 2 500 korun.", "cs"))
        assertEquals("Вес 2,5 кг.", spoken("Вес 2,5 кг.", "ru"))
        assertEquals("В 1999 году 100 человек.", spoken("В 1999 году 100 человек.", "ru"))
        assertEquals("Звоните +7 912 345 67 89.", spoken("Звоните +7 912 345 67 89.", "ru"))
    }

    @Test
    fun otherLanguagesAndNoLanguageAreLeftAlone() {
        assertEquals("38.000 reais", spoken("38.000 reais", null))
        assertEquals("Il a 38.000 euros.", spoken("Il a 38.000 euros.", "fr"))
        assertEquals("Er hat 38 000 Euro.", spoken("Er hat 38 000 Euro.", "de"))
        assertEquals("3,000円です。", spoken("3,000円です。", "ar"))
        assertEquals("3000円です。", spoken("3,000円です。", "ja"))
    }
}
