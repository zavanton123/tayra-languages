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
import com.tayra.languages.core.domain.dictionary.DictionarySense
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A word opened from its sentence gets the dictionary meaning the sentence uses, marked, and one click puts a wrong guess right. */
class TermFormMeaningTest {
    @Before
    fun setUp() = Dispatchers.setMain(Dispatchers.Default)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-term-meaning", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val sentence = "Thank you for coming to the debut of our food truck!"
    private val barter = "обменивать, менять; получать в результате обмена"
    private val lorry = "амер. грузовой автомобиль, грузовик"

    // Ranked as the app ranks them: the barter verb has more meanings than the lorry.
    private val truck = listOf(
        DictionaryEntry("truck", "verb", null, listOf(DictionarySense(listOf(barter)), DictionarySense(listOf("вести меновую торговлю")), DictionarySense(listOf("променять")))),
        DictionaryEntry("truck", "noun", null, listOf(DictionarySense(listOf(lorry)), DictionarySense(listOf("тележка, тачка")))),
    )

    /** Translates as an offline engine would, once [ready] lets it. */
    private class FakeTranslator : SentenceTranslator {
        var ready = CompletableDeferred<Unit>().also { it.complete(Unit) }
        override suspend fun translate(text: String, language: Language): String? {
            ready.await()
            return if (text == "truck") "грузовик" else "Спасибо, что пришли на дебют нашего грузовика!"
        }
    }

    private fun form(translator: SentenceTranslator): TermFormViewModel {
        val english = runBlocking {
            settings.update { it.copy(nativeLanguage = "ru") }
            languages.save(Language(name = "English"))
        }
        val installed = object : DictionaryPackStore {
            override suspend fun installedSize(pack: DictionaryPack): Long? = 1
            override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) = Unit
            override suspend fun remove(pack: DictionaryPack) {}
        }
        val entries = object : DictionaryRepository {
            override suspend fun isAvailable(dictionary: DictionaryId) = true
            override suspend fun entries(dictionary: DictionaryId, wordLc: String) = if (wordLc == "truck") truck else emptyList()
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
        val dictionaries = DictionaryService(installed, entries).also { runBlocking { it.refresh() } }
        return TermFormViewModel(
            TermFormKey.ByText(english, "truck", sentence), TermService(terms, languages), terms, languages, settings, engine, noExamples, dictionaries, dictionaries,
            sentenceTranslator = translator,
        )
    }

    private fun waitFor(what: String, condition: () -> Boolean) = runBlocking {
        withTimeout(5_000) { while (!condition()) delay(10) }
        assertTrue(condition(), what)
    }

    @Test
    fun theSentenceChoosesTheMeaningAndMarksIt() {
        val viewModel = form(FakeTranslator())
        waitFor("the meaning is chosen") { viewModel.state.value.meaning != null }
        val state = viewModel.state.value
        assertEquals(lorry, state.draft.translation)
        assertEquals(1 to 0, state.meaning!!.entry to state.meaning!!.sense)
        assertTrue(state.meaning!!.fits)
        assertTrue(state.translationSuggested)
    }

    @Test
    fun aMeaningPutsAWrongGuessRightThenAddsToWhatWasChosen() {
        val viewModel = form(FakeTranslator())
        waitFor("the meaning is chosen") { viewModel.state.value.meaning != null }
        viewModel.addGloss("тележка, тачка")
        assertEquals("тележка, тачка", viewModel.state.value.draft.translation, "in place of the suggestion")
        assertNull(viewModel.state.value.meaning)
        viewModel.addGloss(lorry)
        assertEquals("тележка, тачка; $lorry", viewModel.state.value.draft.translation, "after what the reader chose")
    }

    @Test
    fun whatTheReaderTypedIsNeverReplaced() {
        val translator = FakeTranslator().apply { ready = CompletableDeferred() }
        val viewModel = form(translator)
        // The dictionary's first meaning stands in until the translations come.
        waitFor("the first meaning is suggested") { viewModel.state.value.draft.translation == barter }
        viewModel.update { it.copy(translation = "фургон") }
        translator.ready.complete(Unit)
        runBlocking { delay(300) }
        assertEquals("фургон", viewModel.state.value.draft.translation)
        assertNull(viewModel.state.value.meaning)
    }
}
