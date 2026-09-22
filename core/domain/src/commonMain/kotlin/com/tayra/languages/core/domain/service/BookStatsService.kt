package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.BookStats
import com.tayra.languages.core.domain.parse.isSupported
import com.tayra.languages.core.domain.render.TextItem
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.stats.BookStatsCalculator

/**
 * Book statistics (distribution of term statuses), calculated from a sample of pages
 * around the current page and cached per book.
 */
class BookStatsService(
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val readingService: ReadingService,
) {

    /** Cached stats, or freshly calculated ones. */
    suspend fun stats(bookId: Long): BookStats? {
        books.getStats(bookId)?.let { return it }
        val book = books.getBook(bookId) ?: return null
        return calculate(book)?.also { books.saveStats(bookId, it) }
    }

    suspend fun refreshAll() {
        for (id in books.bookIdsWithoutStats()) {
            val book = books.getBook(id) ?: continue
            calculate(book)?.let { books.saveStats(id, it) }
        }
    }

    suspend fun markStale(bookId: Long) = books.clearStats(bookId)

    suspend fun calculate(book: Book): BookStats? {
        val language = languages.getById(book.languageId) ?: return null
        if (!language.isSupported) return null
        val pages = books.getPages(book.id)
        if (pages.isEmpty()) return null

        val currentIndex = pages.indexOfFirst { it.id == book.currentPageId }.coerceAtLeast(0)
        val sampleSize = settings.current.statsSampleSize.coerceAtLeast(1)
        val start = maxOf(0, currentIndex - sampleSize)
        val end = minOf(pages.size, currentIndex + sampleSize)
        val sample = pages.subList(start, end).takeLast(sampleSize)

        val index = readingService.multiwordIndex(language)
        val items = mutableListOf<TextItem>()
        for (page in sample) items += readingService.renderItems(page.text, language, index)
        return BookStatsCalculator.calculate(items)
    }
}
