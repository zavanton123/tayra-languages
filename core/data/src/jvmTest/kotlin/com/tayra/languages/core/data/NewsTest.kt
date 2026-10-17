package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.news.ArticleText
import com.tayra.languages.core.data.news.FeedNewsReader
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.domain.books.discover.TextCoverage
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.news.NewsArticle
import com.tayra.languages.core.domain.news.NewsFeedReader
import com.tayra.languages.core.domain.news.NewsLevel
import com.tayra.languages.core.domain.news.NewsService
import com.tayra.languages.core.domain.news.NewsSource
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.TermService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/** News: the feeds read, the articles' texts found on their pages, judged, and read as texts kept apart from the books. */
class NewsTest {
    private val source = NewsSource("pt", "Notícias", "https://news.example/rss", "https://news.example", NewsLevel.STANDARD)

    @Test
    fun anRssFeedListsItsArticlesWithTheirDates() {
        val feed = """<?xml version="1.0"?><rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/" xmlns:media="http://search.yahoo.com/mrss/"><channel>
            <title>Notícias</title><link>https://news.example</link>
            <item><title>O gato voltou</title><link>https://news.example/gato</link>
              <media:content url="https://news.example/gato.jpg" medium="image" />
              <description><![CDATA[<p>O gato voltou para <b>casa</b>.</p>]]></description>
              <pubDate>Fri, 16 Oct 2026 10:30:00 +0200</pubDate></item>
            <item><title>Sem link</title><description>Nada</description></item>
            <item><title>A &amp; B</title><guid>https://news.example/ab</guid><pubDate>Thu, 15 Oct 2026 08:00:00 GMT</pubDate>
              <content:encoded><![CDATA[<p>${"Uma frase longa sobre a cidade e o mar. ".repeat(30)}</p>]]></content:encoded></item>
            </channel></rss>"""
        val (gato, ab) = FeedNewsReader.parse(feed, source)
        assertEquals("O gato voltou", gato.title)
        assertEquals("https://news.example/gato", gato.url)
        // The picture before the summary does not swallow it.
        assertEquals("O gato voltou para casa.", gato.summary)
        assertEquals(Instant.parse("2026-10-16T08:30:00Z"), gato.published)
        assertEquals("O gato voltou para casa.", gato.feedText)
        assertEquals("A & B", ab.title)
        assertEquals("https://news.example/ab", ab.url)
        assertEquals(Instant.parse("2026-10-15T08:00:00Z"), ab.published)
        assertTrue(ab.feedText!!.startsWith("Uma frase longa"), "a feed that carries the whole article gives it")
    }

    @Test
    fun anArticlesTopicsAreTheFeedsCategories() {
        val rss = """<rss><channel><category>Channel topic</category><item><title>T</title><link>https://news.example/t</link>
            <category>Política</category><category><![CDATA[Brasil]]></category><category>Política</category><dc:subject>Eleições</dc:subject></item></channel></rss>"""
        assertEquals(listOf("Política", "Brasil", "Eleições"), FeedNewsReader.parse(rss, source).single().topics)
        val atom = """<feed><entry><title>A</title><link href="https://news.example/a"/><category term="sport" label="Desporto"/><category term="futebol"/></entry></feed>"""
        assertEquals(listOf("Desporto", "futebol"), FeedNewsReader.parse(atom, source).single().topics)
    }

    @Test
    fun anAtomFeedAndAnRdfFeedToo() {
        val atom = """<feed xmlns="http://www.w3.org/2005/Atom"><entry><title type="html">Notícia</title>
            <link rel="enclosure" href="https://news.example/audio.mp3"/><link rel="alternate" href="https://news.example/a"/>
            <summary>Resumo.</summary><updated>2026-10-16T12:00:00+03:00</updated><author><name>Ana</name></author></entry></feed>"""
        val article = FeedNewsReader.parse(atom, source).single()
        assertEquals("https://news.example/a", article.url)
        assertEquals("Resumo.", article.summary)
        assertEquals(Instant.parse("2026-10-16T09:00:00Z"), article.published)

        val rdf = """<rdf:RDF><channel><title>DW</title></channel><item rdf:about="https://news.example/b"><title>B</title>
            <link>https://news.example/b</link><description>Texto.</description><dc:date>2026-10-16T21:37:00Z</dc:date></item></rdf:RDF>"""
        assertEquals(Instant.parse("2026-10-16T21:37:00Z"), FeedNewsReader.parse(rdf, source).single().published)
    }

