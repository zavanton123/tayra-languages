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
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private fun book(id: Long, title: String, page: Int, pages: Int, words: Int, opened: kotlin.time.Duration?, finished: Boolean, unknownPercent: Int) = BookListItem(
        id = id,
        title = title,
        languageId = 1,
        languageName = "Portuguese",
        tags = emptyList(),
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
        book(2, "Um sábado tranquilo", 1, 1, 103, 54.minutes, finished = true, unknownPercent = 26),
        book(3, "Demo", 2, 2, 386, 1.hours, finished = true, unknownPercent = 39),
        book(4, "Short Demo", 1, 1, 96, 70.minutes, finished = true, unknownPercent = 24),
        book(5, "A Maldição", 1, 1, 163, 4.days, finished = true, unknownPercent = 61),
        book(6, "Not opened", 1, 3, 420, null, finished = false, unknownPercent = 80),
    )

    private fun ComposeUiTest.show() = setContent {
        var state by remember { mutableStateOf(BooksUiState(loading = false, books = books, wordsLearned = 349)) }
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
                                    onView = { state = state.copy(view = it) },
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

        onNodeWithText("Page 3 of 5").assertExists()
        assertEquals(0, onAllNodesWithText("Page 1 of 1").fetchSemanticsNodes().size, "one-page books have no page label")
        onNodeWithText("60%").assertExists()
        onNodeWithText("1,223 words").assertExists()
        onNodeWithText("Reading").assertExists()
        assertEquals(4, onAllNodesWithText("Finished").fetchSemanticsNodes().size)
        onNodeWithText("Currently reading").assertExists()
        onNodeWithText("349").assertExists()
        onNodeWithText("6 books").assertExists()
        onNodeWithText("Not opened yet").assertExists()
    }

    @Test
    fun theProgressFilterKeepsOnlyMatchingBooks() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        show()
        onNodeWithText("All progress").performClick()
        onNodeWithText("Not started").performClick()
        waitForIdle()
        onNodeWithText("1 book").assertExists()
        onNodeWithText("Not opened").assertExists()
        assertEquals(0, onAllNodesWithText("Long text").fetchSemanticsNodes().size)
    }

    @Test
    fun progressIsTheCurrentPagesShareAndFullOnlyOnceFinished() {
        assertEquals(60, books[0].progressPercent)
        assertEquals(100, books[1].progressPercent)
        assertEquals(0, books[5].progressPercent)
        assertEquals(99, book(7, "Last page", 4, 4, 10, 1.minutes, finished = false, unknownPercent = 0).progressPercent)
    }

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}
