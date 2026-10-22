package com.tayra.languages.core.data.network

import com.tayra.languages.core.domain.courses.COURSES_PAGE_PREFIX
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.ExampleSource
import com.tayra.languages.core.domain.service.ExampleSourceFeatures
import com.tayra.languages.core.domain.service.failedOnError

/**
 * Sends a search to the source it asks for (Tatoeba, the installed courses or Leipzig), and a next
 * page to the source that made it; [leipzig] is null where it cannot be reached (a browser, as the
 * service allows no other sites). A source that throws gives a failed result.
 */
class SourceRoutingExamples(
    private val tatoeba: ExampleSentencesProvider,
    private val leipzig: ExampleSentencesProvider?,
    private val courses: ExampleSentencesProvider? = null,
) : ExampleSentencesProvider {

    override suspend fun search(query: ExampleSearchQuery): ExampleSearchResult = failedOnError {
        when (query.source) {
            ExampleSource.TATOEBA -> tatoeba.search(query)
            ExampleSource.COURSES -> courses?.search(query) ?: ExampleSearchResult.EMPTY
            ExampleSource.LEIPZIG -> leipzig?.search(query) ?: ExampleSearchResult.EMPTY
        }
    }

    override suspend fun nextPage(nextPage: String, targetLanguage: String): ExampleSearchResult = failedOnError {
        when {
            nextPage.startsWith(LEIPZIG_PAGE_PREFIX) -> leipzig?.nextPage(nextPage, targetLanguage) ?: ExampleSearchResult.EMPTY
            nextPage.startsWith(COURSES_PAGE_PREFIX) -> courses?.nextPage(nextPage, targetLanguage) ?: ExampleSearchResult.EMPTY
            else -> tatoeba.nextPage(nextPage, targetLanguage)
        }
    }

    override suspend fun sources(language: Language): List<ExampleSourceFeatures> =
        tatoeba.sources(language) + courses?.sources(language).orEmpty() + leipzig?.sources(language).orEmpty()
}
