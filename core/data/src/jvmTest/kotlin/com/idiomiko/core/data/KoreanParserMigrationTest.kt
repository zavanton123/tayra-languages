package com.idiomiko.core.data

import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.domain.model.Language
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

/** A Korean added when it was read as space-delimited text is read with the Korean parser; other languages keep theirs. */
class KoreanParserMigrationTest {

    @Test
    fun koreanIsReadWithItsOwnParser() = runTest {
        val file = File.createTempFile("idiomiko-korean", ".db").also { it.delete() }
        val first = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        val korean = first.save(Language(name = "Korean", parserType = "spacedel"))
        val other = first.save(Language(name = "Mine", parserType = "spacedel"))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.pretendVersion(23) }

        val reopened = LanguageRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        assertEquals("korean", reopened.getById(korean)?.parserType)
        assertEquals("spacedel", reopened.getById(other)?.parserType)
    }
}
