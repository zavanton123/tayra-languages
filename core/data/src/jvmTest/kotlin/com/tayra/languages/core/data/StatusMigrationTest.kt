package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

/** Status 5 no longer exists: databases that still have it count those terms as well known. */
class StatusMigrationTest {

    @Test
    fun learnedTermsBecomeWellKnown() = runTest {
        val file = File.createTempFile("tayra-status", ".db").also { it.delete() }
        val first = DatabaseProvider(DatabaseDriverFactory(file))
        val languageId = LanguageRepositoryImpl(first).save(Language(name = "Portuguese"))
        val terms = TermRepositoryImpl(first)
        val learned = terms.save(Term(languageId = languageId, text = "tempo", textLc = "tempo", status = TermStatus.LEARNING_4))
        val learning = terms.save(Term(languageId = languageId, text = "maldição", textLc = "maldição", status = TermStatus.NEW_2))
        // A database from before the change, with one term still at status 5.
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use {
            it.createStatement().execute("UPDATE terms SET status = 5 WHERE id = $learned")
            it.createStatement().execute("PRAGMA user_version = 6")
        }

        val reopened = TermRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(file)))
        assertEquals(TermStatus.WELL_KNOWN, reopened.getById(learned)?.status)
        assertEquals(TermStatus.NEW_2, reopened.getById(learning)?.status)
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            assertEquals(99, c.createStatement().executeQuery("SELECT status FROM terms WHERE id = $learned").let { it.next(); it.getInt(1) })
        }
    }

    @Test
    fun aStatusFiveFromOutsideIsReadAsWellKnown() {
        assertEquals(TermStatus.WELL_KNOWN, TermStatus.fromValue(5))
        assertEquals(listOf("1", "2", "3", "4", "K", "I"), TermStatus.selectable.map { it.abbreviation })
        assertEquals(listOf("U", "1", "2", "3", "4", "K", "I"), TermStatus.paneButtons.map { it.abbreviation })
    }
}
