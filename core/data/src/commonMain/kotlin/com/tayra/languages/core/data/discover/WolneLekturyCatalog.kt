package com.tayra.languages.core.data.discover

import com.tayra.languages.core.domain.books.discover.BookCatalog
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.CatalogPage
import com.tayra.languages.core.domain.books.discover.FoundBook
import com.tayra.languages.core.domain.books.discover.LicensedText
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Wolne Lektury, the Polish library of school readings and classics. Unsearched, it lists its
 * collection of children's and young people's books, fetched once; a search goes to its API. A
 * book's plain text is at a fixed address by its slug, except for a work in volumes, whose
 * volumes have the texts. The licence is only in the text's closing notes: the works themselves
 * are mostly in the public domain, the newer translations under the Free Art License.
 */
class WolneLekturyCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://wolnelektury.pl",
) : BookCatalog {
    override val source = BookSource.WOLNE_LEKTURY

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var children: List<FoundBook>? = null

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage =
        if (query.isEmpty()) childrensBooks(page) else searched(query, page)

    override suspend fun sample(book: FoundBook): String {
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.Range, "bytes=0-${SAMPLE_BYTES - 1}")
            // A partial answer kept by the client's cache would come back for the whole text later.
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        if (response.status == HttpStatusCode.NotFound) return sample(volumes(book).first())
        // The range can end inside a character or a paragraph; both are cut off.
        return WolneLekturyText.clean(checked(response).bodyAsBytes().decodeToString().substringBeforeLast('\n'))
    }

    override suspend fun text(book: FoundBook): String = licensedText(book).text

    override suspend fun licensedText(book: FoundBook): LicensedText {
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        if (response.status == HttpStatusCode.NotFound) {
            val volumes = volumes(book).map { licensedText(it) }
            return LicensedText(volumes.joinToString("\n") { it.text }, volumes.first().license)
        }
        val raw = checked(response).bodyAsText()
        return LicensedText(WolneLekturyText.clean(raw), WolneLekturyText.license(raw) ?: book.license)
    }

    private suspend fun childrensBooks(page: Int): CatalogPage {
        val all = lock.withLock { children } ?: run {
            val response = client.get("$baseUrl/api/collections/$CHILDREN/") {
                timeout { requestTimeoutMillis = CATALOG_TIMEOUT; socketTimeoutMillis = CATALOG_TIMEOUT }
            }
            val books = parseCollection(checked(response).bodyAsText())
            lock.withLock { children = books }
            books
        }
        return CatalogPage(all.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE), hasMore = page * PAGE_SIZE < all.size, total = all.size)
    }

    private suspend fun searched(query: String, page: Int): CatalogPage {
        val response = client.get("$baseUrl/api/2/books/") {
            parameter("search", query)
            parameter("limit", PAGE_SIZE)
            if (page > 1) parameter("offset", (page - 1) * PAGE_SIZE)
            timeout { requestTimeoutMillis = CATALOG_TIMEOUT; socketTimeoutMillis = CATALOG_TIMEOUT }
        }
        return parseSearch(checked(response).bodyAsText())
    }

    /** The volumes of a work in volumes, in order. */
    private suspend fun volumes(book: FoundBook): List<FoundBook> {
        val response = client.get("$baseUrl/api/2/books/${book.id}/") {
            timeout { requestTimeoutMillis = CATALOG_TIMEOUT; socketTimeoutMillis = CATALOG_TIMEOUT }
        }
        val detail = json.parseToJsonElement(checked(response).bodyAsText()).jsonObject
        val volumes = (detail["children"] as? JsonArray).orEmpty().mapNotNull { child ->
            val slug = (child as? JsonObject)?.string("slug") ?: return@mapNotNull null
            book.copy(id = slug, title = child.string("title") ?: book.title, downloadUrl = textUrl(slug))
        }
        if (volumes.isEmpty()) throw IllegalStateException("Wolne Lektury has no text of this book")
        return volumes
    }

    internal fun parseCollection(body: String): List<FoundBook> =
        (json.parseToJsonElement(body).jsonObject["books"] as? JsonArray).orEmpty().mapNotNull { item ->
            val entry = item as? JsonObject ?: return@mapNotNull null
            book(
                slug = entry.string("slug") ?: return@mapNotNull null,
                title = entry.string("title") ?: return@mapNotNull null,
                authors = listOfNotNull(entry.string("author")),
                subjects = entry.string("genre")?.split(',').orEmpty(),
                cover = entry.string("simple_thumb"),
            )
        }

    internal fun parseSearch(body: String): CatalogPage {
        val root = json.parseToJsonElement(body).jsonObject
        val books = root["member"]?.jsonArray.orEmpty().mapNotNull { item ->
            val entry = item as? JsonObject ?: return@mapNotNull null
            // A few of the library's books are in other languages: Hebrew originals beside their translations.
            if (entry.string("language") != POLISH) return@mapNotNull null
            book(
                slug = entry.string("slug") ?: return@mapNotNull null,
                title = entry.string("title") ?: return@mapNotNull null,
                authors = entry.names("authors"),
                subjects = entry.names("genres"),
                cover = entry.string("cover_thumb"),
            )
        }
        val next = (root["view"] as? JsonObject)?.string("next")
        return CatalogPage(books, hasMore = next != null, total = (root["totalItems"] as? JsonPrimitive)?.intOrNull)
    }

    private fun book(slug: String, title: String, authors: List<String>, subjects: List<String>, cover: String?) = FoundBook(
        source = source,
        id = slug,
        title = title.trim(),
        authors = authors.map { it.trim() }.filter { it.isNotEmpty() && !it.equals(UNKNOWN_AUTHOR, ignoreCase = true) },
        description = "",
        subjects = subjects.map { it.trim() }.filter { it.isNotEmpty() },
        license = "Public domain",
        pageUrl = "https://wolnelektury.pl/katalog/lektura/$slug/",
        downloadUrl = textUrl(slug),
        coverUrl = cover?.takeIf { it.isNotBlank() },
    )

    private fun textUrl(slug: String) = "$baseUrl/media/book/txt/$slug.txt"

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.names(key: String): List<String> = (this[key] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.string("name") }

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Wolne Lektury answered ${response.status.value}")
        return response
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val CATALOG_TIMEOUT = 30_000L
        const val TEXT_TIMEOUT = 60_000L
        const val SAMPLE_BYTES = 65_536
        const val CHILDREN = "literatura-dla-dzieci-i-mlodziezy-2"
        const val POLISH = "pol"
        const val UNKNOWN_AUTHOR = "Autor nieznany"
    }
}

