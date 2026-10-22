package com.tayra.languages.core.data.network

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.YesNo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TatoebaExamplesProviderTest {

    private fun client(engine: MockEngine) = HttpClient(engine) { install(HttpTimeout) }

    private val query = ExampleSearchQuery("immer", Language(name = "German"), "en")

    private val body = """
        {"data": [
          {"id": 1, "text": "Ich vertraute ihr immer.", "lang": "deu", "audios": [{"id": 9, "download_url": "https://example.test/audio/9/file", "license": "CC BY 2.0 FR"}], "translations": [
             {"id": 2, "text": "I have always trusted her.", "lang": "eng", "is_direct": false},
             {"id": 3, "text": "I've always trusted her.", "lang": "eng", "is_direct": true},
             {"id": 4, "text": "Je lui ai toujours fait confiance.", "lang": "fra", "is_direct": true}
          ]},
          {"id": 5, "text": "Immer noch?", "lang": "deu", "translations": []},
          {"id": 6, "text": "", "lang": "deu", "translations": []}
        ], "paging": {"total": 3, "has_next": true, "next": "https://example.test/sentences?after=1"}}
    """.trimIndent()

    @Test
    fun parsesSentencesPreferringDirectTranslations() = runTest {
        var url = ""
        val engine = MockEngine { request ->
            url = request.url.toString()
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val provider = TatoebaExamplesProvider(client(engine), baseUrl = "https://example.test/sentences")
        val examples = provider.examples("immer", Language(name = "German"), targetLanguage = "en")
        assertEquals(
            listOf(ExampleSentence("Ich vertraute ihr immer.", "I've always trusted her.", "https://api.tatoeba.org/v1/audios/9/file"), ExampleSentence("Immer noch?", null)),
            examples,
        )
        listOf("lang=deu", "trans%3Alang=eng", "sort=random", "word_count=1-15", "limit=10", "include=audios").forEach {
            assertEquals(true, url.contains(it), "$it in $url")
        }
        assertEquals(false, url.contains("has_audio"), url)
    }

    /** Every recording is offered, a licensed one first; whether it downloads is found out when it is fetched. */
    @Test
    fun aLicensedRecordingIsPreferredButAnyIsOffered() {
        val body = """
            {"data": [
              {"id": 1, "text": "Eu não tenho tempo.", "lang": "por", "audios": [{"id": 7, "license": ""}], "translations": []},
              {"id": 2, "text": "Acabou o tempo.", "lang": "por", "audios": [{"id": 8, "license": null}, {"id": 9, "license": "CC BY-NC 4.0"}], "translations": []},
              {"id": 3, "text": "Quero tempo!", "lang": "por", "audios": [{"id": 10}], "translations": []}
            ]}
        """.trimIndent()
        val result = TatoebaExamplesProvider(HttpClient(MockEngine { respond("", HttpStatusCode.OK) })).extract(body, "eng")
        assertEquals(
            listOf("https://api.tatoeba.org/v1/audios/7/file", "https://api.tatoeba.org/v1/audios/9/file", "https://api.tatoeba.org/v1/audios/10/file"),
            result.sentences.map { it.audioUrl },
        )
    }

    @Test
    fun searchSendsFiltersAndFollowsPaging() = runTest {
        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            urls.add(request.url.toString())
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val provider = TatoebaExamplesProvider(client(engine), baseUrl = "https://example.test/sentences")
        val query = ExampleSearchQuery(
            text = "immer", language = Language(name = "German"), targetLanguage = "en",
            minWords = 4, maxWords = null, sort = ExampleSort.SHORTEST, isNative = YesNo.YES, hasAudio = null,
            tags = listOf("idiom", "!colloquial"), transIsDirect = YesNo.YES, limit = 50,
        )
        val result = provider.search(query)
        assertEquals(3, result.total)
        assertEquals("https://example.test/sentences?after=1", result.nextPage)
        val url = urls.single()
        listOf("word_count=4-", "sort=words", "is_native=yes", "tag=idiom", "tag=%21colloquial", "trans%3Ais_direct=yes", "limit=50", "is_unapproved=no").forEach {
            assertEquals(true, url.contains(it), "$it in $url")
        }
        assertEquals(false, url.contains("has_audio"), url)
        assertEquals(false, url.contains("is_orphan"), url)

        val next = provider.nextPage(result.nextPage!!, "en")
        assertEquals(2, next.sentences.size)
        assertEquals("https://example.test/sentences?after=1", urls.last())
    }

    @Test
    fun unknownLanguagesGiveNoExamples() = runTest {
        val provider = TatoebaExamplesProvider(HttpClient(MockEngine { error("must not be called") }))
        assertEquals(emptyList(), provider.examples("x", Language(name = "Klingon"), "en"))
        assertEquals("cmn", LanguageCodes.tatoebaCodeFor("Classical Chinese"))
        assertEquals("rus", LanguageCodes.tatoebaCode("ru"))
    }

    @Test
    fun requestsWaitLongerThanTheSharedClient() = runTest {
        val waits = mutableListOf<Long?>()
        val engine = MockEngine { request ->
            waits += request.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis
            waits += request.getCapabilityOrNull(HttpTimeoutCapability)?.socketTimeoutMillis
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val provider = TatoebaExamplesProvider(client(engine), baseUrl = "https://example.test/sentences")
        provider.nextPage(provider.search(query).nextPage!!, "en")
        assertEquals(List<Long?>(4) { 45_000L }, waits)
    }

    @Test
    fun aTimeoutIsAFailureNotAnEmptyResult() = runTest {
        val provider = TatoebaExamplesProvider(client(MockEngine { throw SocketTimeoutException("timeout") }), baseUrl = "https://example.test/sentences")
        assertTrue(provider.search(query).failed)
        assertTrue(provider.nextPage("https://example.test/sentences?after=1", "en").failed)
        assertEquals(emptyList(), provider.examples("immer", Language(name = "German"), "en"), "the short list stays quietly empty")
    }

    @Test
    fun aServerErrorIsAFailureButARefusedQueryFindsNothing() = runTest {
        val unavailable = TatoebaExamplesProvider(client(MockEngine { respond("", HttpStatusCode.ServiceUnavailable) }), baseUrl = "https://example.test/sentences")
        assertTrue(unavailable.search(query).failed)
        val refused = TatoebaExamplesProvider(client(MockEngine { respond("", HttpStatusCode.BadRequest) }), baseUrl = "https://example.test/sentences")
        val result = refused.search(query)
        assertFalse(result.failed)
        assertEquals(emptyList(), result.sentences)
    }

    @Test
    fun aCancelledLookupIsNotTurnedIntoAResult() = runBlocking {
        val asked = CompletableDeferred<Unit>()
        val provider = TatoebaExamplesProvider(client(MockEngine { asked.complete(Unit); awaitCancellation() }), baseUrl = "https://example.test/sentences")
        var result: Any? = null
        val job = launch(Dispatchers.Default) { result = provider.search(query) }
        asked.await()
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
        assertNull(result)
    }
}
