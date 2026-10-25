package com.tayra.languages.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

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
        onNodeWithTag("add-language").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Add a language").fetchSemanticsNodes().isNotEmpty() }
        waitForIdle()
    }

    private fun ComposeUiTest.save(env: String) {
        System.getenv(env)?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }

    @Test
    fun wideWindow() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        open(compact = false)
        onNodeWithText("29 languages").assertExists()
        save("LANGUAGE_DIALOG_SCREENSHOT")
    }

    /** With no language chosen yet, as on first start, the dialog asks for one and cannot be closed without choosing. */
    @Test
    fun theFirstLanguageIsAskedFor() = runDesktopComposeUiTest(width = 420, height = 900) {
        val chosen = mutableListOf<Long>()
        val state = LearningLanguageState(names.mapIndexed { i, n -> (i + 1L) to n }, 0L, nativeIds = setOf(9L)) { chosen += it }
        setContent { TayraTheme { ProvideWindowWidth { TargetLanguageDialog(state) } } }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Select target language").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Choose the language you want to study").assertExists()
        onNodeWithText("30 languages").assertExists()
        assertEquals(0, onAllNodesWithTag("current-language").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithContentDescription("Close").fetchSemanticsNodes().size)
        save("TARGET_LANGUAGE_DIALOG_SCREENSHOT")
        onNodeWithTag("language-be").performClick()
        assertEquals(listOf(2L), chosen)
    }

    /** On a phone, adding a language does not open a dialog: it leads to the language screen. */
    @Test
    fun aPhoneOpensTheLanguageScreen() = runDesktopComposeUiTest(width = 420, height = 900) {
        val visited = mutableListOf<Route>()
        val state = LearningLanguageState(names.mapIndexed { i, n -> (i + 1L) to n }, 1L, knownWords = 586) {}
        setContent {
            TayraTheme { ProvideWindowWidth { CompositionLocalProvider(LocalLearningLanguage provides state) { Surface { AppTopBar(title = "Courses", onNavigate = { visited += it }) } } } }
        }
        onNodeWithContentDescription("Learning language: Portuguese (586)").performClick()
        onNodeWithTag("add-language").performClick()
        assertEquals(listOf<Route>(Route.ChooseLanguage), visited)
        assertEquals(0, onAllNodesWithText("Add a language").fetchSemanticsNodes().size, "no dialog")
    }

    /** The phone's screen: the languages not being learned, with a letter index that brings them into view, and a choice that goes back. */
    @Test
    fun theLanguageScreen() = runDesktopComposeUiTest(width = 420, height = 900) {
        val chosen = mutableListOf<Long>()
        var back = 0
        val state = LearningLanguageState(names.mapIndexed { i, n -> (i + 1L) to n }, 1L, nativeIds = setOf(26L), knownWords = 586) { chosen += it }
        setContent {
            TayraTheme { ProvideWindowWidth { CompositionLocalProvider(LocalLearningLanguage provides state) { ChooseLanguageScreen(onBack = { back++ }) } } }
        }
        onNodeWithText("Add a language").assertExists()
        onNodeWithText("29 languages").assertExists()
        assertEquals(0, onAllNodesWithTag("language-pt").fetchSemanticsNodes().size, "the language being learned is not offered")
        save("LANGUAGE_DIALOG_PHONE_SCREENSHOT")
        // Far down the list, Swedish is not even laid out yet.
        assertEquals(0, onAllNodesWithTag("language-sv").fetchSemanticsNodes().size)
        onNodeWithTag("index-S").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("language-sv").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("language-sv").assertIsDisplayed().performClick()
        assertEquals(listOf(names.indexOf("Swedish") + 1L), chosen)
        assertEquals(1, back)
    }

    /** Mandarin is offered as two languages, one in each script; adding the other script keeps the language. */
    @Test
    fun chineseIsOfferedInEachScript() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val scripts = mutableListOf<Pair<Long, ChineseScript>>()
        val chosen = mutableListOf<Long>()
        val state = LearningLanguageState(
            listOf(1L to "Portuguese", 2L to "Mandarin Chinese", 3L to "German"), 2L, knownWords = 40,
            scripts = mapOf(2L to ChineseScript.SIMPLIFIED), onSelectScript = { id, script -> scripts += id to script },
        ) { chosen += it }
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides state) {
                        Surface { AppTopBar(title = "Courses", onNavigate = {}, section = NavSection.COURSES) }
                    }
                }
            }
        }
        onNodeWithContentDescription("Learning language: Chinese (Simplified) (40)").performClick()
        onNodeWithTag("my-language-zh-hans").assertExists()
        save("LANGUAGE_MENU_SCREENSHOT")
        onNodeWithTag("add-language").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Add a language").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("3 languages").assertExists()
        assertEquals(0, onAllNodesWithTag("language-zh-hans").fetchSemanticsNodes().size, "the current one is not offered again")
        assertEquals(0, onAllNodesWithText("Mandarin Chinese").fetchSemanticsNodes().size)
        save("LANGUAGE_DIALOG_CHINESE_SCREENSHOT")
        onNodeWithTag("language-zh-hant").performClick()
        assertEquals(listOf(2L to ChineseScript.TRADITIONAL), scripts)
        assertEquals(emptyList(), chosen)
    }
}
