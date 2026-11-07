package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.language.LanguageDefinitions
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.parse.Lexicon
import com.tayra.languages.core.domain.parse.Lexicons
import com.tayra.languages.core.domain.repository.NewPage
import com.tayra.languages.core.domain.repository.TermListFilter
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.service.CutWordsMigration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A Hebrew added when it was read as space-delimited text is read with the Hebrew parser, its words parted by the maqaf,
 * and the words saved then whole (והוא, בבית) become the words the reader now cuts them into.
 */
class HebrewParserMigrationTest {

    private val file: File = File.createTempFile("tayra-hebrew", ".db").also { it.delete() }

    @AfterTest
    fun forget() = Lexicons.set("he", null)

    @Test
    fun hebrewIsReadWithItsOwnParser() = runTest {
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val old = "\\u05B0-\\u05BF\\u05C1\\u05C2\\u05C7\\u05D0-\\u05EA׳״"
        val hebrew = first.save(Language(name = "Hebrew", parserType = "spacedel", wordCharacters = old))
        val yiddish = first.save(Language(name = "Yiddish", parserType = "spacedel", wordCharacters = old))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(40) }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val now = LanguageDefinitions.all.first { it.name == "Hebrew" }.language
        assertEquals("hebrew" to now.wordCharacters, reopened.getById(hebrew)?.let { it.parserType to it.wordCharacters })
        assertEquals("spacedel" to old, reopened.getById(yiddish)?.let { it.parserType to it.wordCharacters })
    }

    @Test
    fun wordsSavedWholeBecomeTheWordsTheyAreCutInto() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Hebrew" }.language)
        // As the words were saved before: whole, matched with their points and apostrophes.
        suspend fun saved(text: String, status: TermStatus, translation: String? = null, tokens: Int = 1) =
            terms.save(Term(languageId = id, text = text, textLc = text, status = status, translation = translation, tokenCount = tokens))
        saved("והוא", TermStatus.NEW_1, "и он")
        val he = saved("הוא", TermStatus.UNKNOWN)
        saved("בבית", TermStatus.UNKNOWN)
        val house = saved("בית", TermStatus.LEARNING_3, "house")
        val points = saved("שָׁלוֹם", TermStatus.NEW_2)
        val jeans = saved("ג'ינס", TermStatus.NEW_1)
        val went = saved("הלך", TermStatus.NEW_1)
        val phrase = saved("בית​ ​הספר", TermStatus.NEW_1, "school", tokens = 3)
        books.insertBook(Book(languageId = id, title = "Home"), listOf(NewPage("והוא הלך לבית הספר.", 5)))

        val ranks = mapOf("ה" to 1, "ב" to 2, "ל" to 3, "ו" to 4, "הוא" to 14, "בית" to 23, "ספר" to 68, "הלך" to 87, "שלום" to 187, "ג׳ינס" to 5141)
        val migration = CutWordsMigration(languages, terms, books) { Lexicons.set("he", Lexicon(ranks.keys, ranks)) }
        val change = migration.migrate()

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 3, merged = 2)), change)
        val heNow = terms.getById(he)!!
        assertEquals(TermStatus.NEW_1 to "и он", heNow.status to heNow.translation, "והוא was הוא, and the status it had wins")
        assertNull(terms.findByTextLc(id, "והוא"))
        assertEquals(TermStatus.LEARNING_3, terms.getById(house)!!.status, "בבית adds nothing to בית")
        assertEquals("שָׁלוֹם" to "שלום", terms.getById(points)!!.let { it.text to it.textLc }, "matched without its points, written as saved")
        assertEquals("ג׳ינס" to "ג׳ינס", terms.getById(jeans)!!.let { it.text to it.textLc }, "written with the geresh")
        assertEquals("הלך", terms.getById(went)!!.text, "a word of the list stays whole")
        assertEquals("בית​ ​ה​ספר" to 4, terms.getById(phrase)!!.let { it.text to it.tokenCount })
        assertEquals(6, terms.list(TermListFilter(languageId = id), TermListSort(), 0, 100).totalCount)
        assertEquals(1, terms.references(id, "ספר", includeUnread = true).size, "the sentences are cut as the words are")

        assertEquals(emptyMap(), migration.migrate(), "done once")
    }
}
