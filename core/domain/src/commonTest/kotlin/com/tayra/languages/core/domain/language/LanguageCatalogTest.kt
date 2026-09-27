package com.tayra.languages.core.domain.language

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LanguageCatalogTest {
    @Test
    fun targetLanguagesArePredefinedAndSupported() {
        LanguageCatalog.targetLanguages.forEach { name ->
            val definition = PredefinedLanguages.all.firstOrNull { it.name == name }
            assertNotNull(definition, name)
            assertTrue(definition.stories.isNotEmpty(), "$name has sample stories")
            assertNotNull(LanguageCodes.codeFor(name), "$name has a language code")
        }
        assertTrue(LanguageCatalog.isTarget(" german "))
        assertTrue(!LanguageCatalog.isTarget("Welsh"))
    }

    @Test
    fun nativeLanguageFallsBackToEnglish() {
        assertEquals("ru", LanguageCatalog.nativeOption("RU").code)
        assertEquals("de", LanguageCatalog.nativeOption("de").code)
        assertEquals("fr", LanguageCatalog.nativeOption("fr").code)
        assertEquals("en", LanguageCatalog.nativeOption("pl").code)
        assertEquals("en", LanguageCatalog.nativeOption("").code)
    }
}
