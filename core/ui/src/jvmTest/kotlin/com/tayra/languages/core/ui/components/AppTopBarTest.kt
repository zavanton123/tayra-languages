package com.tayra.languages.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
    fun theLogoAlwaysShowsTheAppNameAndLeadsHome() {
        val visited = mutableListOf<Route>()
        rule.setContent { MaterialTheme { AppTopBar(title = "Statistics", onNavigate = { visited += it }, section = NavSection.ABOUT) } }
        rule.onNodeWithText("Tayra Languages").performClick()
        assertEquals(listOf<Route>(Route.Home), visited)
        assertEquals(0, rule.onAllNodesWithText("Statistics").fetchSemanticsNodes().size, "the screen's own title stays out of the wide bar")
    }
}
