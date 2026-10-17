package com.tayra.languages.feature.books

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
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.Difficulty
import com.tayra.languages.core.domain.books.discover.FoundBook
import com.tayra.languages.core.domain.books.discover.TextCoverage
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** The free books of online libraries: each with how hard it is for the reader, and the way into the library. */
@OptIn(ExperimentalTestApi::class)
class FindBooksScreenTest {
    private fun book(id: String, title: String, author: String, source: BookSource = BookSource.GUTENBERG, level: String? = null, levelDifficulty: Difficulty? = null) = FoundBook(
        source = source, id = id, title = title, authors = listOf(author), description = "", subjects = emptyList(), level = level, levelDifficulty = levelDifficulty,
        license = "Public domain", pageUrl = "https://www.gutenberg.org/ebooks/$id", downloadUrl = "https://www.gutenberg.org/cache/epub/$id/pg$id.txt",
    )

    private val classics = listOf(
        FoundBookItem(book("55752", "Dom Casmurro", "Machado de Assis"), CoverageCheck.Checked(TextCoverage(1000, 962))),
        FoundBookItem(book("3333", "Os Lusíadas", "Luís de Camões"), CoverageCheck.Checked(TextCoverage(1000, 781)), libraryId = 9),
        FoundBookItem(book("54829", "Memorias Posthumas de Braz Cubas", "Machado de Assis"), CoverageCheck.Checked(TextCoverage(1000, 893)), adding = true),
        FoundBookItem(book("24919", "Amor Crioulo", "Abel Botelho"), CoverageCheck.Checking),
        FoundBookItem(book("62383", "A Biblia Sagrada", "João Ferreira de Almeida"), error = "Could not download the book"),
    )

    private fun ComposeUiTest.show(initial: FindBooksUiState, actions: (update: ((FindBooksUiState) -> FindBooksUiState) -> Unit) -> FindBooksActions) = setContent {
        var state by remember { mutableStateOf(initial) }
        TayraTheme {
            ProvideWindowWidth {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column {
                        AppTopBar(title = "Find books", onNavigate = {}, onBack = {}, section = NavSection.BOOKS)
                        FindBooksContent(state, actions { state = it(state) })
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.screenshot(variable: String) = System.getenv(variable)?.let { path ->
        waitForIdle()
        ImageIO.write(onNode(isRoot()).captureToImage().toAwtImage(), "png", File(path))
    }

    @Test
    fun eachBookShowsHowMuchOfItTheReaderKnows() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        val added = mutableListOf<String>()
        val read = mutableListOf<Long>()
        show(FindBooksUiState(languageName = "Portuguese", books = classics, loading = false, hasMore = true)) { update ->
            FindBooksActions(
                onDifficulty = { d -> update { it.copy(difficulty = d) } },
                onAdd = { added += it.book.id },
                onRead = { item -> item.libraryId?.let { read += it } },
            )
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Dom Casmurro")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("FIND_BOOKS_SCREENSHOT")
        onNodeWithText("Free books in Portuguese from online libraries, with how much of each you already know.").assertExists()
        onNodeWithTag("coverage-55752").assertExists()
        onNodeWithText("You know 96% of the words").assertExists()
        onNodeWithText("Checking the words…").assertExists()
        onNodeWithText("Could not download the book").assertExists()
        onNodeWithTag("add-54829").assertIsNotEnabled()

        onNodeWithTag("add-55752").performClick()
        onNodeWithTag("read-3333").performClick()
        assertEquals(listOf("55752"), added)
        assertEquals(listOf(9L), read)

        // Hard keeps the book the reader knows least of; the unchecked ones wait for a level.
        onNodeWithTag("difficulty-HARD").performClick()
        waitForIdle()
        onNodeWithTag("found-3333").assertExists()
        for (id in listOf("55752", "54829", "24919", "62383")) assertEquals(0, onAllNodesWithTag("found-$id").fetchSemanticsNodes().size)
        onNodeWithTag("difficulty-MEDIUM").performClick()
        onNodeWithText("Memorias Posthumas de Braz Cubas").assertExists()
    }

    @Test
    fun easyReadersShowTheirLevelUntilTheirWordsAreChecked() = runDesktopComposeUiTest(width = 400, height = 1000) {
        val checked = mutableListOf<String>()
        var source: BookSource? = null
        val readers = listOf(
            FoundBookItem(book("15316", "Volte aqui, gata!", "BookDash", BookSource.GDL, "Level 2", Difficulty.EASY)),
            FoundBookItem(book("15400", "Uma casa para o Senhor Rato", "StoryWeaver", BookSource.GDL, "Level 4", Difficulty.HARD), CoverageCheck.Checked(TextCoverage(200, 196))),
        )
        show(FindBooksUiState(languageName = "Portuguese", source = BookSource.GDL, books = readers, loading = false)) {
            FindBooksActions(onCheck = { checked += it.book.id }, onSource = { source = it })
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Volte aqui, gata!")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("FIND_BOOKS_PHONE_SCREENSHOT")
        onNodeWithText("BookDash · Level 2").assertExists()
        // The level the library gives until the words are checked; then what the reader knows.
        onNodeWithTag("check-15316").performClick()
        assertEquals(listOf("15316"), checked)
        onNodeWithText("You know 98% of the words").assertExists()
        onNodeWithTag("source-GUTENBERG").performClick()
        assertEquals(BookSource.GUTENBERG, source)
    }

    @Test
    fun wikisourceBooksAreJudgedOnSightAndCreditedToTheWiki() = runDesktopComposeUiTest(width = 400, height = 1000) {
        val work = FoundBook(
            source = BookSource.WIKISOURCE, id = "Dom Casmurro", title = "Dom Casmurro", authors = listOf("Machado de Assis"), description = "", subjects = emptyList(),
            license = "CC BY-SA", pageUrl = "https://pt.wikisource.org/wiki/Dom_Casmurro", downloadUrl = "https://ws-export.wmcloud.org/?format=epub-3&lang=pt&page=Dom_Casmurro",
        )
        show(FindBooksUiState(languageName = "Portuguese", source = BookSource.WIKISOURCE, books = listOf(FoundBookItem(work)), loading = false)) { FindBooksActions() }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Dom Casmurro")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("FIND_BOOKS_WIKISOURCE_SCREENSHOT")
        // Its words are checked as it is listed, so there is nothing to ask for.
        assertEquals(0, onAllNodesWithTag("check-Dom Casmurro").fetchSemanticsNodes().size)
        for (source in BookSource.entries) onNodeWithTag("source-${source.name}").assertExists()
        assertEquals("Source: Wikisource, «Dom Casmurro» (https://pt.wikisource.org/wiki/Dom_Casmurro), CC BY-SA.", attribution(work))
    }

    @Test
    fun aLibraryWithoutTheLanguageOffersTheOther() = runDesktopComposeUiTest(width = 1200, height = 800) {
        var source: BookSource? = null
        show(FindBooksUiState(languageName = "Greek", source = BookSource.GDL, loading = false)) { FindBooksActions(onSource = { source = it }) }
        onNodeWithText("Global Digital Library has no books in Greek.").assertExists()
        onNodeWithText("Look in Project Gutenberg").performClick()
        assertEquals(BookSource.GUTENBERG, source)
    }
}
