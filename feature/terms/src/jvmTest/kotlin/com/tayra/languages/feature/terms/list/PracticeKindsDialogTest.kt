package com.tayra.languages.feature.terms.list

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** The dialog that asks which kinds of question to practise. */
@OptIn(ExperimentalTestApi::class)
class PracticeKindsDialogTest {

    /** In a narrow window the count beside the buttons wraps, and Start practice stays one line wide. */
    @Test
    fun aNarrowWindowKeepsTheStartButtonWhole() = runDesktopComposeUiTest(width = 520, height = 950) {
        setContent { TayraTheme { ProvideWindowWidth { PracticeKindsDialog(count = 12, onStart = {}, onDismiss = {}) } } }
        waitForIdle()
        System.getenv("PRACTICE_KINDS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
        val start = onNodeWithTag("start-practice").fetchSemanticsNode().size
        assertTrue(start.width > start.height * 2, "Start practice is ${start.width} wide and ${start.height} high")
    }
}
