package com.tayra.languages.core.data.network

import com.tayra.languages.core.domain.courses.COURSES_PAGE_PREFIX
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.ExampleSource
import com.tayra.languages.core.domain.service.ExampleSourceFeatures
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceRoutingExamplesTest {

    private class Named(private val name: String, private val features: List<ExampleSourceFeatures>) : ExampleSentencesProvider {
        val pages = mutableListOf<String>()
        override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult(listOf(ExampleSentence(name, null)), 1, null)
        override suspend fun nextPage(nextPage: String, targetLanguage: String): ExampleSearchResult {
            pages += nextPage
            return ExampleSearchResult(listOf(ExampleSentence("$name next", null)), 1, null)
        }
        override suspend fun sources(language: Language) = features
    }

    private val leipzigFeatures = ExampleSourceFeatures(ExampleSource.LEIPZIG, audio = false, sorts = emptyList(), translations = false, phrases = false)
    private val language = Language(name = "Portuguese")

    @Test
    fun searchesAndPagesGoToTheSourceThatOwnsThem() = runTest {
        val tatoeba = Named("tatoeba", listOf(ExampleSourceFeatures.TATOEBA))
        val leipzig = Named("leipzig", listOf(leipzigFeatures))
        val courses = Named("courses", listOf(leipzigFeatures.copy(source = ExampleSource.COURSES)))
        val router = SourceRoutingExamples(tatoeba, leipzig, courses)
        assertEquals("tatoeba", router.search(ExampleSearchQuery("casa", language, "en")).sentences.single().text)
        assertEquals("leipzig", router.search(ExampleSearchQuery("casa", language, "en", source = ExampleSource.LEIPZIG)).sentences.single().text)
        assertEquals("leipzig next", router.nextPage("${LEIPZIG_PAGE_PREFIX}{}", "en").sentences.single().text)
        assertEquals("tatoeba next", router.nextPage("https://api.tatoeba.org/v1/sentences?after=3", "en").sentences.single().text)
        assertEquals("courses", router.search(ExampleSearchQuery("casa", language, "en", source = ExampleSource.COURSES)).sentences.single().text)
        assertEquals("courses next", router.nextPage("${COURSES_PAGE_PREFIX}4", "en").sentences.single().text)
        assertEquals(listOf(ExampleSource.TATOEBA, ExampleSource.COURSES, ExampleSource.LEIPZIG), router.sources(language).map { it.source })
    }

    @Test
    fun withoutLeipzigOnlyTatoebaIsOffered() = runTest {
        val router = SourceRoutingExamples(Named("tatoeba", listOf(ExampleSourceFeatures.TATOEBA)), null)
        assertEquals(listOf(ExampleSource.TATOEBA), router.sources(language).map { it.source })
        assertEquals(ExampleSearchResult.EMPTY, router.search(ExampleSearchQuery("casa", language, "en", source = ExampleSource.LEIPZIG)))
        assertEquals(ExampleSearchResult.EMPTY, router.nextPage("${LEIPZIG_PAGE_PREFIX}{}", "en"))
    }
}
