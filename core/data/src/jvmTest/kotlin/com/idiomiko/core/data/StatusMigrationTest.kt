package com.idiomiko.core.data

import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.Term
import com.idiomiko.core.domain.model.TermStatus
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

/** Status 5 no longer exists: databases that still have it count those terms as well known. */
class StatusMigrationTest {

    @Test
    fun learnedTermsBecomeWellKnown() = runTest {
        val file = File.createTempFile("idiomiko-status", ".db").also { it.delete() }
        val first = DatabaseProvider(DatabaseDriverFactory(file))
        val languageId = LanguageRepositoryImpl(first).save(Language(name = "Portuguese"))
        val terms = TermRepositoryImpl(first)
        val learned = terms.save(Term(languageId = languageId, text = "tempo", textLc = "tempo", status = TermStatus.LEARNING_4))
        val learning = terms.save(Term(languageId = languageId, text = "maldição", textLc = "maldição", status = TermStatus.NEW_2))
        // A database from before the change, with one term still at status 5.
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use {
            it.createStatement().execute("UPDATE terms SET status = 5 WHERE id = $learned")
            // The newer schema goes first: its triggers name the columns dropped next.
            it.pretendVersion(6)
            it.createStatement().execute("ALTER TABLE terms DROP COLUMN sentence")
            it.createStatement().execute("ALTER TABLE terms DROP COLUMN anki_exported_at")
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
