package com.tayra.languages.feature.reading

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasTestTag
import org.koin.core.parameter.parametersOf
import org.koin.compose.viewmodel.koinViewModel
import com.tayra.languages.feature.terms.form.TermFormPanel
import com.tayra.languages.core.ui.components.WindowWidth
import com.tayra.languages.core.ui.components.LocalWindowWidth
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
import com.tayra.languages.core.data.repository.PracticeProgressRepositoryImpl
import com.tayra.languages.core.domain.practice.WordProgress
import kotlin.time.Instant

/** The term editor page shows the term, its dictionary entry, examples, status and links, and saves a new status. */
@OptIn(ExperimentalTestApi::class)
class TermEditScreenTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun theEditorShowsEveryPartAndSavesTheStatus() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val editor = openEditor("semana", DictionaryId("pt", "en"))
        val (termId, terms, flashcards) = editor
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
        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("New card, suspended")).fetchSemanticsNodes().isNotEmpty() }
        onNode(isToggleable()).performClick()
        waitUntil(timeoutMillis = 10_000) { runBlocking { flashcards.cardFor(termId) }?.suspended == false }

        onAllNodes(hasText("2") and isSelectable())[0].performClick()
        onNodeWithText("Save changes").performClick()
        waitUntil(timeoutMillis = 10_000) { editor.done }
        assertEquals(TermStatus.NEW_2, runBlocking { terms.getById(termId) }?.status)
    }

    /** With only another edition of the pair installed, a word it lacks is offered the Wiktionary pack for more words, not told a dictionary is missing. */
    @Test
    fun aWordMissingFromAnotherEditionIsOfferedTheFullerPack() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        openEditor("bocejava", DictionaryId("pt", "en", edition = DictionaryId.FREEDICT))
        waitUntil(timeoutMillis = 10_000) { onAllNodes(hasText("Download Wiktionary dictionary")).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(onAllNodes(hasText("No entry for “bocejava” in FreeDict", substring = true)).fetchSemanticsNodes().isNotEmpty())
        assertTrue(onAllNodes(hasText("is not downloaded", substring = true)).fetchSemanticsNodes().isEmpty())
    }

    private class Editor(val termId: Long, val terms: TermRepositoryImpl, val flashcards: FlashcardService) {
        var done = false
        operator fun component1() = termId
        operator fun component2() = terms
        operator fun component3() = flashcards
    }

    /** The editor for [word], a Portuguese term being learned, with only the [installed] dictionary of the pair on the device. */
    private fun ComposeUiTest.openEditor(word: String, installed: DictionaryId, sheet: Boolean = false, practised: List<WordProgress.Answer> = emptyList()): Editor {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-term-edit", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val languageId = runBlocking { languages.save(Language(name = "Portuguese")) }
        val termId = runBlocking { terms.save(Term(languageId = languageId, text = word, textLc = word, status = TermStatus.NEW_1, translation = "week")) }
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
                override suspend fun installedSize(pack: DictionaryPack): Long? = if (pack.id == installed) 1_000_000 else null
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
        val progress = PracticeProgressRepositoryImpl(provider)
        runBlocking { practised.forEachIndexed { i, answer -> progress.record(termId, answer, Instant.fromEpochMilliseconds(1_700_000_000_000L + i)) } }
        val exampleTerms = ExampleTerms(ReadingService(BookRepositoryImpl(provider), languages, terms, WordsReadRepositoryImpl(provider), termService), termService)
        startKoin {
            modules(module {
                single { LocalSpeech(emptyList()) }
                val cache = MemorySpeechAudioCache()
                single { ExampleRecordings(KtorRecordingFetcher(createHttpClient()), cache) }
                single { SentenceAudio(get(), settings, cache, get()) }
                single<SettingsRepository> { settings }
                single { WordTranslationService(terms, dictionaries, engine, settings) }
                viewModel { (key: TermFormKey) -> TermFormViewModel(key, termService, terms, languages, settings, engine, sentences, dictionaries, dictionaries, exampleTerms, flashcards, practiceProgress = progress) }
            })
        }
        val opened = Editor(termId, terms, flashcards)
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {}) {
                        if (sheet) {
                            // As the phone's reader holds it: embedded, in a sheet.
                            CompositionLocalProvider(LocalWindowWidth provides WindowWidth.COMPACT) {
                                TermFormPanel(koinViewModel(key = "sheet-$termId") { parametersOf(TermFormKey.ById(termId)) }, embedded = true)
                            }
                        } else {
                            TermEditScreen(TermFormKey.ById(termId), onNavigate = {}, onBack = {}, onDone = { opened.done = true }, onOpenParent = { _, _ -> })
                        }
                    }
                }
            }
        }

        return opened
    }

    /** In the phone reader's sheet, the word's pronunciation and its first dictionary sense show at once, above the dictionary. */
    @Test
    fun aPhonesWordSheetShowsThePronunciationAndFirstSenseAtOnce() = runDesktopComposeUiTest(width = 400, height = 900) {
        openEditor("semana", DictionaryId("pt", "en"), sheet = true)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("term-first-sense-preview")).fetchSemanticsNodes().isNotEmpty() }
        // At the top of the sheet, before the dictionary repeats them in full.
        onNode(hasText("siˈmane") and hasAnyAncestor(hasTestTag("term-pronunciation-preview")), useUnmergedTree = true).assertExists()
        onNode(hasText("week") and hasAnyAncestor(hasTestTag("term-first-sense-preview")), useUnmergedTree = true).assertExists()
    }

    /** The word's page shows how far practice has taken it, beside its flashcard. */
    @Test
    fun theWordsPageShowsItsPracticeLevel() = runDesktopComposeUiTest(width = 1586, height = 1400) {
        openEditor("semana", DictionaryId("pt", "en"), practised = listOf(WordProgress.Answer.RIGHT, WordProgress.Answer.RIGHT, WordProgress.Answer.WRONG))
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("practice-level")).fetchSemanticsNodes().isNotEmpty() }
        onNode(hasText("Recalling with help")).assertExists()
        onNode(hasText("2 of 3")).assertExists()
        System.getenv("TERM_PRACTICE_SCREENSHOT")?.let { save(it) }
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}
