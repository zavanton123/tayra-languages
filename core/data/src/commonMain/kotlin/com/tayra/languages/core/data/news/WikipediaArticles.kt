package com.tayra.languages.core.data.news

import com.tayra.languages.core.domain.news.NewsArticle
import com.tayra.languages.core.domain.news.NewsSource
import com.tayra.languages.core.domain.news.WikiPage
import com.tayra.languages.core.domain.news.WikipediaReader
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.time.Clock

/**
 * Articles of the language's Wikipedia: the most read of yesterday, from Wikimedia's page views,
 * or those its search finds. A page of them is one request, which gives each article's
 * introduction as plain text, so an article is judged and read by its introduction without more;
 * its whole text is a request of its own. Pages that only list the meanings of a word go, and the
 * most read leave out adult topics, which Wikidata tells apart (a search finds what it is asked for).
 */
class WikipediaArticles(
    private val client: HttpClient,
    private val clock: Clock = Clock.System,
    private val pageViewsUrl: String = "https://wikimedia.org/api/rest_v1/metrics/pageviews/top/",
    private val wikiHost: (String) -> String = { code -> "https://$code.wikipedia.org" },
) : WikipediaReader {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private val mostRead = HashMap<String, List<String>>()

    override suspend fun mostRead(languageCode: String, page: Int): WikiPage {
        val code = wikiCode(languageCode)
        val titles = lock.withLock { mostRead[code] } ?: readMostRead(code).also { lock.withLock { mostRead[code] = it } }
        val batch = titles.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE)
        return WikiPage(described(code, languageCode, batch, forEveryone = true), hasMore = titles.size > page * PAGE_SIZE)
    }

    override suspend fun search(languageCode: String, query: String, page: Int): WikiPage {
        val code = wikiCode(languageCode)
        val body = api(code) {
            parameter("action", "query")
            parameter("list", "search")
            parameter("srsearch", query)
            parameter("srnamespace", "0")
            parameter("srlimit", PAGE_SIZE)
            parameter("srprop", "")
            if (page > 1) parameter("sroffset", (page - 1) * PAGE_SIZE)
        }
        val titles = (body.obj("query")?.get("search") as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.string("title") }
        return WikiPage(described(code, languageCode, titles, forEveryone = false), hasMore = body["continue"] != null)
    }

    override suspend fun wholeText(article: NewsArticle): String {
        val code = Regex("""https://([a-z-]+)\.wikipedia\.org""").find(article.url)?.groupValues?.get(1) ?: error("Not a Wikipedia article: ${article.url}")
        val page = api(code) {
            parameter("action", "query")
            parameter("prop", "extracts")
            parameter("explaintext", "1")
            parameter("exsectionformat", "wiki")
            parameter("redirects", "1")
            parameter("titles", article.title)
            variant(code)
        }.obj("query")?.let { it["pages"] as? JsonArray }?.firstOrNull() as? JsonObject
        val text = page?.string("extract")?.let(::clean).orEmpty()
        if (text.isBlank()) throw IllegalStateException("Wikipedia has no text for \"${article.title}\"")
        return text
    }

    /** The titles of the wiki's most read articles of the last day with figures, without its main page and the pages of other namespaces. */
    private suspend fun readMostRead(code: String): List<String> {
        val site = api(code) {
            parameter("action", "query")
            parameter("meta", "siteinfo")
            parameter("siprop", "general|namespaces|namespacealiases")
        }.obj("query")
        val mainPage = site?.obj("general")?.string("mainpage")
        // Every name a namespace goes by, so "Especial:Pesquisar" and "Wikipédia:Sobre" are known for what they are.
        val namespaces = buildSet {
            site?.obj("namespaces")?.values?.forEach { ns -> (ns as? JsonObject)?.let { o -> listOf("name", "canonical").forEach { o.string(it)?.let(::add) } } }
            (site?.get("namespacealiases") as? JsonArray)?.forEach { (it as? JsonObject)?.string("alias")?.let(::add) }
        }.filter { it.isNotEmpty() }.map { it.lowercase() }.toSet()
        val today = clock.now().toLocalDateTime(TimeZone.UTC).date
        // A day's figures appear some hours after it ends.
        for (back in 1..3) {
            val day = today.minus(back, DateTimeUnit.DAY)
            val response = client.get("$pageViewsUrl$code.wikipedia/all-access/${day.year}/${pad(day.month.ordinal + 1)}/${pad(day.day)}") {
                header(HttpHeaders.UserAgent, FeedNewsReader.USER_AGENT)
                timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
            }
            if (response.status == HttpStatusCode.NotFound) continue
            val articles = (json.parseToJsonElement(checked(response).bodyAsText()) as? JsonObject)?.let { it["items"] as? JsonArray }?.firstOrNull()?.let { (it as? JsonObject)?.get("articles") as? JsonArray }.orEmpty()
            return articles.mapNotNull { (it as? JsonObject)?.string("article")?.replace('_', ' ') }
                .filter { title -> title != mainPage && title != "-" && title.substringBefore(':', "").lowercase() !in namespaces }
        }
        return emptyList()
    }

    /** The articles with [titles], in their order, each with its introduction; lists of a word's meanings left out, and with [forEveryone] adult topics too. */
    private suspend fun described(code: String, languageCode: String, titles: List<String>, forEveryone: Boolean): List<NewsArticle> {
        if (titles.isEmpty()) return emptyList()
        val query = api(code) {
            parameter("action", "query")
            parameter("prop", "extracts|info|pageprops")
            parameter("exintro", "1")
            parameter("explaintext", "1")
            parameter("exlimit", PAGE_SIZE)
            parameter("ppprop", "disambiguation|wikibase_item")
            parameter("redirects", "1")
            parameter("titles", titles.joinToString("|"))
            variant(code)
        }.obj("query")
        // The wiki answers under its own spelling of a title, and under the page a redirect leads to.
        val renamed = listOf("normalized", "redirects").flatMap { key -> (query?.get(key) as? JsonArray).orEmpty().mapNotNull { it as? JsonObject } }
            .associate { it.string("from").orEmpty() to it.string("to").orEmpty() }
        val pages = (query?.get("pages") as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.associateBy { it.string("title").orEmpty() }
        val source = NewsSource(languageCode, "Wikipedia", wikiHost(code), "${wikiHost(code)}/", textInFeed = true)
        val adult = if (forEveryone) adultItems(pages.values.mapNotNull { it.obj("pageprops")?.string("wikibase_item") }) else emptySet()
        return titles.map { generateSequence(it) { title -> renamed[title] }.take(4).last() }.distinct().mapNotNull { title ->
            val page = pages[title] ?: return@mapNotNull null
            if (page["missing"] != null || page.obj("pageprops")?.get("disambiguation") != null) return@mapNotNull null
            if (page.obj("pageprops")?.string("wikibase_item") in adult) return@mapNotNull null
            val intro = page.string("extract")?.let(::clean)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            NewsArticle(
                source = source,
                url = "${wikiHost(code)}/wiki/${title.replace(' ', '_').encodeURLPathPart()}",
                title = title,
                summary = intro.take(SUMMARY_CHARACTERS),
                published = null,
                feedText = intro,
                length = (page["length"] as? JsonPrimitive)?.intOrNull,
            )
        }
    }

    /**
     * The ones of [items] about adult topics, by what Wikidata says of them: a performer in
     * pornography (occupation), a film or comic of an adult genre, or a sexual act, practice or
     * adult business, or a kind of one (instance or subclass, one step up).
     */
    private suspend fun adultItems(items: List<String>): Set<String> {
        if (items.isEmpty()) return emptySet()
        val response = client.get(WIKIDATA) {
            parameter("action", "wbgetentities")
            parameter("ids", items.distinct().joinToString("|"))
            parameter("props", "claims")
            parameter("format", "json")
            parameter("origin", "*")
            header(HttpHeaders.UserAgent, FeedNewsReader.USER_AGENT)
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        val entities = (json.parseToJsonElement(checked(response).bodyAsText()) as? JsonObject)?.obj("entities") ?: return emptySet()
        return entities.entries.filter { (id, entity) ->
            val claims = (entity as? JsonObject)?.obj("claims") ?: return@filter false
            fun ids(property: String) = (claims[property] as? JsonArray).orEmpty().mapNotNull { claim ->
                ((claim as? JsonObject)?.obj("mainsnak")?.obj("datavalue")?.obj("value"))?.string("id")
            }
            id in ADULT_TOPICS || ids("P106").any { it in ADULT_OCCUPATIONS } || ids("P136").any { it in ADULT_GENRES } ||
                (ids("P31") + ids("P279")).any { it in ADULT_TOPICS }
        }.map { it.key }.toSet()
    }

    private suspend fun api(code: String, params: HttpRequestBuilder.() -> Unit): JsonObject {
        val response = client.get("${wikiHost(code)}/w/api.php") {
            params()
            parameter("format", "json")
            parameter("formatversion", "2")
            // Lets the web build read the wiki too.
            parameter("origin", "*")
            header(HttpHeaders.UserAgent, FeedNewsReader.USER_AGENT)
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        val body = json.parseToJsonElement(checked(response).bodyAsText()) as? JsonObject ?: throw IllegalStateException("Wikipedia gave no answer")
        body.obj("error")?.string("info")?.let { throw IllegalStateException(it) }
        return body
    }

    /** Chinese Wikipedia is written in both scripts; asked for one, it gives the simplified. */
    private fun HttpRequestBuilder.variant(code: String) {
        if (code == "zh") parameter("variant", "zh-hans")
    }

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Wikipedia answered ${response.status.value}")
        return response
    }

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    companion object {
        private const val PAGE_SIZE = 20
        private const val SUMMARY_CHARACTERS = 400
        private const val TIMEOUT = 30_000L
        private const val WIKIDATA = "https://www.wikidata.org/w/api.php"

        /** Pornographic actor, erotic model, webcam model, sex worker, prostitute. */
        private val ADULT_OCCUPATIONS = setOf("Q488111", "Q3286043", "Q1027930", "Q852857", "Q14915751")

        /** Pornographic film, hentai, erotica, mature film. */
        private val ADULT_GENRES = setOf("Q185529", "Q172067", "Q181001", "Q5193883")

        /**
         * Pornography and its kinds, human sexual behaviour, copulation, sexual penetration, sex
         * positions, autoeroticism, fetishism, paraphilia, sexual risk behaviour, erotic products,
         * adult sites, studios and subscription services, porn databases, and the topics no class
         * catches: orgasm, prostitution.
         */
        private val ADULT_TOPICS = setOf(
            "Q291", "Q185529", "Q172067", "Q181001", "Q608", "Q30600878", "Q2148678", "Q8394", "Q782623", "Q207791",
            "Q178059", "Q2990601", "Q115921406", "Q110643339", "Q30941437", "Q106040099", "Q90404711", "Q5887", "Q36633",
        )

        private fun pad(n: Int) = n.toString().padStart(2, '0')

        /** The wiki of a language, where its address is not its code. */
        internal fun wikiCode(languageCode: String): String = when (languageCode) {
            "yue" -> "zh-yue"
            else -> languageCode
        }

        private val HEADING = Regex("""^(=+)\s*(.*?)\s*=+$""")

        /**
         * An article's plain text as paragraphs, its headings kept, without the sections that are
         * empty or only list links (see also, references, external links): whatever their names in
         * the language, such sections are short lines with no sentence among them.
         */
        internal fun clean(extract: String): String {
            val sections = mutableListOf<Pair<String?, List<String>>>()
            var heading: String? = null
            var lines = mutableListOf<String>()
            for (raw in extract.lines()) {
                val line = raw.trim()
                val match = HEADING.matchEntire(line)
                if (match != null) {
                    sections += heading to lines
                    heading = match.groupValues[2]
                    lines = mutableListOf()
                } else if (line.isNotEmpty()) {
                    lines += line
                }
            }
            sections += heading to lines
            return sections.filter { (title, body) -> title == null || (body.isNotEmpty() && !isList(body)) }
                .flatMap { (title, body) -> listOfNotNull(title) + body }
                .joinToString("\n")
        }

        private fun isList(lines: List<String>): Boolean = lines.all { it.length < LIST_LINE && it.last() !in SENTENCE_ENDS }

        private const val LIST_LINE = 100
        private val SENTENCE_ENDS = setOf('.', '!', '?', '。', '！', '？', '…', '।', '։')
    }
}
