package com.tayra.languages.core.data.news

import com.tayra.languages.core.data.files.HtmlText
import com.tayra.languages.core.data.files.Markup
import com.tayra.languages.core.data.files.MarkupToken
import com.tayra.languages.core.domain.news.NewsArticle
import com.tayra.languages.core.domain.news.NewsFeedReader
import com.tayra.languages.core.domain.news.NewsSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/**
 * News from RSS 2.0, RSS 1.0 (RDF) and Atom feeds. An article's text is the feed's own when the
 * feed carries the whole of it, and otherwise taken from the article's page ([ArticleText]).
 */
class FeedNewsReader(private val client: HttpClient) : NewsFeedReader {

    override suspend fun articles(source: NewsSource): List<NewsArticle> {
        val response = client.get(source.feedUrl) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        return parse(checked(response, source.name).bodyAsText(), source)
    }

    override suspend fun text(article: NewsArticle): String {
        val inFeed = article.feedText?.takeIf { it.isNotBlank() }
        if (inFeed != null && (article.source.textInFeed || ArticleText.letters(inFeed) >= FULL_TEXT_LETTERS)) return inFeed
        val response = client.get(article.url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.CacheControl, "no-store")
            timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
        }
        val text = ArticleText.extract(checked(response, article.source.name).bodyAsText())
        // A page whose text could not be found still has the feed's text to read.
        return text.takeIf { ArticleText.letters(it) >= MIN_ARTICLE_LETTERS } ?: inFeed
            ?: throw IllegalStateException("Could not find the article's text")
    }

    private fun checked(response: HttpResponse, name: String): HttpResponse {
        if (!response.status.isSuccess()) throw IllegalStateException("$name answered ${response.status.value}")
        return response
    }

    companion object {
        /** News sites, like Wikimedia, are asked politely, with a way to reach the app's makers. */
        const val USER_AGENT = "TayraLanguages/1.0 (https://github.com/zavanton123/tayra-languages)"
        private const val TIMEOUT = 30_000L

        /** A feed item this long, in letters, is the article itself, not a summary of it. */
        private const val FULL_TEXT_LETTERS = 800
        private const val MIN_ARTICLE_LETTERS = 300

        private val ITEMS = setOf("item", "entry")

        /** The articles of an RSS or Atom feed, in its order. */
        internal fun parse(feed: String, source: NewsSource): List<NewsArticle> {
            val articles = mutableListOf<NewsArticle>()
            var fields: MutableMap<String, String>? = null
            val topics = mutableListOf<String>()
            var field: String? = null
            val value = StringBuilder()
            for (token in Markup.tokens(feed)) {
                when (token) {
                    is MarkupToken.Open -> when {
                        token.name in ITEMS -> { fields = mutableMapOf(); topics.clear() }
                        fields == null -> Unit
                        // Atom names a topic in an attribute.
                        token.name == "category" && token.attributes["term"] != null -> topics += (token.attributes["label"] ?: token.attributes.getValue("term"))
                        // Atom's links: the article's page is the alternate one, or the one with no rel.
                        token.name == "link" && token.attributes["href"] != null -> {
                            val rel = token.attributes["rel"] ?: "alternate"
                            if (rel == "alternate" && "link" !in fields) fields["link"] = token.attributes.getValue("href")
                        }
                        // A self-closing tag, such as a picture's, holds no text to keep.
                        field == null && !token.selfClosing -> { field = token.name; value.clear() }
                    }
                    is MarkupToken.Text -> if (field != null) value.append(token.text)
                    is MarkupToken.Close -> when {
                        token.name in ITEMS -> {
                            fields?.let { item(it, source)?.let { article -> articles += article.copy(topics = topics.map(::plain).filter { it.isNotEmpty() }.distinct()) } }
                            fields = null
                        }
                        // RSS lists an article's topics one after another, each its own element.
                        token.name == field && (field == "category" || field == "dc:subject") -> {
                            topics += value.toString().trim()
                            field = null
                        }
                        token.name == field -> {
                            val item = fields
                            val name = field
                            if (item != null && name != null && item[name].isNullOrBlank()) item[name] = value.toString().trim()
                            field = null
                        }
                    }
                }
            }
            return articles
        }

        private fun item(fields: Map<String, String>, source: NewsSource): NewsArticle? {
            val url = (fields["link"] ?: fields["guid"]?.takeIf { it.startsWith("http") })?.trim()?.let { articleUrl(it, source) } ?: return null
            val title = fields["title"]?.let(::plain)?.takeIf { it.isNotEmpty() } ?: return null
            val full = (fields["content:encoded"] ?: fields["content"])?.let(::plain)?.let(::withoutPostNotice)
            val summary = (fields["description"] ?: fields["summary"])?.let(::plain)?.let(::withoutPostNotice).orEmpty()
            val body = listOfNotNull(full, summary).filter { it.isNotEmpty() }.maxByOrNull(ArticleText::letters)
            // Some editions carry the odd article of their English one.
            if (source.languageCode != "en" && inLatinLetters(title) && looksEnglish("$title ${body.orEmpty().take(ENGLISH_SAMPLE)}")) return null
            return NewsArticle(
                source = source,
                url = url,
                title = title,
                summary = summary.ifEmpty { full.orEmpty() }.take(SUMMARY_CHARACTERS),
                published = listOf("pubdate", "published", "dc:date", "updated").firstNotNullOfOrNull { fields[it]?.let(::date) },
                feedText = body,
            )
        }

        private const val SUMMARY_CHARACTERS = 400
        private const val ENGLISH_SAMPLE = 1_000

        /** Words common in English and in no language written with the same letters ("in", "is" and "was" are Dutch and German too). */
        private val ENGLISH_WORDS = setOf("the", "and", "that", "with", "which", "this", "are", "were", "have", "has", "from", "they", "their", "it’s", "it's", "how", "what")

        /**
         * Whether a text is in English, by the share of its letters in words only English uses;
         * letters rather than words, as Chinese and Japanese run a sentence together as one "word".
         */
        internal fun looksEnglish(text: String): Boolean {
            val words = text.lowercase().split(Regex("[^\\p{L}’']+")).filter { it.isNotEmpty() }
            if (words.size < MIN_WORDS_TO_JUDGE) return false
            val letters = words.sumOf { it.length }
            return words.filter { it in ENGLISH_WORDS }.sumOf { it.length } * 100 >= letters * ENGLISH_SHARE
        }

        private const val MIN_WORDS_TO_JUDGE = 6

        /** In English some 10% of the letters are in these words; in other languages next to none. */
        private const val ENGLISH_SHARE = 4

        /** Whether most of a text's letters are Latin ones, as an English title's are, whatever names a Thai or Mongolian one quotes. */
        private fun inLatinLetters(text: String): Boolean {
            val letters = text.filter { it.isLetter() }
            return letters.count { it.code < LATIN_END } * 2 > letters.length
        }

        /** Where the Latin alphabets end, the extended ones included. */
        private const val LATIN_END = 0x250

        /** WordPress's "The post … appeared first on …", in English whatever the site's language, which is no part of the article. */
        private val POST_NOTICE = Regex("""\s*The post .{1,300}? appeared first on [^\n]*""")

        private fun withoutPostNotice(text: String): String = POST_NOTICE.replace(text, "").trim()

        /**
         * The address to read an article at, where a feed's own does not lead to it: UN News links
         * through a "/feed/view" that answers 406, and BBC Chinese's simplified feed links to the
         * traditional pages.
         */
        internal fun articleUrl(link: String, source: NewsSource): String = when {
            "news.un.org/feed/view/" in link -> link.replace("news.un.org/feed/view/", "news.un.org/")
            "/zhongwen/simp/" in source.feedUrl -> link.replace(Regex("/trad(?=[?#]|$)"), "/simp")
            else -> link
        }

        /** Feed text is often HTML, escaped or in CDATA. */
        private fun plain(text: String): String = HtmlText.text(text).trim()

        private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
        private val ZONES = mapOf("UT" to "+00:00", "UTC" to "+00:00", "GMT" to "+00:00", "Z" to "+00:00", "EST" to "-05:00", "EDT" to "-04:00", "CST" to "-06:00", "CDT" to "-05:00", "MST" to "-07:00", "MDT" to "-06:00", "PST" to "-08:00", "PDT" to "-07:00", "CET" to "+01:00", "CEST" to "+02:00", "MSK" to "+03:00")
        private val RFC_822 = Regex("""(?:\w+,\s*)?(\d{1,2})\s+(\w{3})\w*\s+(\d{2,4})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?\s*([+-]\d{4}|[A-Za-z]+)?""")
        private val ISO = Regex("""(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?(?:\.\d+)?)?\s*(Z|[+-]\d{2}:?\d{2})?""")

        /** A feed's date: RFC 822 in RSS ("Fri, 16 Oct 2026 10:00:00 +0200"), ISO 8601 in Atom and RDF. */
        internal fun date(text: String): Instant? {
            val trimmed = text.trim()
            ISO.matchEntire(trimmed)?.let { m ->
                val (y, mo, d, h, mi, s, zone) = m.destructured
                return at(y.toInt(), mo.toInt(), d.toInt(), h.toIntOrNull() ?: 0, mi.toIntOrNull() ?: 0, s.toIntOrNull() ?: 0, zone.ifEmpty { "Z" })
            }
            RFC_822.find(trimmed)?.let { m ->
                val (d, mon, y, h, mi, s, zone) = m.destructured
                val month = MONTHS.indexOf(mon.lowercase().take(3)).takeIf { it >= 0 } ?: return null
                val year = y.toInt().let { if (it < 100) 2000 + it else it }
                return at(year, month + 1, d.toInt(), h.toInt(), mi.toInt(), s.toIntOrNull() ?: 0, zone.ifEmpty { "GMT" })
            }
            return null
        }

        private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, zone: String): Instant? = runCatching {
            val offset = ZONES[zone.uppercase()] ?: zone.takeIf { it.startsWith("+") || it.startsWith("-") }?.let { if (':' in it) it else "${it.take(3)}:${it.drop(3)}" } ?: "+00:00"
            LocalDateTime(LocalDate(year, month, day), LocalTime(hour, minute, second)).toInstant(UtcOffset.parse(offset))
        }.getOrNull()
    }
}
