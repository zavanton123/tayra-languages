package com.tayra.languages.core.data.network

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.parseUrlEncodedParameters
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BaiduTranslationProviderTest {

    private fun provider(appId: String = "2015063000000001", key: String = "12345678", handler: (Map<String, String>) -> Pair<String, HttpStatusCode>): BaiduTranslationProvider {
        val settings = SettingsRepositoryImpl(MapSettings())
        runTest { settings.update { it.copy(nativeLanguage = "en", baiduAppId = appId, baiduSecretKey = key) } }
        return BaiduTranslationProvider(
            HttpClient(MockEngine { request ->
                val form = String(request.body.toByteArray()).parseUrlEncodedParameters()
                val (body, status) = handler(form.names().associateWith { form[it].orEmpty() })
                respond(body, status, headersOf("Content-Type", "application/json"))
            }),
            settings,
            baseUrl = "https://example.test/translate",
            salt = { "1435660288" },
        )
    }

    @Test
    fun translatesWithBaiduCodesAndTheDocumentedSignature() = runTest {
        var seen = emptyMap<String, String>()
        val p = provider { form ->
            seen = form
            """{"from":"en","to":"spa","trans_result":[{"src":"apple","dst":"manzana"}]}""" to HttpStatusCode.OK
        }
        assertEquals("manzana", p.translate("apple", Language(name = "Spanish")))
        assertEquals("spa", seen["from"]); assertEquals("en", seen["to"]); assertEquals("apple", seen["q"])
        assertEquals("2015063000000001", seen["appid"]); assertEquals("1435660288", seen["salt"])
        assertEquals("f89f9594663708c1605f3d736d01d2d4", seen["sign"])
    }

    @Test
    fun rejectedKeyGivesNullAndAReadableCheck() = runTest {
        val p = provider { _ -> """{"error_code":"52003","error_msg":"UNAUTHORIZED USER"}""" to HttpStatusCode.OK }
        assertNull(p.translate("manzana", Language(name = "Spanish")))
        assertTrue(p.checkKey().contains("UNAUTHORIZED USER"))
    }

    @Test
    fun withoutAKeyNothingIsSent() = runTest {
        var calls = 0
        val p = provider(key = "") { _ -> calls++; "{}" to HttpStatusCode.OK }
        assertNull(p.translate("manzana", Language(name = "Spanish")))
        assertEquals("Enter the App ID and secret key first.", p.checkKey())
        assertEquals(0, calls)
    }
}
