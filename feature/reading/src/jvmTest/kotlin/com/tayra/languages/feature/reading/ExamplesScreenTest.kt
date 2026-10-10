package com.tayra.languages.feature.reading

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.text.TextLayoutResult
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPackStore
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.DictionaryRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.feature.terms.examples.ExamplesSearchScreen
import com.tayra.languages.feature.terms.examples.ExamplesSearchViewModel
import com.tayra.languages.feature.terms.form.TermFormKey
import com.tayra.languages.feature.terms.form.TermFormViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import com.tayra.languages.core.data.network.createHttpClient
import com.tayra.languages.core.data.network.KtorRecordingFetcher
import com.tayra.languages.core.domain.service.ExampleRecordings
import kotlinx.coroutines.delay
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
import org.junit.After
import org.junit.Rule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.input.key.Key
import kotlin.test.assertTrue
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.feature.terms.examples.ExampleTerms

class ExamplesScreenTest {

    @get:Rule
    val rule = createComposeRule()

    /** What the fake speech engine was asked to read, as "language:text". */
    private val spoken: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private lateinit var terms: TermRepositoryImpl
    private lateinit var settings: SettingsRepositoryImpl
    private var languageId = 0L

    @After
    fun tearDown() {
        stopKoin()
    }

    /** The examples' font, size and line height are set on the screen, as the reader's and shared with it; the copy button is gone. */
    @Test
    fun theTextSettingsSetTheExamplesAsTheReaders() {
        show(listOf(ExampleSentence(text = "Eu não tenho tempo.", translation = null)))
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Eu não tenho tempo.").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, rule.onAllNodesWithContentDescription("Copy sentence").fetchSemanticsNodes().size, "no copy button")

        rule.onNodeWithContentDescription("Text settings").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("examples-font-size").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("examples-font-size").performSemanticsAction(SemanticsActions.SetProgress) { it(1.5f) }
        rule.waitUntil(5_000) { settings.current.readingFontScale == 1.5f }
        rule.onNodeWithTag("examples-line-height").performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
        rule.waitUntil(5_000) { settings.current.readingLineHeight == 2f }
        val current = com.tayra.languages.core.ui.theme.ReadingFont.byId(settings.current.readingFont)
        rule.onNodeWithText(current.label).performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Open Sans").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Open Sans").performClick()
        rule.waitUntil(5_000) { settings.current.readingFont == "open_sans" }
    }

    /** The term pane beside the results shows the searched term, then a clicked word, until it is closed. */
    @Test
    fun thePaneShowsTheSearchedTermThenTheClickedWord() {
        show(listOf(ExampleSentence(text = "O tempo voa depressa.", translation = "Время летит быстро.")))
        rule.waitUntil(5_000) { termField("tempo") }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("voa depressa", substring = true)).fetchSemanticsNodes().isNotEmpty() }

        val sentence = rule.onAllNodes(hasText("O tempo voa depressa.")).fetchSemanticsNodes().first { SemanticsActions.GetTextLayoutResult in it.config }
        val layouts = mutableListOf<TextLayoutResult>()
        sentence.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val box = layouts.single().getBoundingBox("O tempo voa depressa.".indexOf("voa") + 1)
        rule.onAllNodes(hasText("O tempo voa depressa."))[0].performMouseInput { click(Offset(box.center.x, box.center.y)) }
        rule.waitUntil(5_000) { termField("voa") }

        rule.onNodeWithContentDescription("Close").performClick()
        rule.waitUntil(5_000) { !termField("voa") }
    }

    /** Where [word] sits in the text node showing [sentence]; the first node is the results list's. */
    private fun at(sentence: String, word: String): Offset {
        val node = rule.onAllNodes(hasText(sentence)).fetchSemanticsNodes().first { SemanticsActions.GetTextLayoutResult in it.config }
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single().getBoundingBox(sentence.indexOf(word) + 1).center
    }

    private fun savedTerm(text: String) = runBlocking { terms.findByTextLc(languageId, text) }

    /** The background the text node of [sentence] paints [word] with, or null. */
    private fun backgroundOf(sentence: String, word: String): androidx.compose.ui.graphics.Color? {
        val shown = rule.onAllNodes(hasText(sentence)).fetchSemanticsNodes().first().config[androidx.compose.ui.semantics.SemanticsProperties.Text].single()
        val start = sentence.indexOf(word)
        return shown.spanStyles.lastOrNull { it.start == start && it.end == start + word.length && it.item.background != androidx.compose.ui.graphics.Color.Unspecified }?.item?.background
    }

    /** The text colour the text node of [sentence] gives [word], or null. */
    private fun colorOf(sentence: String, word: String): androidx.compose.ui.graphics.Color? {
        val shown = rule.onAllNodes(hasText(sentence)).fetchSemanticsNodes().first().config[androidx.compose.ui.semantics.SemanticsProperties.Text].single()
        val start = sentence.indexOf(word)
        return shown.spanStyles.lastOrNull { it.start == start && it.end == start + word.length && it.item.color != androidx.compose.ui.graphics.Color.Unspecified }?.item?.color
    }

    /** A right click on a word of a result saves it with that result as its sentence and colours it; another makes it known. */
    @Test
    fun aRightClickSavesAWordOfAnExample() {
        val sentence = "O tempo voa depressa."
        show(listOf(ExampleSentence(text = sentence, translation = null)))
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(sentence)).fetchSemanticsNodes().isNotEmpty() }

        // Words are coloured as on a page: one never saved is unknown, and nothing is stored for it.
        val colors = com.tayra.languages.core.ui.theme.AppThemes.default
        rule.waitUntil(5_000) { backgroundOf(sentence, "voa") == colors.statusColors.background(TermStatus.UNKNOWN) }
        assertNull(savedTerm("voa"))
        // The searched term is the one open in the pane, so it shows as selected.
        assertEquals(colors.selectedText, colorOf(sentence, "tempo"))

        rule.onAllNodes(hasText(sentence))[0].performMouseInput { rightClick(at(sentence, "voa")) }
        rule.waitUntil(5_000) { savedTerm("voa")?.status == TermStatus.NEW_1 }
        assertEquals(sentence, savedTerm("voa")?.sentence)
        val yellow = com.tayra.languages.core.ui.theme.AppThemes.default.statusColors.background(TermStatus.NEW_1)
        rule.waitUntil(5_000) { backgroundOf(sentence, "voa") == yellow }

        rule.onAllNodes(hasText(sentence))[0].performMouseInput { rightClick(at(sentence, "voa")) }
        rule.waitUntil(5_000) { savedTerm("voa")?.status == TermStatus.WELL_KNOWN }
        assertNull(savedTerm("voa")?.sentence)
        rule.waitUntil(5_000) { backgroundOf(sentence, "voa") == null }
    }

    /** The keys walk the results as the reader's sentences: A and D (or the arrows) move the mark, W, S, Space and ↑ ↓ play the marked one. */
    @Test
    fun theKeysWalkAndPlayTheResults() {
        val first = "O tempo voa."
        val second = "Não tenho tempo."
        val third = "Tempo é dinheiro."
        show(listOf(first, second, third).map { ExampleSentence(text = it, translation = null) })
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(third)).fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Preparing speech").fetchSemanticsNodes().isEmpty() }
        fun press(key: Key) = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { pressKey(key) }
        fun marked(sentence: String) = rule.onAllNodes(androidx.compose.ui.test.isSelected() and androidx.compose.ui.test.hasAnyDescendant(hasText(sentence))).fetchSemanticsNodes().isNotEmpty()

        assertEquals(0, rule.onAllNodes(androidx.compose.ui.test.isSelected()).fetchSemanticsNodes().size, "nothing marked at first")
        press(Key.D)
        rule.waitUntil(2_000) { marked(first) }
        press(Key.DirectionRight)
        rule.waitUntil(2_000) { marked(second) }
        press(Key.D)
        rule.waitUntil(2_000) { marked(third) }
        press(Key.D)
        rule.waitForIdle()
        assertTrue(marked(third), "the last result stays marked")
        press(Key.A)
        rule.waitUntil(2_000) { marked(second) }
        press(Key.DirectionLeft)
        rule.waitUntil(2_000) { marked(first) }

        press(Key.W)
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("pt:$first"), spoken.toList().filter { it.endsWith(first) })
        press(Key.Spacebar)
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isEmpty() }
    }

    /** The example being played is kept in the middle of the list, as the reader keeps the sentence being read. */
    @Test
    fun thePlayingExampleIsCentred() {
        val sentences = (1..30).map { "Frase número $it sobre o tempo." }
        show(sentences.map { ExampleSentence(text = it, translation = null) }, speechDelayMs = 0)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(sentences[0])).fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("Preparing speech").fetchSemanticsNodes().isEmpty() }
        fun press(key: Key) = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { pressKey(key) }

        // Walk to the twentieth example and play it.
        repeat(20) { press(Key.D) }
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.isSelected() and androidx.compose.ui.test.hasAnyDescendant(hasText(sentences[19]))).fetchSemanticsNodes().isNotEmpty() }
        press(Key.W)
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        Thread.sleep(800)
        rule.waitForIdle()

        // The results' scrolling list; the pane beside it scrolls too, but holds no results.
        val list = rule.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
            .map { it.boundsInRoot }.maxBy { it.width }
        val playing = rule.onNode(androidx.compose.ui.test.isSelected() and androidx.compose.ui.test.hasAnyDescendant(hasText(sentences[19]))).fetchSemanticsNode().boundsInRoot
        assertTrue(kotlin.math.abs(playing.center.y - list.center.y) < 40, "the playing example (${playing.center.y}) sits in the middle of the list (${list.center.y})")
    }

    /** Playing the marked example after the list was scrolled away from it brings it back, centred. */
    @Test
    fun playingTheMarkedExampleScrollsBackToIt() {
        val sentences = (1..30).map { "Frase número $it sobre o tempo." }
        show(sentences.map { ExampleSentence(text = it, translation = null) }, speechDelayMs = 0)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(sentences[0])).fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("Preparing speech").fetchSemanticsNodes().isEmpty() }
        fun press(key: Key) = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { pressKey(key) }
        fun shown(sentence: String) = rule.onAllNodes(hasText(sentence)).fetchSemanticsNodes().isNotEmpty()

        repeat(20) { press(Key.D) }
        rule.waitUntil(5_000) { shown(sentences[19]) }
        // Back to the top by hand: the marked example leaves the screen (and the composition).
        rule.onNode(androidx.compose.ui.test.hasScrollToIndexAction()).performScrollToIndex(0)
        rule.waitUntil(5_000) { shown(sentences[0]) && !shown(sentences[19]) }

        press(Key.Spacebar)
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { shown(sentences[19]) }
        rule.waitForIdle()
        Thread.sleep(800)
        rule.waitForIdle()
        val list = rule.onAllNodes(androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
            .map { it.boundsInRoot }.maxBy { it.width }
        val playing = rule.onNode(androidx.compose.ui.test.isSelected() and androidx.compose.ui.test.hasAnyDescendant(hasText(sentences[19]))).fetchSemanticsNode().boundsInRoot
        assertTrue(kotlin.math.abs(playing.center.y - list.center.y) < 40, "the playing example (${playing.center.y}) is back in the middle of the list (${list.center.y})")
    }

    /** A right click on the pane's own term among the results shows in the pane as well. */
    @Test
    fun aRightClickOnThePanesTermShowsInThePane() {
        val sentence = "O tempo voa depressa."
        show(listOf(ExampleSentence(text = sentence, translation = null)))
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(sentence)).fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { termField("tempo") }
        fun paneShows(status: TermStatus) = rule.onAllNodes(androidx.compose.ui.test.isSelected() and hasText(status.abbreviation)).fetchSemanticsNodes().isNotEmpty()
        // A word never saved opens in the pane as a new term at 1.
        rule.waitUntil(5_000) { paneShows(TermStatus.NEW_1) }

        // Two right clicks: unknown to 1, then 1 to known; the pane follows.
        rule.onAllNodes(hasText(sentence))[0].performMouseInput { rightClick(at(sentence, "tempo")) }
        rule.waitUntil(5_000) { savedTerm("tempo")?.status == TermStatus.NEW_1 }
        rule.onAllNodes(hasText(sentence))[0].performMouseInput { rightClick(at(sentence, "tempo")) }
        rule.waitUntil(5_000) { savedTerm("tempo")?.status == TermStatus.WELL_KNOWN }
        rule.waitUntil(5_000) { paneShows(TermStatus.WELL_KNOWN) }
    }

    /** Dragging over words of a result opens the phrase in the pane, where a status saves it with the result as its sentence. */
    @Test
    fun draggingOverAnExampleOpensThePhrase() {
        val sentence = "O tempo voa depressa."
        show(listOf(ExampleSentence(text = sentence, translation = null)))
        rule.waitUntil(5_000) { termField("tempo") }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(sentence)).fetchSemanticsNodes().isNotEmpty() }

        rule.onAllNodes(hasText(sentence))[0].performMouseInput {
            moveTo(at(sentence, "voa"))
            press()
            moveTo(at(sentence, "depressa"))
            release()
        }
        // A phrase's words are joined by zero-width spaces in the term field.
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().any { node ->
                androidx.compose.ui.semantics.SemanticsProperties.EditableText in node.config &&
                    node.config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text.replace("\u200B", "") == "voa depressa"
            }
        }

        val three = hasText("3") and androidx.compose.ui.test.hasClickAction()
        rule.waitUntil(5_000) { rule.onAllNodes(three).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(three)[0].performClick()
        rule.waitUntil(5_000) { runBlocking { terms.list(com.tayra.languages.core.domain.repository.TermListFilter(languageId = languageId, minStatus = TermStatus.LEARNING_3, maxStatus = TermStatus.LEARNING_3), com.tayra.languages.core.domain.repository.TermListSort(), 0, 5) }.items.isNotEmpty() }
        val phrase = runBlocking { terms.list(com.tayra.languages.core.domain.repository.TermListFilter(languageId = languageId, minStatus = TermStatus.LEARNING_3, maxStatus = TermStatus.LEARNING_3), com.tayra.languages.core.domain.repository.TermListSort(), 0, 5) }.items.single()
        assertEquals("voa depressa", phrase.displayText)
        assertEquals(sentence, phrase.sentence)
    }

    /** The status shortcuts act on the word under the mouse, as in the reader, and otherwise on the pane's term. */
    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    @Test
    fun statusShortcutsActOnTheHoveredWordOrThePanesTerm() {
        val sentence = "O tempo voa depressa."
        show(listOf(ExampleSentence(text = sentence, translation = null)))
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(sentence)).fetchSemanticsNodes().isNotEmpty() }
        fun press(key: androidx.compose.ui.input.key.Key) = rule.onAllNodes(androidx.compose.ui.test.isRoot())[0].performKeyInput { pressKey(key) }

        rule.onAllNodes(hasText(sentence))[0].performMouseInput { moveTo(at(sentence, "voa")) }
        rule.waitForIdle()
        press(androidx.compose.ui.input.key.Key.Two)
        rule.waitUntil(5_000) { savedTerm("voa")?.status == TermStatus.NEW_2 }
        assertEquals(sentence, savedTerm("voa")?.sentence)
        press(androidx.compose.ui.input.key.Key.K)
        rule.waitUntil(5_000) { savedTerm("voa")?.status == TermStatus.WELL_KNOWN }
        press(androidx.compose.ui.input.key.Key.I)
        rule.waitUntil(5_000) { savedTerm("voa")?.status == TermStatus.IGNORED }
        press(androidx.compose.ui.input.key.Key.U)
        rule.waitUntil(5_000) { savedTerm("voa")?.status == TermStatus.UNKNOWN }

        // A click opens the word in the pane; with the mouse off the words the shortcut goes to that term.
        rule.onAllNodes(hasText(sentence))[0].performMouseInput { click(at(sentence, "depressa")) }
        rule.waitUntil(5_000) { termField("depressa") }
        val selected = com.tayra.languages.core.ui.theme.AppThemes.default.selectedText
        rule.waitUntil(5_000) { colorOf(sentence, "depressa") == selected }
        assertEquals(false, colorOf(sentence, "tempo") == selected, "only the pane's word is selected")
        press(androidx.compose.ui.input.key.Key.Three)
        rule.waitUntil(5_000) { savedTerm("depressa")?.status == TermStatus.LEARNING_3 }
        assertEquals(sentence, savedTerm("depressa")?.sentence)
    }

    /** The examples in the term pane work the same way: a right click saves the word with the example as its sentence. */
    @Test
    fun aRightClickSavesAWordOfThePanesExamples() {
        val sentence = "Eu não tenho tempo."
        show(sentences = emptyList(), paneSentences = listOf(ExampleSentence(text = sentence, translation = null)))
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(sentence)).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasText(sentence))[0].performScrollTo()

        rule.onAllNodes(hasText(sentence))[0].performMouseInput { rightClick(at(sentence, "tenho")) }
        rule.waitUntil(5_000) { savedTerm("tenho")?.status == TermStatus.NEW_1 }
        assertEquals(sentence, savedTerm("tenho")?.sentence)
    }

    /**
     * Examples without a playable recording are read by the chosen speech engine and voice, made
     * ahead like the rest: one with no recording, and one whose recording cannot be downloaded.
     */
    @Test
    fun examplesWithoutAPlayableRecordingAreReadAloud() {
        show(
            listOf(
                ExampleSentence(text = "Eu não tenho tempo.", translation = null),
                // Nothing listens on port 1, so downloading this recording fails.
                ExampleSentence(text = "Acabou o tempo.", translation = null, audioUrl = "http://127.0.0.1:1/audio.mp3"),
            ),
        )
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Read aloud").fetchSemanticsNodes().size == 2 }
        rule.waitUntil(5_000) { spoken.toSet() == setOf("pt:Eu não tenho tempo.", "pt:Acabou o tempo.") }

        rule.onAllNodesWithContentDescription("Read aloud")[1].performClick()
        rule.waitUntil(1_000) { rule.onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, spoken.size, "both were made ahead; the click plays the prepared audio")
    }

    /** A recording is downloaded before it is clicked: its button spins, then plays at once from memory. */
    @Test
    fun recordingsAreDownloadedBeforeTheyAreClicked() {
        val hits = java.util.concurrent.atomic.AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/audio.mp3") { exchange ->
                hits.incrementAndGet()
                Thread.sleep(1_000)
                val body = ByteArray(64)
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }
        try {
            show(
                listOf(
                    ExampleSentence(text = "Acabou o tempo.", translation = null, audioUrl = "http://127.0.0.1:${server.address.port}/audio.mp3"),
                    ExampleSentence(text = "Eu não tenho tempo.", translation = null),
                ),
            )
            rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Loading recording").fetchSemanticsNodes().isNotEmpty() }
            rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Play recording").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(1, hits.get(), "downloaded before any click")

            rule.onNodeWithContentDescription("Play recording").performClick()
            rule.waitForIdle()
            Thread.sleep(300)
            assertEquals(1, hits.get(), "the downloaded recording is played, not downloaded again")
            assertEquals(false, "pt:Acabou o tempo." in spoken, "a recording that arrives is not read aloud")
        } finally {
            server.stop(0)
        }
    }

    /** The term pane's examples follow the same rules as the screen: recordings, else the speech engine. */
    @Test
    fun thePanesExamplesPlayTheSameWay() {
        show(
            sentences = emptyList(),
            paneSentences = listOf(
                ExampleSentence(text = "Eu não tenho tempo.", translation = null),
                ExampleSentence(text = "Acabou o tempo.", translation = null, audioUrl = "http://127.0.0.1:1/audio.mp3"),
            ),
        )
        // The recording cannot be downloaded, so both examples end up read aloud, made ahead.
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Read aloud").fetchSemanticsNodes().size == 2 }
        rule.waitUntil(5_000) { spoken.toSet() == setOf("pt:Eu não tenho tempo.", "pt:Acabou o tempo.") }

        // The examples sit at the bottom of the scrolling pane, below the test window.
        rule.onAllNodesWithContentDescription("Read aloud")[0].performScrollTo().performClick()
        rule.waitUntil(1_000) { rule.onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isNotEmpty() }
    }

    /** An example without a recording is read ahead of time: its button spins, then plays at once. */
    @Test
    fun readAloudExamplesArePreparedBeforeTheyAreClicked() {
        show(listOf(ExampleSentence(text = "Eu não tenho tempo.", translation = null)), speechDelayMs = 1_000)
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Preparing speech").fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Read aloud").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("pt:Eu não tenho tempo."), spoken.toList(), "made before any click")

        rule.onNodeWithContentDescription("Read aloud").performClick()
        rule.waitUntil(1_000) { rule.onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, spoken.size, "the prepared audio is played, not made again")
    }

    private fun show(sentences: List<ExampleSentence>, paneSentences: List<ExampleSentence> = emptyList(), speechDelayMs: Long = 0): ExamplesSearchViewModel {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-examples", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider).also { this.terms = it }
        val settings = SettingsRepositoryImpl(MapSettings()).also { this.settings = it }
        runBlocking { settings.update { it.copy(speechEngine = SpeechEngine.PIPER) } }
        val languageId = runBlocking { languages.save(Language(name = "Portuguese")) }.also { this.languageId = it }
        val examples = object : ExampleSentencesProvider {
            override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult(sentences, sentences.size, null)
            override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
        }
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        val dictionaries = DictionaryService(
            object : DictionaryPackStore {
                override suspend fun installedSize(pack: DictionaryPack): Long? = null
                override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) = error("offline")
                override suspend fun remove(pack: DictionaryPack) {}
            },
            object : DictionaryRepository {
                override suspend fun isAvailable(dictionary: DictionaryId) = false
                override suspend fun entries(dictionary: DictionaryId, wordLc: String) = emptyList<DictionaryEntry>()
                override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
                override suspend fun close(dictionary: DictionaryId) {}
            },
        )
        val termService = TermService(terms, languages)
        val readingService = ReadingService(BookRepositoryImpl(provider), languages, terms, WordsReadRepositoryImpl(provider), termService)
        val exampleTerms = ExampleTerms(readingService, termService)
        startKoin {
            modules(module {
                single { LocalSpeech(listOf(RecordingSpeech(spoken, speechDelayMs))) }
                val cache = MemorySpeechAudioCache()
                single { ExampleRecordings(KtorRecordingFetcher(createHttpClient()), cache) }
                single { SentenceAudio(get(), settings, cache, get()) }
                single<SettingsRepository> { settings }
                single { WordTranslationService(terms, dictionaries, engine, settings) }
                // The pane has its own example list, so its buttons never match the screen's by accident.
                val paneExamples = object : ExampleSentencesProvider {
                    override suspend fun search(query: ExampleSearchQuery) = ExampleSearchResult(paneSentences, paneSentences.size, null)
                    override suspend fun nextPage(nextPage: String, targetLanguage: String) = ExampleSearchResult.EMPTY
                }
                viewModel { (key: TermFormKey) -> TermFormViewModel(key, termService, terms, languages, settings, engine, paneExamples, dictionaries, dictionaries, exampleTerms) }
            })
        }
        val vm = ExamplesSearchViewModel(languageId, "tempo", languages, settings, examples, exampleTerms)
        rule.setContent { ExamplesSearchScreen(languageId, "tempo", onNavigate = {}, onBack = {}, viewModel = vm) }
        return vm
    }

    /** A status set on another screen shows in the results and in a term form once the screen is back in front. */
    @Test
    fun comingBackShowsStatusesChangedElsewhere() {
        val sentence = "Eu não tenho tempo."
        val vm = show(listOf(ExampleSentence(text = sentence, translation = null)))
        rule.waitUntil(5_000) { vm.state.value.words[sentence]?.any { it.status == TermStatus.UNKNOWN } == true }
        val form = org.koin.core.context.GlobalContext.get().get<TermFormViewModel> { org.koin.core.parameter.parametersOf(TermFormKey.ByText(languageId, "tempo", null)) }
        rule.waitUntil(5_000) { !form.state.value.loading && form.state.value.draft.text == "tempo" }
        // The first resume is the one the screen opens with.
        form.onResumed()

        runBlocking { terms.save(com.tayra.languages.core.domain.model.Term(languageId = languageId, text = "tempo", textLc = "tempo", status = TermStatus.LEARNING_3)) }
        vm.onResumed()
        form.onResumed()
        rule.waitUntil(5_000) { vm.state.value.words[sentence]?.any { it.status == TermStatus.LEARNING_3 } == true }
        rule.waitUntil(5_000) { form.state.value.draft.status == TermStatus.LEARNING_3 && form.state.value.draft.id != null }
    }

    private fun termField(text: String) = rule.onAllNodes(hasSetTextAction() and hasText(text)).fetchSemanticsNodes().isNotEmpty()

    /** A Piper stand-in that notes what it reads and returns a short silent WAV. */
    private class RecordingSpeech(private val spoken: MutableList<String>, private val delayMs: Long) : LocalSpeechEngine {
        override val engine = SpeechEngine.PIPER
        override val displayName = "Fake Piper"
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
            delay(delayMs)
            spoken += "$languageCode:$text"
            val data = 1600
            val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + data); put("WAVE".toByteArray())
                put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(16_000); putInt(32_000); putShort(2); putShort(16)
                put("data".toByteArray()); putInt(data)
            }.array()
            return header + ByteArray(data)
        }
    }
}
