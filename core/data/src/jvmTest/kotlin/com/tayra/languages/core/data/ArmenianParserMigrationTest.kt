package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.domain.language.LanguageDefinitions
import com.tayra.languages.core.domain.model.Language
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

/** An Armenian added when ՞ ՛ ՜ cut words and ended sentences takes the settings a new one has. */
class ArmenianParserMigrationTest {

    private val file: File = File.createTempFile("tayra-armenian", ".db").also { it.delete() }

    @Test
    fun armenianIsReadWithItsOwnParser() = runTest {
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val armenian = first.save(
            Language(
                name = "Armenian",
                parserType = "spacedel",
                regexpSplitSentences = "։՜՛՞",
                wordCharacters = "\\u0531-\\u0556\\u0561-\\u0588",
            ),
        )
        val other = first.save(Language(name = "Georgian", parserType = "spacedel", regexpSplitSentences = ".!?"))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(41) }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val migrated = reopened.getById(armenian)
        val current = LanguageDefinitions.all.first { it.name == "Armenian" }.language
        assertEquals(current.parserType, migrated?.parserType)
        assertEquals(current.wordCharacters, migrated?.wordCharacters)
        assertEquals(current.regexpSplitSentences, migrated?.regexpSplitSentences)
        assertEquals("spacedel", reopened.getById(other)?.parserType)
    }

    @Test
    fun settingsChangedByHandStay() = runTest {
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val armenian = first.save(Language(name = "Armenian", parserType = "spacedel", regexpSplitSentences = "։?!", wordCharacters = "\\u0531-\\u0587"))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(41) }

        val migrated = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file))).getById(armenian)
        assertEquals("armenian", migrated?.parserType)
        assertEquals("\\u0531-\\u0587", migrated?.wordCharacters)
        assertEquals("։?!", migrated?.regexpSplitSentences)
    }
}
