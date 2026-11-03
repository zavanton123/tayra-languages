package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.domain.flashcards.ReadingWords
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.NewPage
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** The words ahead: on the page reached in the text read last and the few after it, a form standing for its family's card. */
class ReadingWordsTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-ahead", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val cards = FlashcardRepositoryImpl(provider)
    private val words = ReadingWords(books, languages, ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages)), terms, cards)

    private val pageWords = listOf("casa", "gato", "lobo", "rio", "mar", "sol", "lua", "flor", "mesa", "porta")

    @Test
    fun theWordsOnThePagesAheadInTheTextReadLast() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        val ids = pageWords.associateWith { terms.save(Term(languageId = pt, text = it, textLc = it, status = TermStatus.NEW_1)) }
        // "gatos" is learned with "gato"'s card.
        val gatos = terms.save(Term(languageId = pt, text = "gatos", textLc = "gatos", status = TermStatus.NEW_1))
        terms.setParents(gatos, listOf(ids.getValue("gato")))
        cards.reconcile(Instant.parse("2026-10-01T10:00:00Z"))

        val bookId = books.insertBook(Book(languageId = pt, title = "Dez páginas"), pageWords.mapIndexed { i, w -> NewPage(if (w == "gato") "Os gatos dormem." else "A $w é bonita.", 3) })
        assertEquals(emptySet(), words.termIds(pt), "nothing opened yet")

        // Reached page 2 ("gatos"): it and the five after it, to page 7.
        val page = books.getPage(bookId, 2)!!
        books.setCurrentPage(bookId, page.id)
        books.setPageStartDate(page.id, Instant.parse("2026-10-02T10:00:00Z"))
        assertEquals(listOf("gato", "lobo", "rio", "mar", "sol", "lua").map { ids.getValue(it) }.toSet(), words.termIds(pt))
    }
}
