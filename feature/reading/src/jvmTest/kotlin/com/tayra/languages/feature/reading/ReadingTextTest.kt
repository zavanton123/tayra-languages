package com.tayra.languages.feature.reading

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toAwtImage
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

    /** Practising translation blurs each sentence under its translation; a click shows it, and a click beside its words hides it again. */
    @Test
    fun practisingTranslationHidesAndShowsEachSentence() {
        var revealed by androidx.compose.runtime.mutableStateOf(emptySet<Int>())
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
                translations = mapOf("Hello world." to com.tayra.languages.core.domain.service.SentenceTranslation.Done("Привет, мир.")),
                practice = true,
                revealed = revealed,
                onToggleRevealed = { revealed = if (it in revealed) revealed - it else revealed + it },
                callbacks = ReadingTextCallbacks(
                    onClick = { index, _ -> clicked.add(index) },
                    onTap = {},
                    onLongPress = {},
                    onHover = {},
                    onDragStart = {},
                    onDrag = {},
                    onDragEnd = { _, _ -> },
                    popupContent = { _, _: androidx.compose.ui.unit.IntRect -> },
                ),
            )
        }
        rule.onNodeWithText("Привет, мир.").assertExists()
        rule.onNodeWithContentDescription("Hidden sentence").assertExists()
        System.getenv("READING_PRACTICE_SCREENSHOT")?.let { javax.imageio.ImageIO.write(rule.onRoot().captureToImage().toAwtImage(), "png", java.io.File(it)) }
        // Hidden: a click on a word shows the sentence instead of looking the word up.
        rule.onNodeWithText("Hello world.").performMouseInput { click(Offset(8f, 10f)) }
        rule.waitUntil(5_000) { revealed == setOf(0) }
        assertEquals(emptyList(), clicked)
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Hidden sentence").fetchSemanticsNodes().isEmpty() }

        // Shown: a word is looked up as usual; a click beside the words hides the sentence again.
        rule.onNodeWithText("Hello world.").performMouseInput { click(Offset(8f, 10f)) }
        rule.waitForIdle()
        assertEquals(listOf(0), clicked)
        rule.onNodeWithText("Hello world.").performMouseInput { click(Offset(400f, 10f)) }
        rule.waitUntil(5_000) { revealed.isEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Hidden sentence").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Hidden sentence").performClick()
        rule.waitUntil(5_000) { revealed == setOf(0) }
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
