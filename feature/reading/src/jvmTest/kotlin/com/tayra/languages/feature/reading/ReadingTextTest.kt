package com.tayra.languages.feature.reading

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlin.test.assertTrue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.IntOffset
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.parse.ParsedToken
import com.tayra.languages.core.domain.parse.numbered
import com.tayra.languages.core.domain.render.TextItemCalculator
import com.tayra.languages.core.ui.theme.AppThemes
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ReadingTextTest {

    @get:Rule
    val rule = createComposeRule()

    private val language = Language(id = 1, name = "English")

    private fun page() = TextItemCalculator.toPage(
        TextItemCalculator.calculate(
            listOf(ParsedToken("Hello", true), ParsedToken(" ", false), ParsedToken("world", true), ParsedToken(".", false, true)).numbered(),
            listOf(Term(id = 7, languageId = 1, text = "world", textLc = "world", status = TermStatus.NEW_2)),
            language,
        ).items,
    )

    @Test
    fun hoverAndClickResolveWords() {
        val hovered = mutableListOf<Int?>()
        val clicked = mutableListOf<Int>()
        rule.setContent {
            ReadingText(
                page = page(),
                theme = AppThemes.default,
                showHighlights = true,
                marked = emptySet(),
                hovered = null,
                selection = null,
                popupItem = null,
                fontScale = 1f,
                lineHeight = 1.5f,
                rightToLeft = false,
                callbacks = ReadingTextCallbacks(
                    onClick = { index, _ -> clicked.add(index) },
                    onTap = {},
                    onLongPress = {},
                    onHover = { hovered.add(it) },
                    onDragStart = {},
                    onDrag = {},
                    onDragEnd = { _, _ -> },
                    popupContent = { _, _: androidx.compose.ui.unit.IntRect -> },
                ),
            )
        }
        val text = rule.onNodeWithText("Hello world.")
        text.performMouseInput { moveTo(Offset(8f, 10f)) }
        rule.waitForIdle()
        assertEquals(0, hovered.last(), "first word hovered")

        text.performMouseInput { click(Offset(8f, 10f)) }
        rule.waitForIdle()
        assertEquals(listOf(0), clicked)

        text.performMouseInput { moveTo(Offset(90f, 10f)) }
        rule.waitForIdle()
        assertEquals(2, hovered.last(), "second word hovered")
    }

    @Test
    fun thePopupSitsAboveItsWord() {
        var item by androidx.compose.runtime.mutableStateOf(0)
        rule.setContent {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(top = 160.dp)) {
                ReadingText(
                    page = page(),
                    theme = AppThemes.default,
                    showHighlights = true,
                    marked = emptySet(),
                    hovered = null,
                    selection = null,
                    popupItem = item,
                    fontScale = 1f,
                    lineHeight = 1.5f,
                    rightToLeft = false,
                    callbacks = ReadingTextCallbacks(
                        onClick = { _, _ -> },
                        onTap = {},
                        onLongPress = {},
                        onHover = {},
                        onDragStart = {},
                        onDrag = {},
                        onDragEnd = { _, _ -> },
                        popupContent = { _, word ->
                            androidx.compose.ui.window.Popup(popupPositionProvider = com.tayra.languages.core.ui.components.AboveTargetPositionProvider(word, 6)) {
                                androidx.compose.material3.Text("the card")
                            }
                        },
                    ),
                )
            }
        }
        rule.waitUntil(3_000) { rule.onAllNodesWithText("the card").fetchSemanticsNodes().isNotEmpty() }
        val overHello = rule.onNodeWithText("the card").fetchSemanticsNode().boundsInWindow
        val paragraph = rule.onNodeWithText("Hello world.").fetchSemanticsNode().boundsInWindow
        assertTrue(overHello.bottom <= paragraph.top + 12f, "card bottom ${overHello.bottom} should be above the words at ${paragraph.top}")
        item = 2
        rule.waitForIdle()
        val overWorld = rule.onNodeWithText("the card").fetchSemanticsNode().boundsInWindow
        assertTrue(overWorld.center.x > overHello.center.x + 20f, "the card should follow its word: ${overHello.center.x} then ${overWorld.center.x}")
        assertTrue(overWorld.bottom <= paragraph.top + 12f)
    }
}
