package com.idiomiko.feature.books

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.domain.books.discover.BookSource
import com.idiomiko.core.domain.books.discover.Difficulty
import com.idiomiko.core.domain.books.discover.FoundBook
import com.idiomiko.core.domain.books.discover.LibraryStatus
import com.idiomiko.core.domain.books.discover.Shelf
import com.idiomiko.core.domain.books.discover.TextCoverage
import com.idiomiko.core.domain.books.discover.Topic
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.CoverageCheck
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.AppTheme
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Free books from every library at once: shelves picked by the words the reader knows, a search across them all, and
 * the libraries with whether each can be reached, a card picked narrowing the books to its own. FIND_SHOTS names a
 * folder for the pictures.
 */
@OptIn(ExperimentalTestApi::class)
class FindBooksScreenTest {
    private fun book(id: String, title: String, author: String, source: BookSource = BookSource.GUTENBERG, level: String? = null, levelDifficulty: Difficulty? = null, subjects: List<String> = emptyList()) = FoundBook(
        source = source, id = id, title = title, authors = listOf(author), description = "", subjects = subjects, level = level, levelDifficulty = levelDifficulty,
        license = "Public domain", pageUrl = "https://www.gutenberg.org/ebooks/$id", downloadUrl = "https://www.gutenberg.org/cache/epub/$id/pg$id.txt",
    )

    private fun judged(id: String, title: String, author: String, source: BookSource, percent: Int, words: Int? = null) =
        FoundBookItem(book(id, title, author, source), CoverageCheck.Checked(TextCoverage(1000, percent * 10, words)))

    private val sources = listOf(BookSource.GUTENBERG, BookSource.WIKISOURCE, BookSource.GDL, BookSource.STORYBOOKS, BookSource.STORYWEAVER, BookSource.ADAMASTOR)

    /** The books of the approved design: short ones the reader nearly knows, and classics at 68–86%. */
    private val found = mapOf(
        BookSource.STORYBOOKS to listOf(
            judged("menina", "A menina e a lua", "Global Storybooks", BookSource.STORYBOOKS, 96, 420),
            judged("sapo", "O sapo e a chuva", "Global Storybooks", BookSource.STORYBOOKS, 90, 350),
        ),
        BookSource.STORYWEAVER to listOf(judged("gato", "O gato e o rato", "Pratham Books", BookSource.STORYWEAVER, 94, 230)),
        BookSource.GDL to listOf(
            judged("feira", "Um dia na feira", "African Storybook", BookSource.GDL, 93, 880),
            judged("festa", "A festa junina", "African Storybook", BookSource.GDL, 90, 520),
        ),
        BookSource.ADAMASTOR to listOf(
            judged("exilio", "Canção do exílio", "Gonçalves Dias", BookSource.ADAMASTOR, 91, 180),
            judged("alienista", "O Alienista", "Machado de Assis", BookSource.ADAMASTOR, 86, 18_000),
            judged("iracema", "Iracema", "José de Alencar", BookSource.ADAMASTOR, 80, 32_000),
        ),
        BookSource.WIKISOURCE to listOf(
            judged("fluminenses", "Contos Fluminenses", "Machado de Assis", BookSource.WIKISOURCE, 84),
            judged("moreninha", "A Moreninha", "J. M. de Macedo", BookSource.WIKISOURCE, 82),
            judged("cortico", "O Cortiço", "Aluísio Azevedo", BookSource.WIKISOURCE, 77),
            judged("casmurro", "Dom Casmurro", "Machado de Assis", BookSource.WIKISOURCE, 68),
            FoundBookItem(book("lusiadas", "Os Lusíadas", "Luís de Camões", BookSource.WIKISOURCE), CoverageCheck.Checking),
        ),
    )

    private val maintenance = "Project Gutenberg is down for maintenance. Try again later, or pick another library."
    private val checked = Clock.System.now() - 4.minutes

