package com.idiomiko.feature.settings

import androidx.compose.ui.test.assertCountEquals
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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.service.GoogleTranslation
import com.idiomiko.core.domain.service.LocalPackage
import com.idiomiko.core.domain.service.LocalSentenceTranslator
import com.idiomiko.core.domain.service.LocalTranslation
import com.idiomiko.core.domain.service.LocalTranslationProblem
import com.idiomiko.core.domain.service.TranslationEngine
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.IdiomikoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Translation page shows the engine's state and says what the pair lacks; the native language is set on Languages. */
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

    /** The page says what the pair lacks; the app downloads the models on its own, so the page offers no downloads. */
    @Test
    fun thePageSaysWhatThePairLacks() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-translation-settings", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val argos = FakeArgos(setOf("en-ru", "cs-en", "de-en", "es-en", "fr-en", "bg-en"))
        runBlocking {
            val portuguese = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = portuguese, nativeLanguage = "ru", translationEngine = TranslationEngine.ARGOS) }
        }
        val viewModel = OfflineTranslationViewModel(settings, languages, LocalTranslation(argos), object : GoogleTranslation { override suspend fun checkKey() = "ok" })
        setContent { HostedTranslation(settings) { OfflineTranslationScreen(onNavigate = {}, onBack = {}, viewModel = viewModel) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Installed. The language pair still needs a download.")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("TRANSLATION_SCREENSHOT")?.let { save(it) }
        onNodeWithText("Search packages").assertDoesNotExist()
        onNodeWithText("Language pair").assertDoesNotExist()
        onAllNodes(hasText("Install") and hasClickAction()).assertCountEquals(0)
        assertFalse("pt-en" in argos.installed, "nothing is downloaded from this page")
    }

    @Test
    fun aPhoneSaysWhatThePairLacksToo() = runDesktopComposeUiTest(width = 400, height = 1100) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-translation-phone", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val argos = FakeArgos(setOf("cs-en", "de-en"))
        runBlocking {
            val portuguese = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = portuguese, nativeLanguage = "ru", translationEngine = TranslationEngine.ARGOS) }
        }
        val viewModel = OfflineTranslationViewModel(settings, languages, LocalTranslation(argos), object : GoogleTranslation { override suspend fun checkKey() = "ok" })
        setContent { HostedTranslation(settings) { OfflineTranslationScreen(onNavigate = {}, onBack = {}, viewModel = viewModel) } }

        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Installed. The language pair still needs a download.")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("TRANSLATION_SCREENSHOT")?.let { save(it.replace(".png", "-phone.png")) }
        onAllNodes(hasText("Install") and hasClickAction()).assertCountEquals(0)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}

@Composable
private fun HostedTranslation(settings: SettingsRepositoryImpl, content: @Composable () -> Unit) {
    val current by settings.settings.collectAsState()
    IdiomikoTheme(AppThemes.byId(current.themeId)) {
        ProvideWindowWidth {
            CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(current.currentLanguageId to "Portuguese"), current.currentLanguageId) {}, content = content)
        }
    }
}
