package com.tayra.languages.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/** The language dialog laid out on a wide window and on a phone. */
@OptIn(ExperimentalTestApi::class)
class LanguageDialogTest {

    private val names = listOf(
        "Portuguese", "Belarusian", "Bulgarian", "Catalan", "Croatian", "Czech", "Danish", "Dutch", "English", "Estonian", "Finnish", "French", "Galician",
        "German", "Greek", "Hungarian", "Icelandic", "Italian", "Latin", "Latvian", "Lithuanian", "Macedonian", "Norwegian", "Polish", "Romanian", "Russian",
        "Spanish", "Swedish", "Turkish", "Ukrainian",
    )

    private fun ComposeUiTest.open(compact: Boolean) {
        val state = LearningLanguageState(names.mapIndexed { i, n -> (i + 1L) to n }, 1L, nativeIds = setOf(26L), knownWords = 586) {}
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides state) {
                        Surface { AppTopBar(title = "Courses", onNavigate = {}, section = NavSection.COURSES) }
                    }
                }
            }
        }
        onNodeWithContentDescription("Learning language: Portuguese (586)").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Choose a language").fetchSemanticsNodes().isNotEmpty() }
        waitForIdle()
    }

    private fun ComposeUiTest.save(env: String) {
        System.getenv(env)?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }

    @Test
    fun wideWindow() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        open(compact = false)
        onAllNodesWithText("29 languages").fetchSemanticsNodes().isNotEmpty()
        save("LANGUAGE_DIALOG_SCREENSHOT")
    }

    @Test
    fun phone() = runDesktopComposeUiTest(width = 420, height = 900) {
        open(compact = true)
        save("LANGUAGE_DIALOG_PHONE_SCREENSHOT")
    }
}