    @Test
    fun anArticlesTextIsTheParagraphsUnderTheElementHoldingMostOfThem() {
        val body = (1..6).joinToString("") { "<div class=\"block\"><p>Parágrafo número $it do artigo, com bastante texto para contar como parte dele.</p></div>" }
        val html = """<html><body><header><p>Menu do site com muitas palavras que não fazem parte do artigo de modo algum.</p></header>
            <nav><p>Mais lidas: uma lista de outras notícias que não são esta, com títulos longos.</p></nav>
            <main><article class="sidebar-1"><h1>Título</h1><p>Primeiro parágrafo, sem fecho, como o HTML permite que se escreva às vezes.
            $body<figure><img src="x.jpg"><figcaption>Legenda da foto, que não é texto do artigo e não deve aparecer aqui.</figcaption></figure>
            <p>Último parágrafo do artigo, depois da foto, com o fim da notícia.</p></article></main>
            <footer><p>Todos os direitos reservados, um texto de rodapé bem comprido para atrapalhar.</p></footer></body></html>"""
        val text = ArticleText.extract(html)
        val lines = text.lines()
        assertEquals("Primeiro parágrafo, sem fecho, como o HTML permite que se escreva às vezes.", lines.first())
        assertEquals(8, lines.size, text)
        assertEquals("Último parágrafo do artigo, depois da foto, com o fim da notícia.", lines.last())
        assertTrue(listOf("Menu", "Mais lidas", "Legenda", "direitos").none { it in text }, text)
    }

    @Test
    fun aPageThatShowsLessThanItsDataGivesItsDataText() {
        val whole = (1..10).joinToString("\\n") { "Frase $it do texto completo do artigo, que a página só mostra em parte." }
        val html = """<html><head><script type="application/ld+json">{"@context": "https://schema.org", "@graph": [{"@type": "WebPage"}, {"@type": "NewsArticle", "headline": "T", "articleBody": "$whole"}]}</script></head>
            <body><article><p>Frase 1 do texto completo do artigo, que a página só mostra em parte.</p><p>Assine para continuar a ler esta notícia.</p></article></body></html>"""
        assertEquals((1..10).joinToString("\n") { "Frase $it do texto completo do artigo, que a página só mostra em parte." }, ArticleText.extract(html))
    }

    @Test
    fun aSourceThatCarriesItsArticlesInTheFeedIsReadFromIt() = runTest {
        var pages = 0
        val client = HttpClient(MockEngine { pages++; respond("", HttpStatusCode.OK) }) { install(HttpTimeout) }
        val ruv = NewsSource("is", "RÚV", "https://www.ruv.is/rss/frettir", "https://www.ruv.is", textInFeed = true)
        val short = NewsArticle(ruv, "https://www.ruv.is/a", "Frétt", "Stutt frétt.", null, feedText = "Stutt frétt um veðrið í dag.")
        assertEquals("Stutt frétt um veðrið í dag.", FeedNewsReader(client).text(short))
        assertEquals(0, pages, "the page, which shows its text only to a browser, is not read")
    }

    @Test
    fun feedsWhoseLinksMissTheArticleAreSetRight() {
        val un = NewsSource("ar", "UN News", "https://news.un.org/feed/subscribe/ar/news/all/rss.xml", "")
        assertEquals("https://news.un.org/ar/story/2026/10/123", FeedNewsReader.articleUrl("https://news.un.org/feed/view/ar/story/2026/10/123", un))
        val bbc = NewsSource("zh", "BBC 中文", "https://feeds.bbci.co.uk/zhongwen/simp/rss.xml", "")
        assertEquals("https://www.bbc.com/zhongwen/articles/c123/simp?at_medium=RSS", FeedNewsReader.articleUrl("https://www.bbc.com/zhongwen/articles/c123/trad?at_medium=RSS", bbc))
        val trad = NewsSource("yue", "BBC 中文 (繁體)", "https://feeds.bbci.co.uk/zhongwen/trad/rss.xml", "")
        assertEquals("https://www.bbc.com/zhongwen/articles/c123/trad", FeedNewsReader.articleUrl("https://www.bbc.com/zhongwen/articles/c123/trad", trad))
    }

