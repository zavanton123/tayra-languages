package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.WikisourceCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** Wikisource's works: the most read literature of a language, a search, a first chapter to judge them by, and the whole work to read. */
class WikisourceCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val clock = object : Clock { override fun now(): Instant = LocalDate(2026, 10, 16).atStartOfDayIn(TimeZone.UTC) }

    private fun catalog(answer: (HttpRequestData) -> Any) = WikisourceCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            when (val body = answer(request)) {
                is ByteArray -> respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/epub+zip"))
                else -> respond(body.toString(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }) { install(HttpTimeout) },
        clock,
    )

    private val HttpRequestData.query: Map<String, String> get() = url.parameters.names().associateWith { url.parameters[it].orEmpty() }

    /** A Portuguese Wikisource and Wikidata that know two works, an anthem, a list of versions and the main page. */
    private fun wiki(request: HttpRequestData): Any {
        val q = request.query
        return when {
            request.url.host == "wikimedia.org" -> """{"items": [{"articles": [
                {"article": "Página_principal"}, {"article": "Dom_Casmurro"}, {"article": "Autor:Machado_de_Assis"}, {"article": "Dom_Casmurro/I"},
                {"article": "Hino_Nacional"}, {"article": "Mensagem_(Fernando_Pessoa)"}, {"article": "Os_Lusíadas"}]}]}"""
            request.url.host == "www.wikidata.org" && q["props"] == "claims" -> """{"entities": {
                "Q1": {"claims": {"P31": [{"mainsnak": {"datavalue": {"value": {"id": "Q8261"}}}}], "P50": [{"mainsnak": {"datavalue": {"value": {"id": "Q10"}}}}]}},
                "Q2": {"claims": {"P31": [{"mainsnak": {"datavalue": {"value": {"id": "Q484692"}}}}]}},
                "Q3": {"claims": {"P31": [{"mainsnak": {"datavalue": {"value": {"id": "Q3331189"}}}}], "P629": [{"mainsnak": {"datavalue": {"value": {"id": "Q30"}}}}]}},
                "Q30": {"claims": {"P50": [{"mainsnak": {"datavalue": {"value": {"id": "Q11"}}}}]}}}}"""
            request.url.host == "www.wikidata.org" -> """{"entities": {"Q10": {"labels": {"pt": {"value": "Machado de Assis"}}}, "Q11": {"labels": {"en": {"value": "Fernando Pessoa"}}}}}"""
            q["meta"] == "siteinfo" -> """{"query": {"general": {"mainpage": "Página principal"}}}"""
            q["prop"] == "pageprops" -> """{"query": {
                "normalized": [{"from": "Dom_Casmurro", "to": "Dom Casmurro"}],
                "redirects": [{"from": "Mensagem", "to": "Mensagem (Fernando Pessoa)"}],
                "pages": [
                  {"title": "Dom Casmurro", "pageprops": {"wikibase_item": "Q1"}},
                  {"title": "Hino Nacional", "pageprops": {"wikibase_item": "Q2"}},
                  {"title": "Mensagem (Fernando Pessoa)", "pageprops": {"wikibase_item": "Q3"}},
                  {"title": "Os Lusíadas", "pageprops": {"disambiguation": ""}},
                  {"title": "Ressurreição (Machado de Assis)"}]}}"""
            q["list"] == "search" -> """{"continue": {"sroffset": 40}, "query": {"searchinfo": {"totalhits": 3015},
                "search": [{"title": "Dom Casmurro/I"}, {"title": "Ressurreição (Machado de Assis)"}, {"title": "Dom Casmurro"}, {"title": "Mensagem"}]}}"""
            q["action"] == "parse" && q["page"] == "Dom Casmurro" -> """{"parse": {"text": "<div class=\"ws-noexport\">Índice</div><p><a href=\"/wiki/Autor:Machado_de_Assis\">Machado</a></p><ul><li><a href=\"/wiki/Dom_Casmurro/I\">I</a></li><li><a href=\"/wiki/Dom_Casmurro/II\">II</a></li></ul>"}}"""
            q["action"] == "parse" && q["page"] == "Dom Casmurro/I" -> """{"parse": {"text": "<div class=\"ws-noexport header\"><div>← Dom Casmurro</div><span>Do livro →</span></div><div id=\"ws-data\" class=\"ws-noexport\" style=\"display:none\"><span id=\"ws-title\">Dom Casmurro</span></div><p>Uma noite destas, vindo da cidade.</p>"}}"""
            request.url.host == "ws-export.wmcloud.org" -> epub(
                "title" to "<p>Dom Casmurro, Machado de Assis, 1899</p>",
                "c0_Dom_Casmurro" to "<p>DOM CASMURRO</p><p><a href=\"c1.xhtml\">Do titulo</a> — <a href=\"c2.xhtml\">Do livro</a> — <a href=\"c3.xhtml\">A denuncia</a></p>",
                "c1_Dom_Casmurro_I" to "<p>Uma noite destas, vindo da cidade para o Engenho Novo, encontrei no trem da Central um rapaz aqui do bairro, que eu conheço de vista e de chapéu. Cumprimentou-me, sentou-se ao pé de mim, falou da lua e dos ministros.</p>",
                "about" to "<p>Este eBook foi gerado a partir do Wikisource.</p>",
            )
            else -> error("Unexpected request ${request.url}")
        }
    }

    @Test
    fun withoutASearchTheListIsLastMonthsMostReadLiterature() = runTest {
        val page = catalog(::wiki).search("pt", "", 1)
        assertEquals(listOf("Dom Casmurro", "Mensagem"), page.books.map { it.title })
        val (dom, mensagem) = page.books
        assertEquals(listOf("Machado de Assis"), dom.authors)
        // An edition takes its author from the work it is an edition of, which names the work after him on the wiki.
        assertEquals(listOf("Fernando Pessoa"), mensagem.authors)
        assertEquals("https://pt.wikisource.org/wiki/Mensagem_(Fernando_Pessoa)", mensagem.pageUrl)
        assertEquals("https://ws-export.wmcloud.org/?format=epub-3&lang=pt&page=Dom_Casmurro", dom.downloadUrl)
        assertEquals(false, page.hasMore)
        assertEquals("/api/rest_v1/metrics/pageviews/top/pt.wikisource/all-access/2026/09/all-days", requests.first { it.url.host == "wikimedia.org" }.url.encodedPath)
        // Wikimedia asks to be told who is asking; every request says so and lets the web build read the answer.
        assertTrue(requests.all { "github.com/zavanton123/tayra-languages" in it.headers[HttpHeaders.UserAgent].orEmpty() })
        assertTrue(requests.filter { it.url.encodedPath.endsWith("api.php") }.all { it.url.parameters["origin"] == "*" })
    }

    @Test
    fun aSearchFindsWorksThroughTheirChapters() = runTest {
        val page = catalog(::wiki).search("pt", "machado", 2)
        val search = requests.single { it.url.parameters["list"] == "search" }
        assertEquals("machado", search.url.parameters["srsearch"])
        assertEquals("40", search.url.parameters["sroffset"])
        // A chapter stands for its work, once; a work without Wikidata has no author but is listed.
        assertEquals(listOf("Dom Casmurro", "Ressurreição (Machado de Assis)", "Mensagem"), page.books.map { it.title })
        assertEquals(3015, page.total)
        assertTrue(page.hasMore)
    }

    @Test
    fun aWorkIsJudgedByItsFirstChapterWithoutTheWikisHeader() = runTest {
        val dom = catalog(::wiki).search("pt", "", 1).books.first()
        assertEquals("Uma noite destas, vindo da cidade.", catalog(::wiki).sample(dom))
    }

    @Test
    fun anAddedWorkIsItsTextWithoutTheExportsOwnPages() = runTest {
        val dom = catalog(::wiki).search("pt", "", 1).books.first()
        val text = catalog(::wiki).text(dom)
        assertTrue(text.startsWith("Uma noite destas"), text)
        assertFalse("Wikisource" in text || "DOM CASMURRO" in text || "1899" in text, text)
    }

    @Test
    fun aWorksOwnPageIsKeptUnlessItOnlyListsTheChapters() {
        val toc = "<p>INDICE</p><p><a href=\"a\">Capitulo I</a> <a href=\"b\">Do livro</a> <a href=\"c\">A denuncia</a></p>"
        val preface = "<p>Ao leitor. Este livro conta a historia de um homem que viveu longe da sua terra e voltou velho.</p><p><a href=\"a\">I</a></p>"
        assertFalse(WikisourceCatalog.isWorkText("c0_Dom", toc, listOf("title", "c0_Dom", "c1_Dom_I", "about")))
        assertTrue(WikisourceCatalog.isWorkText("c0_Dom", preface, listOf("title", "c0_Dom", "c1_Dom_I", "about")))
        // A work of one page is all in it, links or not.
        assertTrue(WikisourceCatalog.isWorkText("c0_Poema", toc, listOf("title", "c0_Poema", "about")))
        assertFalse(WikisourceCatalog.isWorkText("about", preface, listOf("title", "c0_Poema", "about")))
    }

    @Test
    fun aWorkNamedAfterItsAuthorLosesTheName() {
        assertEquals("Mensagem", WikisourceCatalog.withoutAuthor("Mensagem (Fernando Pessoa)", listOf("Fernando Pessoa")))
        assertEquals("Памятник", WikisourceCatalog.withoutAuthor("Памятник (Державин)", listOf("Гавриил Романович Державин")))
        assertEquals("И. И. Пущину (Мой первый друг — Пушкин)", WikisourceCatalog.withoutAuthor("И. И. Пущину (Мой первый друг — Пушкин)", listOf("Александр Сергеевич Пушкин")))
        assertEquals("Contos (1899)", WikisourceCatalog.withoutAuthor("Contos (1899)", emptyList()))
    }

    private fun epub(vararg documents: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, text: String) { zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() }
            put("mimetype", "application/epub+zip")
            put("META-INF/container.xml", """<container><rootfiles><rootfile full-path="OPS/content.opf"/></rootfiles></container>""")
            val manifest = documents.joinToString("") { (id, _) -> """<item id="$id" href="$id.xhtml" media-type="application/xhtml+xml"/>""" }
            val spine = documents.joinToString("") { (id, _) -> """<itemref idref="$id" linear="yes"/>""" }
            put("OPS/content.opf", """<package><metadata><dc:title>Dom Casmurro</dc:title></metadata><manifest>$manifest</manifest><spine>$spine</spine></package>""")
            for ((id, body) in documents) put("OPS/$id.xhtml", """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"><body>$body</body></html>""")
        }
        return out.toByteArray()
    }
}
