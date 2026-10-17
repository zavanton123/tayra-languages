package com.tayra.languages.core.domain.news

import com.tayra.languages.core.domain.books.discover.TextCoverage
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.parse.isSupported
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.ReadingService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Instant

/** Whether a news outlet writes for everyone, or in simple language for learners and plain-language readers. */
enum class NewsLevel { EASY, STANDARD }

/** A news outlet's feed in one language. */
data class NewsSource(
    /** ISO 639-1 code of the language it writes in. */
    val languageCode: String,
    val name: String,
    val feedUrl: String,
    val homepage: String,
    val level: NewsLevel = NewsLevel.STANDARD,
    /** Whether the feed carries each article whole, so its page need not be read (some pages show their text only to a browser). */
    val textInFeed: Boolean = false,
)

/** One article a feed lists. */
data class NewsArticle(
    val source: NewsSource,
    val url: String,
    val title: String,
    /** The feed's summary, plain text; empty when it gives none. */
    val summary: String,
    val published: Instant?,
    /** The longest text the feed gives the article, plain and whole: a summary, or the article itself. */
    val feedText: String? = null,
    /** The topics the feed files the article under ("Politics", "Sport"), as it names them. */
    val topics: List<String> = emptyList(),
    /** How long the whole article is, in the site's own measure (a Wikipedia article's size in bytes); null when unknown. */
    val length: Int? = null,
)

/** An article the reader has opened, as a text to read. */
data class NewsBook(val url: String, val bookId: Long, val lastOpened: Instant?, val isCompleted: Boolean)

/** Reads the feeds and the articles they lead to. */
interface NewsFeedReader {
    /** The articles the source's feed lists now, as it orders them. */
    suspend fun articles(source: NewsSource): List<NewsArticle>

    /** The article's text, one paragraph per line, taken from the feed or from its page. */
    suspend fun text(article: NewsArticle): String
}

/**
 * A wiki to read articles from, all free to share under CC BY-SA 4.0. [languages] are the ISO codes
 * of the languages it has an edition in, null for Wikipedia, which has one in nearly every
 * language. Wikimedia [countsViews] of its own wikis, so their articles can be listed by how much
 * they were read; the others' are picked at random.
 */
enum class Wiki(val siteName: String, val shortName: String, val countsViews: Boolean, val languages: Set<String>?) {
    WIKIPEDIA("Wikipedia", "Wikipedia", countsViews = true, languages = null),

    /** Wikipedia in simple English, for learners and children. */
    SIMPLE_WIKIPEDIA("Simple English Wikipedia", "Simple English", countsViews = true, languages = setOf("en")),

    /** The German encyclopedia for children of 8 to 13. */
    KLEXIKON("Klexikon", "Klexikon", countsViews = false, languages = setOf("de")),

    /** The Klexikon in easy German, a short sentence to a line, for children starting to read. */
    MINIKLEXIKON("MiniKlexikon", "MiniKlexikon", countsViews = false, languages = setOf("de")),

    /** Travel guides, in its open editions. */
    WIKIVOYAGE(
        "Wikivoyage", "Wikivoyage", countsViews = true,
        languages = setOf("bn", "cs", "de", "el", "en", "eo", "es", "fa", "fi", "fr", "he", "hi", "id", "it", "ja", "ml", "nl", "pl", "ps", "pt", "ro", "ru", "sv", "tr", "uk", "vi", "zh"),
    ),

    /** Textbooks and guides, a chapter to a page, in its open editions. */
    WIKIBOOKS(
        "Wikibooks", "Wikibooks", countsViews = true,
        languages = setOf(
            "af", "ar", "az", "ba", "be", "bg", "bn", "bs", "ca", "cs", "cv", "cy", "da", "de", "el", "en", "eo", "es", "et", "eu",
            "fa", "fi", "fr", "fy", "gl", "he", "hi", "hr", "hu", "hy", "ia", "id", "is", "it", "ja", "ka", "kk", "km", "ko", "ku",
            "la", "li", "lt", "mg", "mk", "ml", "mr", "ms", "ne", "nl", "no", "oc", "pa", "pl", "pt", "ro", "ru", "sa", "si", "sk",
            "sl", "sq", "sr", "sv", "ta", "te", "tg", "th", "tl", "tr", "tt", "uk", "ur", "vi", "zh",
        ),
    ),
    ;

    fun serves(languageCode: String): Boolean = languages == null || languageCode in languages
}

/** One page of a wiki's articles, each with its introduction as its feed text. */
data class WikiPage(val articles: List<NewsArticle>, val hasMore: Boolean)

