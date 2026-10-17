package com.tayra.languages.core.data.discover

import com.tayra.languages.core.data.files.EpubText
import com.tayra.languages.core.data.files.HtmlText as BodyText
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

/**
 * Liber Liber (Progetto Manuzio), the Italian volunteers' library, through its WordPress API, where
 * a book is a page with its catalog card and links to its files. Unsearched, it lists the books its
 * cards file under young people's fiction; a search looks in titles, and where none matches, in the
 * pages' text, which finds an author's books; either is kept to the pages of books.
 * A book's ePUB is at liberliber.eu under the author's surname and the work's name; where that guess
 * misses, its download page, almost 2 MB, names it. The ePUB is read whole to judge it, without the
 * cover, the notes on the edition, the appeal for donations and the contents; the last few are kept,
 * so adding one just judged downloads nothing again.
 */
class LiberLiberCatalog(
    private val client: HttpClient,
    private val baseUrl: String = "https://liberliber.it",
    private val mediaUrl: String = "https://www.liberliber.eu/mediateca/libri",
) : BookCatalog {
    override val source = BookSource.LIBER_LIBER

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private val texts = LinkedHashMap<String, String>()

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        if (query.isEmpty()) return pages(YOUNG_PEOPLE, page, titlesOnly = false)
        val titled = pages(query, page, titlesOnly = true)
        return if (titled.total == 0) pages(query, page, titlesOnly = false) else titled
    }

    /** A page of the books among the pages WordPress finds for [words]; [CatalogPage.total] counts the pages it found, books or not. */
    private suspend fun pages(words: String, page: Int, titlesOnly: Boolean): CatalogPage {
        val response = client.get("$baseUrl/wp-json/wp/v2/pages") {
            parameter("search", words)
            if (titlesOnly) parameter("search_columns", "post_title")
            parameter("per_page", PAGE_SIZE)
            parameter("page", page)
            parameter("_fields", "id,link,title,content")
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        // WordPress answers 400 for a page past the last.
        if (response.status == HttpStatusCode.BadRequest && page > 1) return CatalogPage(emptyList(), hasMore = false, total = null)
        val pages = checked(response).headers["X-WP-TotalPages"]?.toIntOrNull() ?: page
        val found = response.headers["X-WP-Total"]?.toIntOrNull()
        return CatalogPage(parse(response.bodyAsText()), hasMore = page < pages, total = found)
    }

    override suspend fun sample(book: FoundBook): String = text(book)

    override suspend fun text(book: FoundBook): String {
        lock.withLock { texts[book.id] }?.let { return it }
        val epub = epub(book)
        // The documents come in the spine's order, so the work's first page ends the edition's.
        var started = false
        val text = EpubText.read(epub) { _, html, _ ->
            !isEditionPage(BodyText.text(html), started).also { edition -> if (!edition) started = true }
        }.text
        lock.withLock {
            texts[book.id] = text
            while (texts.size > KEPT_TEXTS) texts.remove(texts.keys.first())
        }
        return text
    }

    /** The book's ePUB: at the address its names make, or else at the one its download page gives. */
    private suspend fun epub(book: FoundBook): ByteArray {
        suspend fun get(url: String) = client.get(url) {
            // Books are kept here as text; the client's cache would keep the whole ePUB, pictures and all.
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TEXT_TIMEOUT; socketTimeoutMillis = TEXT_TIMEOUT }
        }
        val guessed = get(book.downloadUrl)
        if (guessed.status.isSuccess()) return guessed.bodyAsBytes()
        val page = get("$baseUrl/opere/download/?op=${book.id}&type=opera_url_epub")
        val url = EPUB_URL.find(checked(page).bodyAsText())?.groupValues?.get(1) ?: throw IllegalStateException("Liber Liber has no ePUB of this book")
        return checked(get(url)).bodyAsBytes()
    }

    internal fun parse(body: String): List<FoundBook> =
        (json.parseToJsonElement(body) as? JsonArray).orEmpty().mapNotNull { item -> (item as? JsonObject)?.let(::book) }

    private fun book(page: JsonObject): FoundBook? {
        val content = (page["content"] as? JsonObject)?.string("rendered").orEmpty()
        // Author pages and audiobooks have no ePUB.
        if ("opera_url_epub" !in content) return null
        val id = (page["id"] as? JsonPrimitive)?.intOrNull ?: return null
        val link = page.string("link") ?: return null
        val title = (page["title"] as? JsonObject)?.string("rendered")?.let(HtmlText::toPlainText)?.takeIf { it.isNotEmpty() } ?: return null
        val fields = cardFields(content)
        val author = fields["autore"]?.substringBefore('(')?.trim()
        val surname = author?.split(' ')?.lastOrNull { it.isNotBlank() }?.let(::slug).orEmpty()
        val work = link.trimEnd('/').substringAfterLast('/').replace('-', '_')
        return FoundBook(
            source = source,
            id = id.toString(),
            title = title,
            authors = listOfNotNull(author?.takeIf { it.isNotEmpty() }),
            description = fields["descrizione breve"].orEmpty(),
            subjects = emptyList(),
            license = license(fields["licenza"].orEmpty()),
            pageUrl = link,
            downloadUrl = "$mediaUrl/${surname.take(1)}/$surname/$work/epub/${surname}_$work.epub",
        )
    }

    /** The book's catalog card, "titolo:", "autore:", "licenza:" and the rest, each label followed by its value. */
    private fun cardFields(html: String): Map<String, String> {
        val lines = html.replace(Regex("<[^>]+>"), "\n").lines().map { HtmlText.toPlainText(it) }.filter { it.isNotEmpty() }
        return lines.mapIndexedNotNull { i, line ->
            if (line.endsWith(':') && line.length < LABEL_MAX) lines.getOrNull(i + 1)?.takeIf { !it.endsWith(':') }?.let { line.dropLast(1).lowercase() to it } else null
        }.toMap()
    }

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Liber Liber answered ${response.status.value}")
        return response
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    internal companion object {
        const val PAGE_SIZE = 20
        const val TIMEOUT = 30_000L
        const val TEXT_TIMEOUT = 60_000L
        const val KEPT_TEXTS = 10
        const val LABEL_MAX = 40

        /** The word the cards of young people's fiction share ("FICTION PER RAGAZZI"). */
        const val YOUNG_PEOPLE = "ragazzi"
        val EPUB_URL = Regex("""value="(https://www\.liberliber\.eu/[^"]+\.epub)"""")

        /** "Attribuzione - Non commerciale - Condividi allo stesso modo 4.0" as "CC-BY-NC-SA-4.0"; Liber Liber's own terms otherwise. */
        fun license(text: String): String {
            if ("Creative Commons" !in text && "Attribuzione" !in text) return "Non-commercial use"
            val terms = buildList {
                add("BY")
                if ("Non commerciale" in text) add("NC")
                if ("Non opere derivate" in text) add("ND")
                if ("Condividi allo stesso modo" in text) add("SA")
            }
            val version = Regex("""\d\.\d""").find(text)?.value
            return (listOf("CC") + terms + listOfNotNull(version)).joinToString("-")
        }

        /** An ASCII slug, as liberliber.eu names its folders: "Dostoevskij" for "Dostoevskij", "dannunzio" for "D’Annunzio". */
        fun slug(name: String): String = name.lowercase()
            .map { c -> ACCENTS[c] ?: c }.joinToString("")
            .filter { it in 'a'..'z' || it in '0'..'9' }

        private val ACCENTS = mapOf('à' to 'a', 'á' to 'a', 'â' to 'a', 'ä' to 'a', 'è' to 'e', 'é' to 'e', 'ê' to 'e', 'ë' to 'e', 'ì' to 'i', 'í' to 'i', 'î' to 'i', 'ï' to 'i',
            'ò' to 'o', 'ó' to 'o', 'ô' to 'o', 'ö' to 'o', 'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u', 'ç' to 'c', 'ñ' to 'n', 'č' to 'c', 'š' to 's', 'ž' to 'z')

        /**
         * The cover, the notes on the edition, the appeal for donations, the contents or the title
         * page, which open every ePUB before the work has [started]: no part of the work.
         */
        fun isEditionPage(text: String, started: Boolean): Boolean {
            val body = text.trim()
            return "QUESTO E-BOOK:" in body || "liberliber.it/aiuta" in body ||
                !started && (body.startsWith("Indice") || body.startsWith("Table of Contents") || body.length < TITLE_PAGE_LETTERS)
        }

        private const val TITLE_PAGE_LETTERS = 300
    }
}
