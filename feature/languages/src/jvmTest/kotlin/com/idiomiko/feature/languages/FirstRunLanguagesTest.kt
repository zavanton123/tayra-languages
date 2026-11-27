package com.idiomiko.feature.languages

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.IdiomikoTheme
import com.russhwolf.settings.MapSettings
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** After the first language is chosen, the native and interface languages are asked for and saved on Continue. */
@OptIn(ExperimentalTestApi::class)
class FirstRunLanguagesTest {

    @Test
    fun aWideWindowAsksInADialog() = pickAndContinue(width = 1400, height = 900, screenshot = "FIRST_RUN_LANGUAGES_SCREENSHOT")

    @Test
    fun aPhoneAsksOnTheWholeScreen() = pickAndContinue(width = 400, height = 860, screenshot = "FIRST_RUN_LANGUAGES_PHONE_SCREENSHOT")

    private fun pickAndContinue(width: Int, height: Int, screenshot: String) = runDesktopComposeUiTest(width = width, height = height) {
        val settings = SettingsRepositoryImpl(MapSettings())
        var done = false
        setContent {
            // German is being learned, so it cannot be the language meanings are shown in.
            val state = LearningLanguageState(listOf(1L to "Portuguese", 2L to "German"), currentId = 2L) {}
            IdiomikoTheme(AppThemes.byId(AppThemes.all.first().id)) {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides state) {
                        FirstRunLanguagesDialog(onDone = { done = true }, viewModel = LanguagesViewModel(settings))
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("first-run-languages")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv(screenshot)?.let { path -> waitForIdle(); ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", File(path)) }

        onNodeWithTag("native-language").performClick()
        onAllNodesWithText("You are learning it").onLast().assertExists()
        onAllNodesWithText("Russian").onLast().performClick()
        waitForIdle()
        onNodeWithTag("interface-language").performClick()
        onAllNodesWithText("Español").onLast().performClick()
        waitForIdle()
        assertEquals("en", settings.current.uiLanguage, "nothing is saved before Continue")

        onNodeWithTag("first-run-continue").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.uiLanguage == "es" }
        assertEquals("ru", settings.current.nativeLanguage)
        assertTrue(done)
    }
}
