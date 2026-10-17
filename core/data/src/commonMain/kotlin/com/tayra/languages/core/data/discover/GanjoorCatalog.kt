package com.tayra.languages.core.data.discover

import com.tayra.languages.core.data.files.HtmlText
import com.tayra.languages.core.domain.books.discover.BookCatalog
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.CatalogPage
import com.tayra.languages.core.domain.books.discover.FoundBook
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Ganjoor, the library of classical Persian poetry and prose. Each of its texts is one poem or
 * one story, short enough to read at a sitting. Unsearched, it lists the stories of Saadi's
 * Golestan, its most approachable prose, chapter by chapter, each chapter fetched when the list
 * reaches it; a search looks through every poet's texts. Texts come without their short vowel
 * marks, as Persian is mostly written and as words are looked up.
 */
class GanjoorCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.ganjoor.net/api/ganjoor",
) : BookCatalog {
    override val source = BookSource.GANJOOR

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private val stories = mutableListOf<FoundBook>()

    /** The Golestan's chapters not yet read, the book itself first. */
    private val chapters = ArrayDeque(listOf(GOLESTAN))
    private var chaptersFound = false

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage =
        if (query.isEmpty()) golestan(page) else searched(query, page)

    override suspend fun sample(book: FoundBook): String = text(book)

    override suspend fun text(book: FoundBook): String {
        val response = client.get(book.downloadUrl) {
            // The poem alone, without its recitations, comments and neighbours.
            for (part in LEFT_OUT) parameter(part, false)
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        val poem = json.parseToJsonElement(checked(response).bodyAsText()).jsonObject
        val html = poem.string("htmlText")
        val text = if (html.isNullOrBlank()) poem.string("plainText").orEmpty() else HtmlText.text(html)
        return PersianMarks.strip(text).lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }

    private suspend fun golestan(page: Int): CatalogPage = lock.withLock {
        while (stories.size <= page * PAGE_SIZE && chapters.isNotEmpty()) {
            val cat = category(chapters.removeFirst())
            if (!chaptersFound) {
                chaptersFound = true
                (cat["children"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.int("id") }.forEach(chapters::addLast)
            }
            val chapter = cat.string("title").orEmpty()
            val path = cat.string("fullUrl").orEmpty()
            stories += (cat["poems"] as? JsonArray).orEmpty().mapNotNull { poem ->
                val entry = poem as? JsonObject ?: return@mapNotNull null
                book(entry, poet = SAADI, part = chapter, url = "$path/${entry.string("urlSlug").orEmpty()}")
            }
        }
        val all = chapters.isEmpty()
        CatalogPage(stories.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE), hasMore = stories.size > page * PAGE_SIZE || !all, total = stories.size.takeIf { all })
    }

    private suspend fun category(id: Int): JsonObject {
        val response = client.get("$baseUrl/cat/$id") {
            parameter("poems", true)
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        return json.parseToJsonElement(checked(response).bodyAsText()).jsonObject["cat"] as? JsonObject ?: JsonObject(emptyMap())
    }

    private suspend fun searched(query: String, page: Int): CatalogPage {
        val response = client.get("$baseUrl/poems/search") {
            parameter("term", query)
            parameter("poetId", 0)
            parameter("PageNumber", page)
            parameter("PageSize", PAGE_SIZE)
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        val poems = json.parseToJsonElement(checked(response).bodyAsText()) as? JsonArray
        // The paging comes in a header, as JSON.
        val paging = response.headers["paging-headers"]?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
        val books = poems.orEmpty().mapNotNull { poem ->
            val entry = poem as? JsonObject ?: return@mapNotNull null
            // "سعدی » گلستان » باب اول در سیرت پادشاهان » حکایت شمارهٔ ۱"
            val parts = entry.string("fullTitle").orEmpty().split(" » ")
            book(entry, poet = parts.first(), part = parts.drop(1).dropLast(1).joinToString(" » "), url = entry.string("fullUrl").orEmpty())
        }
        return CatalogPage(
            books,
            hasMore = (paging?.get("hasNextPage") as? JsonPrimitive)?.booleanOrNull ?: (books.size == PAGE_SIZE),
            total = paging?.int("totalCount"),
        )
    }

    private fun book(poem: JsonObject, poet: String, part: String, url: String): FoundBook? {
        val id = poem.int("id") ?: return null
        return FoundBook(
            source = source,
            id = id.toString(),
            title = poem.string("title")?.trim()?.takeIf { it.isNotEmpty() } ?: return null,
            authors = listOf(poet).filter { it.isNotBlank() },
            description = part,
            subjects = emptyList(),
            license = "Public domain",
            pageUrl = "https://ganjoor.net$url",
            downloadUrl = "$baseUrl/poem/$id",
        )
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Ganjoor answered ${response.status.value}")
        return response
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val TIMEOUT = 30_000L
        const val GOLESTAN = 1665
        const val SAADI = "سعدی"
        val LEFT_OUT = listOf("catInfo", "catPoems", "rhymes", "recitations", "images", "songs", "comments", "verseDetails", "navigation", "relatedpoems")
    }
}

/** The short vowels and other marks over and under Arabic letters, which most Persian is written without. */
internal object PersianMarks {
    private val marks = Regex("[ً-ٰٟ]")

    fun strip(text: String): String = marks.replace(text, "")
}
