package com.tayra.languages.core.data.discover

import com.tayra.languages.core.data.files.HtmlText
import com.tayra.languages.core.data.files.MarkupToken
import com.tayra.languages.core.domain.books.discover.BookCatalog
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.CatalogPage
import com.tayra.languages.core.domain.books.discover.FoundBook
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.tayra.languages.core.data.network.HtmlText as Entities

/**
 * Arkiv for Dansk Litteratur, the Royal Danish Library's older Danish literature, through the
 * library's text portal. Its JSON answers carry every work's whole text, so works are listed from
 * the portal's Atom feed of the same search. Unsearched, it lists H. C. Andersen's fairy tales in
 * the order of the five volumes of their critical edition, each volume fetched when the list
 * reaches it. A work is read from its page, without the library's links, page numbers
 * and editorial notes; the last few are kept, so adding one just judged downloads nothing again.
 * Its spelling is that of its time: "aa" for "å", nouns capitalised.
 */
class AdlCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://tekster.kb.dk",
) : BookCatalog {
    override val source = BookSource.ADL

    private val lock = Mutex()
    private val texts = LinkedHashMap<String, String>()
    private val fairyTales = mutableListOf<FoundBook>()
    private var volumesRead = 0

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage =
        if (query.isEmpty()) fairyTales(page) else parse(works(query, page = page, perPage = PAGE_SIZE))

    private suspend fun fairyTales(page: Int): CatalogPage = lock.withLock {
        while (fairyTales.size <= page * PAGE_SIZE && volumesRead < FAIRY_TALE_VOLUMES.size) {
            fairyTales += parse(works("", volume = FAIRY_TALE_VOLUMES[volumesRead], perPage = VOLUME_MAX)).books
            volumesRead++
        }
        val all = volumesRead == FAIRY_TALE_VOLUMES.size
        CatalogPage(
            fairyTales.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE),
            hasMore = fairyTales.size > page * PAGE_SIZE || !all,
            total = fairyTales.size.takeIf { all },
        )
    }

    /** The feed of the works matching [query], in a [volume] when one is named. */
    private suspend fun works(query: String, volume: String? = null, page: Int = 1, perPage: Int): String {
        val response = client.get("$baseUrl/text.atom") {
            parameter("q", query)
            parameter("search_field", "Alt")
            parameter("f[subcollection_ssi][]", "adl")
            parameter("f[type_ssi][]", "work")
            // Some works are page scans without a text.
            parameter("f[has_text_ssi][]", "yes")
            if (volume != null) parameter("f[volume_title_ssi][]", volume)
            parameter("per_page", perPage)
            if (page > 1) parameter("page", page)
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        return checked(response).bodyAsText()
    }

    override suspend fun sample(book: FoundBook): String = text(book)

    override suspend fun text(book: FoundBook): String {
        lock.withLock { texts[book.id] }?.let { return it }
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        val text = workText(checked(response).bodyAsText())
        if (text.isBlank()) throw IllegalStateException("The library has no text of this work")
        lock.withLock {
            texts[book.id] = text
            while (texts.size > KEPT_TEXTS) texts.remove(texts.keys.first())
        }
        return text
    }

    internal fun parse(feed: String): CatalogPage {
        val books = feed.split("<entry>").drop(1).mapNotNull(::book)
        val total = TOTAL.find(feed)?.groupValues?.get(1)?.toIntOrNull()
        return CatalogPage(books, hasMore = NEXT.containsMatchIn(feed.substringBefore("<entry>")), total = total)
    }

    private fun book(entry: String): FoundBook? {
        val url = PAGE.find(entry)?.groupValues?.get(1) ?: return null
        val title = TITLE.find(entry)?.groupValues?.get(1)?.let(Entities::toPlainText)?.takeIf { it.isNotEmpty() } ?: return null
        // The summary is the result's HTML, escaped.
        val summary = entry.substringAfter("<summary", "").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&")
        val author = AUTHOR.find(summary)?.groupValues?.get(1)?.let(Entities::toPlainText)?.takeIf { it.isNotEmpty() }
        return FoundBook(
            source = source,
            id = url.substringAfterLast('/'),
            title = title,
            authors = listOfNotNull(author),
            description = "",
            subjects = emptyList(),
            license = "CC-BY-NC-SA",
            pageUrl = url,
            downloadUrl = url,
        )
    }

    /** The work on its page, between the page's top and its button back to the top. */
    internal fun workText(page: String): String {
        val start = page.indexOf("id=\"root\"").takeIf { it >= 0 } ?: return ""
        val end = page.indexOf("id=\"scrollTop\"", start).takeIf { it >= 0 }?.let { page.lastIndexOf('<', it) } ?: page.length
        val work = page.substring(page.lastIndexOf('<', start), end)
        return HtmlText.text(work, ::isApparatus).lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }

    /** A link to a part of the work or to all of it, a page number, or an editorial note. */
    private fun isApparatus(token: MarkupToken.Open): Boolean {
        if (token.attributes["id"].orEmpty().startsWith("expose_link")) return true
        val classes = token.attributes["class"].orEmpty().split(' ')
        return classes.any { it in APPARATUS }
    }

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("The Royal Danish Library answered ${response.status.value}")
        return response
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val TIMEOUT = 30_000L
        const val KEPT_TEXTS = 10
        const val VOLUME_MAX = 100

        /** The volumes of the critical edition of Andersen's fairy tales, as the portal names them; the last two hold little. */
        val FAIRY_TALE_VOLUMES = listOf("i", "ii", "iii", "iv", "v").map { "h.c. andersens eventyr bd. $it kritisk udg. efter de originale eventyrhæfter med varianter ved erik dal" }
        val APPARATUS = setOf("note", "note_content", "pageBreak", "symbol", "quote", "the_whole")
        val TOTAL = Regex("""<opensearch:totalResults>(\d+)</opensearch:totalResults>""")
        val NEXT = Regex("""<link rel="next"""")
        val PAGE = Regex("""<link rel="alternate" type="text/html" href="([^"]+)"""")
        val TITLE = Regex("""<title>([^<]*)</title>""")
        val AUTHOR = Regex("""blacklight-author_id_ssi col-md-9"\s*>\s*<a[^>]*>([^<]*)</a>""")
    }
}