    private val libraries = sources.associateWith { LibraryInfo(LibraryStatus.AVAILABLE, checked) } +
        (BookSource.GUTENBERG to LibraryInfo(LibraryStatus.DOWN_FOR_MAINTENANCE, checked, maintenance))

    private val discover = FindBooksUiState(languageName = "Portuguese", sources = sources, found = found, libraries = libraries)

    private fun ComposeUiTest.show(initial: FindBooksUiState, theme: AppTheme = AppThemes.default, knownWords: Int? = 1540, actions: (update: ((FindBooksUiState) -> FindBooksUiState) -> Unit) -> FindBooksActions) = setContent {
        var state by remember { mutableStateOf(initial) }
        IdiomikoTheme(theme) {
            ProvideWindowWidth {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column {
                        AppTopBar(title = "Find books", onNavigate = {}, onBack = {}, section = NavSection.BOOKS)
                        FindBooksContent(state, actions { state = it(state) }, knownWords = knownWords)
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.shot(name: String) {
        val dir = System.getenv("FIND_SHOTS") ?: return
        waitForIdle()
        File(dir).mkdirs()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(dir, "$name.png"))
    }

    private fun ComposeUiTest.count(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().size

    @Test
    fun theShelvesPickBooksForTheWordsTheReaderKnows() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val shown = mutableListOf<Shelf>()
        val opened = mutableListOf<BookSource>()
        val added = mutableListOf<String>()
        show(discover) { update ->
            FindBooksActions(
                onMinKnown = { v -> update { it.copy(minKnown = v) } },
                onShowAll = { shown += it },
                onLibrary = { opened += it },
                onAdd = { added += it.book.id },
            )
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("A menina e a lua")).fetchSemanticsNodes().isNotEmpty() }
        shot("discover-desktop-light")
        onNodeWithText("Find something to read").assertExists()
        onNodeWithText("Free Portuguese books, picked for the 1,540 words you know.").assertExists()
        onNodeWithTag("hero-mascot").assertExists()
        onNodeWithText("90%+ known").assertExists()
        // Short and nearly known; then the classics in reach, best known first, and one harder past them.
        onNodeWithText("Short, and you know almost every word").assertExists()
        onNodeWithText("Famous Portuguese books at 75–89% known").assertExists()
        onNodeWithText("Storybooks · ~7 min").assertExists()
        onNodeWithText("Machado de Assis · hard").assertExists()
        onNodeWithTag("known-menina", useUnmergedTree = true).assertExists()
        onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("library-GUTENBERG"))
        shot("discover-desktop-light-libraries")
        onNodeWithText("Down for maintenance").assertExists()
        assertEquals(6, sources.count { count("library-${it.name}") == 1 })
        // No topic without a library to back it: audio is in none of them.
        for (topic in Topic.entries) assertEquals(1, count("topic-${topic.name}"))

        onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("add-menina"))
        onNodeWithTag("add-menina").performClick()
        assertEquals(listOf("menina"), added)
        onNodeWithTag("see-all-CLASSICS_WITHIN_REACH").performClick()
        assertEquals(listOf(Shelf.CLASSICS_WITHIN_REACH), shown)
        onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("library-GUTENBERG"))
        onNodeWithTag("library-GUTENBERG").performClick()
        assertEquals(listOf(BookSource.GUTENBERG), opened)

