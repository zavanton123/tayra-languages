package com.tayra.languages.feature.flashcards

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    ): FlashcardsViewModel {
        val languageId = runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = id, nativeLanguage = "ru") }
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
        val viewModel = FlashcardsViewModel(service, languages, settings, translator, noDictionary, WordTranslationService(terms, noDictionary, suggestions, settings), audio, books = books)
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(
                        LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {},
                        LocalFlashcardsDue provides 1,
                    ) {
                        val state by viewModel.state.collectAsState()
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column {
                                AppTopBar(title = "Flashcards", onNavigate = {}, section = NavSection.FLASHCARDS, actions = { if (phone) BrowseAction {} })
                                FlashcardsContent(
                                    state,
                                    speaker = if (withSpeaker) rememberSpeaker(LocalSpeech(emptyList()), settings) else null,
                                    actions = FlashcardActions(onReveal = viewModel::reveal, onAnswer = viewModel::answer, onUndo = viewModel::undo, onSuspend = viewModel::suspendCard, onDelete = viewModel::deleteCard, onRead = onRead, onChooseExample = onChooseExample, onStartWithoutWaiting = viewModel::startWithoutWaiting),
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

    @Test
    fun aCardHidesItsWordUntilTheAnswerIsShown() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Когда нам нужно что-то купить, мы заходим в супермаркет.").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Quando precisamos [...] alguma coisa, passamos no supermercado.").assertExists()
        assertEquals(0, onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithText("купить, покупать").fetchSemanticsNodes().size)
        onNodeWithText("Card 1 of 1").assertExists()
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

    /** Before the first card the screen shows the session's speech being made, and Start now begins at once. */
    @Test
    fun theSessionWaitsForItsSpeechUnlessStartedNow() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        runBlocking { settings.update { it.copy(speechEngine = SpeechEngine.PIPER) } }
        show(audio = SentenceAudio(LocalSpeech(listOf(SlowSpeech())), settings, MemorySpeechAudioCache()), waitForCard = false)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("0 of 2 sounds ready").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithTag("flashcard").fetchSemanticsNodes().size, "no card while the speech is made")
        save("FLASHCARD_PREPARING_SCREENSHOT")
        onNodeWithTag("start-without-waiting").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard").fetchSemanticsNodes().isNotEmpty() }
    }

    /** The translation's place is kept while it is looked up, and after a failed lookup, so the card does not jump when it comes. */
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
        show()
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("answer-Good").performClick()
        // Ten minutes to wait and nothing else to show: the card comes straight back, as in Anki.
        waitForCard("Card 2 of 2")
        assertEquals(TermStatus.NEW_1, status())
        // One answer given, one card still waiting.
        onNodeWithText("Card 2 of 2").assertExists()

        // Space shows the answer, and Space again answers Good.
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard-answer").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("All done for today").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(TermStatus.NEW_2, status(), "the card now waits a day or more")
        onNodeWithText("2 of 2 answered").assertExists()
        save("FLASHCARD_DONE_SCREENSHOT")

        // Back one card: the second answer is taken back and that card shown again.
        onNodeWithText("Previous card").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard").fetchSemanticsNodes().isNotEmpty() }
        waitForCard("Card 2 of 2")
        assertEquals(TermStatus.NEW_1, status())
        onNodeWithText("Card 2 of 2").assertExists()
        // Back once more, with the shortcut (Ctrl+Z by default): the first answer goes too, and the card is new again.
        onNodeWithTag("flashcard").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) } }
        waitForCard("Card 1 of 1")
        onNodeWithText("Card 1 of 1").assertExists()
        // Nothing lies behind the first card.
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.DirectionLeft) }
        waitForIdle()
        onNodeWithText("Card 1 of 1").assertExists()
    }

    /** The shortcut back is the configurable one: given another key, that key goes back and Ctrl+Z no longer does. */
    @Test
    fun theShortcutBackCanBeChanged() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(hotkeys = HotkeyAction.defaults + (HotkeyAction.FLASHCARDS_PREVIOUS to Hotkey("B")))
        onNodeWithText("Show answer").performClick()
        onNodeWithTag("answer-Good").performClick()
        waitForCard("Card 2 of 2")
        onNodeWithTag("flashcard").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) } }
        waitForIdle()
        onNodeWithText("Card 2 of 2").assertExists()
        onNodeWithTag("flashcard").performKeyInput { pressKey(Key.B) }
        waitForCard("Card 1 of 1")
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
        onNodeWithText("A casa nova").assertExists()
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
        waitForCard("Card 2 of 2")
    }

    /** Deleting the card on show deletes its word, and the next card, if any, comes up. */
    @Test
    fun deletingTheCardOnShowDeletesItsWord() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithText("More").performClick()
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
        assertEquals(TermStatus.LEARNING_3, status())
    }
}
