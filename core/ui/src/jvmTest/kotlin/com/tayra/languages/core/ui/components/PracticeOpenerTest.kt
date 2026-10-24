package com.tayra.languages.core.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.ui.navigation.LocalPracticeDialog
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.navigation.rememberPracticeOpener
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** A practice opens in the app's dialog over the page on a wide window, and as a page on a phone. */
@OptIn(ExperimentalTestApi::class)
class PracticeOpenerTest {

    private val visited = mutableListOf<Route>()

    private fun ComposeUiTest.show() = setContent {
        TayraTheme {
            ProvideWindowWidth {
                CompositionLocalProvider(LocalPracticeDialog provides { route, onClose -> Button(onClick = onClose) { Text("Practising $route") } }) {
                    val practise = rememberPracticeOpener { visited += it }
                    Button(onClick = { practise(Route.Practice(1, 2)) }) { Text("Practise") }
                }
            }
        }
    }

    @Test
    fun aWideWindowShowsThePracticeOverThePage() = runDesktopComposeUiTest(width = 1400, height = 900) {
        show()
        onNodeWithText("Practise").performClick()
        onNodeWithText("Practising ${Route.Practice(1, 2)}").assertExists().performClick()
        onNodeWithText("Practising ${Route.Practice(1, 2)}").assertDoesNotExist()
        onNodeWithText("Practise").assertExists()
        assertEquals(emptyList(), visited)
    }

    @Test
    fun aPhoneOpensThePracticeAsAPage() = runDesktopComposeUiTest(width = 400, height = 800) {
        show()
        onNodeWithText("Practise").performClick()
        onNodeWithText("Practising", substring = true).assertDoesNotExist()
        assertEquals(listOf<Route>(Route.Practice(1, 2)), visited)
    }
}
