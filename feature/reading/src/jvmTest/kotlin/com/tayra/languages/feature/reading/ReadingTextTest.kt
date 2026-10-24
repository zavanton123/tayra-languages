package com.tayra.languages.feature.reading

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.getBoundsInRoot
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
import androidx.compose.ui.test.performTouchInput
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

    private fun twoSentencePage() = TextItemCalculator.toPage(
        TextItemCalculator.calculate(
            listOf(
                ParsedToken("Hello", true), ParsedToken(".", false, true),
                ParsedToken(" ", false), ParsedToken("World", true), ParsedToken("!", false, true),
            ).numbered(),
            emptyList(),
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

    /** Sentence controls occupy one fixed gutter and do not change the item indexes hit in the text. */
    @Test
    fun sentencePlaybackUsesAStableGutterWithoutBreakingWordSelection() {
        val spoken = mutableListOf<Int>()
        val clicked = mutableListOf<Int>()
        rule.setContent {
            ReadingText(
                page = twoSentencePage(),
                theme = AppThemes.default,
                showHighlights = true,
                marked = emptySet(),
                hovered = null,
                selection = null,
                popupItem = null,
                fontScale = 1.3f,
                lineHeight = 1.5f,
                rightToLeft = false,
                splitSentences = true,
                onSpeakSentence = spoken::add,
                callbacks = ReadingTextCallbacks(
                    onClick = { index, _ -> clicked += index },
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

        val controls = rule.onAllNodesWithContentDescription("Play sentence")
        controls[0].assertExists().performClick()
        controls[1].assertExists().performClick()
        assertEquals(listOf(0, 1), spoken, "each gutter control keeps its sentence index")

        val firstControl = controls[0].getBoundsInRoot()
        val secondControl = controls[1].getBoundsInRoot()
        val hello = rule.onNodeWithText("Hello.").getBoundsInRoot()
        val world = rule.onNodeWithText("World!").getBoundsInRoot()
        assertTrue(firstControl.right - firstControl.left >= 48.dp && firstControl.bottom - firstControl.top >= 48.dp, "sentence controls keep a 48dp touch target")
        assertTrue(secondControl.right - secondControl.left >= 48.dp && secondControl.bottom - secondControl.top >= 48.dp, "every gutter control keeps the minimum touch target")
        assertEquals(firstControl.left, secondControl.left, "sentence controls share a fixed gutter")
        assertEquals(hello.left, world.left, "sentence text starts at one stable edge")
        assertTrue(firstControl.right <= hello.left, "the control stays before the text instead of splitting its words")
        assertTrue(secondControl.right <= world.left, "the second control stays outside its sentence too")

        rule.onNodeWithText("Hello.").performMouseInput { click(Offset(8f, 10f)) }
        rule.onNodeWithText("World!").performMouseInput { click(Offset(8f, 10f)) }
        rule.waitForIdle()
        assertEquals(listOf(0, 3), clicked, "the audio gutter does not offset the page's word indexes")
    }

    /** Side by side, the sentence takes the wider column, play button and all, and its translation the narrower. */
    @Test
    fun sideBySideGivesTheTextTheWiderColumn() {
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
                sideBySide = true,
                onSpeakSentence = {},
                callbacks = ReadingTextCallbacks(
                    onClick = { _, _ -> },
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
        val translation = rule.onAllNodesWithTag("side-translation")[0].fetchSemanticsNode().boundsInRoot
        // The text column runs from the row's start to the gap before the translation.
        val text = translation.left
        assertTrue(text > translation.width * 1.3f, "the text column ($text) is wider than the translation's (${translation.width})")
    }

    /** Practising translation blurs each sentence under its translation; the mouse shows it while over it, and by touch a tap shows it and a tap beside its words hides it. */
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
                onRevealed = { no, shown -> revealed = if (shown) revealed + no else revealed - no },
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
        // The mouse over the sentence shows it; a click then looks the word up as usual.
        rule.onNodeWithText("Hello world.").performMouseInput { moveTo(Offset(8f, 10f)) }
        rule.waitUntil(5_000) { revealed == setOf(0) }
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Hidden sentence").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Hello world.").performMouseInput { click(Offset(8f, 10f)) }
        rule.waitForIdle()
        assertEquals(listOf(0), clicked)
        // The pointer leaving, down to the translation, blurs it again.
        rule.onNodeWithText("Привет, мир.").performMouseInput { moveTo(Offset(8f, 8f)) }
        rule.waitUntil(5_000) { revealed.isEmpty() }
        rule.waitUntil(5_000) { rule.onAllNodesWithContentDescription("Hidden sentence").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Привет, мир.").performMouseInput { exit() }

        // By touch: a tap on the blur shows the sentence, a tap beside its words hides it.
        rule.onNodeWithContentDescription("Hidden sentence").performTouchInput { click(Offset(8f, 10f)) }
        rule.waitUntil(5_000) { revealed == setOf(0) }
        rule.onNodeWithText("Hello world.").performTouchInput { click(Offset(400f, 10f)) }
        rule.waitUntil(5_000) { revealed.isEmpty() }
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
