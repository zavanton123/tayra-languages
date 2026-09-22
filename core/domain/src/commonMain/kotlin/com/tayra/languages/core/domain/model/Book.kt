package com.tayra.languages.core.domain.model

import kotlin.time.Instant

data class Book(
    val id: Long = 0,
    val languageId: Long,
    val title: String,
    val sourceUri: String? = null,
    val currentPageId: Long? = null,
    val archived: Boolean = false,
    val audioFilename: String? = null,
    val audioCurrentPos: Double? = null,
    val audioBookmarks: String? = null,
    val tags: List<String> = emptyList(),
)

/** One page of a book. */
data class Page(
    val id: Long = 0,
    val bookId: Long,
    val order: Int,
    val text: String,
    val wordCount: Int = 0,
    val startDate: Instant? = null,
    val readDate: Instant? = null,
) {
    val isRead: Boolean get() = readDate != null
}

/** A parsed sentence of a page: tokens joined by [ZWS], wrapped in [ZWS]. */
data class Sentence(
    val order: Int,
    val text: String,
    val textLc: String,
)

data class PageBookmark(
    val id: Long = 0,
    val pageId: Long,
    val pageNumber: Int,
    val title: String,
)

data class WordsReadEntry(
    val languageId: Long,
    val pageId: Long?,
    val readAt: Instant,
    val wordCount: Int,
)

data class BookStats(
    val distinctTerms: Int,
    val distinctUnknowns: Int,
    val unknownPercent: Int,
    val statusDistribution: Map<TermStatus, Int>,
)

/** A row in the book listing. */
data class BookListItem(
    val id: Long,
    val title: String,
    val languageId: Long,
    val languageName: String,
    val tags: List<String>,
    val currentPage: Int,
    val pageCount: Int,
    val wordCount: Int,
    val lastOpened: Instant?,
    val isCompleted: Boolean,
    val isArchived: Boolean,
    val stats: BookStats?,
    val sourceUri: String?,
)

enum class PageSplitMode(val key: String, val label: String) {
    PARAGRAPHS("paragraphs", "Paragraphs"),
    SENTENCES("sentences", "Sentences");

    companion object {
        fun fromKey(key: String): PageSplitMode = entries.firstOrNull { it.key == key } ?: PARAGRAPHS
    }
}

/** Data captured when creating or editing a book. */
data class BookDraft(
    val id: Long? = null,
    val languageId: Long,
    val title: String,
    val text: String = "",
    val sourceUri: String = "",
    val tags: List<String> = emptyList(),
    val splitBy: PageSplitMode = PageSplitMode.PARAGRAPHS,
    val wordsPerPage: Int = DEFAULT_WORDS_PER_PAGE,
    val audioFilename: String? = null,
) {
    companion object {
        const val DEFAULT_WORDS_PER_PAGE = 250
        const val MAX_WORDS_PER_PAGE = 1500
    }
}
