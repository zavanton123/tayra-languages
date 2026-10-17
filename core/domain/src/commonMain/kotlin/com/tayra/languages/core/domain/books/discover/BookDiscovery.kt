package com.tayra.languages.core.domain.books.discover

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.parse.isSupported
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.ReadingService

/**
 * A free library online to find books in. [judgedOnSight] libraries have a cheap sample of each
 * book, so their books are judged as they are listed; the others only when the reader asks.
 */
enum class BookSource(val judgedOnSight: Boolean) {
    /** Project Gutenberg's public-domain classics. */
    GUTENBERG(judgedOnSight = true),

    /** Wikisource's volunteer transcriptions of public-domain and freely licensed texts, one wiki per language. */
    WIKISOURCE(judgedOnSight = true),

    /** The Global Digital Library's openly licensed early-grade readers, StoryWeaver's among them. */
    GDL(judgedOnSight = false),
}

enum class Difficulty { EASY, MEDIUM, HARD }

/** A book a free library offers, before it is added to the reader's own. */
data class FoundBook(
    val source: BookSource,
    val id: String,
    val title: String,
    /** Its authors, or its publisher when the library names no author. */
    val authors: List<String>,
    val description: String,
    val subjects: List<String>,
    /** The library's own reading level ("Level 2"), for libraries that grade their books. */
    val level: String? = null,
    /** How hard the library's grading makes the book, without looking at its words. */
    val levelDifficulty: Difficulty? = null,
    /** "Public domain", "CC-BY-4.0"... */
    val license: String,
    /** The book's page in the library; it marks the book once it is in the reader's library. */
    val pageUrl: String,
    val downloadUrl: String,
    val coverUrl: String? = null,
)

/** One page of a catalog's books; [total] counts the books on every page, null where the catalog does not say. */
data class CatalogPage(val books: List<FoundBook>, val hasMore: Boolean, val total: Int? = null)

/** A free library's catalog: what it has in a language, and the texts of its books. */
interface BookCatalog {
    val source: BookSource

    /** The books in the language with the ISO 639-1 [languageCode], matching [query] unless it is blank; [page] counts from 1. */
    suspend fun search(languageCode: String, query: String, page: Int): CatalogPage

    /** The opening of the book's text, enough to judge how hard it is; cheaper than the whole text where the library allows. */
    suspend fun sample(book: FoundBook): String

    /** The book's whole text, one paragraph per line. */
    suspend fun text(book: FoundBook): String
}

/** How much of a text the reader knows: its running words, each counted where it stands. */
data class TextCoverage(val words: Int, val known: Int) {
    val percent: Int get() = if (words == 0) 0 else 100 * known / words

    /** At 95% of the words known a text reads comfortably; below 85% it is hard going. */
    val difficulty: Difficulty
        get() = when {
            percent >= 95 -> Difficulty.EASY
            percent >= 85 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }
}

/**
 * Free books to read, found in online libraries and judged by the words the reader knows; one of
 * them added to the library becomes a book like any other, with its source as its source URI.
 */
class BookDiscoveryService(
    private val catalogs: List<BookCatalog>,
    private val readingService: ReadingService,
    private val bookService: BookService,
    private val books: BookRepository,
) {
    val sources: List<BookSource> get() = catalogs.map { it.source }

    /** The books of [source] in [language]; nothing for a language the app has no code for. */
    suspend fun search(source: BookSource, language: Language, query: String, page: Int): CatalogPage {
        val code = LanguageCodes.codeFor(language.name) ?: return CatalogPage(emptyList(), hasMore = false, total = 0)
        return catalog(source).search(code, query.trim(), page)
    }

    /** How much of the book's opening the reader knows; null for a language the app cannot split into words. */
    suspend fun coverage(book: FoundBook, language: Language): TextCoverage? {
        if (!language.isSupported) return null
        val sample = catalog(book.source).sample(book)
        val words = readingService.renderItems(opening(sample), language).filter { it.isWord && it.tokenCount == 1 }
        return TextCoverage(words.size, words.count { it.status != TermStatus.UNKNOWN })
    }

    /** Adds the book to [language]'s library with [attribution] as its last line, and returns its id. */
    suspend fun addToLibrary(book: FoundBook, language: Language, attribution: String): Long {
        val text = catalog(book.source).text(book)
        val tags = listOfNotNull(book.level, book.source.tag)
        return bookService.create(BookDraft(languageId = language.id, title = book.title, text = "$text\n\n$attribution", sourceUri = book.pageUrl, tags = tags))
    }

    /** The reader's books found in a library, by their page there. */
    suspend fun libraryBooks(): Map<String, Long> =
        books.getBooks().mapNotNull { book -> book.sourceUri?.takeIf { uri -> LIBRARY_HOSTS.any { it in uri } }?.let { it to book.id } }.toMap()

    private fun catalog(source: BookSource): BookCatalog = catalogs.first { it.source == source }

    private companion object {
        val LIBRARY_HOSTS = listOf("gutenberg.org", "wikisource.org", "digitallibrary.io")

        /** Enough words to judge a text by, without parsing a whole novel. */
        const val SAMPLE_CHARACTERS = 15_000

        /** The opening of [text] up to a paragraph's end. */
        fun opening(text: String): String =
            if (text.length <= SAMPLE_CHARACTERS) text
            else text.substring(0, SAMPLE_CHARACTERS).substringBeforeLast('\n').ifEmpty { text.substring(0, SAMPLE_CHARACTERS) }
    }
}

private val BookSource.tag: String
    get() = when (this) {
        BookSource.GUTENBERG -> "gutenberg"
        BookSource.WIKISOURCE -> "wikisource"
        BookSource.GDL -> "gdl"
    }
