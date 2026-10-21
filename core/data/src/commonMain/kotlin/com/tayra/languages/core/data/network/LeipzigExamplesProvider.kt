package com.tayra.languages.core.data.network

import co.touchlab.kermit.Logger
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.service.CorpusFrequency
import com.tayra.languages.core.domain.service.CorpusWords
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.ExampleSource
import com.tayra.languages.core.domain.service.ExampleSourceFeatures
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.random.Random
import kotlin.time.TimeSource

/** How a next page of [LeipzigExamplesProvider] starts, so a router can tell it from Tatoeba's links. */
const val LEIPZIG_PAGE_PREFIX = "leipzig:"

/**
 * Example sentences from the Leipzig Corpora Collection (Universität Leipzig, CC BY 4.0): news and
 * Wikipedia sentences found by the exact form of a single word, with the article each came from.
 * The service has no translations, recordings or filters, and pages through a word's sentences in
 * alphabetical order, so the word-count filter and the sorts work on the sentences fetched for a page.
 */
class LeipzigExamplesProvider(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.wortschatz-leipzig.de/ws",
    private val random: Random = Random.Default,
) : ExampleSentencesProvider, CorpusWords {

    private val json = Json { ignoreUnknownKeys = true }
    private val corporaLock = Mutex()
    private var corpora: List<Corpus>? = null
    private var corporaFailedAt: TimeSource.Monotonic.ValueTimeMark? = null

    override suspend fun sources(language: Language): List<ExampleSourceFeatures> =
        if (corpusFor(language) != null) listOf(FEATURES) else emptyList()

    override suspend fun search(query: ExampleSearchQuery): ExampleSearchResult {
        val word = query.text.replace(ZWS_STRING, "").trim()
        if (word.isEmpty() || word.any { it.isWhitespace() }) return ExampleSearchResult.EMPTY
        val corpus = corpusFor(query.language) ?: return ExampleSearchResult.EMPTY
        val chunk = chunkSize(query.limit, query.minWords, query.maxWords)
        val first = sentences(corpus, word, 0, chunk) ?: return ExampleSearchResult.EMPTY
        val prefetched = mutableMapOf(word to first)
        // The service matches case exactly: a word that starts sentences (or a German noun typed in lower case) is also looked up in its other case.
        val other = otherCase(word)
        if (first.count < FEW_RESULTS && other != null) sentences(corpus, other, 0, chunk)?.let { prefetched[other] = it }
        val forms = prefetched.map { (form, page) -> Form(form, page.count) }.filter { it.count > 0 }
        if (forms.isEmpty()) return ExampleSearchResult(emptyList(), 0, null)
        val cursor = Cursor(corpus, forms, query.sort, query.minWords, query.maxWords, query.limit, chunk)
        return if (query.sort == ExampleSort.RANDOM) randomPage(cursor, prefetched) else orderedPage(cursor, prefetched)
    }

    override suspend fun nextPage(nextPage: String, targetLanguage: String): ExampleSearchResult {
        val cursor = runCatching { Cursor.parse(nextPage.removePrefix(LEIPZIG_PAGE_PREFIX), json) }
            .onFailure { Logger.w(it) { "Not a Leipzig page: $nextPage" } }
            .getOrNull() ?: return ExampleSearchResult.EMPTY
        return if (cursor.sort == ExampleSort.RANDOM) randomPage(cursor, emptyMap()) else orderedPage(cursor, emptyMap())
    }

    override suspend fun frequency(word: String, language: Language): CorpusFrequency? {
        val corpus = corpusFor(language) ?: return null
        val body = fetch(word) { get("words", corpus, "word", word.trim()) } ?: return null
        return runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            CorpusFrequency(
                count = root["freq"]?.jsonPrimitive?.longOrNull ?: return null,
                rank = root["wordRank"]?.jsonPrimitive?.intOrNull,
                frequencyClass = root["frequencyClass"]?.jsonPrimitive?.intOrNull,
            )
        }.getOrNull()
    }

    override suspend fun cooccurrences(word: String, language: Language, limit: Int): List<String> {
        val text = word.trim()
        val corpus = corpusFor(language) ?: return emptyList()
        // Numbers and punctuation rank among the strongest neighbours, so more are asked for than are shown.
        val body = fetch(text) { get("cooccurrences", corpus, "cooccurrences", text, params = mapOf("limit" to (limit * 3).toString())) } ?: return emptyList()
        return runCatching {
            json.parseToJsonElement(body).jsonArray
                .mapNotNull { it.jsonObject["w2"]?.jsonObject?.get("word")?.jsonPrimitive?.contentOrNull }
                .filter { w -> w.any { it.isLetter() } && !w.equals(text, ignoreCase = true) }
                .distinctBy { it.lowercase() }
                .take(limit)
        }.getOrDefault(emptyList())
    }

    // ---- corpora

    /** The corpus examples in [language] come from, or null when the collection has none (or cannot be reached). */
    internal suspend fun corpusFor(language: Language): String? {
        val code = LanguageCodes.tatoebaCodeFor(language.name) ?: return null
        return chooseCorpus(corpora() ?: return null, code)
    }

    private suspend fun corpora(): List<Corpus>? = corporaLock.withLock {
        corpora?.let { return it }
        // A failed fetch waits a minute before the next try, so a screen opened offline does not ask every time.
        if (corporaFailedAt?.let { it.elapsedNow().inWholeMilliseconds < RETRY_CORPORA_MS } == true) return null
        val list = fetch("corpora") { get("corpora") }?.let { body -> runCatching { parseCorpora(body) }.getOrNull() }
        if (list.isNullOrEmpty()) corporaFailedAt = TimeSource.Monotonic.markNow() else corpora = list
        list?.takeIf { it.isNotEmpty() }
    }

    internal fun parseCorpora(body: String): List<Corpus> = json.parseToJsonElement(body).jsonArray.mapNotNull { element ->
        val item = element as? JsonObject ?: return@mapNotNull null
        val name = item["corpusName"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val match = CORPUS_NAME.matchEntire(name) ?: return@mapNotNull null
        Corpus(
            name = name,
            language = match.groupValues[1],
            country = match.groupValues[2].ifEmpty { null },
            genre = match.groupValues[3],
            year = match.groupValues[4].toInt(),
            sentences = item["numberOfSentences"]?.jsonPrimitive?.longOrNull ?: 0,
        )
    }

    internal data class Corpus(val name: String, val language: String, val country: String?, val genre: String, val year: Int, val sentences: Long) {
        val genreRank: Int get() = genre.lowercase().let { g ->
            when {
                g.startsWith("news") -> 0
                g.startsWith("wikipedia") -> 1
                g.startsWith("web") -> 2
                g.startsWith("community") -> 3
                else -> 4
            }
        }
    }

    // ---- pages

    private data class Form(val word: String, val count: Int)

    internal class Fetched(val count: Int, val sentences: List<ExampleSentence>)

    /** Where a search stands: the forms found and their counts, and how far the pages have gone ([position] in order, [usedSlots] at random). */
    private data class Cursor(
        val corpus: String,
        val forms: List<Form>,
        val sort: ExampleSort,
        val minWords: Int?,
        val maxWords: Int?,
        val limit: Int,
        val chunk: Int,
        /** The form and the offset in it the next ordered page starts at. */
        val position: Pair<Int, Int> = 0 to 0,
        /** The chunks of [chunk] sentences random pages have taken, numbered across the forms. */
        val usedSlots: Set<Int> = emptySet(),
    ) {
        val total: Int get() = forms.sumOf { it.count }

        /** Every chunk of every form, as the form and its offset. */
        val slots: List<Pair<Int, Int>> get() = forms.flatMapIndexed { index, form -> (0 until (form.count + chunk - 1) / chunk).map { index to it * chunk } }

        fun accepts(sentence: ExampleSentence): Boolean {
            val words = wordCount(sentence.text)
            return (minWords == null || words >= minWords) && (maxWords == null || words <= maxWords)
        }

        fun token(): String = LEIPZIG_PAGE_PREFIX + buildJsonObject {
            put("c", corpus)
            putJsonArray("f") { forms.forEach { addJsonArray { add(it.word); add(it.count) } } }
            put("s", sort.name)
            minWords?.let { put("min", it) }
            maxWords?.let { put("max", it) }
            put("l", limit)
            put("k", chunk)
            putJsonArray("p") { add(position.first); add(position.second) }
            putJsonArray("u") { usedSlots.sorted().forEach { add(it) } }
        }.toString()

        companion object {
            fun parse(body: String, json: Json): Cursor {
                val root = json.parseToJsonElement(body).jsonObject
                val position = root["p"]!!.jsonArray
                return Cursor(
                    corpus = root["c"]!!.jsonPrimitive.content,
                    forms = root["f"]!!.jsonArray.map { val pair = it.jsonArray; Form(pair[0].jsonPrimitive.content, pair[1].jsonPrimitive.int) },
                    sort = ExampleSort.valueOf(root["s"]!!.jsonPrimitive.content),
                    minWords = root["min"]?.jsonPrimitive?.intOrNull,
                    maxWords = root["max"]?.jsonPrimitive?.intOrNull,
                    limit = root["l"]!!.jsonPrimitive.int,
                    chunk = root["k"]!!.jsonPrimitive.int,
                    position = position[0].jsonPrimitive.int to position[1].jsonPrimitive.int,
                    usedSlots = root["u"]!!.jsonArray.map { it.jsonPrimitive.int }.toSet(),
                )
            }
        }
    }

    /**
     * The next sentences in the corpus' order that pass the word-count filter, read in up to
     * [MAX_ROUNDS] requests, then sorted among themselves; the page ends right after the last one kept.
     */
    private suspend fun orderedPage(cursor: Cursor, prefetched: Map<String, Fetched>): ExampleSearchResult {
        val kept = mutableListOf<ExampleSentence>()
        var (formIndex, offset) = cursor.position
        var rounds = 0
        while (formIndex < cursor.forms.size && kept.size < cursor.limit && rounds < MAX_ROUNDS) {
            val form = cursor.forms[formIndex]
            val fetched = prefetched[form.word]?.takeIf { offset == 0 } ?: sentences(cursor.corpus, form.word, offset, cursor.chunk).also { rounds++ }
            if (fetched == null) break
            var consumed = fetched.sentences.size
            for ((i, sentence) in fetched.sentences.withIndex()) {
                if (!cursor.accepts(sentence)) continue
                kept += sentence
                if (kept.size == cursor.limit) { consumed = i + 1; break }
            }
            offset += consumed
            if (fetched.sentences.size < cursor.chunk && consumed == fetched.sentences.size || offset >= form.count) {
                formIndex++
                offset = 0
            }
        }
        val next = if (formIndex < cursor.forms.size) cursor.copy(position = formIndex to offset).token() else null
        return ExampleSearchResult(sortWithin(kept.distinctBy { it.text }, cursor.sort), cursor.total, next)
    }

    /** A few chunks at random places among the forms' sentences, fetched together and shuffled; a chunk is never taken twice. */
    private suspend fun randomPage(cursor: Cursor, prefetched: Map<String, Fetched>): ExampleSearchResult {
        val slots = cursor.slots
        val free = slots.indices.filter { it !in cursor.usedSlots }
        if (free.isEmpty()) return ExampleSearchResult(emptyList(), cursor.total, null)
        val wanted = if (cursor.minWords == null && cursor.maxWords == null) 2 else RANDOM_CHUNKS
        val picked = free.shuffled(random).take(wanted)
        val fetched = coroutineScope {
            picked.map { index ->
                val (formIndex, offset) = slots[index]
                val form = cursor.forms[formIndex]
                async { prefetched[form.word]?.takeIf { offset == 0 } ?: sentences(cursor.corpus, form.word, offset, cursor.chunk) }
            }.awaitAll()
        }
        val sentences = fetched.filterNotNull().flatMap { it.sentences }.filter(cursor::accepts).distinctBy { it.text }.shuffled(random).take(cursor.limit)
        val used = cursor.usedSlots + picked
        val next = if (used.size < slots.size) cursor.copy(usedSlots = used).token() else null
        return ExampleSearchResult(sentences, cursor.total, next)
    }

    private fun sortWithin(sentences: List<ExampleSentence>, sort: ExampleSort): List<ExampleSentence> = when (sort) {
        ExampleSort.SHORTEST -> sentences.sortedWith(compareBy({ wordCount(it.text) }, { it.text.length }))
        ExampleSort.LONGEST -> sentences.sortedWith(compareByDescending<ExampleSentence> { wordCount(it.text) }.thenByDescending { it.text.length })
        ExampleSort.NEWEST -> sentences.sortedWith(compareBy(nullsLast(reverseOrder())) { it.date })
        ExampleSort.OLDEST -> sentences.sortedWith(compareBy(nullsLast()) { it.date })
        else -> sentences
    }

    /** One request for [limit] of [word]'s sentences from [offset]; a 404 (no such word, or past the end) is none. Null when the service failed. */
    private suspend fun sentences(corpus: String, word: String, offset: Int, limit: Int): Fetched? {
        val response = request(word) {
            get("sentences", corpus, "sentences", word, params = mapOf("offset" to offset.toString(), "limit" to limit.toString()))
        } ?: return null
        if (response.status == HttpStatusCode.NotFound) return Fetched(0, emptyList())
        if (response.status != HttpStatusCode.OK) return null
        return runCatching { extract(response.bodyAsText()) }
            .onFailure { if (it is CancellationException) throw it; Logger.w(it) { "Could not parse Leipzig sentences for $word" } }
            .getOrNull()
    }

    internal fun extract(body: String): Fetched {
        val root = json.parseToJsonElement(body).jsonObject
        val sentences = (root["sentences"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val text = item["sentence"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (text.isEmpty()) return@mapNotNull null
            val source = item["source"] as? JsonObject
            ExampleSentence(
                text = text,
                translation = null,
                audioUrl = null,
                sourceUrl = source?.get("url")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                date = source?.get("date")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
            )
        }
        return Fetched(root["count"]?.jsonPrimitive?.long?.toInt() ?: sentences.size, sentences)
    }

    // ---- requests

    private suspend fun get(vararg segments: String, params: Map<String, String> = emptyMap()): HttpResponse = client.get {
        url {
            takeFrom(baseUrl)
            appendPathSegments(segments.toList(), encodeSlash = true)
            params.forEach { (name, value) -> parameters.append(name, value) }
        }
    }

    private suspend fun request(what: String, call: suspend () -> HttpResponse): HttpResponse? = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w(e) { "Leipzig lookup failed for $what" }
        null
    }

    private suspend fun fetch(what: String, call: suspend () -> HttpResponse): String? {
        val response = request(what, call) ?: return null
        return if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
    }

    internal companion object {
        val FEATURES = ExampleSourceFeatures(
            ExampleSource.LEIPZIG,
            audio = false,
            sorts = listOf(ExampleSort.RANDOM, ExampleSort.SHORTEST, ExampleSort.LONGEST, ExampleSort.NEWEST, ExampleSort.OLDEST),
            translations = false,
            phrases = false,
        )

        private val CORPUS_NAME = Regex("""([a-z]{3})(?:-([a-z]+))?_([^_]+)_(\d{4})(?:_.*)?""")
        private val WHITESPACE = Regex("""\s+""")

        /** Below this many sentences the word's other case is looked up too. */
        const val FEW_RESULTS = 50
        const val MAX_ROUNDS = 3
        const val RANDOM_CHUNKS = 3
        const val MAX_CHUNK = 100
        const val RETRY_CORPORA_MS = 60_000L

        /** The corpus for an ISO 639-3 [code]: news first, then Wikipedia, web and community texts; one for no country first; then the largest and newest. */
        fun chooseCorpus(corpora: List<Corpus>, code: String): String? = corpora.filter { it.language == code }
            .minWithOrNull(compareBy<Corpus>({ it.genreRank }, { it.country != null }).thenByDescending { it.sentences }.thenByDescending { it.year })
            ?.name

        /** A filtered page reads three times its size, as only some sentences pass. */
        fun chunkSize(limit: Int, minWords: Int?, maxWords: Int?): Int =
            (if (minWords == null && maxWords == null) limit else limit * 3).coerceIn(1, MAX_CHUNK)

        fun wordCount(text: String): Int = text.split(WHITESPACE).count { w -> w.any { it.isLetterOrDigit() } }

        fun otherCase(word: String): String? {
            val first = word.first()
            val other = if (first.isUpperCase()) first.lowercaseChar() else first.uppercaseChar()
            return if (other == first) null else other + word.drop(1)
        }
    }
}
