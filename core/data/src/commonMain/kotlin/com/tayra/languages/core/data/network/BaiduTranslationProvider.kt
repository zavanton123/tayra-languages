package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.service.BaiduTranslation
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlin.random.Random
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Baidu Translate (the open platform's general translation API) with the user's App ID and
 * secret key; each request carries `sign = md5(appid + text + salt + key)`. Serves both whole
 * sentences and single terms, from the text's language into the native language.
 */
class BaiduTranslationProvider(
    private val client: HttpClient,
    private val settings: SettingsRepository,
    private val baseUrl: String = "https://fanyi-api.baidu.com/api/trans/vip/translate",
    private val salt: () -> String = { Random.nextInt(100_000_000, Int.MAX_VALUE).toString() },
) : TermTranslationProvider, SentenceTranslator, BaiduTranslation {

    private val json = Json { ignoreUnknownKeys = true }

    override val name: String = "Baidu Translate"

    override suspend fun suggestTranslation(text: String, language: Language): String? = translate(text, language)

    override suspend fun translate(text: String, language: Language): String? {
        val query = text.replace(ZWS_STRING, "").trim()
        val source = LanguageCodes.codeFor(language.name) ?: return null
        val target = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        if (query.isEmpty() || source == target) return null
        return request(query, source, target)
            .onFailure { Logger.w { "Baidu Translate failed for '$query': ${it.message}" } }
            .getOrNull()
            ?.takeIf { !it.equals(query, ignoreCase = true) }
    }

    override suspend fun checkKey(): String {
        val s = settings.current
        if (s.baiduAppId.isBlank() || s.baiduSecretKey.isBlank()) return "Enter the App ID and secret key first."
        return request("Good morning", "en", "es").fold(
            onSuccess = { "The App ID works: \"Good morning\" \u2192 \"$it\"." },
            onFailure = { "The App ID was rejected: ${it.message}" },
        )
    }

    /** One signed call; the failure carries Baidu's error message when it sent one. */
    private suspend fun request(query: String, source: String, target: String): Result<String> = runCatching {
        val s = settings.current
        val appId = s.baiduAppId.trim()
        val key = s.baiduSecretKey.trim()
        require(appId.isNotEmpty() && key.isNotEmpty()) { "no App ID" }
        val from = baiduCode(source) ?: throw IllegalStateException("Baidu Translate has no code for '$source'")
        val to = baiduCode(target) ?: throw IllegalStateException("Baidu Translate has no code for '$target'")
        val nonce = salt()
        val sign = Md5.hex((appId + query + nonce + key).encodeToByteArray())
        val response = client.submitForm(
            url = baseUrl,
            formParameters = parameters {
                append("q", query); append("from", from); append("to", to)
                append("appid", appId); append("salt", nonce); append("sign", sign)
            },
        )
        val text = response.bodyAsText()
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        root?.get("error_code")?.jsonPrimitive?.content?.takeIf { it != "52000" }?.let { code ->
            throw IllegalStateException(root["error_msg"]?.jsonPrimitive?.content?.let { "$it ($code)" } ?: "error $code")
        }
        if (response.status.value !in 200..299) throw IllegalStateException("HTTP ${response.status.value}")
        val translated = root?.get("trans_result")?.jsonArray?.joinToString("\n") { it.jsonObject["dst"]?.jsonPrimitive?.content.orEmpty() }?.trim()
        translated?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("empty translation")
    }

    private fun baiduCode(code: String): String? = CODES[code]

    private companion object {
        /** Baidu's own language codes for the catalog's ISO codes. */
        val CODES: Map<String, String> = mapOf(
            "en" to "en", "de" to "de", "pt" to "pt", "ru" to "ru", "fr" to "fra", "es" to "spa", "it" to "it", "nl" to "nl",
            "pl" to "pl", "cs" to "cs", "el" to "el", "bg" to "bul", "da" to "dan", "et" to "est", "fi" to "fin", "hu" to "hu",
            "ro" to "rom", "sl" to "slo", "sv" to "swe", "nb" to "nor", "uk" to "ukr", "tr" to "tr", "sk" to "sk", "lt" to "lit",
            "lv" to "lav", "mk" to "mac", "sr" to "srp", "hr" to "hrv", "bs" to "bos", "is" to "ice", "ga" to "gle", "gl" to "glg",
            "ca" to "cat", "eu" to "baq", "be" to "bel", "la" to "lat", "cy" to "wel", "sq" to "alb",
        )
    }
}
