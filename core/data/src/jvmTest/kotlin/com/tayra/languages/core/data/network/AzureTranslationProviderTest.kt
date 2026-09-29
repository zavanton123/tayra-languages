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

class AzureTranslationProviderTest {

    private fun provider(key: String = "k", region: String = "westeurope", handler: (HttpRequestData) -> Pair<String, HttpStatusCode>): AzureTranslationProvider {
        val settings = SettingsRepositoryImpl(MapSettings())
        runTest { settings.update { it.copy(nativeLanguage = "en", azureTranslatorApiKey = key, azureTranslatorRegion = region) } }
        return AzureTranslationProvider(
            HttpClient(MockEngine { request ->
                val (body, status) = handler(request)
                respond(body, status, headersOf("Content-Type", "application/json"))
            }),
            settings,
            baseUrl = "https://example.test/translate",
        )
    }

    @Test
    fun translatesAndSendsKeyRegionAndLanguages() = runTest {
        var seen = ""
        val p = provider { request ->
            seen = listOf(
                request.headers["Ocp-Apim-Subscription-Key"], request.headers["Ocp-Apim-Subscription-Region"],
                request.url.parameters["from"], request.url.parameters["to"], request.url.parameters["api-version"],
            ).joinToString("|")
            """[{"detectedLanguage":null,"translations":[{"text":"The dog sleeps.","to":"en"}]}]""" to HttpStatusCode.OK
        }
        assertEquals("The dog sleeps.", p.translate("El perro duerme.", Language(name = "Spanish")))
        assertEquals("k|westeurope|es|en|3.0", seen)
    }

    @Test
    fun rejectedKeyGivesNullAndAReadableCheck() = runTest {
        val p = provider { _ -> """{"error":{"code":401000,"message":"The request is not authorized because credentials are missing or invalid."}}""" to HttpStatusCode.Unauthorized }
        assertNull(p.translate("El perro duerme.", Language(name = "Spanish")))
        assertTrue(p.checkKey().contains("not authorized"))
    }

    @Test
    fun withoutAKeyNothingIsSent() = runTest {
        var calls = 0
        val p = provider(key = "") { _ -> calls++; "[]" to HttpStatusCode.OK }
        assertNull(p.translate("El perro duerme.", Language(name = "Spanish")))
        assertEquals("Enter an API key first.", p.checkKey())
        assertEquals(0, calls)
    }
}
