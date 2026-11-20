package com.idiomiko.core.domain.language

import com.idiomiko.core.domain.courses.CoursePacks
import com.idiomiko.core.domain.dictionary.DictionaryPacks
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A language the app ships learning material for has to be in the catalog, or the app removes it on start. */
class LanguageCatalogCoverageTest {
    private val root = File("../..")

    private val definedCodes: Set<String> = LanguageDefinitions.all.mapNotNull { LanguageCodes.codeFor(it.name) }.toSet()

    private val catalogCodes: Set<String> = LanguageCatalog.targetLanguages.mapNotNull { LanguageCodes.codeFor(it) }.toSet()

    @Test
    fun everyLanguageWithAFrequencyListIsInTheCatalog() {
        val codes = root.resolve("feature/frequency/src/commonMain/composeResources/files/frequency")
            .listFiles { file -> file.extension == "tsv" }.orEmpty().map { it.nameWithoutExtension }
        assertTrue(codes.isNotEmpty(), "no frequency lists found under ${root.canonicalPath}")
        assertCatalogued(codes, "a frequency list")
    }

    @Test
    fun everyLanguageWithCoursesIsInTheCatalog() {
        val sources = root.resolve("tools/courses").listFiles { file -> file.isDirectory }.orEmpty().map { it.name }
        assertTrue(sources.isNotEmpty(), "no course sources found under ${root.canonicalPath}")
        assertCatalogued(sources, "course sources")
        assertCatalogued(CoursePacks.all.map { it.languageCode }, "a course pack")
    }

    @Test
    fun everyLearnableDictionarySourceIsInTheCatalog() {
        assertCatalogued(DictionaryPacks.all.map { it.id.sourceLanguage }, "a dictionary")
        assertCatalogued(DictionaryPacks.glossaries.map { it.id.targetLanguage }, "a word finder")
    }

    private fun assertCatalogued(codes: Collection<String>, what: String) {
        val missing = codes.filter { it in definedCodes && it !in catalogCodes }.distinct().sorted()
        assertEquals(emptyList(), missing, "languages with $what that LanguageCatalog.targetLanguages leaves out")
    }
}
