package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Editing a book's text or page setup cuts it into new pages without losing the reader's place. */
class BookRebuildTest {

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-rebuild", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val wordsRead = WordsReadRepositoryImpl(provider)
    private val reading = ReadingService(books, languages, terms, wordsRead, TermService(terms, languages))
    private val service = BookService(books, languages)

    /** Twelve paragraphs of five words, so five words a page gives six pages of two paragraphs. */
    private val paragraphs = ('a'..'l').map { c -> "k$c la ma na pa." }
    private val text = paragraphs.joinToString("\n")

    private suspend fun book(): Long {
        val languageId = languages.save(Language(name = "English"))
        return service.create(BookDraft(languageId = languageId, title = "Book", text = text, tags = listOf("old"), wordsPerPage = 5))
    }

    @Test
    fun theTextIsTheJoinedPagesAndTheSetupIsEstimatedFromThem() = runTest {
        val id = book()
        assertEquals(6, books.pageCount(id))
        assertEquals(text, service.text(id))
        assertEquals(5, service.estimatedWordsPerPage(id))
    }

    @Test
    fun rebuildingCarriesThePlaceBookmarksAndReadPagesOver() = runTest {
        val id = book()
        reading.markPageRead(id, 1, markRestAsKnown = false)
        reading.markPageRead(id, 2, markRestAsKnown = false)
        reading.openPage(id, 3, trackOpen = true)
        books.addBookmark(books.getPage(id, 4)!!.id, "Here")
        val readBefore = wordsRead.dailyCounts().sumOf { it.wordCount }

        val draft = BookDraft(id = id, languageId = books.getBook(id)!!.languageId, title = "Renamed", text = service.text(id), tags = listOf("new"), wordsPerPage = 15)
        service.update(draft, rebuildPages = true)

        // Twenty-word pages now: old pages 1-2 make page 1, 3-4 page 2, 5-6 page 3.
        val pages = books.getPages(id)
        assertEquals(listOf(20, 20, 20), pages.map { it.wordCount })
        assertEquals(text, service.text(id))
        val book = books.getBook(id)!!
        assertEquals("Renamed", book.title)
        assertEquals(listOf("new"), book.tags)
        assertEquals(pages[1].id, book.currentPageId, "the reader was on old page 3, now part of page 2")
        assertEquals(listOf(2 to "Here"), books.observeBookmarks(id).first().map { it.pageNumber to it.title })
        assertNotNull(pages[0].readDate, "both pages it covers were read")
        assertNull(pages[1].readDate, "old page 4 was never read")
        assertNotNull(pages[1].startDate, "old page 3 was opened")
        assertNull(pages[2].startDate)
        assertEquals(readBefore, wordsRead.dailyCounts().sumOf { it.wordCount }, "the words read stay counted")
    }

    @Test
    fun savingOnlyTheDetailsKeepsThePages() = runTest {
        val id = book()
        val before = books.getPages(id).map { it.id }
        service.update(BookDraft(id = id, languageId = books.getBook(id)!!.languageId, title = "Renamed", text = "ignored"))
        assertEquals(before, books.getPages(id).map { it.id })
    }
}
