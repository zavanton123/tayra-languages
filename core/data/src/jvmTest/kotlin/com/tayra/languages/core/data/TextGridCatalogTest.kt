package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.TextGridCatalog
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
import kotlin.test.assertTrue

/** TextGrid's Digitale Bibliothek: the Grimms' tales unsearched, a search by title and author, and texts without their opening lines. */
class TextGridCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val catalog = TextGridCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            when {
                request.url.encodedPath.endsWith("/tgsearch-public/search") -> respond(RESULTS, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/xml"))
                request.url.encodedPath.endsWith("/aggregator/text/textgrid:qn07.0") -> respond(
                    "Franz Kafka\nDie Verwandlung\nI\nI\nAls Gregor Samsa eines Morgens aus unruhigen Träumen erwachte.\n»Was ist mit mir geschehen?« dachte er.\n",
                    HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain;charset=UTF-8"),
                )
                else -> error("Unexpected request ${request.url}")
            }
        }) { install(HttpTimeout) },
    )

    @Test
    fun withoutASearchItListsTheGrimmsTalesWithoutBiographiesAndNotes() = runTest {
        val page = catalog.search("de", "", 2)
        val q = requests.single().url.parameters
        assertEquals("""edition.agent.value:"Grimm, Jacob und Wilhelm"""", q["q"])
        assertEquals(listOf("format:text/xml", "project.id:TGPR-372fe6dc-57f2-6cd4-01b5-2c4bbefcfd3c"), q.getAll("filter"))
        assertEquals("20", q["start"])
        assertEquals(980, page.total)
        assertTrue(page.hasMore)
        assertEquals(listOf("textgrid:qn07.0", "textgrid:nz3f.0"), page.books.map { it.id })

        val kafka = page.books.first()
        assertEquals("Die Verwandlung", kafka.title)
        assertEquals(listOf("Franz Kafka"), kafka.authors)
        assertEquals("CC-BY-3.0", kafka.license)
        assertEquals("https://textgridrep.org/browse/textgrid:qn07.0", kafka.pageUrl)
        assertEquals("https://textgridlab.org/1.0/aggregator/text/textgrid:qn07.0", kafka.downloadUrl)
        assertEquals(listOf("Jacob und Wilhelm Grimm"), page.books[1].authors)
    }

    @Test
    fun aSearchMatchesEveryWordInTitlesOrAuthors() = runTest {
        catalog.search("de", "Kafka  Verwandlung?", 1)
        assertEquals("(title:Kafka* OR edition.agent.value:Kafka*) AND (title:Verwandlung* OR edition.agent.value:Verwandlung*)", requests.single().url.parameters["q"])
    }

    @Test
    fun aTextLosesItsAuthorTitleAndRepeatedHeading() = runTest {
        val kafka = catalog.search("de", "", 1).books.first()
        val expected = "I\nAls Gregor Samsa eines Morgens aus unruhigen Träumen erwachte.\n»Was ist mit mir geschehen?« dachte er."
        assertEquals(expected, catalog.text(kafka))
        assertEquals(expected, catalog.sample(kafka))
        assertEquals(1, requests.count { "aggregator" in it.url.encodedPath }, "the text judged is the text added")
    }

    private companion object {
        fun result(author: String, title: String, uri: String) =
            """<tgs:result author="$author" score="13.8"><object><generic><provided><title>$title</title><format>text/xml</format></provided><generated><textgridUri extRef="">$uri</textgridUri><extent>6486</extent></generated></generic></object></tgs:result>"""

        val RESULTS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><tgs:response xmlns:tgs="http://www.textgrid.info/namespaces/middleware/tgsearch" hits="980" session="deprecated" limit="20">""" +
            result("Kafka, Franz", "Die Verwandlung", "textgrid:qn07.0") +
            result("Grimm, Jacob und Wilhelm", "26. Rotkäppchen", "textgrid:nz3f.0") +
            result("Grimm, Jacob und Wilhelm", "Biographie: Grimm, Jacob und Wilhelm", "textgrid:nz75.0") +
            result("Grimm, Jacob und Wilhelm", "Vorbemerkung von Herman Grimm", "textgrid:nzkp.0") +
            "</tgs:response>"
    }
}
