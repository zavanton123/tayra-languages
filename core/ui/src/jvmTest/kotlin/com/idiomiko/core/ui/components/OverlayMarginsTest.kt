package com.idiomiko.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** A phone's snackbars and chart tooltips keep off the screen's sides, however long their text. */
@OptIn(ExperimentalTestApi::class)
class OverlayMarginsTest {
    private val long = "Nov 2 – Nov 6 · 1,202 answers (Learning 265, Young 702, Mature 136, Relearning 99), and then some more words"

    private fun ComposeUiTest.assertOffTheSides(tag: String) {
        val bounds = onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        val edge = 12.dp.value * density.density
        assertTrue(bounds.left >= edge && bounds.right <= WIDTH * density.density - edge, "$tag spans ${bounds.left}..${bounds.right}")
    }

    private fun ComposeUiTest.picture(env: String) = System.getenv(env)?.let { path ->
        waitForIdle()
        val roots = onAllNodes(isRoot()).fetchSemanticsNodes().indices
        roots.forEach { i -> ImageIO.write(onAllNodes(isRoot())[i].captureToImage().toAwtImage(), "png", File(path.replace(".png", "-$i.png"))) }
    }

    @Test
    fun aSnackbarKeepsOffTheSides() = runDesktopComposeUiTest(width = WIDTH, height = 600) {
        setContent { MaterialTheme { Scaffold(snackbarHost = { AppSnackbar(long, actionLabel = "OK", onAction = {}) }) {} } }
        assertOffTheSides("snackbar")
        picture("SNACKBAR_SCREENSHOT")
    }

    @Test
    fun aLongChartTooltipKeepsOffTheSides() = runDesktopComposeUiTest(width = WIDTH, height = 400) {
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalWindowWidth provides WindowWidth.COMPACT) {
                    ColumnTooltips(5, { long }, Modifier.fillMaxWidth().height(200.dp).testTag("chart")) {}
                }
            }
        }
        onNodeWithTag("chart").performTouchInput { click(centerRight.copy(x = width - 2f)) }
        waitForIdle()
        assertOffTheSides("chart-tooltip")
        picture("TOOLTIP_SCREENSHOT")
    }

    private companion object {
        const val WIDTH = 400
    }
}
