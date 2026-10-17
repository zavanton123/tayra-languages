package com.tayra.languages.core.data.discover

import com.tayra.languages.core.domain.books.discover.BookCatalog
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.CatalogPage
import com.tayra.languages.core.domain.books.discover.FoundBook
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Global Storybooks: short illustrated stories for children, a few sentences a page, translated into
 * scores of languages and shared under CC BY. Its collections keep each story as a Markdown file on
 * GitHub (`<language>/<number>_<slug>.md`); jsDelivr lists them and serves them, with no limit on
 * requests. The African Storybook's stories come in most collections under the same number, so a
 * language has each of them once. A collection's list is fetched once; a page of stories is read as
 * it is listed, for each one's title and author, and kept, so judging and adding them read nothing
 * again.
 */
class StorybooksCatalog(
    private val client: HttpClient,
    private val listUrl: String = "https://data.jsdelivr.com/v1/package/gh/global-asp",
    private val fileUrl: String = "https://cdn.jsdelivr.net/gh/global-asp",
) : BookCatalog {
    override val source = BookSource.STORYBOOKS

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var files: Map<String, List<String>>? = null
    private val texts = LinkedHashMap<String, String>()

    /** A story file of a collection, with the words it is found by. */
    private class Story(val book: FoundBook, val number: String, val words: String)

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val stories = stories(languageCode).filter { story -> words.all { it in story.words } }.map { it.book }
        val books = coroutineScope { stories.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE).map { async { described(it) } }.awaitAll() }
        return CatalogPage(books, hasMore = page * PAGE_SIZE < stories.size, total = stories.size)
    }

    override suspend fun sample(book: FoundBook): String = text(book)

    override suspend fun text(book: FoundBook): String = StoryMarkdown.text(markdown(book))

    /** The story with the title and author its file gives; as listed when the file cannot be read. */
    private suspend fun described(book: FoundBook): FoundBook {
        val markdown = runCatching { markdown(book) }.getOrNull() ?: return book
        return book.copy(
            title = StoryMarkdown.title(markdown) ?: book.title,
            authors = listOfNotNull(StoryMarkdown.credit(markdown, "Text")),
        )
    }

    private suspend fun markdown(book: FoundBook): String {
        lock.withLock { texts[book.id] }?.let { return it }
        val response = client.get(book.downloadUrl) { timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT } }
        val markdown = checked(response).bodyAsText()
        lock.withLock {
            texts[book.id] = markdown
            while (texts.size > KEPT_TEXTS) texts.remove(texts.keys.first())
        }
        return markdown
    }

    /** The stories in the language, those numbered after the African Storybook's first, in order, then each other collection's. */
    private suspend fun stories(languageCode: String): List<Story> {
        val listed = collections()
        val all = COLLECTIONS.flatMap { collection ->
            listed[collection].orEmpty().mapNotNull { path -> story(collection, path, languageCode) }
        }
        val shared = all.filter { it.book.id.substringBefore('/') in ASP_NUMBERED }.distinctBy { it.number }.sortedBy { it.number }
        return shared + all.filter { it.book.id.substringBefore('/') !in ASP_NUMBERED }
    }

    /** Every collection's story files, as "/<language>/<file>.md". */
    private suspend fun collections(): Map<String, List<String>> = lock.withLock { files } ?: coroutineScope {
        val listed = COLLECTIONS.map { collection ->
            async {
                collection to runCatching {
                    val response = client.get("$listUrl/$collection@master/flat") { timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT } }
                    (json.parseToJsonElement(checked(response).bodyAsText()).jsonObject["files"] as? JsonArray).orEmpty()
                        .mapNotNull { ((it as? JsonObject)?.get("name") as? JsonPrimitive)?.contentOrNull }
                        .filter { it.endsWith(".md") && it.count { c -> c == '/' } == 2 }
                }.getOrNull()
            }
        }.awaitAll().toMap()
        // A collection that could not be listed is asked again next time.
        if (listed.values.none { it == null }) lock.withLock { files = listed.mapValues { it.value.orEmpty() } }
        listed.mapValues { it.value.orEmpty() }
    }

    private fun story(collection: String, path: String, languageCode: String): Story? {
        val folder = path.substring(1).substringBefore('/')
        if (!servesLanguage(folder, languageCode)) return null
        val file = path.substringAfterLast('/').removeSuffix(".md")
        val number = file.substringBefore('_')
        // "0001_um-homem-muito-alto": the title as the file name spells it, until the story is read.
        val title = file.substringAfter('_', file).replace('-', ' ').replaceFirstChar { it.uppercase() }
        val book = FoundBook(
            source = source,
            id = "$collection/$folder/$file",
            title = title,
            authors = emptyList(),
            description = "",
            subjects = emptyList(),
            license = "CC-BY",
            pageUrl = "https://github.com/global-asp/$collection/blob/master/$folder/${"$file.md".encodeURLPathPart()}",
            downloadUrl = "$fileUrl/$collection@master/$folder/${"$file.md".encodeURLPathPart()}",
        )
        return Story(book, number, title.lowercase())
    }

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("Global Storybooks answered ${response.status.value}")
        return response
    }

    internal companion object {
        const val PAGE_SIZE = 20
        const val TIMEOUT = 30_000L
        const val KEPT_TEXTS = 200

        /**
         * The collections: Storybooks Canada, Norway, UK, Jamaica, Kenya and Uganda and the African
         * Storybook, which number the African Storybook's stories alike; then LIDA's, Pratham Books'
         * (Indian languages) and Indigenous Storybooks'.
         */
        val COLLECTIONS = listOf("sbc-source", "sbno-source", "sbuk-source", "sbjm-source", "sbk-source", "sbug-source", "asp-source", "lida-source", "pb-source", "isb-source")
        val ASP_NUMBERED = setOf("sbc-source", "sbno-source", "sbuk-source", "sbjm-source", "sbk-source", "sbug-source", "asp-source")

        /** Whether a collection's folder holds the language: "pt", "ha-ne" for Hausa, "nb" (Bokmål) for Norwegian. */
        fun servesLanguage(folder: String, languageCode: String): Boolean =
            folder == languageCode || folder.substringBefore('-') == languageCode || (languageCode == "no" && folder == "nb")
    }
}

/** A Global Storybooks story file: "# Title", the pages after "##", and the credits as a "* Key: value" list. */
object StoryMarkdown {
    fun text(markdown: String): String =
        markdown.replace("\r\n", "\n").lines().map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("*") && !it.startsWith("![") }
            .joinToString("\n")

    fun title(markdown: String): String? = markdown.lines().firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim()?.takeIf { it.isNotEmpty() }

    /** A credit from the list that ends the story: "* Text: Cornelius Gulere". */
    fun credit(markdown: String, key: String): String? =
        markdown.lines().firstNotNullOfOrNull { line -> line.trim().removePrefix("*").trim().takeIf { it.startsWith("$key:") }?.substringAfter(':')?.trim() }
            ?.takeIf { it.isNotEmpty() }
}
