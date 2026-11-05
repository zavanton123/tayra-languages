package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.PracticeExampleRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.book.SentenceBuilder
import com.tayra.languages.core.domain.flashcards.CardSentences
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.language.LanguageDefinitions
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.practice.PracticeExampleStore
import com.tayra.languages.core.domain.practice.StoredExample
import com.tayra.languages.core.domain.practice.WordForms
import com.tayra.languages.core.domain.repository.NewPage
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/** A word saved without a sentence gets one from what was read, or else from the examples, as a card needs one. */
class CardSentencesTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-sentences", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val cards = FlashcardRepositoryImpl(provider)
    private val repository = PracticeExampleRepositoryImpl(provider)
    private val lists = object : FrequencyLists {
        override suspend fun list(languageCode: String) = FrequencyList.parse("esquecer\tesquecer esqueci")
    }
    private val found = mutableListOf<ExampleSentence>()
    private val examples = object : ExampleSentencesProvider {
        override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult(found.toList(), found.size, null)
        override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
    }
    private val forms = WordForms(lists, terms)
    private val sentences = CardSentences(terms, languages, forms, PracticeExampleStore(repository, examples, forms, terms, languages, SettingsRepositoryImpl(MapSettings())))

    private suspend fun portuguese() = languages.save(LanguageDefinitions.all.first { it.name == "Portuguese" }.language)

    private suspend fun word(language: Long, text: String, status: TermStatus = TermStatus.NEW_1, sentence: String? = null) =
        terms.save(Term(languageId = language, text = text, textLc = text, status = status, sentence = sentence))

    private suspend fun page(language: Long, text: String, read: Boolean = true) {
        val book = books.insertBook(Book(languageId = language, title = text), listOf(NewPage(text, 0)))
        val page = books.getPage(book, 1)!!
        books.replaceSentences(page.id, SentenceBuilder.build(text, languages.getById(language)!!))
        if (read) books.setPageReadDate(page.id, Clock.System.now())
    }

    @Test
    fun aWordTakesASentenceFromAPageAlreadyRead() = runBlocking {
        val pt = portuguese()
        val lobo = word(pt, "lobo")
        page(pt, "O lobo dorme na floresta.")
        found += ExampleSentence("Um lobo uivou ontem à noite.", "A wolf howled.")

        assertTrue(sentences.fill(lobo))

        val term = terms.getById(lobo)!!
        assertEquals("O lobo dorme na floresta.", term.sentence)
        assertNull(term.cardForm, "the word stands in it as saved")
        assertNull(term.sentenceTranslation, "made when the card asks for it")
    }

    @Test
    fun anotherOfTheWordsFormsComesWithItsSentence() = runBlocking {
        val pt = portuguese()
        val esquecer = word(pt, "esquecer")
        page(pt, "Eu esqueci tudo ontem.")

        assertTrue(sentences.fill(esquecer))

        val term = terms.getById(esquecer)!!
        assertEquals("Eu esqueci tudo ontem.", term.sentence)
        assertEquals("esqueci", term.cardForm)
    }

    @Test
    fun aPageNotReadOrOnlyATitleGivesNoSentenceAndAnExampleIsTaken() = runBlocking {
        val pt = portuguese()
        val lobo = word(pt, "lobo")
        page(pt, "O lobo dorme na floresta.", read = false)
        page(pt, "Lobo.")
        found += ExampleSentence("Ele não viu nada ontem.", "Not the word.")
        found += ExampleSentence("Um lobo uivou ontem à noite.", "A wolf howled.")

        assertTrue(sentences.fill(lobo))

        val term = terms.getById(lobo)!!
        assertEquals("Um lobo uivou ontem à noite.", term.sentence)
        assertEquals("A wolf howled.", term.sentenceTranslation)
    }

    @Test
    fun anExampleKeptForTheWordIsUsedWithoutAskingAgain() = runBlocking {
        val pt = portuguese()
        val relatar = word(pt, "relatar", TermStatus.WELL_KNOWN)
        repository.add(listOf(StoredExample(relatar, "Muito me entristece relatar isto.", "Sad.", "relatar", 20, PracticeExampleStore.SOURCE_TATOEBA)), Clock.System.now())

        assertTrue(sentences.fill(relatar), "a word already known still has its card")

        assertEquals("Muito me entristece relatar isto.", terms.getById(relatar)?.sentence)
    }

    @Test
    fun aWordWithNoSentenceAnywhereWaitsAndOneWithASentenceKeepsIt() = runBlocking {
        val pt = portuguese()
        val lobo = word(pt, "lobo")
        val gato = word(pt, "gato", sentence = "O gato mia.")
        page(pt, "O gato dorme na cama.")

        assertFalse(sentences.fill(lobo))
        assertFalse(sentences.fill(gato))

        assertNull(terms.getById(lobo)?.sentence)
        assertEquals("O gato mia.", terms.getById(gato)?.sentence)
    }

    @Test
    fun theWordsToFindSentencesForAreThoseWithOrComingToACard() = runBlocking {
        val pt = portuguese()
        val learning = word(pt, "lobo")
        val withSentence = word(pt, "gato", sentence = "O gato mia.")
        val known = word(pt, "casa", TermStatus.WELL_KNOWN)
        val parent = word(pt, "rio", sentence = "O rio corre.")
        val form = word(pt, "rios")
        terms.setParents(form, listOf(parent))
        cards.reconcile(Clock.System.now())
        val keptCard = word(pt, "mar", TermStatus.NEW_1, sentence = "O mar é azul.")
        cards.reconcile(Clock.System.now())
        terms.updateSentence(keptCard, null)
        terms.updateStatus(listOf(keptCard), TermStatus.WELL_KNOWN)

        assertEquals(setOf(learning, keptCard), terms.needingSentence().toSet(), "not $withSentence, $known or the form $form its parent's card teaches")
    }
}
