package com.idiomiko.feature.terms.list

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
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
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.export.AnkiPackagerImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.dictionary.DictionaryId
import com.idiomiko.core.domain.dictionary.OfflineDictionary
import com.idiomiko.core.domain.export.AnkiExportService
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.TermDraft
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.practice.PracticeResponse
import com.idiomiko.core.domain.practice.PracticeSetup
import com.idiomiko.core.domain.service.LocalSpeech
import com.idiomiko.core.domain.service.MemorySpeechAudioCache
import com.idiomiko.core.domain.service.SentenceAudio
import com.idiomiko.core.domain.service.SentenceTranslator
import com.idiomiko.core.domain.service.TermService
import com.idiomiko.core.domain.settings.SettingsRepository
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.io.File
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel
import com.idiomiko.core.domain.practice.PracticeSource
import com.idiomiko.core.domain.practice.PracticeMaterialLoader
import com.idiomiko.core.domain.service.ReadingService
import com.idiomiko.core.data.repository.WordsReadRepositoryImpl
import com.idiomiko.core.data.repository.BookRepositoryImpl

/** The vocabulary filters apply as they change: statuses ticked as chips, the days since a term was added, and removable chips for what is on. */
@OptIn(ExperimentalTestApi::class)
class TermsScreenTest {

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-terms", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = TermService(terms, languages)
    private val sentenceAudio = SentenceAudio(LocalSpeech(emptyList()), settings, MemorySpeechAudioCache())

    private var languageId = 0L

    @AfterTest
    fun tearDown() = stopKoin()

