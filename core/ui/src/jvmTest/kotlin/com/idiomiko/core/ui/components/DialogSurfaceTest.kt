package com.idiomiko.core.ui.components

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
import com.idiomiko.core.ui.theme.AppSpacing
import com.idiomiko.core.ui.theme.IdiomikoTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals

/** A window-filling dialog stays below a title bar the window's content runs under, with the same margin on every side. */
@OptIn(ExperimentalTestApi::class)
class DialogSurfaceTest {

    @Test
    fun aDialogStaysBelowTheTitleBar() = runDesktopComposeUiTest(width = 1200, height = 800) {
        setContent {
            IdiomikoTheme {
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

    /** A dialog that fills the window starts below the title bar too, so its back arrow is clear of the window's buttons. */
    @Test
    fun aScreenDialogStartsBelowTheTitleBar() = runDesktopComposeUiTest(width = 400, height = 800) {
        setContent {
            IdiomikoTheme {
                CompositionLocalProvider(LocalTitleBarInset provides 28.dp) {
                    AppScreenDialog(onDismissRequest = {}) { Box(Modifier.fillMaxSize().testTag("screen-dialog-content")) }
                }
            }
        }
        val content = onNodeWithTag("screen-dialog-content").fetchSemanticsNode().boundsInRoot
        assertEquals(with(density) { 28.dp.toPx() }, content.top, 1f)
        assertEquals(0f, content.left, 1f)
    }

    /** On a phone a dialog with content takes the whole screen, its buttons at the foot; a question stays a small dialog. */
    @Test
    fun onAPhoneContentTakesTheScreenAndAQuestionStaysSmall() = runDesktopComposeUiTest(width = 400, height = 800) {
        var asking by mutableStateOf(false)
        setContent {
            IdiomikoTheme {
                CompositionLocalProvider(LocalWindowWidth provides WindowWidth.COMPACT) {
                    if (!asking) {
                        AppDialog(
                            onDismissRequest = {}, fullScreenOnPhone = true, modifier = Modifier.testTag("content-dialog"),
                            title = { Text("Study more") },
                            text = { Text("Learn new cards ahead of schedule, or review the ones you forgot.") },
                            dismissButton = { QuietButton("Cancel", onClick = {}) },
                            confirmButton = { PrimaryButton("Start", onClick = {}, modifier = Modifier.testTag("content-confirm")) },
                        )
                    } else {
                        ConfirmDialog("Delete this book?", "Its saved words stay.", onConfirm = {}, onDismiss = {})
                    }
                }
            }
        }
        val screen = onNodeWithTag("content-dialog").getBoundsInRoot()
        assertEquals(400.dp, screen.right - screen.left)
        assertEquals(800.dp, screen.bottom - screen.top)
        assertTrue(800.dp - onNodeWithTag("content-confirm").getBoundsInRoot().bottom < 40.dp, "the buttons sit at the foot")
        onNodeWithContentDescription("Close").assertExists()
        System.getenv("PHONE_DIALOG_SCREENSHOT")?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[1].captureToImage().toAwtImage(), "png", File(it)) }

        asking = true
        waitForIdle()
        val question = onNode(hasText("Delete this book?")).getBoundsInRoot()
        assertTrue(question.top > 100.dp, "a question is a small dialog in the middle, not the whole screen")
    }
}
