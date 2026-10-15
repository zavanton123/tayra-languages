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
import com.tayra.languages.core.domain.dictionary.KanjiInfo
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class KanjiSectionTest {

    @Test
    fun eachKanjiShowsItsMeaningsReadingsAndFacts() = runDesktopComposeUiTest(width = 460, height = 320) {
        setContent {
            TayraTheme {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    KanjiSection(
                        listOf(
                            KanjiInfo("日", listOf("day", "sun", "Japan"), listOf("ニチ", "ジツ"), listOf("ひ", "-び", "-か"), 4, 1, 4, 1),
                            KanjiInfo("𠮟", listOf("scold"), listOf("シツ"), listOf("しか.る"), 5, 8),
                        ),
                    )
                }
            }
        }
        onNodeWithTag("kanji-日").assertExists()
        onNodeWithText("day, sun, Japan").assertExists()
        onNodeWithText("ニチ、ジツ").assertExists()
        onNodeWithText("4 strokes · Grade 1 · Old JLPT 4 · Frequency #1").assertExists()
        onNodeWithText("5 strokes · Secondary school").assertExists()
        System.getenv("KANJI_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }
}
