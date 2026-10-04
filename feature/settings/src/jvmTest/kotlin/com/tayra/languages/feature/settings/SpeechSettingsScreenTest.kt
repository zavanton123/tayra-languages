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
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Speech page shows the engine's state, the voice and speed for the language being learned, and its downloads. */
@OptIn(ExperimentalTestApi::class)
class SpeechSettingsScreenTest {

    /** Kokoro: one download with every voice. */
    private class FakeKokoro : LocalSpeechEngine {
        var installed = true
        override val engine = SpeechEngine.KOKORO
        override val displayName = "Kokoro"
        override val description = "A neural voice that runs on this computer. Unsupported languages use the system voice."
        override val packagesDescription = "One download holds the model and all its voices."
        override val hasRuntimeSetup = true
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun status() = "Kokoro 0.6.1 · Python 3.12.14 · " + if (installed) "Model downloaded" else "Model not downloaded yet"
        override suspend fun isReady() = true
        override suspend fun setUp() = "ready"
        override suspend fun packages() = listOf(
            SpeechPackage("kokoro", "Kokoro model with 34 voices", null, "English, Spanish, French, Italian, Portuguese", 120_500_000, installed),
        )
        override suspend fun installPackage(id: String) { installed = true }
        override suspend fun removePackage(id: String) { installed = false }
        override suspend fun voices(languageCode: String) =
            if (installed && languageCode == "pt") listOf(SpeechVoice("pm_alex", "Alex (Brazilian, male)", "pt"), SpeechVoice("pf_dora", "Dora (Brazilian, female)", "pt")) else emptyList()
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? = null
    }

    /** Piper: one download per voice. */
    private class FakePiper : LocalSpeechEngine {
        val installed = mutableSetOf("pt_BR-faber-medium")
        private val catalog = listOf(
            SpeechPackage("pt_BR-faber-medium", "Faber (Brazil, medium)", "pt", "Portuguese", 63_000_000, false),
            SpeechPackage("pt_PT-tugão-medium", "Tugão (Portugal, medium)", "pt", "Portuguese", 63_000_000, false),
            SpeechPackage("pt_BR-cadu-medium", "Cadu (Brazil, medium)", "pt", "Portuguese", 63_000_000, false),
            SpeechPackage("cs_CZ-jirka-low", "Jirka (Czechia, low)", "cs", "Czech", 28_000_000, false),
            SpeechPackage("de_DE-thorsten-high", "Thorsten (Germany, high)", "de", "German", 114_000_000, false),
        )
        override val engine = SpeechEngine.PIPER
        override val displayName = "Piper"
        override val description = "Piper is a neural speech engine that runs on this computer with no network."
        override val packagesDescription = "One download per voice, 20 to 120 MB each."
        override val hasRuntimeSetup = true
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun status() = "Piper 1.3.0 · Python 3.12.14 · ${installed.size} voices downloaded"
        override suspend fun isReady() = true
        override suspend fun setUp() = "ready"
        override suspend fun packages() = catalog.map { it.copy(installed = it.id in installed) }
        override suspend fun installPackage(id: String) { installed += id }
        override suspend fun removePackage(id: String) { installed -= id }
        override suspend fun voices(languageCode: String) =
            packages().filter { it.installed && it.languageCode == languageCode }.map { SpeechVoice(it.id, it.title, languageCode) }
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? = null
    }

    private fun ComposeUiTest.show(engine: SpeechEngine, kokoro: FakeKokoro, piper: FakePiper): SettingsRepositoryImpl {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-speech-settings", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        runBlocking {
            val portuguese = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = portuguese, speechEngine = engine, speechSpeed = 0.8f) }
        }
        val viewModel = SpeechViewModel(settings, languages, LocalSpeech(listOf(piper, kokoro)))
        setContent { HostedSpeech(settings) { SpeechScreen(onNavigate = {}, onBack = {}, viewModel = viewModel) } }
        return settings
    }

    @Test
    fun kokoroShowsItsModelAndTheChosenVoice() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val kokoro = FakeKokoro()
        val settings = show(SpeechEngine.KOKORO, kokoro, FakePiper())
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Offline speech ready")).fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Alex (Brazilian, male)")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Portuguese uses Alex (Brazilian, male).", substring = true).assertExists()
        onNodeWithText("80%").assertExists()
        System.getenv("SPEECH_SCREENSHOT")?.let { save(it) }

        onNodeWithContentDescription("Increase speech speed").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.speechSpeed == 0.85f }

        // Removing the model leaves Portuguese without a Kokoro voice.
        onNodeWithContentDescription("More for Kokoro model with 34 voices").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Remove")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Remove").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("No Portuguese voice yet")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Download").assertExists()
    }

    @Test
    fun piperListsVoicesToDownloadForTheLanguage() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val piper = FakePiper()
        show(SpeechEngine.PIPER, FakeKokoro(), piper)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Faber (Brazil, medium)")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("SPEECH_SCREENSHOT")?.let { save(it.replace(".png", "-piper.png")) }

        // Available offers the other Portuguese voices; other languages appear when searched for.
        onNodeWithText("Available").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Cadu (Brazil, medium)")).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(onAllNodes(hasText("Jirka (Czechia, low)")).fetchSemanticsNodes().isEmpty())
        onAllNodes(hasText("Download") and hasClickAction())[0].performClick()
        waitUntil(timeoutMillis = 5_000) { piper.installed.size == 2 }
        onNodeWithText("Search voices").performTextInput("czech")
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Jirka (Czechia, low)")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, piper.installed.size)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}

@Composable
private fun HostedSpeech(settings: SettingsRepositoryImpl, content: @Composable () -> Unit) {
    val current by settings.settings.collectAsState()
    TayraTheme(AppThemes.byId(current.themeId)) {
        ProvideWindowWidth {
            CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(current.currentLanguageId to "Portuguese"), current.currentLanguageId) {}, content = content)
        }
    }
}
