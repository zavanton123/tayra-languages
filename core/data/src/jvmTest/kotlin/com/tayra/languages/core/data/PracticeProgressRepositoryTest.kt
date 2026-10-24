package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.PracticeProgressRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.practice.WordProgress
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** A word's practice progress is kept, moved by each answer, and goes with the word. */
class PracticeProgressRepositoryTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-progress", ".db").also { it.delete() }))
    private val terms = TermRepositoryImpl(provider)
    private val progress = PracticeProgressRepositoryImpl(provider)

    @Test
    fun answersMoveAndKeepTheProgress() = runBlocking {
        val language = LanguageRepositoryImpl(provider).save(Language(name = "Portuguese"))
        val word = terms.save(Term(languageId = language, text = "moro", textLc = "moro", status = TermStatus.NEW_1))
        assertEquals(emptyMap(), progress.progress(listOf(word)))
        progress.record(word, WordProgress.Answer.RIGHT, Instant.fromEpochMilliseconds(10))
        progress.record(word, WordProgress.Answer.RIGHT, Instant.fromEpochMilliseconds(20))
        assertEquals(WordProgress(word, level = 1, streak = 0, asked = 2, right = 2, lastPractised = Instant.fromEpochMilliseconds(20)), progress.progress(listOf(word))[word])
        terms.delete(word)
        assertEquals(emptyMap(), progress.progress(listOf(word)), "deleting the word deletes its progress")
    }
}
