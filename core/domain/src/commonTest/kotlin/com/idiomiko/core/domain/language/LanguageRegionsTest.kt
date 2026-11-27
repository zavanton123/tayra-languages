package com.idiomiko.core.domain.language

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every language to learn is in a group, and the popular ones are languages to learn. */
class LanguageRegionsTest {

    @Test
    fun everyLanguageIsInAGroup() {
        assertEquals(emptyList(), LanguageCatalog.targetLanguages.filter { LanguageRegions.of(it).isEmpty() })
    }

    @Test
    fun thePopularLanguagesAreLanguagesToLearn() {
        assertTrue(LanguageRegions.popular.all(LanguageCatalog::isTarget))
    }

    @Test
    fun aLanguageBetweenGroupsIsInEach() {
        assertEquals(setOf(LanguageRegion.EUROPE, LanguageRegion.ASIA), LanguageRegions.of("Russian"))
        assertEquals(setOf(LanguageRegion.EUROPE, LanguageRegion.MIDDLE_EAST), LanguageRegions.of("Turkish"))
        assertEquals(setOf(LanguageRegion.AMERICAS_PACIFIC), LanguageRegions.of("Hawaiian"))
    }
}