/** A Wolne Lektury plain text as a book's text: without the title page and the closing notes, one paragraph or verse per line. */
object WolneLekturyText {
    /** Author, title, translator and ISBN open the text, two blank lines or more before the work. */
    private val titlePage = Regex("""\n[ \t]*\n[ \t]*\n""")
    private val notes = Regex("""\n-----\n""")
    private val licenseLine = Regex("""Ten utwór jest ([^\n]*)""")
    private val version = Regex("""\d+(?:\.\d+)?""")

    fun clean(raw: String): String {
        var text = raw.replace("\r\n", "\n").removePrefix("﻿")
        notes.findAll(text).lastOrNull()?.let { text = text.substring(0, it.range.first) }
        titlePage.find(text)?.takeIf { it.range.first < TITLE_PAGE_MAX }?.let { text = text.substring(it.range.last + 1) }
        return text.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }

    /** The licence the closing notes give the work: "Public domain", "CC-BY-SA-3.0"... */
    fun license(raw: String): String? {
        val line = licenseLine.findAll(raw).lastOrNull()?.groupValues?.get(1) ?: return null
        val number = version.find(line)?.value
        return when {
            "domenie publicznej" in line -> "Public domain"
            "Wolnej Sztuki" in line -> listOfNotNull("Free Art License", number).joinToString(" ")
            "Creative Commons" in line || "CC BY" in line -> {
                val terms = when {
                    "Na tych samych warunkach" in line || "Na Tych Samych Warunkach" in line || "BY-SA" in line -> "BY-SA"
                    else -> "BY"
                }
                listOfNotNull("CC", terms, number).joinToString("-")
            }
            else -> null
        }
    }

    private const val TITLE_PAGE_MAX = 1_000
}
