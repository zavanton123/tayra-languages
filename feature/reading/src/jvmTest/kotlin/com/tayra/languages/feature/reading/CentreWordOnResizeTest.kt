package com.tayra.languages.feature.reading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.parse.ParsedToken
import com.tayra.languages.core.domain.parse.numbered
import com.tayra.languages.core.domain.render.TextItemCalculator
import com.tayra.languages.core.ui.theme.AppThemes
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertTrue

class CentreWordOnResizeTest {

    @get:Rule
    val rule = createComposeRule()

    private val language = Language(id = 1, name = "English")

    private fun longPage() = TextItemCalculator.toPage(
        TextItemCalculator.calculate(
            (0 until 400).flatMap { listOf(ParsedToken("word$it", true), ParsedToken(" ", false)) }.numbered(),
            emptyList(),
            language,
        ).items,
    )

    /** The term pane opening narrows the text; the word clicked comes back to the middle of the view. */
    @Test
    fun theMarkedWordIsCentredAfterTheViewNarrows() {
        val page = longPage()
        val word = page.items.indexOfFirst { it.isWord && it.renderText == "word300" }
        val locator = WordLocator()
        val viewport = arrayOfNulls<LayoutCoordinates>(1)
        var width by mutableStateOf(800.dp)
        rule.setContent {
            val scroll = rememberScrollState()
            var size by remember { mutableStateOf(IntSize.Zero) }
            CentreWordOnResize(scroll, word, locator, size) { viewport[0] }
            Box(Modifier.width(width).height(400.dp)) {
                Column(Modifier.onGloballyPositioned { viewport[0] = it; if (it.size != size) size = it.size }.verticalScroll(scroll)) {
                    CompositionLocalProvider(LocalWordLocator provides locator) {
                        ReadingText(
                            page = page,
                            theme = AppThemes.default,
                            showHighlights = true,
                            marked = setOf(word),
                            hovered = null,
                            selection = null,
                            popupItem = null,
                            fontScale = 1f,
                            lineHeight = 1.5f,
                            rightToLeft = false,
                            callbacks = ReadingTextCallbacks(
                                onClick = { _, _ -> }, onTap = {}, onLongPress = {}, onHover = {},
                                onDragStart = {}, onDrag = {}, onDragEnd = { _, _ -> },
                                popupContent = { _, _: androidx.compose.ui.unit.IntRect -> },
                            ),
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        width = 400.dp
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        val bounds = locator.boundsOf(word)!!
        val view = viewport[0]!!.boundsInRoot()
        assertTrue(abs(bounds.center.y - view.center.y) < bounds.height, "word at ${bounds.center.y}, view centre at ${view.center.y}")
    }
}
