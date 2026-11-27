package com.idiomiko.feature.languages

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.IdiomikoTheme
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
            val state = LearningLanguageState(listOf(1L to "Portuguese", 2L to "German", 3L to "English"), current) { current = it; learningId = it }
            IdiomikoTheme(AppThemes.byId(AppThemes.all.first().id)) {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides state) {
                        LanguagesScreen(onNavigate = {}, onBack = {}, viewModel = LanguagesViewModel(settings))
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("I'm learning")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("LANGUAGES_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
        }

        assertTrue(onAllNodesWithText("Changes saved").fetchSemanticsNodes().isEmpty(), "nothing has changed yet")
        // The language to learn is chosen in the large dialog of every language, the current one ticked.
        onNodeWithTag("learning-language").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Select the language to study")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("3 languages").assertExists()
        onNodeWithContentDescription("Selected").assertExists()
        System.getenv("LANGUAGES_MENU_SCREENSHOT")?.let { path -> waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path)) }
        onNodeWithTag("language-de").performClick()
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

    /** A phone gets the three choices as cards whose rows open bottom sheets of the languages. */
    @Test
    fun aPhonePicksTheLanguagesFromSheets() = runDesktopComposeUiTest(width = 400, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        var learningId = 1L
        setContent {
            var current by remember { mutableLongStateOf(1L) }
            val state = LearningLanguageState(listOf(1L to "Portuguese", 2L to "German", 3L to "English"), current) { current = it; learningId = it }
            IdiomikoTheme(AppThemes.byId(AppThemes.all.first().id)) {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides state) {
                        LanguagesScreen(onNavigate = {}, onBack = {}, viewModel = LanguagesViewModel(settings))
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Show meanings in")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Language setup").assertDoesNotExist()
        onNodeWithText("Books, courses, vocabulary and flashcards.").assertDoesNotExist()
        System.getenv("LANGUAGES_SCREENSHOT")?.let { path -> waitForIdle(); ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path.replace(".png", "-phone.png"))) }

        onNodeWithTag("learning-language").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("German").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText("German").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { learningId == 2L }

        onNodeWithTag("native-language").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("You are learning it").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText("Russian").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.nativeLanguage == "ru" }

        onNodeWithTag("interface-language").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Русский").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText("Русский").onLast().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.uiLanguage == "ru" }
    }

    /** Each language being learned but the current one has a link that makes it the one learned. */
    @Test
    fun anotherLanguageBeingLearnedCanBeLearnedNow() = runDesktopComposeUiTest(width = 1580, height = 1200) {
        val chosen = mutableListOf<Long>()
        setContent {
            val state = LearningLanguageState(listOf(1L to "Portuguese", 2L to "German", 3L to "English"), 1L, mine = listOf(1L, 2L)) { chosen += it }
            IdiomikoTheme(AppThemes.byId(AppThemes.all.first().id)) {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides state) {
                        LanguagesScreen(onNavigate = {}, onBack = {}, viewModel = LanguagesViewModel(SettingsRepositoryImpl(MapSettings())))
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Languages I'm learning")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("learn-language-pt").assertDoesNotExist()
        onNodeWithTag("learn-language-de").performClick()
        assertEquals(listOf(2L), chosen)
        onNodeWithText("Changes saved").assertExists()
    }

    /** The languages being learned are listed with their words, each with a way to remove it that asks first. */
    @Test
    fun aLanguageBeingLearnedCanBeRemoved() = runDesktopComposeUiTest(width = 1580, height = 1200) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val viewModel = LanguagesViewModel(settings)
        setContent {
            val state = LearningLanguageState(
                listOf(1L to "Portuguese", 2L to "German", 3L to "English"), 1L, knownWords = 586,
                mine = listOf(1L, 2L), wordCounts = mapOf(1L to 586, 2L to 12),
            ) {}
            IdiomikoTheme(AppThemes.byId(AppThemes.all.first().id)) {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides state) {
                        LanguagesScreen(onNavigate = {}, onBack = {}, viewModel = viewModel)
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Languages I'm learning")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("586 words · Learning now").assertExists()
        onNodeWithText("12 words").assertExists()
        // Portuguese, learned now, comes before German.
        assertTrue(onNodeWithText("586 words · Learning now").fetchSemanticsNode().boundsInRoot.top < onNodeWithText("12 words").fetchSemanticsNode().boundsInRoot.top)
        System.getenv("MY_LANGUAGES_SCREENSHOT")?.let { path -> waitForIdle(); ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path)) }

        onNodeWithTag("remove-language-de").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Remove German?").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2L, viewModel.removing.value?.languageId)
        // Nothing is counted first: the removal can be confirmed at once.
        onNodeWithText("texts, saved words, progress, flashcards and downloads", substring = true).assertExists()
        onNodeWithTag("confirm-remove").assertIsEnabled()
        onNodeWithText("Cancel").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.removing.value == null }
    }
}
