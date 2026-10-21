package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.dictionary.DictionaryExample
import com.tayra.languages.core.domain.dictionary.OnlineWiktionary
import com.tayra.languages.core.domain.dictionary.WiktionaryAudio
import com.tayra.languages.core.domain.dictionary.WiktionaryCodes
import com.tayra.languages.core.domain.dictionary.WiktionaryEntry
import com.tayra.languages.core.domain.dictionary.WiktionaryForm
import com.tayra.languages.core.domain.dictionary.WiktionaryForms
import com.tayra.languages.core.domain.dictionary.WiktionaryLemma
import com.tayra.languages.core.domain.dictionary.WiktionaryPage
import com.tayra.languages.core.domain.dictionary.WiktionaryPronunciation
import com.tayra.languages.core.domain.dictionary.WiktionarySearchResult
import com.tayra.languages.core.domain.dictionary.WiktionarySense
import com.tayra.languages.core.domain.repository.SentenceTranslationCache
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

/**
 * Wiktionary through WiktApi (api.wiktapi.dev): a word's page, its forms and a prefix search.
 *
 * Answers are kept in memory for the session and in [store] for a day, misses included, so a word
 * opened again asks nobody. Requests are plain GETs with no headers of their own, one at a time and
 * spaced out; a failed connection stops all requests for a while, as when the device is offline.
 * WiktApi matches words exactly, so a capitalised word it lacks is asked again in lower case.
 */
