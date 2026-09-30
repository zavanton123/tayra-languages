package com.tayra.languages.feature.reading

import com.russhwolf.settings.MapSettings
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

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun reader(): ReadingViewModel {
        Dispatchers.setMain(Dispatchers.Default)
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-hover", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val books = BookRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
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

    @Test
    fun noCardDuringADragSelection() = runBlocking {
        val vm = reader()
        vm.startSelection(vm.state.value.items[vm.index("lobo")].index)
        assertNull(vm.hoverCard("floresta"))
    }
}