/** The articles of a language's wikis: the most read (or, where views are not counted, some at random), the ones a search finds, and each one's whole text. */
interface WikiReader {
    /** The articles read most yesterday, or picked at random from a wiki that does not count views; a page at a time from 1. */
    suspend fun mostRead(wiki: Wiki, languageCode: String, page: Int): WikiPage

    suspend fun search(wiki: Wiki, languageCode: String, query: String, page: Int): WikiPage

    /** The whole article, one paragraph per line, its headings kept. */
    suspend fun wholeText(article: NewsArticle): String
}

/** The articles of a language's feeds, and the sources that failed to answer. */
data class NewsList(val articles: List<NewsArticle>, val failed: List<NewsSource>)

/**
 * The news of the language being learned: the latest articles of its feeds, judged by the words
 * the reader knows, and read as texts of their own, kept apart from the reader's books.
 */
class NewsService(
    private val reader: NewsFeedReader,
    private val wikis: WikiReader,
    private val readingService: ReadingService,
    private val bookService: BookService,
    private val books: BookRepository,
    private val catalog: List<NewsSource> = NewsSources.all,
) {
    private val lock = Mutex()

    /** The last texts read, so judging an article and then opening it fetches it once. */
    private val texts = LinkedHashMap<String, String>()

    fun sources(language: Language): List<NewsSource> {
        val code = LanguageCodes.codeFor(language.name) ?: return emptyList()
        return catalog.filter { it.languageCode == code }
    }

    /** The latest articles of the language's feeds, newest first; a feed that fails is named rather than failing the rest. */
    suspend fun latest(language: Language): NewsList = coroutineScope {
        val results = sources(language).map { source -> async { source to runCatching { reader.articles(source) } } }.awaitAll()
        val articles = results.flatMap { (_, result) -> result.getOrDefault(emptyList()) }
            .distinctBy { it.url }
            .sortedByDescending { it.published ?: Instant.DISTANT_PAST }
        NewsList(articles, results.filter { it.second.isFailure }.map { it.first })
    }

    /** How much of the article the reader knows; null for a language the app cannot split into words. */
    suspend fun coverage(article: NewsArticle, language: Language): TextCoverage? {
        if (!language.isSupported) return null
        val words = readingService.renderItems(text(article), language).filter { it.isWord && it.tokenCount == 1 }
        return TextCoverage(words.size, words.count { it.status != TermStatus.UNKNOWN })
    }

    /** The text to read for the article, made the first time it is opened; ends with [attribution]. */
    suspend fun open(article: NewsArticle, language: Language, attribution: String): Long {
        books.newsBookId(article.url)?.let { return it }
        val draft = BookDraft(languageId = language.id, title = article.title, text = "${text(article)}\n\n$attribution", sourceUri = article.url, tags = listOf("news"))
        return bookService.create(draft).also { books.linkNews(article.url, it) }
    }

    /** The wikis with an edition in [language], Wikipedia first. */
    fun wikis(language: Language): List<Wiki> {
        val code = LanguageCodes.codeFor(language.name) ?: return emptyList()
        return Wiki.entries.filter { it.serves(code) }
    }

    /** The articles of [wiki] in [language] found by [query], or the most read when it is blank. */
    suspend fun wiki(language: Language, wiki: Wiki, query: String, page: Int): WikiPage {
        val code = LanguageCodes.codeFor(language.name) ?: return WikiPage(emptyList(), hasMore = false)
        if (!wiki.serves(code)) return WikiPage(emptyList(), hasMore = false)
        return if (query.isBlank()) wikis.mostRead(wiki, code, page) else wikis.search(wiki, code, query.trim(), page)
    }

    /** The text to read for the whole of a wiki's article, apart from its introduction, which is [open]ed as the article itself; tagged with the wiki's name. */
    suspend fun openWhole(article: NewsArticle, language: Language, title: String, attribution: String): Long {
        val key = wholeKey(article)
        books.newsBookId(key)?.let { return it }
        val text = wikis.wholeText(article)
        val draft = BookDraft(languageId = language.id, title = title, text = "$text\n\n$attribution", sourceUri = article.url, tags = listOf(article.source.name.lowercase().replace(' ', '-')))
        return bookService.create(draft).also { books.linkNews(key, it) }
    }

    fun observeOpened(): Flow<Map<String, NewsBook>> = books.observeNewsBooks()

    private suspend fun text(article: NewsArticle): String {
        lock.withLock { texts[article.url] }?.let { return it }
        val text = reader.text(article)
        lock.withLock {
            texts[article.url] = text
            while (texts.size > KEPT_TEXTS) texts.remove(texts.keys.first())
        }
        return text
    }

    companion object {
        private const val KEPT_TEXTS = 60

        /** The key a whole wiki article is kept under among the news read, apart from its introduction's. */
        fun wholeKey(article: NewsArticle): String = "${article.url}#whole"
    }
}
