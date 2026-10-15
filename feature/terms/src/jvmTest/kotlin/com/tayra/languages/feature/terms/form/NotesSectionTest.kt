package com.tayra.languages.feature.terms.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.morphology.JapaneseNotes
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NotesSectionTest {

    @Test
    fun eraYearsNumbersAndAreasAreSpelledOut() = runDesktopComposeUiTest(width = 460, height = 220) {
        val notes = JapaneseNotes.at("平成31年4月30日", 0) + JapaneseNotes.at("四畳半の部屋", 0) + JapaneseNotes.at("39,800万円", 0)
        setContent { TayraTheme { Surface(color = MaterialTheme.colorScheme.surface) { NotesSection(notes) } } }
        onNodeWithText("平成31年4月30日 = 2019-04-30").assertExists()
        onNodeWithText("四畳半 = 7.29 m²").assertExists()
        onNodeWithText("京間 8.21 m², 中京間 7.45 m², 江戸間 6.97 m², 団地間 6.5 m²").assertExists()
        onNodeWithText("39,800万 = 398,000,000").assertExists()
        System.getenv("NOTES_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }

    @Test
    fun valuesKeepTwoDecimalsAtMost() {
        assertEquals(listOf("7.29", "1,981", "13.89", "0.5", "1,000,000"), listOf(7.29, 1981.0, 13.888, 0.5, 1e6).map(::decimal))
    }
}
