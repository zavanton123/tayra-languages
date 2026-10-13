package com.tayra.languages.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tayra.languages.core.ui.navigation.Route
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AppTopBarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theLogoAlwaysShowsTheAppNameAndLeadsToTheCourses() {
        val visited = mutableListOf<Route>()
        rule.setContent { MaterialTheme { AppTopBar(title = "Statistics", onNavigate = { visited += it }, section = NavSection.TERMS) } }
        rule.onNodeWithText("Tayra Languages").performClick()
        assertEquals(listOf<Route>(Route.Courses), visited)
        assertEquals(0, rule.onAllNodesWithText("Statistics").fetchSemanticsNodes().size, "the screen's own title stays out of the wide bar")
    }

    @Test
    fun theLanguageSelectorShowsTheWordsKnownInTheLanguage() {
        val state = LearningLanguageState(listOf(1L to "Portuguese", 2L to "German"), 1L, knownWords = 1234) {}
        rule.setContent {
            MaterialTheme { CompositionLocalProvider(LocalLearningLanguage provides state) { AppTopBar(title = "Books", onNavigate = {}, section = NavSection.BOOKS) } }
        }
        rule.onNodeWithText("Portuguese (1,234)").assertExists()
        rule.onNodeWithContentDescription("Learning language: Portuguese (1,234)").assertExists()
    }

    /** The selector opens a dialog: the current language first with its words, the rest as tiles that the search narrows; the native one cannot be chosen. */
    @Test
    fun theLanguageDialogListsTheRestToChooseFrom() {
        val chosen = mutableListOf<Long>()
        val state = LearningLanguageState(listOf(1L to "Portuguese", 2L to "German", 3L to "Russian"), 1L, nativeIds = setOf(3L), knownWords = 586) { chosen += it }
        rule.setContent {
            MaterialTheme { CompositionLocalProvider(LocalLearningLanguage provides state) { AppTopBar(title = "Books", onNavigate = {}, section = NavSection.BOOKS) } }
        }
        rule.onNodeWithContentDescription("Learning language: Portuguese (586)").performClick()
        rule.onNodeWithText("Choose a language").assertExists()
        rule.onNodeWithTag("current-language").assertExists()
        rule.onNodeWithText("586 words").assertExists()
        rule.onNodeWithText("2 languages").assertExists()
        rule.onNodeWithText("Your native language").assertExists()
        rule.onNodeWithTag("language-ru").assertIsNotEnabled()

        rule.onNodeWithTag("language-search").performTextInput("ger")
        rule.onNodeWithText("1 language").assertExists()
        assertEquals(0, rule.onAllNodesWithTag("language-ru").fetchSemanticsNodes().size)
        rule.onNodeWithTag("language-de").performClick()
        assertEquals(listOf(2L), chosen)
        assertEquals(0, rule.onAllNodesWithText("Choose a language").fetchSemanticsNodes().size, "the dialog closes with the choice")
    }
}
