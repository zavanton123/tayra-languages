package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Cards made before they were dated take a date from their word: a card not yet answered from the word's last
 * status change, its move to learning, and an answered one, whose answers moved the status since, from the word's saving.
 */
class FlashcardDateMigrationTest {

    private fun <T> sql(file: File, block: Connection.() -> T): T = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use(block)
    private fun Connection.strings(query: String): List<String> = createStatement().executeQuery(query).use { r ->
        buildList { while (r.next()) add((1..r.metaData.columnCount).joinToString("|") { r.getString(it) ?: "" }) }
    }

    @Test
    fun cardsMadeBeforeAreDatedFromTheirWords() = runBlocking<Unit> {
        val file = File.createTempFile("tayra-card-dates", ".db").also { it.delete() }
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val pt = LanguageRepositoryImpl(provider).save(Language(name = "Portuguese"))
        val terms = TermRepositoryImpl(provider)
        val lobo = terms.save(Term(languageId = pt, text = "lobo", textLc = "lobo", status = TermStatus.NEW_1))
        val gato = terms.save(Term(languageId = pt, text = "gato", textLc = "gato", status = TermStatus.NEW_2))
        val changed = sql(file) {
            createStatement().execute("UPDATE terms SET created_at = 1000, status_changed_at = 2000 WHERE id = $lobo")
            createStatement().execute("UPDATE terms SET created_at = 3000, status_changed_at = 9000 WHERE id = $gato")
            createStatement().execute("INSERT INTO flashcards(term_id, due, seen_status) VALUES ($lobo, 1000, 1)")
            createStatement().execute("INSERT INTO flashcards(term_id, state, due, reps, last_review, seen_status) VALUES ($gato, 1, 9500, 1, 9000, 2)")
            createStatement().execute("INSERT INTO flashcard_reviews(term_id, reviewed_at, rating, state_before, interval_days, elapsed_days) VALUES ($gato, 9000, 3, 0, 0, 0)")
            pretendVersion(29)
            strings("SELECT updated_at FROM flashcards ORDER BY term_id")
        }

        DatabaseProvider(DatabaseDriverFactory(file)).database()
        sql(file) {
            assertEquals(listOf("lobo|2000", "gato|3000"), strings("SELECT t.text, f.created_at FROM flashcards f JOIN terms t ON t.id = f.term_id ORDER BY f.created_at"))
            assertEquals(changed, strings("SELECT updated_at FROM flashcards ORDER BY term_id"), "dating the cards is no change to sync")
        }
    }
}
