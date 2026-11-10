package com.tayra.languages.feature.flashcards

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CompletableDeferred
import androidx.compose.ui.test.onLast
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Clock
import com.tayra.languages.core.domain.flashcards.MemoryState
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.CardSchedule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.tayra.languages.core.domain.practice.Heard
import com.tayra.languages.core.domain.practice.TypedResult
import com.tayra.languages.core.domain.practice.SpeechRecognition
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsSelected
import com.tayra.languages.core.domain.flashcards.AnswerMode
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.onNodeWithContentDescription
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import com.tayra.languages.core.ui.audio.rememberSpeaker
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import com.tayra.languages.core.domain.settings.Hotkey
import com.tayra.languages.core.domain.settings.HotkeyAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalFlashcardsDue
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import com.tayra.languages.core.domain.flashcards.AutoAnswer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.tayra.languages.core.domain.flashcards.Rating
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsDisplayed

/** A card is shown front first, the answer on request, and answering it moves on and moves the word's status. */
@OptIn(ExperimentalTestApi::class)
class FlashcardsScreenTest {

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-flash", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = FlashcardService(FlashcardRepositoryImpl(provider), terms, settings)
    private val sentence = "Quando precisamos comprar alguma coisa, passamos no supermercado."
    private var termId = 0L

    private fun ComposeUiTest.show(
        hotkeys: Map<HotkeyAction, Hotkey?> = HotkeyAction.defaults,
        phone: Boolean = false,
        books: BookRepositoryImpl? = null,
        onRead: (Long) -> Unit = {},
        onChooseExample: (CardContent) -> Unit = {},
        withSpeaker: Boolean = false,
        audio: SentenceAudio? = null,
        waitForCard: Boolean = true,
        translate: suspend (String) -> String? = { "Когда нам нужно что-то купить, мы заходим в супермаркет." },
        /** Saves more words in the language, after the first card's. */
        moreTerms: suspend (languageId: Long) -> Unit = {},
        answerMode: AnswerMode = AnswerMode.SHOW,
        speech: SpeechRecognition? = null,
    ): FlashcardsViewModel {
        val languageId = runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = id, nativeLanguage = "ru", flashcardAnswerMode = answerMode) }
            termId = terms.save(Term(languageId = id, text = "comprar", textLc = "comprar", status = TermStatus.NEW_1, translation = "купить, покупать", sentence = sentence))
            moreTerms(id)
            id
        }
        val noDictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val translator = object : SentenceTranslator {
            override suspend fun translate(text: String, language: Language): String? = translate(text)
        }
        val suggestions = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        val viewModel = FlashcardsViewModel(service, languages, settings, translator, noDictionary, WordTranslationService(terms, noDictionary, suggestions, settings), audio, books = books, speech = speech)
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(
                        LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {},
                        LocalFlashcardsDue provides 1,
                    ) {
                        val state by viewModel.state.collectAsState()
                        var studyingMore by remember { mutableStateOf(false) }
                        if (studyingMore) StudyMoreDialog(viewModel, onAddNew = viewModel::addNewToday, onStart = viewModel::startPass, onDismiss = { studyingMore = false })
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column {
                                AppTopBar(title = "Flashcards", onNavigate = {}, section = NavSection.FLASHCARDS, actions = { if (phone) BrowseAction {} })
                                FlashcardsContent(
                                    state,
                                    speaker = if (withSpeaker) rememberSpeaker(LocalSpeech(emptyList()), settings) else null,
                                    actions = FlashcardActions(onReveal = viewModel::reveal, onAnswer = viewModel::answer, onUndo = viewModel::undo, onSuspend = viewModel::suspendCard, onSkip = viewModel::skipCard, onDelete = viewModel::deleteCard, onRead = onRead, onChooseExample = onChooseExample, onStartWithoutWaiting = viewModel::startWithoutWaiting, onType = viewModel::type, onCheck = viewModel::check, onAnswerMode = viewModel::setAnswerMode, onListen = viewModel::listen, onStopListening = viewModel::stopListening, microphoneLevel = viewModel.microphoneLevel, onTypeInstead = viewModel::typeInstead, onTryAgain = viewModel::tryAgain, onStudyMore = { studyingMore = true }, onEndPass = viewModel::endPass),
                                    hotkeys = hotkeys,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (waitForCard) waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard").fetchSemanticsNodes().isNotEmpty() }
        return viewModel
    }

    private fun ComposeUiTest.save(env: String) {
        System.getenv(env)?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }

    /** Waits for the next card itself: the count changes as an answer is given, before the answered card has gone. */
    private fun ComposeUiTest.waitForCard(count: String) = waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText(count).fetchSemanticsNodes().isNotEmpty() && onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isEmpty()
    }

    private fun status() = runBlocking { terms.getById(termId)!!.status }

    private fun ComposeUiTest.typeAndCheck(text: String) {
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-typed").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard-typed").performTextInput(text)
        onNodeWithTag("flashcard-typed").performKeyInput { pressKey(Key.Enter) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun aWordTypedRightSuggestsGoodAndEnterGivesIt() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(answerMode = AnswerMode.TYPE)
        onNodeWithText("Show answer").assertExists()
        save("FLASHCARD_TYPING_SCREENSHOT")
        typeAndCheck("Comprar")
        onNodeWithText("Correct").assertExists()
        assertEquals(0, onAllNodesWithTag("flashcard-typed-check").fetchSemanticsNodes().size, "nothing to set right")
        onNodeWithTag("answer-Good").assertIsSelected()
        save("FLASHCARD_TYPED_SCREENSHOT")
        onNodeWithTag("flashcards-keys").performKeyInput { pressKey(Key.Enter) }
        waitUntil(timeoutMillis = 5_000) { runBlocking { service.reviews(termId) }.isNotEmpty() }
        val review = runBlocking { service.reviews(termId) }.single()
        assertEquals(Rating.GOOD, review.rating)
        assertEquals(AnswerMode.TYPE, review.answeredBy)
        assertTrue(review.answerTime!! > Duration.ZERO, "timed from the card shown")
    }

    @Test
    fun aSlipSuggestsHardAndAWrongWordAgain() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(answerMode = AnswerMode.TYPE)
        typeAndCheck("cmprar")
        onNodeWithText("Almost: mind the spelling").assertExists()
        save("FLASHCARD_SLIP_SCREENSHOT")
        onNodeWithTag("answer-Hard").assertIsSelected()
        onNodeWithTag("flashcard-typed-check").assertExists()
        // Answered Again, the card is the only one left, so it comes straight back: now a wrong word.
        onNodeWithTag("answer-Again").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isEmpty() }
        typeAndCheck("compra")
        onNodeWithText("Not quite").assertExists()
        onNodeWithTag("answer-Again").assertIsSelected()
    }

    @Test
    fun theFieldKeepsTheKeysTypedAndNothingTypedOnlyShowsTheAnswer() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(answerMode = AnswerMode.TYPE)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-typed").fetchSemanticsNodes().isNotEmpty() }
        // The shortcuts' letters are the word's while it is typed.
        onNodeWithTag("flashcard-typed").performTextInput("sedb")
        onNodeWithTag("flashcard-typed").assertTextEquals("sedb")
        onNodeWithText("Check").assertExists()
        onNodeWithTag("flashcard-typed").performTextClearance()
        onNodeWithText("Show answer").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithTag("flashcard-verdict").fetchSemanticsNodes().size)
        onNodeWithTag("answer-Again").assertIsSelected()
        onNodeWithTag("flashcards-keys").performKeyInput { pressKey(Key.Enter) }
        waitUntil(timeoutMillis = 5_000) { runBlocking { service.reviews(termId) }.isNotEmpty() }
        val review = runBlocking { service.reviews(termId) }.single()
        assertEquals(Rating.AGAIN, review.rating)
        assertEquals(AnswerMode.SHOW, review.answeredBy, "nothing was typed")
    }

    @Test
    fun theSwitchChangesHowCardsAreAnsweredAndIsRemembered() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        assertEquals(0, onAllNodesWithTag("flashcard-typed").fetchSemanticsNodes().size)
        onNodeWithTag("answer-mode-TYPE").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-typed").fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardAnswerMode == AnswerMode.TYPE }
        onNodeWithTag("answer-mode-SHOW").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-typed").fetchSemanticsNodes().isEmpty() }
        onNodeWithTag("flashcards-keys").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun eachWayOfAnsweringHasItsKeyEvenWhileTheWordIsTyped() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(speech = FakeSpeech())
        onNodeWithTag("flashcards-keys").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Two) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-typed").fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardAnswerMode == AnswerMode.TYPE }
        onNodeWithTag("flashcard-typed").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Three) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-speak").fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardAnswerMode == AnswerMode.SPEAK }
        onNodeWithTag("flashcards-keys").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.One) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-speak").fetchSemanticsNodes().isEmpty() }
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardAnswerMode == AnswerMode.SHOW }
    }

    @Test
    fun aWordTypedWronglyCanBeTriedAgainWithNothingSaved() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(answerMode = AnswerMode.TYPE)
        typeAndCheck("vender")
        onNodeWithText("Not quite").assertExists()
        save("FLASHCARD_TRY_AGAIN_SCREENSHOT")
        onNodeWithTag("flashcard-try-again").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-verdict").fetchSemanticsNodes().isEmpty() }
        assertEquals(emptyList(), runBlocking { service.reviews(termId) }, "nothing is saved by a try")
        // Back on the question, its word hidden again.
        onNodeWithTag("flashcard-answer").assertDoesNotExist()
        typeAndCheck("comprar")
        onNodeWithText("Correct").assertExists()
        onNodeWithTag("flashcard-try-again").assertDoesNotExist()
        onNodeWithTag("answer-Good").assertIsSelected()
        onNodeWithTag("answer-Good").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { service.reviews(termId) }.isNotEmpty() }
        assertEquals(listOf(Rating.GOOD), runBlocking { service.reviews(termId) }.map { it.rating })
    }

    @Test
    fun aSentenceSaidWronglyCanBeTriedAgainByItsKey() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(answerMode = AnswerMode.SPEAK, speech = FakeSpeech(true, Heard.Words("nada disso"), Heard.Words("Quando precisamos comprar alguma coisa")))
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-speak").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-try-again").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcards-keys").performKeyInput { pressKey(Key.T) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-speak").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("", viewModel.state.value.heard)
        assertEquals(emptyList(), runBlocking { service.reviews(termId) })
        onNodeWithTag("flashcard-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.spokenResult == TypedResult.CORRECT }
        onNodeWithTag("flashcard-try-again").assertDoesNotExist()
    }

    @Test
    fun theWordBeingTypedKeepsItsKeys() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(answerMode = AnswerMode.TYPE, hotkeys = HotkeyAction.defaults + (HotkeyAction.FLASHCARDS_MODE_SHOW to Hotkey("Q")))
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-typed").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard-typed").performTextInput("co")
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.typed == "co" }
        onNodeWithTag("flashcard-typed").performKeyInput { pressKey(Key.Q) }
        waitForIdle()
        assertEquals(AnswerMode.TYPE, settings.current.flashcardAnswerMode, "a plain key stays part of the word")
        // The key of the way already in use changes nothing, so what was typed stays.
        onNodeWithTag("flashcard-typed").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Two) } }
        waitForIdle()
        assertEquals("co", viewModel.state.value.typed.take(2))
    }

    /** While listening, a meter shows the microphone is on and whether sound comes through it. */
    @Test
    fun theMicrophoneShowsItHearsSound() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val speech = LoudSpeech()
        show(answerMode = AnswerMode.SPEAK, speech = speech)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-speak").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-microphone").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Listening…").assertExists()
        onNodeWithTag("microphone-status").assertTextEquals("Speak now")
        speech.level.value = 0.7f
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Sound detected").fetchSemanticsNodes().isNotEmpty() }
        save("FLASHCARD_MICROPHONE_SCREENSHOT")
        speech.level.value = 0f
        onNodeWithTag("flashcard-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-microphone").fetchSemanticsNodes().isEmpty() }
    }

    /** Listens until stopped, as loud as [level] says. */
    private class LoudSpeech : SpeechRecognition {
        override val level = MutableStateFlow(0f)
        private val stopped = CompletableDeferred<Unit>()
        override suspend fun isAvailable(languageCode: String) = true
        override suspend fun listen(languageCode: String, hint: String?, longPauses: Boolean, onRecorded: () -> Unit): Heard {
            stopped.await()
            return Heard.Nothing
        }
        override fun stop() {
            stopped.complete(Unit)
        }
    }

    /** Hears [replies] in turn, keeping the hints it was given and whether it waited out long pauses. */
    private class FakeSpeech(private val available: Boolean = true, vararg replies: Heard) : SpeechRecognition {
        private val replies = ArrayDeque(replies.toList())
        val hints = mutableListOf<String?>()
        val longPauses = mutableListOf<Boolean>()
        override suspend fun isAvailable(languageCode: String) = available
        override suspend fun listen(languageCode: String, hint: String?, longPauses: Boolean, onRecorded: () -> Unit): Heard {
            hints += hint
            this.longPauses += longPauses
            onRecorded()
            return replies.removeFirstOrNull() ?: Heard.Nothing
        }
    }

    @Test
    fun theSentenceSaidWithItsWordSuggestsGood() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val speech = FakeSpeech(true, Heard.Words("Quando precisamos comprar alguma coisa passamos no supermercado"))
        show(answerMode = AnswerMode.SPEAK, speech = speech)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-speak").fetchSemanticsNodes().isNotEmpty() }
        save("FLASHCARD_SPEAK_SCREENSHOT")
        // Space starts listening while the sentence is to be said.
        onNodeWithTag("flashcards-keys").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf<String?>(sentence), speech.hints, "the recogniser leans towards the sentence")
        assertEquals(listOf(true), speech.longPauses, "a whole sentence may pause")
        onNodeWithText("Correct").assertExists()
        onNodeWithTag("flashcard-heard-check").assertExists()
        onNodeWithTag("answer-Good").assertIsSelected()
        save("FLASHCARD_SPOKEN_SCREENSHOT")
        onNodeWithTag("flashcards-keys").performKeyInput { pressKey(Key.Enter) }
        waitUntil(timeoutMillis = 5_000) { runBlocking { service.reviews(termId) }.isNotEmpty() }
        val review = runBlocking { service.reviews(termId) }.single()
        assertEquals(Rating.GOOD, review.rating)
        assertEquals(AnswerMode.SPEAK, review.answeredBy)
    }

    @Test
    fun theWordMissedSuggestsAgainAndNothingHeardIsSaidAgain() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val speech = FakeSpeech(true, Heard.Nothing, Heard.Words("Quando precisamos comer alguma coisa"))
        show(answerMode = AnswerMode.SPEAK, speech = speech)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-speak").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Nothing was heard. Try again.").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().size)
        onNodeWithTag("flashcard-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Not quite").assertExists()
        onNodeWithTag("answer-Again").assertIsSelected()
    }

    @Test
    fun aWordThatCannotBeSaidIsTyped() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(answerMode = AnswerMode.SPEAK, speech = FakeSpeech(available = false))
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-typed").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard-speak-unavailable").assertExists()
        assertEquals(0, onAllNodesWithTag("flashcard-speak").fetchSemanticsNodes().size)
    }

    @Test
    fun aRefusedMicrophoneOrTypeInsteadTypesTheWord() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(answerMode = AnswerMode.SPEAK, speech = FakeSpeech(true, Heard.Refused))
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-speak").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard-type-instead").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-typed").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithTag("flashcard-speak-unavailable").fetchSemanticsNodes().size, "typed by choice")
        typeAndCheck("comprar")
        onNodeWithTag("flashcards-keys").performKeyInput { pressKey(Key.Enter) }
        // A new card's Good waits ten minutes; with no other card it comes back early, to be said this time.
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-speak").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard-speak").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-speak-unavailable").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("The microphone could not be used, so type the word.").assertExists()
    }

    @Test
    fun aPhoneOffersTheWaysOfAnsweringAsIcons() = runDesktopComposeUiTest(width = 420, height = 900) {
        show(phone = true, answerMode = AnswerMode.SPEAK, speech = FakeSpeech(true))
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-speak").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("answer-mode-SPEAK").assertExists()
        save("FLASHCARD_PHONE_SPEAK_SCREENSHOT")
        onNodeWithTag("answer-mode-TYPE").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-typed").fetchSemanticsNodes().isNotEmpty() }
        save("FLASHCARD_PHONE_TYPE_SCREENSHOT")
    }

    private fun ComposeUiTest.answerShown(rating: String) {
        onNodeWithTag("flashcards-keys").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("answer-$rating").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("answer-$rating").performClick()
    }

    private fun ComposeUiTest.openStudyMore() {
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("study-more").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("study-more").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("study-more-dialog").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun moreNewCardsCanBeStudiedToday() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(moreTerms = { id ->
            settings.update { it.copy(flashcardNewPerDay = 1) }
            terms.save(Term(languageId = id, text = "casa", textLc = "casa", status = TermStatus.NEW_1, translation = "дом", sentence = "A casa é grande."))
        })
        answerShown("Easy")
        openStudyMore()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("1 new card waits beyond today's limit.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Nothing is saved: each card keeps its schedule and status.").assertDoesNotExist()
        save("STUDY_MORE_SCREENSHOT")
        onNodeWithTag("study-more-start").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("A [...] é grande.").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun reviewAheadGoesOverTheComingDaysAndEndsInTheDaysCards() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        answerShown("Easy")
        openStudyMore()
        onNodeWithTag("study-more-AHEAD").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("0 cards are due in that time.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Nothing is saved: each card keeps its schedule and status.").assertExists()
        onNodeWithTag("study-more-start").assertIsNotEnabled()
        // Easy put the card about a week away.
        repeat(5) { onNodeWithContentDescription("Increase Days ahead").performClick() }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("1 card is due in that time.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("study-more-start").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Review ahead · card 1 of 1").fetchSemanticsNodes().isNotEmpty() }
        val cardBefore = runBlocking { service.cardFor(termId) }
        val statusBefore = runBlocking { terms.getById(termId) }?.status

        answerShown("Again")
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Review ahead · card 2 of 2").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("previous-card").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Review ahead · card 1 of 1").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcards-keys").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("answer-Good").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("answer-Hard").assertDoesNotExist()
        onNodeWithTag("answer-Easy").assertDoesNotExist()
        save("REVIEW_AHEAD_SCREENSHOT")
        onNodeWithTag("answer-Good").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("study-more").fetchSemanticsNodes().isNotEmpty() }

        assertEquals(listOf(Rating.EASY), runBlocking { service.reviews(termId) }.map { it.rating }, "no review is logged")
        assertEquals(cardBefore, runBlocking { service.cardFor(termId) }, "the card keeps its schedule")
        assertEquals(statusBefore, runBlocking { terms.getById(termId) }?.status, "the word keeps its status")
    }

    @Test
    fun aCardSkippedInAPassIsNotPutAwayUntilTomorrow() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        answerShown("Easy")
        val skippedBefore = settings.current.flashcardSkipped
        openStudyMore()
        onNodeWithTag("study-more-PRACTISE").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("1 card to practise.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("study-more-start").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Practise words · card 1 of 1").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("card-menu").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Skip card").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Skip card").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("study-more").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(skippedBefore, settings.current.flashcardSkipped)
    }

    @Test
    fun aPassOfPractiseCanBeLeftForTheDaysCards() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        answerShown("Easy")
        openStudyMore()
        onNodeWithTag("study-more-PRACTISE").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("1 card to practise.").fetchSemanticsNodes().isNotEmpty() }
        // The card's word is at status 4 after Easy; without it ticked there is nothing to practise.
        onNodeWithTag("practise-status-LEARNING_4").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("0 cards to practise.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("practise-status-LEARNING_4").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("1 card to practise.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("study-more-start").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Practise words · card 1 of 1").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("end-pass").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("study-more").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, runBlocking { service.reviews(termId) }.size, "nothing answered in the pass")
    }

    @Test
    fun aBacklogSaysWhichCardsComeFirst() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val repository = FlashcardRepositoryImpl(provider)
        show(moreTerms = { id ->
            val now = Clock.System.now()
            repeat(31) { n ->
                val word = terms.save(Term(languageId = id, text = "palavra$n", textLc = "palavra$n", status = TermStatus.NEW_2, translation = "слово", sentence = "Uma frase com palavra$n."))
                repository.reconcile(now)
                val card = repository.get(word)!!
                repository.save(card.copy(schedule = CardSchedule(CardState.REVIEW, now - 2.days, MemoryState(3.0 + n, 5.0), 3, 0, 2, 0, now - 5.days)))
            }
        })
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("backlog-note").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("You're behind: the cards most at risk come first.").assertExists()
        save("BACKLOG_SCREENSHOT")
    }

    @Test
    fun aCardForgottenOnceTooOftenIsCalledALeech() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val repository = FlashcardRepositoryImpl(provider)
        show(moreTerms = {
            val now = Clock.System.now()
            repository.reconcile(now)
            val card = repository.get(termId)!!
            repository.save(card.copy(schedule = CardSchedule(CardState.REVIEW, now - 1.days, MemoryState(3.0, 5.0), 3, 0, 9, 7, now - 4.days)))
        })
        answerShown("Again")
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("leech-note").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("\"comprar\" was forgotten 8 times and is a leech. A better sentence or a note may help it stick.").assertExists()
        save("LEECH_SCREENSHOT")
    }

    @Test
    fun aCardLeftAloneShowsItsAnswerAndIsAnsweredAsSet() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(moreTerms = {
            settings.update { it.copy(flashcardShowTimer = true, flashcardAutoReveal = 1, flashcardAutoAnswer = 1, flashcardAutoAnswerAction = AutoAnswer.HARD) }
        })
        onNodeWithTag("answer-timer").assertExists()
        save("TIMER_SCREENSHOT")
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 5_000) { runBlocking { service.reviews(termId) }.isNotEmpty() }
        assertEquals(Rating.HARD, runBlocking { service.reviews(termId) }.single().rating)
    }

    @Test
    fun aCardHidesItsWordUntilTheAnswerIsShown() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Когда нам нужно что-то купить, мы заходим в супермаркет.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Quando precisamos [...] alguma coisa, passamos no supermercado.").assertExists()
        assertEquals(0, onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithText("купить, покупать").fetchSemanticsNodes().size)
        onNodeWithText("1 of 1").assertExists()
        save("FLASHCARD_FRONT_SCREENSHOT")

        onNodeWithText("Show answer").performClick()
        onNodeWithText(sentence).assertExists()
        onNodeWithText("купить, покупать").assertExists()
        // The answers carry their names only, without the waits.
        for (answer in listOf("Again", "Hard", "Good", "Easy")) onNodeWithText(answer).assertExists()
        for (gone in listOf("<1m", "<6m", "<10m", "Space")) assertEquals(0, onAllNodesWithText(gone).fetchSemanticsNodes().size, gone)
        save("FLASHCARD_BACK_SCREENSHOT")
    }

    /** An answered card offers other examples; one chosen replaces its sentence and translation, and the card stays answered. */
    @Test
    fun theAnsweredCardTakesAnExampleChosenForIt() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val asked = mutableListOf<Long>()
        val viewModel = show(onChooseExample = { asked += it.termId })
        assertEquals(0, onAllNodesWithTag("choose-example").fetchSemanticsNodes().size, "not before the answer")
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("choose-example").performClick()
        assertEquals(listOf(termId), asked)

        viewModel.replaceSentence("Vou comprar pão amanhã.", "Я куплю хлеб завтра.")
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Vou comprar pão amanhã.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Я куплю хлеб завтра.").assertExists()
        onNodeWithTag("flashcard-answer").assertExists()
        assertEquals("Vou comprar pão amanhã.", runBlocking { terms.getById(termId)!!.sentence })
        assertEquals(4 until 11, viewModel.state.value.card!!.wordRange)
        assertEquals(TermStatus.NEW_1, status(), "nothing answered")
        save("FLASHCARD_CHOOSE_SCREENSHOT")
    }

    /** An example holding another form of the word has that form blanked, and a machine translation when it came without one. */
    @Test
    fun anExampleWithAnotherFormBlanksThatForm() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        onNodeWithText("Show answer").performClick()
        viewModel.replaceSentence("Ela compra pão.", null)
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.card?.sentence == "Ela compra pão." && viewModel.state.value.card?.sentenceTranslation != null }
        assertEquals(4 until 10, viewModel.state.value.card!!.wordRange)
        assertEquals("Когда нам нужно что-то купить, мы заходим в супермаркет.", viewModel.state.value.card!!.sentenceTranslation)
    }

    /** An engine that takes its time over each sound, so the wait for the session's speech can be seen. */
    private class SlowSpeech : LocalSpeechEngine {
        override val engine = SpeechEngine.PIPER
        override val displayName = "Slow"
        override val description = ""
        override val packagesDescription = ""
        override val hasRuntimeSetup = false
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun status() = "ready"
        override suspend fun isReady() = true
        override suspend fun setUp() = "ready"
        override suspend fun packages() = emptyList<SpeechPackage>()
        override suspend fun installPackage(id: String) {}
        override suspend fun removePackage(id: String) {}
        override suspend fun voices(languageCode: String) = emptyList<SpeechVoice>()
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray {
            delay(3_000)
            return text.encodeToByteArray()
        }
    }

    /** Before the first card's sound is made the screen says so, and Start now begins at once. */
    @Test
    fun theSessionWaitsForItsSpeechUnlessStartedNow() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        runBlocking { settings.update { it.copy(speechEngine = SpeechEngine.PIPER) } }
        show(audio = SentenceAudio(LocalSpeech(listOf(SlowSpeech())), settings, MemorySpeechAudioCache()), waitForCard = false)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Making the first card's sound. The others are made in the background while you review.").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithTag("flashcard").fetchSemanticsNodes().size, "no card before its sound is made")
        save("FLASHCARD_PREPARING_SCREENSHOT")
        onNodeWithTag("start-without-waiting").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard").fetchSemanticsNodes().isNotEmpty() }
    }

    /** The translation's place is kept while it is looked up, and after a failed lookup, so the card does not jump when it comes. */
    /** A translation kept with the word is shown as it is, the translator not asked. */
    @Test
    fun aKeptTranslationIsShownWithoutTranslating() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        var asked = 0
        show(translate = { asked++; "made now" }, moreTerms = { runBlocking { terms.updateSentenceTranslation(termId, "When we need to buy something, we stop at the supermarket.") } })
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("When we need to buy something, we stop at the supermarket.").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, asked)
    }

    @Test
    fun theTranslationKeepsItsPlaceWhileItIsLookedUp() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val answer = kotlinx.coroutines.CompletableDeferred<String?>()
        show(translate = { answer.await() })
        onNodeWithTag("flashcard-translation").assertTextEquals("Translating…")
        val before = onNodeWithTag("flashcard-translation").fetchSemanticsNode().boundsInRoot
        answer.complete("Когда нам нужно что-то купить, мы заходим в супермаркет.")
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Когда нам нужно что-то купить, мы заходим в супермаркет.").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(before, onNodeWithTag("flashcard-translation").fetchSemanticsNode().boundsInRoot, "the same place")
    }

    @Test
    fun aFailedTranslationKeepsItsPlaceToo() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(translate = { null })
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("No translation available").fetchSemanticsNodes().isNotEmpty() }
    }

    /** The sentences of the cards coming next are translated while the first is on show. */
    @Test
    fun theNextCardsSentencesAreTranslatedAhead() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val asked = java.util.Collections.synchronizedList(mutableListOf<String>())
        val next = "Ela compra pão todos os dias."
        show(translate = { asked += it; "перевод" }, moreTerms = { id -> terms.save(Term(languageId = id, text = "pão", textLc = "pão", status = TermStatus.NEW_1, sentence = next)) })
        waitUntil(timeoutMillis = 5_000) { next in asked }
    }

    /** The answered card's two round buttons say what they do on hover. */
    @Test
    fun theCardsButtonsNameThemselvesOnHover() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(withSpeaker = true)
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("choose-example").performMouseInput { moveTo(center) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Choose another example").fetchSemanticsNodes().isNotEmpty() }
        save("FLASHCARD_BUTTONS_SCREENSHOT")
        onNodeWithContentDescription("Listen to the sentence").performMouseInput { moveTo(center) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Listen to the sentence").fetchSemanticsNodes().isNotEmpty() }
    }

    /** A phone offers other examples in the card's corner too. */
    @Test
    fun aPhoneOffersAnotherExampleInTheCorner() = runDesktopComposeUiTest(width = 430, height = 932) {
        show(phone = true)
        onNodeWithText("Show answer").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("choose-example").fetchSemanticsNodes().isNotEmpty() }
        save("FLASHCARD_CHOOSE_PHONE_SCREENSHOT")
    }

    /** The back holds the word, its part of speech and translation alone: nothing more is looked up to change it later. */
    @Test
    fun theBackLooksUpNoWiktionaryForms() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        onNodeWithText("Show answer").performClick()
        onNodeWithText("купить, покупать").assertExists()
        assertEquals(0, onAllNodesWithText("Wiktionary contributors · CC BY-SA 4.0").fetchSemanticsNodes().size)
        onNodeWithText("Good").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.answered == 1 }
    }

    @Test
    fun answeringMovesOnAndRaisesTheStatus() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("answer-Good").performClick()
        // Ten minutes to wait and nothing else to show: the card comes straight back, as in Anki.
        waitForCard("1 of 1")
        assertEquals(TermStatus.NEW_1, status())
        // The card answered still waits, and the day's total stays what it was.
        onNodeWithText("1 of 1").assertExists()

        // Space shows the answer, and Space again answers Good.
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        // The last card answered sets off fireworks, caught in flight with the clock held.
        mainClock.autoAdvance = false
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Spacebar) }
        repeat(200) { if (onAllNodesWithText("All done for today").fetchSemanticsNodes().isEmpty()) { Thread.sleep(20); mainClock.advanceTimeByFrame() } }
        mainClock.advanceTimeBy(700)
        onNodeWithTag("confetti").assertExists()
        System.getenv("FLASHCARD_CONFETTI_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", File(it)) }
        mainClock.autoAdvance = true
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("All done for today").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(TermStatus.NEW_2, status(), "the card now waits a day or more")
        onNodeWithText("1 of 1 answered").assertExists()
        save("FLASHCARD_DONE_SCREENSHOT")

        // Back one card: the second answer is taken back and that card shown again.
        onNodeWithText("Previous card").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard").fetchSemanticsNodes().isNotEmpty() }
        waitForCard("1 of 1")
        assertEquals(TermStatus.NEW_1, status())
        onNodeWithText("1 of 1").assertExists()
        // Back once more, with the shortcut (Ctrl+Z by default): the first answer goes too, and the card is new again.
        onNodeWithTag("flashcard").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) } }
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.answered == 0 }
        waitForCard("1 of 1")
        onNodeWithText("1 of 1").assertExists()
        // Nothing lies behind the first card.
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.DirectionLeft) }
        waitForIdle()
        onNodeWithText("1 of 1").assertExists()
    }

    /** Each answer moves the count on, a card still in its steps included, out of the same total. */
    @Test
    fun eachAnswerMovesTheCountOn() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(moreTerms = { id ->
            listOf("pão" to "Eu comprei pão.", "leite" to "Bebo leite.").forEach { (text, sentence) ->
                terms.save(Term(languageId = id, text = text, textLc = text, status = TermStatus.NEW_1, translation = "x", sentence = sentence))
            }
        })
        waitForCard("3 of 3")
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("answer-Good").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.answered == 1 }
        waitForCard("2 of 3")
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("answer-Good").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.answered == 2 }
        waitForCard("2 of 3")
        // Both cards answered are still in their steps, so three wait; the menu counts as the review does.
        assertEquals(3, viewModel.state.value.counts.total)
        waitUntil(timeoutMillis = 5_000) { runBlocking { service.observeDueCount().first() } == 2 }
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("answer-Good").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.answered == 3 }
        waitForCard("1 of 3")
    }

    /** The shortcut back is the configurable one: given another key, that key goes back and Ctrl+Z no longer does. */
    @Test
    fun theShortcutBackCanBeChanged() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(hotkeys = HotkeyAction.defaults + (HotkeyAction.FLASHCARDS_PREVIOUS to Hotkey("P")))
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("answer-Good").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.answered == 1 }
        waitForCard("1 of 1")
        onNodeWithTag("flashcard").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) } }
        waitForIdle()
        assertEquals(1, viewModel.state.value.answered)
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.P) }
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.answered == 0 }
    }

    /** Back from the card's details, the card shows as it is now with its answer still showing; a card left unanswered stays so. */
    @Test
    fun theAnswerStaysShownAfterTheDetailsClose() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        onNodeWithText("Show answer").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.revealed }
        runBlocking { terms.save(terms.getById(termId)!!.copy(translation = "покупать")) }
        viewModel.reloadCard()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.card?.translation == "покупать" }
        assertEquals(true, viewModel.state.value.revealed)
        onNodeWithText("покупать").assertExists()

        // Another visit to the page shows the front first, as before.
        viewModel.load()
        waitUntil(timeoutMillis = 5_000) { !viewModel.state.value.revealed }
    }

    /** The answer keys are the ones set in the shortcuts: given others, those show and answer the card, and the old ones no longer do. */
    @Test
    fun theAnswerKeysCanBeChanged() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show(hotkeys = HotkeyAction.defaults + mapOf(
            HotkeyAction.FLASHCARDS_SHOW_ANSWER to Hotkey("A"),
            HotkeyAction.FLASHCARDS_SHOW_ANSWER_ENTER to null,
            HotkeyAction.FLASHCARDS_EASY to Hotkey("L"),
        ))
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Spacebar) }
        waitForIdle()
        assertEquals(false, viewModel.state.value.revealed, "Space no longer shows the answer")
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.A) }
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.revealed }
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Four) }
        waitForIdle()
        assertEquals(true, viewModel.state.value.revealed, "4 no longer answers Easy")
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.L) }
        waitUntil(timeoutMillis = 5_000) { runBlocking { service.cardFor(termId)!!.schedule.reps } == 1 }
        assertEquals(Rating.EASY, runBlocking { service.reviews(termId).single().rating })
    }

    /** The answer is read aloud when shown, and the reading stops as the card goes away. */
    @Test
    fun movingOnStopsTheReading() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        val events = java.util.Collections.synchronizedList(mutableListOf<FlashcardsEvent>())
        val collecting = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch { viewModel.events.flow.collect { events += it } }
        onNodeWithText("Show answer").performClick()
        waitUntil(timeoutMillis = 5_000) { events.size == 1 }
        assertEquals(FlashcardsEvent.Speak(sentence, "pt"), events[0])
        onNodeWithTag("answer-Good").performClick()
        waitUntil(timeoutMillis = 5_000) { events.size == 2 }
        assertEquals(FlashcardsEvent.StopSpeaking, events[1])
        collecting.cancel()
    }

    /** Once the cards are done, the way on is back into the text read last, not the list of books. */
    @Test
    fun aFinishedSessionGoesBackToTheTextReadLast() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val books = BookRepositoryImpl(provider)
        val read = mutableListOf<Long>()
        show(books = books, onRead = { read += it })
        // Read before the cards, in the language they are in.
        val bookId = runBlocking {
            val id = BookService(books, languages).create(BookDraft(languageId = settings.current.currentLanguageId, title = "A casa nova", text = "A casa é nova. O quarto é grande."))
            ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages)).openPage(id, 1, trackOpen = true)
            id
        }
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("answer-Easy").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("continue-reading").fetchSemanticsNodes().isNotEmpty() }
        // The button alone leads back into the text, without its title under it, nor the vocabulary beside it.
        assertEquals(0, onAllNodesWithText("A casa nova").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithText("Open vocabulary").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithText("Keep reading").fetchSemanticsNodes().size)
        save("FLASHCARD_FINISHED_SCREENSHOT")
        onNodeWithTag("continue-reading").performClick()
        assertEquals(listOf(bookId), read)
    }

    /** A phone has the page's own header, one title, and the short footer. */
    @Test
    fun aPhoneShowsTheCardUnderItsOwnHeader() = runDesktopComposeUiTest(width = 420, height = 900) {
        show(phone = true)
        assertEquals(1, onAllNodesWithText("Flashcards").fetchSemanticsNodes().size, "the title once, in the header")
        onNodeWithTag("browse-flashcards").assertExists()
        for (label in listOf("Previous", "Edit", "More")) onNodeWithText(label).assertExists()
        save("FLASHCARD_PHONE_SCREENSHOT")
        val reveal = onNodeWithText("Show answer").fetchSemanticsNode().boundsInRoot
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("flashcard-answer").assertExists()
        save("FLASHCARD_PHONE_BACK_SCREENSHOT")
        // The answers take Show answer's place: the same top edge and height, so the thumb need not move.
        val good = onNodeWithTag("answer-Good").fetchSemanticsNode().boundsInRoot
        assertEquals(reveal.top, good.top, 1f)
        assertEquals(reveal.height, good.height, 1f)
    }

    /** On a short phone the answer opens below the fold; the card scrolls to it, clear of the answer buttons. */
    @Test
    fun aShortPhoneScrollsTheCardToTheAnswer() = runDesktopComposeUiTest(width = 400, height = 700) {
        show(phone = true)
        onNodeWithText("Show answer").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        waitForIdle()
        // Unclipped: a part scrolled out of the card's frame still counts.
        val answer = onNodeWithTag("flashcard-answer").fetchSemanticsNode().let { it.positionInRoot.y + it.size.height }
        val good = onNodeWithTag("answer-Good").fetchSemanticsNode().boundsInRoot.top
        assertTrue(answer <= good, "the answer ends at $answer, under the buttons at $good")
        save("FLASHCARD_SHORT_PHONE_SCREENSHOT")
    }

    /** With two answer buttons only Again and Good are offered, and the keys of the others do nothing. */
    @Test
    fun twoButtonsLeaveOutHardAndEasy() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        runBlocking { settings.update { it.copy(flashcardTwoButtons = true) } }
        show()
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("answer-Again").assertExists()
        onNodeWithTag("answer-Good").assertExists()
        assertEquals(0, onAllNodesWithTag("answer-Hard").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithTag("answer-Easy").fetchSemanticsNodes().size)
        save("FLASHCARD_TWO_BUTTONS_SCREENSHOT")
        // Easy's key gives nothing; Good's still answers.
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Four) }
        waitForIdle()
        onNodeWithTag("flashcard-answer").assertExists()
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Three) }
        waitForCard("1 of 1")
    }

    /** In a low window the card keeps its place between the progress and the answers, and its content scrolls under a bar. */
    @Test
    fun aCardTallerThanTheWindowScrolls() = runDesktopComposeUiTest(width = 1586, height = 640) {
        show()
        onNodeWithText("Show answer").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("answer-Good").assertIsDisplayed()
        onNodeWithTag("flashcard-answer").performScrollTo().assertIsDisplayed()
        save("FLASHCARD_SCROLL_SCREENSHOT")
    }

    /** Deleting the card on show deletes its word, and the next card, if any, comes up. */
    @Test
    fun deletingTheCardOnShowDeletesItsWord() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithContentDescription("More actions").performClick()
        onNodeWithText("Delete card").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("No cards are due").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(null, runBlocking { terms.getById(termId) })
    }

    @Test
    fun easyKeySendsANewCardDaysAway() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Four) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("All done for today").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(TermStatus.LEARNING_4, status())
    }
}
