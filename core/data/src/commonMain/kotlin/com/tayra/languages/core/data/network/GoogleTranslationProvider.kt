package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.service.GoogleTranslation
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Google Cloud Translation (basic v2 edition) with the user's own API key. Serves both whole
 * sentences and single terms, from the text's language into the native language.
 */
class GoogleTranslationProvider(
    private val client: HttpClient,
    private val settings: SettingsRepository,
    private val baseUrl: String = "https://translation.googleapis.com/language/translate/v2",
) : TermTranslationProvider, SentenceTranslator, GoogleTranslation {

    private val json = Json { ignoreUnknownKeys = true }

    override val name: String = "Google Translate"

    override suspend fun suggestTranslation(text: String, language: Language): String? = translate(text, language)

    override suspend fun translate(text: String, language: Language): String? {
        val query = text.replace(ZWS_STRING, "").trim()
        val source = LanguageCodes.codeFor(language.name) ?: return null
        val target = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        if (query.isEmpty() || source == target) return null
        return request(query, source, target)
            .onFailure { Logger.w { "Google Translate failed for '$query': ${it.message}" } }
            .getOrNull()
            ?.takeIf { !it.equals(query, ignoreCase = true) }
    }

    override suspend fun checkKey(): String {
        if (settings.current.googleTranslateApiKey.isBlank()) return "Enter an API key first."
        return request("Good morning", "en", "es").fold(
            onSuccess = { "The key works: \"Good morning\" \u2192 \"$it\"." },
            onFailure = { "The key was rejected: ${it.message}" },
        )
    }

    /** One call to the v2 endpoint; the failure carries Google's own message when it sent one. */
    private suspend fun request(query: String, source: String, target: String): Result<String> = runCatching {
        val key = settings.current.googleTranslateApiKey.trim()
        require(key.isNotEmpty()) { "no API key" }
        val body = buildJsonObject {
            putJsonArray("q") { add(kotlinx.serialization.json.JsonPrimitive(query)) }
            put("source", googleCode(source))
            put("target", googleCode(target))
            put("format", "text")
        }
        val response = client.post(baseUrl) {
            parameter("key", key)
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = response.bodyAsText()
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        root?.get("error")?.jsonObject?.let { err ->
            val message = err["message"]?.jsonPrimitive?.content ?: "HTTP ${response.status.value}"
            throw IllegalStateException(message)
        }
        if (response.status.value !in 200..299) throw IllegalStateException("HTTP ${response.status.value}")
        val translated = root?.get("data")?.jsonObject?.get("translations")?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("translatedText")?.jsonPrimitive?.content?.trim()
        translated?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("empty translation")
    }

    /** Google spells a few catalog codes differently. */
    private fun googleCode(code: String): String = when (code) {
        "nb" -> "no"
        else -> code
    }
}
