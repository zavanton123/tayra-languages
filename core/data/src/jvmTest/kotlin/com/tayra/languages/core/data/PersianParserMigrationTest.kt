package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.language.LanguageDefinitions
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.CutWordsMigration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A Farsi added when its words were cut at the non-joiner is read with the Persian parser, its saved words matched folded. */
class PersianParserMigrationTest {

    private val file: File = File.createTempFile("tayra-persian", ".db").also { it.delete() }
    private val farsi = LanguageDefinitions.all.first { it.name == "Farsi" }.language
    private val oldCharacters = farsi.wordCharacters.removePrefix("\\u200C")

    @Test
    fun farsiIsReadWithItsOwnParser() = runTest {
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val old = first.save(Language(name = "Farsi", parserType = "spacedel", wordCharacters = oldCharacters))
        val own = first.save(Language(name = "Mine", parserType = "spacedel", wordCharacters = oldCharacters))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(37) }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        assertEquals("persian", reopened.getById(old)?.parserType)
        assertEquals(farsi.wordCharacters, reopened.getById(old)?.wordCharacters)
        assertEquals("spacedel", reopened.getById(own)?.parserType)
        assertEquals(oldCharacters, reopened.getById(own)?.wordCharacters)
    }

    @Test
    fun wordsSavedInArabicLettersBecomeThePersianWord() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val id = languages.save(farsi)
        suspend fun saved(text: String, status: TermStatus, translation: String? = null) =
            terms.save(Term(languageId = id, text = text, textLc = text, status = status, translation = translation, tokenCount = 1))
        saved("كتاب", TermStatus.NEW_1, "book")
        val book = saved("کتاب", TermStatus.UNKNOWN)
        val really = saved("واقعاً", TermStatus.NEW_2)
        val going = saved("روم", TermStatus.LEARNING_3)

        val migration = CutWordsMigration(languages, terms, BookRepositoryImpl(provider)) { }
        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 1, merged = 1)), migration.migrate())

        assertEquals(TermStatus.NEW_1 to "book", terms.getById(book)!!.let { it.status to it.translation })
        assertNull(terms.findByTextLc(id, "كتاب"))
        assertEquals("واقعاً" to "واقعا", terms.getById(really)!!.let { it.text to it.textLc }, "matched without its marks, written as saved")
        assertEquals("روم", terms.getById(going)!!.text)
        assertEquals(emptyMap(), migration.migrate(), "done once")
    }
}
