package com.idiomiko.feature.books

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.BookRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.data.repository.WordsReadRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.books.discover.BookCatalog
import com.idiomiko.core.domain.books.discover.BookDiscoveryService
import com.idiomiko.core.domain.books.discover.BookSource
import com.idiomiko.core.domain.books.discover.CatalogPage
import com.idiomiko.core.domain.books.discover.FoundBook
import com.idiomiko.core.domain.books.discover.LibraryDownException
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.service.BookService
import com.idiomiko.core.domain.service.ReadingService
import com.idiomiko.core.domain.service.TermService
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.IdiomikoTheme
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The page with its libraries behind it, as fakes: each library's books show as they come, a library down holds up
 * none, and a library picked loads its further pages.
 */
@OptIn(ExperimentalTestApi::class)
class FindBooksFlowTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-find", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages))

    /** A library of [pages] pages of a book each that answers once [gate] opens, or fails with [error]; it counts the searches it is asked. */
    private class FakeLibrary(override val source: BookSource, private val title: String, private val gate: CompletableDeferred<Unit>? = null, private val error: Exception? = null, private val pages: Int = 1) : BookCatalog {
        var searches = 0
        override val sampleIsWholeText = true
        override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
            searches++
            gate?.await()
            error?.let { throw it }
            val book = FoundBook(
                source = source, id = "${source.tag}-$page", title = if (page == 1) title else "$title $page", authors = listOf("Ana Lima"), description = "", subjects = emptyList(),
                license = "CC-BY-4.0", pageUrl = "https://${source.host}/$page", downloadUrl = "https://${source.host}/$page.txt",
            )
            return CatalogPage(listOf(book), hasMore = page < pages)
        }
        override suspend fun sample(book: FoundBook) = "O gato dorme na casa."
        override suspend fun text(book: FoundBook) = sample(book)
    }

    private val slow = CompletableDeferred<Unit>()
    private val storybooks = FakeLibrary(BookSource.STORYBOOKS, "O gato e o rato", pages = 3)
    private val wikisource = FakeLibrary(BookSource.WIKISOURCE, "Canção do exílio", gate = slow)
    private val gutenberg = FakeLibrary(BookSource.GUTENBERG, "Dom Casmurro", error = LibraryDownException("Project Gutenberg is down for maintenance. Try again later, or pick another library."))
    private val discovery = BookDiscoveryService(listOf(gutenberg, wikisource, storybooks), reading, BookService(books, languages), books)

    private fun ComposeUiTest.page(): FindBooksViewModel {
        runBlocking {
            val id = languages.save(Language(name = "Portuguese"))
            settings.update { it.copy(currentLanguageId = id) }
        }
        val viewModel = FindBooksViewModel(discovery, languages, settings)
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L, knownWords = 1540) {}) {
                        FindBooksScreen(onNavigate = {}, onBack = {}, viewModel = viewModel)
                    }
                }
            }
        }
        return viewModel
    }

    private fun ComposeUiTest.waitFor(tag: String) = waitUntil(timeoutMillis = 10_000) { onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun aQuickLibraryShowsBeforeASlowOneAndOneDownSaysSo() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val viewModel = page()
        // The reader knows none of the words yet, so the shelves take books of every level.
        viewModel.setMinKnown(0)
        // Storybooks has answered and its book is judged, while Wikisource is still on its way.
        waitFor("cover-storybooks-1")
        waitUntil(timeoutMillis = 10_000) { viewModel.state.value.libraries[BookSource.GUTENBERG]?.status?.isDown == true }
        onNodeWithText("Free Portuguese books, picked for the 1,540 words you know.").assertExists()
        onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("library-GUTENBERG"))
        onNodeWithTag("library-status-WIKISOURCE", useUnmergedTree = true).assertExists()
        onNode(hasText("Checking…"), useUnmergedTree = true).assertExists()
        onNode(hasText("Down for maintenance"), useUnmergedTree = true).assertExists()

        slow.complete(Unit)
        waitUntil(timeoutMillis = 10_000) { viewModel.state.value.found.containsKey(BookSource.WIKISOURCE) }

        // The library down, picked, takes its break under the cards; asked again, it is asked alone.
        viewModel.toggleLibrary(BookSource.GUTENBERG)
        waitFor("library-break")
        onNodeWithText("Gutenberg is taking a break").assertExists()
        onNodeWithTag("library-retry").performClick()
        waitUntil(timeoutMillis = 10_000) { viewModel.state.value.libraries[BookSource.GUTENBERG]?.status?.isDown == true && gutenberg.searches == 2 }
        // Let go, every answer is kept, without asking the libraries again.
        viewModel.toggleLibrary(BookSource.GUTENBERG)
        waitFor("cover-storybooks-1")
        assertEquals(false, viewModel.back())
        viewModel.retry()
        waitUntil(timeoutMillis = 10_000) { !viewModel.state.value.waiting && viewModel.state.value.found.size == 3 }
        assertEquals(1, storybooks.searches)
        assertEquals(1, wikisource.searches)
        assertEquals(3, gutenberg.searches)
    }

    @Test
    fun aLibraryPickedLoadsItsNextPagesAtTheEnd() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val viewModel = page()
        viewModel.setMinKnown(0)
        waitFor("cover-storybooks-1")
        viewModel.toggleLibrary(BookSource.STORYBOOKS)
        // Its one grid ends in view, so the second page comes once the first is judged; the third waits for the button.
        waitFor("library-books")
        waitFor("cover-storybooks-2")
        onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("load-more"))
        onNodeWithTag("load-more").performClick()
        waitFor("cover-storybooks-3")
        waitUntil(timeoutMillis = 10_000) { viewModel.state.value.books.all { it.coverage != null } }
        assertEquals(3, storybooks.searches)
        assertEquals(false, viewModel.state.value.hasMore)
        assertEquals(0, onAllNodes(hasTestTag("load-more")).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodes(hasTestTag("cover-wikisource-1")).fetchSemanticsNodes().size)
        // Let go, the shelves are every library's first page again.
        viewModel.clearLibrary()
        waitFor("cover-storybooks-1")
        assertEquals(0, onAllNodes(hasTestTag("cover-storybooks-2")).fetchSemanticsNodes().size)
    }
}
