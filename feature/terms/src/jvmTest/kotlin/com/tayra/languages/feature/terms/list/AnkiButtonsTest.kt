package com.tayra.languages.feature.terms.list

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlin.test.Test

/** One button puts the words into Anki, its label showing whether they go straight in or into a file. */
@OptIn(ExperimentalTestApi::class)
class AnkiButtonsTest {

    @Test
    fun theOneButtonShowsWhereTheWordsAreGoing() = runDesktopComposeUiTest(width = 1200, height = 300) {
        var state by mutableStateOf<Pair<String?, Boolean>>(null to false)
        setContent { TayraTheme { PageHeader(exporting = state.first, sending = state.second, onExport = {}, onNew = {}) } }
        onNodeWithText("Export to Anki").assertIsEnabled()
        onNodeWithText("Send to Anki").assertDoesNotExist()
        state = "3 of 12" to true
        onNodeWithText("Sending 3 of 12").assertIsNotEnabled()
        state = "3 of 12" to false
        onNodeWithText("Exporting 3 of 12").assertIsNotEnabled()
    }
}
