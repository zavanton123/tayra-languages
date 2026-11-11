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
 * Thai came to be cut by syllables (ต่อมา is ต่อ + มา, no longer ต่อม + า): a phrase saved over the old cut is cut
 * again, and the words saved stay as they are. Then grammatical words came to stand apart (ไม่ดี is ไม่ + ดี): a word
 * saved that the reader now reads as several becomes a phrase of them.
 */
class ThaiParserMigrationTest {

    private val file: File = File.createTempFile("tayra-thai", ".db").also { it.delete() }

    @AfterTest
    fun forget() = Lexicons.set("th", null)

    @Test
    fun phrasesSavedOverTheOldCutAreCutAgain() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Thai" }.language)
        suspend fun saved(text: String, status: TermStatus, tokens: Int = 1) =
            terms.save(Term(languageId = id, text = text, textLc = text, status = status, tokenCount = tokens))
        // ต่อมา "later" read as ต่อม + า, saved as the two together; ต่อม "gland" and the fragment มีน alone.
        val later = saved("ต่อม​า", TermStatus.NEW_2, tokens = 2)
        val gland = saved("ต่อม", TermStatus.NEW_1)
        val march = saved("มีน", TermStatus.IGNORED)
        books.insertBook(Book(languageId = id, title = "Home"), listOf(NewPage("ต่อมาเขากลับบ้าน", 4)))

        val words = listOf("ต่อ", "ต่อม", "มา", "เขา", "กลับ", "บ้าน", "มีน")
        val migration = CutWordsMigration(languages, terms, books) { Lexicons.set("th", Lexicon(words)) }

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 1, merged = 0)), migration.migrate())
        assertEquals(Triple("ต่อ​มา", 2, TermStatus.NEW_2), terms.getById(later)!!.let { Triple(it.text, it.tokenCount, it.status) })
        assertEquals(listOf("ต่อม", "มีน"), listOf(gland, march).map { terms.getById(it)!!.text })
        assertEquals(1, terms.references(id, "ต่อ​มา", includeUnread = true).size, "the sentences are cut as the words are")

        assertEquals(emptyMap(), migration.migrate(), "done once")
    }

    @Test
    fun wordsSavedThatTheReaderNowCutsBecomePhrases() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Thai" }.language)
        suspend fun saved(text: String, status: TermStatus) = terms.save(Term(languageId = id, text = text, textLc = text, status = status))
        val bad = saved("ไม่ดี", TermStatus.LEARNING_3)
        val think = saved("คิดว่า", TermStatus.NEW_1)
        // ไม่ + ดี saved as a phrase before: the word's status goes to it, as the more advanced.
        terms.save(Term(languageId = id, text = "ไม่​ดี", textLc = "ไม่​ดี", status = TermStatus.NEW_1, tokenCount = 2))
        val airport = saved("สนามบิน", TermStatus.NEW_2)
        val name = saved("สมศรี", TermStatus.IGNORED)
        books.insertBook(Book(languageId = id, title = "Home"), listOf(NewPage("ไม่ดี", 1)))

        val words = listOf("ไม่", "ดี", "ไม่ดี", "คิด", "ว่า", "คิดว่า", "สนาม", "บิน", "สนามบิน")
        val migration = CutWordsMigration(languages, terms, books) { Lexicons.set("th", Lexicon(words)) }

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 1, merged = 1)), migration.migrate())
        assertEquals(null, terms.getById(bad))
        assertEquals(TermStatus.LEARNING_3, terms.findByTextLc(id, "ไม่​ดี")!!.status)
        assertEquals(Triple("คิด​ว่า", 2, TermStatus.NEW_1), terms.getById(think)!!.let { Triple(it.text, it.tokenCount, it.status) })
        assertEquals(listOf("สนามบิน", "สมศรี"), listOf(airport, name).map { terms.getById(it)!!.text })
        assertEquals(emptyMap(), migration.migrate(), "done once")
    }
}
