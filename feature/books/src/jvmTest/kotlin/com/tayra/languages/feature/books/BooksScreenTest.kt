package com.tayra.languages.feature.books

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.unit.Density
import com.tayra.languages.core.domain.dictionary.PackState
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.domain.model.BookStats
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import kotlin.test.assertTrue
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
class BooksScreenTest {

    private val now = Clock.System.now()

    private fun book(id: Long, title: String, page: Int, pages: Int, words: Int, opened: kotlin.time.Duration?, finished: Boolean, unknownPercent: Int, tags: List<String> = emptyList(), sourceUri: String? = null) = BookListItem(
        id = id,
        title = title,
        languageId = 1,
        languageName = "Portuguese",
        tags = tags,
        currentPage = page,
        pageCount = pages,
        wordCount = words,
        lastOpened = opened?.let { now - it },
        isCompleted = finished,
        isArchived = false,
        stats = BookStats(100, unknownPercent, unknownPercent, mapOf(TermStatus.UNKNOWN to unknownPercent, TermStatus.WELL_KNOWN to 100 - unknownPercent)),
        sourceUri = sourceUri,
    )

    private val books = listOf(
        book(1, "Long text", 3, 5, 1223, 2.minutes, finished = false, unknownPercent = 31, tags = listOf("news", "travel")),
        book(2, "Um sábado tranquilo", 1, 1, 103, 54.minutes, finished = true, unknownPercent = 26, tags = listOf("travel"), sourceUri = "https://www.example.com/books/sabado"),
        book(3, "Demo", 2, 2, 386, 1.hours, finished = true, unknownPercent = 39),
        book(4, "Short Demo", 1, 1, 96, 70.minutes, finished = true, unknownPercent = 24),
        book(5, "A Maldição", 1, 1, 163, 4.days, finished = true, unknownPercent = 61, tags = listOf("news")),
        book(6, "Not opened", 1, 3, 420, null, finished = false, unknownPercent = 80),
    )

    /** The books a bulk action was asked for, by action. */
    private val bulk = mutableMapOf<String, List<String>>()

    private fun ComposeUiTest.show(library: List<BookListItem> = books) = setContent {
        var state by remember { mutableStateOf(BooksUiState(loading = false, books = library, wordsLearned = 349)) }
        TayraTheme {
            ProvideWindowWidth {
                CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L) {}) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Column {
                            AppTopBar(title = "Tayra Languages", onNavigate = {}, section = NavSection.BOOKS)
                            BooksContent(
                                state,
                                BooksCallbacks(
                                    onSort = { state = state.copy(sort = it) },
                                    onProgress = { state = state.copy(progress = it) },
                                    onTags = { tags, all -> state = state.copy(tags = tags, matchAllTags = all) },
                                    onView = { state = state.copy(view = it) },
                                    onToggle = { book -> state = state.copy(selected = if (book.id in state.selected) state.selected - book.id else state.selected + book.id) },
                                    onSelectAll = { all -> state = state.copy(selected = if (all) state.filteredBooks.map { it.id }.toSet() else emptySet()) },
                                    onArchiveSelected = { bulk["archive"] = state.selectedBooks.map { it.title } },
                                    onDeleteSelected = { bulk["delete"] = state.selectedBooks.map { it.title } },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun theTableShowsReadingProgressStatusAndVocabulary() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        waitForIdle()
        System.getenv("BOOKS_SCREENSHOT")?.let { save(it) }

        // Only the card of the book being read gives its page; the list does not.
        onNodeWithText("Page 3 of 5 · 1,223 words").assertExists()
        assertEquals(1, onAllNodesWithText("Page ", substring = true).fetchSemanticsNodes().size)
        onNodeWithText("Continue reading").assertExists()
        // The book being read is on its card only: the list below leaves it out, so it is not offered twice.
        assertEquals(1, onAllNodesWithText("Long text").fetchSemanticsNodes().size)
        assertTrue(onAllNodesWithText("40%").fetchSemanticsNodes().isNotEmpty())
        // The tab; the one book in progress is the card's, so no badge of it shows in the list.
        assertEquals(1, onAllNodesWithText("In progress").fetchSemanticsNodes().size)
        assertEquals(5, onAllNodesWithText("Finished").fetchSemanticsNodes().size)
        onNode(hasScrollToIndexAction()).performScrollToNode(hasText("6 books  ·  349 words learned"))
        onNodeWithText("6 books  ·  349 words learned").assertExists()
        onNodeWithText("Not opened yet").assertExists()
    }

    /** When the one match is the book in the continue card, the list says there is nothing else, not that the filters hide it. */
    @Test
    fun aFilterMatchingOnlyTheContinuedBookOffersOtherBooks() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("progress-READING").performClick()
        waitForIdle()
        onNodeWithText("Continue reading").assertExists()
        onNodeWithText("No other books yet").assertExists()
        // The header's Find books and the empty list's own.
        assertEquals(2, onAllNodesWithText("Find books").fetchSemanticsNodes().size)
        onNodeWithText("No books match these filters.").assertDoesNotExist()
    }

    /** A phone library holding only the book being read shows its card and no claim that filters hide the rest. */
    @Test
    fun aPhoneLibraryOfOnlyTheContinuedBookOffersOtherBooks() = runDesktopComposeUiTest(width = 420, height = 900) {
        show(books.take(1))
        waitForIdle()
        onNodeWithText("Continue reading").assertExists()
        onNode(hasScrollToIndexAction()).performScrollToNode(hasText("No other books yet"))
        onNodeWithText("No other books yet").assertExists()
        onNodeWithText("No books match these filters.").assertDoesNotExist()
    }

    @Test
    fun theProgressTabsKeepOnlyMatchingBooks() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithTag("progress-NOT_STARTED").performClick()
        waitForIdle()
        onNodeWithText("1 book").assertExists()
        onNodeWithText("Not opened").assertExists()
        // Only in the card of the book being read, not in the list.
        assertEquals(1, onAllNodesWithText("Long text").fetchSemanticsNodes().size)
    }

    @Test
    fun selectingBooksOffersTheBulkActions() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        assertEquals(0, onAllNodesWithContentDescription("Select Long text").fetchSemanticsNodes().size, "no checkboxes until Select")
        onNodeWithTag("select-books").performClick()
        onNodeWithContentDescription("Select Long text").performClick()
        onNodeWithContentDescription("Select Demo").performClick()
        onNodeWithText("2 of 6 selected").assertExists()
        System.getenv("BOOKS_SELECTED_SCREENSHOT")?.let { save(it) }
        onNodeWithText("Archive").performClick()
        waitForIdle()
        assertEquals(listOf("Long text", "Demo"), bulk["archive"])

        onNodeWithContentDescription("Select all books").performClick()
        onNodeWithText("6 of 6 selected").assertExists()
        onNodeWithTag("done-selecting").performClick()
        assertEquals(0, onAllNodesWithContentDescription("Select Long text").fetchSemanticsNodes().size)
    }

