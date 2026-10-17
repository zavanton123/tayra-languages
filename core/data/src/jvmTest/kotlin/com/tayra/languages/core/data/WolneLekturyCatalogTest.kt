package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.WolneLekturyCatalog
import com.tayra.languages.core.data.discover.WolneLekturyText
import com.tayra.languages.core.domain.books.discover.LicensedText
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Wolne Lektury's books: its children's collection, a search, and texts without their title page and closing notes. */
class WolneLekturyCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val catalog = WolneLekturyCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            val path = request.url.encodedPath
            when {
                path == "/api/collections/literatura-dla-dzieci-i-mlodziezy-2/" -> respond(COLLECTION, HttpStatusCode.OK, JSON)
                path == "/api/2/books/" -> respond(SEARCH, HttpStatusCode.OK, JSON)
                path == "/api/2/books/lalka/" -> respond("""{"slug": "lalka", "children": [{"slug": "lalka-tom-pierwszy", "title": "Lalka, tom pierwszy"}, {"slug": "lalka-tom-drugi", "title": "Lalka, tom drugi"}]}""", HttpStatusCode.OK, JSON)
                path == "/media/book/txt/calineczka.txt" -> respond(CALINECZKA, HttpStatusCode.OK, TEXT)
                path == "/media/book/txt/lalka-tom-pierwszy.txt" -> respond(volume("Tom pierwszy", "Ten utwór jest w domenie publicznej."), HttpStatusCode.OK, TEXT)
                path == "/media/book/txt/lalka-tom-drugi.txt" -> respond(volume("Tom drugi", "Ten utwór jest w domenie publicznej."), HttpStatusCode.OK, TEXT)
                else -> respond("<html>404 Not Found</html>", HttpStatusCode.NotFound, TEXT)
            }
        }) { install(HttpTimeout) },
    )

    @Test
    fun withoutASearchItListsTheChildrensCollectionPageByPage() = runTest {
        val first = catalog.search("pl", "", 1)
        assertEquals(25, first.total)
        assertEquals(20, first.books.size)
        assertTrue(first.hasMore)
        val book = first.books.first()
        assertEquals("calineczka", book.id)
        assertEquals("Calineczka", book.title)
        assertEquals(listOf("Hans Christian Andersen"), book.authors)
        assertEquals(listOf("Baśń"), book.subjects)
        assertEquals("https://wolnelektury.pl/katalog/lektura/calineczka/", book.pageUrl)
        assertEquals("https://wolnelektury.pl/media/book/txt/calineczka.txt", book.downloadUrl)
        assertEquals("https://wolnelektury.pl/media/book/cover_api_thumb/calineczka.jpg", book.coverUrl)
        assertEquals(emptyList(), first.books[1].authors, "an unknown author is no author")

        val second = catalog.search("pl", "", 2)
        assertEquals(5, second.books.size)
        assertFalse(second.hasMore)
        assertEquals(1, requests.count { it.url.encodedPath.startsWith("/api/collections/") }, "the collection is fetched once")
    }

    @Test
    fun aSearchListsThePolishBooksOnly() = runTest {
        val page = catalog.search("pl", "tadeusz", 2)
        val request = requests.single()
        assertEquals("tadeusz", request.url.parameters["search"])
        assertEquals("20", request.url.parameters["offset"])
        assertEquals(listOf("pan-tadeusz"), page.books.map { it.id })
        assertEquals(listOf("Adam Mickiewicz"), page.books.single().authors)
        assertEquals(listOf("Epopeja"), page.books.single().subjects)
        assertEquals(41, page.total)
        assertTrue(page.hasMore)
    }

    @Test
    fun aTextLosesItsTitlePageAndClosingNotesAndKeepsItsLicence() = runTest {
        val book = catalog.search("pl", "", 1).books.first()
        val text = catalog.licensedText(book)
        assertEquals(LicensedText("Pewna dobra kobieta bardzo pragnęła mieć maleńkie dziecko.\n— Tak bym chciała — rzekła.", "Free Art License 1.3"), text)

        assertEquals(text.text, catalog.sample(book))
        assertEquals("bytes=0-65535", requests.last().headers[HttpHeaders.Range])
    }

    @Test
    fun aWorkInVolumesIsReadFromItsVolumes() = runTest {
        val lalka = catalog.search("pl", "", 1).books.first().copy(id = "lalka", title = "Lalka", downloadUrl = "https://wolnelektury.pl/media/book/txt/lalka.txt")
        assertEquals(LicensedText("Tom pierwszy zaczyna się tak.\nTom drugi zaczyna się tak.", "Public domain"), catalog.licensedText(lalka))
        assertEquals("Tom pierwszy zaczyna się tak.", catalog.sample(lalka))
    }

    @Test
    fun theLicenceIsReadFromTheClosingNotes() {
        assertEquals("Public domain", WolneLekturyText.license("-----\nTen utwór jest w domenie publicznej.\n"))
        assertEquals("CC-BY-SA-3.0", WolneLekturyText.license("Ten utwór jest udostępniony na licencji Creative Commons Uznanie Autorstwa – Na Tych Samych Warunkach 3.0 PL."))
        assertNull(WolneLekturyText.license("Wszystkie zasoby Wolnych Lektur możesz swobodnie wykorzystywać."))
        // Verses keep their lines; the indentation of every other verse goes.
        assertEquals("Litwo! Ojczyzno moja!\nIle cię trzeba cenić,", WolneLekturyText.clean("Adam Mickiewicz\r\n\r\nPan Tadeusz\r\n\r\n\r\n    Litwo! Ojczyzno moja!\r\nIle cię trzeba cenić,\r\n"))
    }

    private companion object {
        val JSON = headersOf(HttpHeaders.ContentType, "application/json")
        val TEXT = headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8")

        val COLLECTION = """{"title": "Literatura dla dzieci i młodzieży", "books": [
            {"slug": "calineczka", "title": "Calineczka", "author": "Hans Christian Andersen", "genre": "Baśń",
             "simple_thumb": "https://wolnelektury.pl/media/book/cover_api_thumb/calineczka.jpg", "href": "https://wolnelektury.pl/api/books/calineczka/"},
            {"slug": "agady", "title": "Agady talmudyczne", "author": "Autor nieznany ", "genre": "Legenda, Opowiadanie", "simple_thumb": ""},
            ${(3..25).joinToString(",\n") { """{"slug": "ksiazka-$it", "title": "Książka $it", "author": "Autor $it", "genre": "Bajka"}""" }}
            ]}"""

        val SEARCH = """{"totalItems": 41, "view": {"previous": "https://wolnelektury.pl/api/2/books/?limit=20&search=tadeusz", "next": "https://wolnelektury.pl/api/2/books/?limit=20&offset=40&search=tadeusz"},
            "member": [
              {"slug": "pan-tadeusz", "title": "Pan Tadeusz", "language": "pol", "authors": [{"name": "Adam Mickiewicz"}], "genres": [{"name": "Epopeja"}], "children": [], "cover_thumb": "https://wolnelektury.pl/media/book/cover_thumb/pan-tadeusz.jpg"},
              {"slug": "bereszit", "title": "בראשית", "language": "heb", "authors": [], "genres": [{"name": "Tekst religijny"}], "children": []}
            ]}"""

        val CALINECZKA = "﻿Hans Christian Andersen\r\n\r\nCalineczka\r\ntłum. Cecylia Niewiadomska\r\n\r\nISBN 978-83-288-2047-0\r\n\r\n\r\n" +
            "Pewna dobra kobieta bardzo pragnęła mieć maleńkie dziecko.\r\n\r\n— Tak bym chciała — rzekła.\r\n\r\n\r\n" +
            "-----\r\nTa lektura, podobnie jak tysiące innych, jest dostępna on-line na stronie wolnelektury.pl.\r\n\r\n" +
            "Ten utwór jest udostępniony na licencji Licencja Wolnej Sztuki 1.3.: https://artlibre.org/licence/lal/pl/\r\n" +
            "Wszystkie materiały dodatkowe (przypisy, motywy literackie) są udostępnione na Licencji Wolnej Sztuki 1.3: https://artlibre.org/licence/lal/pl/\r\n"

        fun volume(name: String, license: String) = "Bolesław Prus\n\nLalka, $name\n\n\n$name zaczyna się tak.\n\n-----\n$license\n"
    }
}
