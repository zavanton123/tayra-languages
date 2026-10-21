package com.tayra.languages.core.data.discover

import com.tayra.languages.core.data.files.EpubText
import com.tayra.languages.core.data.files.FileTextExtractor
import com.tayra.languages.core.domain.books.discover.BookCatalog
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.CatalogPage
import com.tayra.languages.core.domain.books.discover.FoundBook
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

/**
 * The texts of Wikisource, one wiki per language (pt.wikisource.org...), transcribed by volunteers
 * from public-domain and freely licensed books.
 *
 * Without a search the list is the wiki's most read works of last month (Wikimedia's page views),
 * kept to those Wikidata calls literature, as the most read pages also hold laws, anthems and
 * prayers; a search uses the wiki's own, with a chapter found standing for its work. Wikidata
 * names the authors. A book is judged by its first chapter, read through the wiki's API; added, it
 * is the ePUB that Wikisource's export tool makes of the whole work, without the title page, the
 * table of contents and the credits the tool adds.
 */
class WikisourceCatalog(
    private val client: HttpClient,
    private val clock: Clock = Clock.System,
    private val exportUrl: String = "https://ws-export.wmcloud.org/",
    private val pageViewsUrl: String = "https://wikimedia.org/api/rest_v1/metrics/pageviews/top/",
    private val wikidataUrl: String = "https://www.wikidata.org/w/api.php",
    private val wikiHost: (String) -> String = { code -> "https://$code.wikisource.org" },
) : BookCatalog {
    override val source = BookSource.WIKISOURCE

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private val popular = HashMap<String, Popular>()

    /** A wiki's most read works, judged by Wikidata a batch at a time as the pages ask for them. */
    private class Popular(val titles: List<String>) {
        val works = mutableListOf<FoundBook>()
        var scanned = 0
    }

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage =
        if (query.isEmpty()) popular(languageCode, page) else searched(languageCode, query, page)

    override suspend fun sample(book: FoundBook): String {
        val code = book.languageCode()
        val root = parsedHtml(code, book.id)
        // A work in chapters lists them on its page; the first is the place to judge it.
        val chapter = SUBPAGE_LINK.findAll(root).map { decodeTitle(it.groupValues[1]) }.firstOrNull { it.startsWith("${book.id}/") }
        return htmlText(withoutNotices(if (chapter != null) parsedHtml(code, chapter) else root))
    }

    override suspend fun text(book: FoundBook): String {
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = EXPORT_TIMEOUT; socketTimeoutMillis = EXPORT_TIMEOUT }
        }
        val text = EpubText.read(checked(response).bodyAsBytes(), ::isWorkText).text
        if (text.split(Regex("\\s+")).count { word -> word.any { it.isLetter() } } < MIN_WORDS) throw IllegalStateException("This page has too little text to read")
        return text
    }

    private suspend fun popular(code: String, page: Int): CatalogPage {
        val list = lock.withLock { popular[code] } ?: Popular(mostRead(code)).also { lock.withLock { popular[code] = it } }
        // Wikidata judges the titles a batch at a time until the page is full or the list is through.
        while (list.works.size < page * PAGE_SIZE && list.scanned < list.titles.size) {
            val batch = list.titles.subList(list.scanned, minOf(list.titles.size, list.scanned + BATCH))
            list.works += described(code, batch, literatureOnly = true)
            list.scanned += batch.size
        }
        val from = (page - 1) * PAGE_SIZE
        val books = list.works.drop(from).take(PAGE_SIZE)
        return CatalogPage(books, hasMore = list.works.size > from + PAGE_SIZE || list.scanned < list.titles.size)
    }

    private suspend fun searched(code: String, query: String, page: Int): CatalogPage {
        val body = api(code) {
            parameter("action", "query")
            parameter("list", "search")
            parameter("srsearch", query)
            parameter("srnamespace", "0")
            parameter("srlimit", SEARCH_SIZE)
            parameter("srprop", "")
            if (page > 1) parameter("sroffset", (page - 1) * SEARCH_SIZE)
        }
        val hits = body.obj("query")?.array("search").orEmpty().mapNotNull { (it as? JsonObject)?.string("title") }
        val total = body.obj("query")?.obj("searchinfo")?.get("totalhits")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        // A chapter found stands for its work.
        val works = hits.map { it.substringBefore('/') }.distinct()
        return CatalogPage(described(code, works, literatureOnly = false), hasMore = body["continue"] != null, total = total)
    }

    /** The most read pages of the wiki in the last month with figures, the works among them in order. */
    private suspend fun mostRead(code: String): List<String> {
        val mainPage = api(code) {
            parameter("action", "query")
            parameter("meta", "siteinfo")
        }.obj("query")?.obj("general")?.string("mainpage")
        val today = clock.now().toLocalDateTime(TimeZone.UTC).date
        // A month's figures appear a day or two after it ends.
        for (back in 1..2) {
            val month = today.minus(back, DateTimeUnit.MONTH)
            val response = client.get("$pageViewsUrl$code.wikisource/all-access/${month.year}/${month.month.ordinal.plus(1).toString().padStart(2, '0')}/all-days") {
                header(HttpHeaders.UserAgent, USER_AGENT)
                timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
            }
            if (response.status == HttpStatusCode.NotFound) continue
            val articles = json.parseToJsonElement(checked(response).bodyAsText()).jsonObject.array("items")?.firstOrNull()?.jsonObject?.array("articles").orEmpty()
            return articles.mapNotNull { (it as? JsonObject)?.string("article")?.replace('_', ' ') }
                // Other namespaces have a colon in their names (Autor:, Página:), chapters a slash.
                .filter { ':' !in it && '/' !in it && it != mainPage }
        }
        return emptyList()
    }

    /**
     * The books for [titles], in their order: without the pages that list a work's versions, and
     * with [literatureOnly] without the ones Wikidata does not call literature or does not know.
     */
    private suspend fun described(code: String, titles: List<String>, literatureOnly: Boolean): List<FoundBook> = coroutineScope {
        // Titles in other scripts grow long once encoded, and the wiki refuses a long address.
        titles.chunked(TITLES_PER_REQUEST).map { async { describedBatch(code, it, literatureOnly) } }.awaitAll().flatten()
    }

    private suspend fun describedBatch(code: String, titles: List<String>, literatureOnly: Boolean): List<FoundBook> {
        if (titles.isEmpty()) return emptyList()
        val pages = api(code) {
            parameter("action", "query")
            parameter("prop", "pageprops")
            parameter("ppprop", "wikibase_item|disambiguation")
            parameter("redirects", "1")
            parameter("titles", titles.joinToString("|"))
        }.obj("query")
        // The wiki answers under its own spelling of a title, and under the page a redirect leads to.
        val renamed = listOf("normalized", "redirects").flatMap { key -> pages?.array(key).orEmpty().mapNotNull { it as? JsonObject } }
            .associate { it.string("from").orEmpty() to it.string("to").orEmpty() }
        fun final(title: String): String = generateSequence(title) { renamed[it] }.take(4).last()
        val props = pages?.array("pages").orEmpty().mapNotNull { it as? JsonObject }.filter { it["missing"] == null }
            .associate { page -> page.string("title").orEmpty() to page.obj("pageprops") }
        val items = props.values.mapNotNull { it?.string("wikibase_item") }
        val entities = entities(items, "claims")
        // An edition often names no author; the work it is an edition of does.
        val works = entities(entities.values.filter { claimIds(it, "P50").isEmpty() }.flatMap { claimIds(it, "P629") }.distinct(), "claims")
        fun authorsOf(entity: JsonObject): List<String> = claimIds(entity, "P50").ifEmpty { claimIds(entity, "P629").flatMap { work -> works[work]?.let { claimIds(it, "P50") }.orEmpty() } }
        val authorIds = entities.values.flatMap(::authorsOf).distinct()
        val authors = entities(authorIds, "labels").mapValues { (_, entity) -> label(entity, code) }
        return titles.map(::final).distinct().mapNotNull { title -> props.entries.firstOrNull { it.key == title }?.let { title to it.value } }
            .filter { (_, pageProps) -> pageProps?.get("disambiguation") == null }
            .mapNotNull { (title, pageProps) ->
                val entity = pageProps?.string("wikibase_item")?.let { entities[it] }
                if (literatureOnly && (entity == null || claimIds(entity, "P31").none { it in LITERATURE })) return@mapNotNull null
                val names = entity?.let { authorsOf(it).mapNotNull { id -> authors[id] } }.orEmpty()
                book(code, title, names)
            }
    }

    private fun book(code: String, title: String, authors: List<String>): FoundBook {
        val path = title.replace(' ', '_').encodeURLPathPart()
        return FoundBook(
            source = source,
            id = title,
            title = withoutAuthor(title, authors),
            authors = authors,
            description = "",
            subjects = emptyList(),
            license = "CC BY-SA",
            pageUrl = "${wikiHost(code)}/wiki/$path",
            downloadUrl = "${exportUrl}?format=epub-3&lang=$code&page=$path",
        )
    }

    /** The Wikidata entities with [ids], with their [props], by id. */
    private suspend fun entities(ids: List<String>, props: String): Map<String, JsonObject> =
        ids.chunked(BATCH).flatMap { chunk ->
            val response = client.get(wikidataUrl) {
                parameter("action", "wbgetentities")
                parameter("ids", chunk.joinToString("|"))
                parameter("props", props)
                parameter("format", "json")
                parameter("origin", "*")
                header(HttpHeaders.UserAgent, USER_AGENT)
                timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
            }
            json.parseToJsonElement(checked(response).bodyAsText()).jsonObject.obj("entities")?.entries.orEmpty()
                .mapNotNull { (id, entity) -> (entity as? JsonObject)?.let { id to it } }
        }.toMap()

    private fun claimIds(entity: JsonObject, property: String): List<String> =
        entity.obj("claims")?.array(property).orEmpty().mapNotNull { claim ->
            (claim as? JsonObject)?.obj("mainsnak")?.obj("datavalue")?.obj("value")?.string("id")
        }

    /** The entity's name in the wiki's language, or else in English. */
    private fun label(entity: JsonObject, code: String): String? =
        entity.obj("labels")?.let { labels -> (labels.obj(code) ?: labels.obj("en"))?.string("value") }

    /** The page's text as the wiki shows it. */
    private suspend fun parsedHtml(code: String, title: String): String =
        api(code) {
            parameter("action", "parse")
            parameter("page", title)
            parameter("prop", "text")
            parameter("redirects", "1")
            parameter("disableeditsection", "1")
            parameter("disablelimitreport", "1")
        }.obj("parse")?.string("text") ?: throw IllegalStateException("Wikisource has no page \"$title\"")

    private suspend fun htmlText(html: String): String =
        FileTextExtractor.extract("page.html", "<html><body>$html</body></html>".encodeToByteArray()).text

    private suspend fun api(code: String, params: HttpRequestBuilder.() -> Unit): JsonObject {
        val response = client.get("${wikiHost(code)}/w/api.php") {
            params()
            parameter("format", "json")
            parameter("formatversion", "2")
            // Lets the web build read the wiki too.
            parameter("origin", "*")
            header(HttpHeaders.UserAgent, USER_AGENT)
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        val body = json.parseToJsonElement(checked(response).bodyAsText()).jsonObject
        body.obj("error")?.string("info")?.let { throw IllegalStateException(it) }
        return body
    }

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Wikisource answered ${response.status.value}")
        return response
    }

    private fun FoundBook.languageCode(): String = Regex("""https://([a-z-]+)\.""").find(pageUrl)?.groupValues?.get(1) ?: error("Not a Wikisource page: $pageUrl")

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray
    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    internal companion object {
        /** Wikimedia asks for a User-Agent that says how to reach the app's makers. */
        const val USER_AGENT = "TayraLanguages/1.0 (https://github.com/zavanton123/tayra-languages)"
        const val PAGE_SIZE = 25
        const val SEARCH_SIZE = 40
        const val BATCH = 50
        const val TITLES_PER_REQUEST = 20
        const val TIMEOUT = 30_000L
        const val EXPORT_TIMEOUT = 120_000L
        const val MIN_WORDS = 30
        val SUBPAGE_LINK = Regex("""href="/wiki/([^"#?]+)"""")

        /** What Wikidata calls the kinds of literature: a literary work, an edition or translation, a novel, a poem... */
        val LITERATURE = setOf(
            "Q7725634", "Q3331189", "Q47461344", "Q571", "Q8261", "Q5185279", "Q49084", "Q25379", "Q12106333", "Q35760",
            "Q699", "Q693", "Q80056", "Q1279564", "Q7725310", "Q182357", "Q37484", "Q482", "Q149537", "Q17518461",
        )

        /**
         * The wiki names a work after its author when others share its title, by name or surname:
         * "Mensagem (Fernando Pessoa)", "Памятник (Державин)".
         */
        fun withoutAuthor(title: String, authors: List<String>): String {
            val bracket = Regex("""\s*\(([^()]+)\)$""").find(title) ?: return title
            val named = bracket.groupValues[1]
            return if (authors.any { it == named || it.endsWith(" $named") }) title.substring(0, bracket.range.first) else title
        }

        private val NOTICE = Regex("""<(div|span|table|p)\b[^>]*\bclass="[^"]*\bws-noexport\b[^"]*"[^>]*>""")

        /**
         * The page without what the wiki marks as not part of the text, as its export tool leaves it
         * out: the header with the neighbouring chapters, the hidden record of title and author.
         */
        fun withoutNotices(html: String): String {
            var text = html
            while (true) {
                val open = NOTICE.find(text) ?: return text
                val tag = open.groupValues[1]
                val tags = Regex("""<(/?)$tag\b[^>]*>""")
                var depth = 0
                var end = text.length
                for (match in tags.findAll(text, open.range.first)) {
                    depth += if (match.groupValues[1].isEmpty()) 1 else -1
                    if (depth == 0) { end = match.range.last + 1; break }
                }
                text = text.removeRange(open.range.first, end)
            }
        }

        fun decodeTitle(path: String): String = path.decodeURLPart().replace('_', ' ')

        /**
         * Whether a document of the export is the work's own text: not the title page or the credits
         * the tool adds, and not the work's page when that only lists the chapters that follow it.
         */
        fun isWorkText(id: String, html: String, spine: List<String>): Boolean {
            if (id == "title" || id == "about") return false
            if (!id.startsWith("c0_") || spine.none { it.startsWith("c1_") }) return true
            val words = { text: String -> Regex("""[\p{L}\p{N}]+""").findAll(text.replace(Regex("<[^>]+>"), " ")).count() }
            val linked = Regex("""<a\b[^>]*>([\s\S]*?)</a>""").findAll(html).sumOf { words(it.groupValues[1]) }
            return linked * 10 < words(html) * 4
        }
    }
}
