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
import com.idiomiko.core.domain.service.CutWordsMigration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A Maltese added when f'Malta was read as one word is read with the Maltese parser, and the words saved so are cut. */
class MalteseParserMigrationTest {

    private val file: File = File.createTempFile("idiomiko-maltese", ".db").also { it.delete() }

    @Test
    fun malteseIsReadWithItsOwnParser() = runTest {
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val maltese = first.save(Language(name = "Maltese", parserType = "spacedel"))
        val other = first.save(Language(name = "Manx", parserType = "spacedel"))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(46) }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        assertEquals(LanguageDefinitions.all.first { it.name == "Maltese" }.language.parserType, reopened.getById(maltese)?.parserType)
        assertEquals("maltese", reopened.getById(maltese)?.parserType)
        assertEquals("spacedel", reopened.getById(other)?.parserType)
    }

    @Test
    fun aWordSavedWithTheWordCutShortBeforeItIsTheWordAfter() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Maltese" }.language)
        suspend fun saved(text: String, status: TermStatus) =
            terms.save(Term(languageId = id, text = text, textLc = text.lowercase(), status = status, tokenCount = 1))
        val inMalta = saved("f'Malta", TermStatus.LEARNING_3)
        val malta = saved("Malta", TermStatus.NEW_1)
        val what = saved("x'inhu", TermStatus.NEW_2)
        val of = saved("ta'", TermStatus.WELL_KNOWN)
        val migration = CutWordsMigration(languages, terms, BookRepositoryImpl(provider)) {}

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 1, merged = 1)), migration.migrate())
        assertNull(terms.getById(inMalta))
        assertEquals(TermStatus.LEARNING_3, terms.getById(malta)!!.status, "the more advanced status wins")
        assertEquals("inhu" to "inhu", terms.getById(what)!!.let { it.text to it.textLc })
        assertEquals("ta'", terms.getById(of)!!.textLc)
        assertEquals(emptyMap(), migration.migrate(), "done once")
    }
}
