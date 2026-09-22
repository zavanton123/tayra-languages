package com.tayra.languages.core.data.network

import com.tayra.languages.core.domain.model.Language
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WiktionaryTranslationProviderTest {

    private val gato = """
        {
          "en": [{"partOfSpeech": "Noun", "language": "English", "definitions": [{"definition": "<span class=\"x\"></span> "}]}],
          "es": [
            {"partOfSpeech": "Noun", "language": "Spanish", "definitions": [
              {"definition": "<a href=\"/wiki/cat\">cat</a> <span>(male)</span>"},
              {"definition": "<a href=\"/wiki/tomcat\">tomcat</a>"},
              {"definition": "jack"}
            ]},
            {"partOfSpeech": "Adjective", "language": "Spanish", "definitions": [{"definition": "sly &amp; cunning"}]}
          ]
        }
    """.trimIndent()

    private fun provider(vararg responses: Pair<String, String?>): WiktionaryTranslationProvider {
        val engine = MockEngine { request ->
            val title = request.url.encodedPath.substringAfterLast('/')
            val body = responses.firstOrNull { it.first == title }?.second
            if (body == null) respond("", HttpStatusCode.NotFound) else respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        return WiktionaryTranslationProvider(HttpClient(engine), baseUrl = "https://example.test/definition/")
    }

    @Test
    fun picksTheSectionForTheLanguage() = runTest {
        val gloss = provider("gato" to gato).suggestTranslation("gato", Language(name = "Spanish"))
        assertEquals("noun: cat (male); tomcat\nadjective: sly & cunning", gloss)
    }

    @Test
    fun fallsBackToLowercaseAndReturnsNullWhenUnknown() = runTest {
        val p = provider("gato" to gato)
        assertEquals("noun: cat (male); tomcat\nadjective: sly & cunning", p.suggestTranslation("Gato", Language(name = "Spanish")))
        assertNull(p.suggestTranslation("gato", Language(name = "French")))
        assertNull(p.suggestTranslation("perro", Language(name = "Spanish")))
    }
}
