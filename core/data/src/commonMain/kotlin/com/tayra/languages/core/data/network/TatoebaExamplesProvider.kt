package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Example sentences from the Tatoeba corpus (CC BY 2.0 FR), restricted to sentences that
 * have a translation in the target language. Tatoeba uses ISO 639-3 language codes and
 * paginates with an opaque "next" link.
 */
class TatoebaExamplesProvider(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.tatoeba.org/v1/sentences",
) : ExampleSentencesProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: ExampleSearchQuery): ExampleSearchResult {
        val text = query.text.replace(ZWS_STRING, "").trim()
        val source = LanguageCodes.tatoebaCodeFor(query.language.name) ?: return ExampleSearchResult.EMPTY
        val target = LanguageCodes.tatoebaCode(query.targetLanguage) ?: "eng"
        if (text.isEmpty() || source == target) return ExampleSearchResult.EMPTY
        return fetch(text, target) {
            client.get(baseUrl) {
                slow()
                parameter("lang", source)
                parameter("q", text)
                parameter("trans:lang", target)
                parameter("sort", query.sort.apiValue)
                parameter("limit", query.limit)
                parameter("include", "audios")
                wordCountRange(query.minWords, query.maxWords)?.let { parameter("word_count", it) }
                query.isOrphan?.let { parameter("is_orphan", it.apiValue) }
                query.isUnapproved?.let { parameter("is_unapproved", it.apiValue) }
                query.isNative?.let { parameter("is_native", it.apiValue) }
                query.hasAudio?.let { parameter("has_audio", it.apiValue) }
                query.tags.map { it.trim() }.filter { it.isNotEmpty() }.forEach { parameter("tag", it) }
                query.listId?.trim()?.takeIf { it.isNotEmpty() }?.let { parameter("list", it) }
                query.owner?.trim()?.takeIf { it.isNotEmpty() }?.let { parameter("owner", it) }
                query.origin?.let { parameter("origin", it.apiValue) }
                query.transIsDirect?.let { parameter("trans:is_direct", it.apiValue) }
                query.transIsNative?.let { parameter("trans:is_native", it.apiValue) }
                query.transHasAudio?.let { parameter("trans:has_audio", it.apiValue) }
                query.transIsUnapproved?.let { parameter("trans:is_unapproved", it.apiValue) }
                query.transIsOrphan?.let { parameter("trans:is_orphan", it.apiValue) }
            }
        }
    }

    override suspend fun nextPage(nextPage: String, targetLanguage: String): ExampleSearchResult {
        val target = LanguageCodes.tatoebaCode(targetLanguage) ?: "eng"
        return fetch(nextPage, target) { client.get(nextPage) { slow() } }
    }

    // Tatoeba can take half a minute to answer, longer than the shared client waits.
    private fun HttpRequestBuilder.slow() = timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }

    /** The page [request] gives; a timeout, a network error or a server error is a failure, not a page without examples. */
    private suspend fun fetch(what: String, target: String, request: suspend () -> HttpResponse): ExampleSearchResult = try {
        val response = request()
        when {
            response.status == HttpStatusCode.OK -> parse(response.bodyAsText(), target)
            response.status.value >= 500 -> ExampleSearchResult.FAILED.also { Logger.w { "Tatoeba answered ${response.status} for $what" } }
            else -> ExampleSearchResult.EMPTY
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w(e) { "Tatoeba lookup failed for $what" }
        ExampleSearchResult.FAILED
    }

    private fun parse(body: String, target: String): ExampleSearchResult = runCatching { extract(body, target) }
        .onFailure { Logger.w(it) { "Could not parse Tatoeba response" } }
        .getOrDefault(ExampleSearchResult.EMPTY)

    private fun wordCountRange(min: Int?, max: Int?): String? = when {
        min == null && max == null -> null
        min != null && max != null -> "$min-$max"
        min != null -> "$min-"
        else -> "-$max"
    }

    internal fun extract(body: String, target: String): ExampleSearchResult {
        val root = json.parseToJsonElement(body).jsonObject
        val data = (root["data"] as? JsonArray) ?: return ExampleSearchResult.EMPTY
        val sentences = data.mapNotNull { element ->
            val sentence = element as? JsonObject ?: return@mapNotNull null
            val text = sentence["text"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (text.isEmpty()) return@mapNotNull null
            val translations = (sentence["translations"] as? JsonArray).orEmpty()
                .map { it.jsonObject }
                .filter { it["lang"]?.jsonPrimitive?.content == target }
            // Direct translations are the most reliable; fall back to any in the target language.
            val translation = (translations.firstOrNull { it["is_direct"]?.jsonPrimitive?.booleanOrNull == true } ?: translations.firstOrNull())
                ?.get("text")?.jsonPrimitive?.content?.trim()
            // The API's download_url points at a path that returns 404; the file endpoint is /v1/audios/{id}/file.
            // A licensed recording is tried first, as those without a licence are usually refused (403); a
            // refused download is found when it is fetched ahead of time, and the sentence is read aloud instead.
            val audios = (sentence["audios"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
            val audioUrl = (audios.firstOrNull { !it["license"]?.jsonPrimitive?.contentOrNull.isNullOrBlank() } ?: audios.firstOrNull())
                ?.get("id")?.jsonPrimitive?.content
                ?.let { "$AUDIO_FILE_URL/$it/file" }
            ExampleSentence(text, translation, audioUrl)
        }
        val paging = root["paging"] as? JsonObject
        val hasNext = paging?.get("has_next")?.jsonPrimitive?.booleanOrNull == true
        return ExampleSearchResult(
            sentences = sentences,
            total = paging?.get("total")?.jsonPrimitive?.intOrNull,
            nextPage = if (hasNext) paging["next"]?.jsonPrimitive?.content else null,
        )
    }

    private companion object {
        const val AUDIO_FILE_URL = "https://api.tatoeba.org/v1/audios"
        const val TIMEOUT = 45_000L
    }
}
