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

/**
 * Lao came to be cut between syllables only (ຕາມ, no longer ຕາ "eye" + ມ) and read in today's spelling (ຫນັງສື as ໜັງສື): a
 * phrase saved over the old cut is cut again, a word saved in the old spelling is matched in the new one, and a word saved
 * that the reader now reads as several (ຄິດວ່າ, ຄິດ + ວ່າ) becomes a phrase of them.
 */
class LaoParserMigrationTest {

    private val file: File = File.createTempFile("tayra-lao", ".db").also { it.delete() }

    @AfterTest
    fun forget() = Lexicons.set("lo", null)

    @Test
    fun termsAreReadAsTheReaderNowReadsThem() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Lao" }.language)
        suspend fun saved(text: String, status: TermStatus, tokens: Int = 1) =
            terms.save(Term(languageId = id, text = text, textLc = text, status = status, tokenCount = tokens))
        // ຕາມ "along" read as ຕາ + ມ, saved as the two together; ຕາ "eye" alone stays.
        val along = saved("ຕາ​ມ", TermStatus.NEW_2, tokens = 2)
        val eye = saved("ຕາ", TermStatus.NEW_1)
        val book = saved("ຫນັງສື", TermStatus.LEARNING_3)
        val think = saved("ຄິດວ່າ", TermStatus.NEW_1)
        books.insertBook(Book(languageId = id, title = "Home"), listOf(NewPage("ລາວໄປຕາມທາງ", 3)))

        val words = listOf("ລາວ", "ໄປ", "ຕາ", "ຕາມ", "ທາງ", "ໜັງສື", "ຄິດ", "ວ່າ", "ຄິດວ່າ")
        val migration = CutWordsMigration(languages, terms, books) { Lexicons.set("lo", Lexicon(words)) }

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 3, merged = 0)), migration.migrate())
        assertEquals(Triple("ຕາມ", 1, TermStatus.NEW_2), terms.getById(along)!!.let { Triple(it.text, it.tokenCount, it.status) })
        assertEquals("ຕາ", terms.getById(eye)!!.text)
        assertEquals(Pair("ໜັງສື", "ໜັງສື"), terms.getById(book)!!.let { it.text to it.textLc })
        assertEquals(Pair("ຄິດ​ວ່າ", 2), terms.getById(think)!!.let { it.text to it.tokenCount })
        assertEquals(1, terms.references(id, "ຕາມ", includeUnread = true).size, "the sentences are cut as the words are")

        assertEquals(emptyMap(), migration.migrate(), "done once")
    }
}
