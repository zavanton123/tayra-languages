package com.tayra.languages.core.data.network

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.ExampleSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LeipzigExamplesProviderTest {

    private fun resource(name: String) = javaClass.getResource("/leipzig/$name")!!.readText()

    private val portuguese = Language(name = "Portuguese")

    /** A corpus whose words have the given sentences, served as the service does: 404 for an unknown word or past the end. */
    private class FakeService(private val sentences: Map<String, List<String>>, private val corpora: String) {
        val requests = mutableListOf<HttpRequestData>()

        val paths: List<String> get() = requests.map { it.url.encodedPath }

        fun handle(scope: MockRequestHandleScope, request: HttpRequestData): HttpResponseData {
            requests += request
            val path = request.url.encodedPath
            if (path.endsWith("/corpora")) return scope.json(corpora)
            val word = request.url.segments.last()
            val all = sentences[word] ?: return scope.respond("", HttpStatusCode.NotFound)
            val offset = request.url.parameters["offset"]?.toInt() ?: 0
            val limit = request.url.parameters["limit"]?.toInt() ?: 10
            if (offset >= all.size) return scope.respond("", HttpStatusCode.NotFound)
            val body = buildJsonObject {
                put("count", all.size)
                put("sentences", buildJsonArray {
                    all.drop(offset).take(limit).forEachIndexed { i, text ->
                        add(buildJsonObject {
                            put("id", (offset + i).toString())
                            put("sentence", text)
                            putJsonObject("source") { put("id", "1"); put("url", "https://news.test/$word/${offset + i}"); put("date", "2013-01-${(offset + i) % 28 + 1}".padDate()) }
                        })
                    }
                })
            }
            return scope.json(body.toString())
        }

        private fun String.padDate() = split('-').let { (y, m, d) -> "$y-$m-${d.padStart(2, '0')}" }

        private fun MockRequestHandleScope.json(body: String) = respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
    }

    private fun provider(service: FakeService, random: Random = Random(7)) =
        LeipzigExamplesProvider(HttpClient(MockEngine { service.handle(this, it) }), baseUrl = "https://leipzig.test/ws", random = random)

    private fun query(text: String, sort: ExampleSort = ExampleSort.SHORTEST, minWords: Int? = null, maxWords: Int? = null, limit: Int = 10) =
        ExampleSearchQuery(text, portuguese, "en", minWords = minWords, maxWords = maxWords, sort = sort, limit = limit, source = ExampleSource.LEIPZIG)

    @Test
    fun parsesTheServicesSentences() {
        val fetched = provider(FakeService(emptyMap(), "[]")).extract(resource("sentences.json"))
        assertEquals(33156, fetched.count)
        assertEquals(5, fetched.sentences.size)
        assertEquals(
            ExampleSentence(
                text = "01 – Triângulo jogou muito.",
                translation = null,
                audioUrl = null,
                sourceUrl = "http://jmonline.com.br/novo/?noticias,32,BICO+DA+CHANCA,81803",
                date = "2013-06-12",
            ),
            fetched.sentences.first(),
        )
    }

    @Test
    fun choosesNewsThenWikipediaThenWebWithoutACountryAndTheLargestAndNewest() {
        val corpora = provider(FakeService(emptyMap(), "[]")).parseCorpora(resource("corpora.json"))
        assertEquals(104, corpora.size)
        val chosen = listOf("por", "deu", "eng", "ara", "ita", "ces", "lat", "afr", "xho", "zul", "kck", "jpn")
            .associateWith { LeipzigExamplesProvider.chooseCorpus(corpora, it) }
        assertEquals(
            mapOf(
                "por" to "por_news_2013_1M",
                "deu" to "deu_news_2012_3M",
                "eng" to "eng_news_2013_3M",
                "ara" to "ara_newscrawl_2013_1M",
                "ita" to "ita_news_2012_1M",
                "ces" to "ces_news_2013_1M",
                "lat" to "lat_wikipedia_2012_100K",
                "afr" to "afr_wikipedia_2018_300K",
                "xho" to "xho-za_web_2018_30K",
                "zul" to "zul-za_web_2018_30K",
                "kck" to "kck_community_2019",
                "jpn" to null,
            ),
            chosen,
        )
    }

    @Test
    fun onlyLanguagesWithACorpusOfferTheSourceAndTheListIsFetchedOnce() = runTest {
        val service = FakeService(emptyMap(), resource("corpora.json"))
        val provider = provider(service)
        assertEquals(listOf(ExampleSource.LEIPZIG), provider.sources(portuguese).map { it.source })
        assertEquals(emptyList(), provider.sources(Language(name = "Japanese")))
        assertEquals(1, service.paths.count { it.endsWith("/corpora") })
    }

    @Test
    fun anUnknownWordAndAPhraseFindNothing() = runTest {
        val service = FakeService(emptyMap(), resource("corpora.json"))
        val provider = provider(service)
        val unknown = provider.search(query("xyzzy"))
        assertEquals(emptyList(), unknown.sentences)
        assertEquals(0, unknown.total)
        assertNull(unknown.nextPage)
        // The lower-case word found too few, so its capitalised form was tried as well.
        assertEquals(listOf("/ws/sentences/por_news_2013_1M/sentences/xyzzy", "/ws/sentences/por_news_2013_1M/sentences/Xyzzy"), service.paths.drop(1))

        service.requests.clear()
        assertEquals(emptyList(), provider.search(query("muito bem")).sentences)
        assertEquals(emptyList(), service.requests)
    }

    @Test
    fun theWordGoesIntoOnePathSegment() = runTest {
        val service = FakeService(emptyMap(), resource("corpora.json"))
        provider(service).search(query("a/b?c#d"))
        assertEquals("/ws/sentences/por_news_2013_1M/sentences/a%2Fb%3Fc%23d", service.paths[1])
    }

    @Test
    fun theWordCountFilterFillsPagesInOrderAndThePagesContinueWhereTheLastStopped() = runTest {
        // Sentence i has i % 6 + 1 words; a filter of at most 2 words keeps a third of them.
        val sentences = (0 until 60).map { i -> (List(i % 6) { "pal$it" } + "casa$i").joinToString(" ") }
        val service = FakeService(mapOf("casa" to sentences), resource("corpora.json"))
        val provider = provider(service)
        val first = provider.search(query("casa", sort = ExampleSort.LONGEST, maxWords = 2, limit = 4))
        assertEquals(listOf("pal0 casa1", "pal0 casa7", "casa0", "casa6"), first.sentences.map { it.text })
        assertEquals(60, first.total)
        val token = first.nextPage!!
        assertTrue(token.startsWith(LEIPZIG_PAGE_PREFIX), token)
        // Plenty of sentences: no capitalised look-up, and one request filled the page.
        assertEquals(1, service.paths.count { "/sentences/" in it })

        val second = provider.nextPage(token, "en")
        assertEquals(listOf("pal0 casa13", "pal0 casa19", "casa12", "casa18"), second.sentences.map { it.text })
        assertEquals("8", service.requests.last().url.parameters["offset"])

        var page = second
        val seen = (first.sentences + second.sentences).map { it.text }.toMutableList()
        while (page.nextPage != null) {
            page = provider.nextPage(page.nextPage!!, "en")
            seen += page.sentences.map { it.text }
        }
        assertEquals(sentences.filter { LeipzigExamplesProvider.wordCount(it) <= 2 }.toSet(), seen.toSet())
        assertEquals(seen.size, seen.toSet().size)
    }

    @Test
    fun aRandomPageTakesAFewChunksTogetherAndNeverTheSameOneTwice() = runTest {
        val sentences = (0 until 100).map { "Frase $it com casa." }
        val service = FakeService(mapOf("casa" to sentences), resource("corpora.json"))
        val provider = provider(service)
        val first = provider.search(query("casa", sort = ExampleSort.RANDOM, limit = 10))
        assertEquals(10, first.sentences.size)
        // The first chunk tells the count; two chunks at random places follow it.
        val sentenceRequests = { service.requests.filter { "/sentences/" in it.url.encodedPath } }
        assertTrue(sentenceRequests().size in 2..3, sentenceRequests().toString())
        val firstOffsets = sentenceRequests().drop(1).map { it.url.parameters["offset"] }.toSet()

        service.requests.clear()
        val second = provider.nextPage(first.nextPage!!, "en")
        assertEquals(10, second.sentences.size)
        val secondOffsets = sentenceRequests().map { it.url.parameters["offset"] }.toSet()
        assertEquals(2, secondOffsets.size)
        assertEquals(emptySet(), firstOffsets intersect secondOffsets)
        assertEquals(emptySet(), first.sentences.map { it.text }.toSet() intersect second.sentences.map { it.text }.toSet())
    }

    @Test
    fun aRareWordIsAlsoLookedUpInItsOtherCase() = runTest {
        val service = FakeService(mapOf("brasil" to listOf("o brasil."), "Brasil" to listOf("O Brasil ganhou.", "Viva o Brasil!")), resource("corpora.json"))
        val result = provider(service).search(query("brasil", sort = ExampleSort.SHORTEST))
        assertEquals(listOf("o brasil.", "Viva o Brasil!", "O Brasil ganhou."), result.sentences.map { it.text })
        assertEquals(3, result.total)
        assertNull(result.nextPage)
    }

    @Test
    fun cooccurrencesSkipNumbersAndTheWordItself() = runTest {
        val body = """[
            {"w1":{"word":"casa"},"w2":{"word":"fora","freq":9344},"freq":1639,"sig":6367.89},
            {"w1":{"word":"casa"},"w2":{"word":"2013","freq":901},"freq":432,"sig":2635.99},
            {"w1":{"word":"casa"},"w2":{"word":"Casa","freq":10},"freq":4,"sig":20.0},
            {"w1":{"word":"casa"},"w2":{"word":"donos","freq":901},"freq":432,"sig":2635.99}
        ]"""
        var path = ""
        var limit: String? = null
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/corpora")) return@MockEngine respond(resource("corpora.json"), HttpStatusCode.OK)
            path = request.url.encodedPath
            limit = request.url.parameters["limit"]
            respond(body, HttpStatusCode.OK)
        }
        val provider = LeipzigExamplesProvider(HttpClient(engine), baseUrl = "https://leipzig.test/ws")
        assertEquals(listOf("fora", "donos"), provider.cooccurrences("casa", portuguese, limit = 8))
        assertEquals("/ws/cooccurrences/por_news_2013_1M/cooccurrences/casa", path)
        assertEquals("24", limit)
    }

    @Test
    fun readsAWordsFrequency() = runTest {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("/corpora") -> respond(resource("corpora.json"), HttpStatusCode.OK)
                request.url.encodedPath.endsWith("/word/casa") -> respond("""{"id":243,"word":"casa","freq":11730,"wordRank":143,"frequencyClass":6}""", HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val provider = LeipzigExamplesProvider(HttpClient(engine), baseUrl = "https://leipzig.test/ws")
        assertEquals(com.tayra.languages.core.domain.service.CorpusFrequency(11730, 143, 6), provider.frequency("casa", portuguese))
        assertNull(provider.frequency("xyzzy", portuguese))
    }
}
