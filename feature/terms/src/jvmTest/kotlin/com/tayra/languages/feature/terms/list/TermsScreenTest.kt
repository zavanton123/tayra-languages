package com.tayra.languages.feature.terms.list

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.tayra.languages.core.domain.practice.ExerciseType
import com.tayra.languages.core.domain.practice.PracticeResponse
import com.tayra.languages.core.domain.practice.PracticeSetup
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

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

    private fun ComposeUiTest.show(onNavigate: (Route) -> Unit = {}, extra: Int = 0) {
        val languageId = runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = id) }
            listOf(
                "gostar" to TermStatus.WELL_KNOWN, "comprar" to TermStatus.NEW_1, "produtos" to TermStatus.NEW_1,
                "expressão" to TermStatus.WELL_KNOWN, "pensar" to TermStatus.LEARNING_3, "lá" to TermStatus.IGNORED,
            ).forEach { (text, status) -> service.save(TermDraft(languageId = id, text = text, status = status, statusExplicitlySet = true)) }
            repeat(extra) { service.save(TermDraft(languageId = id, text = "palavra$it", status = TermStatus.NEW_1, statusExplicitlySet = true)) }
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
                        TermsScreen(termIds = null, onNavigate = onNavigate, onBack = null, viewModel = viewModel)
                    }
                }
            }
        }
        // The newest term comes first: with many more added, the first of them.
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(if (extra > 0) "palavra${extra - 1}" else "comprar").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.listed(word: String) = onAllNodesWithText(word).fetchSemanticsNodes().isNotEmpty()

    private fun ComposeUiTest.waitForList(vararg shown: String, hidden: List<String>) =
        waitUntil(timeoutMillis = 5_000) { shown.all { listed(it) } && hidden.none { listed(it) } }

    @Test
    fun tickingStatusesAndDaysNarrowsTheListAtOnce() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        // The screen opens on the words being learned: statuses 1 to 4 are ticked, known and ignored terms stay out.
        waitForList("comprar", "produtos", "pensar", hidden = listOf("gostar", "expressão", "lá"))
        // The four learning statuses read as one filter, and the line says how many terms that leaves.
        onNodeWithContentDescription("1 filter on").assertExists()
        onNodeWithText("Learning 1–4 · 3 terms").assertExists()
        onAllNodesWithText("Show all words")[0].performClick()
        // With nothing ticked, the known terms show too; ignored terms stay out until asked for.
        waitForList("gostar", "comprar", "pensar", hidden = listOf("lá"))
        onNodeWithText("Filters").performClick()
        onNodeWithText("Filter vocabulary").assertExists()
        assertEquals(0, onAllNodesWithText("Apply filters").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithText("Cancel").fetchSemanticsNodes().size)

        onNodeWithTag("status-filter-3").performClick()
        onNodeWithTag("status-filter-K").performClick()
        waitForList("gostar", "expressão", "pensar", hidden = listOf("comprar", "produtos", "lá"))
        onNodeWithTag("added-MONTH").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Learning 3, Known · Added in the last 30 days · 3 terms").fetchSemanticsNodes().isNotEmpty() }
        // The badge counts the kinds of filter the line names: statuses, and when the terms were added.
        onNodeWithContentDescription("2 filters on").assertExists()
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }

        onNodeWithTag("status-filter-K").performClick()
        waitForList("pensar", hidden = listOf("gostar", "expressão"))
        onNodeWithTag("status-filter-I").performClick()
        waitForList("pensar", "lá", hidden = listOf("gostar"))

        onAllNodesWithText("Show all words")[0].performClick()
        waitForList("gostar", "comprar", "pensar", hidden = listOf("lá"))
        assertEquals(0, onAllNodesWithText("Added: up to 30 days ago").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithContentDescription("filters on", substring = true).fetchSemanticsNodes().size)

        // The panel's reset goes back to how the screen opened.
        onNodeWithText("Show learning words").performClick()
        waitForList("comprar", "pensar", hidden = listOf("gostar", "lá"))
        onNodeWithContentDescription("1 filter on").assertExists()
    }

    /** Chosen terms can be practised: the list menu asks which kinds of question, any of them, then opens the practice on them. */
    @Test
    fun chosenTermsArePractisedWithTheKindsTicked() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val visited = mutableListOf<Route>()
        show(onNavigate = { visited += it })
        waitForList("comprar", "pensar", hidden = listOf("gostar"))
        // Ticked through the rows' checkboxes: the header's ticks all, the rows' one each.
        val rows = onAllNodes(androidx.compose.ui.test.isToggleable())
        rows[1].performClick()
        rows[2].performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("2 of 3 selected").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription("List actions").performClick()
        onNodeWithText("Practise 2 selected").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Practise 2 words").fetchSemanticsNodes().isNotEmpty() }
        // Every kind is ticked to begin with; the typed ones are unticked here.
        onNodeWithTag("practice-kind-GAP_TYPED").performClick()
        onNodeWithTag("practice-kind-DICTATION").performClick()
        onNodeWithText("3 question types selected").assertExists()
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", File(it.replace(".png", "-practice-dialog.png"))) }
        onNodeWithTag("start-practice").performClick()
        waitUntil(timeoutMillis = 5_000) { visited.isNotEmpty() }
        val route = visited.single() as Route.PracticeWords
        val setup = PracticeSetup.decode(route.setup)
        assertEquals(setOf(ExerciseType.GAP, ExerciseType.MEANING, ExerciseType.HEAR_WORD), setup.types)
        assertEquals(setOf(PracticeResponse.CHOOSE), setup.responses)
        assertEquals(2, route.termIds.size)
        assertTrue(route.termIds.all { id -> runBlocking { terms.getById(id) }?.text in setOf("comprar", "pensar", "produtos") })
    }

    /** A phone keeps the filters in a sheet: statuses in two columns, the days picked from a list, and a button that closes it. */
    @Test
    fun aPhoneFiltersFromASheet() = runDesktopComposeUiTest(width = 400, height = 1100) {
        show()
        waitForList("comprar", "produtos", "pensar", hidden = listOf("gostar", "expressão", "lá"))
        onNodeWithText("Learning 1–4 · 3 terms").assertExists()
        onNodeWithText("Filter vocabulary").assertDoesNotExist()
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it.replace(".png", "-phone.png"))) }

        onNodeWithText("Filters").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Learning status").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("status-filter-K").performClick()
        waitForList("gostar", "expressão", "comprar", "pensar", hidden = listOf("lá"))
        onNodeWithTag("added-MONTH").performScrollTo().performClick()
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", File(it.replace(".png", "-phone-filters.png"))) }
        onNodeWithText("Show 5 terms").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Learning status").fetchSemanticsNodes().isEmpty() }
        onNodeWithContentDescription("2 filters on").assertExists()
        // The page names the filters that are on, so a filtered list never passes for the whole vocabulary.
        onNodeWithText("Learning 1–4, Known · Added in the last 30 days · 5 terms").assertExists()
        // The summary reopens the filters.
        onNodeWithTag("active-filter-summary").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Learning status").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithContentDescription("Close").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Learning status").fetchSemanticsNodes().isEmpty() }

        // The sheet's Show learning words goes back to how the screen opened.
        onNodeWithText("Filters").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Show learning words").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Show learning words").performClick()
        waitForList("comprar", "pensar", hidden = listOf("gostar", "lá"))
        onNodeWithContentDescription("Close").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Learning status").fetchSemanticsNodes().isEmpty() }
        onNodeWithContentDescription("1 filter on").assertExists()
    }

    /** A phone has no pages: the list grows by the next terms as its end comes into view. */
    @Test
    fun aPhonesListLoadsMoreAsItScrolls() = runDesktopComposeUiTest(width = 400, height = 1100) {
        show(extra = 120)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Learning 1–4 · 123 terms").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithContentDescription("Next page").fetchSemanticsNodes().size, "no pager on a phone")
        // The first 50 are listed at the start; scrolling towards their end brings the next ones, until the oldest shows.
        for (index in 40..130 step 10) {
            runCatching { onNode(hasScrollToIndexAction()).performScrollToIndex(index) }
            waitForIdle()
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("palavra0").fetchSemanticsNodes().isNotEmpty() }
    }
}
