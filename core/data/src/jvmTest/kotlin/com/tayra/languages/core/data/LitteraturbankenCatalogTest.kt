package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.LitteraturbankenCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Litteraturbanken's CC0 e-texts: listed once, the most read first, searched by title and author, and read without the edition's front matter. */
class LitteraturbankenCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val catalog = LitteraturbankenCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            when (request.url.encodedPath) {
                "/api/list_all/etext" -> respond(LIST, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                "/txt/epub/S%C3%B6derbergH_DoktorGlas.epub" -> respond(GLAS, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/epub+zip"))
                else -> error("Unexpected request ${request.url}")
            }
        }) { install(HttpTimeout) },
    )

    @Test
    fun itListsTheFreeSwedishTextsTheMostReadFirst() = runTest {
        val first = catalog.search("sv", "", 1)
        val request = requests.single()
        assertEquals("popularity|desc", request.url.parameters["sort_field"])
        assertTrue(request.url.parameters["include"]!!.contains("epub_license"), "the fields are named, or every page of every text comes too")
        // Under the bank's own terms, without an ePUB, or in French: left out.
        assertEquals(22, first.total)
        assertEquals(20, first.books.size)
        assertTrue(first.hasMore)

        val glas = first.books.first()
        assertEquals("lb1728740", glas.id)
        assertEquals("Doktor Glas", glas.title)
        assertEquals(listOf("Hjalmar Söderberg"), glas.authors)
        assertEquals("CC0", glas.license)
        assertEquals("https://litteraturbanken.se/författare/SöderbergH/titlar/DoktorGlas/info", glas.pageUrl)
        assertEquals("https://litteraturbanken.se/txt/epub/S%C3%B6derbergH_DoktorGlas.epub", glas.downloadUrl)

        assertEquals(2, catalog.search("sv", "", 2).books.size)
        assertEquals(1, requests.size, "the list is fetched once")
    }

    @Test
    fun aSearchMatchesEveryWordInTheTitleOrAuthors() = runTest {
        assertEquals(listOf("Nils Holgerssons underbara resa"), catalog.search("sv", "lagerlöf  NILS", 1).books.map { it.title })
        assertEquals(listOf("Doktor Glas"), catalog.search("sv", "glas", 1).books.map { it.title })
        val none = catalog.search("sv", "kallocain", 1)
        assertEquals(0, none.total)
        assertFalse(none.hasMore)
    }

    @Test
    fun aBookIsReadWithoutItsCoverAndColophonAndKept() = runTest {
        val glas = catalog.search("sv", "glas", 1).books.single()
        assertEquals("12 juni.\nJag har aldrig sett en sådan sommar.", catalog.sample(glas))
        assertEquals("12 juni.\nJag har aldrig sett en sådan sommar.", catalog.text(glas))
        assertEquals(1, requests.count { it.url.encodedPath.endsWith(".epub") }, "the text judged is the text added")
    }

    private companion object {
        val LIST = """{"hits": 26, "data": [
            {"lbworkid": "lb1728740", "titleid": "DoktorGlas", "shorttitle": "Doktor Glas", "authors": [{"full_name": "Hjalmar Söderberg", "authorid": "SöderbergH"}], "epub_license": "cc-0", "has_epub": true, "language": "swe"},
            {"lbworkid": "lb2", "titleid": "Kallocain", "shorttitle": "Kallocain", "authors": [{"full_name": "Karin Boye", "authorid": "BoyeK"}], "epub_license": "lb-2-20120306", "has_epub": true, "language": "swe"},
            {"lbworkid": "lb3", "titleid": "NilsHolgersson1", "shorttitle": "Nils Holgerssons underbara resa", "authors": [{"full_name": "Selma Lagerlöf", "authorid": "LagerlöfS"}], "epub_license": "cc-0", "has_epub": true, "language": "swe"},
            {"lbworkid": "lb4", "titleid": "Faksimil", "shorttitle": "Faksimil", "authors": [{"full_name": "Selma Lagerlöf", "authorid": "LagerlöfS"}], "epub_license": "", "has_epub": false, "language": "swe"},
            {"lbworkid": "lb5", "titleid": "Poemes", "shorttitle": "Poèmes", "authors": [{"full_name": "Gustaf III", "authorid": "GustafIII"}], "epub_license": "cc-0", "has_epub": true, "language": "fra"},
            ${(6..25).joinToString(",\n") { """{"lbworkid": "lb$it", "titleid": "Bok$it", "shorttitle": "Bok $it", "authors": [{"full_name": "Författare $it", "authorid": "F$it"}], "epub_license": "cc-0", "has_epub": true, "language": "swe"}""" }}
            ]}"""

        val GLAS: ByteArray = run {
            val documents = listOf(
                "cover" to """<body id="titelsida"><div class="cover-wrapper"><img src="cover.jpg"/></div></body>""",
                "kolofon1" to """<body id="kolofon"><p>Hjalmar Söderberg. Doktor Glas. Litteraturbanken.</p></body>""",
                "kolofon2" to """<body id="kolofon"><p>Information om utgåvan. CC0.</p></body>""",
                "chapter1" to """<body><div class="chapter"><h4>12 juni.</h4><p class="indfirst">Jag har aldrig sett en sådan sommar.</p></div></body>""",
            )
            val out = ByteArrayOutputStream()
            ZipOutputStream(out).use { zip ->
                fun put(name: String, text: String) { zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() }
                put("mimetype", "application/epub+zip")
                put("META-INF/container.xml", """<container><rootfiles><rootfile full-path="content.opf"/></rootfiles></container>""")
                val manifest = documents.joinToString("") { (id, _) -> """<item id="$id" href="$id.xhtml" media-type="application/xhtml+xml"/>""" }
                val spine = documents.joinToString("") { (id, _) -> """<itemref idref="$id"/>""" }
                put("content.opf", """<package><manifest>$manifest</manifest><spine>$spine</spine></package>""")
                for ((id, body) in documents) put("$id.xhtml", """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>$id</title></head>$body</html>""")
            }
            out.toByteArray()
        }
    }
}
