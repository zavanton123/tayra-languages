package com.idiomiko.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.ui.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals

/** The phone's bottom bar marks the current area and counts the flashcards due; More opens the sheet of the other pages, the vocabulary among them. */
@OptIn(ExperimentalTestApi::class)
class PhoneNavigationTest {
    @Test
    fun theBarMarksTheAreaAndLeadsToTheOthers() = runDesktopComposeUiTest(width = 400, height = 800) {
        val visited = mutableListOf<Route>()
        var more = 0
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalFlashcardsDue provides 19) {
                    PhoneNavBar(section = NavSection.BOOKS, onNavigate = { visited += it }, onMore = { more++ })
                }
            }
        }
        onNodeWithTag("phone-nav-READ").assertIsSelected()
        onNodeWithText("19", useUnmergedTree = true).assertExists()
        onNodeWithTag("phone-nav-HOME").performClick()
        assertEquals(listOf<Route>(Route.Home), visited)
        for (gone in listOf("COURSES", "BOOKS", "TERMS")) assertEquals(0, onAllNodesWithTag("phone-nav-$gone").fetchSemanticsNodes().size, gone)
        onNodeWithTag("phone-nav-SETTINGS").performClick()
        assertEquals(1, more)
    }

    /** Read opens a sheet with the courses, the books and the news, as the wide bar's Read menu. */
    @Test
    fun readOpensTheSheetOfWhatThereIsToRead() = runDesktopComposeUiTest(width = 400, height = 900) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { PhoneNavBar(section = NavSection.NEWS, onNavigate = { visited += it }, onMore = {}) } }
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

    @Test
    fun theVocabularyPagesMarkMore() = runDesktopComposeUiTest(width = 400, height = 800) {
        setContent { MaterialTheme { PhoneNavBar(section = NavSection.TERMS, onNavigate = {}, onMore = {}) } }
        onNodeWithTag("phone-nav-SETTINGS").assertIsSelected()
    }

    @Test
    fun theMoreSheetListsThePagesAndNamesTheLearningLanguage() = runDesktopComposeUiTest(width = 400, height = 900) {
        val visited = mutableListOf<Route>()
        var open by mutableStateOf(true)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Croatian"), 1L) {}) {
                    if (open) MoreSheet(onNavigate = { visited += it; open = false }, onDismiss = { open = false })
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Word frequency").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Croatian").assertExists()
        // The plan's four groups; clearing the data and the settings of single features are found from Settings.
        for (group in listOf("LISTEN AND WATCH", "WORDS AND PROGRESS", "OFFLINE RESOURCES", "PREFERENCES AND DATA")) onNodeWithText(group).assertExists()
        // Under Read instead.
        for (page in listOf("more-News", "more-Find books")) onNodeWithTag(page).assertDoesNotExist()
        for (gone in listOf("Clear data", "Keyboard shortcuts", "Flashcard settings", "Vocabulary settings")) onNodeWithText(gone).assertDoesNotExist()
        onNodeWithTag("more-Statistics").assertExists()
        // The Flashcards tab opens the review, so the cards are browsed from here.
        onNodeWithTag("more-Browse flashcards").assertExists()
        onNodeWithTag("more-Vocabulary").performClick()
        assertEquals(listOf<Route>(Route.Terms()), visited)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Word frequency").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun theMoreSheetOffersVideosOnlyWhenTheyAreOn() = runDesktopComposeUiTest(width = 400, height = 900) {
        var videos by mutableStateOf(false)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalVideosEnabled provides videos) { MoreSheet(onNavigate = {}, onDismiss = {}) }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("more-Playlists").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithTag("more-Videos").fetchSemanticsNodes().size)
        // Tests run from the sources, which count as a debug build.
        onNodeWithTag("more-Experimental features").assertExists()
        videos = true
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("more-Videos").fetchSemanticsNodes().isNotEmpty() }
    }
}
