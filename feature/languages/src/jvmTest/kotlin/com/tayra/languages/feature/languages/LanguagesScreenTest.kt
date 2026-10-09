package com.tayra.languages.feature.languages

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.AppThemes
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Languages page chooses the language being learned, the native language and the interface language. */
@OptIn(ExperimentalTestApi::class)
class LanguagesScreenTest {

    @Test
    fun thePickersChangeTheThreeLanguages() = runDesktopComposeUiTest(width = 1580, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        var learningId = 1L
        setContent {
            var current by remember { mutableLongStateOf(1L) }
            val state = LearningLanguageState(listOf(1L to "Portuguese", 2L to "German", 3L to "English"), current, nativeIds = setOf(3L)) { current = it; learningId = it }
            TayraTheme(AppThemes.byId(AppThemes.all.first().id)) {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides state) {
                        LanguagesScreen(onNavigate = {}, onBack = {}, viewModel = LanguagesViewModel(settings))
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Language setup")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("LANGUAGES_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
        }

        assertTrue(onAllNodesWithText("Changes saved").fetchSemanticsNodes().isEmpty(), "nothing has changed yet")
        // English is the native language: listed, dimmed, and not chosen when clicked.
        onNodeWithTag("learning-language").performClick()
        onNodeWithText("Your native language").assertExists()
        System.getenv("LANGUAGES_MENU_SCREENSHOT")?.let { path -> waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path)) }
        onAllNodesWithText("English").onLast().assertIsNotEnabled().performClick()
        waitForIdle()
        assertEquals(1L, learningId)
        if (onAllNodesWithText("Your native language").fetchSemanticsNodes().isEmpty()) onNodeWithTag("learning-language").performClick()
        onAllNodesWithText("German").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { learningId == 2L }
        onNodeWithText("Changes saved").assertExists()

        // German is being learned now, so it is listed dimmed as a native language.
        onNodeWithTag("native-language").performClick()
        onNodeWithText("You are learning it").assertExists()
        System.getenv("LANGUAGES_MENU_SCREENSHOT")?.let { path -> waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path.replace(".png", "-native.png"))) }
        onAllNodesWithText("German").onLast().assertIsNotEnabled()
        onAllNodesWithText("Russian").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.nativeLanguage == "ru" }

        onNodeWithTag("interface-language").performClick()
        onAllNodesWithText("Русский").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.uiLanguage == "ru" }
        assertEquals("ru", settings.current.nativeLanguage)
    }
}
