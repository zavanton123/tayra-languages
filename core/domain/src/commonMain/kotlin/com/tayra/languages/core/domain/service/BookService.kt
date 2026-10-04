package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.book.PageSplitter
import com.tayra.languages.core.domain.book.SentenceBuilder
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.NewPage
import com.tayra.languages.core.domain.repository.RebuiltBookmark
import com.tayra.languages.core.domain.repository.RebuiltPage
import kotlinx.coroutines.flow.first

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
        val pages = PageSplitter.split(draft.text, language, draft.wordsPerPage)
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

    /**
     * Saves the book's title, tags and source. With [rebuildPages] the draft's text is cut into
     * pages again by its words per page; see [replacePages] for what carries over.
     */
    suspend fun update(draft: BookDraft, rebuildPages: Boolean = false) {
        val id = draft.id ?: throw BookValidationException("Book is not saved")
        if (draft.title.isBlank()) throw BookValidationException("Title is required")
        val book = books.getBook(id) ?: throw NoSuchElementException("No book $id")
        if (rebuildPages) replacePages(book, draft)
        books.updateBook(
            book.copy(
                title = draft.title.trim(),
                sourceUri = draft.sourceUri.trim().ifEmpty { null },
                audioFilename = draft.audioFilename,
                tags = cleanTags(draft.tags),
            ),
        )
    }

    /** The whole text of the book: its pages joined, each starting a paragraph. */
    suspend fun text(bookId: Long): String = books.getPages(bookId).joinToString("\n") { it.text }

    /**
     * A words-per-page setting that would give pages like the book's: pages end once they pass
     * the setting, so it sits just below a typical full page. The last page, often short, is
     * left out.
     */
    suspend fun estimatedWordsPerPage(bookId: Long): Int {
        val full = books.getPages(bookId).dropLast(1).map { it.wordCount }.filter { it > 0 }.sorted()
        if (full.isEmpty()) return BookDraft.DEFAULT_WORDS_PER_PAGE
        val typical = full[full.size / 2]
        val step = if (typical >= 100) 50 else 5
        // A page of exactly the setting would not have closed yet, so the setting is below it.
        return ((typical - 1) / step * step).coerceIn(1, BookDraft.MAX_WORDS_PER_PAGE)
    }

    /**
     * Cuts [draft]'s text into new pages. Old and new pages are matched by where they fall in the
     * text, counted in words (scaled when the text grew or shrank): the book reopens where the
     * reader was, bookmarks move to the page now holding their place, and a new page counts as
     * read only when every old page it covers was read.
     */
    private suspend fun replacePages(book: Book, draft: BookDraft) {
        if (draft.text.isBlank()) throw BookValidationException("Text is required")
        if (draft.wordsPerPage !in 1..BookDraft.MAX_WORDS_PER_PAGE) {
            throw BookValidationException("Words per page must be between 1 and ${BookDraft.MAX_WORDS_PER_PAGE}")
        }
        val language = language(book.languageId)
        val texts = PageSplitter.split(draft.text, language, draft.wordsPerPage)
        if (texts.isEmpty()) throw BookValidationException("Text contains no words")
        val counts = texts.map { SentenceBuilder.wordCount(it, language) }
        val old = books.getPages(book.id)
        val oldStarts = old.runningFold(0) { sum, page -> sum + page.wordCount }
        val newStarts = counts.runningFold(0) { sum, count -> sum + count }
        val oldTotal = oldStarts.last().coerceAtLeast(1)
        val newTotal = newStarts.last().coerceAtLeast(1)

        fun newIndexOfOldPage(oldIndex: Int): Int {
            val offset = oldStarts[oldIndex.coerceIn(0, old.lastIndex.coerceAtLeast(0))].toLong() * newTotal / oldTotal
            return newStarts.indexOfLast { it <= offset }.coerceIn(0, texts.lastIndex)
        }

        val pages = texts.indices.map { index ->
            val from = newStarts[index].toLong() * oldTotal / newTotal
            val to = newStarts[index + 1].toLong() * oldTotal / newTotal
            val covered = old.indices.filter { i -> oldStarts[i] < to && oldStarts[i + 1] > from }
                .ifEmpty { listOfNotNull(old.indices.lastOrNull { oldStarts[it] <= from }) }
                .map { old[it] }
            RebuiltPage(
                text = texts[index],
                wordCount = counts[index],
                startDate = covered.mapNotNull { it.startDate }.maxOrNull(),
                readDate = if (covered.isNotEmpty() && covered.all { it.isRead }) covered.mapNotNull { it.readDate }.maxOrNull() else null,
            )
        }
        val current = old.indexOfFirst { it.id == book.currentPageId }.takeIf { it >= 0 }?.let(::newIndexOfOldPage) ?: 0
        val bookmarks = books.observeBookmarks(book.id).first().map { RebuiltBookmark(newIndexOfOldPage(it.pageNumber - 1), it.title) }
        books.replacePages(book.id, pages, current, bookmarks)
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
