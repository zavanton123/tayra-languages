package com.tayra.languages.core.data.discover

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
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Aozora Bunko, the Japanese library of works out of copyright, through the Bungo API, which
 * serves its catalog and its texts in UTF-8 where Aozora's own files are in Shift_JIS. Unsearched,
 * it lists the works for children, the most read first. A search naming an author lists the
 * author's works, the API's own search matching titles only. A text keeps Aozora's markup of
 * readings and notes, which is taken out.
 */
class AozoraCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.bungo.app/v1",
) : BookCatalog {
    override val source = BookSource.AOZORA

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        if (query.isEmpty()) return books(page) { parameter("juvenile", true) }
        // The API takes up to 12 characters.
        val words = query.take(MAX_QUERY)
        val author = author(words) ?: return books(page) { parameter("q", words) }
        return books(page) { parameter("author_id", author) }
    }

    override suspend fun sample(book: FoundBook): String {
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.Range, "bytes=0-${SAMPLE_BYTES - 1}")
            // A partial answer kept by the client's cache would come back for the whole text later.
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        // The range can end inside a character or a paragraph; both are cut off.
        return AozoraText.clean(checked(response).bodyAsBytes().decodeToString().substringBeforeLast('\n'))
    }

    override suspend fun text(book: FoundBook): String {
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        return AozoraText.clean(checked(response).bodyAsText())
    }

    private suspend fun books(page: Int, filter: HttpRequestBuilder.() -> Unit): CatalogPage {
        val response = client.get("$baseUrl/books") {
            filter()
            // Works still under copyright whose authors let them be read free are left out.
            parameter("rights_reserved", false)
            parameter("sort", "access_count")
            parameter("limit", PAGE_SIZE)
            if (page > 1) parameter("offset", (page - 1) * PAGE_SIZE)
            timeout { requestTimeoutMillis = CATALOG_TIMEOUT; socketTimeoutMillis = CATALOG_TIMEOUT }
        }
        return parse(checked(response).bodyAsText())
    }

    /** The id of the first author whose name matches [name]. */
    private suspend fun author(name: String): Int? {
        val response = client.get("$baseUrl/persons") {
            parameter("q", name)
            parameter("rights_reserved", false)
            parameter("limit", 1)
            timeout { requestTimeoutMillis = CATALOG_TIMEOUT; socketTimeoutMillis = CATALOG_TIMEOUT }
        }
        val persons = json.parseToJsonElement(checked(response).bodyAsText()).jsonObject["persons"] as? JsonArray
        return (persons?.firstOrNull() as? JsonObject)?.int("id")
    }

    internal fun parse(body: String): CatalogPage {
        val root = json.parseToJsonElement(body).jsonObject
        val books = (root["books"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let(::book) }
        val more = (root["hasNext"] as? JsonPrimitive)?.booleanOrNull ?: false
        return CatalogPage(books, hasMore = more, total = root.int("totalCount"))
    }

    private fun book(item: JsonObject): FoundBook? {
        val id = item.int("id") ?: return null
        val author = item["author"] as? JsonObject
        val title = listOfNotNull(item.string("title"), item.string("subTitle")).filter { it.isNotBlank() }.joinToString(" ")
        // Old kana spellings and old characters are harder going than the modern ones most works have.
        val writing = item.string("characterType")?.takeIf { it != MODERN }
        return FoundBook(
            source = source,
            id = id.toString(),
            title = title.ifEmpty { return null },
            authors = listOfNotNull(author?.string("fullName")),
            description = item.string("beginning").orEmpty().trim(),
            subjects = emptyList(),
            level = writing,
            license = "Public domain",
            pageUrl = "https://www.aozora.gr.jp/cards/${(author?.int("id") ?: 0).toString().padStart(6, '0')}/card$id.html",
            downloadUrl = "$baseUrl/books/$id/text",
        )
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Aozora Bunko answered ${response.status.value}")
        return response
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val MAX_QUERY = 12
        const val CATALOG_TIMEOUT = 30_000L
        const val TEXT_TIMEOUT = 60_000L
        const val SAMPLE_BYTES = 65_536
        const val MODERN = "新字新仮名"
    }
}

/** An Aozora Bunko text as a book's text: without its title lines, notes on its markup, readings and colophon. */
object AozoraText {
    private val rule = Regex("""^-{10,}\s*$""", RegexOption.MULTILINE)
    private val colophon = Regex("""\n底本：""")

    /** A character missing from the character set, described in a note: ※［＃「特のへん＋廴＋聿」、第3水準1-87-71］. */
    private val missing = Regex("""※［＃[^］]*］""")
    private val note = Regex("""［＃[^］]*］""")
    private val reading = Regex("""《[^》]*》""")

    fun clean(raw: String): String {
        var text = raw.replace("\r\n", "\n").removePrefix("﻿")
        colophon.find(text)?.let { text = text.substring(0, it.range.first) }
        // The title and author come first, then the notes on the markup between two rules, if any.
        val rules = rule.findAll(text).take(2).toList()
        text = if (rules.size == 2 && rules[0].range.first < HEADER_MAX) text.substring(rules[1].range.last + 1)
        else text.indexOf("\n\n").takeIf { it in 0 until HEADER_MAX }?.let { text.substring(it + 2) } ?: text
        text = missing.replace(text, "")
        text = note.replace(text, "")
        text = reading.replace(text, "").replace("｜", "")
        return text.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }

    private const val HEADER_MAX = 400
}
