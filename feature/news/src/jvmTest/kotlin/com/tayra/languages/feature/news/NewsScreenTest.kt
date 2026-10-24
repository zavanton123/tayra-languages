package com.tayra.languages.feature.news

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.books.discover.Difficulty
import com.tayra.languages.core.domain.books.discover.TextCoverage
import com.tayra.languages.core.domain.news.NewsArticle
import com.tayra.languages.core.domain.news.NewsBook
import com.tayra.languages.core.domain.news.NewsLevel
import com.tayra.languages.core.domain.news.NewsSource
import com.tayra.languages.core.domain.news.Wiki
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.CoverageCheck
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The news of the language being learned: each article with how much of it the reader knows, and the way into it. */
@OptIn(ExperimentalTestApi::class)
class NewsScreenTest {
    private val now = Instant.parse("2026-10-17T12:00:00Z")
    private val bbc = NewsSource("pt", "BBC News Brasil", "https://feeds.bbci.co.uk/portuguese/rss.xml", "https://www.bbc.com/portuguese")
    private val easy = NewsSource("pt", "Joca", "https://joca.example/rss", "https://joca.example", NewsLevel.EASY)

    private fun article(source: NewsSource, slug: String, title: String, hoursAgo: Int) =
        NewsArticle(source, "https://news.example/$slug", title, "Um resumo curto da notícia, com as primeiras frases do texto.", now - hoursAgo.hours)

    private val items = listOf(
        NewsItem(article(easy, "gatos", "Gatos ganham um parque novo na cidade", 1), CoverageCheck.Checked(TextCoverage(300, 290))),
        NewsItem(article(bbc, "eleicao", "Eleição: o que muda no segundo turno", 2), CoverageCheck.Checked(TextCoverage(800, 700))),
        NewsItem(article(bbc, "clima", "Acordo do clima fica sem texto final", 3), CoverageCheck.Checking),
        NewsItem(article(bbc, "video", "Vídeo: a enchente no Sul", 5), CoverageCheck.Failed("Could not read the article")),
    )

