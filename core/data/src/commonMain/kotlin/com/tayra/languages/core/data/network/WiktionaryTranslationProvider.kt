package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.service.TermTranslationProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * English glosses from the Wiktionary REST definition API.
 *
 * The response is keyed by language code; each entry carries the language name, a part of
 * speech and HTML definitions. The section whose language name matches the term's language
 * is used; a lowercase lookup is tried when the exact title is missing.
 */
class WiktionaryTranslationProvider(
    private val client: HttpClient,
    private val baseUrl: String = "https://en.wiktionary.org/api/rest_v1/page/definition/",
) : TermTranslationProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun suggestTranslation(text: String, language: Language): String? {
        val title = text.replace(ZWS_STRING, "").trim().replace(' ', '_')
        if (title.isEmpty()) return null
        val candidates = listOf(title, title.lowercase()).distinct()
        for (candidate in candidates) {
            val body = fetch(candidate) ?: continue
            val gloss = runCatching { extract(body, language.name) }
                .onFailure { Logger.w(it) { "Could not parse Wiktionary response for $candidate" } }
                .getOrNull()
            if (!gloss.isNullOrBlank()) return gloss
        }
        return null
    }

    private suspend fun fetch(title: String): String? = try {
        val response = client.get(baseUrl + title.encodeURLPathPart())
        if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
    } catch (e: Exception) {
        Logger.w(e) { "Wiktionary lookup failed for $title" }
        null
    }

    /** Builds a compact gloss: one line per part of speech with its first definitions. */
    internal fun extract(body: String, languageName: String): String? {
        val sections = json.parseToJsonElement(body).jsonObject.values
            .filterIsInstance<JsonArray>()
            .flatMap { it }
            .map { it.jsonObject }
            .filter { it["language"]?.jsonPrimitive?.content.equals(languageName, ignoreCase = true) }
        if (sections.isEmpty()) return null

        val lines = sections.mapNotNull { section ->
            val definitions = (section["definitions"] as? JsonArray).orEmpty()
                .mapNotNull { (it as? JsonObject)?.get("definition")?.jsonPrimitive?.content }
                .map { HtmlText.toPlainText(it) }
                .filter { it.isNotBlank() }
                .take(MAX_DEFINITIONS_PER_SECTION)
            if (definitions.isEmpty()) return@mapNotNull null
            val partOfSpeech = section["partOfSpeech"]?.jsonPrimitive?.content?.lowercase()
            val joined = definitions.joinToString("; ")
            if (partOfSpeech.isNullOrBlank()) joined else "$partOfSpeech: $joined"
        }.take(MAX_SECTIONS)
        return lines.joinToString("\n").ifBlank { null }
    }

    private companion object {
        const val MAX_DEFINITIONS_PER_SECTION = 2
        const val MAX_SECTIONS = 3
    }
}
