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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * English glosses from the Wiktionary REST definition API.
 *
 * The response is keyed by language code; each entry carries the language name, a part of
 * speech and HTML definitions. Only the section matching the term's language is used, and
 * inflected forms ("plural of X") are resolved to their lemma so the gloss is a translation.
 */
class WiktionaryTranslationProvider(
    private val client: HttpClient,
    private val baseUrl: String = "https://en.wiktionary.org/api/rest_v1/page/definition/",
) : TermTranslationProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun suggestTranslation(text: String, language: Language): String? =
        lookup(text.replace(ZWS_STRING, "").trim(), language.name, followForms = true)

    private suspend fun lookup(text: String, languageName: String, followForms: Boolean): String? {
        val title = text.replace(' ', '_')
        if (title.isEmpty()) return null
        for (candidate in listOf(title, title.lowercase()).distinct()) {
            val body = fetch(candidate) ?: continue
            val parsed = runCatching { extract(body, languageName) }
                .onFailure { Logger.w(it) { "Could not parse Wiktionary response for $candidate" } }
                .getOrNull() ?: continue
            parsed.gloss?.let { return it }
            if (followForms && parsed.lemma != null && !parsed.lemma.equals(text, ignoreCase = true)) {
                lookup(parsed.lemma, languageName, followForms = false)?.let { return it }
            }
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

    /** A translation gloss, or the lemma of an inflected form when only form-of definitions exist. */
    internal data class Parsed(val gloss: String?, val lemma: String?)

    internal fun extract(body: String, languageName: String): Parsed? {
        val sections = json.parseToJsonElement(body).jsonObject.values
            .filterIsInstance<JsonArray>()
            .flatMap { it }
            .map { it.jsonObject }
            .filter { it["language"]?.jsonPrimitive?.content.equals(languageName, ignoreCase = true) }
        if (sections.isEmpty()) return null

        var lemma: String? = null
        for (section in sections) {
            val definitions = (section["definitions"] as? JsonArray).orEmpty()
                .mapNotNull { (it as? JsonObject)?.get("definition")?.jsonPrimitive?.content }
            val plain = definitions.filterNot { isFormOf(it) }.map { HtmlText.toPlainText(it) }.filter { it.isNotBlank() }
            if (plain.isNotEmpty()) return Parsed(plain.take(MAX_DEFINITIONS).joinToString("; "), null)
            if (lemma == null) lemma = definitions.firstNotNullOfOrNull { lemmaOf(it) }
        }
        return Parsed(null, lemma)
    }

    private fun isFormOf(html: String): Boolean = html.contains("form-of-definition")

    /** The linked lemma of a form-of definition such as "plural of <a title="gato">gato</a>". */
    private fun lemmaOf(html: String): String? {
        if (!isFormOf(html)) return null
        val linked = LINK_TITLE.findAll(html).map { it.groupValues[1] }.firstOrNull { it.isNotBlank() }
        if (linked != null) return linked
        return HtmlText.toPlainText(html).substringAfter(" of ", "").substringBefore(" (").trim().ifEmpty { null }
    }

    private companion object {
        const val MAX_DEFINITIONS = 3
        val LINK_TITLE = Regex("""<a\b[^>]*\btitle="([^"#]+)""")
    }
}