    private fun ComposeUiTest.show(onNavigate: (Route) -> Unit = {}, extra: Int = 0, termIds: List<Long>? = null, dialog: Boolean = false, seed: Boolean = true, more: suspend (Long) -> Unit = {}) {
        val languageId = runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = id) }
            if (seed) listOf(
                "gostar" to TermStatus.WELL_KNOWN, "comprar" to TermStatus.NEW_1, "produtos" to TermStatus.NEW_1,
                "expressão" to TermStatus.WELL_KNOWN, "pensar" to TermStatus.LEARNING_3, "lá" to TermStatus.IGNORED,
            ).forEach { (text, status) -> service.save(TermDraft(languageId = id, text = text, status = status, statusExplicitlySet = true)) }
            repeat(extra) { service.save(TermDraft(languageId = id, text = "palavra$it", status = TermStatus.NEW_1, statusExplicitlySet = true)) }
            more(id)
            id
        }
        this@TermsScreenTest.languageId = languageId
        val books = BookRepositoryImpl(provider)
        val material = PracticeMaterialLoader(books, languages, ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), service), terms)
        val noSentences = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null }
        startKoin {
            modules(module {
                single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings }; single { sentenceAudio }
                viewModel { (source: PracticeSource) -> PracticeSetupViewModel(source, material, noSentences, settings) }
            })
        }
        val noDictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val noTranslator = object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null }
        val anki = AnkiExportService(terms, languages, noDictionary, noTranslator, sentenceAudio, settings, AnkiPackagerImpl())
        val viewModel = TermsListViewModel(termIds, terms, languages, settings, service, anki)
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {}) {
                        TermsScreen(
                            termIds = termIds, onNavigate = onNavigate, onBack = if (termIds != null) ({}) else null, viewModel = viewModel,
                            dialog = dialog, title = "Lesson vocabulary".takeIf { dialog }, subtitle = "Uma amizade antiga".takeIf { dialog },
                        )
                    }
                }
            }
        }
        // The newest term comes first: with many more added, the first of them.
        if (seed && termIds?.isEmpty() != true) waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(if (extra > 0) "palavra${extra - 1}" else "comprar").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.listed(word: String) = onAllNodesWithText(word).fetchSemanticsNodes().isNotEmpty()

    /** Whether [text] shows inside the node tagged [tag]. */
    private fun ComposeUiTest.shownIn(tag: String, text: String) =
        onAllNodes(hasText(text) and hasAnyAncestor(hasTestTag(tag))).fetchSemanticsNodes().isNotEmpty()

    private fun ComposeUiTest.screenshot(suffix: String) =
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it.replace(".png", "$suffix.png"))) }

    /** The overview counts the whole vocabulary: the total, the known words' share of those known and learning in the ring, and both in the legend. */
    @Test
    fun theOverviewShowsTheKnownShareAndBothCounts() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        // Two known and three learning; the ignored word counts in neither.
        waitUntil(timeoutMillis = 5_000) {
            shownIn("vocabulary-overview", "5") && shownIn("legend-known", "2") && shownIn("legend-learning", "3")
        }
        onNodeWithText("Overview").assertExists()
        onNodeWithText("All vocabulary").assertDoesNotExist()
        assertTrue(shownIn("vocabulary-ring", "40%") && shownIn("vocabulary-ring", "known"))
        assertTrue(shownIn("legend-known", "Known") && shownIn("legend-known", "40%"))
        assertTrue(shownIn("legend-learning", "Learning") && shownIn("legend-learning", "60%"))
        screenshot("-overview")
    }

    /** A window narrower than a desktop's keeps the whole overview, wrapping it onto two lines where it must. */
    @Test
    fun aMediumWindowKeepsTheWholeOverview() = runDesktopComposeUiTest(width = 1000, height = 1000) {
        show()
        waitUntil(timeoutMillis = 5_000) { shownIn("legend-learning", "3") }
        onNodeWithText("Total terms").assertExists()
        assertTrue(shownIn("legend-known", "40%") && shownIn("legend-learning", "60%"))
        screenshot("-medium")
    }

    @Test
    fun aNarrowWindowWrapsTheOverview() = runDesktopComposeUiTest(width = 760, height = 1000) {
        show()
        waitUntil(timeoutMillis = 5_000) { shownIn("legend-learning", "3") }
        assertTrue(shownIn("vocabulary-ring", "40%") && shownIn("legend-learning", "60%"))
        screenshot("-narrow")
    }

    /** Large counts are grouped as the interface language writes them, and the shares keep a tenth of a percent. */
    @Test
    fun theOverviewGroupsLargeCounts() = runDesktopComposeUiTest(width = 1586, height = 640) {
        setContent {
            IdiomikoTheme {
                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(32.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp)) {
                    VocabularyOverview(19159, known = 18470, learning = 689)
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.width(360.dp)) { PhoneVocabularyOverview(19159, known = 18470, learning = 689) }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { listed("19,159") && listed("18,470") }
        assertEquals(2, onAllNodes(hasText("96.4%") and hasAnyAncestor(hasTestTag("vocabulary-ring"))).fetchSemanticsNodes().size)
        assertEquals(2, onAllNodes(hasText("3.6%") and hasAnyAncestor(hasTestTag("legend-learning"))).fetchSemanticsNodes().size)
        screenshot("-overview-card")
    }

    /** A language with no terms yet shows a grey ring at 0%, not a share of nothing. */
    @Test
    fun anEmptyLanguagesOverviewShowsNoShare() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(seed = false)
        waitUntil(timeoutMillis = 5_000) { shownIn("vocabulary-ring", "0%") }
        assertTrue(shownIn("vocabulary-overview", "0"))
        assertTrue(shownIn("legend-known", "0%") && shownIn("legend-learning", "0%"))
        screenshot("-overview-empty")
    }

    private fun ComposeUiTest.waitForList(vararg shown: String, hidden: List<String>) =
        waitUntil(timeoutMillis = 5_000) { shown.all { listed(it) } && hidden.none { listed(it) } }

    @Test
    fun tickingStatusesAndDaysNarrowsTheListAtOnce() = runDesktopComposeUiTest(width = 1586, height = 1200) {
        show()
        // The screen opens on the words being learned: statuses 1 to 4 are ticked, known and ignored terms stay out.
        waitForList("comprar", "produtos", "pensar", hidden = listOf("gostar", "expressão", "lá"))
        // The four learning statuses read as one filter, and the line says how many terms that leaves.
        onNodeWithContentDescription("1 filter on").assertExists()
        onNodeWithText("Learning 1–4 · 3 terms").assertExists()
        onAllNodesWithText("Clear filters")[0].performClick()
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

        onAllNodesWithText("Clear filters")[0].performClick()
        waitForList("gostar", "comprar", "pensar", hidden = listOf("lá"))
        assertEquals(0, onAllNodesWithText("Added: up to 30 days ago").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithContentDescription("filters on", substring = true).fetchSemanticsNodes().size)
        // The chips themselves bring the learning words back; the panel has no reset of its own.
        onNodeWithText("Show learning words").assertDoesNotExist()
    }

    /** Over a lesson the list keeps its toolbar on one line, and each row labels its translation and example. */
    @Test
    fun aLessonsWordsListOverTheReader() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(termIds = (1L..9L).toList(), dialog = true) { id ->
            listOf(
                Triple("coração", "анат. сердце", "O coração nunca envelhece."),
                Triple("felicidade", "счастье", "Não se pode comprar a felicidade."),
                Triple("amizade", "дружба", "Acredito na amizade."),
            ).forEach { (text, translation, sentence) ->
                service.save(TermDraft(languageId = id, text = text, translation = translation, status = TermStatus.NEW_1, statusExplicitlySet = true), sentence = sentence)
            }
        }
        waitForList("amizade", "comprar", hidden = listOf("gostar"))
        onNodeWithText("Acredito na amizade.").assertExists()
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it.replace(".png", "-dialog.png"))) }
    }

    /** A reader's page opens on its words being learned; showing all words adds its known ones and stays on the page. */
    @Test
    fun aPageOpensOnItsLearningWords() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        // gostar (known), comprar (learning 1) and pensar (learning 3) are on the page; produtos is not.
        show(termIds = listOf(1L, 2L, 5L))
        waitForList("comprar", "pensar", hidden = listOf("gostar", "produtos"))
        onAllNodesWithText("Clear filters")[0].performClick()
        waitForList("gostar", "comprar", "pensar", hidden = listOf("produtos"))
    }

    /** A reader's page whose words are none of them saved yet lists nothing, not the whole vocabulary. */
    @Test
    fun aPageWithNoSavedWordsListsNone() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show(termIds = emptyList())
        waitUntil(timeoutMillis = 5_000) { listed("No saved words here yet.") }
        assertEquals(false, listed("comprar"))
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it.replace(".png", "-empty.png"))) }
    }

    /** Chosen terms can be practised: the selection bar asks which kinds of question, any of them, then opens the practice on them. */
    @Test
    fun chosenTermsArePractisedWithTheKindsTicked() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val visited = mutableListOf<Route>()
        show(onNavigate = { visited += it })
        waitForList("comprar", "pensar", hidden = listOf("gostar"))
        // Ticked through the rows' checkboxes: the header's ticks all, the rows' one each.
        val rows = onAllNodes(androidx.compose.ui.test.isToggleable())
        rows[1].performClick()
        rows[2].performClick()
        // The ticked terms' actions show in a bar above the list, with how many are chosen.
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("2 of 3 selected").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("terms-selection-bar").assertExists()
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it.replace(".png", "-selection.png"))) }
        onNodeWithContentDescription("List actions").assertDoesNotExist()
        // Practised in the sentences they were saved with.
        runBlocking {
            for ((text, sentence) in listOf("comprar" to "Vou comprar pão.", "produtos" to "Os produtos são bons.", "pensar" to "Vou pensar nisso.")) {
                terms.findByTextLc(languageId, text)?.let { terms.save(it.copy(sentence = sentence)) }
            }
        }
        onNodeWithTag("selection-practise").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Practise 2 words").fetchSemanticsNodes().isNotEmpty() }
        // Every question of the balanced setup is ticked to begin with; answering by typing is unticked here.
        onNodeWithTag("practice-response-TYPE").performClick()
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot()).onLast().captureToImage().toAwtImage(), "png", File(it.replace(".png", "-practice-dialog.png"))) }
        onNodeWithTag("start-practice").performClick()
        waitUntil(timeoutMillis = 5_000) { visited.isNotEmpty() }
        val route = visited.single() as Route.PracticeWords
        val setup = PracticeSetup.decode(route.setup)
        assertEquals(PracticeSetup.FIRST.types, setup.types)
        assertEquals(setOf(PracticeResponse.CHOOSE), setup.responses)
        // Kept for the next practice, from the reader as well.
        waitUntil(timeoutMillis = 5_000) { settings.current.practiceSetupVocabulary == route.setup }
        assertEquals(route.setup, settings.current.practiceSetupReading)
        assertEquals(2, route.termIds.size)
        assertTrue(route.termIds.all { id -> runBlocking { terms.getById(id) }?.text in setOf("comprar", "pensar", "produtos") })

        // Opened again on the same screen, the dialog starts from the practice just started, not from where it first began.
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("start-practice").fetchSemanticsNodes().isEmpty() }
        onNodeWithTag("selection-practise").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("start-practice").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("start-practice").performClick()
        waitUntil(timeoutMillis = 5_000) { visited.size == 2 }
        assertEquals(route.setup, (visited[1] as Route.PracticeWords).setup)
    }

    /** A phone keeps the filters in a sheet: statuses in two columns, the days picked from a list, and a button that closes it. */
    @Test
    fun aPhoneFiltersFromASheet() = runDesktopComposeUiTest(width = 400, height = 1100) {
        show()
        waitForList("comprar", "produtos", "pensar", hidden = listOf("gostar", "expressão", "lá"))
        onNodeWithText("Learning 1–4 · 3 terms").assertExists()
        onNodeWithText("Filter vocabulary").assertDoesNotExist()
        waitUntil(timeoutMillis = 5_000) { shownIn("legend-known", "40%") && shownIn("legend-learning", "3") }
        onNodeWithText("Overview").assertExists()
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
        onNodeWithText("Show learning words").assertDoesNotExist()
    }

    /** On a phone Add term floats at the bottom corner, apart from the search, and opens a new term. */
    @Test
    fun aPhoneAddsATermFromTheFloatingButton() = runDesktopComposeUiTest(width = 400, height = 1100) {
        val visited = mutableListOf<Route>()
        show(onNavigate = { visited += it })
        waitForList("comprar", "produtos", "pensar", hidden = listOf("gostar"))
        val add = onNodeWithTag("phone-add-term").fetchSemanticsNode().boundsInRoot
        val root = onAllNodes(isRoot())[0].fetchSemanticsNode().boundsInRoot
        assertTrue(root.right - add.right < 40f && root.bottom - add.bottom < 40f, "at the bottom corner: $add in $root")
        onNodeWithTag("phone-add-term").performClick()
        assertEquals(listOf<Route>(Route.NewTerm), visited)
    }

    /**
     * On a phone every term has its checkbox, which alone chooses it: a tap on the card opens the term. Choosing brings a
     * bar to the foot with how many and what to do with them; its cross clears the choice. Anki is among its actions; the top bar has none.
     */
    @Test
    fun aPhonesSelectionShowsItsActionsAtTheFoot() = runDesktopComposeUiTest(width = 400, height = 1100) {
        val visited = mutableListOf<Route>()
        show(onNavigate = { visited += it })
        waitForList("comprar", "produtos", "pensar", hidden = listOf("gostar"))
        onNodeWithContentDescription("List actions").assertDoesNotExist()
        onNodeWithContentDescription("Export to Anki").assertDoesNotExist()
        assertEquals(3, onAllNodes(androidx.compose.ui.test.isToggleable()).fetchSemanticsNodes().size, "a checkbox on every term")
        onNodeWithTag("phone-selection-bar").assertDoesNotExist()

        // A tap on the card opens the term and chooses nothing.
        onNodeWithText("pensar").performClick()
        waitForIdle()
        onNodeWithTag("phone-selection-bar").assertDoesNotExist()

        onAllNodes(androidx.compose.ui.test.isToggleable())[0].performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("1 selected").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("phone-add-term").assertDoesNotExist()
        onNodeWithTag("selection-anki").assertExists()
        // Select all ticks every term the filters list; again, it clears them.
        onNodeWithTag("selection-all").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("3 selected").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it.replace(".png", "-phone-selection.png"))) }
        onNodeWithTag("selection-all").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("phone-selection-bar").fetchSemanticsNodes().isEmpty() }
        onAllNodes(androidx.compose.ui.test.isToggleable())[0].performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("1 selected").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("selection-delete").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Delete 1 term?").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Cancel").performClick()
        onNodeWithContentDescription("Clear selection").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("phone-selection-bar").fetchSemanticsNodes().isEmpty() }
        onNodeWithTag("phone-add-term").assertExists()
    }

    /** A phone's list comes in pages, as a wide window's: the pager under it turns them. */
    @Test
    fun aPhonesListComesInPages() = runDesktopComposeUiTest(width = 400, height = 1100) {
        show(extra = 120)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Learning 1–4 · 123 terms").fetchSemanticsNodes().isNotEmpty() }
        onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Next page"))
        onNodeWithText("1–50 of 123").assertExists()
        System.getenv("TERMS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it.replace(".png", "-phone-pager.png"))) }
        onNodeWithContentDescription("Next page").performSemanticsAction(SemanticsActions.OnClick)
        // The next page is read from its top: the sort shows again, the pager is out of sight below.
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("phone-sort").fetchSemanticsNodes().isNotEmpty() }
        onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Next page"))
        onNodeWithText("51–100 of 123").assertExists()
    }
}
