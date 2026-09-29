package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.TranslationEngine
import com.tayra.languages.core.domain.service.effectiveEngine
import com.tayra.languages.core.domain.service.TermSuggestion
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Word translation from the MyMemory translation memory (free, no key; a contact email
 * raises the daily quota). Translates from the term's language into the language chosen
 * in settings.
 */
class MyMemoryTranslationProvider(
    private val client: HttpClient,
    private val settings: SettingsRepository,
    private val baseUrl: String = "https://api.mymemory.translated.net/get",
) : TermTranslationProvider, SentenceTranslator {

    private val json = Json { ignoreUnknownKeys = true }

    override val name: String = "MyMemory"

    override suspend fun suggestTranslation(text: String, language: Language): String? = request(text, language)

    /** Whole sentences use the same endpoint; MyMemory rejects queries over 500 characters. */
    override suspend fun translate(text: String, language: Language): String? =
        if (text.length > MAX_QUERY_LENGTH) null else request(text, language)

    private suspend fun request(text: String, language: Language): String? {
        val query = text.replace(ZWS_STRING, "").trim()
        val source = LanguageCodes.codeFor(language.name) ?: return null
        val target = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        if (query.isEmpty() || source == target) return null
        val body = try {
            val response = client.get(baseUrl) {
                parameter("q", query)
                parameter("langpair", "$source|$target")
                settings.current.translationContactEmail.trim().takeIf { it.isNotEmpty() }?.let { parameter("de", it) }
            }
            if (response.status != HttpStatusCode.OK) return null
            response.bodyAsText()
        } catch (e: Exception) {
            Logger.w(e) { "MyMemory lookup failed for $query" }
            return null
        }
        return runCatching { extract(body, query) }
            .onFailure { Logger.w(it) { "Could not parse MyMemory response for $query" } }
            .getOrNull()
    }

    internal fun extract(body: String, query: String): String? {
        val root = json.parseToJsonElement(body).jsonObject
        if (root["responseStatus"]?.jsonPrimitive?.content != "200") return null
        val translated = root["responseData"]?.jsonObject?.get("translatedText")?.jsonPrimitive?.content?.trim() ?: return null
        val upper = translated.uppercase()
        val isServiceMessage = SERVICE_MESSAGE_MARKERS.any { upper.contains(it) }
        if (translated.isEmpty() || isServiceMessage || translated.equals(query, ignoreCase = true)) return null
        return translated
    }

    private companion object {
        const val MAX_QUERY_LENGTH = 480
        val SERVICE_MESSAGE_MARKERS = listOf("PLEASE SELECT", "QUERY LENGTH LIMIT", "MYMEMORY WARNING", "INVALID LANGUAGE")
    }
}

/**
 * Chooses the dictionary by target language: English glosses come from Wiktionary with
 * MyMemory as fallback; other target languages can only use MyMemory.
 */
class TranslationSuggestionProvider(
    private val wiktionary: TermTranslationProvider,
    private val myMemory: TermTranslationProvider,
    private val google: TermTranslationProvider,
    private val azure: TermTranslationProvider,
    private val alibaba: TermTranslationProvider,
    private val baidu: TermTranslationProvider,
    private val settings: SettingsRepository,
    private val local: LocalTranslation,
) : TermTranslationProvider {
    override val name: String = "Suggestions"

    override suspend fun suggestTranslation(text: String, language: Language): String? = suggest(text, language)?.text

    /**
     * The chosen engine goes first: Argos when its models for the pair are installed (the term
     * stays offline), Google with a key; then the free online chain.
     */
    override suspend fun suggest(text: String, language: Language): TermSuggestion? {
        val target = settings.current.nativeLanguage.trim().lowercase()
        val argos = local.translator
        when (settings.current.effectiveEngine(argos != null)) {
            TranslationEngine.ARGOS -> {
                val source = LanguageCodes.codeFor(language.name)
                if (argos != null && source != null && argos.canTranslate(source, target.ifEmpty { "en" })) {
                    argos.translate(text, language)?.let { return TermSuggestion(it, "Argos Translate") }
                }
            }
            TranslationEngine.GOOGLE -> google.suggest(text, language)?.let { return it }
            TranslationEngine.AZURE -> azure.suggest(text, language)?.let { return it }
            TranslationEngine.ALIBABA -> alibaba.suggest(text, language)?.let { return it }
            TranslationEngine.BAIDU -> baidu.suggest(text, language)?.let { return it }
            TranslationEngine.MYMEMORY -> {}
        }
        val providers = if (target.isEmpty() || target == "en") listOf(wiktionary, myMemory) else listOf(myMemory)
        for (provider in providers) provider.suggest(text, language)?.let { return it }
        return null
    }
}
