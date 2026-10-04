package com.tayra.languages.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
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
    fun theLogoAndNameLeadHome() {
        val visited = mutableListOf<Route>()
        rule.setContent { MaterialTheme { AppTopBar(title = "Tayra Languages", onNavigate = { visited += it }, section = NavSection.BOOKS) } }
        rule.onNodeWithText("Tayra Languages").performClick()
        assertEquals(listOf<Route>(Route.Home), visited)
    }
}
