package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.AdamastorCatalog
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

/** Projecto Adamastor's books: its catalogue page searched here, and ePUBs found from each book's page and read without the edition's pages. */
class AdamastorCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val catalog = AdamastorCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            val html = headersOf(HttpHeaders.ContentType, "text/html; charset=UTF-8")
            when (request.url.encodedPath) {
                "/catalogo/" -> respond(CATALOGUE, HttpStatusCode.OK, html)
                "/a-cidade-e-as-serras-eca-de-queiros/" -> respond(
                    """<div class="wp-block-buttons"><a class="wp-block-button__link" href="https://projectoadamastor.org/download/a-cidade-e-as-serras-eca-de-queiros/?wpdmdl=3017">EPUB</a>
                       <a class="wp-block-button__link" href="https://projectoadamastor.org/download/a-cidade-e-as-serras-eca-de-queiros-mobi/?wpdmdl=3019">MOBI</a></div>""",
                    HttpStatusCode.OK, html,
                )
                "/download/a-cidade-e-as-serras-eca-de-queiros/" -> respond(EPUB, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/epub+zip"))
                else -> error("Unexpected request ${request.url}")
            }
        }) { install(HttpTimeout) },
    )

    @Test
    fun itListsTheCatalogueOnceAndSearchesItHere() = runTest {
        val all = catalog.search("pt", "", 1)
        assertEquals(listOf("A Cidade e as Serras", "A Confissão de Lúcio", "Sonetos"), all.books.map { it.title })
        assertEquals(3, all.total)
        assertFalse(all.hasMore)
        val cidade = all.books.first()
        assertEquals("a-cidade-e-as-serras-eca-de-queiros", cidade.id)
        assertEquals(listOf("Eça de Queirós"), cidade.authors)
        assertEquals(listOf("Ficção", "Romance"), cidade.subjects)
        assertEquals("CC-BY-SA-4.0", cidade.license)
        assertEquals("https://projectoadamastor.org/a-cidade-e-as-serras-eca-de-queiros/", cidade.pageUrl)
        assertEquals("https://projectoadamastor.org/wp-content/uploads/cache/2019/07/A-Cidade-e-as-Serras_Capa/645702042.jpg", cidade.coverUrl)

        assertEquals(listOf("A Confissão de Lúcio"), catalog.search("pt", "sá-carneiro", 1).books.map { it.title })
        assertEquals(listOf("Sonetos"), catalog.search("pt", "poesia", 1).books.map { it.title }, "genres are searched too")
        assertEquals(1, requests.size, "the catalogue is fetched once")
    }

    @Test
    fun aBookIsReadFromTheEpubItsPageLinksWithoutTheEditionsPages() = runTest {
        val cidade = catalog.search("pt", "cidade", 1).books.single()
        assertEquals("Advertência\nDesde a página 241, as provas não foram revistas.\n\nI\nO meu amigo Jacinto nasceu num palácio.", catalog.sample(cidade))
        assertEquals("3017", requests.last().url.parameters["wpdmdl"], "the ePUB, not the Kindle file")
        catalog.text(cidade)
        assertEquals(3, requests.size, "the text judged is the text added")
    }

    private companion object {
        fun item(slug: String, collection: String, genres: String, alt: String, cover: String) =
            """<div class="fg-item fg-type-iframe fg-loading"><figure class="fg-item-inner"><a href="https://projectoadamastor.org/$slug/" target="_blank" data-caption-title="$collection" data-caption-desc="$genres" data-attachment-id="3016" data-type="iframe" class="fg-thumb"><span class="fg-image-wrap"><img src="$cover" alt="$alt" title="$collection" width="400" height="638" class="skip-lazy fg-image" decoding="async" loading="lazy"></span><span class="fg-image-overlay"></span></a><figcaption class="fg-caption"><div class="fg-caption-inner"><div class="fg-caption-title">$collection</div><div class="fg-caption-desc">$genres</div></div></figcaption></figure><div class="fg-loader"></div></div>"""

        val CATALOGUE = """<html><body><nav><a href="https://projectoadamastor.org/blog/">Blog</a></nav><div class="foogallery">""" +
            item("a-cidade-e-as-serras-eca-de-queiros", "Colecção Clássicos, n.º 44", "Ficção, Romance", "Capa de «A Cidade e as Serras», de Eça de Queirós", "https://projectoadamastor.org/wp-content/uploads/cache/2019/07/A-Cidade-e-as-Serras_Capa/645702042.jpg") +
            item("a-confissao-de-lucio-mario-de-sa-carneiro", "Colecção Clássicos, n.º 8", "Ficção, Novela", "Capa de «A Confissão de Lúcio», de Mário de Sá-Carneiro", "https://projectoadamastor.org/c.jpg") +
            item("sonetos-antero-de-quental", "Colecção Clássicos, n.º 43", "Poesia", "Capa de «Sonetos», de Antero de Quental", "https://projectoadamastor.org/s.jpg") +
            "</div></body></html>"

        val EPUB: ByteArray = run {
            val documents = listOf(
                "capa.html" to "<img src=\"capa.jpg\"/>",
                "titulo.html" to "<h1>A Cidade e as Serras</h1><p>Eça de Queirós</p>",
                "ficha_tecnica.html" to "<h1>Ficha Técnica</h1><p>Este trabalho foi licenciado com uma Licença Creative Commons.</p>",
                "indice.html" to "<p>I</p><p>II</p>",
                "advertencia.html" to "<h1>Advertência</h1><p>Desde a página 241, as provas não foram revistas.</p>",
                "capitulo_1.html" to "<h1>I</h1><p>O meu amigo Jacinto nasceu num palácio.</p>",
                "contracapa.html" to "<p>Eça de Queirós. Nascimento: 1845.</p>",
            )
            val out = ByteArrayOutputStream()
            ZipOutputStream(out).use { zip ->
                fun put(name: String, text: String) { zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() }
                put("mimetype", "application/epub+zip")
                put("META-INF/container.xml", """<container><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>""")
                val manifest = documents.joinToString("") { (id, _) -> """<item id="$id" href="Text/$id" media-type="application/xhtml+xml"/>""" }
                val spine = documents.joinToString("") { (id, _) -> """<itemref idref="$id"/>""" }
                put("OEBPS/content.opf", """<package><manifest>$manifest</manifest><spine>$spine</spine></package>""")
                for ((id, body) in documents) put("OEBPS/Text/$id", """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"><body>$body</body></html>""")
            }
            out.toByteArray()
        }
    }
}
