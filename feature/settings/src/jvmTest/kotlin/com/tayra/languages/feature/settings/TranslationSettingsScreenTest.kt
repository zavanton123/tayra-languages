package com.tayra.languages.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.GoogleTranslation
import com.tayra.languages.core.domain.service.LocalPackage
import com.tayra.languages.core.domain.service.LocalSentenceTranslator
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.LocalTranslationProblem
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.AppThemes
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Translation page shows the pair, the engine's state and the packages, and installs what the pair lacks. */
@OptIn(ExperimentalTestApi::class)
class TranslationSettingsScreenTest {

    /** Argos with a catalogue of packages, some installed. */
    private class FakeArgos(installed: Set<String>) : LocalSentenceTranslator {
        private val names = mapOf(
            "en" to "English", "ru" to "Russian", "pt" to "Portuguese", "cs" to "Czech", "de" to "German", "es" to "Spanish",
            "fr" to "French", "bg" to "Bulgarian", "it" to "Italian", "pl" to "Polish",
        )
        private val keys = listOf("en-ru", "cs-en", "de-en", "es-en", "fr-en", "bg-en", "pt-en", "it-en", "pl-en", "en-pt", "en-de")
        val installed = installed.toMutableSet()
        override val displayName = "Argos Translate"
        override val description = ""
        override val packagesDescription = ""
        override val hasRuntimeSetup = true
        override val lastError = MutableStateFlow<LocalTranslationProblem?>(null)
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun translate(text: String, language: Language): String? = null
        override suspend fun status() = "Argos Translate 1.11.0 · Python 3.12.14 · ${installed.size} language pairs"
        override suspend fun prepare(fromCode: String, toCode: String, fromName: String, toName: String) = Unit
        override suspend fun canTranslate(fromCode: String, toCode: String) = true
        override suspend fun installModels(fromCode: String, toCode: String) = Unit
        override suspend fun setUp() = "ready"
        override suspend fun packages() = keys.map { key ->
            val (from, to) = key.split("-")
            LocalPackage(from, to, names.getValue(from), names.getValue(to), installed = key in installed, sizeBytes = if (key in installed) 84_000_000L + key.hashCode() % 1000 else 0)
        }
        override suspend fun installPackage(fromCode: String, toCode: String) { installed += "$fromCode-$toCode" }
        override suspend fun removePackage(fromCode: String, toCode: String) { installed -= "$fromCode-$toCode" }
    }

    @Test
    fun theMissingPackageForThePairIsInstalledAndOthersRemoved() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-translation-settings", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val argos = FakeArgos(setOf("en-ru", "cs-en", "de-en", "es-en", "fr-en", "bg-en"))
        runBlocking {
            val portuguese = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = portuguese, nativeLanguage = "ru", translationEngine = TranslationEngine.ARGOS) }
        }
        val viewModel = OfflineTranslationViewModel(settings, languages, LocalTranslation(argos), object : GoogleTranslation { override suspend fun checkKey() = "ok" })
        setContent { HostedTranslation(settings) { OfflineTranslationScreen(onNavigate = {}, onBack = {}, viewModel = viewModel) } }

        // The pair lacks Portuguese → English; installing it makes the pair ready.
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Portuguese → Russian needs", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("TRANSLATION_SCREENSHOT")?.let { save(it.replace(".png", "-needs.png")) }
        onAllNodes(hasText("Install") and hasClickAction())[0].performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Portuguese → Russian is ready")).fetchSemanticsNodes().isNotEmpty() }
        assertTrue("pt-en" in argos.installed)
        onNodeWithText("Offline translation ready").assertExists()
        System.getenv("TRANSLATION_SCREENSHOT")?.let { save(it) }

        // The other packages are found under Available, and searching narrows them.
        onNodeWithText("Available").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Italian → English")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Search packages").performTextInput("pol")
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Italian → English")).fetchSemanticsNodes().isEmpty() }
        onNodeWithText("Polish → English").assertExists()

        // An installed package is removed from its menu.
        onNode(hasSetTextAction()).performTextClearance()
        onNodeWithText("Installed").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Bulgarian → English")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription("More for Bulgarian → English").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Remove")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Remove").performClick()
        waitUntil(timeoutMillis = 5_000) { "bg-en" !in argos.installed }
        assertFalse("bg-en" in argos.installed)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}

@Composable
private fun HostedTranslation(settings: SettingsRepositoryImpl, content: @Composable () -> Unit) {
    val current by settings.settings.collectAsState()
    TayraTheme(AppThemes.byId(current.themeId)) {
        ProvideWindowWidth {
            CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(current.currentLanguageId to "Portuguese"), current.currentLanguageId) {}, content = content)
        }
    }
}
