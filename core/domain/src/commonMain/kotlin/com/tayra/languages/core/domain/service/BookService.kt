package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.book.PageSplitter
import com.tayra.languages.core.domain.book.SentenceBuilder
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.NewPage

class BookValidationException(message: String) : Exception(message)

enum class PagePosition { BEFORE, AFTER }

class BookService(
    private val books: BookRepository,
    private val languages: LanguageRepository,
) {

    suspend fun create(draft: BookDraft): Long {
        if (draft.title.isBlank()) throw BookValidationException("Title is required")
        if (draft.text.isBlank()) throw BookValidationException("Text is required")
        if (draft.wordsPerPage !in 1..BookDraft.MAX_WORDS_PER_PAGE) {
            throw BookValidationException("Words per page must be between 1 and ${BookDraft.MAX_WORDS_PER_PAGE}")
        }
        val language = language(draft.languageId)
        val pages = PageSplitter.split(draft.text, language, draft.splitBy, draft.wordsPerPage)
            .map { NewPage(it, SentenceBuilder.wordCount(it, language)) }
        if (pages.isEmpty()) throw BookValidationException("Text contains no words")
        val book = Book(
            languageId = language.id,
            title = draft.title.trim(),
            sourceUri = draft.sourceUri.trim().ifEmpty { null },
            audioFilename = draft.audioFilename,
            tags = cleanTags(draft.tags),
        )
        return books.insertBook(book, pages)
    }

    suspend fun update(draft: BookDraft) {
        val id = draft.id ?: throw BookValidationException("Book is not saved")
        if (draft.title.isBlank()) throw BookValidationException("Title is required")
        val book = books.getBook(id) ?: throw NoSuchElementException("No book $id")
        books.updateBook(
            book.copy(
                title = draft.title.trim(),
                sourceUri = draft.sourceUri.trim().ifEmpty { null },
                audioFilename = draft.audioFilename,
                tags = cleanTags(draft.tags),
            ),
        )
    }

    suspend fun archive(bookId: Long) = books.setArchived(bookId, true)
    suspend fun unarchive(bookId: Long) = books.setArchived(bookId, false)
    suspend fun delete(bookId: Long) = books.deleteBook(bookId)

    suspend fun updatePageText(bookId: Long, pageNumber: Int, text: String) {
        val book = books.getBook(bookId) ?: return
        val page = books.getPage(bookId, pageNumber) ?: return
        val language = language(book.languageId)
        books.updatePageText(page.id, text, SentenceBuilder.wordCount(text, language))
        books.replaceSentences(page.id, SentenceBuilder.build(text, language))
        books.clearStats(bookId)
    }

    /** Adds a page next to [pageNumber]; returns the new page's number. */
    suspend fun addPage(bookId: Long, position: PagePosition, pageNumber: Int, text: String): Int {
        val book = books.getBook(bookId) ?: throw NoSuchElementException("No book $bookId")
        val language = language(book.languageId)
        val count = books.pageCount(bookId)
        val anchor = pageNumber.coerceIn(1, maxOf(count, 1))
        val order = if (position == PagePosition.BEFORE) anchor else anchor + 1
        val pageId = books.insertPage(bookId, order, NewPage(text, SentenceBuilder.wordCount(text, language)))
        books.setCurrentPage(bookId, pageId)
        books.clearStats(bookId)
        return order
    }

    /** Deletes the page unless it is the only one. Returns false if nothing was deleted. */
    suspend fun deletePage(bookId: Long, pageNumber: Int): Boolean {
        if (books.pageCount(bookId) <= 1) return false
        val page = books.getPage(bookId, pageNumber) ?: return false
        books.deletePage(page.id)
        books.clearStats(bookId)
        return true
    }

    private suspend fun language(languageId: Long): Language =
        languages.getById(languageId) ?: throw BookValidationException("Please select a language")

    private fun cleanTags(tags: List<String>) = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
