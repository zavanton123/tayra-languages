package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.service.DeeplTranslation
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * DeepL API v2 with the user's key. Free keys end in `:fx` and must use the free host. Serves
 * both whole sentences and single terms, from the text's language into the native language.
 */
class DeeplTranslationProvider(
    private val client: HttpClient,
    private val settings: SettingsRepository,
    private val baseUrlOverride: String? = null,
) : TermTranslationProvider, SentenceTranslator, DeeplTranslation {

    private val json = Json { ignoreUnknownKeys = true }

    override val name: String = "DeepL"

    override suspend fun suggestTranslation(text: String, language: Language): String? = translate(text, language)

    override suspend fun translate(text: String, language: Language): String? {
        val query = text.replace(ZWS_STRING, "").trim()
        val source = LanguageCodes.codeFor(language.name) ?: return null
        val target = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        if (query.isEmpty() || source == target) return null
        return request(query, source, target)
            .onFailure { Logger.w { "DeepL failed for '$query': ${it.message}" } }
            .getOrNull()
            ?.takeIf { !it.equals(query, ignoreCase = true) }
    }

    override suspend fun checkKey(): String {
        if (settings.current.deeplApiKey.isBlank()) return "Enter an API key first."
        return request("Good morning", "en", "es").fold(
            onSuccess = { "The key works: \"Good morning\" \u2192 \"$it\"." },
            onFailure = { "The key was rejected: ${it.message}" },
        )
    }

    private suspend fun request(query: String, source: String, target: String): Result<String> = runCatching {
        val key = settings.current.deeplApiKey.trim()
        require(key.isNotEmpty()) { "no API key" }
        val url = baseUrlOverride ?: if (key.endsWith(":fx")) FREE_URL else PAID_URL
        val body = buildJsonObject {
            putJsonArray("text") { add(JsonPrimitive(query)) }
            put("source_lang", sourceCode(source))
            put("target_lang", targetCode(target))
        }
        val response = client.post(url) {
            header("Authorization", "DeepL-Auth-Key $key")
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = response.bodyAsText()
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        if (response.status.value !in 200..299) {
            throw IllegalStateException(root?.get("message")?.jsonPrimitive?.content ?: "HTTP ${response.status.value}")
        }
        val translated = root?.get("translations")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content?.trim()
        translated?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("empty translation")
    }

    /** DeepL wants uppercase codes; a few targets need a regional variant. */
    private fun sourceCode(code: String): String = code.uppercase()

    private fun targetCode(code: String): String = when (code) {
        "en" -> "EN-US"
        "pt" -> "PT-PT"
        else -> code.uppercase()
    }

    private companion object {
        const val FREE_URL = "https://api-free.deepl.com/v2/translate"
        const val PAID_URL = "https://api.deepl.com/v2/translate"
    }
}
