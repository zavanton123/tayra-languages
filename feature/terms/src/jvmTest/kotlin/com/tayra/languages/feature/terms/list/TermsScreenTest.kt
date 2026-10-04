package com.tayra.languages.feature.terms.list

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.export.AnkiPackagerImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.export.AnkiExportService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermDraft
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The vocabulary filters apply as they change: statuses ticked as chips, the days since a term was added, and removable chips for what is on. */
@OptIn(ExperimentalTestApi::class)
class TermsScreenTest {

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-terms", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = TermService(terms, languages)
    private val sentenceAudio = SentenceAudio(LocalSpeech(emptyList()), settings, MemorySpeechAudioCache())

    @AfterTest
    fun tearDown() = stopKoin()

    private fun ComposeUiTest.show() {
        val languageId = runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = id) }
            listOf(
                "gostar" to TermStatus.WELL_KNOWN, "comprar" to TermStatus.NEW_1, "produtos" to TermStatus.NEW_1,
                "expressão" to TermStatus.WELL_KNOWN, "pensar" to TermStatus.LEARNING_3, "lá" to TermStatus.IGNORED,
            ).forEach { (text, status) -> service.save(TermDraft(languageId = id, text = text, status = status, statusExplicitlySet = true)) }
            id
        }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio } }) }
        val noDictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val noTranslator = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null }
        val anki = AnkiExportService(terms, languages, noDictionary, noTranslator, sentenceAudio, settings, AnkiPackagerImpl())
        val viewModel = TermsListViewModel(null, terms, languages, settings, service, anki)
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {}) {
                        TermsScreen(termIds = null, onNavigate = {}, onBack = null, viewModel = viewModel)
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("gostar").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.listed(word: String) = onAllNodesWithText(word).fetchSemanticsNodes().isNotEmpty()

    private fun ComposeUiTest.waitForList(vararg shown: String, hidden: List<String>) =
        waitUntil(timeoutMillis = 5_000) { shown.all { listed(it) } && hidden.none { listed(it) } }

    @Test
    fun tickingStatusesAndDaysNarrowsTheListAtOnce() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        // Ignored terms stay out until asked for.
        assertTrue(!listed("lá"))
        onNodeWithText("Filters").performClick()
        onNodeWithText("Filter vocabulary").assertExists()
        assertEquals(0, onAllNodesWithText("Apply filters").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithText("Cancel").fetchSemanticsNodes().size)

        onNodeWithTag("status-filter-3").performClick()
        onNodeWithTag("status-filter-K").performClick()
        waitForList("gostar", "expressão", "pensar", hidden = listOf("comprar", "produtos", "lá"))
        onNodeWithContentDescription("Added to, days ago").performTextInput("30")
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Added: up to 30 days ago").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription("2 filters on").assertExists()
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }

        onNodeWithContentDescription("Remove Known").performClick()
        waitForList("pensar", hidden = listOf("gostar", "expressão"))
        onNodeWithTag("status-filter-I").performClick()
        waitForList("pensar", "lá", hidden = listOf("gostar"))

        onAllNodesWithText("Clear all")[0].performClick()
        waitForList("gostar", "comprar", "pensar", hidden = listOf("lá"))
        assertEquals(0, onAllNodesWithText("Added: up to 30 days ago").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithContentDescription("filters on", substring = true).fetchSemanticsNodes().size)
    }
}
