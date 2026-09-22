package com.tayra.languages.feature.reading

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
                    popupContent = { _, _: IntOffset -> },
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
}
