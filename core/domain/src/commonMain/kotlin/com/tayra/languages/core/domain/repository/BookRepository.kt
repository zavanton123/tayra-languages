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
    /** Inserts a page at [order], shifting later pages. Returns the new page id. */
    suspend fun insertPage(bookId: Long, order: Int, page: NewPage): Long
    suspend fun updatePageText(pageId: Long, text: String, wordCount: Int)
    /** Deletes the page and renumbers the following pages. */
    suspend fun deletePage(pageId: Long)
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
