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

/** A Tibetan added before its word characters were mended gets them mended; a language of one's own keeps its own. */
class TibetanWordCharactersMigrationTest {

    @Test
    fun tibetanWordCharactersAreMended() = runTest {
        val file = File.createTempFile("tayra-tibetan", ".db").also { it.delete() }
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val broken = "u\\0F00\\u0F15-\\u0F39\\u0F3E-\\u0FFF"
        val tibetan = first.save(Language(name = "Tibetan", wordCharacters = broken))
        val own = first.save(Language(name = "Mine", wordCharacters = "a-z"))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(21) }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val expected = LanguageDefinitions.all.first { it.name == "Tibetan" }.language.wordCharacters
        assertEquals(expected, reopened.getById(tibetan)?.wordCharacters)
        assertEquals("a-z", reopened.getById(own)?.wordCharacters)
    }
}
