package com.tayra.languages.feature.terms.examples

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.CorpusFrequency
import com.tayra.languages.core.domain.service.CorpusWords
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.ExampleSource
import com.tayra.languages.core.domain.service.ExampleSourceFeatures
import com.tayra.languages.core.domain.service.SentenceTranslation
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.YesNo
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

class ExamplesSearchViewModelTest {

    private val leipzig = ExampleSourceFeatures(
        ExampleSource.LEIPZIG, audio = false, sorts = listOf(ExampleSort.RANDOM, ExampleSort.SHORTEST), translations = false, phrases = false,
    )

    private class FakeExamples(private val sources: List<ExampleSourceFeatures>, private val results: (ExampleSearchQuery) -> List<ExampleSentence>) : ExampleSentencesProvider {
        val queries: MutableList<ExampleSearchQuery> = Collections.synchronizedList(mutableListOf())
        override suspend fun search(query: ExampleSearchQuery): ExampleSearchResult {
            queries += query
            val found = results(query)
            return ExampleSearchResult(found, found.size, null)
        }
        override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        override suspend fun sources(language: Language) = sources
    }

    private class FakeCorpus : CorpusWords {
        val asked: MutableList<String> = Collections.synchronizedList(mutableListOf())
        override suspend fun frequency(word: String, language: Language): CorpusFrequency? = null
        override suspend fun cooccurrences(word: String, language: Language, limit: Int): List<String> {
            asked += word
            return listOf("$word-near", "$word-far").take(limit)
        }
    }

    private class FakeTranslator : SentenceTranslator {
        val asked: MutableList<String> = Collections.synchronizedList(mutableListOf())
        override suspend fun translate(text: String, language: Language): String? {
            asked += text
            return if ("untranslatable" in text) null else "EN: $text"
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Default)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        examples: ExampleSentencesProvider,
        corpus: CorpusWords? = null,
        translator: SentenceTranslator? = null,
        text: String = "casa",
    ): ExamplesSearchViewModel {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-examples-vm", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val languageId = runBlocking { languages.save(Language(name = "Portuguese")) }
        return ExamplesSearchViewModel(languageId, text, languages, settings, examples, translator = translator, corpusWords = corpus)
    }

    private fun waitFor(what: String, condition: () -> Boolean) = runBlocking {
        withTimeout(5_000) { while (!condition()) delay(10) }
        assertTrue(condition(), what)
    }

    @Test
    fun switchingToLeipzigHidesWhatItLacksAndAllowsLongerSentences() {
        val examples = FakeExamples(listOf(ExampleSourceFeatures.TATOEBA, leipzig)) { listOf(ExampleSentence("Uma casa ${it.source}.", null)) }
        val corpus = FakeCorpus()
        val vm = viewModel(examples, corpus)
        waitFor("sources") { vm.state.value.sources.size == 2 && !vm.state.value.searching }
        assertEquals(ExampleSource.TATOEBA, examples.queries.single().source)
        assertEquals(6, examples.queries.single().maxWords)
        assertEquals(emptyList(), corpus.asked, "collocations come from the corpus, asked only for its searches")

        vm.updateQuery { it.copy(sort = ExampleSort.NEWEST, hasAudio = YesNo.YES) }
        vm.setSource(ExampleSource.LEIPZIG)
        waitFor("Leipzig results") { vm.state.value.results.singleOrNull()?.text == "Uma casa LEIPZIG." }
        val query = examples.queries.last()
        assertEquals(ExampleSource.LEIPZIG, query.source)
        assertEquals(15, query.maxWords)
        assertEquals(ExampleSort.RANDOM, query.sort, "a sort the source lacks gives way to its first")
        assertEquals(null, query.hasAudio)
        assertEquals(leipzig, vm.state.value.features)
        waitFor("collocations") { vm.state.value.collocations == listOf("casa-near", "casa-far") }

        vm.resetFilters()
        waitFor("reset") { examples.queries.size == 3 }
        assertEquals(15, examples.queries.last().maxWords)
        assertEquals(ExampleSource.LEIPZIG, examples.queries.last().source)

        vm.searchFor("casa-near")
        waitFor("collocation search") { examples.queries.last().text == "casa-near" && vm.state.value.collocations.firstOrNull() == "casa-near-near" }
        assertEquals(ExampleSource.LEIPZIG, examples.queries.last().source)

        vm.setSource(ExampleSource.TATOEBA)
        waitFor("back to Tatoeba") { examples.queries.last().source == ExampleSource.TATOEBA }
        assertEquals(6, examples.queries.last().maxWords)
        assertEquals(emptyList(), vm.state.value.collocations)
    }

    @Test
    fun aSourceThatIsNotOfferedCannotBeChosen() {
        val examples = FakeExamples(listOf(ExampleSourceFeatures.TATOEBA)) { emptyList() }
        val vm = viewModel(examples)
        waitFor("search") { examples.queries.size == 1 && !vm.state.value.searching }
        vm.setSource(ExampleSource.LEIPZIG)
        Thread.sleep(200)
        assertEquals(1, examples.queries.size)
        assertFalse(vm.state.value.offersLeipzig, "no Leipzig where it cannot be reached")
    }

    @Test
    fun anEmptyTatoebaSearchForAWordOffersLeipzig() {
        val examples = FakeExamples(listOf(ExampleSourceFeatures.TATOEBA, leipzig)) { if (it.source == ExampleSource.TATOEBA) emptyList() else listOf(ExampleSentence("x", null)) }
        val vm = viewModel(examples)
        waitFor("offer") { vm.state.value.offersLeipzig }
        vm.updateQuery { it.copy(text = "duas palavras") }
        vm.search()
        waitFor("phrase searched") { examples.queries.size == 2 && !vm.state.value.searching }
        assertFalse(vm.state.value.offersLeipzig, "Leipzig finds single words only")
    }

    @Test
    fun leipzigResultsAreTranslatedOnceEachAsTheyAreShown() {
        val examples = FakeExamples(listOf(ExampleSourceFeatures.TATOEBA, leipzig)) {
            if (it.source == ExampleSource.TATOEBA) listOf(ExampleSentence("Tem casa.", "Has a house."))
            else listOf(ExampleSentence("A casa é grande.", null), ExampleSentence("Casa untranslatable.", null))
        }
        val translator = FakeTranslator()
        val vm = viewModel(examples, translator = translator)
        waitFor("sources") { vm.state.value.sources.size == 2 && vm.state.value.results.isNotEmpty() }
        vm.translate("Tem casa.")
        Thread.sleep(100)
        assertEquals(emptyList(), translator.asked, "Tatoeba has translations of its own")

        vm.setSource(ExampleSource.LEIPZIG)
        waitFor("Leipzig results") { vm.state.value.results.size == 2 }
        vm.translate("A casa é grande.")
        vm.translate("A casa é grande.")
        vm.translate("Casa untranslatable.")
        waitFor("translated") { vm.state.value.translations.size == 2 && vm.state.value.translations.values.none { it == SentenceTranslation.Loading } }
        assertEquals("EN: A casa é grande.", (vm.state.value.translations["A casa é grande."] as SentenceTranslation.Done).text)
        assertEquals(SentenceTranslation.Unavailable, vm.state.value.translations["Casa untranslatable."])
        assertEquals(listOf("A casa é grande.", "Casa untranslatable."), translator.asked)

        // A new search keeps what was translated and tries the rest again.
        vm.search()
        waitFor("searched again") { !vm.state.value.searching && vm.state.value.results.size == 2 }
        assertEquals(setOf("A casa é grande."), vm.state.value.translations.keys)
    }
}
