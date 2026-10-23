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
 * An Arabic added when it was read as space-delimited text is read with the Arabic parser, and the words saved then
 * whole (وهو, والكتاب) become the words the reader now cuts them into.
 */
class ArabicParserMigrationTest {

    private val file: File = File.createTempFile("tayra-arabic", ".db").also { it.delete() }

    @AfterTest
    fun forget() = Lexicons.set("ar", null)

    @Test
    fun arabicIsReadWithItsOwnParser() = runTest {
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val arabic = first.save(Language(name = "Arabic", parserType = "spacedel"))
        val egyptian = first.save(Language(name = "Egyptian Arabic", parserType = "spacedel"))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.createStatement().execute("PRAGMA user_version = 24") }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        assertEquals("arabic", reopened.getById(arabic)?.parserType)
        assertEquals("spacedel", reopened.getById(egyptian)?.parserType)
    }

    @Test
    fun wordsSavedWholeBecomeTheWordsTheyAreCutInto() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Arabic" }.language)
        // As the words were saved before: whole, matched with their vowel signs.
        suspend fun saved(text: String, status: TermStatus, translation: String? = null, tokens: Int = 1) =
            terms.save(Term(languageId = id, text = text, textLc = text, status = status, translation = translation, tokenCount = tokens))
        saved("وهو", TermStatus.NEW_1, "он")
        val he = saved("هو", TermStatus.UNKNOWN)
        saved("والكتاب", TermStatus.UNKNOWN)
        val book = saved("كتاب", TermStatus.LEARNING_3, "book")
        val also = saved("أيضًا", TermStatus.NEW_2)
        saved("وبيوتهم", TermStatus.UNKNOWN)
        val phrase = saved("في​ ​البيت", TermStatus.NEW_1, "at home", tokens = 3)
        books.insertBook(Book(languageId = id, title = "Home"), listOf(NewPage("وهو في البيت مع الكتاب.", 5)))

        val ranks = mapOf("ال" to 1, "و" to 2, "في" to 3, "مع" to 20, "هو" to 18, "كتاب" to 164, "بيت" to 198, "أيضا" to 51)
        val migration = CutWordsMigration(languages, terms, books) { Lexicons.set("ar", Lexicon(ranks.keys + "بيوت", ranks)) }
        val change = migration.migrate()

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 3, merged = 2)), change)
        val heNow = terms.getById(he)!!
        assertEquals(TermStatus.NEW_1 to "он", heNow.status to heNow.translation, "وهو was هو, and the status it had wins")
        assertNull(terms.findByTextLc(id, "وهو"))
        assertEquals(TermStatus.LEARNING_3, terms.getById(book)!!.status, "والكتاب adds nothing to كتاب")
        assertEquals("أيضًا" to "أيضا", terms.getById(also)!!.let { it.text to it.textLc }, "matched without its vowels, written as saved")
        assertEquals("بيوتهم", terms.findByTextLc(id, "بيوتهم")?.text, "the pronoun stays on the word")
        assertEquals("في​ ​ال​بيت" to 4, terms.getById(phrase)!!.let { it.text to it.tokenCount })
        assertEquals(5, terms.list(TermListFilter(languageId = id), TermListSort(), 0, 100).totalCount)
        assertEquals(1, terms.references(id, "بيت", includeUnread = true).size, "the sentences are cut as the words are")

        assertEquals(emptyMap(), migration.migrate(), "done once")
    }

    @Test
    fun aVerbSavedWithTheFutureOnItsAlifBecomesTheVerb() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Arabic" }.language)
        // سآخذ "I will take" was saved whole before the future was cut off a verb's آ; لست stays the word of the list it is.
        val take = terms.save(Term(languageId = id, text = "سآخذ", textLc = "سآخذ", status = TermStatus.NEW_2, tokenCount = 1))
        val notI = terms.save(Term(languageId = id, text = "لست", textLc = "لست", status = TermStatus.NEW_1, tokenCount = 1))
        val ranks = mapOf("ال" to 1, "و" to 2, "ل" to 7, "س" to 41, "ليس" to 60, "لست" to 60, "أخذ" to 192, "آخذ" to 192, "ست" to 670)
        val migration = CutWordsMigration(languages, terms, BookRepositoryImpl(provider)) { Lexicons.set("ar", Lexicon(ranks.keys, ranks)) }

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 1, merged = 0)), migration.migrate())
        assertEquals("آخذ" to TermStatus.NEW_2, terms.getById(take)!!.let { it.text to it.status })
        assertEquals("لست", terms.getById(notI)!!.text)
    }

    @Test
    fun aWordWithAHamzaOnASeatBeforeItsPronounIsCut() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Arabic" }.language)
        // بأصدقائه "with his friends" was saved whole while the reader found no أصدقاء in أصدقائه; كلفانا "he charged us
        // two" stays whole, كلفا's with a pronoun, now that it is no ك + لفانا.
        val friends = terms.save(Term(languageId = id, text = "بأصدقائه", textLc = "بأصدقائه", status = TermStatus.NEW_2, tokenCount = 1))
        val charged = terms.save(Term(languageId = id, text = "كلفانا", textLc = "كلفانا", status = TermStatus.NEW_1, tokenCount = 1))
        val ranks = mapOf("ال" to 1, "و" to 2, "ب" to 5, "ل" to 7, "ك" to 68, "صديق" to 298, "أصدقاء" to 298, "كلف" to 2277, "كلفا" to 2277)
        val migration = CutWordsMigration(languages, terms, BookRepositoryImpl(provider)) {
            Lexicons.set("ar", Lexicon(ranks.keys + "لفا", ranks, mapOf("صديق" to 298, "كلف" to 2277)))
        }

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 1, merged = 0)), migration.migrate())
        assertEquals("أصدقائه" to TermStatus.NEW_2, terms.getById(friends)!!.let { it.text to it.status })
        assertEquals("كلفانا", terms.getById(charged)!!.text)
    }

    @Test
    fun wordsTheReaderLeavesWholeStayAsSaved() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Arabic" }.language)
        val saved = listOf("برد", "لبن", "كلنا", "فقد", "فهم").map {
            terms.save(Term(languageId = id, text = it, textLc = it, status = TermStatus.NEW_1, tokenCount = 1))
        }
        // برد "cold" is a word of the list, no ب + رد; كلنا is كل with a pronoun; فقد and فهم alone are whole.
        val ranks = mapOf(
            "ال" to 1, "و" to 2, "ب" to 5, "ل" to 7, "لنا" to 7, "ف" to 21, "قد" to 28, "كل" to 19, "ك" to 36, "بن" to 82,
            "هم" to 123, "رد" to 479, "لبن" to 574, "فهم" to 521, "فقد" to 637, "برد" to 1942,
        )
        val heads = mapOf("كل" to 19, "برد" to 1942, "لبن" to 574)
        val migration = CutWordsMigration(languages, terms, BookRepositoryImpl(provider)) { Lexicons.set("ar", Lexicon(ranks.keys, ranks, heads)) }

        assertEquals(emptyMap(), migration.migrate())
        assertEquals(listOf("برد", "لبن", "كلنا", "فقد", "فهم"), saved.map { terms.getById(it)!!.text })
    }
}
