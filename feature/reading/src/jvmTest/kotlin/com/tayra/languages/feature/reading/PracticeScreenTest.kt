package com.tayra.languages.feature.reading

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
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
import com.tayra.languages.core.domain.practice.WordForms
import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.practice.PracticeExampleStore
import com.tayra.languages.core.data.repository.PracticeExampleRepositoryImpl
import com.tayra.languages.core.data.repository.PracticeProgressRepositoryImpl
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
import com.tayra.languages.feature.reading.practice.PracticeResult
import com.tayra.languages.feature.reading.practice.PracticeViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import com.tayra.languages.core.domain.practice.Heard
import com.tayra.languages.core.domain.practice.SpeechRecognition
import com.tayra.languages.core.domain.practice.PracticePreset
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
    private val kept = PracticeExampleRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val termService = TermService(terms, languages)
    private val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), termService)
    private val text = "Eu moro no Brasil. Durante a semana, tenho uma rotina bastante organizada e muito cheia. " +
        "Quando precisamos comprar alguma coisa, passamos no supermercado perto de casa."
    private val learning = mapOf("moro" to "I live", "semana" to "week", "rotina" to "routine", "comprar" to "to buy")
    private val spoken = mutableListOf<String>()
    private var bookId = 0L
    private var languageId = 0L

    private val progress = PracticeProgressRepositoryImpl(provider)

    /** The questions whose word's translation was asked to be put right. */
    private val fixing = mutableListOf<Exercise>()

    /** The read-aloud switch, as the screen keeps it in the settings. */
    private var readAloud by androidx.compose.runtime.mutableStateOf(false)

    /** Hears what [answer] says for the question on show; until [heard] is completed when it is set, it is still listening. */
    private class FakeSpeech(private val available: Boolean = true) : SpeechRecognition {
        var answer: () -> Heard = { Heard.Nothing }
        var heard: CompletableDeferred<Unit>? = null
        var stopped = false
        override suspend fun isAvailable(languageCode: String) = available && languageCode == "pt"
        val hints = mutableListOf<String?>()
        /** Released to let the recognising, after the speaking, finish; none keeps no one waiting. */
        var recognised: CompletableDeferred<Unit>? = null
        override suspend fun listen(languageCode: String, hint: String?, onRecorded: () -> Unit): Heard {
            hints += hint
            heard?.await()
            onRecorded()
            recognised?.await()
            return answer()
        }
        override fun stop() {
            stopped = true
            heard?.complete(Unit)
        }
    }

    private fun ComposeUiTest.show(seed: Int = 7, setup: PracticeSetup = PracticeSetup(), forms: WordForms? = null, speech: SpeechRecognition? = null): PracticeViewModel {
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
        val viewModel = PracticeViewModel(PracticeSource.Page(bookId, 1), setup, PracticeMaterialLoader(books, languages, reading, terms), PracticeExampleStore(kept, examples, WordForms(null, terms), terms, languages, settings, translator), kept, translator, forms, progress, speech, Random(seed))
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    val state by viewModel.state.collectAsState()
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        if (!state.loading) {
                            PracticeContent(
                                state,
                                PracticeActions(
                                    onChoose = viewModel::choose, onMatch = viewModel::match, onTyped = viewModel::setTyped, onSubmit = viewModel::submit, onSkip = viewModel::skip,
                                    onNext = viewModel::next, onRestart = viewModel::start, onRestartMissed = viewModel::startMissed, onSpeak = { spoken += it },
                                    onListen = viewModel::listen, onStopListening = viewModel::stopListening, onTypeInstead = viewModel::typeInstead,
                                    onReadAloud = { readAloud = it },
                                    onFixTranslation = { fixing += it },
                                ),
                                readAloud = readAloud,
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
        val viewModel = PracticeViewModel(PracticeSource.Page(bookId, 1), PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.CHOOSE)), PracticeMaterialLoader(books, languages, reading, terms), PracticeExampleStore(kept, examples, WordForms(null, terms), terms, languages, settings, translator), kept, translator, random = Random(7))
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
        val viewModel = PracticeViewModel(PracticeSource.Book(bookId), PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.CHOOSE)), PracticeMaterialLoader(books, languages, reading, terms), PracticeExampleStore(kept, examples, WordForms(null, terms), terms, languages, settings, translator), kept, translator, random = Random(3))
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
        val viewModel = PracticeViewModel(PracticeSource.Words(ids), PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.HEAR_WORD), responses = setOf(PracticeResponse.CHOOSE)), PracticeMaterialLoader(books, languages, reading, terms), PracticeExampleStore(kept, examples, WordForms(null, terms), terms, languages, settings, translator), kept, translator, random = Random(3))
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
        onNodeWithText("Questions: missing word, hear the word").assertExists()
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

    /** A heard sentence is read aloud and hidden; the question gives the meaning and the word is chosen. */
    @Test
    fun aHeardSentenceIsHiddenAndAsksForTheWordWithAMeaning() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.HEAR_SENTENCE), responses = setOf(PracticeResponse.CHOOSE)))
        val exercise = viewModel.state.value.exercises.first()
        assertEquals(ExerciseType.HEAR_SENTENCE, exercise.type)
        waitUntil(timeoutMillis = 5_000) { exercise.sentence in spoken }
        assertEquals(0, onAllNodesWithTag("practice-sentence").fetchSemanticsNodes().size, "the sentence heard is not shown")
        onNodeWithText("Which word means “${exercise.wordTranslation}”?").assertExists()
        shot("hear-sentence")
        onNodeWithTag("practice-option-${exercise.options.indexOf(exercise.answer)}").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Correct").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("practice-sentence").assertExists()
    }

    /** A missing word's choices include other forms of it, which the practice looks up for the words it asks. */
    @Test
    fun aMissingWordIsChosenAmongItsOtherForms() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val lists = object : FrequencyLists {
            override suspend fun list(languageCode: String) = FrequencyList.parse("comprar\tcomprar compro compra compramos compraram")
        }
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.CHOOSE)), forms = WordForms(lists, terms))
        val exercise = viewModel.state.value.exercises.first { it.answer == "comprar" }
        assertEquals(2, exercise.options.count { it in setOf("compro", "compra", "compramos", "compraram") }, exercise.options.toString())
        val first = viewModel.state.value.exercises.first()
        onNodeWithText("CHOOSE THE MISSING WORD").assertExists()
        shot("form")
        onNodeWithTag("practice-option-${first.options.indexOf(first.answer)}").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Correct").fetchSemanticsNodes().isNotEmpty() }
    }

    /** Words are matched with their meanings: a wrong pair counts against the round, which ends once every word is matched. */
    @Test
    fun wordsAreMatchedWithTheirMeanings() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.MATCHING)))
        val exercise = viewModel.state.value.exercises.single()
        assertEquals(ExerciseType.MATCHING, exercise.type)
        assertEquals(learning.keys, exercise.pairs.map { it.first }.toSet())
        fun tap(text: String) = onNode(hasText(text) and hasClickAction()).performClick()
        // A wrong pair first.
        tap("moro"); tap("week")
        waitForIdle()
        assertEquals(1, viewModel.state.value.wrongPairs)
        shot("matching")
        for ((text, meaning) in learning) { tap(text); tap(meaning) }
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.answered }
        assertEquals(PracticeResult.ALMOST, viewModel.state.value.result)
        onNodeWithText("Every pair matched, with 1 wrong try").assertExists()
    }

    /** A sentence is built from its words, read from its translation, and checked as a whole. */
    @Test
    fun aSentenceIsBuiltFromItsWords() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.BUILD), responses = setOf(PracticeResponse.CHOOSE)))
        val exercise = viewModel.state.value.exercises.single()
        assertEquals("Eu moro no Brasil.", exercise.answer)
        onNodeWithText("PUT THE WORDS IN ORDER").assertExists()
        assertEquals(0, onAllNodesWithTag("practice-sentence").fetchSemanticsNodes().size, "the sentence is what is built")
        for (part in exercise.answer.split(" ")) onNodeWithTag("practice-tile-${exercise.tiles.indexOf(part)}").performClick()
        assertEquals(exercise.answer, viewModel.state.value.typed)
        shot("build")
        onNodeWithText("Check").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Correct").fetchSemanticsNodes().isNotEmpty() }
    }

    /** A sentence typed whole with a word wrong shows, word by word, what was right. */
    @Test
    fun aDictationShowsWhichWordsWereRight() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.DICTATION)))
        val index = viewModel.state.value.exercises.indexOfFirst { it.wholeSentence }
        repeat(index) { viewModel.skip(); viewModel.next() }
        waitForIdle()
        onNodeWithTag("practice-input").performTextInput("Eu mora no Brasil")
        onNodeWithText("Check").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("practice-sentence-check").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("Eu moro mora no Brasil.", onNodeWithTag("practice-sentence-check").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].single().text)
        shot("dictation-check")
    }

    /** Each answer moves the word's practice level: two right answers take a new word from choosing to typing it as well. */
    @Test
    fun answersMoveTheWordsLevel() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.MEANING), length = null, adaptive = true))
        val exercises = viewModel.state.value.exercises
        assertTrue(exercises.all { it.response == PracticeResponse.CHOOSE }, "every word is new")
        exercises.forEach { e ->
            waitUntil(timeoutMillis = 5_000) { viewModel.state.value.current === e }
            viewModel.choose(e.answer)
            viewModel.next()
        }
        val termIds = exercises.mapNotNull { it.termId }.toSet()
        waitUntil(timeoutMillis = 5_000) { runBlocking { progress.progress(termIds).values.sumOf { it.asked } } == exercises.size }
        assertEquals(setOf(1), runBlocking { progress.progress(termIds) }.values.map { it.level }.toSet())
    }

    @Test
    fun everyQuestionCanBeAnsweredAndNothingChangesTheWords() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        // Mixed rather than by level, so new words meet typed questions too.
        val viewModel = show(setup = PracticeSetup(adaptive = false))
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

    /** Said aloud, a gap counts when its word is heard; nothing made out leaves it to try again. */
    @Test
    fun aSpokenAnswerIsHeardAndChecked() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val speech = FakeSpeech()
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.SPEAK)), speech = speech)
        val first = viewModel.state.value.current!!
        assertEquals(PracticeResponse.SPEAK, first.response)
        onNodeWithText("SAY THE MARKED PHRASE, WITH THE MISSING WORD").assertExists()
        onNodeWithTag("practice-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Nothing was heard. Try again.").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(!viewModel.state.value.answered)

        speech.answer = { Heard.Words(first.sentence) }
        onNodeWithTag("practice-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Correct").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Heard: ${first.sentence}").assertExists()
        assertEquals(first.saying, speech.hints.last(), "the recogniser is told the phrase expected")
        shot("spoken-gap")
        viewModel.next()

        val second = viewModel.state.value.current!!
        speech.answer = { Heard.Words(second.saying!!.replace(second.word, "nada")) }
        onNodeWithTag("practice-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Not quite").fetchSemanticsNodes().isNotEmpty() }
        // What was said, word by word against the phrase.
        onNodeWithTag("practice-sentence-check").assertExists()
        viewModel.next()

        // The word alone does not do: the phrase is to be said.
        val third = viewModel.state.value.current!!
        shot("spoken-phrase")
        speech.answer = { Heard.Words(third.word) }
        onNodeWithTag("practice-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Not quite").fetchSemanticsNodes().isNotEmpty() }
    }

    /** A sentence repeated after the voice is heard first and checked word by word; stopping early checks what was said so far. */
    @Test
    fun aSentenceIsRepeatedAfterTheVoice() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val speech = FakeSpeech()
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.SHADOW), responses = setOf(PracticeResponse.SPEAK)), speech = speech)
        val exercise = viewModel.state.value.current!!
        assertEquals(ExerciseType.SHADOW, exercise.type)
        waitUntil(timeoutMillis = 5_000) { exercise.sentence in spoken }
        // The sentence is shown, the phrase to say marked in it, before it is said.
        onNodeWithTag("practice-sentence").assertExists()
        onAllNodesWithTag("practice-type-instead").fetchSemanticsNodes().let { assertTrue(it.isEmpty(), "a phrase to repeat is only said") }
        onAllNodesWithText("Show the answer").fetchSemanticsNodes().let { assertTrue(it.isEmpty(), "and is on show already") }
        shot("shadowing-before")
        val words = exercise.sentence.split(" ")
        speech.heard = CompletableDeferred()
        speech.recognised = CompletableDeferred()
        speech.answer = { Heard.Words(words.dropLast(1).joinToString(" ")) }
        onNodeWithTag("practice-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Listening…").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Stop").performClick()
        // Spoken, and being made out: no button to press meanwhile.
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Recognising…").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithTag("practice-speak").fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
        speech.recognised!!.complete(Unit)
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.answered }
        assertTrue(speech.stopped)
        assertEquals(if (words.size >= 5) PracticeResult.ALMOST else PracticeResult.WRONG, viewModel.state.value.result)
        onNodeWithTag("practice-sentence-check").assertExists()
        shot("shadowing")
    }

    /** A refused microphone turns the rest of the answers to be said into typed ones; one can also be typed by choice. */
    @Test
    fun answersAreTypedWhenTheMicrophoneIsRefused() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val speech = FakeSpeech()
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.SPEAK), length = null), speech = speech)
        onNodeWithTag("practice-type-instead").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("practice-input").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(PracticeResponse.SPEAK, viewModel.state.value.exercises[1].response, "only the one on show is typed")
        viewModel.skip()
        viewModel.next()

        speech.answer = { Heard.Refused }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("practice-speak").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("practice-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("practice-speaking-refused").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("practice-input").assertExists()
        assertTrue(viewModel.state.value.exercises.drop(1).none { it.response == PracticeResponse.SPEAK })
    }

    /** Where nothing can hear the learner, a speaking setup is typed: sentences to repeat are written down instead. */
    @Test
    fun withoutARecogniserSpeakingIsTyped() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(setup = PracticePreset.SPEAKING.setup, speech = FakeSpeech(available = false))
        val state = viewModel.state.value
        assertTrue(PracticeResponse.SPEAK !in state.setup.responses && ExerciseType.SHADOW !in state.setup.types)
        assertTrue(state.exercises.isNotEmpty() && state.exercises.all { it.response == PracticeResponse.TYPE })
        onAllNodesWithTag("practice-speak").fetchSemanticsNodes().let { assertTrue(it.isEmpty()) }
    }

    /** A missing word to choose comes with the sentence's translation as its clue before it is answered. */
    @Test
    fun aGapToChooseShowsTheTranslationBeforeTheAnswer() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.GAP), responses = setOf(PracticeResponse.CHOOSE)))
        val exercise = viewModel.state.value.current!!
        assertTrue(exercise.isChoice)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("practice-translation").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(!viewModel.state.value.answered)
        onNodeWithText("[${exercise.sentence}]").assertExists()
    }

    /** Read aloud, a sentence shown whole is read as soon as the switch is on; a gap's sentence waits for the answer, which it would say. */
    @Test
    fun sentencesAreReadAloudOnceShownWhole() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.MEANING, ExerciseType.GAP), responses = setOf(PracticeResponse.CHOOSE), length = null))
        val meaning = viewModel.state.value.exercises.indexOfFirst { it.type == ExerciseType.MEANING }
        repeat(meaning) { viewModel.skip(); viewModel.next() }
        waitForIdle()
        spoken.clear()
        onNodeWithTag("practice-read-aloud").performClick()
        val sentence = viewModel.state.value.current!!.sentence
        waitUntil(timeoutMillis = 5_000) { sentence in spoken }
        shot("read-aloud")
        val gap = viewModel.state.value.exercises.withIndex().indexOfFirst { (i, e) -> i > meaning && e.type == ExerciseType.GAP }
        repeat(gap - meaning) { viewModel.skip(); viewModel.next() }
        waitForIdle()
        spoken.clear()
        val exercise = viewModel.state.value.current!!
        waitForIdle()
        assertTrue(spoken.isEmpty(), "the gap's sentence would say the answer")
        viewModel.choose(exercise.answer)
        waitUntil(timeoutMillis = 5_000) { exercise.sentence in spoken }
        onNodeWithTag("practice-read-aloud").performClick()
        assertTrue(!readAloud)
    }

    /** After a meaning question the word's translation can be put right, and the practice takes the new one from then on. */
    @Test
    fun aMeaningsTranslationIsPutRightFromTheQuestion() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(setup = PracticeSetup(types = setOf(ExerciseType.MEANING, ExerciseType.HEAR_WORD), responses = setOf(PracticeResponse.CHOOSE), length = null, adaptive = false))
        val index = viewModel.state.value.exercises.indexOfFirst { it.type == ExerciseType.MEANING }
        repeat(index) { viewModel.skip(); viewModel.next() }
        waitForIdle()
        val exercise = viewModel.state.value.current!!
        onAllNodesWithTag("practice-fix-translation").fetchSemanticsNodes().let { assertTrue(it.isEmpty(), "not before the answer, which it would give away") }
        viewModel.choose(exercise.answer)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("practice-fix-translation").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("practice-fix-translation").performClick()
        assertEquals(exercise, fixing.single())

        viewModel.translationFixed(exercise.termId!!, "sentido novo")
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("${exercise.word} — sentido novo").fetchSemanticsNodes().isNotEmpty() }
        val later = viewModel.state.value.exercises.drop(viewModel.state.value.index + 1).filter { it.termId == exercise.termId }
        assertTrue(later.isNotEmpty() && later.all { it.wordTranslation == "sentido novo" })
        later.filter { it.options.isNotEmpty() && it.answer != it.word }.forEach { assertEquals("sentido novo", it.answer); assertTrue("sentido novo" in it.options) }
    }
}
