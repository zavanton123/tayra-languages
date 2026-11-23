package com.idiomiko.feature.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.BookRepositoryImpl
import com.idiomiko.core.data.repository.CourseRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.domain.books.BookPack
import com.idiomiko.core.domain.books.BookPackService
import com.idiomiko.core.domain.books.BookPackStore
import com.idiomiko.core.domain.books.PackedBook
import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.courses.CoursePack
import com.idiomiko.core.domain.courses.CoursePackService
import com.idiomiko.core.domain.courses.CoursePackStore
import com.idiomiko.core.domain.courses.CourseService
import com.idiomiko.core.domain.courses.InstalledCoursePacks
import com.idiomiko.core.domain.dictionary.DictionaryEntry
import com.idiomiko.core.domain.dictionary.DictionaryId
import com.idiomiko.core.domain.dictionary.DictionaryPack
import com.idiomiko.core.domain.dictionary.DictionaryPackStore
import com.idiomiko.core.domain.repository.DictionaryRepository
import com.idiomiko.core.domain.service.BookService
import com.idiomiko.core.domain.service.DictionaryService
import com.idiomiko.core.domain.service.LanguageAssets
import com.idiomiko.core.domain.service.LanguageSetupService
import com.idiomiko.core.domain.service.LocalTranslation
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.service.LocalSpeech
import com.idiomiko.core.domain.service.LocalSpeechEngine
import com.idiomiko.core.domain.service.SpeechEngine
import com.idiomiko.core.domain.service.SpeechPackage
import com.idiomiko.core.domain.service.SpeechVoice
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
import com.idiomiko.core.domain.practice.SpeechRecognitionSetup
import com.idiomiko.core.domain.practice.RecognitionModel
import com.idiomiko.core.domain.practice.RecognitionQuality
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Speech page shows the engine's state, the voice and speed for the language being learned, and downloads its voices and recognition models in place. */
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
        override fun packageVoices(id: String, languageCode: String) =
            if (languageCode == "pt") listOf(SpeechVoice("pm_alex", "Alex (Brazilian, male)", "pt"), SpeechVoice("pf_dora", "Dora (Brazilian, female)", "pt")) else emptyList()
        override suspend fun voices(languageCode: String) =
            if (installed && languageCode == "pt") listOf(SpeechVoice("pm_alex", "Alex (Brazilian, male)", "pt"), SpeechVoice("pf_dora", "Dora (Brazilian, female)", "pt")) else emptyList()
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? = null
    }

    /** Piper: one download per voice, titled as its catalog titles them; [ready] and [runtime] make a broken installation. */
    private class FakePiper : LocalSpeechEngine {
        val installed = mutableSetOf("pt_BR-faber-medium")
        var ready = true
        var runtime = true
        var checks = 0
        var setUps = 0
        private val catalog = listOf(
            SpeechPackage("pt_BR-faber-medium", "Faber \u00b7 medium \u00b7 Brazil", "pt", "Portuguese", 63_000_000, false),
            SpeechPackage("pt_PT-tugão-medium", "Tugão \u00b7 medium \u00b7 Portugal", "pt", "Portuguese", 63_000_000, false),
            SpeechPackage("pt_BR-cadu-medium", "Cadu \u00b7 medium \u00b7 Brazil", "pt", "Portuguese", 63_000_000, false),
            SpeechPackage("pt_BR-jeff-medium", "Jeff \u00b7 medium \u00b7 Brazil", "pt", "Portuguese", 63_000_000, false),
            SpeechPackage("pt_BR-edresson-low", "Edresson \u00b7 low \u00b7 Brazil", "pt", "Portuguese", 28_000_000, false),
            SpeechPackage("pt_BR-faber-x_low", "Faber \u00b7 x low \u00b7 Brazil", "pt", "Portuguese", 21_000_000, false),
            SpeechPackage("cs_CZ-jirka-low", "Jirka \u00b7 low \u00b7 Czechia", "cs", "Czech", 28_000_000, false),
            SpeechPackage("de_DE-thorsten-high", "Thorsten \u00b7 high \u00b7 Germany", "de", "German", 114_000_000, false),
        )
        override val engine = SpeechEngine.PIPER
        override val displayName = "Piper"
        override val description = "Piper is a neural speech engine that runs on this computer with no network."
        override val packagesDescription = "One download per voice, 20 to 120 MB each."
        override val hasRuntimeSetup = true
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun status(): String {
            checks++
            return if (ready) "Piper 1.3.0 · Python 3.12.14 · ${installed.size} voices downloaded" else "Piper is not installed yet: No module named 'piper'"
        }
        override suspend fun isReady() = ready
        override suspend fun runtimeInstalled() = runtime
        override suspend fun setUp(): String { setUps++; ready = true; return "ready" }
        override suspend fun packages() = catalog.map { it.copy(installed = it.id in installed) }
        override suspend fun installPackage(id: String) { installed += id }
        override suspend fun removePackage(id: String) { installed -= id }
        override suspend fun voices(languageCode: String) =
            if (!ready) emptyList() else packages().filter { it.installed && it.languageCode == languageCode }.map { SpeechVoice(it.id, it.title, languageCode) }
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? = null
    }

    /** A recogniser whose models download once [installing] lets them finish. */
    private class FakeRecognition : SpeechRecognitionSetup {
        override val progress = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        var installing = CompletableDeferred<Unit>()
        val installed = mutableSetOf<RecognitionQuality>()
        override suspend fun models() = RecognitionQuality.entries.map { RecognitionModel(it, if (it == RecognitionQuality.MEDIUM) 580_000_000L else 1_716_000_000L, it in installed) }
        override fun supports(languageCode: String) = true
        override suspend fun install(quality: RecognitionQuality) {
            progress.value = "Downloading the recognition model (120 of 484 MB)…"
            installing.await()
            progress.value = null
            installed += quality
        }
        override suspend fun remove(quality: RecognitionQuality) {
            installed -= quality
        }
    }

    /** No course, book or dictionary packs: the page lists only the voices and recognition models. */
    private object NoPacks : CoursePackStore, BookPackStore, DictionaryPackStore, DictionaryRepository {
        override suspend fun installedSize(pack: CoursePack): Long? = null
        override suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit) = Unit
        override suspend fun remove(pack: CoursePack) = Unit
        override suspend fun courseIds(pack: CoursePack) = emptySet<String>()
        override suspend fun courses(pack: CoursePack) = emptyList<Course>()
        override suspend fun installedSize(pack: BookPack): Long? = null
        override suspend fun install(pack: BookPack, onProgress: (Float?) -> Unit) = Unit
        override suspend fun remove(pack: BookPack) = Unit
        override suspend fun books(pack: BookPack) = emptyList<PackedBook>()
        override suspend fun installedSize(pack: DictionaryPack): Long? = null
        override suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) = Unit
        override suspend fun remove(pack: DictionaryPack) = Unit
        override suspend fun isAvailable(dictionary: DictionaryId) = false
        override suspend fun entries(dictionary: DictionaryId, wordLc: String) = emptyList<DictionaryEntry>()
        override suspend fun lemmas(dictionary: DictionaryId, formLc: String) = emptyList<String>()
        override suspend fun close(dictionary: DictionaryId) = Unit
    }

    private fun ComposeUiTest.show(engine: SpeechEngine, kokoro: FakeKokoro, piper: FakePiper, recognition: SpeechRecognitionSetup? = null): SettingsRepositoryImpl {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-speech-settings", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        runBlocking {
            val portuguese = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = portuguese, speechEngine = engine, speechSpeed = 0.8f) }
        }
        val speech = LocalSpeech(listOf(piper, kokoro))
        val bookService = BookService(books, languages)
        val coursePacks = CoursePackService(NoPacks, CourseService(books, languages, bookService, CourseRepositoryImpl(provider), InstalledCoursePacks(NoPacks)))
        val setup = LanguageSetupService(languages, settings, coursePacks, DictionaryService(NoPacks, NoPacks), speech, LocalTranslation(null), recognition)
        val assets = LanguageAssets(setup, BookPackService(NoPacks, books, bookService, languages), languages, settings)
        val viewModel = SpeechViewModel(settings, languages, speech, recognition, setup, assets)
        setContent { HostedSpeech(settings) { SpeechScreen(onNavigate = {}, onBack = {}, viewModel = viewModel) } }
        return settings
    }

    private fun ComposeUiTest.waitForTag(tag: String) =
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeUiTest.waitForNoTag(tag: String) =
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }

    /** Kokoro's voices are a row each; clicking one on the device makes it the voice that speaks. */
    @Test
    fun kokoroVoicesAreRowsOfTheirOwnAndOneIsChosenByItsRow() = runDesktopComposeUiTest(width = 1586, height = 1200) {
        val kokoro = FakeKokoro()
        val settings = show(SpeechEngine.KOKORO, kokoro, FakePiper())
        val alex = "voice-voice:KOKORO:kokoro:pm_alex"
        val dora = "voice-voice:KOKORO:kokoro:pf_dora"
        waitForTag("row-$dora")
        onNodeWithText("80%").assertExists()
        onNodeWithText("Portuguese voices").assertExists()
        onNodeWithText("Brazilian · Female · Kokoro").assertExists()
        onNodeWithText("Try this voice").assertDoesNotExist()
        onNodeWithText("Portuguese voice").assertDoesNotExist()

        onNodeWithContentDescription("speech speed").performSemanticsAction(SemanticsActions.SetProgress) { it(0.85f) }
        waitUntil(timeoutMillis = 5_000) { settings.current.speechSpeed == 0.85f }

        // Nothing chosen yet: the engine's first voice speaks, and its row says so.
        onNodeWithTag("row-$alex").assertIsSelected()
        onNodeWithTag("current-$alex").assertExists()
        onNodeWithTag("row-$dora").assertIsNotSelected()
        onNodeWithTag("play-$dora").assertIsEnabled()
        onNodeWithTag("row-$dora").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.speechVoices["KOKORO:pt"] == "pf_dora" }
        waitForTag("current-$dora")
        onNodeWithTag("row-$dora").assertIsSelected()
        onNodeWithTag("row-$alex").assertIsNotSelected()
        onNodeWithTag("current-$alex").assertDoesNotExist()
    }

    /** Before Kokoro's model is on the device each of its voices offers it; downloading one brings them all. */
    @Test
    fun aKokoroVoiceDownloadsTheModelForAllItsVoices() = runDesktopComposeUiTest(width = 1586, height = 1400) {
        val kokoro = FakeKokoro().apply { installed = false }
        show(SpeechEngine.KOKORO, kokoro, FakePiper())
        val alex = "voice-voice:KOKORO:kokoro:pm_alex"
        val dora = "voice-voice:KOKORO:kokoro:pf_dora"
        waitForTag("download-$dora")
        onNodeWithTag("download-$alex").assertExists()
        onNodeWithTag("play-$dora").assertIsNotEnabled()
        onNodeWithTag("download-$dora").performClick()
        waitForTag("remove-$alex")
        assertTrue(kokoro.installed)
        onNodeWithTag("remove-$dora").assertExists()
        onNodeWithTag("play-$alex").assertIsEnabled()
        onNodeWithTag("play-$dora").assertIsEnabled()
    }

    /** The language's voices are downloaded and removed in their rows; the last one that reads it stays. */
    @Test
    fun theVoicesAreDownloadedAndRemovedInPlace() = runDesktopComposeUiTest(width = 1586, height = 1600) {
        val piper = FakePiper()
        val kokoro = FakeKokoro().apply { installed = false }
        val recognition = FakeRecognition().apply { installed += RecognitionQuality.MEDIUM }
        val settings = show(SpeechEngine.PIPER, kokoro, piper, recognition)
        val faber = "voice-voice:PIPER:pt_BR-faber-medium"
        val cadu = "voice-voice:PIPER:pt_BR-cadu-medium"
        waitForTag("download-$cadu")
        assertTrue(onAllNodesWithText("Brazil · Medium quality · Piper").fetchSemanticsNodes().isNotEmpty())
        // Piper's voices only: Kokoro's are listed when Kokoro speaks.
        onNodeWithTag("row-voice-voice:KOKORO:kokoro:pf_dora").assertDoesNotExist()
        // The one voice on the device is the last that reads Portuguese, so it cannot be removed.
        onNodeWithTag("row-$faber").assertIsSelected()
        onNodeWithTag("current-$faber").assertExists()
        onNodeWithTag("remove-$faber").assertDoesNotExist()
        onNodeWithTag("play-$faber").assertIsEnabled()
        onNodeWithTag("play-$cadu").assertIsNotEnabled()
        onNodeWithText("Voices").assertDoesNotExist()
        onNodeWithText("Advanced engine settings").assertDoesNotExist()
        System.getenv("SPEECH_SCREENSHOT")?.let { save(it) }

        onNodeWithTag("download-$cadu").performScrollTo().performClick()
        waitForTag("remove-$cadu")
        assertTrue("pt_BR-cadu-medium" in piper.installed)
        onNodeWithTag("play-$cadu").assertIsEnabled()
        // With two voices on the device, either can go, and the new one is chosen by its row.
        waitForTag("remove-$faber")
        onNodeWithTag("row-$cadu").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.speechVoices["PIPER:pt"] == "pt_BR-cadu-medium" }
        waitForTag("current-$cadu")
        onNodeWithTag("row-$cadu").assertIsSelected()

        onNodeWithTag("remove-$faber").performScrollTo().performClick()
        onNodeWithTag("confirm-remove").performClick()
        waitForNoTag("remove-$faber")
        waitForTag("download-$faber")
        assertEquals(setOf("pt_BR-cadu-medium"), piper.installed)
        onNodeWithTag("remove-$cadu").assertDoesNotExist()
    }

    /** A long list shows the voices on the device and enough others to make four, the rest behind Browse more voices. */
    @Test
    fun browseMoreVoicesShowsTheRest() = runDesktopComposeUiTest(width = 1586, height = 1600) {
        show(SpeechEngine.PIPER, FakeKokoro().apply { installed = false }, FakePiper())
        waitForTag("voices-more")
        assertEquals(4, onAllNodes(hasTestTagPrefix("row-voice-voice:PIPER:")).fetchSemanticsNodes().size)
        onNodeWithTag("row-voice-voice:PIPER:pt_BR-faber-x_low").assertDoesNotExist()
        onNodeWithText("Browse more voices").performScrollTo().performClick()
        waitForTag("row-voice-voice:PIPER:pt_BR-faber-x_low")
        assertEquals(6, onAllNodes(hasTestTagPrefix("row-voice-voice:PIPER:")).fetchSemanticsNodes().size)
        onNodeWithText("Brazil · Very low quality · Piper").assertExists()
        System.getenv("SPEECH_SCREENSHOT")?.let { save(it.replace(".png", "-expanded.png")) }
        onNodeWithText("Show fewer").performScrollTo().performClick()
        waitForNoTag("row-voice-voice:PIPER:pt_BR-faber-x_low")
    }

    /** The installation of the engine is checked whenever the page opens; a working one shows no status box and no reinstall. */
    @Test
    fun theEngineIsCheckedOnOpeningAndAWorkingOneShowsNothing() = runDesktopComposeUiTest(width = 1586, height = 1200) {
        val piper = FakePiper()
        show(SpeechEngine.PIPER, FakeKokoro(), piper)
        waitUntil(timeoutMillis = 5_000) { piper.checks >= 1 }
        waitForIdle()
        onNodeWithText("Installed and working").assertDoesNotExist()
        onNodeWithText("Check installation").assertDoesNotExist()
        onNodeWithTag("reinstall-engine").assertDoesNotExist()
        onNodeWithText("Reinstall Piper").assertDoesNotExist()
    }

    /** An engine on the device that does not work shows its reinstall at once, which sets it up again. */
    @Test
    fun aBrokenEngineShowsItsReinstall() = runDesktopComposeUiTest(width = 1586, height = 1200) {
        val piper = FakePiper().apply { ready = false }
        show(SpeechEngine.PIPER, FakeKokoro(), piper)
        waitForTag("reinstall-engine")
        onNodeWithText("Something went wrong").assertExists()
        onNodeWithText("Reinstall Piper").assertExists()
        onNodeWithText("Check installation").assertExists()
        onNodeWithTag("reinstall-engine").performClick()
        waitUntil(timeoutMillis = 5_000) { piper.setUps == 1 }
        waitForNoTag("reinstall-engine")
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Something went wrong")).fetchSemanticsNodes().isEmpty() }
    }

    /** An engine never installed offers Install, not a reinstall. */
    @Test
    fun anEngineNotInstalledOffersInstall() = runDesktopComposeUiTest(width = 1586, height = 1200) {
        val piper = FakePiper().apply { ready = false; runtime = false }
        show(SpeechEngine.PIPER, FakeKokoro(), piper)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Not installed")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Install").assertExists()
        onNodeWithTag("reinstall-engine").assertDoesNotExist()
    }

    /** A phone has the same voice rows under the speed; a missing voice is downloaded from the notice. */
    @Test
    fun aPhoneDownloadsAMissingVoiceFromTheNotice() = runDesktopComposeUiTest(width = 400, height = 2600) {
        val piper = FakePiper()
        piper.installed.clear()
        val settings = show(SpeechEngine.PIPER, FakeKokoro(), piper, FakeRecognition())
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Installed; no voice for this language yet")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("80%").assertExists()
        onNodeWithText("Reader playback and term pronunciation use this engine.").assertDoesNotExist()
        waitForTag("notice-download-voice")
        waitForTag("download-voice-voice:PIPER:pt_BR-cadu-medium")
        onNodeWithTag("play-voice-voice:PIPER:pt_BR-cadu-medium").assertIsNotEnabled()
        onNodeWithTag("notice-download-voice").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { "pt_BR-faber-medium" in piper.installed }
        assertEquals(setOf("pt_BR-faber-medium"), piper.installed, "the engine's own voice, as the app would fetch")
        onNodeWithTag("notice-download-voice").assertDoesNotExist()
        waitForTag("current-voice-voice:PIPER:pt_BR-faber-medium")
        onNodeWithTag("row-voice-voice:PIPER:pt_BR-faber-medium").assertIsSelected()
        onNodeWithTag("play-voice-voice:PIPER:pt_BR-faber-medium").assertIsEnabled()
        onNodeWithTag("download-voice-voice:PIPER:pt_BR-cadu-medium").performScrollTo().performClick()
        waitForTag("remove-voice-voice:PIPER:pt_BR-cadu-medium")
        onNodeWithTag("row-voice-voice:PIPER:pt_BR-cadu-medium").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.speechVoices["PIPER:pt"] == "pt_BR-cadu-medium" }
        waitForTag("current-voice-voice:PIPER:pt_BR-cadu-medium")
        onNodeWithTag("phone-speech-speed").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(1.0f) }
        waitUntil(timeoutMillis = 5_000) { settings.current.speechSpeed == 1.0f }
        onNodeWithTag("phone-speech-speed").performScrollTo()
        System.getenv("SPEECH_SCREENSHOT")?.let { save(it.replace(".png", "-phone.png")) }
    }

    /** The models on the device are chosen between here. */
    @Test
    fun theRecognitionModelOnTheDeviceIsChosen() = runDesktopComposeUiTest(width = 1586, height = 1400) {
        val recognition = FakeRecognition()
        recognition.installed += RecognitionQuality.MEDIUM
        recognition.installed += RecognitionQuality.HIGH
        val settings = show(SpeechEngine.PIPER, FakeKokoro(), FakePiper(), recognition)
        waitForTag("remove-recognition-high")
        System.getenv("SPEECH_SCREENSHOT")?.let { save(it.replace(".png", "-recognition.png")) }
        onNodeWithTag("recognition-model-HIGH").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.speechRecognitionQuality == "HIGH" }
        onNodeWithTag("recognition-model-HIGH").assertIsSelected()
        onNodeWithTag("current-recognition-high", useUnmergedTree = true).assertExists()
        onNodeWithTag("recognition-model-MEDIUM").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.speechRecognitionQuality == "MEDIUM" }
        onNodeWithTag("recognition-model-MEDIUM").assertIsSelected()
        onNodeWithTag("current-recognition-high", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag("download-recognition-medium").assertDoesNotExist()
    }

    /** A model is downloaded from the recognition card, its progress shown on its row, and removed there too. */
    @Test
    fun aRecognitionModelIsDownloadedAndRemovedFromItsCard() = runDesktopComposeUiTest(width = 1586, height = 1400) {
        val recognition = FakeRecognition()
        val settings = show(SpeechEngine.PIPER, FakeKokoro(), FakePiper(), recognition)
        waitForTag("download-recognition-high")
        onNodeWithText("Not installed").assertExists()
        onNodeWithText("Not downloaded").assertDoesNotExist()

        onNodeWithTag("download-recognition-medium").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Downloading the recognition model (120 of 484 MB)…")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("download-recognition-medium").assertDoesNotExist()
        recognition.installing.complete(Unit)
        waitForTag("remove-recognition-medium")
        assertEquals("MEDIUM", settings.current.speechRecognitionQuality, "the first model downloaded hears")
        onNodeWithText("Installed").assertExists()

        onNodeWithTag("remove-recognition-medium").performClick()
        onNodeWithTag("confirm-remove").performClick()
        waitForTag("download-recognition-medium")
        assertTrue(recognition.installed.isEmpty())
    }

    @Test
    fun withNothingToSetUpThereIsNoRecognitionCard() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(SpeechEngine.PIPER, FakeKokoro(), FakePiper())
        waitForIdle()
        onNodeWithTag("speech-recognition").assertDoesNotExist()
    }

    private fun hasTestTagPrefix(prefix: String) =
        SemanticsMatcher("test tag starts with $prefix") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}

@Composable
private fun HostedSpeech(settings: SettingsRepositoryImpl, content: @Composable () -> Unit) {
    val current by settings.settings.collectAsState()
    IdiomikoTheme(AppThemes.byId(current.themeId)) {
        ProvideWindowWidth {
            CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(current.currentLanguageId to "Portuguese"), current.currentLanguageId) {}, content = content)
        }
    }
}
