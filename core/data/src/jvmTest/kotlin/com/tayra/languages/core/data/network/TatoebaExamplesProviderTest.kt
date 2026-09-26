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
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TatoebaExamplesProviderTest {

    private val body = """
        {"data": [
          {"id": 1, "text": "Ich vertraute ihr immer.", "lang": "deu", "audios": [{"id": 9, "download_url": "https://example.test/audio/9/file"}], "translations": [
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
        val provider = TatoebaExamplesProvider(HttpClient(engine), baseUrl = "https://example.test/sentences")
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

    @Test
    fun searchSendsFiltersAndFollowsPaging() = runTest {
        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            urls.add(request.url.toString())
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val provider = TatoebaExamplesProvider(HttpClient(engine), baseUrl = "https://example.test/sentences")
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
}
