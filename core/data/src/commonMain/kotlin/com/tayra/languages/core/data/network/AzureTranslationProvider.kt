package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.service.AzureTranslation
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Microsoft Translator (Azure AI Services, Translator API v3) with the user's own key and
 * region. Serves both whole sentences and single terms, from the text's language into the
 * native language.
 */
class AzureTranslationProvider(
    private val client: HttpClient,
    private val settings: SettingsRepository,
    private val baseUrl: String = "https://api.cognitive.microsofttranslator.com/translate",
) : TermTranslationProvider, SentenceTranslator, AzureTranslation {

    private val json = Json { ignoreUnknownKeys = true }

    override val name: String = "Microsoft Translator"

    override suspend fun suggestTranslation(text: String, language: Language): String? = translate(text, language)

    override suspend fun translate(text: String, language: Language): String? {
        val query = text.replace(ZWS_STRING, "").trim()
        val source = LanguageCodes.codeFor(language.name) ?: return null
        val target = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        if (query.isEmpty() || source == target) return null
        return request(query, source, target)
            .onFailure { Logger.w { "Microsoft Translator failed for '$query': ${it.message}" } }
            .getOrNull()
            ?.takeIf { !it.equals(query, ignoreCase = true) }
    }

    override suspend fun checkKey(): String {
        if (settings.current.azureTranslatorApiKey.isBlank()) return "Enter an API key first."
        return request("Good morning", "en", "es").fold(
            onSuccess = { "The key works: \"Good morning\" \u2192 \"$it\"." },
            onFailure = { "The key was rejected: ${it.message}" },
        )
    }

    /** One call to the v3 endpoint; the failure carries Microsoft's own message when it sent one. */
    private suspend fun request(query: String, source: String, target: String): Result<String> = runCatching {
        val key = settings.current.azureTranslatorApiKey.trim()
        require(key.isNotEmpty()) { "no API key" }
        val region = settings.current.azureTranslatorRegion.trim()
        val body = buildJsonArray { add(buildJsonObject { put("Text", JsonPrimitive(query)) }) }
        val response = client.post(baseUrl) {
            parameter("api-version", "3.0")
            parameter("from", azureCode(source))
            parameter("to", azureCode(target))
            parameter("textType", "plain")
            header("Ocp-Apim-Subscription-Key", key)
            if (region.isNotEmpty()) header("Ocp-Apim-Subscription-Region", region)
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = response.bodyAsText()
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull()
        (root as? kotlinx.serialization.json.JsonObject)?.get("error")?.jsonObject?.let { err ->
            val message = err["message"]?.jsonPrimitive?.content ?: "HTTP ${response.status.value}"
            throw IllegalStateException(message)
        }
        if (response.status.value !in 200..299) throw IllegalStateException("HTTP ${response.status.value}")
        val translated = (root as? kotlinx.serialization.json.JsonArray)?.firstOrNull()?.jsonObject
            ?.get("translations")?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content?.trim()
        translated?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("empty translation")
    }

    /** Microsoft spells a few catalog codes differently. */
    private fun azureCode(code: String): String = when (code) {
        "sr" -> "sr-Cyrl"
        else -> code
    }
}
