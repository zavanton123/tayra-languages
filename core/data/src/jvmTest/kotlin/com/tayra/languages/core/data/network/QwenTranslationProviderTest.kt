package com.tayra.languages.core.data.network

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QwenTranslationProviderTest {

    private fun provider(key: String = "sk-1", international: Boolean = true, baseUrl: String? = "https://example.test/v1/chat/completions", handler: suspend (HttpRequestData) -> Pair<String, HttpStatusCode>): QwenTranslationProvider {
        val settings = SettingsRepositoryImpl(MapSettings())
        runTest { settings.update { it.copy(nativeLanguage = "en", qwenApiKey = key, qwenInternational = international) } }
        return QwenTranslationProvider(
            HttpClient(MockEngine { request ->
                val (body, status) = handler(request)
                respond(body, status, headersOf("Content-Type", "application/json"))
            }),
            settings,
            baseUrlOverride = baseUrl,
        )
    }

    @Test
    fun translatesWithLanguageNamesAndTheBearerKey() = runTest {
        var seen = ""
        val p = provider { request ->
            seen = request.headers["Authorization"] + "|" + String(request.body.toByteArray())
            """{"choices":[{"message":{"role":"assistant","content":"The dog sleeps."}}]}""" to HttpStatusCode.OK
        }
        assertEquals("The dog sleeps.", p.translate("El perro duerme.", Language(name = "Spanish")))
        assertTrue(seen.startsWith("Bearer sk-1|"))
        assertTrue(seen.contains("\"model\":\"qwen-mt-turbo\""))
        assertTrue(seen.contains("\"source_lang\":\"Spanish\"") && seen.contains("\"target_lang\":\"English\""))
    }

    @Test
    fun regionPicksTheHost() = runTest {
        var host = ""
        provider(international = true, baseUrl = null) { request -> host = request.url.host; """{"choices":[{"message":{"content":"x"}}]}""" to HttpStatusCode.OK }
            .translate("hola", Language(name = "Spanish"))
        assertEquals("dashscope-intl.aliyuncs.com", host)
        provider(international = false, baseUrl = null) { request -> host = request.url.host; """{"choices":[{"message":{"content":"x"}}]}""" to HttpStatusCode.OK }
            .translate("hola", Language(name = "Spanish"))
        assertEquals("dashscope.aliyuncs.com", host)
    }

    @Test
    fun rejectedKeyGivesNullAndAReadableCheck() = runTest {
        val p = provider { _ -> """{"error":{"message":"Incorrect API key provided.","type":"invalid_request_error","code":"invalid_api_key"}}""" to HttpStatusCode.Unauthorized }
        assertNull(p.translate("El perro duerme.", Language(name = "Spanish")))
        assertTrue(p.checkKey().contains("Incorrect API key"))
    }

    @Test
    fun withoutAKeyNothingIsSent() = runTest {
        var calls = 0
        val p = provider(key = "") { _ -> calls++; "{}" to HttpStatusCode.OK }
        assertNull(p.translate("El perro duerme.", Language(name = "Spanish")))
        assertEquals("Enter an API key first.", p.checkKey())
        assertEquals(0, calls)
    }
}
