package com.tayra.languages.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.dictionary.ImportedDictionaryInfo
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ImportDictionaryTest {

    @Test
    fun theDialogPrefillsTheLanguagesAndImports() = runDesktopComposeUiTest(width = 700, height = 600) {
        val imported = mutableListOf<Pair<String, String>>()
        val ready = DictionaryImport.Ready(ImportedDictionaryInfo("Jitendex", "2026.10.01", attribution = "CC BY-SA 4.0"), ByteArray(0))
        setContent {
            TayraTheme {
                ImportLanguagesDialog(ready, defaultSource = "ja", defaultTarget = "en", onImport = { s, t -> imported += s to t }, onDismiss = {})
            }
        }
        onNodeWithText("Revision 2026.10.01", substring = true).assertExists()
        onNodeWithText("Japanese").assertExists()
        System.getenv("IMPORT_DIALOG_SCREENSHOT")?.let { path ->
            val roots = onAllNodes(isRoot())
            ImageIO.write(roots[roots.fetchSemanticsNodes().size - 1].captureToImage().toAwtImage(), "png", File(path))
        }
        onNodeWithTag("import-confirm").performClick()
        assertEquals(listOf("ja" to "en"), imported)
    }
}
