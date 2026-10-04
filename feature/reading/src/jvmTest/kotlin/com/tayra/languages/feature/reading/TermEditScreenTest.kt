package com.tayra.languages.feature.reading

import androidx.compose.ui.test.isToggleable
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.domain.flashcards.FlashcardService
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.network.KtorRecordingFetcher
import com.tayra.languages.core.data.network.createHttpClient
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.dictionary.DictionarySense
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.ExampleRecordings
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.feature.terms.examples.ExampleTerms
import com.tayra.languages.feature.terms.form.TermEditScreen
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.form.TermFormViewModel
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The term editor page shows the term, its dictionary entry, examples, status and links, and saves a new status. */
@OptIn(ExperimentalTestApi::class)
class TermEditScreenTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun theEditorShowsEveryPartAndSavesTheStatus() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-term-edit", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val languageId = runBlocking { languages.save(Language(name = "Portuguese")) }
        val termId = runBlocking { terms.save(Term(languageId = languageId, text = "semana", textLc = "semana", status = TermStatus.NEW_1, translation = "week")) }
        val examples = listOf(
            ExampleSentence(text = "Quantas vezes por semana tu te banhas?", translation = null),
            ExampleSentence(text = "Tom lê vários livros por semana.", translation = null),
            ExampleSentence(text = "Que é que você estava fazendo em Boston na semana passada?", translation = null),
        )
        val sentences = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult(examples, 762, null)
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        val dictionaries = DictionaryService(
            object : DictionaryPackStore {
                override suspend fun installedSize(pack: DictionaryPack): Long? = if (pack.id == DictionaryId("pt", "en")) 1_000_000 else null
                override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) = Unit
                override suspend fun remove(pack: DictionaryPack) {}
            },
            object : DictionaryRepository {
                override suspend fun isAvailable(dictionary: DictionaryId) = true
                override suspend fun entries(dictionary: DictionaryId, wordLc: String) =
                    if (wordLc == "semana") listOf(DictionaryEntry("semana", "noun", "siˈmane", listOf(DictionarySense(listOf("week")), DictionarySense(listOf("a week's wages"))))) else emptyList()
                override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
                override suspend fun close(dictionary: DictionaryId) {}
            },
        ).also { runBlocking { it.refresh() } }
        val termService = TermService(terms, languages)
        val flashcards = FlashcardService(FlashcardRepositoryImpl(provider), terms, settings)
        val exampleTerms = ExampleTerms(ReadingService(BookRepositoryImpl(provider), languages, terms, WordsReadRepositoryImpl(provider), termService), termService)
        startKoin {
            modules(module {
                single { LocalSpeech(emptyList()) }
                val cache = MemorySpeechAudioCache()
                single { ExampleRecordings(KtorRecordingFetcher(createHttpClient()), cache) }
                single { SentenceAudio(get(), settings, cache, get()) }
                single<SettingsRepository> { settings }
                single { WordTranslationService(terms, dictionaries, engine, settings) }
                viewModel { (key: TermFormKey) -> TermFormViewModel(key, termService, terms, languages, settings, engine, sentences, dictionaries, dictionaries, exampleTerms, flashcards) }
            })
        }
        var done = false
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {}) {
                        TermEditScreen(TermFormKey.ById(termId), onNavigate = {}, onBack = {}, onDone = { done = true }, onOpenParent = { _, _ -> })
                    }
                }
            }
        }

        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("a week's wages")).fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Tom lê vários livros por semana.")).fetchSemanticsNodes().isNotEmpty() }
        for (part in listOf("Term information", "Learning status", "Flashcard", "Dictionaries", "Delete term", "Edit term")) {
            assertTrue(onAllNodes(hasText(part)).fetchSemanticsNodes().isNotEmpty(), "$part is shown")
        }
        System.getenv("TERM_EDIT_SCREENSHOT")?.let { save(it) }

        // A term being learned has a flashcard, which can be set aside from here.
        onNodeWithText("Not shown yet").assertExists()
        onNode(isToggleable()).performClick()
        waitUntil(timeoutMillis = 10_000) { runBlocking { flashcards.cardFor(termId) }?.suspended == true }
        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("New, suspended")).fetchSemanticsNodes().isNotEmpty() }
        onNode(isToggleable()).performClick()
        waitUntil(timeoutMillis = 10_000) { runBlocking { flashcards.cardFor(termId) }?.suspended == false }

        onAllNodes(hasText("2") and isSelectable())[0].performClick()
        onNodeWithText("Save changes").performClick()
        waitUntil(timeoutMillis = 10_000) { done }
        assertEquals(TermStatus.NEW_2, runBlocking { terms.getById(termId) }?.status)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}
