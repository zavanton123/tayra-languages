package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Example sentences from the Tatoeba corpus (CC BY 2.0 FR), restricted to sentences that
 * have a translation in the target language. Tatoeba uses ISO 639-3 language codes.
 */
class TatoebaExamplesProvider(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.tatoeba.org/unstable/sentences",
) : ExampleSentencesProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun examples(text: String, language: Language, targetLanguage: String, limit: Int): List<ExampleSentence> {
        val query = text.replace(ZWS_STRING, "").trim()
        val source = LanguageCodes.tatoebaCodeFor(language.name) ?: return emptyList()
        val target = LanguageCodes.tatoebaCode(targetLanguage) ?: "eng"
        if (query.isEmpty() || source == target) return emptyList()
        val body = try {
            val response = client.get(baseUrl) {
                parameter("lang", source)
                parameter("q", query)
                parameter("trans:lang", target)
                parameter("sort", "relevance")
                parameter("word_count", "$MIN_WORDS-$MAX_WORDS")
                parameter("limit", limit)
            }
            if (response.status != HttpStatusCode.OK) return emptyList()
            response.bodyAsText()
        } catch (e: Exception) {
            Logger.w(e) { "Tatoeba lookup failed for $query" }
            return emptyList()
        }
        return runCatching { extract(body, target) }
            .onFailure { Logger.w(it) { "Could not parse Tatoeba response for $query" } }
            .getOrDefault(emptyList())
    }

    internal fun extract(body: String, target: String): List<ExampleSentence> {
        val data = (json.parseToJsonElement(body).jsonObject["data"] as? JsonArray) ?: return emptyList()
        return data.mapNotNull { element ->
            val sentence = element as? JsonObject ?: return@mapNotNull null
            val text = sentence["text"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (text.isEmpty()) return@mapNotNull null
            val translations = (sentence["translations"] as? JsonArray).orEmpty()
                .map { it.jsonObject }
                .filter { it["lang"]?.jsonPrimitive?.content == target }
            // Direct translations are the most reliable; fall back to any in the target language.
            val translation = (translations.firstOrNull { it["is_direct"]?.jsonPrimitive?.booleanOrNull == true } ?: translations.firstOrNull())
                ?.get("text")?.jsonPrimitive?.content?.trim()
            ExampleSentence(text, translation)
        }
    }

    private companion object {
        const val MIN_WORDS = 3
        const val MAX_WORDS = 14
    }
}
