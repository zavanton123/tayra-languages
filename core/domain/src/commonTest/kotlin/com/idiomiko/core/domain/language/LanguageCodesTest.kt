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
    fun tamilIsKnown() {
        assertEquals("ta", LanguageCodes.codeFor("Tamil"))
        assertEquals("tam", LanguageCodes.tatoebaCode("ta"))
        assertNotNull(Tutorials.forLanguage("Tamil"))
        assertEquals("spacedel", LanguageDefinitions.all.first { it.name == "Tamil" }.language.parserType)
    }
}