        // An easier level asked for leaves the 90% books out of the easy wins, and the classics' reach moves up.
        onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("known-stop-95"))
        onNodeWithTag("known-stop-95").performClick()
        waitForIdle()
        onNodeWithText("95%+ known").assertExists()
        onNodeWithText("Famous Portuguese books at 80–94% known").assertExists()
        assertEquals(0, count("cover-sapo"))
        onNodeWithTag("cover-menina").assertExists()
    }

    @Test
    fun aCoverOpensTheBookCardWithItsLicenceAndAdd() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val added = mutableListOf<String>()
        show(discover) { FindBooksActions(onAdd = { added += it.book.id }) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("cover-alienista")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("cover-alienista").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("found-book-dialog")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("From Projecto Adamastor, public domain.").assertExists()
        onNodeWithText("You know 86% of the words").assertExists()
        onNodeWithText("~300 min").assertExists()
        onNodeWithTag("dialog-add").performClick()
        assertEquals(listOf("alienista"), added)
    }

    @Test
    fun theDarkThemeKeepsTheNavyHeroAndWhiteBadges() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        show(discover, theme = AppThemes.night) { FindBooksActions() }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("A menina e a lua")).fetchSemanticsNodes().isNotEmpty() }
        shot("discover-desktop-dark")
        onNodeWithText("Classics within reach").assertExists()
    }

    @Test
    fun theFirstLoadShowsShelvesStillToCome() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        show(FindBooksUiState(languageName = "Portuguese", sources = sources), knownWords = null) { FindBooksActions() }
        waitForIdle()
        shot("discover-loading")
        onNodeWithText("Free Portuguese books, picked for the words you know.").assertExists()
        assertEquals(2, count("shelf-skeleton"))
        assertEquals(sources.size, onAllNodes(hasText("Checking…")).fetchSemanticsNodes().size)
    }

    @Test
    fun everyLibraryDownLeavesTheWayToTryAgain() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        var retried = 0
        val down = discover.copy(found = emptyMap(), libraries = sources.associateWith { LibraryInfo(LibraryStatus.UNREACHABLE, checked) })
        show(down) { FindBooksActions(onRetry = { retried++ }) }
        waitForIdle()
        shot("discover-offline")
        onNodeWithText("The libraries can't be reached right now").assertExists()
        onNodeWithText("Checked 4 minutes ago").assertExists()
        onNodeWithTag("retry-all").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun aSearchWithNoResultsOffersToClearItOrLetInHarderBooks() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        var cleared = 0
        val levels = mutableListOf<Int>()
        val searched = discover.copy(
            query = "baleia", searched = "baleia",
            found = mapOf(BookSource.WIKISOURCE to listOf(judged("baleia", "A baleia", "Ana Lima", BookSource.WIKISOURCE, 62))),
        )
        show(searched) { FindBooksActions(onClearSearch = { cleared++ }, onMinKnown = { levels += it }) }
        waitForIdle()
        shot("search-no-results")
        onNodeWithText("Results for “baleia”").assertExists()
        onNodeWithText("No books match your search.").assertExists()
        onNodeWithText("0 books · 1 harder book left out").assertExists()
        onNodeWithTag("no-match-clear").performClick()
        onNodeWithTag("no-match-levels").performClick()
        assertEquals(1, cleared)
        assertEquals(listOf(0), levels)
    }

    @Test
    fun aSearchFromTheHeroAsksEveryLibrary() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        var searches = 0
        val topics = mutableListOf<Topic>()
        show(discover) { update ->
            FindBooksActions(
                onQuery = { q -> update { it.copy(query = q) } },
                onSearch = { searches++; update { it.copy(searched = it.query) } },
                onTopic = { t -> topics += t; update { it.copy(topic = t) } },
            )
        }
        onNodeWithTag("hero-search").performTextInput("Machado")
        onNodeWithTag("hero-search-button").performClick()
        assertEquals(1, searches)
        waitForIdle()
        onNodeWithText("Results for “Machado”").assertExists()
        onNodeWithTag("topic-CHILDRENS_BOOKS").performClick()
        assertEquals(listOf(Topic.CHILDRENS_BOOKS), topics)
    }

    @Test
    fun aPhoneCondensesTheHeroAndScrollsTheShelves() = runDesktopComposeUiTest(width = 400, height = 860) {
        show(discover) { FindBooksActions() }
        // The libraries come right under the How hard? scale, two to a row.
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("library-WIKISOURCE")).fetchSemanticsNodes().isNotEmpty() }
        shot("discover-phone-light")
        assertEquals(0, count("hero-mascot"))
        onNodeWithText("Search every library").assertExists()
        val top = { tag: String -> onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(top("library-WIKISOURCE"), top("library-GDL"))
        assert(top("library-STORYBOOKS") > top("library-GDL"))
        // The shelves follow them.
        onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(hasText("A menina e a lua"))
        shot("discover-phone-light-shelves")
    }

    @Test
    fun aLanguageNoLibraryServesSaysSo() = runDesktopComposeUiTest(width = 1200, height = 800) {
        show(FindBooksUiState(languageName = "Klingon")) { FindBooksActions() }
        onNodeWithText("None of the libraries has books in Klingon yet.").assertExists()
    }

    @Test
    fun aPhoneInTheDarkTheme() = runDesktopComposeUiTest(width = 400, height = 860) {
        show(discover, theme = AppThemes.night) { FindBooksActions() }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("library-WIKISOURCE")).fetchSemanticsNodes().isNotEmpty() }
        shot("discover-phone-dark")
    }

    // A library's card picked: the books narrowed to its own.

    /** The page's actions with a library's card toggling as the view model does. */
    private fun picking(update: ((FindBooksUiState) -> FindBooksUiState) -> Unit, picked: MutableList<BookSource> = mutableListOf()) = FindBooksActions(
        onLibrary = { source -> picked += source; update { it.copy(selected = source.takeIf { s -> s != it.selected }, more = MorePages()) } },
        onClearLibrary = { update { it.copy(selected = null, more = MorePages()) } },
        onTopic = { t -> update { it.copy(topic = if (it.topic == t) null else t) } },
        onMinKnown = { v -> update { it.copy(minKnown = v) } },
    )

    private fun ComposeUiTest.scrollTo(tag: String) = onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(hasTestTag(tag))

    @Test
    fun aLibraryCardPickedShowsOnlyItsBooksUntilLetGo() = runDesktopComposeUiTest(width = 1600, height = 1500) {
        show(discover) { picking(it) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("A menina e a lua")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, count("library-filter"))
        onNodeWithTag("library-WIKISOURCE").performClick()
        waitForIdle()
        shot("desktop-light-wikisource")
        onNodeWithTag("library-WIKISOURCE").assertIsSelected()
        onNodeWithTag("library-ADAMASTOR").assertIsNotSelected()
        onNodeWithTag("library-check-WIKISOURCE", useUnmergedTree = true).assertExists()
        onNodeWithTag("library-filter").assertExists()
        // Wikisource's classics, and nothing of the other libraries; no book of it is short, so no easy wins.
        onNodeWithTag("shelf-CLASSICS_WITHIN_REACH").assertExists()
        assertEquals(0, count("shelf-EASY_WINS"))
        for (id in listOf("fluminenses", "moreninha", "cortico", "casmurro")) onNodeWithTag("cover-$id").assertExists()
        for (id in listOf("menina", "gato", "feira", "alienista", "iracema")) assertEquals(0, count("cover-$id"), id)
        scrollTo("library-footer")
        onNodeWithText("Texts transcribed by Wikisource's volunteers", substring = true).assertExists()

        // The How hard? scale and the topics narrow the library's books further.
        scrollTo("known-stop-80")
        onNodeWithTag("known-stop-80").performClick()
        waitForIdle()
        onNodeWithText("Famous Portuguese books at 65–79% known").assertExists()
        onNodeWithTag("topic-CLASSIC_NOVELS").performClick()
        waitForIdle()
        onNodeWithTag("results").assertExists()
        onNodeWithTag("cover-fluminenses").assertExists()
        assertEquals(0, count("cover-alienista"))
        onNodeWithTag("topic-CLASSIC_NOVELS").performClick()

        // The chip lets it go; so does a second tap on its card.
        scrollTo("library-filter")
        onNodeWithTag("library-filter").performClick()
        waitForIdle()
        assertEquals(0, count("library-filter"))
        onNodeWithTag("library-WIKISOURCE").assertIsNotSelected()
        onNodeWithTag("cover-menina").assertExists()
        scrollTo("library-WIKISOURCE")
        onNodeWithTag("library-WIKISOURCE").performClick()
        onNodeWithTag("library-WIKISOURCE").performClick()
        waitForIdle()
        assertEquals(0, onAllNodesWithTag("library-check-WIKISOURCE", useUnmergedTree = true).fetchSemanticsNodes().size)
        scrollTo("cover-menina")
    }

    @Test
    fun aChildrensLibraryPickedIsOneGridBestKnownFirst() = runDesktopComposeUiTest(width = 1600, height = 1400) {
        show(discover) { picking(it) }
        onNodeWithTag("library-GDL").performClick()
        waitForIdle()
        shot("desktop-light-easy-readers")
        onNodeWithTag("library-books").assertExists()
        onNodeWithText("Global Digital Library").assertExists()
        onNodeWithText("2 books · best known first").assertExists()
        assertEquals(0, count("shelf-EASY_WINS"))
        val left = { tag: String -> onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.left }
        assert(left("cover-feira") < left("cover-festa"))
        assertEquals(0, count("cover-menina"))
    }

    @Test
    fun aLibraryDownAndPickedTakesABreakUnderTheCards() = runDesktopComposeUiTest(width = 1600, height = 1400) {
        var retried = 0
        val picked = mutableListOf<BookSource>()
        show(discover) { update -> picking(update, picked).let { FindBooksActions(onLibrary = it.onLibrary, onClearLibrary = it.onClearLibrary, onRetryLibrary = { retried++ }) } }
        onNodeWithTag("library-GUTENBERG").performClick()
        waitForIdle()
        shot("desktop-light-gutenberg-down")
        onNodeWithTag("library-break").assertExists()
        onNodeWithText("Gutenberg is taking a break").assertExists()
        onNodeWithText(maintenance).assertExists()
        onNodeWithText("Checked 4 minutes ago").assertExists()
        assertEquals(0, count("shelf-CLASSICS_WITHIN_REACH"))
        assert(onNodeWithTag("library-break").fetchSemanticsNode().boundsInRoot.top > onNodeWithTag("library-GUTENBERG").fetchSemanticsNode().boundsInRoot.bottom)
        onNodeWithTag("library-retry").performClick()
        assertEquals(1, retried)
        // Another library from the break, or from its card.
        onNodeWithTag("try-ADAMASTOR").performClick()
        waitForIdle()
        onNodeWithTag("library-ADAMASTOR").assertIsSelected()
        assertEquals(0, count("library-break"))
        onNodeWithTag("library-GUTENBERG").performClick()
        onNodeWithTag("library-WIKISOURCE").performClick()
        waitForIdle()
        onNodeWithTag("library-WIKISOURCE").assertIsSelected()
        assertEquals(listOf(BookSource.GUTENBERG, BookSource.ADAMASTOR, BookSource.GUTENBERG, BookSource.WIKISOURCE), picked)
    }

    @Test
    fun theDarkThemeHighlightsTheLibraryPicked() = runDesktopComposeUiTest(width = 1600, height = 1400) {
        show(discover.copy(selected = BookSource.STORYBOOKS), theme = AppThemes.night) { FindBooksActions() }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("A menina e a lua")).fetchSemanticsNodes().isNotEmpty() }
        shot("desktop-dark-storybooks")
        onNodeWithTag("library-STORYBOOKS").assertIsSelected()
        assertEquals(0, count("cover-feira"))
    }

    @Test
    fun seeAllOfALibraryLoadsItsNextPagesAtTheEnd() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val wikisource = found.getValue(BookSource.WIKISOURCE).filter { it.check is CoverageCheck.Checked }
        val nextPage = listOf(
            judged("iaia", "Iaiá Garcia", "Machado de Assis", BookSource.WIKISOURCE, 81),
            judged("guarani", "O Guarani", "José de Alencar", BookSource.WIKISOURCE, 79),
        )
        var loads = 0
        var back = 0
        val state = discover.copy(
            found = found + (BookSource.WIKISOURCE to wikisource), selected = BookSource.WIKISOURCE, view = FindView.AllOf(Shelf.CLASSICS_WITHIN_REACH),
            libraries = libraries + (BookSource.WIKISOURCE to LibraryInfo(LibraryStatus.AVAILABLE, checked, hasMore = true)),
        )
        show(state) { update ->
            FindBooksActions(
                onLoadMore = { loads++; update { it.copy(more = MorePages(books = nextPage.take(loads), page = it.more.page + 1, hasMore = loads < 2)) } },
                onBack = { back++ },
            )
        }
        // The grid is short, so its end is in view at once: the next page comes by itself.
        waitUntil(timeoutMillis = 5_000) { loads == 1 }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("cover-iaia")).fetchSemanticsNodes().isNotEmpty() }
        shot("desktop-light-wikisource-see-all")
        onNodeWithTag("library-filter").assertExists()
        assertEquals(0, count("cover-menina"))
        // While the end stays in view, the page after waits for the button.
        onNodeWithTag("load-more").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("cover-guarani")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, loads)
        assertEquals(0, count("load-more"))
        onAllNodes(hasText("Find books") and hasClickAction()).onFirst().performClick()
        assertEquals(1, back)
    }

    @Test
    fun aPageThatFailsToLoadOffersToTryAgain() = runDesktopComposeUiTest(width = 1600, height = 1400) {
        var loads = 0
        val state = discover.copy(selected = BookSource.ADAMASTOR, more = MorePages(page = 1, error = "Could not reach the library"), libraries = libraries + (BookSource.ADAMASTOR to LibraryInfo(LibraryStatus.AVAILABLE, checked, hasMore = true)))
        show(state) { FindBooksActions(onLoadMore = { loads++ }) }
        scrollTo("library-footer")
        onNodeWithText("Could not reach the library").assertExists()
        onNodeWithTag("load-more-retry").performClick()
        assertEquals(1, loads)
    }

    @Test
    fun aPhoneKeepsTwoCardsToARowAndTheBooksFollowThePick() = runDesktopComposeUiTest(width = 400, height = 860) {
        show(discover) { picking(it) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("library-STORYBOOKS")).fetchSemanticsNodes().isNotEmpty() }
        scrollTo("library-STORYBOOKS")
        onNodeWithTag("library-STORYBOOKS").performClick()
        waitForIdle()
        onNodeWithTag("library-STORYBOOKS").assertIsSelected()
        val top = { tag: String -> onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(top("library-WIKISOURCE"), top("library-GDL"))
        assertEquals(top("library-STORYBOOKS"), top("library-STORYWEAVER"))
        shot("phone-light-storybooks")
        scrollTo("library-books")
        shot("phone-light-storybooks-books")
        onNodeWithTag("cover-menina").assertExists()
        onNodeWithTag("cover-sapo").assertExists()
        assertEquals(0, count("cover-alienista"))
        assertEquals(0, count("cover-feira"))
    }

    @Test
    fun easyReadersShowTheirLevelUntilTheirWordsAreChecked() = runDesktopComposeUiTest(width = 400, height = 1000) {
        val checks = mutableListOf<String>()
        val readers = listOf(
            FoundBookItem(book("15316", "Volte aqui, gata!", "BookDash", BookSource.GDL, "Level 2", Difficulty.EASY)),
            FoundBookItem(book("15400", "Uma casa para o Senhor Rato", "StoryWeaver", BookSource.GDL, "Level 4", Difficulty.HARD), CoverageCheck.Checked(TextCoverage(200, 196))),
        )
        show(discover.copy(selected = BookSource.GDL, found = mapOf(BookSource.GDL to readers))) { FindBooksActions(onCheck = { checks += it.book.id }) }
        scrollTo("cover-15316")
        // The one judged comes first; the other waits for the reader to ask.
        val left = { tag: String -> onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.left }
        assert(left("cover-15400") < left("cover-15316"))
        onNodeWithTag("cover-15316").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("found-book-dialog")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("BookDash · Level 2").assertExists()
        onNodeWithTag("check-15316").performClick()
        assertEquals(listOf("15316"), checks)
    }

    @Test
    fun wikisourceBooksAreJudgedOnSightAndCreditedToTheWiki() = runDesktopComposeUiTest(width = 400, height = 1000) {
        val work = FoundBook(
            source = BookSource.WIKISOURCE, id = "Dom Casmurro", title = "Dom Casmurro", authors = listOf("Machado de Assis"), description = "", subjects = emptyList(),
            license = "CC BY-SA", pageUrl = "https://pt.wikisource.org/wiki/Dom_Casmurro", downloadUrl = "https://ws-export.wmcloud.org/?format=epub-3&lang=pt&page=Dom_Casmurro",
        )
        show(discover.copy(found = mapOf(BookSource.WIKISOURCE to listOf(FoundBookItem(work, CoverageCheck.Checked(TextCoverage(1000, 810))))))) { FindBooksActions() }
        scrollTo("cover-Dom Casmurro")
        onNodeWithTag("cover-Dom Casmurro").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("found-book-dialog")).fetchSemanticsNodes().isNotEmpty() }
        // Its words are checked as it is listed, so there is nothing to ask for.
        assertEquals(0, count("check-Dom Casmurro"))
        onNodeWithText("From Wikisource, CC BY-SA.").assertExists()
        assertEquals("Source: Wikisource, «Dom Casmurro», CC BY-SA.", attribution(work))
        assertEquals("Source: Project Gutenberg, «Dom Casmurro», public domain.", attribution(book("55752", "Dom Casmurro", "Machado de Assis")))
        assertEquals("Source: Global Digital Library, «Gata», CC BY-SA 4.0.", attribution(work.copy(source = BookSource.GDL, title = "Gata", license = "CC-BY-SA-4.0")))
        assertEquals("Source: Global Digital Library, «Gata», CC BY 4.0.", attribution(work.copy(source = BookSource.GDL, title = "Gata", license = "CC-BY-4.0")))
        assertEquals("Source: Ebooks libres et gratuits, «Le Robinson de douze ans», free for non-commercial use.", attribution(work.copy(source = BookSource.ELG, title = "Le Robinson de douze ans", license = "Non-commercial use")))
        assertEquals("Source: Global Storybooks, «Um homem muito alto», CC BY.", attribution(work.copy(source = BookSource.STORYBOOKS, title = "Um homem muito alto", license = "CC-BY")))
    }

    @Test
    fun aLanguageWithALibraryOfItsOwnListsItBesideTheOthers() = runDesktopComposeUiTest(width = 400, height = 1000) {
        val polish = listOf(BookSource.GUTENBERG, BookSource.WIKISOURCE, BookSource.GDL, BookSource.WOLNE_LEKTURY)
        show(FindBooksUiState(languageName = "Polish", sources = polish, libraries = polish.associateWith { LibraryInfo(LibraryStatus.AVAILABLE, checked) })) { FindBooksActions() }
        waitForIdle()
        for (library in polish) onNodeWithTag("library-${library.name}").assertExists()
        onNodeWithText("School readings").assertExists()
        // Poems need a library that names genres; Wolne Lektury does.
        onNodeWithTag("topic-POEMS").assertExists()
    }

    @Test
    fun aLibraryWithoutBooksInTheLanguageSaysSo() = runDesktopComposeUiTest(width = 1200, height = 1200) {
        val greek = listOf(BookSource.GUTENBERG, BookSource.GDL)
        show(FindBooksUiState(languageName = "Greek", sources = greek, selected = BookSource.GDL, found = mapOf(BookSource.GDL to emptyList()), libraries = greek.associateWith { LibraryInfo(LibraryStatus.AVAILABLE, checked) })) { FindBooksActions() }
        scrollTo("library-filter")
        onNodeWithText("Global Digital Library has no books in Greek.").assertExists()
    }
}
