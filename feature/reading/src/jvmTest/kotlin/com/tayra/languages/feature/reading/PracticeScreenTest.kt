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
import com.tayra.languages.core.domain.practice.Exercise
import com.tayra.languages.core.domain.practice.ExerciseType
import com.tayra.languages.core.domain.practice.PracticeContext
import com.tayra.languages.core.domain.practice.PracticeResponse
import com.tayra.languages.core.domain.practice.PracticeSetup
import com.tayra.languages.core.domain.practice.PracticeBuilder
import com.tayra.languages.core.domain.practice.PracticeMaterialLoader
import com.tayra.languages.core.domain.practice.PracticeSource
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
import com.tayra.languages.core.ui.components.AppDialogSurface
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxHeight

/** The practice of a page asks about its words in every way, checks the answers and leaves the words' statuses alone. */
@OptIn(ExperimentalTestApi::class)
class PracticeScreenTest {

    /** A question's type, answer and sentence, as "GAP-CHOOSE-ORIGINAL". */
    private val Exercise.kind get() = "$type-$response-$context"

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

    private fun ComposeUiTest.show(seed: Int = 7, setup: PracticeSetup = PracticeSetup()): PracticeViewModel {
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
        val viewModel = PracticeViewModel(PracticeSource.Page(bookId, 1), setup, PracticeMaterialLoader(books, languages, reading, terms), examples, translator, settings, Random(seed))
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
                                    onNext = viewModel::next, onRestart = viewModel::start, onRestartMissed = viewModel::startMissed, onSpeak = { spoken += it },
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

    /** In a dialog over the page that started it, the practice's title has a close button beside it. */
    @Test
    fun aPracticeInADialogClosesFromBesideItsTitle() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        runBlocking {
            languageId = languages.save(Language(name = "Portuguese"))
            bookId = BookService(books, languages).create(BookDraft(languageId = languageId, title = "Long text", text = text))
            learning.forEach { (word, meaning) -> terms.save(Term(languageId = languageId, text = word, textLc = word, status = TermStatus.NEW_1, translation = meaning)) }
        }
        val examples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult.EMPTY
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        val translator = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = "[$text]" }
        val viewModel = PracticeViewModel(PracticeSource.Page(bookId, 1), PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.CHOOSE)), PracticeMaterialLoader(books, languages, reading, terms), examples, translator, settings, Random(7))
        var closed = 0
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    val state by viewModel.state.collectAsState()
                    AppDialogSurface(onDismissRequest = {}, modifier = Modifier.fillMaxHeight(), maxWidth = Dp.Infinity, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        if (!state.loading) PracticeContent(state, PracticeActions(onChoose = viewModel::choose, onNext = viewModel::next, onSpeak = {}), onClose = { closed++ })
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("practice-question").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("PRACTICE_DIALOG_SCREENSHOT")?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
        onNodeWithTag("close-practice-dialog").performClick()
        assertEquals(1, closed)
    }

    /** The whole text's practice asks about the words being learned on every page, each in its own sentence. */
    @Test
    fun theWholeTextIsPractisedPageByPage() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        runBlocking {
            languageId = languages.save(Language(name = "Portuguese"))
            bookId = BookService(books, languages).create(BookDraft(languageId = languageId, title = "Rotina", text = "Eu moro no Brasil.\n\nQuando precisamos comprar alguma coisa, passamos no supermercado.\n\nA minha casa fica perto da praia.", wordsPerPage = 4))
            for ((word, meaning) in listOf("moro" to "I live", "comprar" to "to buy")) terms.save(Term(languageId = languageId, text = word, textLc = word, status = TermStatus.NEW_1, translation = meaning))
        }
        assertTrue(runBlocking { books.pageCount(bookId) } > 1, "the text runs over several pages")
        val examples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult.EMPTY
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        val translator = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = "[$text]" }
        val viewModel = PracticeViewModel(PracticeSource.Book(bookId), PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.CHOOSE)), PracticeMaterialLoader(books, languages, reading, terms), examples, translator, settings, Random(3))
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    val state by viewModel.state.collectAsState()
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        if (!state.loading) PracticeContent(state, PracticeActions(onChoose = viewModel::choose, onNext = viewModel::next, onBack = {}, onSpeak = {}))
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("practice-question").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("The 2 words you are learning in Rotina.").assertExists()
        assertEquals(setOf("moro", "comprar"), viewModel.state.value.exercises.map { it.word }.toSet())
        assertTrue(viewModel.state.value.exercises.first { it.word == "comprar" }.sentence.startsWith("Quando precisamos"), "in its own sentence of the text")
    }

    /** Words chosen in the vocabulary are asked in the sentences they were saved with, with the kinds of question chosen: here only choices. */
    @Test
    fun chosenWordsArePractisedWithTheKindsChosen() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val ids = runBlocking {
            languageId = languages.save(Language(name = "Portuguese"))
            listOf(
                Triple("moro", "I live", "Eu moro no Brasil."),
                Triple("semana", "week", "Durante a semana, tenho uma rotina bastante organizada."),
                Triple("comprar", "to buy", "Quando precisamos comprar alguma coisa, passamos no supermercado."),
                Triple("rotina", "routine", "Durante a semana, tenho uma rotina bastante organizada."),
            ).map { (word, meaning, sentence) -> terms.save(Term(languageId = languageId, text = word, textLc = word, status = TermStatus.NEW_1, translation = meaning, sentence = sentence)) } +
                // Saved without a sentence: left out.
                terms.save(Term(languageId = languageId, text = "casa", textLc = "casa", status = TermStatus.NEW_1, translation = "house"))
        }
        val examples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult.EMPTY
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        val translator = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = "[$text]" }
        val viewModel = PracticeViewModel(PracticeSource.Words(ids), PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.HEAR_WORD), responses = setOf(PracticeResponse.CHOOSE)), PracticeMaterialLoader(books, languages, reading, terms), examples, translator, settings, Random(3))
        var left = 0
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    val state by viewModel.state.collectAsState()
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        if (!state.loading) PracticeContent(state, PracticeActions(onChoose = viewModel::choose, onNext = viewModel::next, onBack = { left++ }, onSpeak = { spoken += it }))
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("practice-question").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("4 words from your vocabulary.").assertExists()
        onNodeWithText("Questions: choose the missing word, listen and choose").assertExists()
        val exercises = viewModel.state.value.exercises
        assertTrue(exercises.isNotEmpty() && exercises.all { it.isChoice }, exercises.map { it.kind }.toString())
        assertEquals(setOf("moro", "semana", "comprar", "rotina"), exercises.map { it.word }.toSet())
        exercises.forEachIndexed { index, exercise ->
            waitUntil(timeoutMillis = 5_000) { viewModel.state.value.index == index && !viewModel.state.value.answered }
            onNodeWithTag("practice-option-${exercise.options.indexOf(exercise.answer)}").performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Correct").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText(if (index == exercises.lastIndex) "See the result" else "Continue").performClick()
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Back to vocabulary").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Back to vocabulary").performClick()
        assertEquals(1, left)
    }

    private fun ComposeUiTest.shot(name: String) {
        val folder = System.getenv("PRACTICE_SCREENSHOTS") ?: return
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(folder, "practice-$name.png"))
    }

    /** Asked for a meaning, the sentence shows the word marked, not blanked, and the choices are meanings. */
    @Test
    fun aMeaningQuestionShowsTheWordAndOffersMeanings() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.MEANING)))
        val exercise = viewModel.state.value.exercises.first()
        assertEquals(ExerciseType.MEANING, exercise.type)
        onNodeWithText("WHAT DOES THE MARKED WORD MEAN?").assertExists()
        val shown = onNodeWithTag("practice-sentence").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].single().text
        assertTrue(exercise.word in shown && "[...]" !in shown, shown)
        assertTrue(exercise.options.all { it in learning.values }, exercise.options.toString())
        shot("meaning")
        onNodeWithTag("practice-option-${exercise.options.indexOf(exercise.answer)}").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Correct").fetchSemanticsNodes().isNotEmpty() }
    }

    /** A translation question shows the sentence's translation first, the sentence with its gap under it, and takes the typed word. */
    @Test
    fun aTranslationQuestionStartsFromTheTranslation() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.TRANSLATION), responses = setOf(PracticeResponse.TYPE)))
        val exercise = viewModel.state.value.exercises.first()
        assertEquals(ExerciseType.TRANSLATION, exercise.type)
        assertEquals("[${exercise.sentence}]", exercise.sentenceTranslation)
        onNodeWithText("READ THE TRANSLATION AND TYPE THE MISSING WORD").assertExists()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("practice-translation").fetchSemanticsNodes().isNotEmpty() }
        val translation = onNodeWithTag("practice-translation").fetchSemanticsNode().boundsInRoot
        val sentence = onNodeWithTag("practice-sentence").fetchSemanticsNode()
        assertTrue(translation.bottom <= sentence.boundsInRoot.top, "the translation comes first")
        assertTrue("[...]" in sentence.config[androidx.compose.ui.semantics.SemanticsProperties.Text].single().text)
        shot("translation")
        onNodeWithTag("practice-input").performTextInput(exercise.answer)
        onNodeWithText("Check").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Correct").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, onAllNodesWithTag("practice-translation").fetchSemanticsNodes().size, "and is shown once, not again under the answer")
    }

    @Test
    fun everyQuestionCanBeAnsweredAndNothingChangesTheWords() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        val exercises = viewModel.state.value.exercises
        assertEquals(
            setOf("GAP-CHOOSE-ORIGINAL", "GAP-CHOOSE-EXAMPLE", "GAP-TYPE-ORIGINAL", "HEAR_WORD-CHOOSE-ORIGINAL", "DICTATION-TYPE-ORIGINAL"),
            exercises.map { it.kind }.toSet(),
            "every kind of question is asked",
        )
        assertEquals(learning.keys, exercises.map { it.answer.takeIf { a -> a in learning.keys } ?: it.word }.toSet())
        val seen = mutableSetOf<String>()
        exercises.forEachIndexed { index, exercise ->
            waitUntil(timeoutMillis = 5_000) { viewModel.state.value.index == index && !viewModel.state.value.answered }
            onNodeWithText("Question ${index + 1} of ${exercises.size}").assertExists()
            // The word asked for is not on show before the answer.
            if (exercise.type != ExerciseType.HEAR_WORD && !exercise.wholeSentence) {
                val shown = onNodeWithTag("practice-sentence").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].single().text
                assertTrue("[...]" in shown && exercise.word !in shown.replace(exercise.sentence.replace(exercise.word, ""), ""), shown)
            } else {
                assertEquals(0, onAllNodesWithTag("practice-sentence").fetchSemanticsNodes().size, "what is heard is not shown")
            }
            if (seen.add(exercise.kind)) shot("${exercise.kind.lowercase()}${if (exercise.wholeSentence) "-sentence" else ""}")
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

        // With words missed, the main way on is practising only them.
        onNodeWithTag("practise-missed").performClick()
        waitUntil(timeoutMillis = 10_000) { viewModel.state.value.let { !it.finished && !it.loading && it.exercises.isNotEmpty() } }
        val again = viewModel.state.value.exercises
        val missedWords = missed.map { it.lowercase() }.toSet()
        assertTrue(again.all { (if (it.context == PracticeContext.EXAMPLE) it.answer else it.word).lowercase() in missedWords }, "the questions are on the missed words only")
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Question 1 of ${again.size}").fetchSemanticsNodes().isNotEmpty() }
    }
}
