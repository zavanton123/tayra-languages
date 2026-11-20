package com.idiomiko.feature.books

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.domain.model.BookListItem
import com.idiomiko.core.domain.model.BookStats
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.AppTheme
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * The library as a refined table: the Continue card, one toolbar row, the table with its rings, known shares
 * and hover actions, the grid, and the phone's cards. BOOKS_SHOTS names a folder to save the page's pictures in.
 */
@OptIn(ExperimentalTestApi::class)
class BooksTableTest {
    private val now = Clock.System.now()

    private fun book(id: Long, title: String, page: Int, pages: Int, words: Int, opened: Duration?, finished: Boolean, unknownPercent: Int, tags: List<String> = emptyList()) = BookListItem(
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
        sourceUri = null,
    )

    private val books = listOf(
        book(1, "Long text", 3, 5, 1223, 2.minutes, finished = false, unknownPercent = 31),
        book(2, "Um sábado tranquilo", 1, 1, 103, 54.minutes, finished = true, unknownPercent = 26, tags = listOf("travel")),
        book(3, "Demo", 2, 2, 386, 1.hours, finished = true, unknownPercent = 39),
        book(4, "Short Demo", 1, 1, 96, 70.minutes, finished = true, unknownPercent = 24),
        book(5, "A Maldição", 1, 1, 163, 4.days, finished = true, unknownPercent = 61, tags = listOf("news")),
        book(6, "Not opened", 1, 3, 420, null, finished = false, unknownPercent = 80),
    )

    private val opened = mutableListOf<String>()
    private val practised = mutableListOf<String>()

