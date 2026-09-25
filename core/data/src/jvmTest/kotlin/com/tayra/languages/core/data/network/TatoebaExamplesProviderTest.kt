package com.tayra.languages.core.data.network

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.ExampleSentence
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
          {"id": 1, "text": "Ich vertraute ihr immer.", "lang": "deu", "translations": [
             {"id": 2, "text": "I have always trusted her.", "lang": "eng", "is_direct": false},
             {"id": 3, "text": "I've always trusted her.", "lang": "eng", "is_direct": true},
             {"id": 4, "text": "Je lui ai toujours fait confiance.", "lang": "fra", "is_direct": true}
          ]},
          {"id": 5, "text": "Immer noch?", "lang": "deu", "translations": []},
          {"id": 6, "text": "", "lang": "deu", "translations": []}
        ], "paging": {"total": 3}}
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
            listOf(ExampleSentence("Ich vertraute ihr immer.", "I've always trusted her."), ExampleSentence("Immer noch?", null)),
            examples,
        )
        assertEquals(true, url.contains("lang=deu") && url.contains("trans%3Alang=eng") && url.contains("sort=relevance"), url)
    }

    @Test
    fun unknownLanguagesGiveNoExamples() = runTest {
        val provider = TatoebaExamplesProvider(HttpClient(MockEngine { error("must not be called") }))
        assertEquals(emptyList(), provider.examples("x", Language(name = "Klingon"), "en"))
        assertEquals("cmn", LanguageCodes.tatoebaCodeFor("Classical Chinese"))
        assertEquals("rus", LanguageCodes.tatoebaCode("ru"))
    }
}
