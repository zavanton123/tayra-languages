package com.tayra.languages.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.ui.navigation.Route
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
        onNodeWithTag("phone-nav-BOOKS").assertIsSelected()
        onNodeWithText("19", useUnmergedTree = true).assertExists()
        onNodeWithTag("phone-nav-HOME").performClick()
        onNodeWithTag("phone-nav-COURSES").performClick()
        assertEquals(listOf(Route.Home, Route.Courses), visited)
        assertEquals(0, onAllNodesWithTag("phone-nav-TERMS").fetchSemanticsNodes().size)
        onNodeWithTag("phone-nav-SETTINGS").performClick()
        assertEquals(1, more)
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
        onNodeWithText("Clear data").assertExists()
        onNodeWithText("Keyboard shortcuts").assertDoesNotExist()
        onNodeWithTag("more-Statistics").assertExists()
        // The vocabulary comes first, before its settings among the preferences.
        onAllNodesWithTag("more-Vocabulary")[0].performClick()
        assertEquals(listOf<Route>(Route.Terms()), visited)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Word frequency").fetchSemanticsNodes().isEmpty() }
    }
}
