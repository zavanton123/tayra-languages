package com.idiomiko.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.ui.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The phone's bottom bar marks the current area and counts the flashcards due; Read, Flashcards and Vocabulary open sheets of their pages. */
@OptIn(ExperimentalTestApi::class)
class PhoneNavigationTest {
    @Test
    fun theBarMarksTheAreaAndLeadsToTheOthers() = runDesktopComposeUiTest(width = 400, height = 800) {
        val visited = mutableListOf<Route>()
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalFlashcardsDue provides 19) {
                    PhoneNavBar(section = NavSection.BOOKS, onNavigate = { visited += it })
                }
            }
        }
        onNodeWithTag("phone-nav-READ").assertIsSelected()
        onNodeWithText("19", useUnmergedTree = true).assertExists()
        System.getenv("PHONE_NAV_SCREENSHOT")?.let { path ->
            waitForIdle()
            javax.imageio.ImageIO.write(onNodeWithTag("phone-nav").captureToImage().toAwtImage(), "png", java.io.File(path))
        }
        onNodeWithTag("phone-nav-HOME").performClick()
        onNodeWithTag("phone-nav-PLAYLISTS").performClick()
        assertEquals(listOf(Route.Home, Route.Playlists), visited)
        // The settings and the account are reached from the top bar's avatar.
        for (gone in listOf("COURSES", "BOOKS", "SETTINGS")) assertEquals(0, onAllNodesWithTag("phone-nav-$gone").fetchSemanticsNodes().size, gone)
        onNodeWithText("More").assertDoesNotExist()
    }

    /** Read opens a sheet with the courses, the books and the news, as the wide bar's Read menu. */
    @Test
    fun readOpensTheSheetOfWhatThereIsToRead() = runDesktopComposeUiTest(width = 400, height = 900) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { PhoneNavBar(section = NavSection.NEWS, onNavigate = { visited += it }) } }
        onNodeWithTag("phone-nav-READ").assertIsSelected()
        onNodeWithTag("phone-nav-READ").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("read-sheet").fetchSemanticsNodes().isNotEmpty() }
        for (page in listOf("Courses", "Books", "Find free books", "Book archive", "News")) onNodeWithTag("read-$page").assertExists()
        System.getenv("READ_SHEET_SCREENSHOT")?.let { path ->
            waitForIdle()
            javax.imageio.ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", java.io.File(path))
        }
        onNodeWithTag("read-Book archive").performClick()
        assertEquals(listOf<Route>(Route.ArchivedBooks), visited)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("read-sheet").fetchSemanticsNodes().isEmpty() }
    }

    /** Flashcards opens a sheet with the review, the browser and the statistics, as the wide bar's Flashcards menu. */
    @Test
    fun flashcardsOpensTheSheetOfItsPages() = runDesktopComposeUiTest(width = 400, height = 900) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { CompositionLocalProvider(LocalFlashcardsDue provides 308) { PhoneNavBar(section = NavSection.HOME, onNavigate = { visited += it }) } } }
        onNodeWithTag("phone-nav-FLASHCARDS").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcards-sheet").fetchSemanticsNodes().isNotEmpty() }
        for (page in listOf("Review flashcards", "Browse flashcards", "Flashcard statistics")) onNodeWithTag("flashcards-$page").assertExists()
        // The review shows how many cards wait, in full, where the tab shows 99+.
        onNode(hasText("308") and hasAnyAncestor(hasTestTag("flashcards-Review flashcards")), useUnmergedTree = true).assertExists()
        assertEquals(0, onAllNodes(hasText("308") and hasAnyAncestor(hasTestTag("flashcards-Browse flashcards")), useUnmergedTree = true).fetchSemanticsNodes().size)
        // A row is lit across the sheet's whole width.
        val row = onNodeWithTag("flashcards-Review flashcards").fetchSemanticsNode().boundsInRoot
        assertEquals(onNodeWithTag("flashcards-sheet").fetchSemanticsNode().boundsInRoot.width, row.width, 1f)
        onNodeWithTag("flashcards-Review flashcards").performMouseInput { moveTo(center) }
        System.getenv("FLASHCARDS_SHEET_SCREENSHOT")?.let { path ->
            waitForIdle()
            javax.imageio.ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", java.io.File(path))
        }
        onNodeWithTag("flashcards-Flashcard statistics").performClick()
        assertEquals(listOf<Route>(Route.FlashcardStats), visited)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcards-sheet").fetchSemanticsNodes().isEmpty() }
    }

    /** Vocabulary opens a sheet with the vocabulary, the word frequency and the statistics, as the wide bar's Vocabulary menu. */
    @Test
    fun vocabularyOpensTheSheetOfItsPages() = runDesktopComposeUiTest(width = 400, height = 900) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { PhoneNavBar(section = NavSection.TERMS, onNavigate = { visited += it }) } }
        onNodeWithTag("phone-nav-TERMS").assertIsSelected()
        onNodeWithTag("phone-nav-TERMS").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("vocabulary-sheet").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(3, onAllNodes(hasTestTagPrefix("vocabulary-") and hasClickAction()).fetchSemanticsNodes().size, "only the three pages")
        System.getenv("VOCABULARY_SHEET_SCREENSHOT")?.let { path ->
            waitForIdle()
            javax.imageio.ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", java.io.File(path))
        }
        onNodeWithTag("vocabulary-Word frequency").performClick()
        assertEquals(listOf<Route>(Route.WordFrequency), visited)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("vocabulary-sheet").fetchSemanticsNodes().isEmpty() }
    }

    /** The settings pages and the account light no tab: they are reached from the top bar's avatar. */
    @Test
    fun theSettingsLightNoTab() = runDesktopComposeUiTest(width = 400, height = 800) {
        var section by mutableStateOf(NavSection.SETTINGS)
        setContent { MaterialTheme { PhoneNavBar(section = section, onNavigate = {}) } }
        for (tab in listOf("HOME", "READ", "PLAYLISTS", "FLASHCARDS", "TERMS")) onNodeWithTag("phone-nav-$tab").assertIsNotSelected()
        section = NavSection.VIDEOS
        onNodeWithTag("phone-nav-READ").assertIsSelected()
    }

    /** With the videos on, the Read sheet offers them after the news. */
    @Test
    fun theReadSheetOffersVideosOnlyWhenTheyAreOn() = runDesktopComposeUiTest(width = 400, height = 900) {
        var videos by mutableStateOf(false)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalVideosEnabled provides videos) { PhoneNavBar(section = NavSection.HOME, onNavigate = {}) }
            }
        }
        onNodeWithTag("phone-nav-READ").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("read-News").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithTag("read-Videos").fetchSemanticsNodes().size)
        videos = true
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("read-Videos").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun hasTestTagPrefix(prefix: String) =
        SemanticsMatcher("test tag starts with $prefix") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true }
}
