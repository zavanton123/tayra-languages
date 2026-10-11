package com.tayra.languages.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.ui.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals

/** The phone's bottom bar marks the current area and counts the flashcards due; More opens the sheet of the other pages. */
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
        onNodeWithTag("phone-nav-TERMS").performClick()
        onNodeWithTag("phone-nav-COURSES").performClick()
        assertEquals(listOf(Route.Terms(), Route.Courses), visited)
        onNodeWithTag("phone-nav-SETTINGS").performClick()
        assertEquals(1, more)
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
        onNodeWithTag("more-Statistics").performClick()
        assertEquals(listOf<Route>(Route.Stats), visited)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Word frequency").fetchSemanticsNodes().isEmpty() }
    }
}
