package com.idiomiko.core.data

import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.discover.GdlCatalog
import com.idiomiko.core.data.discover.GutenbergCatalog
import com.idiomiko.core.data.discover.GutenbergText
import com.idiomiko.core.data.repository.BookRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.data.repository.WordsReadRepositoryImpl
import com.idiomiko.core.domain.books.discover.BookCatalog
import com.idiomiko.core.domain.books.discover.BookDiscoveryService
import com.idiomiko.core.domain.books.discover.BookSource
import com.idiomiko.core.domain.books.discover.CatalogPage
import com.idiomiko.core.domain.books.discover.Difficulty
import com.idiomiko.core.domain.books.discover.FoundBook
import com.idiomiko.core.domain.books.discover.LibraryDownException
import com.idiomiko.core.domain.books.discover.LibraryStatus
import com.idiomiko.core.domain.books.discover.LicensedText
import com.idiomiko.core.domain.books.discover.TextCoverage
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.Term
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.service.BookService
import com.idiomiko.core.domain.service.ReadingService
import com.idiomiko.core.domain.service.TermService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Free books found in Project Gutenberg and the Global Digital Library, judged by the words the reader knows and added to the library. */
class BookDiscoveryTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(answer: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) = HttpClient(MockEngine { request -> requests += request; answer(request) }) {
        install(HttpTimeout)
    }

    private fun MockRequestHandleScope.json(body: String) = respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun gutenbergListsTheBooksInTheLanguage() = runTest {
        val catalog = GutenbergCatalog(client { respond(OPDS) })
        val page = catalog.search("pt", "machado", 2)
        val url = requests.single().url
        assertEquals("/ebooks/search.opds/", url.encodedPath)
        assertEquals("l.pt machado", url.parameters["query"])
        assertEquals("26", url.parameters["start_index"])
        assertEquals(true, page.hasMore)
        assertNull(page.total)
        val (dom, bible) = page.books
        assertEquals("Dom Casmurro", dom.title)
        assertEquals(listOf("Machado de Assis"), dom.authors)
        assertEquals("https://www.gutenberg.org/cache/epub/55752/pg55752.txt", dom.downloadUrl)
        assertEquals("https://www.gutenberg.org/ebooks/55752", dom.pageUrl)
        // A title of its own in brackets keeps them, and a book without an author has none.
        assertEquals("Raios de extincta luz (poesias)", bible.title)
        assertEquals(emptyList(), bible.authors)
    }

    @Test
    fun aGutenbergSampleIsTheOpeningOfTheText() = runTest {
        val catalog = GutenbergCatalog(client { respond("$HEADER\nCAPITULO PRIMEIRO\n\nUma noite destas, vindo da cidade\npara o Engenho Novo, encontrei\n\nno trem da Cen") })
        val sample = catalog.sample(GutenbergCatalog(client { respond(OPDS) }).search("pt", "", 1).books.first())
        assertEquals("bytes=0-65535", requests.last().headers[HttpHeaders.Range])
        // The paragraph the range cut off is left out.
        assertEquals("CAPITULO PRIMEIRO\nUma noite destas, vindo da cidade para o Engenho Novo, encontrei", sample)
    }

    @Test
    fun aGutenbergTextLosesItsLicenceAndItsLineBreaks() {
        val raw = "$HEADER\r\nProduced by Someone\r\nat Distributed Proofreaders\r\n\r\n[Illustration]\r\n\r\nUma _noite_ destas,\r\nvindo da cidade.\r\n\r\n\r\nFim.\r\n\r\n*** END OF THE PROJECT GUTENBERG EBOOK DOM CASMURRO ***\r\nThe licence..."
        assertEquals("Uma noite destas, vindo da cidade.\nFim.", GutenbergText.clean(raw))
    }

    @Test
    fun theGlobalDigitalLibrarySearchesEveryRegionalFormOfTheLanguage() = runTest {
        val catalog = GdlCatalog(client { request -> json(if (request.url.encodedPath.endsWith("languages")) GDL_LANGUAGES else GDL_BOOKS) })
        val page = catalog.search("es", "", 2)
        assertEquals("es-es,es-ni", requests.last().url.parameters["language"])
        assertEquals("20", requests.last().url.parameters["_skip"])
        assertEquals(116, page.total)
        assertEquals(true, page.hasMore)
        val book = page.books.single()
        assertEquals("El gato y el perro", book.title)
        assertEquals("Libros para todos’ edición & más", book.description)
        assertEquals(listOf("StoryWeaver"), book.authors)
        assertEquals("Level 2", book.level)
        assertEquals(Difficulty.EASY, book.levelDifficulty)
        assertEquals("CC-BY-4.0", book.license)

        // A language the library lacks needs no search, and the language list is read once.
        assertEquals(CatalogPage(emptyList(), false, 0), catalog.search("el", "", 1))
        assertEquals(1, requests.count { it.url.encodedPath.endsWith("languages") })
    }

    @Test
    fun aFailingLibraryIsAnError() = runTest {
        val catalog = GutenbergCatalog(client { respond("", HttpStatusCode.ServiceUnavailable) })
        val e = assertFailsWith<IllegalStateException> { catalog.search("pt", "", 1) }
        assertEquals("Project Gutenberg answered 503", e.message)
    }

    @Test
    fun aLibraryDownForMaintenanceSaysSo() = runTest {
        val catalog = GutenbergCatalog(client { respond("<title>Temporarily Down for Maintenance</title>", HttpStatusCode.ServiceUnavailable) })
        val e = assertFailsWith<LibraryDownException> { catalog.search("pt", "", 1) }
        assertEquals("Project Gutenberg is down for maintenance. Try again later, or pick another library.", e.message)
    }

    @Test
    fun aTextComesFromTheMirrorWhileTheSiteIsDown() = runTest {
        val book = GutenbergCatalog(client { respond(OPDS) }).search("pt", "", 1).books.first()
        val catalog = GutenbergCatalog(client { request ->
            if (request.url.host == "www.gutenberg.org") respond("", HttpStatusCode.ServiceUnavailable)
            else respond("$HEADER\nUma noite destas.")
        })
        requests.clear()
        assertEquals("Uma noite destas.", catalog.text(book))
        assertEquals(listOf("www.gutenberg.org", "gutenberg.pglaf.org"), requests.map { it.url.host })
        assertEquals(requests[0].url.encodedPath, requests[1].url.encodedPath)
    }

    private val file = File.createTempFile("idiomiko-discover", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages))

    private val found = FoundBook(
        source = BookSource.GUTENBERG, id = "1", title = "O gato", authors = listOf("Ana"), description = "", subjects = emptyList(),
        license = "Public domain", pageUrl = "https://www.gutenberg.org/ebooks/1", downloadUrl = "https://www.gutenberg.org/ebooks/1.txt.utf-8",
    )

    private val fakeCatalog = object : BookCatalog {
        override val source = BookSource.GUTENBERG
        override suspend fun search(languageCode: String, query: String, page: Int) = CatalogPage(listOf(found), hasMore = false, total = 1)
        override suspend fun sample(book: FoundBook) = "O gato dorme na casa.\nA casa é grande, e o gato é pequeno."
        override suspend fun text(book: FoundBook) = "O gato dorme na casa.\nA casa é grande."

        // The text names the licence of a later edition than the catalog's.
        override suspend fun licensedText(book: FoundBook) = LicensedText(text(book), "CC-BY-SA-3.0")
    }

    private val service = BookDiscoveryService(listOf(fakeCatalog), reading, BookService(books, languages), books)

    @Test
    fun aBookIsJudgedByTheRunningWordsTheReaderKnows() = runTest {
        val id = languages.save(Language(name = "Portuguese"))
        val portuguese = languages.getById(id)!!
        // 14 running words, of which o, gato, casa and é, twice each, are known (a word being learned counts): 8 of 14.
        for ((word, status) in listOf("o" to TermStatus.WELL_KNOWN, "gato" to TermStatus.LEARNING_3, "casa" to TermStatus.IGNORED, "é" to TermStatus.NEW_1)) {
            terms.save(Term(languageId = id, text = word, textLc = word, status = status))
        }
        val coverage = service.coverage(found, portuguese)!!
        assertEquals(TextCoverage(words = 14, known = 8), coverage)
        assertEquals(57, coverage.percent)
        assertEquals(Difficulty.HARD, coverage.difficulty)
        assertEquals(Difficulty.EASY, TextCoverage(100, 95).difficulty)
        assertEquals(Difficulty.MEDIUM, TextCoverage(100, 85).difficulty)
        assertNull(service.coverage(found, portuguese.copy(parserType = "missing")))
    }

    @Test
    fun anAddedBookEndsWithItsSourceAndIsKnownByIt() = runTest {
        val id = languages.save(Language(name = "Portuguese"))
        val bookId = service.addToLibrary(found, languages.getById(id)!!) { "Fonte: Projeto Gutenberg, ${it.license}." }
        val book = books.getBook(bookId)!!
        assertEquals("O gato", book.title)
        assertEquals("https://www.gutenberg.org/ebooks/1", book.sourceUri)
        // A blank line sets the source apart, as in the book packs.
        assertEquals("O gato dorme na casa.\nA casa é grande.\n\nFonte: Projeto Gutenberg, CC-BY-SA-3.0.", books.getPages(bookId).joinToString("\n") { it.text })
        assertEquals(mapOf("https://www.gutenberg.org/ebooks/1" to bookId), service.libraryBooks())
    }

    /** A library that answers after [wait], or fails with [error]; it counts the searches it is asked. */
    private inner class FakeLibrary(override val source: BookSource, private val wait: Long = 0, private val error: Exception? = null, private val text: String = "O gato dorme.") : BookCatalog {
        var searches = 0
        override val sampleIsWholeText = true
        override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
            searches++
            delay(wait)
            error?.let { throw it }
            return CatalogPage(listOf(found.copy(source = source, id = "${source.tag}-1")), hasMore = false)
        }
        override suspend fun sample(book: FoundBook) = text
        override suspend fun text(book: FoundBook) = text
    }

    @Test
    fun everyLibraryIsSearchedAtOnceAndAnswersAsItCan() = runTest {
        val portuguese = languages.getById(languages.save(Language(name = "Portuguese")))!!
        val slow = FakeLibrary(BookSource.WIKISOURCE, wait = 5_000)
        val down = FakeLibrary(BookSource.GUTENBERG, wait = 1_000, error = LibraryDownException("Project Gutenberg is down for maintenance. Try again later, or pick another library."))
        val fast = FakeLibrary(BookSource.STORYBOOKS)
        val discovery = BookDiscoveryService(listOf(down, slow, fast), reading, BookService(books, languages), books)
        val results = discovery.searchAll(portuguese, "").toList()
        // The quick library comes first, the slow one last, and the one down for maintenance says so between them.
        assertEquals(listOf(BookSource.STORYBOOKS, BookSource.GUTENBERG, BookSource.WIKISOURCE), results.map { it.source })
        assertEquals(listOf(LibraryStatus.AVAILABLE, LibraryStatus.DOWN_FOR_MAINTENANCE, LibraryStatus.AVAILABLE), results.map { it.status })
        // A second visit is answered from the session's pages; the library that failed is asked again.
        discovery.searchAll(portuguese, " ").toList()
        assertEquals(listOf(2, 1, 1), listOf(down, slow, fast).map { it.searches })
    }

    @Test
    fun aWholeTextJudgedTellsItsLengthAndIsJudgedOncePerSession() = runTest {
        val portuguese = languages.getById(languages.save(Language(name = "Portuguese")))!!
        val story = FakeLibrary(BookSource.STORYBOOKS, text = "O gato dorme na casa.\n" + "A casa é grande.\n".repeat(2_000))
        val discovery = BookDiscoveryService(listOf(story), reading, BookService(books, languages), books)
        val book = found.copy(source = BookSource.STORYBOOKS)
        val coverage = discovery.coverage(book, portuguese)!!
        // Only the opening is parsed; the rest of a whole text is reckoned at the same words to a character.
        assertEquals(true, coverage.words < 8_005)
        assertEquals(8_005.0, coverage.wholeWords!!.toDouble(), 80.0)
        assertEquals(coverage, discovery.cachedCoverage(book, portuguese))
        assertNull(discovery.cachedCoverage(book.copy(id = "2"), portuguese))
        assertNull(service.coverage(found, portuguese)!!.wholeWords)
    }

    private companion object {
        const val HEADER = "The Project Gutenberg eBook of Dom Casmurro\n\nTitle: Dom Casmurro\n\n*** START OF THE PROJECT GUTENBERG EBOOK DOM CASMURRO ***\n"

        val OPDS = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
            <title>Books:  Language: Portuguese </title>
            <link rel="next" title="Next Page" type="application/atom+xml;profile=opds-catalog" href="/ebooks/search.opds/?query=l.pt&amp;start_index=51"/>
            <entry>
            <id>https://www.gutenberg.org/ebooks/55752.opds</id>
            <title>Dom Casmurro (Portuguese)</title>
            <content type="text">Machado de Assis</content>
            </entry>
            <entry>
            <id>https://www.gutenberg.org/ebooks/20874.opds</id>
            <title>Raios de extincta luz (poesias)
             (Portuguese)</title>
            <content type="text">39921 downloads</content>
            </entry>
            </feed>
        """.trimIndent()

        val GDL_LANGUAGES = """[{"name": "Spanish (Spain)", "slug": "es-es"}, {"name": "Spanish (Nicaragua)", "slug": "es-ni"}, {"name": "Estonian", "slug": "est"}, {"name": "English", "slug": "en"}]"""

        val GDL_BOOKS = """
            {"meta": {"count": 116, "limit": 20}, "books": [
              {"postId": 15316, "title": "El gato y el perro", "description": "Libros para todos&#8217; edición &amp; más",
               "postLink": "https://content.digitallibrary.io/es-es/book/el-gato/", "epubUrl": "https://content.digitallibrary.io/wp-content/uploads/el-gato.epub",
               "topic": [{"name": "Library Books"}, {"name": "Level 2"}], "publisher": "StoryWeaver", "thumbnail": "https://content.digitallibrary.io/cover.jpg",
               "license": [{"name": "CC-BY-4.0"}], "language": [{"slug": "es-es"}]},
              {"postId": 15317, "title": "Without an ePUB", "epubUrl": "", "topic": []}
            ]}
        """.trimIndent()
    }
}
