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
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** A window-filling dialog keeps its margin, and stays below a title bar the window's content runs under. */
@OptIn(ExperimentalTestApi::class)
class DialogSurfaceTest {

    @Test
    fun aDialogStaysBelowTheTitleBar() = runDesktopComposeUiTest(width = 1200, height = 800) {
        setContent {
            TayraTheme {
                CompositionLocalProvider(LocalTitleBarInset provides 28.dp) {
                    AppDialogSurface(onDismissRequest = {}, modifier = Modifier.fillMaxHeight()) {
                        Box(Modifier.fillMaxSize().testTag("dialog-content"))
                    }
                }
            }
        }
        val top = onNodeWithTag("dialog-content").fetchSemanticsNode().boundsInRoot.top
        assertEquals(with(density) { (28.dp + AppSpacing.l).toPx() }, top, 1f)
    }
}
