package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.service.AlibabaTranslation
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Alibaba Cloud Machine Translation (the general-purpose `TranslateGeneral` action) with the
 * user's RAM AccessKey pair; every request is signed by [AliyunSigner]. Serves both whole
 * sentences and single terms, from the text's language into the native language.
 */
@OptIn(ExperimentalTime::class)
class AlibabaTranslationProvider(
    private val client: HttpClient,
    private val settings: SettingsRepository,
    private val endpointOverride: String? = null,
    private val clock: Clock = Clock.System,
    private val nonce: () -> String = { Random.nextLong().toULong().toString(16) + Random.nextLong().toULong().toString(16) },
) : TermTranslationProvider, SentenceTranslator, AlibabaTranslation {

    private val json = Json { ignoreUnknownKeys = true }

    override val name: String = "Alibaba Cloud Translation"

    override suspend fun suggestTranslation(text: String, language: Language): String? = translate(text, language)

    override suspend fun translate(text: String, language: Language): String? {
        val query = text.replace(ZWS_STRING, "").trim()
        val source = LanguageCodes.codeFor(language.name) ?: return null
        val target = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        if (query.isEmpty() || source == target) return null
        return request(query, source, target)
            .onFailure { Logger.w { "Alibaba Cloud Translation failed for '$query': ${it.message}" } }
            .getOrNull()
            ?.takeIf { !it.equals(query, ignoreCase = true) }
    }

    override suspend fun checkKey(): String {
        val s = settings.current
        if (s.alibabaAccessKeyId.isBlank() || s.alibabaAccessKeySecret.isBlank()) return "Enter the AccessKey ID and Secret first."
        return request("Good morning", "en", "es").fold(
            onSuccess = { "The AccessKey works: \"Good morning\" \u2192 \"$it\"." },
            onFailure = { "The AccessKey was rejected: ${it.message}" },
        )
    }

    /** One signed call; the failure carries Alibaba's own message when it sent one. */
    private suspend fun request(query: String, source: String, target: String): Result<String> = runCatching {
        val s = settings.current
        val id = s.alibabaAccessKeyId.trim()
        val secret = s.alibabaAccessKeySecret.trim()
        require(id.isNotEmpty() && secret.isNotEmpty()) { "no AccessKey" }
        val host = (endpointOverride ?: s.alibabaEndpoint).trim().ifEmpty { "mt.aliyuncs.com" }.removePrefix("https://").removePrefix("http://").trimEnd('/')
        val params = mapOf(
            "Action" to "TranslateGeneral",
            "Version" to "2018-10-12",
            "Format" to "JSON",
            "AccessKeyId" to id,
            "SignatureMethod" to "HMAC-SHA1",
            "SignatureVersion" to "1.0",
            "SignatureNonce" to nonce(),
            "Timestamp" to timestamp(),
            "FormatType" to "text",
            "Scene" to "general",
            "SourceLanguage" to alibabaCode(source),
            "TargetLanguage" to alibabaCode(target),
            "SourceText" to query,
        )
        val signed = AliyunSigner.sign("POST", params, secret)
        val response = client.post("https://$host/") {
            signed.forEach { (k, v) -> parameter(k, v) }
        }
        val text = response.bodyAsText()
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
        val code = root?.get("Code")?.jsonPrimitive?.content
        if (code != null && code != "200") {
            throw IllegalStateException(root["Message"]?.jsonPrimitive?.content ?: code)
        }
        if (response.status.value !in 200..299) throw IllegalStateException(root?.get("Message")?.jsonPrimitive?.content ?: "HTTP ${response.status.value}")
        val translated = root?.get("Data")?.jsonObject?.get("Translated")?.jsonPrimitive?.content?.trim()
        translated?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("empty translation")
    }

    /** UTC as `yyyy-MM-ddTHH:mm:ssZ`, the only form the signature accepts. */
    private fun timestamp(): String {
        val t = clock.now().toLocalDateTime(TimeZone.UTC)
        fun two(n: Int) = n.toString().padStart(2, '0')
        return "${t.year}-${two(t.monthNumber)}-${two(t.dayOfMonth)}T${two(t.hour)}:${two(t.minute)}:${two(t.second)}Z"
    }

    /** Alibaba spells a few catalog codes differently. */
    private fun alibabaCode(code: String): String = when (code) {
        "nb" -> "no"
        else -> code
    }
}
