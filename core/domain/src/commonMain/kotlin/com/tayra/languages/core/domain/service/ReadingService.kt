package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.book.SentenceBuilder
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Page
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.model.WordsReadEntry
import com.tayra.languages.core.domain.parse.ParsedToken
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.parse.parseTokens
import com.tayra.languages.core.domain.render.MultiwordTermIndex
import com.tayra.languages.core.domain.render.RenderedPage
import com.tayra.languages.core.domain.render.TextItem
import com.tayra.languages.core.domain.render.TextItemCalculator
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.repository.WordsReadRepository
import kotlin.time.Clock

/** Everything needed to show a page of a book. */
data class ReadingPage(
    val book: Book,
    val language: Language,
    val page: Page,
    val pageCount: Int,
    val rendered: RenderedPage,
)

class ReadingService(
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val terms: TermRepository,
    private val wordsRead: WordsReadRepository,
    private val termService: TermService,
    private val clock: Clock = Clock.System,
) {

    /**
     * Loads and renders a page. When [trackOpen] is true the page becomes the book's
     * current page and its start date is recorded.
     */
    suspend fun openPage(bookId: Long, pageNumber: Int, trackOpen: Boolean): ReadingPage {
        val book = books.getBook(bookId) ?: throw NoSuchElementException("No book $bookId")
        val language = languages.getById(book.languageId) ?: throw NoSuchElementException("No language ${book.languageId}")
        val pageCount = books.pageCount(bookId)
        val order = pageNumber.coerceIn(1, maxOf(pageCount, 1))
        val page = books.getPage(bookId, order) ?: throw NoSuchElementException("No page $order in book $bookId")

        books.replaceSentences(page.id, SentenceBuilder.build(page.text, language))
        books.clearStats(bookId)
        if (trackOpen) {
            books.setPageStartDate(page.id, clock.now())
            books.setCurrentPage(bookId, page.id)
        }

        val rendered = renderPage(page.text, language)
        return ReadingPage(book, language, page, pageCount, rendered)
    }

    /** Current page number of the book (1 if never opened). */
    suspend fun currentPageNumber(book: Book): Int =
        book.currentPageId?.let { books.getPageById(it)?.order } ?: 1

    /**
     * Renders text, persisting new status-0 terms so every word has a term id.
     */
    suspend fun renderPage(text: String, language: Language): RenderedPage {
        val tokens = language.parseTokens(text)
        var found = findTermsInTokens(tokens, language)
        var result = TextItemCalculator.calculate(tokens, found, language)
        if (result.newTerms.isNotEmpty()) {
            terms.insertAll(result.newTerms)
            found = findTermsInTokens(tokens, language)
            result = TextItemCalculator.calculate(tokens, found, language)
        }
        return TextItemCalculator.toPage(result.items)
    }

    /** Renders text without touching the database (used for statistics). */
    suspend fun renderItems(text: String, language: Language, multiwordIndex: MultiwordTermIndex? = null): List<TextItem> {
        val tokens = language.parseTokens(text)
        val found = findTermsInTokens(tokens, language, multiwordIndex)
        return TextItemCalculator.calculate(tokens, found, language).items
    }

    /** Saved terms (single and multi-word) contained in the text. */
    suspend fun findTermsInText(text: String, language: Language): List<Term> =
        findTermsInTokens(language.parseTokens(text.replace(Regex(" +"), " ")), language)

    suspend fun findTermsInTokens(
        tokens: List<ParsedToken>,
        language: Language,
        multiwordIndex: MultiwordTermIndex? = null,
    ): List<Term> {
        val tokensLc = tokens.map { language.lowercase(it.token) }
        val index = multiwordIndex ?: multiwordIndex(language)
        val textLcs = LinkedHashSet<String>(tokensLc)
        textLcs.addAll(index.findTerms(tokensLc))
        return terms.findByTextLcs(language.id, textLcs)
    }

    suspend fun multiwordIndex(language: Language): MultiwordTermIndex =
        MultiwordTermIndex(terms.multiwordTerms(language.id).map { it.textLc })

    /** Marks the page read, records the words read, and optionally sets unknown words to well known. */
    suspend fun markPageRead(bookId: Long, pageNumber: Int, markRestAsKnown: Boolean) {
        val book = books.getBook(bookId) ?: return
        val page = books.getPage(bookId, pageNumber) ?: return
        val language = languages.getById(book.languageId) ?: return
        val now = clock.now()
        books.setPageReadDate(page.id, now)
        books.replaceSentences(page.id, SentenceBuilder.build(page.text, language))
        wordsRead.add(WordsReadEntry(language.id, page.id, now, page.wordCount))
        if (markRestAsKnown) setUnknownsToWellKnown(page.text, language)
    }

    suspend fun setUnknownsToWellKnown(text: String, language: Language) {
        val rendered = renderPage(text, language)
        val unknownIds = rendered.words
            .filter { it.status == TermStatus.UNKNOWN }
            .mapNotNull { it.termId }
            .distinct()
        if (unknownIds.isNotEmpty()) termService.setStatus(unknownIds, TermStatus.WELL_KNOWN)
    }

    /** Creates or updates terms for the given texts with the status (bulk "quick set"). */
    suspend fun setStatusForTexts(language: Language, texts: List<String>, status: TermStatus) {
        for (text in texts) {
            val draft = termService.findOrNew(language.id, text).copy(status = status, statusExplicitlySet = true)
            termService.save(draft)
        }
    }
}
