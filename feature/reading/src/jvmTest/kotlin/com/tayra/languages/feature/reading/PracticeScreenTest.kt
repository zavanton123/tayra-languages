package com.tayra.languages.feature.reading

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.practice.ExerciseKind
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import com.tayra.languages.feature.reading.practice.PracticeActions
import com.tayra.languages.feature.reading.practice.PracticeContent
import com.tayra.languages.feature.reading.practice.PracticeViewModel
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The practice of a page asks about its words in every way, checks the answers and leaves the words' statuses alone. */
@OptIn(ExperimentalTestApi::class)
class PracticeScreenTest {

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-practice", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val termService = TermService(terms, languages)
    private val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), termService)
    private val text = "Eu moro no Brasil. Durante a semana, tenho uma rotina bastante organizada e muito cheia. " +
        "Quando precisamos comprar alguma coisa, passamos no supermercado perto de casa."
    private val learning = mapOf("moro" to "I live", "semana" to "week", "rotina" to "routine", "comprar" to "to buy")
    private val spoken = mutableListOf<String>()
    private var bookId = 0L
    private var languageId = 0L

    private fun ComposeUiTest.show(seed: Int = 7): PracticeViewModel {
        runBlocking {
            languageId = languages.save(Language(name = "Portuguese"))
            bookId = BookService(books, languages).create(BookDraft(languageId = languageId, title = "Long text", text = text))
            learning.forEach { (word, meaning) -> terms.save(Term(languageId = languageId, text = word, textLc = word, status = TermStatus.NEW_1, translation = meaning)) }
        }
        val examples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) =
                if (query.text == "comprar") ExampleSearchResult(listOf(ExampleSentence("Vou comprar pão amanhã.", "I will buy bread tomorrow.")), 1, null) else ExampleSearchResult.EMPTY
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        val translator = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = "[$text]" }
        val viewModel = PracticeViewModel(bookId, 1, books, languages, reading, examples, translator, settings, Random(seed))
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    val state by viewModel.state.collectAsState()
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        if (!state.loading) {
                            PracticeContent(
                                state,
                                PracticeActions(
                                    onChoose = viewModel::choose, onTyped = viewModel::setTyped, onSubmit = viewModel::submit, onSkip = viewModel::skip,
                                    onNext = viewModel::next, onRestart = viewModel::start, onSpeak = { spoken += it },
                                ),
                            )
                        }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("practice-question").fetchSemanticsNodes().isNotEmpty() }
        return viewModel
    }

    private fun ComposeUiTest.shot(name: String) {
        val folder = System.getenv("PRACTICE_SCREENSHOTS") ?: return
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(folder, "practice-$name.png"))
    }

    @Test
    fun everyQuestionCanBeAnsweredAndNothingChangesTheWords() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        val exercises = viewModel.state.value.exercises
        assertEquals(ExerciseKind.entries.toSet(), exercises.map { it.kind }.toSet(), "every kind of question is asked")
        assertEquals(learning.keys, exercises.map { it.answer.takeIf { a -> a in learning.keys } ?: it.word }.toSet())
        val seen = mutableSetOf<ExerciseKind>()
        exercises.forEachIndexed { index, exercise ->
            waitUntil(timeoutMillis = 5_000) { viewModel.state.value.index == index && !viewModel.state.value.answered }
            onNodeWithText("Question ${index + 1} of ${exercises.size}").assertExists()
            // The word asked for is not on show before the answer.
            if (exercise.kind != ExerciseKind.HEAR_CHOOSE && !exercise.wholeSentence) {
                val shown = onNodeWithTag("practice-sentence").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].single().text
                assertTrue("[...]" in shown && exercise.word !in shown.replace(exercise.sentence.replace(exercise.word, ""), ""), shown)
            } else {
                assertEquals(0, onAllNodesWithTag("practice-sentence").fetchSemanticsNodes().size, "what is heard is not shown")
            }
            if (seen.add(exercise.kind)) shot("${exercise.kind.name.lowercase()}${if (exercise.wholeSentence) "-sentence" else ""}")
            if (exercise.isChoice) {
                onNodeWithTag("practice-option-${exercise.options.indexOf(exercise.answer)}").performClick()
            } else {
                onNodeWithTag("practice-input").performTextInput(exercise.answer.lowercase())
                onNodeWithText("Check").performClick()
            }
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Correct").fetchSemanticsNodes().isNotEmpty() }
            if (index == 0) shot("answered")
            onNodeWithText(if (index == exercises.lastIndex) "See the result" else "Continue").performClick()
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("${exercises.size} of ${exercises.size} right").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Every answer was right.").assertExists()
        shot("result")

        // The listening questions were read aloud: the word for a choice, the sentence for a dictation.
        assertEquals(exercises.mapNotNull { it.spoken }, spoken)
        assertTrue(spoken.isNotEmpty())
        // Practice asks only: every word is still at the status it had.
        learning.keys.forEach { word -> assertEquals(TermStatus.NEW_1, runBlocking { terms.findByTextLc(languageId, word) }?.status, word) }
    }

    @Test
    fun wrongAndSkippedAnswersShowTheWordAndAreListedAtTheEnd() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        val exercises = viewModel.state.value.exercises
        val missed = mutableListOf<String>()
        exercises.forEachIndexed { index, exercise ->
            waitUntil(timeoutMillis = 5_000) { viewModel.state.value.index == index && !viewModel.state.value.answered }
            when {
                index == 0 && exercise.isChoice -> onNodeWithTag("practice-option-${exercise.options.indexOfFirst { it != exercise.answer }}").performClick()
                index == 0 -> {
                    onNodeWithTag("practice-input").performTextInput("errado")
                    onNodeWithText("Check").performClick()
                }
                index == 1 -> onNodeWithText("Show the answer").performClick()
                exercise.isChoice -> onNodeWithTag("practice-option-${exercise.options.indexOf(exercise.answer)}").performClick()
                else -> {
                    onNodeWithTag("practice-input").performTextInput(exercise.answer)
                    onNodeWithText("Check").performClick()
                }
            }
            if (index < 2) {
                waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Not quite").fetchSemanticsNodes().isNotEmpty() }
                if (exercise.word !in missed) missed += exercise.word
                if (index == 0) shot("wrong")
            }
            waitUntil(timeoutMillis = 5_000) { viewModel.state.value.answered }
            onNodeWithText(if (index == exercises.lastIndex) "See the result" else "Continue").performClick()
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("${exercises.size - 2} of ${exercises.size} right").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Words to look at again: ${missed.joinToString(", ")}").assertExists()

        onNodeWithText("Practice again").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Question 1 of ${exercises.size}").fetchSemanticsNodes().isNotEmpty() }
    }
}
