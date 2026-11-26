package com.idiomiko.feature.terms.form

import com.idiomiko.feature.terms.TestMain
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.courses.CourseExamplesProvider
import com.idiomiko.core.domain.dictionary.DictionaryEntry
import com.idiomiko.core.domain.dictionary.DictionaryId
import com.idiomiko.core.domain.dictionary.DictionaryPack
import com.idiomiko.core.domain.dictionary.DictionaryPackStore
import com.idiomiko.core.domain.dictionary.OnlineWiktionary
import com.idiomiko.core.domain.dictionary.WiktionaryEntry
import com.idiomiko.core.domain.dictionary.WiktionaryForm
import com.idiomiko.core.domain.dictionary.WiktionaryForms
import com.idiomiko.core.domain.dictionary.WiktionaryPage
import com.idiomiko.core.domain.dictionary.WiktionarySearchResult
import com.idiomiko.core.domain.dictionary.WiktionarySense
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.repository.DictionaryRepository
import com.idiomiko.core.domain.service.DictionaryService
import com.idiomiko.core.domain.service.ExampleSearchQuery
import com.idiomiko.core.domain.service.ExampleSearchResult
import com.idiomiko.core.domain.service.ExampleSentence
import com.idiomiko.core.domain.service.ExampleSentencesProvider
import com.idiomiko.core.domain.service.ExampleSource
import com.idiomiko.core.domain.service.ExampleSourceFeatures
import com.idiomiko.core.domain.service.TermService
import com.idiomiko.core.domain.service.TermTranslationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
    fun setUp() = TestMain.use()


    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-term-examples", ".db").also { it.delete() }))
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
