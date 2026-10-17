package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.ElejandriaCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.decodeURLPart
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Elejandría: its children's collections unsearched, a search through its sitemap, and each listed book read from its page. */
class ElejandriaCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val catalog = ElejandriaCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            val path = request.url.encodedPath.decodeURLPart()
            val html = headersOf(HttpHeaders.ContentType, "text/html; charset=UTF-8")
            when {
                path == "/sitemap.xml" -> respond(SITEMAP, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/xml"))
                path == "/coleccion/cuentos-infantiles" -> respond(collection("el-principito/antoine-de-saint-exupery/2256", "cuentos-de-la-selva/horacio-quiroga/301"), HttpStatusCode.OK, html)
                path.startsWith("/coleccion/") -> respond(collection("el-principito/antoine-de-saint-exupery/2256"), HttpStatusCode.OK, html)
                path == "/libro/el-principito/antoine-de-saint-exupery/2256" -> respond(page("El principito", "Antoine de Saint-Exupéry", 2256, 5569, 5570), HttpStatusCode.OK, html)
                path == "/libro/cuentos-de-la-selva/horacio-quiroga/301" -> respond(page("Cuentos de la selva", "Horacio Quiroga", 301, 900, 901), HttpStatusCode.OK, html)
                path == "/libro/gestion-administrativa/batet-jimenez/50" -> respond(page("Gestión administrativa", "Batet Jiménez", 50, 100, null), HttpStatusCode.OK, html)
                path == "/libro/link_descarga_libro/2256/5570" -> respond(EPUB, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/octet-stream"))
                else -> respond("Not Found", HttpStatusCode.NotFound)
            }
        }) { install(HttpTimeout) },
    )

    @Test
    fun withoutASearchItListsTheChildrensCollectionsReadFromTheirPages() = runTest {
        val page = catalog.search("es", "", 1)
        assertEquals(listOf("El principito", "Cuentos de la selva"), page.books.map { it.title }, "each book once, and none of the popular ones beside the collection")
        assertEquals(2, page.total)
        assertFalse(page.hasMore)
        val principito = page.books.first()
        assertEquals("2256", principito.id)
        assertEquals(listOf("Antoine de Saint-Exupéry"), principito.authors)
        assertEquals("https://www.elejandria.com/libro/link_descarga_libro/2256/5570", principito.downloadUrl, "the ePub, not the PDF")
        assertEquals("https://www.elejandria.com/covers/El_principito-lg.png", principito.coverUrl)
        assertEquals("Public domain or open licence", principito.license)
        assertEquals("https://www.elejandria.com/libro/el-principito/antoine-de-saint-exupery/2256", principito.pageUrl)
    }

    @Test
    fun aSearchFindsTheWordsOfTheSitemapsAddressesWhateverTheirAccents() = runTest {
        assertEquals(listOf("Cuentos de la selva"), catalog.search("es", "Quiroga selva", 1).books.map { it.title })
        // A book without an ePub is left out.
        assertEquals(emptyList(), catalog.search("es", "gestión", 1).books)
        assertEquals(1, requests.count { it.url.encodedPath == "/sitemap.xml" }, "the sitemap is fetched once")
    }

    @Test
    fun aBookIsReadFromItsEpub() = runTest {
        val principito = catalog.search("es", "principito", 1).books.single()
        assertEquals("Cuando yo tenía seis años vi en un libro una magnífica lámina.", catalog.text(principito))
        assertEquals(catalog.text(principito), catalog.sample(principito))
        assertEquals(1, requests.count { "link_descarga_libro" in it.url.encodedPath }, "the text judged is the text added")
    }

    private companion object {
        val SITEMAP = """<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
            <url><loc>https://www.elejandria.com</loc></url>
            <url><loc>https://www.elejandria.com/libro/el-principito/antoine-de-saint-exupery/2256</loc></url>
            <url><loc>https://www.elejandria.com/libro/cuentos-de-la-selva/horacio-quiroga/301</loc></url>
            <url><loc>https://www.elejandria.com/libro/gestion-administrativa/batet-jimenez/50</loc></url></urlset>"""

        fun collection(vararg books: String) = "<html><body><h1>Colección</h1>" +
            books.joinToString("") { """<a href="https://www.elejandria.com/libro/$it">libro</a>""" } +
            """<h2>Otros libros populares en este momento</h2><a href="https://www.elejandria.com/libro/kamasutra-el-arte-de-amar/anonimo/828">Kamasutra</a></body></html>"""

        fun page(title: String, author: String, id: Int, pdf: Int, epub: Int?) = """<html><head>
            <meta property="og:title"         content="Libro $title en PDF y ePub - Elejandría" />
            <meta property="og:description" content="📘 📙 Descargar gratis $title de $author en PDF, ePUB o MOBI ✅" />
            <meta property="og:image" content="https://www.elejandria.com/covers/${title.replace(' ', '_')}-lg.png" /></head><body>
            <a class="btn" href="https://www.elejandria.com/libro/descargar/x/y/$id/$pdf"><span class="book-description-link">Descargar en PDF</span></a>
            ${epub?.let { """<a class="btn" href="https://www.elejandria.com/libro/descargar/x/y/$id/$it"><span class="book-description-link">Descargar en ePub</span></a>""" }.orEmpty()}
            </body></html>"""

        val EPUB: ByteArray = run {
            val out = ByteArrayOutputStream()
            ZipOutputStream(out).use { zip ->
                fun put(name: String, text: String) { zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() }
                put("mimetype", "application/epub+zip")
                put("META-INF/container.xml", """<container><rootfiles><rootfile full-path="content.opf"/></rootfiles></container>""")
                put("content.opf", """<package><manifest><item id="c1" href="c1.html" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="c1"/></spine></package>""")
                put("c1.html", """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"><body><p>Libro descargado en www.elejandria.com, tu sitio web de obras de dominio público</p><p>¡Esperamos que lo disfrutéis!</p><p>Cuando yo tenía seis años vi en un libro una magnífica lámina.</p><p>¡Gracias por leer este libro de www.elejandria.com!</p><p>Descubre nuestra colección de obras de dominio público en castellano en nuestra web</p></body></html>""")
            }
            out.toByteArray()
        }
    }
}
