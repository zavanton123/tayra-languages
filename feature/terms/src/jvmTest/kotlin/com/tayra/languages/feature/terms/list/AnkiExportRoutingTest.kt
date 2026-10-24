package com.tayra.languages.feature.terms.list

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.export.AnkiPackagerImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.export.AnkiAddResult
import com.tayra.languages.core.domain.export.AnkiCollection
import com.tayra.languages.core.domain.export.AnkiConnection
import com.tayra.languages.core.domain.export.AnkiExportService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermDraft
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout

/** One Anki action: the words go straight into a running Anki, or into a file when none answers. */
class AnkiExportRoutingTest {

    private val provider = DatabaseProvider(DatabaseDriverFactory(kotlin.io.path.createTempFile("tayra-anki", ".db").toFile().also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = TermService(terms, languages)
    private val sentenceAudio = SentenceAudio(LocalSpeech(emptyList()), settings, MemorySpeechAudioCache())

    /** An Anki that answers with [version], or never when it is null and [hangs]; it keeps the collections it is given. */
    private class FakeAnki(private val version: Int?, private val hangs: Boolean = false) : AnkiConnection {
        val added = mutableListOf<AnkiCollection>()
        override suspend fun version(): Int? = if (hangs) awaitCancellation() else version
        override suspend fun add(collection: AnkiCollection): AnkiAddResult {
            added += collection
            return AnkiAddResult(collection.notes.indices.toSet(), emptySet(), emptyMap())
        }
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(Dispatchers.Default)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(connection: AnkiConnection?): TermsListViewModel {
        runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = id) }
            for (word in listOf("comprar", "pensar")) {
                service.save(TermDraft(languageId = id, text = word, translation = "x", status = TermStatus.NEW_1, statusExplicitlySet = true))
            }
        }
        val noDictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val noTranslator = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null }
        val anki = AnkiExportService(terms, languages, noDictionary, noTranslator, sentenceAudio, settings, AnkiPackagerImpl())
        return TermsListViewModel(null, terms, languages, settings, service, anki, connection)
    }

    /** Exports, and returns whether a file was offered and the message shown once done. */
    private fun export(viewModel: TermsListViewModel): Pair<Boolean, String?> = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        var file = false
        scope.launch { viewModel.events.flow.collect { if (it is TermsListEvent.AnkiReady) file = true } }
        // The state is only kept up while it is watched.
        scope.launch { viewModel.state.collect {} }
        viewModel.state.first { !it.loading }
        viewModel.exportAnki()
        val done = withTimeout(10_000) { viewModel.state.first { it.exporting == null && (it.message != null || file) } }
        scope.cancel()
        file to done.message
    }

    @Test
    fun aRunningAnkiTakesTheWordsDirectly() {
        val anki = FakeAnki(version = 6)
        val (file, message) = export(viewModel(anki))
        assertEquals(false, file, "no file to save")
        assertEquals(1, anki.added.size)
        assertEquals("Added 2 cards to Anki", message)
    }

    @Test
    fun withoutAnkiRunningTheWordsGoIntoAFile() {
        val anki = FakeAnki(version = null)
        val (file, _) = export(viewModel(anki))
        assertTrue(file, "a file to save")
        assertEquals(0, anki.added.size)
    }

    @Test
    fun anAnkiThatDoesNotAnswerIsGivenUpForAFile() {
        val anki = FakeAnki(version = null, hangs = true)
        val (file, _) = export(viewModel(anki))
        assertTrue(file, "a file to save")
        assertEquals(0, anki.added.size)
    }

    @Test
    fun withNoWayToReachAnkiTheWordsGoIntoAFile() {
        val (file, _) = export(viewModel(null))
        assertTrue(file, "a file to save")
    }
}
