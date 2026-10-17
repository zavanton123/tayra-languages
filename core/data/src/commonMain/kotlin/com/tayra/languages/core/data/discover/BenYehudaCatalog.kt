package com.tayra.languages.core.data.discover

import com.tayra.languages.core.domain.books.discover.BookCatalog
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.CatalogPage
import com.tayra.languages.core.domain.books.discover.FoundBook
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Project Ben-Yehuda, the Hebrew library, through the dump of its public-domain works on GitHub;
 * its own API asks for a key. The dump's catalog is one file, fetched once and searched here by
 * title and author. Unsearched, it lists the fables, then the stories. Texts come without their
 * vowel points, as Hebrew is mostly written and as words are looked up.
 */
class BenYehudaCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://raw.githubusercontent.com/projectbenyehuda/public_domain_dump/master",
) : BookCatalog {
    override val source = BookSource.BEN_YEHUDA

    private val lock = Mutex()
    private var all: List<Listed>? = null

    /** A work with its genre and the words it is found by. */
    private class Listed(val book: FoundBook, val genre: String, val words: String)

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        val words = HebrewPoints.strip(query).lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val matching = if (words.isEmpty()) {
            val listed = catalog()
            DEFAULT_GENRES.flatMap { genre -> listed.filter { it.genre == genre } }
        } else {
            catalog().filter { listed -> listed.genre in SEARCHED_GENRES && words.all { it in listed.words } }
        }.map { it.book }
        return CatalogPage(matching.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE), hasMore = page * PAGE_SIZE < matching.size, total = matching.size)
    }

    override suspend fun sample(book: FoundBook): String {
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.Range, "bytes=0-${SAMPLE_BYTES - 1}")
            // A partial answer kept by the client's cache would come back for the whole text later.
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        // The range can end inside a character or a paragraph; both are cut off.
        return clean(checked(response).bodyAsBytes().decodeToString().substringBeforeLast('\n'))
    }

    override suspend fun text(book: FoundBook): String {
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        return clean(checked(response).bodyAsText())
    }

    private suspend fun catalog(): List<Listed> = lock.withLock { all } ?: run {
        val response = client.get("$baseUrl/pseudocatalogue.csv") {
            timeout { requestTimeoutMillis = CATALOG_TIMEOUT; socketTimeoutMillis = CATALOG_TIMEOUT }
        }
        val listed = parse(checked(response).bodyAsText())
        lock.withLock { all = listed }
        listed
    }

    private fun parse(csv: String): List<Listed> = Csv.records(csv).mapNotNull { row ->
        val id = row["ID"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val path = row["path"]?.takeIf { it.startsWith("/") } ?: return@mapNotNull null
        val title = row["title"]?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val authors = row["authors"].orEmpty().split(", ").map { it.trim() }.filter { it.isNotEmpty() }
        // "Translation missing: he.prose": the genre is the last word.
        val genre = row["genre"].orEmpty().substringAfterLast('.')
        val book = FoundBook(
            source = source,
            id = id,
            title = title,
            authors = authors,
            description = "",
            subjects = listOf(genre).filter { it.isNotEmpty() },
            license = "Public domain",
            pageUrl = "https://benyehuda.org/read/$id",
            downloadUrl = "$baseUrl/txt_stripped$path.txt",
        )
        val translators = row["translators"].orEmpty()
        Listed(book, genre, HebrewPoints.strip(listOf(title, authors.joinToString(" "), translators).joinToString(" ")).lowercase())
    }

    /** The text without its title, the first line. */
    private fun clean(raw: String): String =
        raw.replace("\r\n", "\n").removePrefix("﻿").substringAfter('\n', "")
            .lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Project Ben-Yehuda answered ${response.status.value}")
        return response
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val CATALOG_TIMEOUT = 60_000L
        const val TEXT_TIMEOUT = 60_000L
        const val SAMPLE_BYTES = 65_536

        /** Short and plain first: fables, then stories. */
        val DEFAULT_GENRES = listOf("fables", "prose")

        /** Every genre but the reference works and lexicons. */
        val SEARCHED_GENRES = setOf("fables", "prose", "poetry", "article", "memoir", "letters", "drama")
    }
}

/** Hebrew's vowel points and cantillation marks, which most Hebrew is written without. */
internal object HebrewPoints {
    private val points = Regex("[֑-ׇֽֿׁׂׅׄ]")

    fun strip(text: String): String = points.replace(text, "")
}
