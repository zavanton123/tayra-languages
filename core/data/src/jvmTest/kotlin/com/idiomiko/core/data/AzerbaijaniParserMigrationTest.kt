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

/**
 * An Azerbaijani added when it was read as space-delimited text is read with its own parser, and the words saved then
 * lowercased as other languages lowercase (İndi as i̇ndi, Işıq as işıq) are matched as Azerbaijani lowercases them.
 */
class AzerbaijaniParserMigrationTest {

    private val file: File = File.createTempFile("idiomiko-azerbaijani", ".db").also { it.delete() }

    @Test
    fun azerbaijaniIsReadWithItsOwnParser() = runTest {
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val azerbaijani = first.save(Language(name = "Azerbaijani", parserType = "spacedel"))
        val other = first.save(Language(name = "Uzbek", parserType = "spacedel"))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(47) }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val current = LanguageDefinitions.all.first { it.name == "Azerbaijani" }.language
        assertEquals(current.parserType, reopened.getById(azerbaijani)?.parserType)
        assertEquals("spacedel", reopened.getById(other)?.parserType)
    }

    @Test
    fun wordsSavedWithTheOtherIAreMatchedAsAzerbaijaniLowercasesThem() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Azerbaijani" }.language)
        // As the words were saved before: lowercased as other languages lowercase.
        suspend fun saved(text: String, status: TermStatus, translation: String? = null) =
            terms.save(Term(languageId = id, text = text, textLc = text.lowercase(), status = status, translation = translation, tokenCount = 1))
        val now = saved("indi", TermStatus.NEW_1)
        saved("İndi", TermStatus.LEARNING_3, "now")
        val light = saved("Işıq", TermStatus.NEW_2, "light")
        val house = saved("ev", TermStatus.WELL_KNOWN)

        val migration = CutWordsMigration(languages, terms, BookRepositoryImpl(provider)) {}

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 1, merged = 1)), migration.migrate())
        assertEquals(TermStatus.LEARNING_3 to "now", terms.getById(now)!!.let { it.status to it.translation }, "İndi was indi")
        assertNull(terms.findByTextLc(id, "i̇ndi"))
        assertEquals("ışıq", terms.getById(light)!!.textLc)
        assertEquals("ev", terms.getById(house)!!.textLc)
        assertEquals(emptyMap(), migration.migrate(), "done once")
    }
}
