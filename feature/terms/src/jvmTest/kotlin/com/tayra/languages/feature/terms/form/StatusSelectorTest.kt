package com.tayra.languages.feature.terms.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** The status row with the learning levels left out: three statuses by name, the levels kept underneath. */
@OptIn(ExperimentalTestApi::class)
class StatusSelectorTest {

    @Test
    fun learningIsOneStatusThatKeepsAWordsLevel() = runDesktopComposeUiTest(width = 440, height = 160) {
        var status by mutableStateOf(TermStatus.UNKNOWN)
        setContent {
            TayraTheme(learningLevels = false) {
                Surface(color = MaterialTheme.colorScheme.surface) { StatusSelector(selected = status, onSelect = { status = it }, large = true) }
            }
        }
        // Three buttons, not seven: no numbered levels, and no Ignored.
        for (name in listOf("New", "Learning", "Known")) onNodeWithContentDescription(name).assertExists()
        for (name in listOf("Learning 2", "Ignored")) assertEquals(0, onAllNodes(androidx.compose.ui.test.hasContentDescription(name)).fetchSemanticsNodes().size)
        System.getenv("STATUS_SELECTOR_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }

        // An unknown word starts at level 1.
        onNodeWithContentDescription("Learning").performClick()
        waitForIdle()
        assertEquals(TermStatus.NEW_1, status)

        // A word already being learned keeps its level when Learning is chosen again.
        status = TermStatus.LEARNING_3
        waitForIdle()
        onNodeWithContentDescription("Learning").performClick()
        waitForIdle()
        assertEquals(TermStatus.LEARNING_3, status)

        onNodeWithContentDescription("Known").performClick()
        waitForIdle()
        assertEquals(TermStatus.WELL_KNOWN, status)
    }

    @Test
    fun withTheLevelsShownEachLevelHasItsButton() = runDesktopComposeUiTest(width = 440, height = 160) {
        var status by mutableStateOf(TermStatus.UNKNOWN)
        setContent { TayraTheme(learningLevels = true) { StatusSelector(selected = status, onSelect = { status = it }, large = true) } }
        onNodeWithContentDescription("Learning 3").performClick()
        waitForIdle()
        assertEquals(TermStatus.LEARNING_3, status)
    }
}
