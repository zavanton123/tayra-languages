package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.language.LanguageDefinitions
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.parse.Lexicon
import com.tayra.languages.core.domain.parse.Lexicons
import com.tayra.languages.core.domain.repository.NewPage
import com.tayra.languages.core.domain.service.CutWordsMigration
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Khmer came to be cut by syllables (កម្រិត is no longer កម + ្រិត) and to match subscript ដ as ត: a phrase saved over the
 * old cut is cut again, and a word saved with ដ is the word with ត, one term with it.
 */
class KhmerParserMigrationTest {

    private val file: File = File.createTempFile("tayra-khmer", ".db").also { it.delete() }

    @AfterTest
    fun forget() = Lexicons.set("km", null)

    @Test
    fun termsAreKeptAsTheReaderNowReadsThem() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Khmer" }.language)
        suspend fun saved(text: String, status: TermStatus, tokens: Int = 1) =
            terms.save(Term(languageId = id, text = text, textLc = text, status = status, tokenCount = tokens))
        // កម្រិត "level" read as កម + ្រិត and saved as the two together; សប្ដាហ៍ "week" saved in both spellings.
        val level = saved("កម​្រិត", TermStatus.NEW_2, tokens = 2)
        val weekDa = saved("សប្ដាហ៍", TermStatus.LEARNING_3)
        val weekTa = saved("សប្តាហ៍", TermStatus.NEW_1)
        val home = saved("ផ្ទះ", TermStatus.NEW_1)
        books.insertBook(Book(languageId = id, title = "Home"), listOf(NewPage("កម្រិតខ្ពស់។", 2)))

        val words = listOf("កម", "កម្រិត", "ខ្ពស់", "សប្ដាហ៍", "ផ្ទះ")
        val migration = CutWordsMigration(languages, terms, books) { Lexicons.set("km", Lexicon(words)) }

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 1, merged = 1)), migration.migrate())
        assertEquals(Triple("កម្រិត", 1, TermStatus.NEW_2), terms.getById(level)!!.let { Triple(it.text, it.tokenCount, it.status) })
        assertNull(terms.getById(weekDa))
        assertEquals(TermStatus.LEARNING_3, terms.getById(weekTa)!!.status)
        assertEquals("ផ្ទះ", terms.getById(home)!!.textLc)
        assertEquals(1, terms.references(id, "កម្រិត", includeUnread = true).size, "the sentences are cut as the words are")
        assertEquals(emptyMap(), migration.migrate(), "done once")
    }
}
