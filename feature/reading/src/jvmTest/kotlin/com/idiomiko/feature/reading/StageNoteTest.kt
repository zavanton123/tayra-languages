package com.idiomiko.feature.reading

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.domain.model.ReadingStage
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.WindowWidth
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class StageNoteTest {
    /** On a phone the stage's action sits under its words, which keep the card's width, rather than squeezing them beside it. */
    @Test
    fun aPhonePutsTheActionUnderTheWords() = runDesktopComposeUiTest(width = 400, height = 500) {
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalWindowWidth provides WindowWidth.COMPACT) {
                    StageNote(ReadingStage.FLASHCARDS, ReadingStage.of(hasQuestions = false), onAction = {}, onClose = {})
                }
            }
        }
        val hint = onNodeWithText("Review today's flashcards", substring = true).fetchSemanticsNode().boundsInRoot
        val action = onNodeWithTag("stage-action").fetchSemanticsNode().boundsInRoot
        assertTrue(action.top >= hint.bottom, "the action is under the hint")
        assertTrue(hint.width > 200f, "the hint keeps the card's width (${hint.width})")
        System.getenv("STAGE_NOTE_SCREENSHOT")?.let { path ->
            waitForIdle()
            javax.imageio.ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", java.io.File(path))
        }
    }
}
