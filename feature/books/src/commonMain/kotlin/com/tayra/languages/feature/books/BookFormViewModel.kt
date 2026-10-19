package com.tayra.languages.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.data.files.FileImportException
import com.tayra.languages.core.data.files.FileTextExtractor
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.ocr.NoTextRecognizer
import com.tayra.languages.core.domain.ocr.OcrException
import com.tayra.languages.core.domain.ocr.OcrReadiness
import com.tayra.languages.core.domain.ocr.TextRecognizer
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.BookValidationException
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.web.WebPageException
import com.tayra.languages.core.domain.web.WebPageReader
import com.tayra.languages.core.ui.i18n.LanguageCase
import com.tayra.languages.core.ui.i18n.languageInSentence
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.state.UiEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class BookFormUiState(
    val loading: Boolean = true,
    val draft: BookDraft = BookDraft(languageId = 0, title = ""),
    val languages: List<Language> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val importedFileName: String? = null,
    val error: String? = null,
    val notice: String? = null,
    val busy: Boolean = false,
    /** For an existing book: its text and page size as loaded, to tell whether the pages need rebuilding. */
    val loaded: BookDraft? = null,
    val pageCount: Int = 0,
    /** Text recognition the photos being imported need, offered for download. */
    val ocrOffer: OcrOffer? = null,
    /** What a long import is doing (downloading a model, reading photo 2 of 5). */
    val progress: String? = null,
    /** Whether a web page's article can be read into the book here. */
    val readsWebPages: Boolean = false,
) {
    val isNew: Boolean get() = draft.id == null

    /** Whether saving cuts the text into new pages: only when the text or the page size changed. */
    val rebuildsPages: Boolean
        get() = loaded != null && (draft.text != loaded.text || draft.wordsPerPage != loaded.wordsPerPage)
    val language: Language? get() = languages.firstOrNull { it.id == draft.languageId }
}

/** The download that reading [languageName] from photos needs: [sizeBytes] of it, null when Google Play fetches it. */
data class OcrOffer(val languageName: String, val sizeBytes: Long?)

sealed interface BookFormEvent {
    data class Saved(val bookId: Long, val isNew: Boolean) : BookFormEvent
}

