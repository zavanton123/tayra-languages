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

/** An Amharic added when its words were cut at ፚ and the gemination marks takes every Ethiopic letter; a language of one's own keeps its own. */
class AmharicWordCharactersMigrationTest {

    @Test
    fun amharicWordsTakeEveryEthiopicLetter() = runTest {
        val file = File.createTempFile("idiomiko-amharic", ".db").also { it.delete() }
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val old = "\\u1200-\\u1359\\u1369-\\u137C"
        val amharic = first.save(Language(name = "Amharic", parserType = "spacedel", wordCharacters = old))
        val own = first.save(Language(name = "Mine", parserType = "spacedel", wordCharacters = old))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(49) }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val expected = LanguageDefinitions.all.first { it.name == "Amharic" }.language.wordCharacters
        assertEquals(expected, reopened.getById(amharic)?.wordCharacters)
        assertEquals(old, reopened.getById(own)?.wordCharacters)
    }
}
