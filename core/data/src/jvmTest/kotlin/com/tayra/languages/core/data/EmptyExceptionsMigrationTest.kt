package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.language.LanguageDefinitions
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.BookStats
import com.tayra.languages.core.domain.repository.NewPage
import com.tayra.languages.core.domain.service.EmptyExceptionsMigration
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A Dutch saved with the exceptions list ending in "|", under which no word was found, loses the empty choice, and
 * its books' word counts, sentences and stats are worked out again.
 */
class EmptyExceptionsMigrationTest {

    private val file: File = File.createTempFile("tayra-exceptions", ".db").also { it.delete() }

    @Test
    fun theBooksAreReadAgainWithoutTheEmptyChoice() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val defined = LanguageDefinitions.all.first { it.name == "Dutch" }.language
        val dutch = languages.save(defined.copy(exceptionsSplitSentences = defined.exceptionsSplitSentences + "|"))
        val own = languages.save(defined.copy(name = "Mine", exceptionsSplitSentences = "bv.|nr."))
        books.insertBook(Book(languageId = dutch, title = "Thuis"), listOf(NewPage("Ik woon in Amsterdam, nr. 5. Het huis is klein.", 0)))
        val book = books.getBooks().first { it.languageId == dutch }
        books.saveStats(book.id, BookStats(distinctTerms = 0, distinctUnknowns = 0, unknownPercent = 0, statusDistribution = emptyMap()))

        val migration = EmptyExceptionsMigration(languages, books)
        assertEquals(listOf(dutch), migration.migrate())

        assertEquals(defined.exceptionsSplitSentences, languages.getById(dutch)?.exceptionsSplitSentences)
        assertEquals("bv.|nr.", languages.getById(own)?.exceptionsSplitSentences)
        assertEquals(9, books.getPages(book.id).single().wordCount)
        assertEquals(1, terms.references(dutch, "huis", includeUnread = true).size, "the sentences are cut into words")
        assertNull(books.getStats(book.id), "the stats are worked out again")

        assertEquals(emptyList(), migration.migrate(), "done once")
    }
}
