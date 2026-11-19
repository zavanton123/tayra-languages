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
 * An Ancient Greek added when it was read as space-delimited text is read with its own parser, and the words saved then
 * with a grave or an enclitic's second accent (καλὸς, ἄνθρωπός) are matched with the accent they have standing alone.
 */
class AncientGreekParserMigrationTest {

    private val file: File = File.createTempFile("idiomiko-ancient-greek", ".db").also { it.delete() }

    @Test
    fun ancientGreekIsReadWithItsOwnParser() = runTest {
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val ancient = first.save(Language(name = "Ancient Greek", parserType = "spacedel"))
        val modern = first.save(Language(name = "Greek", parserType = "spacedel"))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(44) }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val current = LanguageDefinitions.all.first { it.name == "Ancient Greek" }.language
        assertEquals(current.parserType, reopened.getById(ancient)?.parserType)
        assertEquals("spacedel", reopened.getById(modern)?.parserType)
    }

    @Test
    fun wordsSavedWithAGraveOrASecondAccentAreMatchedAsTheWord() = runBlocking<Unit> {
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val id = languages.save(LanguageDefinitions.all.first { it.name == "Ancient Greek" }.language)
        // As the words were saved before: lowercase only.
        suspend fun saved(text: String, status: TermStatus, translation: String? = null) =
            terms.save(Term(languageId = id, text = text, textLc = text.lowercase(), status = status, translation = translation, tokenCount = 1))
        val good = saved("καλός", TermStatus.NEW_1)
        saved("καλὸς", TermStatus.LEARNING_3, "good")
        val man = saved("ἄνθρωπός", TermStatus.NEW_2, "man")
        val article = saved("τῷ", TermStatus.WELL_KNOWN)

        val migration = CutWordsMigration(languages, terms, BookRepositoryImpl(provider)) {}

        assertEquals(mapOf(id to CutWordsMigration.Change(cut = 1, merged = 1)), migration.migrate())
        assertEquals(TermStatus.LEARNING_3 to "good", terms.getById(good)!!.let { it.status to it.translation }, "καλὸς was καλός")
        assertNull(terms.findByTextLc(id, "καλὸς"))
        assertEquals("ἄνθρωπος", terms.getById(man)!!.textLc)
        assertEquals("τῷ", terms.getById(article)!!.textLc)
        assertEquals(emptyMap(), migration.migrate(), "done once")
    }
}
