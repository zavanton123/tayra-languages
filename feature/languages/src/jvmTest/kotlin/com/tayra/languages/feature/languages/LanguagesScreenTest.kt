package com.tayra.languages.feature.languages

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
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

/** The Languages page chooses the language being learned, the native language and the interface language. */
@OptIn(ExperimentalTestApi::class)
class LanguagesScreenTest {

    @Test
    fun thePickersChangeTheThreeLanguages() = runDesktopComposeUiTest(width = 1400, height = 900) {
        val settings = SettingsRepositoryImpl(MapSettings())
        var learningId = 1L
        setContent {
            var current by remember { mutableLongStateOf(1L) }
            val state = LearningLanguageState(listOf(1L to "Portuguese", 2L to "German"), current) { current = it; learningId = it }
            TayraTheme(AppThemes.byId(AppThemes.all.first().id)) {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides state) {
                        LanguagesScreen(onNavigate = {}, onBack = {}, viewModel = LanguagesViewModel(settings))
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Your languages")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("LANGUAGES_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
        }

        onAllNodesWithText("Portuguese").onLast().performClick()
        onAllNodesWithText("German").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { learningId == 2L }

        onAllNodesWithText("English")[0].performClick()
        onAllNodesWithText("Russian").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.nativeLanguage == "ru" }

        onAllNodesWithText("English")[0].performClick()
        onAllNodesWithText("Spanish").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.uiLanguage == "es" }
        assertEquals("ru", settings.current.nativeLanguage)
        onNodeWithText("Spanish").assertExists()
    }
}
