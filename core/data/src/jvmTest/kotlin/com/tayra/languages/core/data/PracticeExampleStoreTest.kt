package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.PracticeExampleRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.practice.PracticeExampleStore
import com.tayra.languages.core.domain.practice.WordForms
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.SentenceTranslator
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/** Sentences that use a word being learned, in any of its forms, are kept translated for its practice. */
class PracticeExampleStoreTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-store", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val repository = PracticeExampleRepositoryImpl(provider)
    private val lists = object : FrequencyLists {
        override suspend fun list(languageCode: String) = FrequencyList.parse("esquecer\tesquecer esqueci esqueça esqueço")
    }
    private val searched = mutableListOf<String>()
    private val examples = object : ExampleSentencesProvider {
        override suspend fun search(query: ExampleSearchQuery): ExampleSearchResult {
            searched += query.text
            return ExampleSearchResult(
                listOf(
                    ExampleSentence("Não se esqueça de dizer obrigado.", "Don't forget to say thank you."),
                    ExampleSentence("Eu esqueci minha carteira em casa.", "I forgot my wallet at home."),
                    ExampleSentence("Esqueci o que vim fazer aqui.", null),
                    ExampleSentence("Ele lembrou de tudo ontem.", "He remembered everything yesterday."),
                ),
                4, null,
            )
        }
        override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
    }
    private val translator = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = "<$text>" }

    private fun store() = PracticeExampleStore(repository, examples, WordForms(lists, terms), terms, languages, SettingsRepositoryImpl(MapSettings()), translator)

    @Test
    fun aWordBeingLearnedKeepsSentencesInAnyOfItsForms() = runBlocking {
        val language = languages.save(Language(name = "Portuguese"))
        val id = terms.save(Term(languageId = language, text = "esqueci", textLc = "esqueci", status = TermStatus.NEW_1, sentence = "Eu esqueci minha carteira em casa."))
        assertEquals(2, store().fill(id))
        val kept = repository.examples(listOf(id)).getValue(id)
        // Its own form first; the sentence it was read in, and one without the word, are left out.
        assertEquals(listOf("Esqueci o que vim fazer aqui.", "Não se esqueça de dizer obrigado."), kept.map { it.sentence })
        assertEquals(listOf("Esqueci", "esqueça"), kept.map { it.form })
        assertEquals("esqueça", kept[1].sentence.substring(kept[1].range))
        assertEquals(listOf("<Esqueci o que vim fazer aqui.>", "Don't forget to say thank you."), kept.map { it.translation }, "a missing translation is made")
    }

    @Test
    fun onlyWordsBeingLearnedAndShortOfExamplesAreLookedUp() = runBlocking {
        val language = languages.save(Language(name = "Portuguese"))
        val known = terms.save(Term(languageId = language, text = "esqueço", textLc = "esqueço", status = TermStatus.WELL_KNOWN))
        assertEquals(0, store().fill(known))
        val learning = terms.save(Term(languageId = language, text = "esqueci", textLc = "esqueci", status = TermStatus.NEW_1))
        store().topUp(listOf(known, learning), 5.seconds)
        assertEquals(listOf("esqueci"), searched)
        assertEquals(3, repository.counts(listOf(learning))[learning])
        // With three it has enough: no new search.
        store().topUp(listOf(learning), 5.seconds)
        assertEquals(listOf("esqueci"), searched)
    }
}
