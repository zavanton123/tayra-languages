package com.tayra.languages.core.data.network

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoogleTranslationProviderTest {

    private fun provider(key: String = "k", handler: (HttpRequestData) -> Pair<String, HttpStatusCode>): GoogleTranslationProvider {
        val settings = SettingsRepositoryImpl(MapSettings())
        runTest { settings.update { it.copy(nativeLanguage = "en", googleTranslateApiKey = key) } }
        return GoogleTranslationProvider(
            HttpClient(MockEngine { request ->
                val (body, status) = handler(request)
                respond(body, status, headersOf("Content-Type", "application/json"))
            }),
            settings,
            baseUrl = "https://example.test/v2",
        )
    }

    @Test
    fun translatesAndSendsTheKey() = runTest {
        var seenKey = ""
        val p = provider { request ->
            seenKey = request.url.parameters["key"].orEmpty()
            """{"data":{"translations":[{"translatedText":"The dog sleeps."}]}}""" to HttpStatusCode.OK
        }
        assertEquals("The dog sleeps.", p.translate("El perro duerme.", Language(name = "Spanish")))
        assertEquals("k", seenKey)
    }

    @Test
    fun rejectedKeyGivesNullAndAReadableCheck() = runTest {
        val p = provider { _ -> """{"error":{"code":403,"message":"API key not valid."}}""" to HttpStatusCode.Forbidden }
        assertNull(p.translate("El perro duerme.", Language(name = "Spanish")))
        assertTrue(p.checkKey().contains("API key not valid"))
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
