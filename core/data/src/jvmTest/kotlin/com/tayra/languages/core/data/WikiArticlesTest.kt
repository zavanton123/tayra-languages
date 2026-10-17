package com.tayra.languages.core.data

import com.tayra.languages.core.data.news.WikiArticles
import com.tayra.languages.core.domain.news.Wiki
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** A language's wikis: yesterday's most read articles, each with its introduction, a search, an article whole, and the Klexikons' articles at random. */
class WikiArticlesTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val clock = object : Clock { override fun now(): Instant = LocalDate(2026, 10, 17).atStartOfDayIn(TimeZone.UTC) }

    private fun wiki(request: HttpRequestData): String {
        val q = request.url.parameters
        return when {
            request.url.host.endsWith("zum.de") && q["list"] == "random" -> """{"query": {"random": [{"id": 1, "ns": 0, "title": "Hund"}, {"id": 2, "ns": 0, "title": "Berlin"}]}}"""
            request.url.host.endsWith("zum.de") && q["prop"] == "extracts|info|pageprops" -> """{"query": {"pages": [
                {"title": "Hund", "length": 1217, "extract": "Hunde sind Säuge-Tiere.\nEin männlicher Hund wird Rüde genannt."},
                {"title": "Berlin", "length": 1603, "extract": "Berlin ist die Hauptstadt von Deutschland."}]}}"""
            request.url.host.endsWith("zum.de") && q["prop"] == "extracts" -> """{"query": {"pages": [{"title": "Hund", "extract": "Hunde sind Säuge-Tiere.\n\n\n== Aussehen ==\nHunde haben ein Fell."}]}}"""
            request.url.host == "wikimedia.org" && "pt.wikivoyage" in request.url.encodedPath -> """{"items": [{"articles": [{"article": "Ficheiro:Flag_of_Oman.svg"}, {"article": "Gato"}]}]}"""
            request.url.host == "wikimedia.org" && request.url.encodedPath.endsWith("/2026/10/16") -> """{"type": "not_found"}"""
            request.url.host == "wikimedia.org" -> """{"items": [{"articles": [
                {"article": "Wikipédia:Página_principal"}, {"article": "Especial:Pesquisar"}, {"article": "Gato"}, {"article": "-"},
                {"article": "Mercúrio_(desambiguação)"}, {"article": "Busca:Coisa"}, {"article": "Star_Wars:_Episódio_I"}, {"article": "Felis_catus"}, {"article": "Atriz_X"}, {"article": "Masturbação"}, {"article": "Filme_Y"}]}]}"""
            request.url.host == "www.wikidata.org" -> """{"entities": {
                "Q1": {"claims": {"P31": [{"mainsnak": {"datavalue": {"value": {"id": "Q146"}}}}]}},
                "Q2": {"claims": {"P31": [{"mainsnak": {"datavalue": {"value": {"id": "Q5"}}}}], "P106": [{"mainsnak": {"datavalue": {"value": {"id": "Q488111"}}}}]}},
                "Q3": {"claims": {"P31": [{"mainsnak": {"datavalue": {"value": {"id": "Q782623"}}}}], "P279": [{"mainsnak": {"datavalue": {"value": {"id": "Q608"}}}}]}},
                "Q4": {"claims": {"P31": [{"mainsnak": {"datavalue": {"value": {"id": "Q11424"}}}}], "P136": [{"mainsnak": {"datavalue": {"value": {"id": "Q185529"}}}}]}}}}"""
            q["meta"] == "siteinfo" -> """{"query": {"general": {"mainpage": "Wikipédia:Página principal"},
                "namespaces": {"-1": {"name": "Especial", "canonical": "Special"}, "0": {"name": ""}, "4": {"name": "Wikipédia", "canonical": "Project"}},
                "namespacealiases": [{"id": -1, "alias": "Busca"}]}}"""
            q["list"] == "search" -> """{"continue": {"sroffset": 20}, "query": {"search": [{"title": "Gato"}]}}"""
            q["prop"] == "extracts|info|pageprops" -> """{"query": {
                "normalized": [{"from": "Felis_catus", "to": "Felis catus"}], "redirects": [{"from": "Felis catus", "to": "Gato"}],
                "pages": [
                  {"title": "Gato", "length": 170365, "pageprops": {"wikibase_item": "Q1"}, "extract": "O gato é um mamífero carnívoro.\n\nÉ muito popular."},
                  {"title": "Atriz X", "length": 5000, "pageprops": {"wikibase_item": "Q2"}, "extract": "Uma atriz."},
                  {"title": "Masturbação", "length": 9000, "pageprops": {"wikibase_item": "Q3"}, "extract": "Um tema."},
                  {"title": "Filme Y", "length": 4000, "pageprops": {"wikibase_item": "Q4"}, "extract": "Um filme."},
                  {"title": "Mercúrio (desambiguação)", "length": 855, "pageprops": {"disambiguation": ""}, "extract": "Mercúrio pode referir-se a:"},
                  {"title": "Star Wars: Episódio I", "length": 41000, "extract": "Um filme de 1999."}]}}"""
            q["prop"] == "extracts" -> """{"query": {"pages": [{"title": "Gato", "extract": "O gato é um mamífero.\n\n\n== Nome ==\nA palavra vem do latim.\n\n\n== Ver também ==\nGatos na cultura popular\nLista de raças de gatos\n\n\n== Referências ==\n\n\n=== Bibliografia ===\n\n\n== Ligações externas ==\n«Ficha sobre o gato»"}]}}"""
            else -> error("Unexpected ${request.url}")
        }
    }

    private val articles = WikiArticles(HttpClient(MockEngine { request ->
        requests += request
        val body = wiki(request)
        respond(body, if ("not_found" in body) HttpStatusCode.NotFound else HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(HttpTimeout) }, clock)

    @Test
    fun theMostReadArticlesComeWithTheirIntroductions() = runTest {
        val page = articles.mostRead(Wiki.WIKIPEDIA, "pt", 1)
        // The main page, other namespaces under any of their names, lists of meanings, a title that leads to one already listed,
        // and adult topics (a performer, a sexual practice, a film of an adult genre) go.
        assertEquals(listOf("Gato", "Star Wars: Episódio I"), page.articles.map { it.title })
        val gato = page.articles.first()
        assertEquals("O gato é um mamífero carnívoro.\nÉ muito popular.", gato.feedText)
        assertEquals(170365, gato.length)
        assertEquals("https://pt.wikipedia.org/wiki/Gato", gato.url)
        assertTrue(gato.source.textInFeed)
        assertEquals("Wikipedia", gato.source.name)
        // Yesterday's figures were not out yet, so the day before's were read.
        assertEquals(listOf("/api/rest_v1/metrics/pageviews/top/pt.wikipedia/all-access/2026/10/16", "/api/rest_v1/metrics/pageviews/top/pt.wikipedia/all-access/2026/10/15"),
            requests.filter { it.url.host == "wikimedia.org" }.map { it.url.encodedPath })
        assertEquals(false, page.hasMore)
    }

    @Test
    fun aSearchAndAWholeArticle() = runTest {
        val found = articles.search(Wiki.WIKIPEDIA, "pt", "gato", 2)
        // A search finds what it is asked for, so Wikidata is not asked about it.
        assertEquals(0, requests.count { it.url.host == "www.wikidata.org" })
        assertEquals("20", requests.single { it.url.parameters["list"] == "search" }.url.parameters["sroffset"])
        assertEquals(true, found.hasMore)
        assertEquals("O gato é um mamífero.\nNome\nA palavra vem do latim.", articles.wholeText(found.articles.single()))
    }

    @Test
    fun theOtherWikimediaWikisAreCountedUnderTheirOwnNames() = runTest {
        val page = articles.mostRead(Wiki.WIKIVOYAGE, "pt", 1)
        assertEquals(listOf("/api/rest_v1/metrics/pageviews/top/pt.wikivoyage/all-access/2026/10/16"), requests.filter { it.url.host == "wikimedia.org" }.map { it.url.encodedPath })
        assertEquals("pt.wikivoyage.org", requests.first().url.host)
        assertEquals("/w/api.php", requests.first().url.encodedPath)
        val gato = page.articles.single()
        assertEquals("https://pt.wikivoyage.org/wiki/Gato", gato.url)
        assertEquals("Wikivoyage", gato.source.name)
    }

    @Test
    fun theKlexikonsArticlesComeAtRandomFromTheirOwnApi() = runTest {
        val page = articles.mostRead(Wiki.MINIKLEXIKON, "de", 3)
        assertEquals(setOf("miniklexikon.zum.de"), requests.map { it.url.host }.toSet(), "no page views are asked for")
        assertEquals("/api.php", requests.first().url.encodedPath)
        assertEquals(listOf("Hund", "Berlin"), page.articles.map { it.title })
        assertTrue(page.hasMore, "there are always more at random")
        val hund = page.articles.first()
        assertEquals("https://miniklexikon.zum.de/wiki/Hund", hund.url)
        assertEquals("MiniKlexikon", hund.source.name)
        assertEquals("Hunde sind Säuge-Tiere.\nAussehen\nHunde haben ein Fell.", articles.wholeText(hund))
        assertEquals("/api.php", requests.last().url.encodedPath)
    }

    @Test
    fun eachWikiHasItsAddress() {
        assertEquals("https://zh-yue.wikipedia.org", WikiArticles.host(Wiki.WIKIPEDIA, "yue"), "Cantonese has a Wikipedia of its own name")
        assertEquals("https://pt.wikipedia.org", WikiArticles.host(Wiki.WIKIPEDIA, "pt"))
        assertEquals("https://simple.wikipedia.org", WikiArticles.host(Wiki.SIMPLE_WIKIPEDIA, "en"))
        assertEquals("https://klexikon.zum.de", WikiArticles.host(Wiki.KLEXIKON, "de"))
        assertEquals("https://de.wikibooks.org", WikiArticles.host(Wiki.WIKIBOOKS, "de"))
    }
}
