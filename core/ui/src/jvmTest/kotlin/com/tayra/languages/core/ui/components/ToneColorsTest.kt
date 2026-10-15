package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.morphology.Chinese
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ToneColorsTest {

    private fun colours(reading: String) = toneColored(reading).let { text -> text.spanStyles.map { text.text.substring(it.start, it.end) to ToneColors.indexOf(it.item.color) + 1 } }

    @Test
    fun eachSyllableTakesItsTonesColour() {
        assertEquals(listOf("wǒ" to 3, "men" to 5), colours("wǒmen"))
        assertEquals(listOf("nǐ" to 3, "hǎo" to 3), colours("nǐ hǎo"))
        assertEquals(listOf("zhōng" to 1, "wén" to 2, "diàn" to 4), colours("zhōngwén diàn"))
        assertEquals(listOf("ㄋㄧˇ" to 3, "ㄏㄠˇ" to 3), colours(Chinese.zhuyin("nǐ hǎo")))
        assertEquals(listOf("ㄨㄛˇ" to 3, "˙ㄇㄣ" to 5), colours(Chinese.zhuyin("wǒmen")))
    }

    @Test
    fun readingsLookLikeZhongwens() = runDesktopComposeUiTest(width = 360, height = 140) {
        setContent {
            TayraTheme {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(8.dp)) {
                        listOf("wǒmen", "zhōngwén diànnǎo", Chinese.zhuyin("nǐ hǎo ma")).forEach { Text(toneColored(it), style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
        }
        System.getenv("TONES_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }
}
