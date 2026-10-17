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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Projecto Adamastor's editions of Portuguese classics, revised into today's spelling (before the
 * 1990 agreement) and shared under CC BY-SA 4.0. Its catalogue is one page, fetched once and
 * searched here. A book's ePUB is linked from its page; it is read whole to judge it, without
 * the edition's cover, title page, credits, contents and author's note, and the last few are kept,
 * so adding one just judged downloads nothing again.
 */
class AdamastorCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://projectoadamastor.org",
) : BookCatalog {
    override val source = BookSource.ADAMASTOR

    private val lock = Mutex()
    private var all: List<Listed>? = null
    private val texts = LinkedHashMap<String, String>()

    /** A book with the words it is found by. */
    private class Listed(val book: FoundBook, val words: String)

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val matching = catalog().filter { listed -> words.all { it in listed.words } }.map { it.book }
        return CatalogPage(matching.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE), hasMore = page * PAGE_SIZE < matching.size, total = matching.size)
    }

    override suspend fun sample(book: FoundBook): String = text(book)

    override suspend fun text(book: FoundBook): String {
        lock.withLock { texts[book.id] }?.let { return it }
        val page = client.get(book.pageUrl) { timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT } }
        val epub = EPUB_LINK.find(checked(page).bodyAsText())?.groupValues?.get(1)?.let(HtmlText::toPlainText)
            ?: throw IllegalStateException("Projecto Adamastor has no ePUB of this book")
        // Books are kept here as text; the client's cache would keep the whole ePUB, pictures and all.
        val response = client.get(epub) {
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        val text = EpubText.read(checked(response).bodyAsBytes()) { id, _, _ -> !EDITION_PAGES.matches(id.substringBeforeLast('.')) }.text
        lock.withLock {
            texts[book.id] = text
            while (texts.size > KEPT_TEXTS) texts.remove(texts.keys.first())
        }
        return text
    }

    private suspend fun catalog(): List<Listed> = lock.withLock { all } ?: run {
        val response = client.get("$baseUrl/catalogo/") { timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT } }
        val listed = parse(checked(response).bodyAsText())
        lock.withLock { all = listed }
        listed
    }

    private fun parse(html: String): List<Listed> = THUMB.findAll(html).mapNotNull { link ->
        val attributes = link.groupValues[1]
        val url = attribute(attributes, "href") ?: return@mapNotNull null
        val img = IMAGE.find(html, link.range.last)?.takeIf { it.range.first - link.range.last < IMAGE_DISTANCE }
        // "Capa de «A Cidade e as Serras», de Eça de Queirós"
        val alt = img?.let { attribute(it.groupValues[1], "alt") }.orEmpty()
        val title = TITLE.find(alt)?.groupValues?.get(1) ?: return@mapNotNull null
        val author = alt.substringAfter("», de ", "").trim()
        val genres = attribute(attributes, "data-caption-desc").orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val book = FoundBook(
            source = source,
            id = url.trimEnd('/').substringAfterLast('/'),
            title = title,
            authors = listOf(author).filter { it.isNotEmpty() },
            description = "",
            subjects = genres,
            license = "CC-BY-SA-4.0",
            pageUrl = url,
            downloadUrl = url,
            coverUrl = img?.let { attribute(it.groupValues[1], "src") },
        )
        Listed(book, (listOf(title, author) + genres).joinToString(" ").lowercase())
    }.distinctBy { it.book.id }.toList()

    private fun attribute(attributes: String, name: String): String? =
        Regex("""\b$name="([^"]*)"""").find(attributes)?.groupValues?.get(1)?.let(HtmlText::toPlainText)

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Projecto Adamastor answered ${response.status.value}")
        return response
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val TIMEOUT = 30_000L
        const val TEXT_TIMEOUT = 60_000L
        const val KEPT_TEXTS = 10

        /** How far after its link a book's cover may be. */
        const val IMAGE_DISTANCE = 300
        val THUMB = Regex("""<a ([^>]*\bclass="fg-thumb"[^>]*)>""")
        val IMAGE = Regex("""<img ([^>]*)>""")
        val TITLE = Regex("""«([^»]+)»""")
        val EPUB_LINK = Regex("""<a [^>]*href="([^"]*/download/[^"]*wpdmdl=[^"]*)"[^>]*>\s*EPUB""")

        /** The edition's own pages around the work. */
        val EDITION_PAGES = Regex("""capa|titulo|ficha_tecnica|indice|contracapa""")
    }
}
