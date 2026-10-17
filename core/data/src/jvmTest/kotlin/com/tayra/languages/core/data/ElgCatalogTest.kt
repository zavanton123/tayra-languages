package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.ElgCatalog
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

/** Ebooks libres et gratuits: its books for the young unsearched, a search, and ePUBs without the title page and the edition's notes. */
class ElgCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val catalog = ElgCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            when (request.url.encodedPath) {
                "/opds/feed.php" -> respond(FEED, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/atom+xml"))
                "/newsendbook.php" -> respond(EPUB, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/epub+zip"))
                else -> error("Unexpected request ${request.url}")
            }
        }) { install(HttpTimeout) },
    )

    @Test
    fun withoutASearchItListsTheBooksForTheYoung() = runTest {
        val page = catalog.search("fr", "", 1)
        val q = requests.single().url.parameters
        assertEquals("genre", q["mode"])
        assertEquals("6", q["id"])
        assertEquals(null, q["page"])
        assertEquals(3263, page.total)
        assertTrue(page.hasMore)

        val book = page.books.single()
        assertEquals("3521", book.id)
        assertEquals("La Redoute des loups", book.title)
        assertEquals(listOf("Georges Le Faure"), book.authors, "the name as it is said")
        assertEquals("Dans une province des Balkans, le commandant Richter bey exerce une répression impitoyable.", book.description)
        assertEquals(listOf("Romans", "Aventures"), book.subjects)
        assertEquals("Non-commercial use", book.license)
        assertEquals("https://www.ebooksgratuits.com/details.php?book=3521", book.pageUrl)
        assertEquals("https://www.ebooksgratuits.com/newsendbook.php?id=3521&format=epub", book.downloadUrl)
    }

    @Test
    fun aSearchAsksTheCatalogPageByPage() = runTest {
        catalog.search("fr", "verne", 3)
        val q = requests.single().url.parameters
        assertEquals("search", q["mode"])
        assertEquals("verne", q["query"])
        assertEquals("2", q["page"], "the feed counts its pages from 0")
    }

    @Test
    fun aBookIsReadWithoutItsTitlePageAndTheEditionsNotes() = runTest {
        val book = catalog.search("fr", "", 1).books.single()
        val text = catalog.text(book)
        assertEquals("I LA REDOUTE DES LOUPS\nSon Excellence Richter bey fumait béatement un cigare de choix.", text)
        assertFalse("propos" in catalog.sample(book))
        assertEquals(1, requests.count { it.url.encodedPath == "/newsendbook.php" }, "the text judged is the text added")
    }

    private companion object {
        val FEED = """<?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom" xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/">
            <link type="application/atom+xml" href="https://www.ebooksgratuits.com/opds/feed.php?page=1" rel="next" title="Next Page"/>
            <opensearch:totalResults>3263</opensearch:totalResults>
            <entry>
              <title>La Redoute des loups</title>
              <id>https://www.ebooksgratuits.com/details.php?book=3521</id>
              <author><name>Le Faure, Georges</name><uri>https://www.ebooksgratuits.com/ebooks.php?auteur=Le+Faure_Georges</uri></author>
              <category term="Romans" label="Romans"/><category term="Aventures" label="Aventures"/>
              <content type="text"><![CDATA[Dans une province des Balkans, le commandant Richter bey exerce une répression impitoyable.<br /><br />Édition Ebooks libres et gratuits.]]></content>
              <link type="application/epub+zip" href="https://www.ebooksgratuits.com/newsendbook.php?id=3521&amp;format=epub" rel="http://opds-spec.org/acquisition" />
              <link type="application/pdf" href="https://www.ebooksgratuits.com/newsendbook.php?id=3521&amp;format=pdf" rel="http://opds-spec.org/acquisition" />
            </entry></feed>"""

        val EPUB: ByteArray = run {
            val documents = listOf(
                "titlepage" to "<div><svg><image href=\"cover.jpeg\"/></svg></div>",
                "id27" to "<p>Georges Le Faure</p><p>LA REDOUTE DES LOUPS</p><p>1934</p>",
                "id26" to "<h1>I LA REDOUTE DES LOUPS</h1><p>Son Excellence Richter bey fumait béatement un cigare de choix.</p>",
                "id21" to "<h1>À propos de cette édition électronique</h1><p>Texte libre de droits. Corrections, édition, conversion informatique et publication par le groupe Ebooks libres et gratuits.</p>",
            )
            val out = ByteArrayOutputStream()
            ZipOutputStream(out).use { zip ->
                fun put(name: String, text: String) { zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() }
                put("mimetype", "application/epub+zip")
                put("META-INF/container.xml", """<container><rootfiles><rootfile full-path="content.opf"/></rootfiles></container>""")
                val manifest = documents.joinToString("") { (id, _) -> """<item id="$id" href="$id.html" media-type="application/xhtml+xml"/>""" }
                val spine = documents.joinToString("") { (id, _) -> """<itemref idref="$id"/>""" }
                put("content.opf", """<package><manifest>$manifest</manifest><spine>$spine</spine></package>""")
                for ((id, body) in documents) put("$id.html", """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"><body>$body</body></html>""")
            }
            out.toByteArray()
        }
    }
}
