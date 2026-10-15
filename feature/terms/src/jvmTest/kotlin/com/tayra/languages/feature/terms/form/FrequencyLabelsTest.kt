package com.tayra.languages.feature.terms.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.dictionary.WordFrequency
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FrequencyLabelsTest {

    @Test
    fun eachFrequencyShowsItsDictionaryAndItsRankOrCount() = runDesktopComposeUiTest(width = 420, height = 80) {
        setContent {
            TayraTheme {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    FrequencyLabels(listOf(WordFrequency("JPDB", 1234, null, rankBased = true), WordFrequency("Novels", 5821, null, rankBased = false), WordFrequency("BCCWJ", 3, "3㋕", rankBased = true)))
                }
            }
        }
        onNodeWithText("#1234").assertExists()
        onNodeWithText("5821").assertExists()
        onNodeWithText("3㋕").assertExists()
        System.getenv("FREQUENCY_LABELS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }
}