class BookFormViewModel(
    private val bookId: Long?,
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val bookService: BookService,
    private val recognizer: TextRecognizer = NoTextRecognizer,
    private val webPages: WebPageReader? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(BookFormUiState(readsWebPages = webPages != null))
    val state: StateFlow<BookFormUiState> = _state.asStateFlow()
    val events = UiEvents<BookFormEvent>()

    init {
        viewModelScope.launch {
            val languageList = languages.getAll()
            val tags = books.allBookTags()
            val book = bookId?.let { books.getBook(it) }
            val draft = if (bookId != null) {
                if (book == null) {
                    BookDraft(languageId = 0, title = "")
                } else {
                    BookDraft(
                        id = book.id,
                        languageId = book.languageId,
                        title = book.title,
                        text = bookService.text(book.id),
                        sourceUri = book.sourceUri.orEmpty(),
                        tags = book.tags,
                        wordsPerPage = bookService.estimatedWordsPerPage(book.id),
                        audioFilename = book.audioFilename,
                    )
                }
            } else {
                val current = settings.current.currentLanguageId
                val languageId = if (languageList.any { it.id == current }) current else languageList.singleOrNull()?.id ?: 0
                BookDraft(languageId = languageId, title = "")
            }
            _state.update {
                it.copy(
                    loading = false,
                    draft = draft,
                    languages = languageList,
                    tagSuggestions = tags,
                    loaded = draft.takeIf { book != null },
                    pageCount = book?.let { b -> books.pageCount(b.id) } ?: 0,
                )
            }
        }
    }

    fun update(transform: (BookDraft) -> BookDraft) {
        _state.update { it.copy(draft = transform(it.draft), error = null) }
    }

    fun importFile(fileName: String, bytes: ByteArray) {
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                val extracted = FileTextExtractor.extract(fileName, bytes)
                val text = extracted.text
                _state.update {
                    it.copy(
                        busy = false,
                        importedFileName = fileName,
                        draft = it.draft.copy(text = text, title = it.draft.title.ifBlank { extracted.title ?: fileName.substringBeforeLast('.') }),
                        notice = trPlural(text.length, "Loaded {0} character from {1}.", "Loaded {0} characters from {1}.", fileName),
                    )
                }
            } catch (e: FileImportException) {
                _state.update { it.copy(busy = false, error = importError(fileName, e.message.orEmpty())) }
            }
        }
    }

    /**
     * Reads the article of the web page at [address] into the book, its title too when the book has
     * none yet, and keeps the address as the book's source; [onRead] follows once the text is in.
     */
    fun importWebPage(address: String, onRead: () -> Unit = {}) {
        val reader = webPages ?: return
        _state.update { it.copy(busy = true, error = null, notice = null, progress = tr("Reading the page…")) }
        viewModelScope.launch {
            try {
                val article = reader.read(address)
                val site = article.siteName ?: hostOf(article.url)
                _state.update {
                    it.copy(
                        busy = false,
                        progress = null,
                        importedFileName = null,
                        draft = it.draft.copy(text = article.text, title = it.draft.title.ifBlank { article.title ?: site }, sourceUri = article.url),
                        notice = trPlural(article.text.length, "Loaded {0} character from {1}.", "Loaded {0} characters from {1}.", site),
                    )
                }
                onRead()
            } catch (e: WebPageException) {
                val message = when (e) {
                    is WebPageException.InvalidAddress -> tr("Enter the address of a web page, such as https://example.com/article.")
                    is WebPageException.Unreachable -> when (e.status) {
                        null -> tr("The page could not be loaded. Check the address and your connection.")
                        401, 402, 403, 451 -> tr("The site does not let apps read this page; it may need a subscription or a sign-in.")
                        else -> tr("The site answered with error {0}.", e.status)
                    }
                    is WebPageException.NoArticle -> tr("No article text was found on this page.")
                }
                _state.update { it.copy(busy = false, progress = null, error = message) }
            }
        }
    }

    private fun hostOf(url: String): String = url.substringAfter("://").substringBefore('/').removePrefix("www.")

    /** Photos waiting for the text recognition model offered for download. */
    private var pendingPhotos: List<Pair<String, ByteArray>> = emptyList()

    /** Files chosen to import: one document, whose text is read, or photos of pages, whose text is recognized. */
    fun importFiles(files: List<Pair<String, ByteArray>>) {
        val photos = files.filter { FileTextExtractor.isImage(it.first) }
        when {
            files.isEmpty() -> Unit
            photos.size == files.size -> importPhotos(photos.sortedWith { a, b -> naturalCompare(a.first, b.first) })
            files.size == 1 -> importFile(files[0].first, files[0].second)
            else -> _state.update { it.copy(error = tr("Choose one document, or one or more photos of pages.")) }
        }
    }

    /** Recognizes the text of [photos], once the language's model is here; asks to download it first if not. */
    fun importPhotos(photos: List<Pair<String, ByteArray>>) {
        val language = _state.value.language ?: run {
            _state.update { it.copy(error = tr("Please select a language")) }
            return
        }
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            when (val readiness = recognizer.readiness(language)) {
                OcrReadiness.Unsupported -> _state.update {
                    it.copy(busy = false, error = tr("Reading {0} from photos is not available on this device.", languageInSentence(language.name, LanguageCase.PREPOSITIONAL)))
                }
                is OcrReadiness.NeedsDownload -> {
                    pendingPhotos = photos
                    _state.update { it.copy(busy = false, ocrOffer = OcrOffer(language.name, readiness.sizeBytes)) }
                }
                OcrReadiness.Ready -> recognize(photos, language)
            }
        }
    }

    /** Downloads the offered text recognition model, then reads the photos that wait for it. */
    fun downloadRecognition() {
        val language = _state.value.language ?: return
        val photos = pendingPhotos
        pendingPhotos = emptyList()
        _state.update { it.copy(ocrOffer = null, busy = true, error = null, progress = tr("Downloading text recognition…")) }
        viewModelScope.launch {
            try {
                recognizer.download(language) { share ->
                    val percent = share?.let { (it * 100).roundToInt() }
                    _state.update { it.copy(progress = percent?.let { p -> tr("Downloading text recognition… {0}%", p) } ?: tr("Downloading text recognition…")) }
                }
            } catch (e: OcrException) {
                _state.update { it.copy(busy = false, progress = null, error = tr(e.message.orEmpty())) }
                return@launch
            }
            recognize(photos, language)
        }
    }

    /** Leaves the photos unread, the model not downloaded. */
    fun cancelPhotos() {
        pendingPhotos = emptyList()
        _state.update { it.copy(ocrOffer = null, busy = false) }
    }

    /** Reads [photos] in order and adds their text after any already in the form, a page of a book at a time. */
    private suspend fun recognize(photos: List<Pair<String, ByteArray>>, language: Language) {
        val texts = mutableListOf<String>()
        try {
            photos.forEachIndexed { index, (_, bytes) ->
                _state.update { it.copy(busy = true, progress = tr("Reading photo {0} of {1}…", index + 1, photos.size)) }
                texts += recognizer.recognize(bytes, language)
            }
        } catch (e: OcrException) {
            _state.update { it.copy(busy = false, progress = null, error = tr(e.message.orEmpty())) }
            return
        } catch (e: Exception) {
            _state.update { it.copy(busy = false, progress = null, error = tr("Could not read the photo")) }
            return
        }
        val text = texts.filter { it.isNotBlank() }.joinToString("\n\n")
        if (text.isBlank()) {
            _state.update { it.copy(busy = false, progress = null, error = trPlural(photos.size, "No text was found in the photo.", "No text was found in the photos.")) }
            return
        }
        _state.update {
            it.copy(
                busy = false,
                progress = null,
                importedFileName = photos.last().first,
                draft = it.draft.copy(text = listOf(it.draft.text.trim(), text).filter { part -> part.isNotEmpty() }.joinToString("\n\n")),
                notice = trPlural(photos.size, "Read the text of {0} photo; check it for misread words.", "Read the text of {0} photos; check them for misread words."),
            )
        }
    }

    fun save() {
        val draft = _state.value.draft
        val rebuild = _state.value.rebuildsPages
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val id = if (draft.id == null) bookService.create(draft) else { bookService.update(draft, rebuildPages = rebuild); draft.id!! }
                events.send(BookFormEvent.Saved(id, draft.id == null))
            } catch (e: BookValidationException) {
                _state.update { it.copy(busy = false, error = e.message?.let { m -> tr(m) }) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.message?.let { m -> tr(m) } ?: tr("Could not save book")) }
            }
        }
    }

    /** Names in the order people number them: "IMG_2" before "IMG_10". */
    private fun naturalCompare(a: String, b: String): Int {
        val chunk = Regex("\\d+|\\D+")
        val left = chunk.findAll(a.lowercase()).map { it.value }.toList()
        val right = chunk.findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(left.size, right.size)) {
            val x = left[i]
            val y = right[i]
            val order = if (x[0].isDigit() && y[0].isDigit()) compareValuesBy(x, y, { it.length }, { it }) else x.compareTo(y)
            if (order != 0) return order
        }
        return left.size - right.size
    }

    /** The import error in the interface language; the messages naming the file are matched to put the name in place. */
    private fun importError(fileName: String, message: String): String = when (message) {
        "$fileName is empty." -> tr("{0} is empty.", fileName)
        "$fileName is not utf-8 encoding, please convert it to utf-8 first" -> tr("{0} is not utf-8 encoding, please convert it to utf-8 first", fileName)
        else -> tr(message)
    }
}
