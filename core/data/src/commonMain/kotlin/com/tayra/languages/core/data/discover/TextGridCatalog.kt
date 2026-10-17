package com.tayra.languages.core.data.discover

import com.tayra.languages.core.data.network.HtmlText
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

/**
 * The TextGrid Repository's Digitale Bibliothek: German literature from the Middle Ages to the early
 * 20th century, each tale, poem, story or novel a text of its own, shared under CC BY 3.0. Unsearched,
 * it lists the Grimms' tales and legends; a search matches every word against titles and authors
 * (its plain search also matches the texts themselves, which finds too much). A text comes from the
 * repository's aggregator as plain text, without the author and title it opens with; the last ones
 * read are kept, so adding one just judged downloads nothing again.
 */
class TextGridCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://textgridlab.org/1.0",
) : BookCatalog {
    override val source = BookSource.TEXTGRID

    private val lock = Mutex()
    private val texts = LinkedHashMap<String, String>()

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        val words = query.split(Regex("\\s+")).map { it.replace(LUCENE_SPECIAL, "") }.filter { it.isNotEmpty() }
        val lucene = if (words.isEmpty()) GRIMM else words.joinToString(" AND ") { "(title:$it* OR edition.agent.value:$it*)" }
        val response = client.get("$baseUrl/tgsearch-public/search") {
            parameter("q", lucene)
            parameter("filter", "format:text/xml")
            parameter("filter", "project.id:$DIGITAL_LIBRARY")
            parameter("limit", PAGE_SIZE)
            if (page > 1) parameter("start", (page - 1) * PAGE_SIZE)
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        return parse(checked(response).bodyAsText(), page)
    }

    override suspend fun sample(book: FoundBook): String = text(book)

    override suspend fun text(book: FoundBook): String {
        lock.withLock { texts[book.id] }?.let { return it }
        val response = client.get(book.downloadUrl) { timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT } }
        val text = clean(checked(response).bodyAsText(), book)
        lock.withLock {
            texts[book.id] = text
            while (texts.size > KEPT_TEXTS) texts.remove(texts.keys.first())
        }
        return text
    }

    internal fun parse(xml: String, page: Int): CatalogPage {
        val total = HITS.find(xml)?.groupValues?.get(1)?.toIntOrNull()
        val books = xml.split("<tgs:result").drop(1).mapNotNull(::book)
            // A biography of the author, a preface or notes are no reading of the author's own.
            .filter { book -> APPARATUS.none { book.title.startsWith(it) } }
        return CatalogPage(books, hasMore = total != null && page * PAGE_SIZE < total, total = total)
    }

    private fun book(result: String): FoundBook? {
        val uri = URI.find(result)?.groupValues?.get(1) ?: return null
        val title = TITLE.find(result)?.groupValues?.get(1)?.let(HtmlText::toPlainText)?.takeIf { it.isNotEmpty() } ?: return null
        // "Kafka, Franz": the name as it is said; "Grimm, Jacob und Wilhelm" too.
        val author = AUTHOR.find(result.substringBefore('>'))?.groupValues?.get(1)?.let(HtmlText::toPlainText)
            ?.let { name -> if (',' in name) "${name.substringAfter(',').trim()} ${name.substringBefore(',').trim()}" else name }
        return FoundBook(
            source = source,
            id = uri,
            title = title,
            authors = listOfNotNull(author?.takeIf { it.isNotBlank() }),
            description = "",
            subjects = emptyList(),
            license = "CC-BY-3.0",
            pageUrl = "https://textgridrep.org/browse/$uri",
            downloadUrl = "$baseUrl/aggregator/text/$uri",
        )
    }

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("TextGrid answered ${response.status.value}")
        return response
    }

    internal companion object {
        const val PAGE_SIZE = 20
        const val TIMEOUT = 30_000L
        const val TEXT_TIMEOUT = 60_000L
        const val KEPT_TEXTS = 40
        const val DIGITAL_LIBRARY = "TGPR-372fe6dc-57f2-6cd4-01b5-2c4bbefcfd3c"
        const val GRIMM = "edition.agent.value:\"Grimm, Jacob und Wilhelm\""
        val APPARATUS = listOf("Biographie", "Anhang", "Anmerkung", "Vorrede", "Vorbemerkung", "Vorwort", "Einleitung")
        val LUCENE_SPECIAL = Regex("""[+\-&|!(){}\[\]^"~*?:\\/]""")
        val HITS = Regex("""<tgs:response[^>]*\bhits="(\d+)"""")
        val URI = Regex("""<textgridUri[^>]*>([^<]+)</textgridUri>""")
        val TITLE = Regex("""<title>([^<]*)</title>""")
        val AUTHOR = Regex("""\bauthor="([^"]*)"""")

        /** The text without the author and title it opens with, and without a heading said twice ("I", "I"). */
        fun clean(raw: String, book: FoundBook): String {
            val lines = raw.replace("\r\n", "\n").lines().map { it.trim() }.filter { it.isNotEmpty() }
            val opening = (book.authors + book.title).map { it.trimEnd('.').lowercase() }.toSet()
            val kept = lines.dropWhile { it.trimEnd('.').lowercase() in opening }
            return kept.filterIndexed { i, line -> i == 0 || line != kept[i - 1] }.joinToString("\n")
        }
    }
}
