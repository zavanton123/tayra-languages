package com.tayra.languages.core.domain.repository

import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.domain.model.BookStats
import com.tayra.languages.core.domain.model.Page
import com.tayra.languages.core.domain.model.PageBookmark
import com.tayra.languages.core.domain.model.Sentence
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

data class NewPage(val text: String, val wordCount: Int)

/** A page of a rebuilt book, with the dates carried over from the pages it replaces. */
data class RebuiltPage(val text: String, val wordCount: Int, val startDate: Instant? = null, val readDate: Instant? = null)

/** A bookmark to re-add on a rebuilt book, on the page at [pageIndex] (from 0). */
data class RebuiltBookmark(val pageIndex: Int, val title: String)

interface BookRepository {
    fun observeBooks(archived: Boolean): Flow<List<BookListItem>>
    fun observeBook(id: Long): Flow<Book?>
    suspend fun getBook(id: Long): Book?
    suspend fun getBooks(): List<Book>
    suspend fun findByTitle(title: String, languageId: Long): Book?
    suspend fun insertBook(book: Book, pages: List<NewPage>): Long
    suspend fun updateBook(book: Book)
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun deleteBook(id: Long)
    suspend fun allBookTags(): List<String>

    suspend fun getPages(bookId: Long): List<Page>
    suspend fun getPage(bookId: Long, order: Int): Page?
    suspend fun getPageById(pageId: Long): Page?
    suspend fun pageCount(bookId: Long): Int
    suspend fun updatePageText(pageId: Long, text: String, wordCount: Int)
    /**
     * Replaces all of the book's pages in one transaction: the old pages go with their sentences
     * and bookmarks (words read keep their counts), [bookmarks] are added on the new pages and the
     * book opens at the page at [currentIndex].
     */
    suspend fun replacePages(bookId: Long, pages: List<RebuiltPage>, currentIndex: Int, bookmarks: List<RebuiltBookmark>)
    suspend fun setCurrentPage(bookId: Long, pageId: Long)
    suspend fun setPageStartDate(pageId: Long, date: Instant)
    suspend fun setPageReadDate(pageId: Long, date: Instant)
    suspend fun replaceSentences(pageId: Long, sentences: List<Sentence>)

    suspend fun getStats(bookId: Long): BookStats?
    suspend fun saveStats(bookId: Long, stats: BookStats)
    suspend fun clearStats(bookId: Long)
    suspend fun bookIdsWithoutStats(): List<Long>

    fun observeBookmarks(bookId: Long): Flow<List<PageBookmark>>
    suspend fun addBookmark(pageId: Long, title: String): Long
    suspend fun renameBookmark(id: Long, title: String)
    suspend fun deleteBookmark(id: Long)
}
