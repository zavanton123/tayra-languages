package com.idiomiko.core.data

import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.BookRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.domain.language.LanguageDefinitions
import com.idiomiko.core.domain.model.Book
import com.idiomiko.core.domain.model.Term
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.parse.Lexicon
import com.idiomiko.core.domain.parse.Lexicons
import com.idiomiko.core.domain.repository.NewPage
import com.idiomiko.core.domain.service.CutWordsMigration
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Khmer came to be cut by syllables (កម្រិត is no longer កម + ្រិត), to match subscript ដ as ត and a subscript រ written
 * first as written after the subscript beside it: a phrase saved over the old cut is cut again, and a word saved with ដ
 * is the word with ត, one term with it, as one saved with រ first is the word with រ after.
 */
class KhmerParserMigrationTest {

    private val file: File = File.createTempFile("idiomiko-khmer", ".db").also { it.delete() }

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
        val scienceRoFirst = saved("សាស្រ្ត", TermStatus.NEW_2)
        books.insertBook(Book(languageId = id, title = "Home"), listOf(NewPage("កម្រិតខ្ពស់។", 2)))

        val words = listOf("កម", "កម្រិត", "ខ្ពស់", "សប្ដាហ៍", "ផ្ទះ", "សាស្ត្រ")
        val migration = CutWordsMigration(languages, terms, books) { Lexicons.set("km", Lexicon(words)) }

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 2, merged = 1)), migration.migrate())
        assertEquals(Triple("កម្រិត", 1, TermStatus.NEW_2), terms.getById(level)!!.let { Triple(it.text, it.tokenCount, it.status) })
        assertNull(terms.getById(weekDa))
        assertEquals(TermStatus.LEARNING_3, terms.getById(weekTa)!!.status)
        assertEquals("ផ្ទះ", terms.getById(home)!!.textLc)
        assertEquals("សាស្រ្ត" to "សាស្ត្រ", terms.getById(scienceRoFirst)!!.let { it.text to it.textLc })
        assertEquals(1, terms.references(id, "កម្រិត", includeUnread = true).size, "the sentences are cut as the words are")
        assertEquals(emptyMap(), migration.migrate(), "done once")
    }
}
