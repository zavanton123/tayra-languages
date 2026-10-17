package com.tayra.languages.core.data.discover

import com.tayra.languages.core.data.files.FileTextExtractor
import com.tayra.languages.core.data.network.HtmlText
import com.tayra.languages.core.domain.books.discover.BookCatalog
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.CatalogPage
import com.tayra.languages.core.domain.books.discover.Difficulty
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The Global Digital Library's early-grade readers (content.digitallibrary.io), StoryWeaver's and
 * other publishers', openly licensed and graded by level. Its languages have slugs of their own
 * ("pt-br", "es-es" and "es-ni" for Spanish), so a language's books are those of every slug with
 * its code. A book is an ePUB, read whole to judge it; the last few are kept, so adding one just
 * judged downloads nothing again.
 */
class GdlCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://content.digitallibrary.io/wp-json/content-api/v1/",
) : BookCatalog {
    override val source = BookSource.GDL

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var slugs: List<String>? = null
    private val texts = LinkedHashMap<String, String>()

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        val wanted = slugsFor(languageCode)
        if (wanted.isEmpty()) return CatalogPage(emptyList(), hasMore = false, total = 0)
        val response = client.get(baseUrl + "contentsearch") {
            parameter("language", wanted.joinToString(","))
            if (query.isNotEmpty()) parameter("query", query)
            if (page > 1) parameter("_skip", (page - 1) * PAGE_SIZE)
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        return parse(checked(response).bodyAsText(), page)
    }

    override suspend fun sample(book: FoundBook): String = text(book)

    override suspend fun text(book: FoundBook): String {
        lock.withLock { texts[book.id] }?.let { return it }
        // Books are kept here as text; the client's cache would keep the whole ePUB, pictures and all.
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        val text = FileTextExtractor.extract("book.epub", checked(response).bodyAsBytes()).text
        // Many of the library's books are picture books with a few captions at most.
        if (text.split(Regex("\\s+")).count { word -> word.any { it.isLetter() } } < MIN_WORDS) throw IllegalStateException("This book is mostly pictures, with too little text to read")
        lock.withLock {
            texts[book.id] = text
            while (texts.size > KEPT_TEXTS) texts.remove(texts.keys.first())
        }
        return text
    }

    internal fun parse(body: String, page: Int): CatalogPage {
        val root = json.parseToJsonElement(body).jsonObject
        val books = root["books"]?.jsonArray.orEmpty().mapNotNull { (it as? JsonObject)?.let(::book) }
        val total = root["meta"]?.jsonObject?.get("count")?.jsonPrimitive?.intOrNull ?: books.size
        return CatalogPage(books, hasMore = page * PAGE_SIZE < total, total = total)
    }

    private fun book(item: JsonObject): FoundBook? {
        val epub = item.string("epubUrl")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val level = item.terms("topic").firstOrNull { LEVEL.matches(it) } ?: item.terms("topic").firstOrNull { it.equals("Emergent Readers", ignoreCase = true) }
        return FoundBook(
            source = source,
            id = item["postId"]?.jsonPrimitive?.intOrNull?.toString() ?: return null,
            title = item.string("title")?.let(HtmlText::toPlainText)?.takeIf { it.isNotEmpty() } ?: return null,
            authors = listOfNotNull(item.string("publisher")?.let(HtmlText::toPlainText)?.takeIf { it.isNotEmpty() }),
            description = item.string("description")?.let(HtmlText::toPlainText).orEmpty(),
            subjects = emptyList(),
            level = level,
            levelDifficulty = level?.let(::difficultyOf),
            license = item.terms("license").firstOrNull() ?: "Open licence",
            pageUrl = item.string("postLink").orEmpty().ifEmpty { epub },
            downloadUrl = epub,
            coverUrl = item.string("thumbnail"),
        )
    }

    /** The slugs of the language with [code]: the code itself and its regional forms. */
    private suspend fun slugsFor(code: String): List<String> {
        val all = lock.withLock { slugs } ?: run {
            val response = client.get(baseUrl + "languages") { timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT } }
            val list = json.parseToJsonElement(checked(response).bodyAsText()).jsonArray
                .mapNotNull { (it as? JsonObject)?.string("slug") }
            lock.withLock { slugs = list }
            list
        }
        return all.filter { it == code || it.startsWith("$code-") }
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    /** The names of the terms a book is filed under in [taxonomy]. */
    private fun JsonObject.terms(taxonomy: String): List<String> =
        (this[taxonomy] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.string("name") }

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("The Global Digital Library answered ${response.status.value}")
        return response
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val TIMEOUT = 30_000L
        const val MIN_WORDS = 30
        const val KEPT_TEXTS = 10
        val LEVEL = Regex("""Level\s*\d+""", RegexOption.IGNORE_CASE)

        /** Levels 1 and 2, and the emergent readers before them, are the easiest; 4 and up the hardest. */
        fun difficultyOf(level: String): Difficulty {
            val n = Regex("""\d+""").find(level)?.value?.toInt() ?: return Difficulty.EASY
            return when {
                n <= 2 -> Difficulty.EASY
                n >= 4 -> Difficulty.HARD
                else -> Difficulty.MEDIUM
            }
        }
    }
}
