package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.LiberLiberCatalog
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

/** Liber Liber: book pages from its WordPress API, ePUBs at the address their names make or the one their download page gives, read without the edition's pages. */
class LiberLiberCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val catalog = LiberLiberCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            val path = request.url.encodedPath
            when {
                path == "/wp-json/wp/v2/pages" && request.url.parameters["page"] == "9" -> respond("""{"code":"rest_post_invalid_page_number"}""", HttpStatusCode.BadRequest)
                path == "/wp-json/wp/v2/pages" && request.url.parameters["search"] == "collodi" && request.url.parameters["search_columns"] != null ->
                    respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType to listOf("application/json"), "X-WP-Total" to listOf("0"), "X-WP-TotalPages" to listOf("0")))
                path == "/wp-json/wp/v2/pages" -> respond(PAGES, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType to listOf("application/json"), "X-WP-TotalPages" to listOf("3")))
                path == "/mediateca/libri/c/collodi/pinocchio/epub/collodi_pinocchio.epub" -> respond(EPUB, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/epub+zip"))
                path == "/opere/download/" -> respond("""<div>…</div><input type="hidden" value="https://www.liberliber.eu/mediateca/libri/c/collodi/pinocchio/epub/collodi_pinocchio.epub" id="myUrl" />""", HttpStatusCode.OK)
                else -> respond("Not Found", HttpStatusCode.NotFound)
            }
        }) { install(HttpTimeout) },
    )

    @Test
    fun withoutASearchItListsTheBooksForYoungPeopleAndNotAuthorsOrAudiobooks() = runTest {
        val page = catalog.search("it", "", 1)
        assertEquals("ragazzi", requests.single().url.parameters["search"])
        assertEquals(listOf("2345022", "2400001"), page.books.map { it.id })
        assertTrue(page.hasMore)

        val pinocchio = page.books.first()
        assertEquals("Pinocchio", pinocchio.title)
        assertEquals(listOf("Carlo Collodi"), pinocchio.authors)
        assertEquals("Il capolavoro di Collodi è una storia di grande carica umana.", pinocchio.description)
        assertEquals("CC-BY-NC-SA-4.0", pinocchio.license)
        assertEquals("https://liberliber.it/autori/autori-c/carlo-collodi-alias-carlo-lorenzini/pinocchio/", pinocchio.pageUrl)
        assertEquals("https://www.liberliber.eu/mediateca/libri/c/collodi/pinocchio/epub/collodi_pinocchio.epub", pinocchio.downloadUrl)
        // Hyphens become underscores, and accents go.
        assertEquals("https://www.liberliber.eu/mediateca/libri/f/fanciull/lisa_betta/epub/fanciull_lisa_betta.epub", page.books[1].downloadUrl)
        assertEquals("Non-commercial use", page.books[1].license, "Liber Liber's own terms")

        assertFalse(catalog.search("it", "pinocchio", 9).hasMore, "a page past the last is no error")
    }

    @Test
    fun aSearchLooksInTitlesThenInTheText() = runTest {
        catalog.search("it", "pinocchio", 1)
        assertEquals("post_title", requests.single().url.parameters["search_columns"])
        requests.clear()
        // No title has the author's name; the cards do.
        assertEquals(listOf("2345022", "2400001"), catalog.search("it", "collodi", 1).books.map { it.id })
        assertEquals(listOf("post_title", null), requests.map { it.url.parameters["search_columns"] })
    }

    @Test
    fun anEpubIsReadWithoutTheEditionsPages() = runTest {
        val pinocchio = catalog.search("it", "", 1).books.first()
        val expected = "I. Come andò che maestro Ciliegia trovò un pezzo di legno.\nC’era una volta un pezzo di legno. Non era un legno di lusso, ma un semplice pezzo da catasta, di quelli che d’inverno si mettono nelle stufe e nei caminetti per accendere il fuoco e per riscaldare le stanze. Non so come andasse, ma il fatto gli è che un bel giorno questo pezzo di legno capitò nella bottega di un vecchio falegname.\n\nFine."
        assertEquals(expected, catalog.text(pinocchio))
        assertEquals(expected, catalog.sample(pinocchio))
        assertEquals(0, requests.count { it.url.encodedPath == "/opere/download/" }, "the address the names make is right")
    }

    @Test
    fun aMissedAddressIsFoundOnTheDownloadPage() = runTest {
        val wrong = catalog.search("it", "", 1).books.first().copy(id = "2345022", downloadUrl = "https://www.liberliber.eu/mediateca/libri/l/lorenzini/pinocchio/epub/lorenzini_pinocchio.epub")
        assertTrue(catalog.text(wrong).startsWith("I. Come andò"))
        assertEquals("2345022", requests.single { it.url.encodedPath == "/opere/download/" }.url.parameters["op"])
    }

    @Test
    fun aLicenceAndASlug() {
        assertEquals("CC-BY-SA-4.0", LiberLiberCatalog.license("Creative Commons \"Attribuzione - Condividi allo stesso modo 4.0 Internazionale\""))
        assertEquals("dannunzio", LiberLiberCatalog.slug("D’Annunzio"))
        assertEquals("cehov", LiberLiberCatalog.slug("Čehov"))
    }

    private companion object {
        fun page(id: Int, title: String, link: String, card: String, epub: Boolean = true) =
            """{"id": $id, "link": "$link", "title": {"rendered": "$title"}, "content": {"rendered": "${card.replace("\"", "\\\"")}${if (epub) "<a href=\\\"https://liberliber.it/opere/download/?op=$id&type=opera_url_epub\\\">ePub</a>" else ""}"}}"""

        val PAGES = "[" + listOf(
            page(2345022, "Pinocchio", "https://liberliber.it/autori/autori-c/carlo-collodi-alias-carlo-lorenzini/pinocchio/",
                "<p>titolo:<br /><strong>Pinocchio</strong></p><p>descrizione breve:<br />Il capolavoro di Collodi è una storia di grande carica umana.</p><p>autore:<br />Carlo Collodi (alias Carlo Lorenzini)</p><p>licenza:<br />Creative Commons \"Attribuzione - Non commerciale - Condividi allo stesso modo 4.0 Internazionale\"</p>"),
            page(2400001, "Lisa-Betta", "https://liberliber.it/autori/autori-f/giuseppe-fanciulli/lisa-betta/",
                "<p>autore:<br />Giuseppe Fanciull</p><p>licenza:<br />Licenza Liber Liber</p>"),
            page(671, "Anna Franchi", "https://liberliber.it/autori/autori-f/anna-franchi/", "<p>Scrittrice, ragazzi</p>", epub = false),
            page(2354329, "Pinocchio [audiolibro]", "https://liberliber.it/autori/autori-c/carlo-collodi-alias-carlo-lorenzini/pinocchio-audiolibro/", "<p>audiolibro</p>", epub = false),
        ).joinToString(",") + "]"

        val EPUB: ByteArray = run {
            val documents = listOf(
                "Section0001.xhtml" to "<p>Pinocchio, di Carlo Collodi</p><p>Copertina</p>",
                "Section0002.xhtml" to "<h1>Informazioni</h1><p>QUESTO E-BOOK: TITOLO: Pinocchio AUTORE: Collodi, Carlo. LICENZA: questo testo è distribuito con la licenza specificata.</p>",
                "Section0003.xhtml" to "<h1>Liber Liber</h1><p>Se questo libro ti è piaciuto, aiutaci a realizzarne altri. Fai una donazione: liberliber.it/aiuta .</p>",
                "Section0004.xhtml" to "<h1>Indice</h1><p>Copertina</p><p>Informazioni</p><p>I. Come andò che maestro Ciliegia trovò un pezzo di legno.</p>",
                "Section0005.xhtml" to "<p>Pinocchio</p><p>Carlo Collodi</p>",
                "Section0006.xhtml" to "<h1>I. Come andò che maestro Ciliegia trovò un pezzo di legno.</h1><p>C’era una volta un pezzo di legno. Non era un legno di lusso, ma un semplice pezzo da catasta, di quelli che d’inverno si mettono nelle stufe e nei caminetti per accendere il fuoco e per riscaldare le stanze. Non so come andasse, ma il fatto gli è che un bel giorno questo pezzo di legno capitò nella bottega di un vecchio falegname.</p>",
                "Section0007.xhtml" to "<p>Fine.</p>",
            )
            val out = ByteArrayOutputStream()
            ZipOutputStream(out).use { zip ->
                fun put(name: String, text: String) { zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() }
                put("mimetype", "application/epub+zip")
                put("META-INF/container.xml", """<container><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>""")
                val manifest = documents.joinToString("") { (id, _) -> """<item id="$id" href="Text/$id" media-type="application/xhtml+xml"/>""" }
                val spine = documents.joinToString("") { (id, _) -> """<itemref idref="$id"/>""" }
                put("OEBPS/content.opf", """<package><manifest>$manifest</manifest><spine>$spine</spine></package>""")
                for ((id, body) in documents) put("OEBPS/Text/$id", """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>Pinocchio, Carlo Collodi</title></head><body>$body</body></html>""")
            }
            out.toByteArray()
        }
    }
}
