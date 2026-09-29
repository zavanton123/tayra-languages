package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.service.QwenTranslation
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Qwen-MT through Alibaba Model Studio's OpenAI-compatible chat endpoint: the text is the one
 * user message and `translation_options` names the languages in English. Serves both whole
 * sentences and single terms, from the text's language into the native language.
 */
class QwenTranslationProvider(
    private val client: HttpClient,
    private val settings: SettingsRepository,
    private val baseUrlOverride: String? = null,
) : TermTranslationProvider, SentenceTranslator, QwenTranslation {

    private val json = Json { ignoreUnknownKeys = true }

    override val name: String = "Qwen-MT"

    override suspend fun suggestTranslation(text: String, language: Language): String? = translate(text, language)

    override suspend fun translate(text: String, language: Language): String? {
        val query = text.replace(ZWS_STRING, "").trim()
        val source = LanguageCodes.codeFor(language.name) ?: return null
        val target = LanguageCatalog.nativeOption(settings.current.nativeLanguage)
        if (query.isEmpty() || source == target.code) return null
        return request(query, language.name, target.name)
            .onFailure { Logger.w { "Qwen-MT failed for '$query': ${it.message}" } }
            .getOrNull()
            ?.takeIf { !it.equals(query, ignoreCase = true) }
    }

    override suspend fun checkKey(): String {
        if (settings.current.qwenApiKey.isBlank()) return "Enter an API key first."
        return request("Good morning", "English", "Spanish").fold(
            onSuccess = { "The key works: \"Good morning\" \u2192 \"$it\"." },
            onFailure = { "The key was rejected: ${it.message}" },
        )
    }

    private suspend fun request(query: String, sourceName: String, targetName: String): Result<String> = runCatching {
        val s = settings.current
        val key = s.qwenApiKey.trim()
        require(key.isNotEmpty()) { "no API key" }
        val url = baseUrlOverride ?: if (s.qwenInternational) INTERNATIONAL_URL else CHINA_URL
        val body = buildJsonObject {
            put("model", s.qwenModel.trim().ifEmpty { "qwen-mt-turbo" })
            putJsonArray("messages") { add(buildJsonObject { put("role", "user"); put("content", query) }) }
            putJsonObject("translation_options") { put("source_lang", sourceName); put("target_lang", targetName) }
        }
        val response = client.post(url) {
            header("Authorization", "Bearer $key")
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = response.bodyAsText()
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        root?.get("error")?.jsonObject?.let { err ->
            throw IllegalStateException(err["message"]?.jsonPrimitive?.content ?: "HTTP ${response.status.value}")
        }
        if (response.status.value !in 200..299) throw IllegalStateException("HTTP ${response.status.value}")
        val translated = root?.get("choices")?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content?.trim()
        translated?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("empty translation")
    }

    private companion object {
        const val INTERNATIONAL_URL = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions"
        const val CHINA_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    }
}
