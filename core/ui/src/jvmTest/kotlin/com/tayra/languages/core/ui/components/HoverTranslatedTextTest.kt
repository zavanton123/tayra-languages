package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HoverTranslatedTextTest {

    @get:Rule
    val rule = createComposeRule()

    private val asked = mutableListOf<String>()

    private fun show() = rule.setContent {
        MaterialTheme {
            HoverTranslatedText(
                AnnotatedString("casa gato"),
                translate = { word -> asked += word; mapOf("casa" to "house", "gato" to "cat")[word] },
                modifier = Modifier.padding(top = 120.dp, start = 40.dp),
            )
        }
    }

    private fun waitFor(text: String) = rule.waitUntil(3_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun showsTheHoveredWordsTranslationAboveIt() {
        show()
        val sentence = rule.onNodeWithText("casa gato").fetchSemanticsNode().boundsInWindow
        rule.onNodeWithText("casa gato").performMouseInput { moveTo(Offset(width * 0.85f, height / 2f)) }
        waitFor("cat")
        val tip = rule.onNodeWithText("cat").fetchSemanticsNode().boundsInWindow
        assertTrue(tip.bottom <= sentence.top + 1f, "tooltip bottom ${tip.bottom} should be above the text top ${sentence.top}")
        assertTrue(tip.center.x > sentence.center.x, "tooltip should sit over the second word")
        assertEquals(listOf("gato"), asked)
    }

    @Test
    fun movesToTheNextWordAndHidesWhenThePointerLeaves() {
        show()
        rule.onNodeWithText("casa gato").performMouseInput { moveTo(Offset(width * 0.15f, height / 2f)) }
        waitFor("house")
        rule.onNodeWithText("casa gato").performMouseInput { moveTo(Offset(width * 0.85f, height / 2f)) }
        waitFor("cat")
        assertTrue(rule.onAllNodesWithText("house").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithText("casa gato").performMouseInput { moveTo(Offset(width * 0.5f, -80f)) }
        rule.waitUntil(3_000) { rule.onAllNodesWithText("cat").fetchSemanticsNodes().isEmpty() }
    }
}