    private fun ComposeUiTest.show(initial: NewsUiState, actions: (update: ((NewsUiState) -> NewsUiState) -> Unit) -> NewsActions) = setContent {
        var state by remember { mutableStateOf(initial) }
        TayraTheme {
            ProvideWindowWidth {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column {
                        AppTopBar(title = "News", onNavigate = {}, section = NavSection.NEWS)
                        NewsContent(state, actions { state = it(state) }, now = now)
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.screenshot(variable: String) = System.getenv(variable)?.let { path ->
        waitForIdle()
        ImageIO.write(onNode(isRoot()).captureToImage().toAwtImage(), "png", File(path))
    }

    @Test
    fun eachArticleShowsItsSourceItsAgeAndHowMuchOfItTheReaderKnows() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        val opened = mutableListOf<String>()
        val state = NewsUiState(
            languageName = "Portuguese", sources = listOf(easy, bbc), articles = items, loading = false,
            opened = mapOf("https://news.example/eleicao" to NewsBook("https://news.example/eleicao", 7, now - 30.minutes, false)),
        )
        show(state) { update ->
            NewsActions(onSource = { s -> update { it.copy(source = s) } }, onDifficulty = { d -> update { it.copy(difficulty = d) } }, onOpen = { opened += it.article.url })
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Gatos ganham um parque novo na cidade")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("NEWS_SCREENSHOT")
        onNodeWithText("The latest news in Portuguese, with how much of each article you already know.").assertExists()
        onNodeWithText("Joca · 1 hour ago").assertExists()
        onNodeWithText("Easy language").assertExists()
        onNodeWithText("You know 96% of the words").assertExists()
        onNodeWithText("Checking the words…").assertExists()
        onNodeWithText("Could not read the article").assertExists()
        // An article opened before is gone on with.
        onNodeWithText("Continue").assertExists()
        onNodeWithTag("read-https://news.example/gatos").performClick()
        onNodeWithTag("read-https://news.example/eleicao").performClick()
        assertEquals(listOf("https://news.example/gatos", "https://news.example/eleicao"), opened)

        onNodeWithTag("source-Joca").performClick()
        waitForIdle()
        assertEquals(0, onAllNodesWithTag("article-https://news.example/clima").fetchSemanticsNodes().size)
        onNodeWithTag("source-all").performClick()
        onNodeWithTag("difficulty-MEDIUM").performClick()
        waitForIdle()
        onNodeWithTag("article-https://news.example/eleicao").assertExists()
        assertEquals(0, onAllNodesWithTag("article-https://news.example/gatos").fetchSemanticsNodes().size)
    }

    @Test
    fun onAPhoneTheButtonsSpanTheCards() = runDesktopComposeUiTest(width = 400, height = 1000) {
        show(NewsUiState(languageName = "Portuguese", sources = listOf(easy, bbc), articles = items, loading = false, failed = listOf(easy))) { NewsActions() }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Gatos ganham um parque novo na cidade")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("NEWS_PHONE_SCREENSHOT")
        onNodeWithText("Could not read: Joca").assertExists()
    }

    private fun item(source: NewsSource, slug: String, title: String, hoursAgo: Int, topics: List<String>, coverage: Int? = null) = NewsItem(
        NewsArticle(source, "https://news.example/$slug", title, "Resumo de $title.", now - hoursAgo.hours, topics = topics),
        coverage?.let { CoverageCheck.Checked(TextCoverage(100, it)) } ?: CoverageCheck.NotChecked,
    )

    private val many = listOf(
        item(easy, "parque", "Parque novo para gatos", 2, listOf("Cidade", "Animais"), coverage = 97),
        item(bbc, "segundo-turno", "O segundo turno da eleição", 5, listOf("Política", "Brasil"), coverage = 88),
        item(bbc, "debate", "Debate antes do segundo turno", 30, listOf("política"), coverage = 70),
        item(bbc, "chuva", "Chuva forte no Sul", 100, listOf("Clima", "Brasil")),
        item(bbc, "sem-data", "Notícia sem data", 0, listOf("Brasil")).let { it.copy(article = it.article.copy(published = null)) },
    )

    private val filters = NewsUiState(
        languageName = "Portuguese", sources = listOf(easy, bbc), articles = many, loading = false, loadedAt = now,
        opened = mapOf("https://news.example/debate" to NewsBook("https://news.example/debate", 9, now, true)),
    )

    @Test
    fun eachFilterNarrowsTheArticles() {
        fun titles(state: NewsUiState) = state.shown.map { it.article.title }
        assertEquals(listOf("O segundo turno da eleição", "Debate antes do segundo turno"), titles(filters.copy(query = "segundo TURNO")))
        assertEquals(listOf("Parque novo para gatos", "O segundo turno da eleição"), titles(filters.copy(period = NewsPeriod.TODAY)))
        assertEquals(4, filters.copy(period = NewsPeriod.WEEK).shown.size, "an article without a date is no week's")
        assertEquals(listOf("Parque novo para gatos"), titles(filters.copy(easyOnly = true)))
        assertEquals(4, filters.copy(hideRead = true).shown.size)
        // A topic is the same whatever its case, and offered once two articles share it.
        assertEquals(listOf("Brasil", "Política"), filters.topics)
        assertEquals(listOf("O segundo turno da eleição", "Debate antes do segundo turno"), titles(filters.copy(topic = "Política")))
        assertEquals(listOf("Debate antes do segundo turno"), titles(filters.copy(difficulty = Difficulty.HARD)))
        // With a difficulty chosen, the articles not judged yet are still being judged.
        assertEquals(2, filters.copy(difficulty = Difficulty.HARD).stillChecking)
        assertEquals(false, filters.filtering)
        assertEquals(true, filters.copy(hideRead = true).filtering)
    }

    @Test
    fun onAPhoneTheFiltersWaitInASheet() = runDesktopComposeUiTest(width = 400, height = 1000) {
        show(filters) { update -> NewsActions(onTopic = { t -> update { it.copy(topic = t) } }) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Parque novo para gatos")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithTag("topic-Política").fetchSemanticsNodes().size)
        onNodeWithTag("news-filters").performClick()
        onNodeWithTag("topic-Política").performClick()
        onNodeWithText("Show 2 articles").assertExists()
        onNodeWithTag("news-filters-done").performClick()
        waitForIdle()
        assertEquals(0, onAllNodesWithTag("topic-Política").fetchSemanticsNodes().size)
        onNodeWithContentDescription("1 filter on", substring = true).assertExists()
    }

    @Test
    fun theFiltersWorkOnTheScreen() = runDesktopComposeUiTest(width = 1586, height = 1300) {
        val cleared = mutableListOf<Unit>()
        show(filters) { update ->
            NewsActions(
                onQuery = { q -> update { it.copy(query = q) } },
                onPeriod = { p -> update { it.copy(period = p) } },
                onEasyOnly = { on -> update { it.copy(easyOnly = on) } },
                onHideRead = { on -> update { it.copy(hideRead = on) } },
                onTopic = { t -> update { it.copy(topic = t) } },
                onClear = { cleared += Unit; update { it.copy(query = "", period = NewsPeriod.ANY, easyOnly = false, hideRead = false, topic = null) } },
            )
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Parque novo para gatos")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("topic-Política").performClick()
        waitForIdle()
        screenshot("NEWS_FILTERS_SCREENSHOT")
        // The summary names the filters on and the articles they leave.
        onNodeWithText("Política · 2 articles").assertExists()
        assertEquals(0, onAllNodesWithTag("article-https://news.example/parque").fetchSemanticsNodes().size)
        onNodeWithTag("hide-read").performClick()
        onNodeWithText("Hide read · Política · 1 article").assertExists()
        onNodeWithTag("clear-filters").performClick()
        onNodeWithTag("article-https://news.example/parque").assertExists()
        assertEquals(1, cleared.size)
        onNodeWithTag("news-search").performTextInput("chuva")
        waitForIdle()
        onNodeWithTag("article-https://news.example/chuva").assertExists()
        assertEquals(0, onAllNodesWithTag("article-https://news.example/parque").fetchSemanticsNodes().size)
    }

    private fun wiki(title: String, size: Int, known: Int, slug: String = title) = NewsItem(
        NewsArticle(NewsSource("pt", "Wikipedia", "https://pt.wikipedia.org", "", textInFeed = true), "https://pt.wikipedia.org/wiki/$slug", title,
            "Introdução do artigo $title, com as primeiras frases.", null, feedText = "Introdução.", length = size),
        CoverageCheck.Checked(TextCoverage(100, known)),
    )

    private val wikiState = WikiUiState(
        languageName = "Portuguese", loading = false, hasMore = true, wikis = listOf(Wiki.WIKIPEDIA, Wiki.WIKIVOYAGE, Wiki.WIKIBOOKS),
        articles = listOf(wiki("Gato", 170_000, 92), wiki("Rafael Câmara", 8_000, 97, "Rafael_Câmara"), wiki("Mercúrio", 30_000, 80)),
        opened = mapOf("https://pt.wikipedia.org/wiki/Gato" to NewsBook("https://pt.wikipedia.org/wiki/Gato", 3, now, false)),
    )

    @Test
    fun wikipediaArticlesAreReadByTheirIntroductionOrWhole() = runDesktopComposeUiTest(width = 1586, height = 1200) {
        val read = mutableListOf<String>()
        var tab: NewsTab? = null
        var site: Wiki? = null
        setContent {
            var state by remember { mutableStateOf(wikiState) }
            TayraTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column {
                            AppTopBar(title = "News", onNavigate = {}, section = NavSection.NEWS)
                            WikipediaContent(
                                state,
                                WikiActions(
                                    onWiki = { site = it },
                                    onLength = { l -> state = state.copy(length = l) },
                                    onOpen = { read += "intro " + it.article.title },
                                    onOpenWhole = { read += "whole " + it.article.title },
                                ),
                                tabs = { NewsTabs(NewsTab.WIKIPEDIA) { tab = it } },
                            )
                        }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Rafael Câmara")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("WIKI_SCREENSHOT")
        onNodeWithText("Wikipedia: the articles read most yesterday in Portuguese, with how much of each introduction you already know.").assertExists()
        onNodeWithTag("wiki-site-WIKIBOOKS").performClick()
        assertEquals(Wiki.WIKIBOOKS, site)
        onNodeWithText("Long article").assertExists()
        onNodeWithText("Short article").assertExists()
        // The introduction read before is gone on with; the whole article is a choice of its own.
        onNodeWithText("Continue").assertExists()
        onNodeWithTag("read-https://pt.wikipedia.org/wiki/Gato").performClick()
        onNodeWithTag("whole-https://pt.wikipedia.org/wiki/Mercúrio").performClick()
        assertEquals(listOf("intro Gato", "whole Mercúrio"), read)
        onNodeWithTag("length-SHORT").performClick()
        waitForIdle()
        assertEquals(0, onAllNodesWithTag("wiki-Gato").fetchSemanticsNodes().size)
        onNodeWithTag("wiki-Rafael Câmara").assertExists()
        onNodeWithTag("tab-NEWS").performClick()
        assertEquals(NewsTab.NEWS, tab)
    }

    @Test
    fun aWikiWithoutCountedViewsListsArticlesAtRandom() = runDesktopComposeUiTest(width = 400, height = 1000) {
        val hund = NewsItem(
            NewsArticle(NewsSource("de", "MiniKlexikon", "https://miniklexikon.zum.de", "", textInFeed = true), "https://miniklexikon.zum.de/wiki/Hund", "Hund",
                "Hunde sind Säuge-Tiere.", null, feedText = "Hunde sind Säuge-Tiere.", length = 1217),
            CoverageCheck.Checked(TextCoverage(100, 96)),
        )
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        WikipediaContent(
                            WikiUiState(languageName = "German", wikis = listOf(Wiki.WIKIPEDIA, Wiki.KLEXIKON, Wiki.MINIKLEXIKON, Wiki.WIKIVOYAGE, Wiki.WIKIBOOKS), wiki = Wiki.MINIKLEXIKON, loading = false, articles = listOf(hund)),
                            WikiActions(),
                            tabs = { NewsTabs(NewsTab.WIKIPEDIA) {} },
                        )
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Hund")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("WIKI_GERMAN_SCREENSHOT")
        onNodeWithText("MiniKlexikon: articles picked at random, with how much of each introduction you already know.").assertExists()
        onNodeWithText("The Klexikon in easy German, a short sentence to a line, for children starting to read.").assertExists()
        for (wiki in listOf(Wiki.WIKIPEDIA, Wiki.KLEXIKON, Wiki.MINIKLEXIKON, Wiki.WIKIVOYAGE, Wiki.WIKIBOOKS)) onNodeWithTag("wiki-site-${wiki.name}").assertExists()
        assertEquals("Source: MiniKlexikon, «Hund», CC BY-SA 4.0.", wikiAttribution(hund.article))
        assertEquals("Source: Wikipedia, «Gato», CC BY-SA 4.0.", wikiAttribution(wiki("Gato", 1, 1).article))
    }

    /** The filters chosen in a phone's sheet are named under its button, with the articles left, and the whole card opens one. */
    @Test
    fun aWikisFiltersAreSummedUpAndTheCardOpens() = runDesktopComposeUiTest(width = 400, height = 1000) {
        val gato = wiki("Gato", 1, 1)
        val opened = mutableListOf<String>()
        var hideRead = true
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        WikipediaContent(
                            WikiUiState(languageName = "Portuguese", wikis = listOf(Wiki.WIKIPEDIA), loading = false, articles = listOf(gato), length = ArticleLength.SHORT, hideRead = hideRead),
                            WikiActions(onHideRead = { hideRead = it }, onOpen = { opened += it.article.title }),
                        )
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Gato")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Short · Hide read · 1 article").assertExists()
        onNodeWithTag("wiki-Gato").performClick()
        assertEquals(listOf("Gato"), opened)
        onNodeWithTag("clear-filters").performClick()
        assertEquals(false, hideRead)
    }

    @Test
    fun aLanguageWithoutFeedsSaysSo() = runDesktopComposeUiTest(width = 1200, height = 800) {
        show(NewsUiState(languageName = "Cornish", loading = false)) { NewsActions() }
        onNodeWithText("There is no news feed in Cornish yet.").assertExists()
    }

    @Test
    fun anArticleEndsWithItsSource() {
        assertEquals("Source: BBC News Brasil.", attribution(items[2].article))
    }
}
