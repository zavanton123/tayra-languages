package com.tayra.languages.core.data.network

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.language.LanguageCodes
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

class MyMemoryTranslationProviderTest {

    private fun response(text: String) = """{"responseData":{"translatedText":"$text","match":1},"responseStatus":200,"matches":[]}"""

    private fun provider(body: (langpair: String, q: String) -> String) = MyMemoryTranslationProvider(
        HttpClient(MockEngine { request ->
            val pair = request.url.parameters["langpair"].orEmpty()
            val q = request.url.parameters["q"].orEmpty()
            respond(body(pair, q), HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }),
        SettingsRepositoryImpl(MapSettings()),
        baseUrl = "https://example.test/get",
    )

    @Test
    fun translatesUsingTheLanguagePair() = runTest {
        val p = provider { pair, q -> if (pair == "es|en" && q == "muchacho") response("boy") else response("?") }
        assertEquals("boy", p.suggestTranslation("muchacho", Language(name = "Spanish")))
    }

    @Test
    fun ignoresEchoesServiceMessagesAndUnknownLanguages() = runTest {
        assertNull(provider { _, _ -> response("GATO") }.suggestTranslation("gato", Language(name = "Spanish")))
        assertNull(provider { _, _ -> response("PLEASE SELECT TWO DISTINCT LANGUAGES") }.suggestTranslation("gato", Language(name = "Spanish")))
        assertNull(provider { _, _ -> response("cat") }.suggestTranslation("gato", Language(name = "Klingon")))
        assertNull(provider { _, _ -> response("cat") }.suggestTranslation("cat", Language(name = "English")))
    }

    @Test
    fun languageCodes() {
        assertEquals("ru", LanguageCodes.codeFor("Russian"))
        assertEquals("zh", LanguageCodes.codeFor("Classical Chinese"))
        assertEquals("sa", LanguageCodes.codeFor("Sanskrit (Devanagari)"))
        assertNull(LanguageCodes.codeFor("Klingon"))
    }
}
