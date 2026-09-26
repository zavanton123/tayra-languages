package com.tayra.languages.core.domain.language

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
}
