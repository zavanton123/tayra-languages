package com.idiomiko.core.domain.language

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LanguageCodesTest {
    @Test
    fun optionsListEveryCodeOnce() {
        val codes = LanguageCodes.options.map { it.code }
        assertEquals(codes.distinct(), codes)
        assertEquals(codes.sorted(), LanguageCodes.options.sortedBy { it.name }.map { it.code }.sorted())
        assertEquals(LanguageOption("zh", "Chinese"), LanguageCodes.option("zh"))
        assertEquals("English", LanguageCodes.option("EN ")?.name)
        assertNull(LanguageCodes.option("xx"))
    }

    @Test
    fun everyOptionHasTatoebaCode() {
        LanguageCodes.options.forEach { assertNotNull(LanguageCodes.tatoebaCode(it.code), it.code) }
    }

    @Test
    fun malayIsStandardMalay() {
        assertEquals("ms", LanguageCodes.codeFor("Malay"))
        assertEquals("zsm", LanguageCodes.tatoebaCode("ms"))
        assertNotNull(Tutorials.forLanguage("Malay"))
        assertEquals("spacedel", LanguageDefinitions.all.first { it.name == "Malay" }.language.parserType)
    }

    @Test
    fun urduIsALanguageWithItsTutorial() {
        assertEquals("ur", LanguageCodes.codeFor("Urdu"))
        assertEquals("urd", LanguageCodes.tatoebaCode("ur"))
        assertNotNull(Tutorials.forLanguage("Urdu"))
        assertEquals("urdu", LanguageDefinitions.all.first { it.name == "Urdu" }.language.parserType)
    }

    @Test
    fun pashtoIsALanguageWithItsTutorial() {
        assertEquals("ps", LanguageCodes.codeFor("Pashto"))
        assertEquals("pus", LanguageCodes.tatoebaCode("ps"))
        assertNotNull(Tutorials.forLanguage("Pashto"))
        assertEquals("pashto", LanguageDefinitions.all.first { it.name == "Pashto" }.language.parserType)
    }

    @Test
    fun tamilIsKnown() {
        assertEquals("ta", LanguageCodes.codeFor("Tamil"))
        assertEquals("tam", LanguageCodes.tatoebaCode("ta"))
        assertNotNull(Tutorials.forLanguage("Tamil"))
        assertEquals("spacedel", LanguageDefinitions.all.first { it.name == "Tamil" }.language.parserType)
    }

    @Test
    fun teluguIsLearnedInItsOwnScript() {
        assertEquals("te", LanguageCodes.codeFor("Telugu"))
        assertEquals("tel", LanguageCodes.tatoebaCode("te"))
        assertNotNull(Tutorials.forLanguage("Telugu"))
        assertEquals(setOf(Script.TELUGU), Scripts.of("te"))
        assertEquals(Script.TELUGU, Scripts.dominant("నేను తెలుగు నేర్చుకుంటున్నాను."))
        assertEquals(Script.KANNADA, Scripts.dominant("ನಾನು ಕನ್ನಡ ಕಲಿಯುತ್ತಿದ್ದೇನೆ."))
    }

    @Test
    fun nepaliIsALanguageWithItsTutorial() {
        assertEquals("ne", LanguageCodes.codeFor("Nepali"))
        assertEquals("nep", LanguageCodes.tatoebaCode("ne"))
        assertNotNull(Tutorials.forLanguage("Nepali"))
        assertEquals("spacedel", LanguageDefinitions.all.first { it.name == "Nepali" }.language.parserType)
        assertEquals(setOf(Script.DEVANAGARI), Scripts.of("ne"))
    }

    @Test
    fun gujaratiIsALanguageWithItsTutorial() {
        assertEquals("gu", LanguageCodes.codeFor("Gujarati"))
        assertEquals("guj", LanguageCodes.tatoebaCode("gu"))
        assertNotNull(Tutorials.forLanguage("Gujarati"))
        assertEquals("spacedel", LanguageDefinitions.all.first { it.name == "Gujarati" }.language.parserType)
    }

    @Test
    fun sinhalaIsALanguageWithItsTutorial() {
        assertEquals("si", LanguageCodes.codeFor("Sinhala"))
        assertEquals("sin", LanguageCodes.tatoebaCode("si"))
        assertEquals("Sinhala", LanguageCodes.option("si")?.name)
        assertNotNull(Tutorials.forLanguage("Sinhala"))
        assertEquals("spacedel", LanguageDefinitions.all.first { it.name == "Sinhala" }.language.parserType)
    }

    @Test
    fun hausaIsALanguageWithItsTutorial() {
        assertEquals("ha", LanguageCodes.codeFor("Hausa"))
        assertEquals("hau", LanguageCodes.tatoebaCode("ha"))
        assertNotNull(Tutorials.forLanguage("Hausa"))
        assertEquals("hausa", LanguageDefinitions.all.first { it.name == "Hausa" }.language.parserType)
    }

    @Test
    fun marathiIsALanguageInDevanagariWithItsTutorial() {
        assertEquals("mr", LanguageCodes.codeFor("Marathi"))
        assertEquals("mar", LanguageCodes.tatoebaCode("mr"))
        assertNotNull(Tutorials.forLanguage("Marathi"))
        assertEquals("spacedel", LanguageDefinitions.all.first { it.name == "Marathi" }.language.parserType)
        assertEquals(setOf(Script.DEVANAGARI), Scripts.of("mr"))
    }

    @Test
    fun zuluIsALanguageWithItsTutorial() {
        assertEquals("zu", LanguageCodes.codeFor("Zulu"))
        assertEquals("zul", LanguageCodes.tatoebaCode("zu"))
        assertNotNull(Tutorials.forLanguage("Zulu"))
        assertEquals("spacedel", LanguageDefinitions.all.first { it.name == "Zulu" }.language.parserType)
    }

    @Test
    fun somaliIsALanguageWithItsTutorial() {
        assertEquals("so", LanguageCodes.codeFor("Somali"))
        assertEquals("som", LanguageCodes.tatoebaCode("so"))
        assertNotNull(Tutorials.forLanguage("Somali"))
        assertEquals("spacedel", LanguageDefinitions.all.first { it.name == "Somali" }.language.parserType)
    }

    @Test
    fun uzbekIsALanguageWithItsTutorial() {
        assertEquals("uz", LanguageCodes.codeFor("Uzbek"))
        assertEquals("uzb", LanguageCodes.tatoebaCode("uz"))
        assertNotNull(Tutorials.forLanguage("Uzbek"))
        assertEquals("uzbek", LanguageDefinitions.all.first { it.name == "Uzbek" }.language.parserType)
    }

    @Test
    fun yorubaIsALanguageWithItsTutorial() {
        assertEquals("yo", LanguageCodes.codeFor("Yoruba"))
        assertEquals("yor", LanguageCodes.tatoebaCode("yo"))
        assertNotNull(Tutorials.forLanguage("Yoruba"))
        assertEquals("yoruba", LanguageDefinitions.all.first { it.name == "Yoruba" }.language.parserType)
    }

    @Test
    fun burmeseIsALanguageWithItsTutorial() {
        assertEquals("my", LanguageCodes.codeFor("Burmese"))
        assertEquals("mya", LanguageCodes.tatoebaCode("my"))
        assertNotNull(Tutorials.forLanguage("Burmese"))
        assertEquals("burmese", LanguageDefinitions.all.first { it.name == "Burmese" }.language.parserType)
    }

    @Test
    fun amharicIsALanguageWithItsTutorial() {
        assertEquals("am", LanguageCodes.codeFor("Amharic"))
        assertEquals("amh", LanguageCodes.tatoebaCode("am"))
        assertNotNull(Tutorials.forLanguage("Amharic"))
        assertEquals("spacedel", LanguageDefinitions.all.first { it.name == "Amharic" }.language.parserType)
    }
}
