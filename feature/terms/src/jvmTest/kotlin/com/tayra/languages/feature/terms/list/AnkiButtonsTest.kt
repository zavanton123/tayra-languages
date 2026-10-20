package com.tayra.languages.feature.terms.list

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlin.test.Test

/** An Anki export shows its progress on the button that started it. */
@OptIn(ExperimentalTestApi::class)
class AnkiButtonsTest {

    @Test
    fun sendingShowsOnTheSendButton() = runDesktopComposeUiTest(width = 1200, height = 300) {
        setContent { TayraTheme { PageHeader(exporting = "3 of 12", sending = true, onExport = {}, onSend = {}, onNew = {}) } }
        onNodeWithText("Sending 3 of 12").assertExists()
        onNodeWithText("Export to Anki").assertIsNotEnabled()
    }

    @Test
    fun exportingShowsOnTheExportButton() = runDesktopComposeUiTest(width = 1200, height = 300) {
        setContent { TayraTheme { PageHeader(exporting = "3 of 12", sending = false, onExport = {}, onSend = {}, onNew = {}) } }
        onNodeWithText("Exporting 3 of 12").assertExists()
        onNodeWithText("Send to Anki").assertIsNotEnabled()
    }
}
