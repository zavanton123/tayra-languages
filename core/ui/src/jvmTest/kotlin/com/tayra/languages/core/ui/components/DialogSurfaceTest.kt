package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** A window-filling dialog stays below a title bar the window's content runs under, with the same margin on every side. */
@OptIn(ExperimentalTestApi::class)
class DialogSurfaceTest {

    @Test
    fun aDialogStaysBelowTheTitleBar() = runDesktopComposeUiTest(width = 1200, height = 800) {
        setContent {
            TayraTheme {
                CompositionLocalProvider(LocalTitleBarInset provides 28.dp) {
                    AppDialogSurface(onDismissRequest = {}, modifier = Modifier.fillMaxHeight(), maxWidth = Dp.Infinity) {
                        Box(Modifier.fillMaxSize().testTag("dialog-content"))
                    }
                }
            }
        }
        val content = onNodeWithTag("dialog-content").fetchSemanticsNode().boundsInRoot
        val margin = with(density) { (28.dp + AppSpacing.l).toPx() }
        // The test window's size, in pixels.
        val window = androidx.compose.ui.geometry.Size(1200f, 800f)
        assertEquals(margin, content.top, 1f)
        assertEquals(margin, content.left, 1f)
        assertEquals(margin, window.width - content.right, 1f)
        assertEquals(margin, window.height - content.bottom, 1f)
    }
}
