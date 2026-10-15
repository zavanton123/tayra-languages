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
        onNodeWithText("The book has 6 pages. Change the text or the words per page to rebuild them.").assertExists()
        assertEquals(0, onAllNodesWithText("Split by").fetchSemanticsNodes().size, "pages always end at a sentence")
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

    /** A file that names its book, as an FB2 does, gives the new book that title; the form lists the formats it reads. */
    @Test
    fun anImportedBookTakesTheTitleItsFileGives() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        runBlocking { languages.save(Language(name = "Portuguese")) }
        val viewModel = BookFormViewModel(null, books, languages, settings, service)
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L) {}) {
                        BookFormScreen(bookId = null, onNavigate = {}, onBack = {}, onSaved = { _, _ -> }, viewModel = viewModel)
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Create new book").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText("FB2", substring = true).fetchSemanticsNodes().let { kotlin.test.assertTrue(it.isNotEmpty(), "the formats name FB2") }
        val fb2 = """<?xml version="1.0" encoding="utf-8"?><FictionBook><description><title-info><book-title>O Lobo</book-title></title-info></description>
            <body><section><p>O lobo dorme na floresta.</p></section></body></FictionBook>"""
        viewModel.importFile("lobo.fb2", fb2.toByteArray())
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.importedFileName != null }
        assertEquals("O Lobo", viewModel.state.value.draft.title)
        assertEquals("O lobo dorme na floresta.", viewModel.state.value.draft.text)
    }

    /** Says a model must be downloaded until it is, then reads a photo's bytes as its text. */
    private class FakeRecognizer(private val supported: Boolean = true) : com.tayra.languages.core.domain.ocr.TextRecognizer {
        var downloaded = false
        override suspend fun readiness(language: Language) = when {
            !supported -> com.tayra.languages.core.domain.ocr.OcrReadiness.Unsupported
            downloaded -> com.tayra.languages.core.domain.ocr.OcrReadiness.Ready
            else -> com.tayra.languages.core.domain.ocr.OcrReadiness.NeedsDownload(3_600_000)
        }
        override suspend fun download(language: Language, onProgress: (Float?) -> Unit) { onProgress(0.5f); downloaded = true }
        override suspend fun recognize(image: ByteArray, language: Language) = image.decodeToString()
        override suspend fun installedPacks() = emptyList<com.tayra.languages.core.domain.ocr.OcrPack>()
        override suspend fun remove(pack: com.tayra.languages.core.domain.ocr.OcrPack) = Unit
    }

    private fun ComposeUiTest.newBook(recognizer: com.tayra.languages.core.domain.ocr.TextRecognizer): BookFormViewModel {
        runBlocking { languages.save(Language(name = "Portuguese")) }
        val viewModel = BookFormViewModel(null, books, languages, settings, service, recognizer)
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L) {}) {
                        BookFormScreen(bookId = null, onNavigate = {}, onBack = {}, onSaved = { _, _ -> }, viewModel = viewModel)
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Create new book").fetchSemanticsNodes().isNotEmpty() }
        return viewModel
    }

    /**
     * Photos of pages are read in the order their names number them, after the model their language
     * needs is offered and downloaded, and their text follows what the form already has.
     */
    @Test
    fun photosAreReadInOrderOnceTheirModelIsDownloaded() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val recognizer = FakeRecognizer()
        val viewModel = newBook(recognizer)
        viewModel.update { it.copy(text = "Prefácio.") }
        viewModel.importFiles(listOf("IMG_10.jpg" to "Página dez.".encodeToByteArray(), "IMG_2.jpg" to "Página dois.".encodeToByteArray()))
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Download text recognition?").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("3.6 MB", substring = true).assertExists()
        onNodeWithText("Download").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.notice != null }
        assertEquals("Prefácio.\n\nPágina dois.\n\nPágina dez.", viewModel.state.value.draft.text)
        assertEquals(true, recognizer.downloaded)
        onNodeWithText("Read the text of 2 photos; check them for misread words.").assertExists()
    }

    @Test
    fun aLanguageThisDeviceCannotReadSaysSo() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = newBook(FakeRecognizer(supported = false))
        viewModel.importFiles(listOf("page.jpg" to "x".encodeToByteArray()))
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.error != null }
        assertEquals("Reading Portuguese from photos is not available on this device.", viewModel.state.value.error)
        assertEquals("", viewModel.state.value.draft.text)
    }
}
