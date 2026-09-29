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

class DeeplTranslationProviderTest {

    private fun provider(key: String = "abc:fx", baseUrl: String? = "https://example.test/v2/translate", handler: suspend (HttpRequestData) -> Pair<String, HttpStatusCode>): DeeplTranslationProvider {
        val settings = SettingsRepositoryImpl(MapSettings())
        runTest { settings.update { it.copy(nativeLanguage = "en", deeplApiKey = key) } }
        return DeeplTranslationProvider(
            HttpClient(MockEngine { request ->
                val (body, status) = handler(request)
                respond(body, status, headersOf("Content-Type", "application/json"))
            }),
            settings,
            baseUrlOverride = baseUrl,
        )
    }

    @Test
    fun translatesWithUppercaseCodesAndTheAuthHeader() = runTest {
        var seen = ""
        val p = provider { request ->
            seen = request.headers["Authorization"] + "|" + String(request.body.toByteArray())
            """{"translations":[{"detected_source_language":"ES","text":"The dog sleeps."}]}""" to HttpStatusCode.OK
        }
        assertEquals("The dog sleeps.", p.translate("El perro duerme.", Language(name = "Spanish")))
        assertTrue(seen.startsWith("DeepL-Auth-Key abc:fx|"))
        assertTrue(seen.contains("\"source_lang\":\"ES\"") && seen.contains("\"target_lang\":\"EN-US\""))
    }

    @Test
    fun freeKeysGoToTheFreeHost() = runTest {
        var host = ""
        val p = provider(key = "abc:fx", baseUrl = null) { request -> host = request.url.host; """{"translations":[{"text":"x"}]}""" to HttpStatusCode.OK }
        p.translate("hola", Language(name = "Spanish"))
        assertEquals("api-free.deepl.com", host)
        val paid = provider(key = "abc", baseUrl = null) { request -> host = request.url.host; """{"translations":[{"text":"x"}]}""" to HttpStatusCode.OK }
        paid.translate("hola", Language(name = "Spanish"))
        assertEquals("api.deepl.com", host)
    }

    @Test
    fun rejectedKeyGivesNullAndAReadableCheck() = runTest {
        val p = provider { _ -> """{"message":"Authorization failed. Please supply a valid DeepL-Auth-Key."}""" to HttpStatusCode.Forbidden }
        assertNull(p.translate("El perro duerme.", Language(name = "Spanish")))
        assertTrue(p.checkKey().contains("Authorization failed"))
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
