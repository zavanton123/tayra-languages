package com.tayra.languages.feature.books

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BookFormScreenTest {

    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-form", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = BookService(books, languages)
    private val text = ('a'..'l').joinToString("\n") { c -> "O lobo$c dorme na floresta." }

    private fun ComposeUiTest.edit(bookId: Long, saved: () -> Unit = {}): BookFormViewModel {
        val viewModel = BookFormViewModel(bookId, books, languages, settings, service)
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L) {}) {
                        BookFormScreen(bookId = bookId, onNavigate = {}, onBack = {}, onSaved = { _, _ -> saved() }, viewModel = viewModel)
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Edit book").fetchSemanticsNodes().isNotEmpty() }
        return viewModel
    }

    private fun book(): Long = runBlocking {
        val languageId = languages.save(Language(name = "Portuguese"))
        service.create(BookDraft(languageId = languageId, title = "Short Demo", text = text, tags = listOf("demo", "short"), wordsPerPage = 5))
    }

    @Test
    fun editingShowsTheWholeTextTitleAndTagsWithoutFileImport() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val id = book()
        edit(id)
        System.getenv("BOOK_EDIT_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }

        onNodeWithText(text).assertExists()
        onNodeWithText("Short Demo").assertExists()
        onNodeWithText("demo").assertExists()
        onNodeWithText("short").assertExists()
        onNodeWithText("5").assertExists()
        assertEquals(0, onAllNodesWithText("Import file").fetchSemanticsNodes().size)
        onNodeWithText("The book has 6 pages. Change the text or these settings to rebuild them.").assertExists()
    }

    @Test
    fun changingThePageSizeRebuildsThePagesOnSave() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val id = book()
        var saved = false
        val viewModel = edit(id) { saved = true }
        viewModel.update { it.copy(wordsPerPage = 15) }
        waitForIdle()
        onNodeWithText("Saving rebuilds the pages. Your place, bookmarks and read pages carry over.").assertExists()
        onNodeWithText("Save changes").performClick()
        waitUntil(timeoutMillis = 5_000) { saved }
        assertEquals(3, runBlocking { books.pageCount(id) })
    }
}
