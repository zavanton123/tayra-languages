package com.tayra.languages.core.data.network

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.ExampleSource
import com.tayra.languages.core.domain.service.ExampleSourceFeatures

/**
 * Sends a search to the source it asks for, and a next page to the source that made it; [leipzig]
 * is null where it cannot be reached (a browser, as the service allows no other sites).
 */
class SourceRoutingExamples(
    private val tatoeba: ExampleSentencesProvider,
    private val leipzig: ExampleSentencesProvider?,
) : ExampleSentencesProvider {

    override suspend fun search(query: ExampleSearchQuery): ExampleSearchResult = when (query.source) {
        ExampleSource.TATOEBA -> tatoeba.search(query)
        ExampleSource.LEIPZIG -> leipzig?.search(query) ?: ExampleSearchResult.EMPTY
    }

    override suspend fun nextPage(nextPage: String, targetLanguage: String): ExampleSearchResult =
        if (nextPage.startsWith(LEIPZIG_PAGE_PREFIX)) leipzig?.nextPage(nextPage, targetLanguage) ?: ExampleSearchResult.EMPTY
        else tatoeba.nextPage(nextPage, targetLanguage)

    override suspend fun sources(language: Language): List<ExampleSourceFeatures> =
        tatoeba.sources(language) + leipzig?.sources(language).orEmpty()
}
