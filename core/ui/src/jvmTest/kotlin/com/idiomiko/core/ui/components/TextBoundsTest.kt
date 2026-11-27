package com.idiomiko.core.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** Words of a justified paragraph are boxed where they are drawn, on every line. */
@OptIn(ExperimentalTestApi::class)
class TextBoundsTest {

    @Test
    fun wordsOfAJustifiedParagraphAreBoxedWhereTheyAreDrawn() = runDesktopComposeUiTest(width = 600, height = 400) {
        val text = "Envy is an uncomfortable emotion because it combines admiration and resentment, and it rarely leaves."
        var layout: TextLayoutResult? = null
        setContent {
            Text(text, Modifier.width(260.dp), style = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Justify), onTextLayout = { layout = it })
        }
        waitForIdle()
        val result = layout!!
        assertTrue(result.lineCount > 2, "the paragraph wraps")
        val words = Regex("\\S+").findAll(text).map { it.range }.toList()
        // The layout here already reports justified positions, so the boxes are the layout's own.
        words.forEach { word ->
            val expected = result.getPathForRange(word.first, word.last + 1).getBounds()
            val actual = result.textBounds(word.first, word.last + 1)
            assertTrue(abs(expected.left - actual.left) < 0.5f && abs(expected.right - actual.right) < 0.5f, "${text.substring(word)}: $actual, drawn at $expected")
        }
        // A justified line's last word reaches the right edge.
        val lastOnFirstLine = words.last { result.getLineForOffset(it.first) == 0 }
        val right = result.textBounds(lastOnFirstLine.first, lastOnFirstLine.last + 1).right
        assertTrue(abs(right - result.size.width) < 2f, "first line ends at $right of ${result.size.width}")
    }
}
