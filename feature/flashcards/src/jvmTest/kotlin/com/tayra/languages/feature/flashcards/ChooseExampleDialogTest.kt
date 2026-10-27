package com.tayra.languages.feature.flashcards

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.network.KtorRecordingFetcher
import com.tayra.languages.core.data.network.createHttpClient
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.service.ExampleRecordings
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.feature.terms.examples.ChooseExampleDialog
import com.tayra.languages.feature.terms.examples.ExamplesSearchViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The examples to replace a card's sentence with: only those holding a form of the word can be chosen. */
@OptIn(ExperimentalTestApi::class)
class ChooseExampleDialogTest {

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-choose", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val results = listOf(
        ExampleSentence("As turmas começam às oito.", "Занятия начинаются в восемь."),
        ExampleSentence("Há três turmas nesta escola.", "В этой школе три класса."),
        ExampleSentence("Uma turma nova abriu.", "Открылся новый класс."),
        ExampleSentence("A escola fecha cedo.", "Школа закрывается рано."),
    )

    @After
    fun tearDown() {
        stopKoin()
    }

    /** The dialog over the screen for the card of "turmas"; what it hands back on replacing goes to [replaced]. */
    private fun ComposeUiTest.show(replaced: MutableList<Pair<String, String?>>, onDismiss: () -> Unit = {}) {
        val (languageId, termId) = runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            id to terms.save(Term(languageId = id, text = "turmas", textLc = "turmas", status = TermStatus.NEW_1, sentence = "Nossa escola tem nove turmas."))
        }
        val examples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult(results, 1248, null)
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        startKoin {
            modules(module {
                val cache = MemorySpeechAudioCache()
                single { LocalSpeech(emptyList()) }
                single { ExampleRecordings(KtorRecordingFetcher(createHttpClient()), cache) }
                single { SentenceAudio(get(), settings, cache, get()) }
                single<SettingsRepository> { settings }
                single<TermRepository> { terms }
                viewModel { (id: Long, text: String) -> ExamplesSearchViewModel(id, text, languages, settings, examples) }
            })
        }
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        ChooseExampleDialog(
                            termId = termId,
                            languageId = languageId,
                            word = "turmas",
                            sentence = "Nossa escola tem nove turmas.",
                            translation = "В нашей школе девять классов.",
                            onReplace = { sentence, translation -> replaced += sentence to translation },
                            onDismiss = onDismiss,
                        )
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("As turmas começam às oito.", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.save(env: String) {
        System.getenv(env)?.let { waitForIdle(); ImageIO.write(onAllNodes(isDialog())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }

    private fun ComposeUiTest.choice(sentence: String) = onNode(hasTestTag("example-choice") and hasText(sentence, substring = true))

    @Test
    fun aChosenExampleReplacesTheCardsWithItsTranslation() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val replaced = mutableListOf<Pair<String, String?>>()
        show(replaced)
        onNodeWithText("Choose an example for ‘turmas’").assertExists()
        onNodeWithTag("current-example").assertExists()
        onNodeWithText("1248 sentences").assertExists()
        onNodeWithTag("replace-example").assertIsNotEnabled()

        choice("Há três turmas nesta escola.").performClick()
        choice("Há três turmas nesta escola.").assertIsSelected()
        onNodeWithTag("replace-example").assertIsEnabled()
        save("CHOOSE_EXAMPLE_SCREENSHOT")
        onNodeWithTag("replace-example").performClick()
        assertEquals(listOf<Pair<String, String?>>("Há três turmas nesta escola." to "В этой школе три класса."), replaced)
    }

    /** "turma" is a form of "turmas" the card can blank; a sentence without either cannot be chosen. */
    @Test
    fun onlyASentenceWithAFormOfTheWordCanBeChosen() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val replaced = mutableListOf<Pair<String, String?>>()
        show(replaced)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("No form of ‘turmas’ in this sentence").fetchSemanticsNodes().size == 1 }
        choice("A escola fecha cedo.").performClick()
        onNodeWithTag("replace-example").assertIsNotEnabled()
        choice("Uma turma nova abriu.").performClick()
        onNodeWithTag("replace-example").performClick()
        assertEquals(listOf<Pair<String, String?>>("Uma turma nova abriu." to "Открылся новый класс."), replaced)
    }

    /** ↓ and ↑ move the choice over the sentences that can be chosen, and Enter replaces the example with it. */
    @Test
    fun theKeysMoveTheChoiceAndReplace() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val replaced = mutableListOf<Pair<String, String?>>()
        show(replaced)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("No form of ‘turmas’ in this sentence").fetchSemanticsNodes().size == 1 }
        val keys = onNodeWithTag("choose-example-dialog")
        keys.performKeyInput { pressKey(Key.DirectionDown); pressKey(Key.DirectionDown); pressKey(Key.DirectionDown); pressKey(Key.DirectionDown); pressKey(Key.DirectionUp) }
        choice("Há três turmas nesta escola.").assertIsSelected()
        keys.performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf<Pair<String, String?>>("Há três turmas nesta escola." to "В этой школе три класса."), replaced)
    }

    @Test
    fun cancelReplacesNothing() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val replaced = mutableListOf<Pair<String, String?>>()
        var dismissed = false
        show(replaced, onDismiss = { dismissed = true })
        choice("As turmas começam às oito.").performClick()
        onNodeWithText("Cancel").performClick()
        assertTrue(dismissed)
        assertEquals(emptyList<Pair<String, String?>>(), replaced)
    }

    /** A phone sets the filters two to a row and says in the footer whether an example is chosen. */
    @Test
    fun aPhoneSaysWhetherAnExampleIsChosen() = runDesktopComposeUiTest(width = 430, height = 932) {
        val replaced = mutableListOf<Pair<String, String?>>()
        show(replaced)
        onNodeWithText("Choose an example").assertExists()
        onNodeWithText("For ‘turmas’").assertExists()
        onNodeWithText("No example selected").assertExists()
        choice("As turmas começam às oito.").performClick()
        onNodeWithText("1 example selected").assertExists()
        assertTrue(onAllNodesWithTag("replace-example").fetchSemanticsNodes().isNotEmpty())
        save("CHOOSE_EXAMPLE_PHONE_SCREENSHOT")
    }
}
