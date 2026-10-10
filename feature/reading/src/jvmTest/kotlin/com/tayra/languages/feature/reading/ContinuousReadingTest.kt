package com.tayra.languages.feature.reading

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.TermPopupBuilder
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.input.key.Key
import kotlin.test.assertTrue
import kotlin.math.abs
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** The page's play button reads sentence after sentence and turns pages; auto-pause stops after each. */
class ContinuousReadingTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var settings: SettingsRepositoryImpl
    private val sentences = listOf("O lobo dorme.", "A noite é fria.", "O dia chega.")

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun theWholeTextIsReadAcrossPages() {
        show(autoPause = false)
        rule.onNodeWithContentDescription("Read the page").performClick()

        assertEquals(sentences, readUntilSilent())
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Read the page").fetchSemanticsNodes().isNotEmpty() }
    }

    /** A sentence that occurs twice on the page is read in each place in turn, with one highlight at a time. */
    @Test
    fun aRepeatedSentenceIsReadInEachPlace() {
        show(autoPause = false, text = "O lobo dorme. A noite é fria. O lobo dorme.\n---\nO dia chega.")
        rule.onNodeWithContentDescription("Read the page").performClick()

        val current = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, CURRENT_SENTENCE)
        var mostHighlighted = 0
        val heard = readUntilSilent { mostHighlighted = maxOf(mostHighlighted, rule.onAllNodes(current).fetchSemanticsNodes().size) }
        assertEquals(listOf("O lobo dorme.", "A noite é fria.", "O lobo dorme.", "O dia chega."), heard)
        assertEquals(1, mostHighlighted, "only the sentence being read is highlighted, never its twin too")
    }

    @Test
    fun autoPauseStopsAfterEachSentence() {
        show(autoPause = true)
        rule.onNodeWithContentDescription("Auto-pause on").fetchSemanticsNode()

        rule.onNodeWithContentDescription("Read the page").performClick()
        assertEquals(listOf("O lobo dorme."), readUntilSilent())
        // Stopped after the sentence, and still on it.
        Thread.sleep(500)
        assertTrue(highlighted("O lobo dorme."), "the highlight stays on the sentence just read")
        rule.onNodeWithContentDescription("Read the page").performClick()
        assertEquals(listOf("A noite é fria."), readUntilSilent())
        assertTrue(highlighted("A noite é fria."))
    }

    /** Coming back to the screen (from the examples, say) finds reading where it stopped: play goes on from that sentence, not the first. */
    @Test
    fun comingBackToTheScreenKeepsTheSentenceReadingStoppedOn() {
        val shown = androidx.compose.runtime.mutableStateOf(true)
        show(autoPause = true, shown = shown)
        rule.onNodeWithContentDescription("Read the page").performClick()
        assertEquals(listOf("O lobo dorme."), readUntilSilent())
        rule.onNodeWithContentDescription("Read the page").performClick()
        assertEquals(listOf("A noite é fria."), readUntilSilent())
        assertTrue(highlighted("A noite é fria."))

        shown.value = false
        rule.waitForIdle()
        shown.value = true
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("Read the page").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(highlighted("A noite é fria."), "the sentence reading stopped on is still marked")
        rule.onNodeWithContentDescription("Read the page").performClick()
        assertEquals(listOf("A noite é fria."), readUntilSilent(), "reading goes on from where it stopped")
    }

    /** A sentence played with its own button and heard to the end counts as read: Space goes on with the next. */
    @Test
    fun spaceAfterASentenceButtonGoesOnToTheNextSentence() {
        show(autoPause = true)
        fun press(key: Key) = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { pressKey(key) }

        rule.onAllNodesWithContentDescription("Play sentence")[0].performClick()
        assertEquals(listOf("O lobo dorme."), readUntilSilent())
        assertTrue(highlighted("O lobo dorme."))
        press(Key.Spacebar)
        assertEquals(listOf("A noite é fria."), readUntilSilent())
        assertTrue(highlighted("A noite é fria."))
    }

    /** Whether the highlighted sentence is [sentence]. */
    private fun highlighted(sentence: String): Boolean {
        rule.waitForIdle()
        val current = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, CURRENT_SENTENCE)
        return rule.onAllNodes(current and hasAnyDescendant(hasText(sentence, substring = true))).fetchSemanticsNodes().isNotEmpty()
    }

    /** The keyboard drives reading aloud: Space plays and goes on after an auto-pause, A goes back, Q and Shift+→ too. */
    @Test
    fun theKeyboardControlsReadingAloud() {
        show(autoPause = true)
        fun press(key: Key, shift: Boolean = false) = rule.onRoot().performKeyInput {
            if (shift) keyDown(Key.ShiftLeft)
            pressKey(key)
            if (shift) keyUp(Key.ShiftLeft)
        }

        press(Key.Spacebar)
        assertEquals(listOf("O lobo dorme."), readUntilSilent())
        assertTrue(highlighted("O lobo dorme."))
        // After an auto-pause Space moves the highlight on and reads the next sentence.
        press(Key.Spacebar)
        assertEquals(listOf("A noite é fria."), readUntilSilent())
        assertTrue(highlighted("A noite é fria."))
        press(Key.A)
        assertEquals(listOf("O lobo dorme."), readUntilSilent())

        press(Key.Q)
        rule.waitUntil(2_000) { rule.onAllNodesWithContentDescription("Auto-pause off").fetchSemanticsNodes().isNotEmpty() }
        // W resumes reading, and pauses it again.
        press(Key.W)
        rule.waitUntil(2_000) { rule.onAllNodesWithContentDescription("Pause reading").fetchSemanticsNodes().isNotEmpty() }
        press(Key.W)
        rule.waitUntil(2_000) { rule.onAllNodesWithContentDescription("Read the page").fetchSemanticsNodes().isNotEmpty() }

        press(Key.DirectionRight, shift = true)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("O dia chega.", substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Ctrl with + and − sizes the text, with Shift the line height; ↑ pauses and resumes like W. */
    @Test
    fun displayShortcutsAndTheUpArrow() {
        show(autoPause = true, sentenceMillis = 3_000)
        fun press(key: Key, shift: Boolean = false) = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput {
            keyDown(Key.CtrlLeft)
            if (shift) keyDown(Key.ShiftLeft)
            pressKey(key)
            if (shift) keyUp(Key.ShiftLeft)
            keyUp(Key.CtrlLeft)
        }
        val font = settings.current.readingFontScale
        val lines = settings.current.readingLineHeight
        press(Key.Equals)
        rule.waitUntil(2_000) { settings.current.readingFontScale > font }
        press(Key.Minus)
        rule.waitUntil(2_000) { settings.current.readingFontScale == font }
        press(Key.Equals, shift = true)
        rule.waitUntil(2_000) { settings.current.readingLineHeight > lines }
        press(Key.Minus, shift = true)
        rule.waitUntil(2_000) { settings.current.readingLineHeight == lines }
        assertEquals(font, settings.current.readingFontScale, "line-height keys leave the text size alone")

        // Down to the smallest line height and beyond: the text size never moves.
        repeat(8) { press(Key.Minus, shift = true); rule.waitForIdle(); Thread.sleep(80) }
        rule.waitUntil(2_000) { settings.current.readingLineHeight == 1.0f }
        assertEquals(font, settings.current.readingFontScale)
        // Ctrl+0 and Ctrl+Shift+0 put both back.
        press(Key.Equals); press(Key.Equals)
        rule.waitUntil(2_000) { settings.current.readingFontScale > font }
        press(Key.Zero)
        rule.waitUntil(2_000) { settings.current.readingFontScale == 1.0f }
        press(Key.Zero, shift = true)
        rule.waitUntil(2_000) { settings.current.readingLineHeight == 1.6f }
        assertEquals(1.0f, settings.current.readingFontScale, "resetting the line height leaves the text size alone")

        fun tap(key: Key) = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { pressKey(key) }
        fun reading() = rule.onAllNodes(hasContentDescription("Stop")).fetchSemanticsNodes().isNotEmpty()
        tap(Key.Spacebar)
        rule.waitUntil(2_000) { reading() }
        tap(Key.DirectionUp)
        rule.waitUntil(2_000) { !reading() }
        tap(Key.DirectionUp)
        rule.waitUntil(2_000) { reading() }
    }

    /** Next on the page's last sentence stays there: no page turn, and the place is kept. */
    @Test
    fun nextOnTheLastSentenceStaysThere() {
        show(autoPause = true)
        fun press(key: Key) = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { pressKey(key) }

        press(Key.Spacebar)
        assertEquals(listOf("O lobo dorme."), readUntilSilent())
        press(Key.D)
        assertEquals(listOf("A noite é fria."), readUntilSilent())
        press(Key.D)
        Thread.sleep(800)
        rule.waitForIdle()
        assertEquals(emptyList(), readUntilSilent(), "nothing more is read")
        assertTrue(highlighted("A noite é fria."), "the last sentence stays the current one")
        assertEquals(0, rule.onAllNodes(hasText("O dia chega.", substring = true)).fetchSemanticsNodes().size, "the page is not turned")
    }

    /** Pausing holds the sentence where it is: resuming finishes the rest rather than starting over. */
    @Test
    fun pauseResumesWhereItStopped() {
        show(autoPause = true, sentenceMillis = 3_000)
        fun press(key: Key) = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { pressKey(key) }
        fun reading() = rule.onAllNodes(hasContentDescription("Stop")).fetchSemanticsNodes().isNotEmpty()

        press(Key.Spacebar)
        rule.waitUntil(2_000) { reading() }
        Thread.sleep(1_200)
        press(Key.Spacebar)
        rule.waitUntil(1_000) { !reading() }
        Thread.sleep(800)
        assertTrue(highlighted("O lobo dorme."), "the paused sentence stays highlighted")

        press(Key.Spacebar)
        val resumed = System.currentTimeMillis()
        rule.waitUntil(2_000) { reading() }
        rule.waitUntil(5_000) { !reading() }
        val rest = System.currentTimeMillis() - resumed
        assertTrue(rest < 2_500, "the rest of the sentence took $rest ms; starting over would take 3000")
        assertTrue(highlighted("O lobo dorme."), "auto-pause stops on the sentence once it is finished")
    }

    /** On a long page the sentence being read is scrolled to the middle of the reading area. */
    @Test
    fun theSentenceBeingReadIsCentred() {
        val long = (1..30).joinToString(" ") { "Frase número $it." }
        show(autoPause = false, text = long, sentenceMillis = 3_000)
        rule.onNodeWithContentDescription("Read the page").performClick()

        // Jump to sentence 15 with its own button; reading carries on from there.
        rule.onNode(hasText("Frase número 15.", substring = true)).performScrollTo()
        rule.waitForIdle()
        val row = rule.onNode(hasText("Frase número 15.", substring = true)).fetchSemanticsNode().boundsInRoot
        val index = rule.onAllNodes(hasContentDescription("Play sentence")).fetchSemanticsNodes()
            .indexOfFirst { it.boundsInRoot.center.y in row.top..row.bottom }
        rule.onAllNodes(hasContentDescription("Play sentence"))[index].performClick()
        rule.waitUntil(5_000) { sentenceBeingRead(listOf("Frase número 15.")) != null }
        Thread.sleep(800)
        rule.waitForIdle()

        // The Stop button sits in the middle of the row being read.
        // The reader's scrolling area; the closed drawer has one too, off to the left.
        val area = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
            .map { it.boundsInRoot }.first { it.left >= 0f }
        val reading = rule.onNode(hasContentDescription("Stop")).fetchSemanticsNode().boundsInRoot
        assertEquals("Frase número 15.", sentenceBeingRead(listOf("Frase número 15.")))
        assertTrue(abs(reading.center.y - area.center.y) < 30, "the row being read (${reading.center.y}) should sit at the middle (${area.center.y})")
    }

    /** The sentences read aloud, in order, until reading stops for a moment; [onPoll] looks at the screen each time. */
    private fun readUntilSilent(onPoll: () -> Unit = {}): List<String> {
        val heard = mutableListOf<String>()
        var quiet = 0
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            rule.waitForIdle()
            onPoll()
            val now = sentenceBeingRead()
            if (now != null && heard.lastOrNull() != now) heard += now
            quiet = if (now == null && heard.isNotEmpty()) quiet + 1 else 0
            if (quiet >= 15) break
            Thread.sleep(40)
        }
        return heard
    }

    /** The sentence whose button shows Stop, matched by the row it sits in. */
    private fun sentenceBeingRead(among: List<String> = sentences): String? {
        val stop = rule.onAllNodes(hasContentDescription("Stop")).fetchSemanticsNodes().firstOrNull() ?: return null
        val y = stop.boundsInRoot.center.y
        return among.firstOrNull { sentence ->
            rule.onAllNodes(hasText(sentence, substring = true)).fetchSemanticsNodes().any { it.boundsInRoot.top <= y && y <= it.boundsInRoot.bottom }
        }
    }

    /** [shown], when given, hides and shows the screen through a saveable state holder, as the navigation does with a back-stack entry. */
    private fun show(autoPause: Boolean, text: String = "O lobo dorme. A noite é fria.\n---\nO dia chega.", sentenceMillis: Int = 400, shown: androidx.compose.runtime.MutableState<Boolean>? = null) {
        val vm = runBlocking {
            val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-continuous", ".db").also { it.delete() }))
            val languages = LanguageRepositoryImpl(provider)
            val books = BookRepositoryImpl(provider)
            val terms = TermRepositoryImpl(provider)
            settings = SettingsRepositoryImpl(MapSettings())
            settings.update { it.copy(speechEngine = SpeechEngine.PIPER, splitSentences = true, autoPause = autoPause) }
            val termService = TermService(terms, languages)
            val readingService = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), termService)
            val bookService = BookService(books, languages)
            val languageId = languages.save(Language(name = "Portuguese"))
            val bookId = bookService.create(BookDraft(languageId = languageId, title = "T", text = text, wordsPerPage = 500))
            val engine = object : TermTranslationProvider {
                override val name = "Fake"
                override suspend fun suggestTranslation(text: String, language: Language): String? = null
            }
            val offline = object : OfflineDictionary {
                override suspend fun isAvailable(dictionary: DictionaryId) = false
                override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
            }
            val speech = LocalSpeech(listOf(ShortPiper(sentenceMillis)))
            val audio = SentenceAudio(speech, settings, MemorySpeechAudioCache())
            startKoin { modules(module { single { speech }; single<SettingsRepository> { settings }; single { audio } }) }
            ReadingViewModel(
                bookId, 1, readingService, books, termService,
                TermPopupBuilder(terms, languages, readingService), BookStatsService(books, languages, settings, readingService), settings,
                object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null },
                LocalTranslation(null), speech, WordTranslationService(terms, offline, engine, settings), audio,
            )
        }
        rule.setContent {
            if (shown == null) ReadingScreen(bookId = 1, initialPage = 1, onNavigate = {}, onHome = {}, viewModel = vm) else {
                val holder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
                if (shown.value) holder.SaveableStateProvider("reader") { ReadingScreen(bookId = 1, initialPage = 1, onNavigate = {}, onHome = {}, viewModel = vm) }
            }
        }
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("Read the page").fetchSemanticsNodes().isNotEmpty() }
        // Every sentence of the page is ready, so the reading below does not wait on synthesis.
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("Preparing sentence").fetchSemanticsNodes().isEmpty() }
    }

    /** A Piper stand-in whose every sentence is [millis] of silence. */
    private class ShortPiper(private val millis: Int) : LocalSpeechEngine {
        override val engine = SpeechEngine.PIPER
        override val displayName = "Short Piper"
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
            val data = 16_000 * 2 * millis / 1000
            val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + data); put("WAVE".toByteArray())
                put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(16_000); putInt(32_000); putShort(2); putShort(16)
                put("data".toByteArray()); putInt(data)
            }.array()
            return header + ByteArray(data)
        }
    }
}
