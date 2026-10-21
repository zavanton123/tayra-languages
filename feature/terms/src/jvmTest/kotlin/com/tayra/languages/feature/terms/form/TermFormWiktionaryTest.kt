package com.tayra.languages.feature.terms.form

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.dictionary.OnlineWiktionary
import com.tayra.languages.core.domain.dictionary.WiktionaryEntry
import com.tayra.languages.core.domain.dictionary.WiktionaryForm
import com.tayra.languages.core.domain.dictionary.WiktionaryForms
import com.tayra.languages.core.domain.dictionary.WiktionaryLemma
import com.tayra.languages.core.domain.dictionary.WiktionaryPage
import com.tayra.languages.core.domain.dictionary.WiktionarySearchResult
import com.tayra.languages.core.domain.dictionary.WiktionarySense
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import java.io.File
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What the term form asks Wiktionary for a word it opens, and when its card shows. */
class TermFormWiktionaryTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        WiktionarySections.tableOpen = true
        WiktionarySections.expressionsOpen = true
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-term-wiktionary", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val portuguese = runBlocking { languages.save(Language(name = "Portuguese")) }

    private val comem = WiktionaryPage(
        "comem", "en", "pt",
        listOf(WiktionaryEntry("verb", listOf(WiktionarySense(listOf("third-person plural present indicative of comer"), formOf = listOf(WiktionaryLemma("comer", "comer")))))),
    )
    private val conjugation = listOf("first-person", "second-person", "third-person").flatMap { person ->
        listOf("singular", "plural").map { number -> WiktionaryForm("$person-$number", listOf(person, number, "present", "indicative"), "conjugation") } +
            WiktionaryForm("$person-past", listOf(person, "singular", "preterite", "indicative"), "conjugation")
    }
    private val muito = WiktionaryPage("muito", "en", "pt", listOf(WiktionaryEntry("adv", listOf(WiktionarySense(listOf("very"))))))

    /** Answers as WiktApi would, remembering every request it was asked. */
    private class FakeWiktionary(private val pages: List<WiktionaryPage>, private val forms: Map<String, List<WiktionaryForm>>, private val found: List<String>) : OnlineWiktionary {
        val asked: MutableList<String> = Collections.synchronizedList(mutableListOf())
        override suspend fun page(word: String, languageCode: String, nativeLanguage: String): WiktionaryPage? {
            asked += "page $word"
            return pages.firstOrNull { it.word == word }
        }
        override suspend fun forms(word: String, languageCode: String, edition: String): List<WiktionaryForms> {
            asked += "forms $word"
            return forms[word]?.let { listOf(WiktionaryForms("verb", it)) }.orEmpty()
        }
        override suspend fun search(prefix: String, languageCode: String, edition: String): List<WiktionarySearchResult> {
            asked += "search $prefix"
            return found.map { WiktionarySearchResult(it, "phrase") }
        }
    }

    private fun form(text: String, wiktionary: OnlineWiktionary): TermFormViewModel {
        val noPacks = object : DictionaryPackStore {
            override suspend fun installedSize(pack: DictionaryPack): Long? = null
            override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) = error("offline")
            override suspend fun remove(pack: DictionaryPack) {}
        }
        val noEntries = object : DictionaryRepository {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun entries(dictionary: DictionaryId, wordLc: String) = emptyList<DictionaryEntry>()
            override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
            override suspend fun close(dictionary: DictionaryId) {}
        }
        val noExamples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult.EMPTY
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        val dictionaries = DictionaryService(noPacks, noEntries)
        return TermFormViewModel(
            TermFormKey.ByText(portuguese, text), TermService(terms, languages), terms, languages, settings, engine, noExamples, dictionaries, dictionaries,
            wiktionary = wiktionary,
        )
    }

    private fun waitFor(what: String, condition: () -> Boolean) = runBlocking {
        withTimeout(5_000) { while (!condition()) delay(10) }
        assertTrue(condition(), what)
    }

    @Test
    fun aFormAsksForItsPageThenItsLemmasFormsAndExpressions() {
        val wiktionary = FakeWiktionary(listOf(comem), mapOf("comer" to conjugation), listOf("comer", "comer o chapéu"))
        val viewModel = form("comem", wiktionary)
        waitFor("the card is complete") { viewModel.state.value.wiktionary.complete }
        assertEquals(listOf("page comem", "forms comer", "search comer"), wiktionary.asked)
        val card = viewModel.state.value.wiktionary
        assertTrue(card.isShown)
        assertEquals("comer", card.tableWord)
        assertEquals(listOf("comer o chapéu"), card.expressions)
        assertTrue(card.tableOpen && card.expressionsOpen)
        // The online lemma becomes the parent of a word the offline dictionary lacks.
        waitFor("the parent is linked") { viewModel.state.value.draft.parents == listOf("comer") }
    }

    @Test
    fun aWordWithNeitherTableNorExpressionsHasNoCard() {
        val wiktionary = FakeWiktionary(listOf(muito), emptyMap(), listOf("muito", "muitos"))
        val viewModel = form("muito", wiktionary)
        waitFor("the card is complete") { viewModel.state.value.wiktionary.complete }
        // A word with no lemma and no table of its own asks for no forms.
        assertEquals(listOf("page muito", "search muito"), wiktionary.asked)
        assertFalse(viewModel.state.value.wiktionary.isShown)
    }

    @Test
    fun theSectionsStayAsTheyWereLeft() {
        val wiktionary = FakeWiktionary(listOf(comem), mapOf("comer" to conjugation), listOf("comer o chapéu"))
        val first = form("comem", wiktionary)
        waitFor("the card is complete") { first.state.value.wiktionary.complete }
        first.toggleTable()
        assertFalse(first.state.value.wiktionary.tableOpen)
        val next = form("comem", wiktionary)
        waitFor("the next card is complete") { next.state.value.wiktionary.complete }
        assertFalse(next.state.value.wiktionary.tableOpen)
        assertTrue(next.state.value.wiktionary.expressionsOpen)
    }
}
