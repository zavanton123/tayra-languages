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

class AlibabaTranslationProviderTest {

    private fun provider(id: String = "id", secret: String = "secret", handler: (HttpRequestData) -> Pair<String, HttpStatusCode>): AlibabaTranslationProvider {
        val settings = SettingsRepositoryImpl(MapSettings())
        runTest { settings.update { it.copy(nativeLanguage = "en", alibabaAccessKeyId = id, alibabaAccessKeySecret = secret) } }
        return AlibabaTranslationProvider(
            HttpClient(MockEngine { request ->
                val (body, status) = handler(request)
                respond(body, status, headersOf("Content-Type", "application/json"))
            }),
            settings,
            endpointOverride = "example.test",
        )
    }

    @Test
    fun translatesWithASignedRequest() = runTest {
        var seen = emptyMap<String, String?>()
        val p = provider { request ->
            val q = request.url.parameters
            seen = listOf("Action", "AccessKeyId", "SourceLanguage", "TargetLanguage", "SourceText", "SignatureMethod").associateWith { q[it] } + ("host" to request.url.host) + ("hasSignature" to (q["Signature"]?.isNotEmpty()).toString())
            """{"RequestId":"r","Data":{"WordCount":"3","Translated":"The dog sleeps."},"Code":"200"}""" to HttpStatusCode.OK
        }
        assertEquals("The dog sleeps.", p.translate("El perro duerme.", Language(name = "Spanish")))
        assertEquals("TranslateGeneral", seen["Action"]); assertEquals("id", seen["AccessKeyId"])
        assertEquals("es", seen["SourceLanguage"]); assertEquals("en", seen["TargetLanguage"])
        assertEquals("El perro duerme.", seen["SourceText"]); assertEquals("HMAC-SHA1", seen["SignatureMethod"])
        assertEquals("example.test", seen["host"]); assertEquals("true", seen["hasSignature"])
    }

    @Test
    fun rejectedKeyGivesNullAndAReadableCheck() = runTest {
        val p = provider { _ -> """{"RequestId":"r","Message":"Specified access key is not found.","Code":"InvalidAccessKeyId.NotFound"}""" to HttpStatusCode.NotFound }
        assertNull(p.translate("El perro duerme.", Language(name = "Spanish")))
        assertTrue(p.checkKey().contains("access key is not found"))
    }

    @Test
    fun withoutAKeyNothingIsSent() = runTest {
        var calls = 0
        val p = provider(secret = "") { _ -> calls++; "{}" to HttpStatusCode.OK }
        assertNull(p.translate("El perro duerme.", Language(name = "Spanish")))
        assertEquals("Enter the AccessKey ID and Secret first.", p.checkKey())
        assertEquals(0, calls)
    }
}
