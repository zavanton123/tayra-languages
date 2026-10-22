package com.tayra.languages.feature.terms.form

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.courses.CourseExamplesProvider
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.dictionary.OnlineWiktionary
import com.tayra.languages.core.domain.dictionary.WiktionaryEntry
import com.tayra.languages.core.domain.dictionary.WiktionaryForm
import com.tayra.languages.core.domain.dictionary.WiktionaryForms
import com.tayra.languages.core.domain.dictionary.WiktionaryPage
import com.tayra.languages.core.domain.dictionary.WiktionarySearchResult
import com.tayra.languages.core.domain.dictionary.WiktionarySense
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.ExampleSource
import com.tayra.languages.core.domain.service.ExampleSourceFeatures
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the term form shows when Tatoeba cannot be asked for examples. */
class TermFormExamplesTest {

    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Default)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-term-examples", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val portuguese = runBlocking { languages.save(Language(name = "Portuguese")) }

    private val tatoebaSentence = ExampleSentence("A casa é grande.", "The house is big.")
    private val courseSentence = ExampleSentence("Eles têm uma casa.", null)

    /** Tatoeba answers with [tatoeba]; the installed courses, when there are some, with their sentence. */
    private class FakeExamples(@Volatile var tatoeba: () -> ExampleSearchResult, private val courses: List<ExampleSentence>?) : ExampleSentencesProvider {
        val asked: MutableList<ExampleSource> = Collections.synchronizedList(mutableListOf())
        override suspend fun search(query: ExampleSearchQuery): ExampleSearchResult {
            asked += query.source
            return when (query.source) {
                ExampleSource.TATOEBA -> tatoeba()
                ExampleSource.COURSES -> ExampleSearchResult(courses.orEmpty(), courses?.size, null)
                ExampleSource.LEIPZIG -> ExampleSearchResult.EMPTY
            }
        }
        override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        override suspend fun sources(language: Language) =
            listOf(ExampleSourceFeatures.TATOEBA) + if (courses != null) listOf(CourseExamplesProvider.FEATURES) else emptyList()
    }

    /** A page with a conjugation table, so the Wiktionary card completes. */
    private class TableWiktionary : OnlineWiktionary {
        override suspend fun page(word: String, languageCode: String, nativeLanguage: String) =
            WiktionaryPage(word, "en", "pt", listOf(WiktionaryEntry("noun", listOf(WiktionarySense(listOf("house"))))))
        override suspend fun forms(word: String, languageCode: String, edition: String) =
            listOf(WiktionaryForms("noun", listOf(WiktionaryForm("casas", listOf("plural"), "declension"))))
        override suspend fun search(prefix: String, languageCode: String, edition: String) = emptyList<WiktionarySearchResult>()
    }

    private fun form(examples: ExampleSentencesProvider, wiktionary: OnlineWiktionary? = null): TermFormViewModel {
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
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        val dictionaries = DictionaryService(noPacks, noEntries)
        return TermFormViewModel(
            TermFormKey.ByText(portuguese, "casa"), TermService(terms, languages), terms, languages, settings, engine, examples, dictionaries, dictionaries,
            wiktionary = wiktionary,
        )
    }

    private fun waitFor(what: String, condition: () -> Boolean) = runBlocking {
        withTimeout(5_000) { while (!condition()) delay(10) }
        assertTrue(condition(), what)
    }

    @Test
    fun aFailedLookupFallsBackToTheCourses() {
        val examples = FakeExamples({ ExampleSearchResult.FAILED }, courses = listOf(courseSentence))
        val viewModel = form(examples)
        waitFor("the courses' examples") { viewModel.state.value.examples.isNotEmpty() && !viewModel.state.value.loadingExamples }
        val state = viewModel.state.value
        assertTrue(state.examplesFailed)
        assertTrue(state.examplesFromCourses)
        assertEquals(listOf(courseSentence), state.examples)
        assertNull(state.examplesTotal, "the count of the courses' examples is not offered as Tatoeba's")
        assertEquals(listOf(ExampleSource.TATOEBA, ExampleSource.COURSES), examples.asked)
    }

    @Test
    fun retryAsksTatoebaAgain() {
        val examples = FakeExamples({ ExampleSearchResult.FAILED }, courses = null)
        val viewModel = form(examples)
        waitFor("the failure") { viewModel.state.value.examplesFailed && !viewModel.state.value.loadingExamples }
        assertEquals(emptyList(), viewModel.state.value.examples)
        assertFalse(viewModel.state.value.examplesFromCourses)
        assertEquals(listOf(ExampleSource.TATOEBA), examples.asked, "without courses nothing stands in")

        examples.tatoeba = { ExampleSearchResult(listOf(tatoebaSentence), 7, null) }
        viewModel.retryExamples()
        waitFor("Tatoeba's examples") { viewModel.state.value.examples == listOf(tatoebaSentence) && !viewModel.state.value.loadingExamples }
        val state = viewModel.state.value
        assertFalse(state.examplesFailed)
        assertFalse(state.examplesFromCourses)
        assertEquals(7, state.examplesTotal)
    }

    @Test
    fun aProviderThatThrowsEndsTheLoadingAndStopsNoOtherLookup() {
        val examples = FakeExamples({ throw IllegalStateException("timeout") }, courses = null)
        val viewModel = form(examples, TableWiktionary())
        waitFor("the failure") { viewModel.state.value.examplesFailed }
        waitFor("the loading ends") { !viewModel.state.value.loadingExamples }
        waitFor("the Wiktionary card still completes") { viewModel.state.value.wiktionary.complete }
    }
}
