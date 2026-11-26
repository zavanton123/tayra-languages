package com.idiomiko.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/** On a phone a tapped chart's tooltip goes after a few seconds; with a pointer it stays while pointed at. */
@OptIn(ExperimentalTestApi::class)
class ChartTooltipTimeoutTest {
    private fun tooltips(test: androidx.compose.ui.test.ComposeUiTest) = test.onAllNodesWithTag("chart-tooltip").fetchSemanticsNodes().size

    private fun androidx.compose.ui.test.ComposeUiTest.chart(width: WindowWidth) {
        mainClock.autoAdvance = false
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalWindowWidth provides width) {
                    ColumnTooltips(5, { "Column $it" }, Modifier.fillMaxWidth().height(120.dp).testTag("chart")) {}
                }
            }
        }
        onNodeWithTag("chart").performTouchInput { click(center) }
        mainClock.advanceTimeBy(100)
        assertEquals(1, tooltips(this), "the tapped column is named")
    }

    @Test
    fun aPhonesTooltipGoesAfterThreeSeconds() = runDesktopComposeUiTest(width = 400, height = 400) {
        chart(WindowWidth.COMPACT)
        mainClock.advanceTimeBy(TOUCH_TOOLTIP_MILLIS - 500)
        assertEquals(1, tooltips(this), "still up just before")
        mainClock.advanceTimeBy(1_000)
        assertEquals(0, tooltips(this), "gone after three seconds")
    }

    @Test
    fun aWideWindowsTooltipStays() = runDesktopComposeUiTest(width = 1200, height = 400) {
        chart(WindowWidth.EXPANDED)
        mainClock.advanceTimeBy(TOUCH_TOOLTIP_MILLIS * 2)
        assertEquals(1, tooltips(this))
    }
}
