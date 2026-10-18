package com.tayra.languages.core.data.discover

import com.tayra.languages.core.data.files.FileImportException
import com.tayra.languages.core.data.files.LineReflow
import com.tayra.languages.core.data.files.pdfLines
import com.tayra.languages.core.domain.books.discover.BookCatalog
import com.tayra.languages.core.domain.books.discover.BookSource
import com.tayra.languages.core.domain.books.discover.CatalogPage
import com.tayra.languages.core.domain.books.discover.Difficulty
import com.tayra.languages.core.domain.books.discover.FoundBook
import com.tayra.languages.core.domain.morphology.Unicode
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pratham Books' StoryWeaver picture books, as Learning Equality's Kolibri Studio keeps them: a
 * 2018 copy of StoryWeaver's library, every book a PDF under CC BY 4.0. StoryWeaver's own site
 * gives a book's text only to those signed in, and Kolibri's catalog marks every book English, so
 * tools/build_storyweaver_index.py reads each PDF once for its language, level, credits and length
 * and writes a list per language into data/storyweaver/, read here through jsDelivr. A book's text
 * is its PDF's: the story's pages, without its title and credits, its page numbers, or the
 * attributions and blurb StoryWeaver ends it with. Chinese books draw some characters with their
 * Kangxi radicals (⿃ for 鳥), which are turned back into the characters.
 */
class StoryWeaverCatalog(
    private val client: HttpClient,
    private val indexUrl: String = "https://cdn.jsdelivr.net/gh/zavanton123/tayra-languages@main/data/storyweaver",
    private val storageUrl: String = "https://studio.learningequality.org/content/storage",
    /** The lines of a PDF's text layer; the platform's PDF reader, replaced in tests. */
    private val pdf: suspend (ByteArray) -> String = { bytes -> pdfLines(bytes).first },
) : BookCatalog {
    override val source = BookSource.STORYWEAVER

    private val lock = Mutex()
    private val indexes = HashMap<String, List<Entry>>()
    private val texts = LinkedHashMap<String, String>()

    /** A book as the index lists it, with the words it is found by. */
    private class Entry(val book: FoundBook, val words: String)

    override suspend fun search(languageCode: String, query: String, page: Int): CatalogPage {
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val found = index(languageCode).filter { entry -> words.all { it in entry.words } }.map { it.book }
        return CatalogPage(found.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE), hasMore = page * PAGE_SIZE < found.size, total = found.size)
    }

    override suspend fun sample(book: FoundBook): String = text(book)

    override suspend fun text(book: FoundBook): String {
        lock.withLock { texts[book.id] }?.let { return it }
        val response = client.get(book.downloadUrl) {
            // A PDF is a couple of megabytes of pictures; only its text is kept.
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        val bytes = checked(response).bodyAsBytes()
        if (bytes.size < 5 || bytes.copyOfRange(0, 5).decodeToString() != "%PDF-") throw FileImportException("StoryWeaver sent something that is not the book's PDF")
        val text = story(pdf(bytes), book.title)
        if (text.isEmpty()) throw IllegalStateException("This book is mostly pictures, with too little text to read")
        lock.withLock {
            texts[book.id] = text
            while (texts.size > KEPT_TEXTS) texts.remove(texts.keys.first())
        }
        return text
    }

    /** The language's books, as its index lists them; none when it has no index. */
    private suspend fun index(languageCode: String): List<Entry> {
        lock.withLock { indexes[languageCode] }?.let { return it }
        val response = client.get("$indexUrl/$languageCode.tsv") { timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT } }
        val entries = if (response.status == HttpStatusCode.NotFound) emptyList() else checked(response).bodyAsText().lines().mapNotNull(::entry)
        lock.withLock { indexes[languageCode] = entries }
        return entries
    }

    /** A line of an index: file id, level, words, title, authors, description and the cover's file. */
    private fun entry(line: String): Entry? {
        val cells = line.split('\t')
        if (cells.size < 6 || cells[0].length < 2) return null
        val file = cells[0]
        val level = cells[1]
        val title = cells[3]
        val authors = cells[4]
        val description = cells[5]
        val cover = cells.getOrNull(6)?.takeIf { it.length > 2 }
        val book = FoundBook(
            source = source,
            id = file,
            title = title,
            authors = authors.split(", ").filter { it.isNotBlank() },
            description = description,
            subjects = emptyList(),
            level = level.takeIf { it.isNotEmpty() }?.let { "Level $it" },
            levelDifficulty = level.toIntOrNull()?.let(::difficultyOf),
            license = "CC-BY-4.0",
            pageUrl = storage(file, "pdf"),
            downloadUrl = storage(file, "pdf"),
            coverUrl = cover?.let { storage(it, "jpg") },
        )
        return Entry(book, "$title $authors $description".lowercase())
    }

    private fun storage(file: String, extension: String) = "$storageUrl/${file[0]}/${file[1]}/$file.$extension"

    private fun checked(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("StoryWeaver's library answered ${response.status.value}")
        return response
    }

    internal companion object {
        const val PAGE_SIZE = 20
        const val TIMEOUT = 60_000L
        const val KEPT_TEXTS = 10

        /** Where a story stops: StoryWeaver's note on the platform, or the attributions when the note is not there. */
        private val STORY_END = listOf("This book was made possible by Pratham Books", "Story Attribution:", "Illustration Attributions:")
        private val CREDIT = Regex("""^(Authors?|Illustrators?|Translators?|Adapted by|Adaptation):""")
        private val PAGE_NUMBER = Regex("""^\d{1,3}$""")

        /** How far into a book its credits can be: a long title takes a few lines. */
        private const val CREDITS_WITHIN = 8

        /** Levels 1 and 2 are the easiest StoryWeaver has, 4 the hardest. */
        fun difficultyOf(level: Int): Difficulty = when {
            level <= 2 -> Difficulty.EASY
            level >= 4 -> Difficulty.HARD
            else -> Difficulty.MEDIUM
        }

        /**
         * A book's story from its PDF's lines: after the title and the credits, before StoryWeaver's
         * closing pages, without the page numbers, its lines joined into paragraphs.
         */
        fun story(lines: String, title: String): String {
            val all = lines.replace("\r\n", "\n").lines().map { radicalsAsCharacters(it).trim() }.filter { it.isNotEmpty() }
            val end = all.indexOfFirst { line -> STORY_END.any { it in line } }.takeIf { it >= 0 } ?: all.size
            var body = all.subList(0, end)
            // The cover: the title, perhaps over a few lines, then the credits.
            val credits = body.take(CREDITS_WITHIN).indexOfLast { CREDIT.containsMatchIn(it) }
            if (credits >= 0) body = body.drop(credits + 1)
            else if (body.firstOrNull()?.let { compact(it) == compact(title) } == true) body = body.drop(1)
            val story = body.filter { !PAGE_NUMBER.matches(it) }
            return LineReflow.paragraphs(story.joinToString("\n")).trim()
        }

        private fun compact(text: String) = text.lowercase().filter { it.isLetterOrDigit() }

        /** Kangxi radicals (U+2F00 to U+2FDF) as the characters they look like; NFKC on the rest would make Chinese punctuation ASCII. */
        private fun radicalsAsCharacters(line: String): String =
            if (line.none { it in '\u2F00'..'\u2FDF' }) line
            else line.map { if (it in '\u2F00'..'\u2FDF') Unicode.nfkc(it.toString()) else it.toString() }.joinToString("")
    }
}
