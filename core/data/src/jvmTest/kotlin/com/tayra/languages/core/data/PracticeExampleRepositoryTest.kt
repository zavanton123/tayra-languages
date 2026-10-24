package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.PracticeExampleRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.practice.StoredExample
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** Example sentences are kept per word, once each, and go with the word. */
class PracticeExampleRepositoryTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-examples", ".db").also { it.delete() }))
    private val terms = TermRepositoryImpl(provider)
    private val examples = PracticeExampleRepositoryImpl(provider)

    @Test
    fun examplesAreKeptOncePerWordAndGoWithIt() = runBlocking {
        val language = LanguageRepositoryImpl(provider).save(Language(name = "Portuguese"))
        val forget = terms.save(Term(languageId = language, text = "esqueci", textLc = "esqueci", status = TermStatus.NEW_1))
        val wallet = terms.save(Term(languageId = language, text = "carteira", textLc = "carteira", status = TermStatus.NEW_1))
        val first = StoredExample(forget, "Não se esqueça de dizer obrigado.", "Don't forget to say thank you.", "esqueça", 7, "tatoeba", "123")
        val second = StoredExample(forget, "Esqueci o que vim fazer aqui.", null, "Esqueci", 0, "course")
        examples.add(listOf(first, second, first.copy(translation = "again")), Instant.fromEpochMilliseconds(1))
        examples.add(listOf(StoredExample(wallet, "A carteira está em casa.", null, "carteira", 2, "tatoeba")), Instant.fromEpochMilliseconds(2))

        assertEquals(mapOf(forget to listOf(first, second), wallet to listOf(StoredExample(wallet, "A carteira está em casa.", null, "carteira", 2, "tatoeba"))), examples.examples(listOf(forget, wallet)))
        assertEquals(mapOf(forget to 2, wallet to 1), examples.counts(listOf(forget, wallet, 999)))
        assertEquals("esqueça", first.sentence.substring(first.range))

        examples.clear(wallet)
        assertEquals(setOf(forget), examples.counts(listOf(forget, wallet)).keys)
        terms.delete(forget)
        assertEquals(emptyMap(), examples.examples(listOf(forget)), "deleting the word deletes its examples")
    }
}
