package com.tayra.languages.feature.reading

import com.russhwolf.settings.MapSettings
import kotlin.test.assertTrue
import org.koin.dsl.module
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.junit.Rule
import com.tayra.languages.core.domain.settings.SettingsRepository
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Offset
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.BookStatsService
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermPopupBuilder
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadingHoverTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var settings: SettingsRepositoryImpl

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    /** [mainIsDefault] swaps the UI thread for a pool, for tests that never draw the screen. */
    private suspend fun reader(mainIsDefault: Boolean = true): ReadingViewModel {
        if (mainIsDefault) Dispatchers.setMain(Dispatchers.Default)
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-hover", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        settings = SettingsRepositoryImpl(MapSettings())
        val termService = TermService(terms, languages)
        val readingService = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), termService)
        val bookService = BookService(books, languages)
        val languageId = languages.save(Language(name = "Portuguese"))
        val bookId = bookService.create(BookDraft(languageId = languageId, title = "T", text = "O lobo dorme na floresta."))
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? = "<$text>"
        }
        val offline = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
        }
        val vm = ReadingViewModel(
            bookId, null, readingService, bookService, books, termService,
            TermPopupBuilder(terms, languages, readingService), BookStatsService(books, languages, settings, readingService), settings,
            object : SentenceTranslator { override suspend fun translate(text: String, language: Language): String? = null },
            LocalTranslation(null), LocalSpeech(emptyList()), WordTranslationService(terms, offline, engine, settings),
        )
        withTimeout(10_000) { while (vm.state.value.items.none { it.isWord }) delay(20) }
        return vm
    }

    private fun ReadingViewModel.index(word: String) = state.value.items.indexOfFirst { it.isWord && it.renderText == word }

    /** Hovers [word] and returns the translation on the card that appears, or null. */
    private suspend fun ReadingViewModel.hoverCard(word: String): String? {
        onHover(index(word))
        delay(700)
        val card = state.value.popup?.popup?.translation
        onHover(null)
        delay(50)
        return card
    }

    @Test
    fun unsavedWordsGetACardWithALookedUpTranslation() = runBlocking {
        val vm = reader()
        assertEquals("<lobo>", vm.hoverCard("lobo"))
    }

    @Test
    fun hoverKeepsWorkingWhileAnotherWordsPaneIsOpen() = runBlocking {
        val vm = reader()
        vm.onWordClick(vm.index("lobo"), shift = false)
        delay(200)
        assertEquals(setOf(vm.index("lobo")), vm.state.value.marked)
        assertEquals("<floresta>", vm.hoverCard("floresta"))
        assertNull(vm.hoverCard("lobo"), "the word whose pane is open needs no card")
    }

    /** The whole screen, as the app shows it: hovering a word with the mouse brings up its card. */
    @Test
    fun theReaderScreenShowsTheCardOfTheHoveredWord() {
        val vm = runBlocking { reader(mainIsDefault = false) }
        startKoin { modules(module { single { LocalSpeech(emptyList()) }; single<SettingsRepository> { settings } }) }
        rule.setContent { ReadingScreen(bookId = 1, initialPage = null, onNavigate = {}, onHome = {}, viewModel = vm) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("lobo dorme", substring = true).fetchSemanticsNodes().isNotEmpty() }
        // Matched by a phrase only the page has, since the card repeats the word.
        val paragraph = rule.onNodeWithText("lobo dorme", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        paragraph.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        val text = layout.layoutInput.text.text
        val start = text.indexOf("floresta")
        val box = layout.getBoundingBox(start + 2)
        paragraph.performMouseInput { moveTo(Offset(box.center.x, box.center.y + 6.dp.toPx())) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("<floresta>").fetchSemanticsNodes().isNotEmpty() }
        val card = rule.onNodeWithText("<floresta>").fetchSemanticsNode().boundsInWindow
        val words = paragraph.fetchSemanticsNode().boundsInWindow
        assertTrue(card.bottom <= words.top + box.top + 6.dp.value * 2 + 1, "the card should sit above the word")
    }

    @Test
    fun noCardDuringADragSelection() = runBlocking {
        val vm = reader()
        vm.startSelection(vm.state.value.items[vm.index("lobo")].index)
        assertNull(vm.hoverCard("floresta"))
    }
}
