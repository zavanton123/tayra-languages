package com.idiomiko.core.data

import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.BookRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.domain.language.LanguageDefinitions
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.Term
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.model.ZWS_STRING
import com.idiomiko.core.domain.service.CutWordsMigration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A Manx added when it was read as space-delimited text is read with the Manx parser, and its saved words as it cuts them. */
class ManxParserMigrationTest {

    private val file: File = File.createTempFile("idiomiko-manx", ".db").also { it.delete() }

    @Test
    fun manxIsReadWithItsOwnParser() = runTest {
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val manx = first.save(Language(name = "Manx", parserType = "spacedel", wordCharacters = "a-zA-ZÀ-ÖØ-öø-ÿĀ-ſ'"))
        val other = first.save(Language(name = "Cornish", parserType = "spacedel"))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(45) }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val current = LanguageDefinitions.all.first { it.name == "Manx" }.language
        assertEquals(current.parserType, reopened.getById(manx)?.parserType)
        assertEquals("spacedel", reopened.getById(other)?.parserType)
    }

    @Test
    fun aWordWrittenOntoAnotherBecomesTheTwoAndAnEdgesApostropheGoes() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Manx" }.language)
        val isHe = terms.save(Term(languageId = id, text = "t'eh", textLc = "t'eh", status = TermStatus.NEW_2, tokenCount = 1))
        val isThe = terms.save(Term(languageId = id, text = "Ta'n", textLc = "ta'n", status = TermStatus.NEW_1, tokenCount = 1))
        // 'sy was saved with its apostrophe, and sy alone too.
        val quoted = terms.save(Term(languageId = id, text = "'sy", textLc = "'sy", status = TermStatus.LEARNING_4, tokenCount = 1))
        val inThe = terms.save(Term(languageId = id, text = "sy", textLc = "sy", status = TermStatus.NEW_1, tokenCount = 1))
        val good = terms.save(Term(languageId = id, text = "mie", textLc = "mie", status = TermStatus.WELL_KNOWN, tokenCount = 1))
        val migration = CutWordsMigration(languages, terms, BookRepositoryImpl(provider)) {}

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 2, merged = 1)), migration.migrate())
        assertEquals(Triple("t'${ZWS_STRING}eh", "t'${ZWS_STRING}eh", 2), terms.getById(isHe)!!.let { Triple(it.text, it.textLc, it.tokenCount) })
        assertEquals(Triple("Ta$ZWS_STRING'n", "ta$ZWS_STRING'n", 2), terms.getById(isThe)!!.let { Triple(it.text, it.textLc, it.tokenCount) })
        assertNull(terms.getById(quoted))
        assertEquals(TermStatus.LEARNING_4, terms.getById(inThe)!!.status)
        assertEquals("mie", terms.getById(good)!!.text)
        assertEquals(emptyMap(), migration.migrate(), "done once")
    }
}
