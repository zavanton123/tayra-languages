package com.tayra.languages.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
}
