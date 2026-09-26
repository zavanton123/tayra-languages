package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Language

/** An example sentence using a term, with a translation when available. */
data class ExampleSentence(val text: String, val translation: String?)

enum class YesNo(val apiValue: String, val label: String) { YES("yes", "Yes"), NO("no", "No") }

enum class ExampleSort(val apiValue: String, val label: String) {
    RELEVANCE("relevance", "Relevance"),
    SHORTEST("words", "Shortest first"),
    LONGEST("-words", "Longest first"),
    NEWEST("-created", "Newest first"),
    OLDEST("created", "Oldest first"),
    RECENTLY_MODIFIED("-modified", "Recently modified"),
    RANDOM("random", "Random"),
}

enum class SentenceOrigin(val apiValue: String, val label: String) {
    ORIGINAL("original", "Original sentences"),
    TRANSLATION("translation", "Translations"),
    KNOWN("known", "Known origin"),
    UNKNOWN("unknown", "Unknown origin"),
}

/** Search criteria for example sentences; null means "any". */
data class ExampleSearchQuery(
    val text: String,
    val language: Language,
    val targetLanguage: String,
    val minWords: Int? = null,
    val maxWords: Int? = null,
    val sort: ExampleSort = ExampleSort.RELEVANCE,
    val isOrphan: YesNo? = YesNo.NO,
    val isUnapproved: YesNo? = YesNo.NO,
    val isNative: YesNo? = null,
    val hasAudio: YesNo? = null,
    val tags: List<String> = emptyList(),
    val listId: String? = null,
    val owner: String? = null,
    val origin: SentenceOrigin? = null,
    val transIsDirect: YesNo? = null,
    val transIsNative: YesNo? = null,
    val transHasAudio: YesNo? = null,
    val transIsUnapproved: YesNo? = null,
    val transIsOrphan: YesNo? = null,
    val limit: Int = 30,
)

data class ExampleSearchResult(
    val sentences: List<ExampleSentence>,
    val total: Int?,
    /** Opaque link to the next page, or null on the last page. */
    val nextPage: String?,
) {
    companion object {
        val EMPTY = ExampleSearchResult(emptyList(), null, null)
    }
}

/** Finds example sentences for a term, e.g. from a sentence corpus. */
interface ExampleSentencesProvider {
    /** Examples in the term's language, translated into the target language when possible. Never throws. */
    suspend fun examples(text: String, language: Language, targetLanguage: String, limit: Int = 20): List<ExampleSentence> =
        search(ExampleSearchQuery(text, language, targetLanguage, minWords = 3, maxWords = 14, limit = limit)).sentences

    suspend fun search(query: ExampleSearchQuery): ExampleSearchResult

    suspend fun nextPage(nextPage: String, targetLanguage: String): ExampleSearchResult
}