    private fun ComposeUiTest.page(library: List<BookListItem> = books, theme: AppTheme = AppThemes.default, phone: Boolean = false, view: BooksView = BooksView.LIST, archived: Boolean = false) = setContent {
        var state by remember {
            mutableStateOf(BooksUiState(loading = false, archived = archived, books = library, view = view, languages = listOf(Language(id = 1, name = "Portuguese")), currentLanguageId = 1, packAvailable = true))
        }
        val callbacks = BooksCallbacks(
            onOpen = { opened += it.title },
            onPractice = { practised += it.title },
            onBackToBooks = { backToBooks++ },
            onSort = { state = state.copy(sort = it) },
            onProgress = { state = state.copy(progress = it) },
            onTags = { tags, all -> state = state.copy(tags = tags, matchAllTags = all) },
            onView = { state = state.copy(view = it) },
            onToggle = { book -> state = state.copy(selected = if (book.id in state.selected) state.selected - book.id else state.selected + book.id) },
            onSelectAll = { all -> state = state.copy(selected = if (all) state.filteredBooks.map { it.id }.toSet() else emptySet()) },
        )
        IdiomikoTheme(theme) {
            ProvideWindowWidth {
                CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L) {}) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column {
                            AppTopBar(title = if (phone) "Books" else "Idiomiko", onNavigate = {}, section = NavSection.BOOKS)
                            if (phone) PhoneBooksContent(state, callbacks, selecting = false, onDone = {}) else BooksContent(state, callbacks)
                        }
                    }
                }
            }
        }
    }

    private var backToBooks = 0

    @Test
    fun anEmptyArchiveShowsTheWayBackAndHowArchivingWorks() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        page(library = emptyList(), archived = true)
        onNodeWithTag("empty-archive").assertIsDisplayed()
        onNodeWithText("No archived books yet").assertExists()
        onNodeWithText("Books you set aside appear here.").assertExists()
        shot("archive-empty-desktop")
        onNodeWithTag("archive-back").performClick()
        assertEquals(1, backToBooks)
        onNodeWithTag("archive-how").performClick()
        waitForIdle()
        onNodeWithTag("archive-how-dialog").assertExists()
        shot("archive-how")
        onNodeWithText("Got it").performClick()
        waitForIdle()
        assertEquals(0, onAllNodesWithTag("archive-how-dialog").fetchSemanticsNodes().size)
    }

    @Test
    fun anArchiveWithNoMatchesClearsItsFilters() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        // The one book is under way, so none is waiting to be started.
        page(library = books.take(1), archived = true)
        onNodeWithTag("progress-NOT_STARTED").performClick()
        waitForIdle()
        onNodeWithText("No archived books match").assertExists()
        shot("archive-no-match")
        onNodeWithTag("archive-clear").performClick()
        waitForIdle()
        assertEquals(0, onAllNodesWithTag("archive-clear").fetchSemanticsNodes().size)
    }

    @Test
    fun anEmptyArchiveOnAPhoneAndInTheDark() = runDesktopComposeUiTest(width = 400, height = 860) {
        page(library = emptyList(), archived = true, phone = true, theme = AppThemes.night)
        onNodeWithTag("empty-archive").assertIsDisplayed()
        shot("archive-empty-phone-dark")
    }

    private fun ComposeUiTest.shot(name: String) {
        val dir = System.getenv("BOOKS_SHOTS") ?: return
        waitForIdle()
        File(dir).mkdirs()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(dir, "$name.png"))
    }

    private fun ComposeUiTest.count(text: String) = onAllNodesWithText(text).fetchSemanticsNodes().size

    private fun ComposeUiTest.tagged(prefix: String) =
        onAllNodes(SemanticsMatcher("test tag starting $prefix") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true }).fetchSemanticsNodes().size

    @Test
    fun theTableListsEveryBookWithItsProgressAndKnownShare() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        page()
        // The Continue card: where the reader is and the minutes left at sixty words a minute.
        onNodeWithText("CONTINUE READING").assertIsDisplayed()
        onNodeWithText("Page 3 of 5 · ~13 min left").assertIsDisplayed()
        onNodeWithText("Pages read").assertExists()
        onNodeWithText("Words you know").assertExists()
        // One toolbar row: the status tabs with their counts, no separate count or library heading.
        onNodeWithText("All 6").assertIsDisplayed()
        onNodeWithText("Reading 1").assertIsDisplayed()
        onNodeWithText("Finished 4").assertIsDisplayed()
        assertEquals(0, count("Your library"))
        assertEquals(0, count("Read, learn, and build your vocabulary with every text."))
        // The table lists the book on the Continue card too, as the mockup does.
        assertEquals(6, tagged("book-row-"))
        onNodeWithText("1,223 words · ~21 min").assertExists()
        onNodeWithText("Page 3 of 5").assertExists()
        assertEquals(4, count("Finished"))
        // Known shares: green from half the words, orange with a hint below that.
        onNodeWithText("hard for you").assertExists()
        onNodeWithText("too hard now").assertExists()
        onNodeWithText("—").assertExists()
        onNodeWithText("6 books · 2,391 words").assertExists()
        shot("desktop-light")
    }

    @Test
    fun theTableInTheDarkTheme() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        page(theme = AppThemes.night)
        onNodeWithTag("book-table").assertExists()
        shot("desktop-dark")
    }

    @Test
    fun hoveringARowOffersReadingOnAndPractice() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        page()
        assertEquals(0, count("Read again"))
        onNodeWithTag("book-row-2").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithTag("quick-read-2").assertIsDisplayed()
        onNodeWithText("Read again").assertIsDisplayed()
        shot("desktop-hover")
        onNodeWithTag("quick-practice-2").performClick()
        assertEquals(listOf("Um sábado tranquilo"), practised)
        // A book never opened offers only to start it.
        onNodeWithTag("book-row-6").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("Start").assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag("quick-practice-6").fetchSemanticsNodes().size)
        onNodeWithText("Start").performClick()
        assertEquals(listOf("Not opened"), opened)
    }

    @Test
    fun aRowsMenuStartsTickingBooks() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        page()
        onNodeWithTag("book-menu-3").performClick()
        onNodeWithText("Select").performClick()
        waitForIdle()
        onNodeWithText("1 of 6 selected").assertExists()
        // While ticking, a row ticks its book instead of opening it.
        onNodeWithTag("book-row-4").performClick()
        onNodeWithText("2 of 6 selected").assertExists()
        assertTrue(opened.isEmpty())
    }

    @Test
    fun theGridShowsTheSameCoversProgressAndKnownShare() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        page(view = BooksView.GRID)
        assertEquals(6, tagged("book-card-"))
        onNodeWithText("69% known").assertExists()
        shot("desktop-grid-light")
    }

    @Test
    fun theGridInTheDarkTheme() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        page(view = BooksView.GRID, theme = AppThemes.night)
        onNodeWithTag("book-card-1").assertExists()
        shot("desktop-grid-dark")
    }

    @Test
    fun anEmptyLibrary() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        page(library = emptyList())
        onNodeWithTag("empty-library").assertIsDisplayed()
        assertEquals(0, onAllNodesWithTag("continue-card").fetchSemanticsNodes().size)
        shot("desktop-empty")
    }

    @Test
    fun aMiddleSizedWindowListsCardsUnderAWrappedToolbar() = runDesktopComposeUiTest(width = 820, height = 1000) {
        page()
        assertEquals(0, onAllNodesWithTag("book-table").fetchSemanticsNodes().size)
        onNodeWithTag("book-card-1").assertExists()
        onNodeWithText("All 6").assertIsDisplayed()
        shot("medium-light")
    }

    @Test
    fun thePhoneShowsCardsUnderTheSearchAndFilters() = runDesktopComposeUiTest(width = 400, height = 860) {
        page(phone = true)
        onNodeWithTag("continue-card").assertIsDisplayed()
        onNodeWithText("Page 3 of 5 · ~13 min left").assertExists()
        onNodeWithTag("phone-filters").assertIsDisplayed()
        // The book on the card is not listed again below it.
        assertEquals(0, onAllNodesWithTag("book-row-1").fetchSemanticsNodes().size)
        onNodeWithText("74% known").assertExists()
        shot("phone-light")
    }

    @Test
    fun thePhoneInTheDarkTheme() = runDesktopComposeUiTest(width = 400, height = 860) {
        page(phone = true, theme = AppThemes.night)
        onNodeWithTag("continue-card").assertIsDisplayed()
        shot("phone-dark")
    }

    @Test
    fun thePhoneFiltersSheetHoldsStatusTagsAndOrder() = runDesktopComposeUiTest(width = 400, height = 860) {
        page(phone = true)
        onNodeWithTag("phone-filters").performClick()
        waitForIdle()
        onNodeWithTag("filters-sheet").assertIsDisplayed()
        shot("phone-filters")
        onNodeWithTag("status-NOT_STARTED").performClick()
        onNodeWithTag("tag-chip-news").assertExists()
        onNodeWithTag("sort-TITLE").assertExists()
        waitForIdle()
        assertEquals(1, onAllNodesWithTag("book-row-6").fetchSemanticsNodes().size)
    }

    @Test
    fun theMinutesLeftCountTheRestOfTheBook() {
        assertEquals(733, books[0].wordsLeft)
        assertEquals(0, books[1].wordsLeft)
        assertEquals(420, books[5].wordsLeft)
        assertEquals(21, books[0].readingMinutes)
    }

    @Test
    fun theKnownShareTurnsHardBelowHalfAndTooHardBelowAThird() {
        assertEquals(KnownLevel.COMFORTABLE, knownLevel(50))
        assertEquals(KnownLevel.HARD, knownLevel(49))
        assertEquals(KnownLevel.HARD, knownLevel(30))
        assertEquals(KnownLevel.TOO_HARD, knownLevel(29))
    }
}
