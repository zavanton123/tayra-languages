package com.tayra.languages.core.data.discover

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
import kotlinx.coroutines.delay

/**
 * Project Gutenberg's public-domain books, found through its own OPDS catalog, the feed e-book
 * readers browse it with; it answers in about a second where the Gutendex catalog can take most of
 * a minute. The feed names a book by its title and author only; its plain text and cover are at
 * fixed addresses by its number. A sample is the first 64 KB of the text, fetched with a range
 * request rather than the whole book.
 */
class GutenbergCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://www.gutenberg.org",
) : BookCatalog {
    override val source = BookSource.GUTENBERG

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        suspend fun ask() = client.get("$baseUrl/ebooks/search.opds/") {
            // The catalog's own search syntax: "l.pt" asks for the books in Portuguese.
            parameter("query", listOf("l.$languageCode", query).filter { it.isNotEmpty() }.joinToString(" "))
            parameter("sort_order", "downloads")
            if (page > 1) parameter("start_index", (page - 1) * PAGE_SIZE + 1)
            timeout { requestTimeoutMillis = CATALOG_TIMEOUT; socketTimeoutMillis = CATALOG_TIMEOUT }
        }
        // The search now and then times out behind the site's gateway, and answers when asked again.
        val response = ask().let { first -> if (first.status.value in 502..504) { delay(RETRY_DELAY); ask() } else first }
        return parse(checked(response).bodyAsText())
    }

    override suspend fun sample(book: FoundBook): String {
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.Range, "bytes=0-${SAMPLE_BYTES - 1}")
            // A partial answer kept by the client's cache would come back for the whole text later.
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        // The range can end inside a character or a paragraph; both are cut off.
        val raw = checked(response).bodyAsBytes().decodeToString()
        return GutenbergText.clean(raw.substringBeforeLast('\n'))
    }

    override suspend fun text(book: FoundBook): String {
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        return GutenbergText.clean(checked(response).bodyAsBytes().decodeToString())
    }

    internal fun parse(feed: String): CatalogPage {
        val books = feed.split("<entry>").drop(1).mapNotNull(::book)
        return CatalogPage(books, hasMore = NEXT.containsMatchIn(feed))
    }

    private fun book(entry: String): FoundBook? {
        val id = BOOK_ID.find(entry)?.groupValues?.get(1) ?: return null
        // The feed follows every title with the book's language: "Dom Casmurro (Portuguese)".
        val title = tag(entry, "title")?.let { LANGUAGE_SUFFIX.replace(it, "") }?.takeIf { it.isNotEmpty() } ?: return null
        // A book with no author shows its downloads in the author's place.
        val author = tag(entry, "content")?.takeUnless { DOWNLOADS.matches(it) }
        return FoundBook(
            source = source,
            id = id,
            title = title,
            authors = listOfNotNull(author),
            description = "",
            subjects = emptyList(),
            license = "Public domain",
            pageUrl = "https://www.gutenberg.org/ebooks/$id",
            downloadUrl = "$baseUrl/cache/epub/$id/pg$id.txt",
            coverUrl = "$baseUrl/cache/epub/$id/pg$id.cover.medium.jpg",
        )
    }

    private fun tag(entry: String, name: String): String? =
        Regex("<$name[^>]*>([^<]*)</$name>").find(entry)?.groupValues?.get(1)?.let(HtmlText::toPlainText)?.takeIf { it.isNotEmpty() }

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Project Gutenberg answered ${response.status.value}")
        return response
    }

    private companion object {
        const val PAGE_SIZE = 25
        const val CATALOG_TIMEOUT = 30_000L
        const val TEXT_TIMEOUT = 60_000L
        const val SAMPLE_BYTES = 65_536
        const val RETRY_DELAY = 1_000L
        val BOOK_ID = Regex("""<id>[^<]*/ebooks/(\d+)\.opds</id>""")
        val NEXT = Regex("""<link[^>]*rel="next"""")
        val LANGUAGE_SUFFIX = Regex("""\s*\([^()]*\)$""")
        val DOWNLOADS = Regex("""\d+ downloads""")
    }
}

/** A Project Gutenberg plain text as a book's text: without the licence around it, one paragraph per line. */
object GutenbergText {
    private val start = Regex("""\*{3}\s*START OF (?:THE |THIS )?PROJECT GUTENBERG[^\n]*\n""", RegexOption.IGNORE_CASE)
    private val end = Regex("""\n\*{3}\s*END OF (?:THE |THIS )?PROJECT GUTENBERG""", RegexOption.IGNORE_CASE)

    /** Credits and notices some books open with, before the work itself. */
    private val credits = Regex("""^(?:Produced by|E-text prepared by|Transcribed from|This eBook is (?:for the use of|donated to)|Transcriber's note)""", RegexOption.IGNORE_CASE)
    private val illustration = Regex("""^\[Illustration[^\]]*]$""", RegexOption.IGNORE_CASE)
    private val italics = Regex("""_([^_\n]+)_""")

    fun clean(raw: String): String {
        var text = raw.replace("\r\n", "\n").removePrefix("﻿")
        start.find(text)?.let { text = text.substring(it.range.last + 1) }
        end.find(text)?.let { text = text.substring(0, it.range.first) }
        return text.split(Regex("\n\\s*\n"))
            // The lines of a paragraph are wrapped at about 70 characters; a paragraph is one line here.
            .map { paragraph -> paragraph.lines().joinToString(" ") { it.trim() }.trim() }
            .filter { it.isNotEmpty() && !illustration.matches(it) }
            .dropWhile { credits.containsMatchIn(it) }
            .joinToString("\n") { italics.replace(it, "$1") }
    }
}
