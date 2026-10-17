package com.tayra.languages.core.data

import com.tayra.languages.core.data.news.WikipediaArticles
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

/** A language's Wikipedia: yesterday's most read articles, each with its introduction, a search, and an article whole. */
class WikipediaArticlesTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val clock = object : Clock { override fun now(): Instant = LocalDate(2026, 10, 17).atStartOfDayIn(TimeZone.UTC) }

    private fun wiki(request: HttpRequestData): String {
        val q = request.url.parameters
        return when {
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

    private val articles = WikipediaArticles(HttpClient(MockEngine { request ->
        requests += request
        val body = wiki(request)
        respond(body, if ("not_found" in body) HttpStatusCode.NotFound else HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(HttpTimeout) }, clock)

    @Test
    fun theMostReadArticlesComeWithTheirIntroductions() = runTest {
        val page = articles.mostRead("pt", 1)
        // The main page, other namespaces under any of their names, lists of meanings, a title that leads to one already listed,
        // and adult topics (a performer, a sexual practice, a film of an adult genre) go.
        assertEquals(listOf("Gato", "Star Wars: Episódio I"), page.articles.map { it.title })
        val gato = page.articles.first()
        assertEquals("O gato é um mamífero carnívoro.\nÉ muito popular.", gato.feedText)
        assertEquals(170365, gato.length)
        assertEquals("https://pt.wikipedia.org/wiki/Gato", gato.url)
        assertTrue(gato.source.textInFeed)
        // Yesterday's figures were not out yet, so the day before's were read.
        assertEquals(listOf("/api/rest_v1/metrics/pageviews/top/pt.wikipedia/all-access/2026/10/16", "/api/rest_v1/metrics/pageviews/top/pt.wikipedia/all-access/2026/10/15"),
            requests.filter { it.url.host == "wikimedia.org" }.map { it.url.encodedPath })
        assertEquals(false, page.hasMore)
    }

    @Test
    fun aSearchAndAWholeArticle() = runTest {
        val found = articles.search("pt", "gato", 2)
        // A search finds what it is asked for, so Wikidata is not asked about it.
        assertEquals(0, requests.count { it.url.host == "www.wikidata.org" })
        assertEquals("20", requests.single { it.url.parameters["list"] == "search" }.url.parameters["sroffset"])
        assertEquals(true, found.hasMore)
        assertEquals("O gato é um mamífero.\nNome\nA palavra vem do latim.", articles.wholeText(found.articles.single()))
    }

    @Test
    fun cantoneseHasAWikiOfItsOwnName() {
        assertEquals("zh-yue", WikipediaArticles.wikiCode("yue"))
        assertEquals("pt", WikipediaArticles.wikiCode("pt"))
    }
}
