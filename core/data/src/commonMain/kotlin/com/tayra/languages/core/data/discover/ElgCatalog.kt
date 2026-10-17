package com.tayra.languages.core.data.discover

import com.tayra.languages.core.data.files.EpubText
import com.tayra.languages.core.data.network.HtmlText
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
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ebooks libres et gratuits, the volunteers' library of French classics, through its OPDS catalog.
 * Unsearched, it lists its books for the young; a feed page holds up to a few hundred books. Its
 * editions are free for non-commercial use. A book is an ePUB, read whole to judge it, without the
 * title page and the notes on the edition that close it; the last few are kept, so adding one just
 * judged downloads nothing again.
 */
class ElgCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://www.ebooksgratuits.com",
) : BookCatalog {
    override val source = BookSource.ELG

    private val lock = Mutex()
    private val texts = LinkedHashMap<String, String>()

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        val response = client.get("$baseUrl/opds/feed.php") {
            if (query.isEmpty()) {
                parameter("mode", "genre")
                parameter("id", YOUTH)
            } else {
                parameter("mode", "search")
                parameter("query", query)
            }
            // The feed counts its pages from 0.
            if (page > 1) parameter("page", page - 1)
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        return parse(checked(response).bodyAsText())
    }

    override suspend fun sample(book: FoundBook): String = text(book)

    override suspend fun text(book: FoundBook): String {
        lock.withLock { texts[book.id] }?.let { return it }
        // Books are kept here as text; the client's cache would keep the whole ePUB, pictures and all.
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        val epub = EpubText.read(checked(response).bodyAsBytes()) { id, html, spine ->
            // A title page, the calibre one or the edition's own: the author, the title and a year.
            id != "titlepage" && !(spine.indexOf(id) <= 1 && HtmlText.toPlainText(html).length < TITLE_PAGE_LETTERS)
        }.text
        val text = epub.substringBefore(EDITION_NOTES).trimEnd()
        lock.withLock {
            texts[book.id] = text
            while (texts.size > KEPT_TEXTS) texts.remove(texts.keys.first())
        }
        return text
    }

    internal fun parse(feed: String): CatalogPage {
        val books = feed.split("<entry>").drop(1).mapNotNull(::book)
        val total = TOTAL.find(feed)?.groupValues?.get(1)?.toIntOrNull()
        return CatalogPage(books, hasMore = NEXT.containsMatchIn(feed), total = total)
    }

    private fun book(entry: String): FoundBook? {
        val id = BOOK_ID.find(entry)?.groupValues?.get(1) ?: return null
        val epub = EPUB.find(entry)?.groupValues?.get(1)?.let(HtmlText::toPlainText) ?: return null
        val title = tag(entry, "title") ?: return null
        // "Le Faure, Georges": the name as it is said.
        val authors = AUTHOR.findAll(entry).map { it.groupValues[1].trim() }
            .map { name -> if (',' in name) "${name.substringAfter(',').trim()} ${name.substringBefore(',').trim()}" else name }.toList()
        val summary = CONTENT.find(entry)?.groupValues?.get(1)?.let(HtmlText::toPlainText).orEmpty()
            .substringBefore("Édition Ebooks libres et gratuits").trim()
        return FoundBook(
            source = source,
            id = id,
            title = title,
            authors = authors,
            description = summary,
            subjects = CATEGORY.findAll(entry).map { HtmlText.toPlainText(it.groupValues[1]) }.toList(),
            license = "Non-commercial use",
            pageUrl = "$baseUrl/details.php?book=$id",
            downloadUrl = epub,
        )
    }

    private fun tag(entry: String, name: String): String? =
        Regex("<$name[^>]*>([^<]*)</$name>").find(entry)?.groupValues?.get(1)?.let(HtmlText::toPlainText)?.takeIf { it.isNotEmpty() }

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Ebooks libres et gratuits answered ${response.status.value}")
        return response
    }

    private companion object {
        const val TIMEOUT = 30_000L
        const val TEXT_TIMEOUT = 60_000L
        const val KEPT_TEXTS = 10
        const val TITLE_PAGE_LETTERS = 300

        /** The genre "Jeunesse". */
        const val YOUTH = 6
        const val EDITION_NOTES = "À propos de cette édition électronique"
        val BOOK_ID = Regex("""<id>[^<]*details\.php\?book=(\d+)</id>""")
        val EPUB = Regex("""<link type="application/epub\+zip" href="([^"]+)"""")
        val AUTHOR = Regex("""<author>\s*<name>([^<]+)</name>""")
        val CONTENT = Regex("""<content[^>]*><!\[CDATA\[(.*?)]]></content>""", RegexOption.DOT_MATCHES_ALL)
        val CATEGORY = Regex("""<category [^>]*label="([^"]+)"""")
        val TOTAL = Regex("""<opensearch:totalResults>(\d+)</opensearch:totalResults>""")
        val NEXT = Regex("""<link[^>]*rel="next"""")
    }
}
