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
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Elejandría, a Spanish library of books in the public domain or under open licences. It has no API
 * and its search is drawn in the browser, so its books are found in its sitemap, fetched once, by the
 * words of their addresses (`/libro/<title>/<author>/<id>`); unsearched, it lists its collections of
 * children's stories. The books of a page are read as they are listed, each from its page, for the
 * title, author and cover the address spells without accents, and its ePub's address. The ePub is
 * read whole to judge it, without the site's greeting and thanks around it, the last few kept, so
 * adding one just judged downloads nothing again.
 */
class ElejandriaCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://www.elejandria.com",
) : BookCatalog {
    override val source = BookSource.ELEJANDRIA

    private val lock = Mutex()
    private var sitemap: List<Listed>? = null
    private var children: List<Listed>? = null
    private val described = HashMap<String, FoundBook>()
    private val texts = LinkedHashMap<String, String>()

    /** A book's page, with the words its address spells. */
    private class Listed(val url: String, val words: String)

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        val words = plain(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
        val matching = if (words.isEmpty()) childrensBooks() else sitemap().filter { listed -> words.all { it in listed.words } }
        val books = coroutineScope { matching.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE).map { async { describe(it.url) } }.awaitAll() }.filterNotNull()
        return CatalogPage(books, hasMore = page * PAGE_SIZE < matching.size, total = matching.size)
    }

    override suspend fun sample(book: FoundBook): String = text(book)

    override suspend fun text(book: FoundBook): String {
        lock.withLock { texts[book.id] }?.let { return it }
        // Books are kept here as text; the client's cache would keep the whole ePub, pictures and all.
        val response = client.get(book.downloadUrl) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        val text = withoutNotices(EpubText.read(checked(response).bodyAsBytes()).text)
        lock.withLock {
            texts[book.id] = text
            while (texts.size > KEPT_TEXTS) texts.remove(texts.keys.first())
        }
        return text
    }

    private suspend fun sitemap(): List<Listed> = lock.withLock { sitemap } ?: run {
        val xml = checked(client.get("$baseUrl/sitemap.xml") { timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT } }).bodyAsText()
        val listed = books(xml)
        lock.withLock { sitemap = listed }
        listed
    }

    private suspend fun childrensBooks(): List<Listed> = lock.withLock { children } ?: run {
        val listed = CHILDREN.flatMap { collection ->
            val html = checked(client.get("$baseUrl/coleccion/$collection") { timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT } }).bodyAsText()
            // The books "popular right now" beside every page are no part of the collection.
            books(html.substringBefore(POPULAR))
        }.distinctBy { it.url }
        lock.withLock { children = listed }
        listed
    }

    /** The book pages a page links to, in its order. */
    private fun books(page: String): List<Listed> =
        BOOK_LINK.findAll(page).map { it.value }.distinct().map { url ->
            Listed(url, plain(url.substringAfter("/libro/").substringBeforeLast('/').replace('/', ' ').replace('-', ' ')))
        }.toList()

    /** The book as its page tells it: title, author, cover and ePub; null for a book with no ePub. */
    private suspend fun describe(url: String): FoundBook? {
        lock.withLock { described[url] }?.let { return it }
        val html = runCatching {
            checked(client.get(url) { timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT } }).bodyAsText()
        }.getOrNull() ?: return null
        val book = book(url, html) ?: return null
        lock.withLock { described[url] = book }
        return book
    }

    internal fun book(url: String, html: String): FoundBook? {
        val id = url.trimEnd('/').substringAfterLast('/')
        val file = EPUB_LINK.find(html)?.groupValues?.get(1) ?: return null
        // "Libro El principito en PDF y ePub - Elejandría"; "Descargar gratis El principito de Antoine de Saint-Exupéry en PDF, ePUB o MOBI".
        val title = meta(html, "og:title")?.removePrefix("Libro ")?.substringBefore(" en PDF")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val author = meta(html, "og:description")?.substringAfter("$title de ", "")?.substringBefore(" en PDF")?.trim()
        return FoundBook(
            source = source,
            id = id,
            title = title,
            authors = listOfNotNull(author?.takeIf { it.isNotEmpty() }),
            description = "",
            subjects = emptyList(),
            license = "Public domain or open licence",
            pageUrl = url,
            downloadUrl = "$baseUrl/libro/link_descarga_libro/$id/$file",
            coverUrl = meta(html, "og:image"),
        )
    }

    private fun meta(html: String, property: String): String? =
        Regex("""<meta\s+property="$property"\s+content="([^"]*)"""").find(html)?.groupValues?.get(1)?.let(HtmlText::toPlainText)?.takeIf { it.isNotEmpty() }

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Elejandría answered ${response.status.value}")
        return response
    }

    internal companion object {
        const val PAGE_SIZE = 20
        const val TIMEOUT = 30_000L
        const val TEXT_TIMEOUT = 60_000L
        const val KEPT_TEXTS = 10
        const val POPULAR = "Otros libros populares"
        val CHILDREN = listOf("cuentos-infantiles", "cuentos-para-dormir", "libros-juveniles-para-pequeños-grandes-lectores")
        val BOOK_LINK = Regex("""https://www\.elejandria\.com/libro/(?!descargar|link_descarga)[^/"<\s]+/[^/"<\s]+/\d+""")

        /** The link whose text is "Descargar en ePub": its last number is the ePub file's. */
        val EPUB_LINK = Regex("""<a [^>]*href="[^"]*/libro/descargar/[^"]*/(\d+)"[^>]*>(?:(?!</a>).)*ePub""", RegexOption.DOT_MATCHES_ALL)

        private const val GREETING = "¡Esperamos que lo disfrutéis!"
        private const val THANKS = "¡Gracias por leer este libro"

        /** The book without "Libro descargado en www.elejandria.com… ¡Esperamos que lo disfrutéis!" before it and the thanks after it. */
        fun withoutNotices(text: String): String =
            text.substringAfter(GREETING).substringBefore(THANKS).trim()

        private val ACCENTS = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n', 'à' to 'a', 'è' to 'e', 'ò' to 'o', 'ç' to 'c')

        /** Lowercase and without accents, as the addresses spell words. */
        fun plain(text: String): String = text.lowercase().map { ACCENTS[it] ?: it }.joinToString("")
    }
}
