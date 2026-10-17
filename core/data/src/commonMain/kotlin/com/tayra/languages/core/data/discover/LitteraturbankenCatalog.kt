package com.tayra.languages.core.data.discover

import com.tayra.languages.core.data.files.EpubText
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
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Litteraturbanken, the Swedish literature bank: its proofread e-texts released under CC0, the
 * rest being under terms of its own. Its list of them is small enough to fetch once, the most read
 * first, and is searched here by title and author. A book is an ePUB, read whole to judge it; the
 * last few are kept, so adding one just judged downloads nothing again. Most keep the spelling of
 * their time ("hvilket", "öfver").
 */
class LitteraturbankenCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://litteraturbanken.se",
) : BookCatalog {
    override val source = BookSource.LITTERATURBANKEN

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var all: List<Listed>? = null
    private val texts = LinkedHashMap<String, String>()

    /** A book with the words it is found by. */
    private class Listed(val book: FoundBook, val words: String)

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val matching = list().filter { listed -> words.all { it in listed.words } }.map { it.book }
        return CatalogPage(matching.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE), hasMore = page * PAGE_SIZE < matching.size, total = matching.size)
    }

    override suspend fun sample(book: FoundBook): String = text(book)

    override suspend fun text(book: FoundBook): String {
        lock.withLock { texts[book.id] }?.let { return it }
        // Books are kept here as text; the client's cache would keep the whole ePUB, pictures and all.
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        val text = EpubText.read(checked(response).bodyAsBytes()) { _, html, _ -> !FRONT_MATTER.containsMatchIn(html) }.text
        lock.withLock {
            texts[book.id] = text
            while (texts.size > KEPT_TEXTS) texts.remove(texts.keys.first())
        }
        return text
    }

    private suspend fun list(): List<Listed> = lock.withLock { all } ?: run {
        val response = client.get("$baseUrl/api/list_all/etext") {
            parameter("from", 0)
            parameter("to", MAX_TEXTS)
            parameter("sort_field", "popularity|desc")
            // Without the fields named, each text comes with its pages.
            parameter("include", "lbworkid,titleid,shorttitle,authors.full_name,authors.authorid,epub_license,has_epub,language")
            timeout { requestTimeoutMillis = CATALOG_TIMEOUT; socketTimeoutMillis = CATALOG_TIMEOUT }
        }
        val listed = parse(checked(response).bodyAsText())
        lock.withLock { all = listed }
        listed
    }

    private fun parse(body: String): List<Listed> =
        (json.parseToJsonElement(body).jsonObject["data"] as? JsonArray).orEmpty().mapNotNull { item ->
            val entry = item as? JsonObject ?: return@mapNotNull null
            if (entry.string("epub_license") != CC0 || (entry["has_epub"] as? JsonPrimitive)?.booleanOrNull != true || entry.string("language") != SWEDISH) return@mapNotNull null
            val authors = (entry["authors"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
            val authorId = authors.firstNotNullOfOrNull { it.string("authorid") } ?: return@mapNotNull null
            val titleId = entry.string("titleid") ?: return@mapNotNull null
            val title = entry.string("shorttitle")?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val names = authors.mapNotNull { it.string("full_name") }
            val book = FoundBook(
                source = source,
                id = entry.string("lbworkid") ?: "${authorId}_$titleId",
                title = title,
                authors = names,
                description = "",
                subjects = emptyList(),
                license = "CC0",
                pageUrl = "https://litteraturbanken.se/författare/$authorId/titlar/$titleId/info",
                downloadUrl = "$baseUrl/txt/epub/${"${authorId}_$titleId".encodeURLPathPart()}.epub",
            )
            Listed(book, (listOf(title) + names).joinToString(" ").lowercase())
        }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Litteraturbanken answered ${response.status.value}")
        return response
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val MAX_TEXTS = 5_000
        const val CATALOG_TIMEOUT = 30_000L
        const val TEXT_TIMEOUT = 60_000L
        const val KEPT_TEXTS = 10
        const val CC0 = "cc-0"
        const val SWEDISH = "swe"

        /** The cover, the title page and the notes on the edition, before the work. */
        val FRONT_MATTER = Regex("""<body[^>]*\bid="(?:titelsida|kolofon)"""")
    }
}
