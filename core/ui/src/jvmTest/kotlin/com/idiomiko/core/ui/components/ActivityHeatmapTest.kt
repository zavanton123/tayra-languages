package com.idiomiko.core.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/** Pointing at a day of the heatmap names it and its value in a tooltip over its square. */
@OptIn(ExperimentalTestApi::class)
class ActivityHeatmapTest {

    @Test
    fun aDayPointedAtIsNamedInATooltipOverItsSquare() = runDesktopComposeUiTest(width = 1200, height = 500) {
        val today = LocalDate(2026, 11, 18)
        setContent {
            IdiomikoTheme {
                ActivityHeatmapCard(
                    title = "Calendar",
                    subtitle = { "Days" },
                    today = today,
                    levels = { mapOf(today to 4) },
                    fewer = "Fewer",
                    more = "More",
                    describe = { date -> if (date == today) "12 answers" else "No reviews" },
                )
            }
        }
        assertEquals(0, onAllNodesWithTag("chart-tooltip").fetchSemanticsNodes().size, "nothing before pointing")
        val grid = onNodeWithTag("heatmap").fetchSemanticsNode().boundsInRoot
        // Today is a Wednesday: the third of the seven squares in the last column.
        val gap = 4f
        val cell = (grid.height - 6 * gap) / 7
        val square = Offset(grid.width - cell / 2, 2 * (cell + gap) + cell / 2)
        onNodeWithTag("heatmap").performMouseInput { moveTo(square) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("chart-tooltip").fetchSemanticsNodes().isNotEmpty() }
        val tip = onAllNodesWithTag("chart-tooltip").fetchSemanticsNodes().single()
        val text = tip.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()
        assertTrue(text.endsWith("12 answers"), text)
        // Over the square, not under the grid.
        assertTrue(tip.boundsInRoot.bottom <= grid.top + square.y, "the tooltip at ${tip.boundsInRoot}, the square at ${square.y + grid.top}")
        System.getenv("HEATMAP_TOOLTIP_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
        onNodeWithTag("heatmap").performMouseInput { moveTo(Offset(-50f, -50f)) }
    }
}
