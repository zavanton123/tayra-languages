package com.tayra.languages.feature.frequency

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.frequency.FrequencyList
import com.tayra.languages.core.domain.frequency.RankedWord
import com.tayra.languages.core.domain.frequency.WordFrequencyOverview
import com.tayra.languages.core.domain.frequency.WordKnowledge
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class WordFrequencyScreenTest {

    private val list: FrequencyList = runBlocking { ResourceFrequencyLists().list("pt")!! }

    /** The first 60 words known, then some being learned and a couple ignored, as a reader a few weeks in might have. */
    private fun status(rank: Int): TermStatus = when {
        rank <= 60 -> TermStatus.WELL_KNOWN
        rank % 7 == 0 && rank < 400 -> TermStatus.NEW_1
        rank % 11 == 0 && rank < 400 -> TermStatus.LEARNING_3
        rank == 75 || rank == 140 -> TermStatus.IGNORED
        rank % 5 == 0 && rank < 200 -> TermStatus.WELL_KNOWN
        else -> TermStatus.UNKNOWN
    }

    private val overview = WordFrequencyOverview("Portuguese", list.source, list.words.map { RankedWord(it, status(it.rank)) })
    private val opened = mutableListOf<String>()
    private val levelsSet = mutableListOf<Int>()

    private fun ComposeUiTest.show() = setContent {
        var state by remember { mutableStateOf(WordFrequencyUiState(loading = false, languageId = 1, languageName = "Portuguese", overview = overview)) }
        TayraTheme {
            ProvideWindowWidth {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column {
                        AppTopBar(title = "Word frequency", onNavigate = {}, section = NavSection.TERMS)
                        WordFrequencyContent(
                            state,
                            onToggle = { k -> state = state.copy(shown = (if (k in state.shown) state.shown - k else state.shown + k).ifEmpty { WordKnowledge.entries.toSet() }) },
                            onSearch = { state = state.copy(search = it) },
                            onOpen = { opened += it },
                            translate = { "<$it>" },
                            onSetLevel = { levelsSet += it },
                        )
                    }
                }
            }
        }
    }

    @Test
    fun thePortugueseListIsBundled() = runBlocking {
        assertEquals(10_000, list.words.size)
        assertTrue(list.source.startsWith("wordfreq"), list.source)
        val top = list.words.take(100).map { it.word }
        listOf("de", "ser", "ter", "estar", "fazer", "dizer", "casa").forEach { assertTrue(it in list.words.take(150).map { w -> w.word }, "$it is among the most common") }
        assertTrue(listOf("foi", "disse", "do", "minha", "the").none { it in top }, "forms and foreign words are not words of their own")
        assertTrue("disse" in list.words.first { it.word == "dizer" }.forms)
        assertNull(ResourceFrequencyLists().list("xx"))
    }

    @Test
    fun everyLanguageTheAppTeachesHasAList() = runBlocking {
        val lists = ResourceFrequencyLists()
        for (name in LanguageCatalog.targetLanguages) {
            val code = LanguageCodes.codeFor(name)!!
            val list = lists.list(code)
            assertTrue(list != null && list.words.size >= 3_000, "$name has a list of at least 3,000 words")
            assertTrue(list.source.isNotBlank(), "$name names its source")
            assertEquals(list.words.size, list.words.map { it.key }.toSet().size, "$name lists each word once")
        }
        val german = lists.list("de")!!.words.take(300).map { it.word }
        assertTrue("Jahr" in german && "haben" in german, "German nouns keep their capital, verbs do not")
    }

    @Test
    fun wordsAreBandedByRankAndColouredByStatus() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        waitForIdle()
        System.getenv("FREQUENCY_SCREENSHOT")?.let { save(it) }
        onNodeWithText("Ranks 1–100").assertExists()
        onNodeWithText("Your level").assertExists()
        onNodeWithTag("chip-KNOWN").assertExists()
        onNodeWithText(overview.count(WordKnowledge.NEW).let { n -> n.toString().reversed().chunked(3).joinToString(",").reversed() }).assertExists()
        onNodeWithTag("word-dizer").performClick()
        assertEquals(listOf("dizer"), opened)
    }

    @Test
    fun theFiltersAndTheSearchNarrowTheList() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("chip-KNOWN").performClick()
        onNodeWithTag("chip-NEW").performClick()
        waitForIdle()
        System.getenv("FREQUENCY_FILTER_SCREENSHOT")?.let { save(it) }
        assertTrue(onAllNodesWithTag("word-de").fetchSemanticsNodes().isEmpty(), "known words are hidden")
        assertTrue(onAllNodesWithTag("word-casa").fetchSemanticsNodes().isEmpty(), "new words are hidden")

        onNodeWithTag("chip-KNOWN").performClick()
        onNodeWithTag("chip-NEW").performClick()
        onNodeWithTag("frequency-search").performTextInput("disse")
        waitForIdle()
        onNodeWithTag("word-dizer").assertExists()
        val found = onAllNodes(hasTestTagStartingWith("word-")).fetchSemanticsNodes().size
        assertTrue(found in 1..6, "a form finds its word, and only words starting with it: $found")
        assertTrue(onAllNodesWithTag("word-casa").fetchSemanticsNodes().isEmpty())
    }

    /** As in Language Reactor, band 101–200's button sets the level to 100: every word before the band is known. */
    @Test
    fun aBandSetsTheLevelAfterAsking() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithText("Set level to 100").assertExists()
        assertTrue(onAllNodesWithTag("set-level-0").fetchSemanticsNodes().isEmpty(), "the first band's button would not change the level")
        onNodeWithTag("set-level-100").performClick()
        onNodeWithText("Change vocabulary level from 0 to 100?", substring = true).assertExists()
        System.getenv("FREQUENCY_LEVEL_DIALOG_SCREENSHOT")?.let { save(it) }
        onNodeWithText("Cancel").performClick()
        assertEquals(emptyList(), levelsSet)
        onNodeWithTag("set-level-100").performClick()
        onNodeWithText("Yes").performClick()
        assertEquals(listOf(100), levelsSet)
    }

    @Test
    fun theRankIndexJumpsToABand() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("rank-5000").performClick()
        waitForIdle()
        onNodeWithText("Ranks 5001–5100").assertExists()
        System.getenv("FREQUENCY_JUMP_SCREENSHOT")?.let { save(it) }
    }

    private fun hasTestTagStartingWith(prefix: String) =
        SemanticsMatcher("test tag starts with $prefix") { node -> node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}
