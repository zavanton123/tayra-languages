package com.tayra.languages.feature.flashcards

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import com.tayra.languages.core.ui.audio.Speaker
import com.tayra.languages.core.ui.audio.SpeechSynthesizer
import com.tayra.languages.core.ui.audio.WavPlayer
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test

/** The flashcard page keeps the keys: clicks on its buttons leave them with it, and it takes them back when it returns to the front. */
@OptIn(ExperimentalTestApi::class)
class FlashcardsKeysTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-keys", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = FlashcardService(FlashcardRepositoryImpl(provider), terms, settings)

    /** An engine whose sentences never finish being made, so the speaker stays in its playing state, the square. */
    private val engine = object : LocalSpeechEngine {
        override val engine = SpeechEngine.PIPER
        override val displayName = "Fake"
        override val description = ""
        override val packagesDescription = ""
        override val hasRuntimeSetup = false
        override val progress = MutableStateFlow<String?>(null)
        override suspend fun status() = "ready"
        override suspend fun isReady() = true
        override suspend fun setUp() = "ready"
        override suspend fun packages() = emptyList<SpeechPackage>()
        override suspend fun installPackage(id: String) {}
        override suspend fun removePackage(id: String) {}
        override suspend fun voices(languageCode: String) = emptyList<SpeechVoice>()
        override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float): ByteArray? = awaitCancellation()
    }

    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }

    private val owner = Owner()
    private val elsewhere = FocusRequester()

    private fun ComposeUiTest.show(): FlashcardsViewModel {
        val id = runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = id, speechEngine = SpeechEngine.PIPER) }
            terms.save(Term(languageId = id, text = "esconder", textLc = "esconder", status = TermStatus.NEW_1, translation = "прятать", sentence = "Eu sei onde se esconder."))
            terms.save(Term(languageId = id, text = "casa", textLc = "casa", status = TermStatus.NEW_1, translation = "дом", sentence = "A casa é grande."))
            id
        }
        val noDictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val translator = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null }
        val suggestions = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = null
        }
        val viewModel = FlashcardsViewModel(service, languages, settings, translator, noDictionary, WordTranslationService(terms, noDictionary, suggestions, settings))
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                TayraTheme {
                    ProvideWindowWidth {
                        val scope = rememberCoroutineScope()
                        val speaker = remember { Speaker(SpeechSynthesizer(), WavPlayer(), LocalSpeech(listOf(engine)), settings, scope) }
                        LaunchedEffect(Unit) {
                            viewModel.events.flow.collect { event ->
                                when (event) {
                                    is FlashcardsEvent.Speak -> speaker.speak(event.text, event.languageCode)
                                    FlashcardsEvent.StopSpeaking -> speaker.stop()
                                }
                            }
                        }
                        val state by viewModel.state.collectAsState()
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column {
                                // Something else that takes the keyboard, as another screen or window does.
                                androidx.compose.foundation.layout.Box(Modifier.size(1.dp).focusRequester(elsewhere).focusable().testTag("elsewhere"))
                                FlashcardsContent(state, speaker = speaker, actions = FlashcardActions(onReveal = viewModel::reveal, onAnswer = viewModel::answer, onUndo = viewModel::undo))
                            }
                        }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashcard").fetchSemanticsNodes().isNotEmpty() }
        return viewModel
    }

    private fun ComposeUiTest.playing() = onAllNodesWithContentDescription("Stop").fetchSemanticsNodes().isNotEmpty()

    @Test
    fun clicksOnTheCardsButtonsLeaveTheKeysWithThePage() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        onNodeWithText("Show answer").performClick()
        waitUntil(timeoutMillis = 5_000) { playing() }
        onNodeWithTag("flashcards-keys").assertIsFocused()
        // Stopped with the square and started again with the word's button: the page still has the keys.
        onAllNodesWithContentDescription("Stop")[0].performClick()
        waitUntil(timeoutMillis = 3_000) { !playing() }
        onAllNodesWithContentDescription("Pronounce").let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        waitUntil(timeoutMillis = 3_000) { playing() }
        onNodeWithTag("flashcards-keys").assertIsFocused()
        onRoot().performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 3_000) { viewModel.state.value.answered == 1 }
    }

    @Test
    fun thePageTakesTheKeysBackWhenItReturnsToTheFront() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = show()
        onRoot().performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 5_000) { playing() }

        // The window goes to the back, and something else has the keyboard when it comes back.
        runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        runOnIdle { elsewhere.requestFocus() }
        onNodeWithTag("flashcards-keys").assertIsNotFocused()
        runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        waitForIdle()
        onNodeWithTag("flashcards-keys").assertIsFocused()
        onRoot().performKeyInput { pressKey(Key.Spacebar) }
        waitUntil(timeoutMillis = 3_000) { viewModel.state.value.answered == 1 }
    }
}
