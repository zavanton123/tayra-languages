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

/**
 * Lao came to be cut between syllables only (ຕາມ, no longer ຕາ "eye" + ມ) and read in today's spelling (ຫນັງສື as ໜັງສື): a
 * phrase saved over the old cut is cut again, a word saved in the old spelling is matched in the new one, and a word saved
 * that the reader now reads as several (ຄິດວ່າ, ຄິດ + ວ່າ) becomes a phrase of them.
 */
class LaoParserMigrationTest {

    private val file: File = File.createTempFile("idiomiko-lao", ".db").also { it.delete() }

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

    /**
     * The reader came to read ຕອນ "the time of" apart from the word after it, and a phrase only the app's own word list has
     * (ຮັງນົກ "bird's nest") as the list's words in it: a word saved so becomes a phrase of them; ຢູ່ໃນ and ໄຂ່ມຸກ stay.
     */
    @Test
    fun wordsTheReaderNowCutsBecomePhrases() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Lao" }.language)
        suspend fun saved(text: String, status: TermStatus) =
            terms.save(Term(languageId = id, text = text, textLc = text, status = status, tokenCount = 1))
        val morning = saved("ຕອນເຊົ້າ", TermStatus.NEW_1)
        val nest = saved("ຮັງນົກ", TermStatus.LEARNING_4)
        val inside = saved("ຢູ່ໃນ", TermStatus.LEARNING_3)
        val pearl = saved("ໄຂ່ມຸກ", TermStatus.NEW_2)

        val ranks = mapOf("ຕອນ" to 208, "ເຊົ້າ" to 1500, "ຕອນເຊົ້າ" to 2981, "ຮັງ" to 2020, "ນົກ" to 1122, "ຢູ່" to 19, "ໃນ" to 4, "ຢູ່ໃນ" to 61, "ໄຂ່" to 1200, "ມຸກ" to 3000, "ໄຂ່ມຸກ" to 2645)
        val migration = CutWordsMigration(languages, terms, books) { Lexicons.set("lo", Lexicon(ranks.keys + "ຮັງນົກ", ranks, ownOnly = listOf("ຮັງນົກ"))) }

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 2, merged = 0)), migration.migrate())
        assertEquals(Pair("ຕອນ\u200Bເຊົ້າ", 2), terms.getById(morning)!!.let { it.text to it.tokenCount })
        assertEquals(Triple("ຮັງ\u200Bນົກ", 2, TermStatus.LEARNING_4), terms.getById(nest)!!.let { Triple(it.text, it.tokenCount, it.status) })
        assertEquals("ຢູ່ໃນ", terms.getById(inside)!!.text)
        assertEquals("ໄຂ່ມຸກ", terms.getById(pearl)!!.text)
        assertEquals(emptyMap(), migration.migrate(), "done once")
    }
}
