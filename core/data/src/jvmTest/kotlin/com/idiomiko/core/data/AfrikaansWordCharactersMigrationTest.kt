package com.idiomiko.core.data

import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.domain.language.LanguageDefinitions
import com.idiomiko.core.domain.model.Language
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

/** An Afrikaans added when its words were cut at ná's á and appèl's è takes every accented letter; a language of one's own keeps its own. */
class AfrikaansWordCharactersMigrationTest {

    @Test
    fun afrikaansWordsTakeEveryAccentedLetter() = runTest {
        val file = File.createTempFile("idiomiko-afrikaans", ".db").also { it.delete() }
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val old = "a-zA-ZÉËÏÓÖÜéëïóöüÊêÔô'"
        val afrikaans = first.save(Language(name = "Afrikaans", parserType = "spacedel", wordCharacters = old))
        val own = first.save(Language(name = "Mine", parserType = "spacedel", wordCharacters = old))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(48) }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val expected = LanguageDefinitions.all.first { it.name == "Afrikaans" }.language.wordCharacters
        assertEquals(expected, reopened.getById(afrikaans)?.wordCharacters)
        assertEquals(old, reopened.getById(own)?.wordCharacters)
    }
}