class WiktApiClient(
    private val client: HttpClient,
    private val store: SentenceTranslationCache? = null,
    private val baseUrl: String = "https://api.wiktapi.dev/v1",
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val minIntervalMs: Long = 150,
    /** A browser reports WiktApi's misses as failed requests, so there a failure is a miss for the session rather than a sign of being offline. */
    private val failureIsMiss: Boolean = false,
) : OnlineWiktionary {

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    /** Bodies by request; an empty one is a miss. */
    private val memory = LinkedHashMap<String, String>()
    private var lastRequestAt = 0L
    private var offlineUntil = 0L

    override suspend fun page(word: String, languageCode: String, nativeLanguage: String): WiktionaryPage? {
        val text = word.trim()
        if (text.isEmpty() || text.length > MAX_WORD_LENGTH) return null
        val lang = WiktionaryCodes.languageCode(languageCode)
        for (edition in WiktionaryCodes.editionsFor(nativeLanguage)) {
            for (candidate in listOf(text, text.lowercase()).distinct()) {
                val body = fetch("$edition/word/${candidate.encodeURLPathPart()}?lang=$lang") ?: continue
                val page = runCatching { parsePage(body, candidate, edition, lang) }
                    .onFailure { Logger.w(it) { "Could not read WiktApi's page for $candidate" } }.getOrNull()
                if (page != null && page.entries.isNotEmpty()) return page
            }
        }
        return null
    }

    override suspend fun forms(word: String, languageCode: String, edition: String): List<WiktionaryForms> {
        val lang = WiktionaryCodes.languageCode(languageCode)
        val body = fetch("$edition/word/${word.trim().encodeURLPathPart()}/forms?lang=$lang") ?: return emptyList()
        return runCatching { parseForms(body, lang) }.getOrElse { emptyList() }
    }

    override suspend fun search(prefix: String, languageCode: String, edition: String): List<WiktionarySearchResult> {
        val lang = WiktionaryCodes.languageCode(languageCode)
        val body = fetch("$edition/search?q=${prefix.trim().encodeURLParameter()}&lang=$lang") ?: return emptyList()
        return runCatching {
            json.parseToJsonElement(body).jsonObject["results"].array().mapNotNull { element ->
                val result = element as? JsonObject ?: return@mapNotNull null
                val found = result.string("word") ?: return@mapNotNull null
                if (result.string("lang_code")?.let { it != lang } == true) return@mapNotNull null
                WiktionarySearchResult(found, result.string("pos").orEmpty())
            }
        }.getOrElse { emptyList() }
    }

    /** The body for [path], from memory, the store or the network; null on a miss or when the network fails. */
    private suspend fun fetch(path: String): String? = lock.withLock {
        memory[path]?.let { cached ->
            memory.remove(path); memory[path] = cached
            return cached.ifEmpty { null }
        }
        val at = now()
        runCatching { store?.get(path, STORE_KEY, at - STORE_TTL_MS) }.getOrNull()?.let { stored ->
            remember(path, stored)
            return stored.ifEmpty { null }
        }
        if (at < offlineUntil) return null
        val wait = minIntervalMs - (at - lastRequestAt)
        if (wait > 0) delay(wait)
        val body = try {
            val response = client.get("$baseUrl/$path")
            lastRequestAt = now()
            when (response.status) {
                HttpStatusCode.OK -> response.bodyAsText()
                HttpStatusCode.NotFound -> ""
                else -> return null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "WiktApi request failed for $path" }
            lastRequestAt = now()
            if (failureIsMiss) remember(path, "") else offlineUntil = lastRequestAt + OFFLINE_PAUSE_MS
            return null
        }
        remember(path, body)
        runCatching { store?.put(path, STORE_KEY, body, lastRequestAt) }
        body.ifEmpty { null }
    }

    private fun remember(path: String, body: String) {
        memory[path] = body
        while (memory.size > MAX_MEMORY) memory.remove(memory.keys.first())
    }

    internal fun parsePage(body: String, requested: String, edition: String, lang: String): WiktionaryPage {
        val root = json.parseToJsonElement(body).jsonObject
        val entries = root["entries"].array().mapNotNull { (it as? JsonObject)?.let(::parseEntry) }
        return WiktionaryPage(root.string("word") ?: requested, root.string("edition") ?: edition, lang, entries)
    }

    private fun parseEntry(entry: JsonObject): WiktionaryEntry {
        val senses = entry["senses"].array().mapNotNull { (it as? JsonObject)?.let(::parseSense) }
        val sounds = entry["sounds"].array().mapNotNull { it as? JsonObject }
        val forms = entry["forms"].array().mapNotNull { (it as? JsonObject)?.let(::parseForm) }
        val pronunciations = sounds.mapNotNull { sound -> sound.string("ipa")?.let { WiktionaryPronunciation(it, sound.strings("tags")) } }
        val audio = sounds.mapNotNull { sound ->
            val file = sound.string("audio") ?: return@mapNotNull null
            val mp3 = sound.string("mp3_url")
            val ogg = sound.string("ogg_url")
            if (mp3 == null && ogg == null) null else WiktionaryAudio(file, mp3, ogg, sound.strings("tags"))
        }
        val categories = entry["senses"].array().mapNotNull { it as? JsonObject }.flatMap { it.strings("categories") }
        return WiktionaryEntry(posOf(categories, senses, forms), senses, pronunciations, audio, forms)
    }

    private fun parseSense(sense: JsonObject): WiktionarySense? {
        val glosses = sense.strings("glosses")
        if (glosses.isEmpty()) return null
        // A lemma's page title is what its link points to: "кошка" for the stressed "ко́шка".
        val links = sense["links"].array().mapNotNull { link -> (link as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content } }
            .filter { it.size >= 2 }.associate { it[0] to it[1].substringBefore('#') }
        val formOf = sense["form_of"].array().mapNotNull { (it as? JsonObject)?.string("word") }
            .map { shown -> WiktionaryLemma(shown, links[shown]?.takeIf { it.isNotBlank() } ?: shown) }
        val examples = sense["examples"].array().mapNotNull { element ->
            val example = element as? JsonObject ?: return@mapNotNull null
            if (example.string("type") == "quotation") return@mapNotNull null
            val text = example.string("text")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            DictionaryExample(text, (example.string("translation") ?: example.string("english"))?.takeIf { it.isNotBlank() })
        }
        val synonyms = sense["synonyms"].array().mapNotNull { (it as? JsonObject)?.string("word") }
        return WiktionarySense(glosses, sense.strings("tags"), examples, synonyms, formOf)
    }

    private fun parseForm(form: JsonObject): WiktionaryForm? {
        val text = form.string("form")?.takeIf { it.isNotBlank() } ?: return null
        return WiktionaryForm(text, form.strings("tags"), form.string("source"))
    }

    internal fun parseForms(body: String, lang: String): List<WiktionaryForms> =
        json.parseToJsonElement(body).jsonObject["forms"].array().mapNotNull { element ->
            val group = element as? JsonObject ?: return@mapNotNull null
            if (group.string("lang_code")?.let { it != lang } == true) return@mapNotNull null
            WiktionaryForms(group.string("pos").orEmpty(), group["forms"].array().mapNotNull { (it as? JsonObject)?.let(::parseForm) })
        }

    /**
     * The page's entries carry no part of speech; their categories usually name it ("Portuguese
     * verb forms", "German nouns"), else a gender or a conjugation table gives it away.
     */
    private fun posOf(categories: List<String>, senses: List<WiktionarySense>, forms: List<WiktionaryForm>): String {
        for (category in categories) {
            val words = category.lowercase()
            POS_BY_CATEGORY.firstOrNull { (suffix, _) -> words.endsWith(" $suffix") || words.endsWith(" $suffix forms") }?.let { return it.second }
        }
        val tags = senses.flatMap { it.tags }
        return when {
            "proper-noun" in tags -> "name"
            forms.any { it.source == "conjugation" } -> "verb"
            tags.any { it in GENDERS } || forms.any { it.source == "declension" } -> "noun"
            else -> ""
        }
    }

    private fun JsonElement?.array(): List<JsonElement> = (this as? JsonArray).orEmpty()

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.strings(key: String): List<String> = this[key].array().mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }

    private companion object {
        /** Kept apart from the sentence translations by the target language it is stored under. */
        const val STORE_KEY = "wiktapi"
        const val STORE_TTL_MS = 24L * 60 * 60 * 1000
        const val OFFLINE_PAUSE_MS = 60_000L
        const val MAX_MEMORY = 300
        const val MAX_WORD_LENGTH = 60
        val GENDERS = setOf("masculine", "feminine", "neuter", "common")

        /** Category endings and the part of speech they name, as kaikki names it; the longer endings first. */
        val POS_BY_CATEGORY = listOf(
            "proper nouns" to "name", "nouns" to "noun", "verbs" to "verb", "verb" to "verb", "adjectives" to "adj", "adjective" to "adj",
            "adverbs" to "adv", "pronouns" to "pron", "pronoun" to "pron", "prepositions" to "prep", "conjunctions" to "conj",
            "interjections" to "intj", "numerals" to "num", "particles" to "particle", "determiners" to "det",
            "articles" to "article", "phrases" to "phrase", "idioms" to "phrase", "proverbs" to "proverb", "noun" to "noun",
        )
    }
}
