package com.tayra.languages.feature.terms.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.dictionary.PitchAccent
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PitchAccentsTest {

    @Test
    fun eachAccentNamesItsDownstepAndPattern() = runDesktopComposeUiTest(width = 420, height = 120) {
        setContent {
            TayraTheme {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    PitchAccents(listOf(PitchAccent("にほん", 2), PitchAccent("にっぽん", 3), PitchAccent("はし", 0), PitchAccent("はし", 1)), isVerbOrAdjective = false)
                }
            }
        }
        onNodeWithTag("pitch-にほん-2").assertExists()
        onNodeWithText("[2] nakadaka").assertExists()
        onNodeWithText("[3] nakadaka").assertExists()
        onNodeWithText("[0] heiban").assertExists()
        onNodeWithText("[1] atamadaka").assertExists()
        System.getenv("PITCH_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }
}