    @Test
    fun aDateWrittenCarelesslyIsStillRead() {
        assertEquals(Instant.parse("2026-10-16T08:00:00Z"), FeedNewsReader.date("Fri,16 Oct 2026 10:00:00 +0200"))
        assertEquals(Instant.parse("2026-10-16T00:00:00Z"), FeedNewsReader.date("2026-10-16"))
        assertNull(FeedNewsReader.date("yesterday"))
    }

    @Test
    fun aPageWithoutTheTextFallsBackToTheSummary() = runTest {
        val client = HttpClient(MockEngine { respond("<html><body><div id=app></div></body></html>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html")) }) { install(HttpTimeout) }
        val article = NewsArticle(source, "https://news.example/app", "App", "O resumo da notícia.", null, feedText = "O resumo da notícia.")
        assertEquals("O resumo da notícia.", FeedNewsReader(client).text(article))
    }

    private val file = File.createTempFile("tayra-news", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages))

    private val other = NewsSource("pt", "Outro", "https://other.example/rss", "https://other.example", NewsLevel.EASY)
    private val broken = NewsSource("pt", "Quebrado", "https://broken.example/rss", "https://broken.example")
    private var fetched = 0

    private val reader = object : NewsFeedReader {
        override suspend fun articles(source: NewsSource): List<NewsArticle> = when (source) {
            this@NewsTest.source -> listOf(
                NewsArticle(source, "https://news.example/old", "Antiga", "", Instant.parse("2026-10-14T10:00:00Z")),
                NewsArticle(source, "https://news.example/gato", "O gato", "", Instant.parse("2026-10-16T10:00:00Z")),
            )
            other -> listOf(NewsArticle(source, "https://other.example/casa", "A casa", "", Instant.parse("2026-10-15T10:00:00Z")))
            else -> error("Feed down")
        }

        override suspend fun text(article: NewsArticle): String {
            fetched++
            return "O gato dorme na casa.\nA casa é grande."
        }
    }

    private val service = NewsService(reader, reading, BookService(books, languages), books, listOf(source, other, broken, NewsSource("es", "Noticias", "https://es.example/rss", "")))

    @Test
    fun theLatestNewsOfTheLanguageComeNewestFirst() = runTest {
        val portuguese = languages.getById(languages.save(Language(name = "Portuguese")))!!
        val news = service.latest(portuguese)
        assertEquals(listOf("O gato", "A casa", "Antiga"), news.articles.map { it.title })
        assertEquals(listOf(broken), news.failed)
    }

    @Test
    fun anArticleIsJudgedAndReadAsATextOfItsOwn() = runTest {
        val id = languages.save(Language(name = "Portuguese"))
        val portuguese = languages.getById(id)!!
        listOf("o" to TermStatus.WELL_KNOWN, "gato" to TermStatus.NEW_2).forEach { (word, status) -> terms.save(Term(languageId = id, text = word, textLc = word, status = status)) }
        val article = service.latest(portuguese).articles.first()
        // 9 running words, of which o and gato are known.
        assertEquals(TextCoverage(words = 9, known = 2), service.coverage(article, portuguese))

        val bookId = service.open(article, portuguese, "Fonte: Notícias.")
        assertEquals(bookId, service.open(article, portuguese, "Fonte: Notícias."), "opening again finds the same text")
        assertEquals(1, fetched, "judging and then opening fetches the article once")
        assertEquals("O gato dorme na casa.\nA casa é grande.\n\nFonte: Notícias.", books.getPages(bookId).joinToString("\n") { it.text })
        assertEquals(mapOf(article.url to bookId), service.observeOpened().first().mapValues { it.value.bookId })
        assertEquals(article.url, books.newsUrl(bookId))
        // The news is read apart from the books.
        assertEquals(emptyList(), books.observeBooks(archived = false).first())
    }
}
