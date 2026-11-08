package com.tayra.languages.feature.flashcards

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Over a text, the cards of its words only are gone over, in their order, and then the cards say they are done. */
class TextWordsFlashcardsTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-text-cards", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = FlashcardService(FlashcardRepositoryImpl(provider), terms, settings)

    private fun model(words: List<Long>): FlashcardsViewModel {
        val noDictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val translator = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null }
        val suggestions = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        return FlashcardsViewModel(service, languages, settings, translator, noDictionary, WordTranslationService(terms, noDictionary, suggestions, settings), textWords = words)
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "timed out waiting for $what" }
            Thread.sleep(10)
        }
    }

    private fun answerGood(viewModel: FlashcardsViewModel) {
        viewModel.reveal()
        waitFor("the answer") { viewModel.state.value.revealed }
        viewModel.answer(Rating.GOOD)
    }

    @Test
    fun onlyTheTextsWordsComeUpAndThenTheyAreDone() = runBlocking {
        val id = languages.save(Language(name = "Portuguese"))
        settings.update { it.copy(currentLanguageId = id) }
        fun save(word: String) = runBlocking { terms.save(Term(languageId = id, text = word, textLc = word, status = TermStatus.NEW_1, translation = "<$word>", sentence = "Uma frase com $word.")) }
        val comprar = save("comprar")
        val casa = save("casa")
        save("livro")
        val viewModel = model(listOf(casa, comprar))

        waitFor("the first card") { viewModel.state.value.card != null }
        assertEquals(casa, viewModel.state.value.card?.termId, "the text's order")
        answerGood(viewModel)
        waitFor("the second card") { viewModel.state.value.card?.termId == comprar }
        assertFalse(viewModel.state.value.textDone)
        answerGood(viewModel)
        waitFor("the end of the text's cards") { viewModel.state.value.textDone }
        assertNull(viewModel.state.value.card, "a word of another text does not follow")
        assertTrue(runBlocking { service.reviews(casa) }.isNotEmpty(), "the answers go into the schedule")
    }

    @Test
    fun aTextWithoutCardsIsDoneAtOnce() = runBlocking {
        val id = languages.save(Language(name = "Portuguese"))
        settings.update { it.copy(currentLanguageId = id) }
        val viewModel = model(listOf(12345L))
        waitFor("the cards to say they are done") { viewModel.state.value.textDone && !viewModel.state.value.loading }
        assertNull(viewModel.state.value.card)
    }
}
