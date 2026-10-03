package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A term keeps the sentence it was being read in while it is being learned (statuses 1 to 4). */
class LearningSentenceTest {

    private class Env {
        val file: File = File.createTempFile("tayra-sentence", ".db").also { it.delete() }
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val service = TermService(terms, languages)
    }

    @Test
    fun theSentenceFollowsTheStatus() = runTest {
        val env = Env()
        val language = env.languages.save(Language(name = "Portuguese"))
        suspend fun lobo() = env.terms.findByTextLc(language, "lobo")!!

        val id = env.service.save(env.service.findOrNew(language, "lobo").copy(status = TermStatus.NEW_1, statusExplicitlySet = true), sentence = "O lobo dorme.")
        assertEquals("O lobo dorme.", lobo().sentence)
        env.service.shiftStatus(listOf(id), 1)
        assertEquals(TermStatus.NEW_2, lobo().status)
        assertEquals("O lobo dorme.", lobo().sentence, "kept while the word goes on being learned")
        env.service.save(env.service.draftOf(lobo()).copy(translation = "wolf"))
        assertEquals("O lobo dorme.", lobo().sentence, "kept when other fields are edited")
        env.service.setStatus(listOf(id), TermStatus.NEW_1, mapOf(id to "Outro lobo."))
        assertEquals("O lobo dorme.", lobo().sentence, "the first sentence stays")
        env.service.setStatus(listOf(id), TermStatus.WELL_KNOWN)
        assertNull(lobo().sentence, "cleared once the word is known")
        env.service.setStatus(listOf(id), TermStatus.LEARNING_3, mapOf(id to "Outro lobo."))
        assertEquals("Outro lobo.", lobo().sentence, "stored again when learning starts again")
        env.service.setStatus(listOf(id), TermStatus.IGNORED)
        assertNull(lobo().sentence, "cleared once the word is ignored")
        env.service.save(env.service.draftOf(lobo()).copy(status = TermStatus.NEW_1, statusExplicitlySet = true))
        assertNull(lobo().sentence, "nothing to store when the word is not read in a text")
        env.service.save(env.service.draftOf(lobo()), sentence = "Lobo mau.")
        assertEquals("Lobo mau.", lobo().sentence, "a learning word without a sentence takes the one it is read in")
        env.service.setStatus(listOf(id), TermStatus.UNKNOWN)
        assertNull(lobo().sentence, "cleared once the word is unknown again")
    }

    @Test
    fun aParentFollowingItsChildSharesTheSentence() = runTest {
        val env = Env()
        val language = env.languages.save(Language(name = "Portuguese"))
        val draft = env.service.findOrNew(language, "lobos").copy(status = TermStatus.NEW_1, statusExplicitlySet = true, parents = listOf("lobo"), syncStatus = true)
        val child = env.service.save(draft, sentence = "Os lobos dormem.")
        val parent = env.terms.findByTextLc(language, "lobo")!!
        assertEquals(TermStatus.NEW_1, parent.status)
        assertEquals("Os lobos dormem.", parent.sentence)

        env.service.setStatus(listOf(child), TermStatus.WELL_KNOWN)
        assertEquals(TermStatus.WELL_KNOWN, env.terms.getById(parent.id)?.status)
        assertNull(env.terms.getById(parent.id)?.sentence)
        assertNull(env.terms.getById(child)?.sentence)
    }

    @Test
    fun olderDatabasesGetTheColumn() = runTest {
        val env = Env()
        val language = env.languages.save(Language(name = "Portuguese"))
        val id = env.terms.save(Term(languageId = language, text = "tempo", textLc = "tempo", status = TermStatus.NEW_2))
        // A database from before the sentence was stored.
        DriverManager.getConnection("jdbc:sqlite:${env.file.absolutePath}").use {
            it.createStatement().execute("ALTER TABLE terms DROP COLUMN sentence")
            it.createStatement().execute("PRAGMA user_version = 7")
        }

        val reopened = TermRepositoryImpl(DatabaseProvider(DatabaseDriverFactory(env.file)))
        val term = reopened.getById(id)!!
        assertNull(term.sentence)
        reopened.save(term.copy(sentence = "O tempo passa."))
        assertEquals("O tempo passa.", reopened.getById(id)?.sentence)
    }
}