    @Test
    fun cardsCanBeSelectedToo() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithContentDescription("grid view").performClick()
        System.getenv("BOOKS_GRID_SCREENSHOT")?.let { save(it) }
        onNodeWithTag("select-books").performClick()
        onNodeWithContentDescription("Select Short Demo").performClick()
        onNodeWithText("1 of 6 selected").assertExists()
        onNodeWithText("Delete").performClick()
        waitForIdle()
        assertEquals(listOf("Short Demo"), bulk["delete"])
        onNodeWithTag("done-selecting").performClick()
        assertEquals(0, onAllNodesWithText("1 of 6 selected").fetchSemanticsNodes().size)
    }

    @Test
    fun progressCountsThePagesFinishedAndIsFullOnlyOnceFinished() {
        assertEquals(40, books[0].progressPercent)
        assertEquals(100, books[1].progressPercent)
        assertEquals(0, books[5].progressPercent)
        assertEquals(75, book(7, "Last page", 4, 4, 10, 1.minutes, finished = false, unknownPercent = 0).progressPercent)
        assertEquals(0, book(8, "One page", 1, 1, 10, 1.minutes, finished = false, unknownPercent = 0).progressPercent)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }

    /** A phone shows the book being read as a card, the library as cards, and sorts and filters from sheets. */
    @Test
    fun aPhoneListsBooksAsCardsAndFiltersFromSheets() = runDesktopComposeUiTest(width = 400, height = 1400) {
        var selecting by mutableStateOf(false)
        setContent {
            var state by remember { mutableStateOf(BooksUiState(loading = false, books = books, wordsLearned = 349)) }
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L) {}) {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            Column {
                                AppTopBar(title = "Books", onNavigate = {}, section = NavSection.BOOKS, actions = { PhoneBooksMenu(archived = false, selecting, onSelecting = { selecting = it }, onNewBook = {}, onArchive = {}) })
                                PhoneBooksContent(
                                    state,
                                    BooksCallbacks(
                                        onSort = { state = state.copy(sort = it) },
                                        onProgress = { state = state.copy(progress = it) },
                                        onTags = { tags, all -> state = state.copy(tags = tags, matchAllTags = all) },
                                        onToggle = { book -> state = state.copy(selected = if (book.id in state.selected) state.selected - book.id else state.selected + book.id) },
                                        onSelectAll = { all -> state = state.copy(selected = if (all) state.filteredBooks.map { it.id }.toSet() else emptySet()) },
                                        onArchiveSelected = { bulk["archive"] = state.selectedBooks.map { it.title } },
                                    ),
                                    selecting = selecting,
                                    onDone = { selecting = false; state = state.copy(selected = emptySet()) },
                                )
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithText("Page 3 of 5 · 1,223 words").assertExists()
        onNodeWithText("40% read").assertExists()
        onNodeWithText("69% known").assertExists()
        onNodeWithText("Your library").assertExists()
        onNodeWithText("Read, learn, and build your vocabulary with every text.").assertDoesNotExist()
        assertEquals(1, onAllNodesWithText("Long text").fetchSemanticsNodes().size)
        onNodeWithTag("book-row-1").assertDoesNotExist()
        onNodeWithText("example.com", substring = true).assertExists()
        onNodeWithText("100% read · 74% known").assertExists()
        System.getenv("BOOKS_SCREENSHOT")?.let { save(it.replace(".png", "-phone.png")) }

        // Progress joins the other controls in a sheet rather than taking a separate tab row.
        onNodeWithTag("phone-progress").performClick()
        onNodeWithText("Not started").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("book-row-6").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Not opened yet", substring = true).assertExists()
        onNodeWithTag("phone-progress").performClick()
        onNodeWithText("All progress").performClick()

        // Sorting from the sheet.
        onNodeWithTag("phone-sort").performClick()
        onNodeWithText("Title").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Title").fetchSemanticsNodes().size == 1 }

        // The tags from the sheet, applied at once.
        onNodeWithTag("phone-filters").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("tag-news").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("tag-news").performClick()
        onNodeWithTag("apply-tags").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Demo").fetchSemanticsNodes().isEmpty() }
        onNodeWithText("A Maldição").assertExists()

        // Ticking books starts from the bar's menu.
        onNodeWithContentDescription("List actions").performClick()
        onNodeWithText("Select books").performClick()
        onNodeWithText("Continue reading").assertDoesNotExist()
        onNodeWithTag("book-row-1").assertExists()
        onNodeWithContentDescription("Select A Maldição").performClick()
        onNodeWithText("1 of 2 selected").assertExists()
        onNodeWithText("Archive").performClick()
        assertEquals(listOf("A Maldição"), bulk["archive"])
        onNodeWithTag("done-selecting").performClick()
        assertEquals(0, onAllNodesWithContentDescription("Select A Maldição").fetchSemanticsNodes().size)
    }

    @Test
    fun phoneBookControlsFitAtLargeText() = runDesktopComposeUiTest(width = 400, height = 860) {
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, 1.3f),
                        LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L) {},
                    ) {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            PhoneBooksContent(BooksUiState(loading = false, books = books), BooksCallbacks(), selecting = false, onDone = {})
                        }
                    }
                }
            }
        }
        waitForIdle()
        val rootWidth = onAllNodes(isRoot())[0].fetchSemanticsNode().size.width
        listOf("phone-progress", "phone-filters", "phone-sort").forEach { tag ->
            val node = onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode()
            assertTrue(node.positionInRoot.x >= 0f && node.positionInRoot.x + node.size.width <= rootWidth, "$tag stays inside the viewport at 130% text")
        }
    }

    /** An empty library offers the language's free book pack, and the offer waits while it downloads. */
    @Test
    fun anEmptyLibraryOffersTheBookPack() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        var state by mutableStateOf(BooksUiState(loading = false, books = emptyList(), languages = emptyList(), currentLanguageId = 1, packAvailable = true))
        var asked = 0
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L) {}) {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            Column {
                                AppTopBar(title = "Tayra Languages", onNavigate = {}, section = NavSection.BOOKS)
                                BooksContent(state.copy(languages = listOf(com.tayra.languages.core.domain.model.Language(id = 1, name = "Portuguese"))), BooksCallbacks(onDownloadBooks = { asked++ }))
                            }
                        }
                    }
                }
            }
        }
        onNodeWithText("Your library is empty").assertExists()
        onNodeWithTag("download-books").performClick()
        assertEquals(1, asked)
        state = state.copy(packState = PackState.Downloading(0.3f))
        waitForIdle()
        onNodeWithText("Downloading…").assertExists()
        onNodeWithTag("download-books").assertIsNotEnabled()
    }

    @Test
    fun theBooksCanBeFilteredByAnyOrAllOfSeveralTags() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        waitForIdle()
        onNodeWithTag("tag-filter").performClick()
        onNodeWithTag("tag-news").performClick()
        onNodeWithTag("tag-travel").performClick()
        onNodeWithTag("apply-tags").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("3 books").fetchSemanticsNodes().isNotEmpty() }
        for (title in listOf("Long text", "Um sábado tranquilo", "A Maldição")) assertTrue(onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty(), title)
        assertEquals(0, onAllNodesWithText("Demo").fetchSemanticsNodes().size)

        onNodeWithTag("tag-filter").performClick()
        onNodeWithTag("match-all").performClick()
        onNodeWithTag("apply-tags").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("1 book").fetchSemanticsNodes().isNotEmpty() }
        // In the card of the book being read only, not again in the list.
        assertEquals(1, onAllNodesWithText("Long text").fetchSemanticsNodes().size)
    }

}
