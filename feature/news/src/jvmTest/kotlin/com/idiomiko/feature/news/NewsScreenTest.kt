package com.idiomiko.feature.news

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.domain.books.discover.TextCoverage
import com.idiomiko.core.domain.news.NewsArticle
import com.idiomiko.core.domain.news.NewsBook
import com.idiomiko.core.domain.news.NewsSource
import com.idiomiko.core.domain.news.NewsSources
import com.idiomiko.core.domain.news.NewsTopic
import com.idiomiko.core.domain.news.Wiki
import com.idiomiko.core.domain.web.WebPictures
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.CoverageCheck
import com.idiomiko.core.ui.components.LocalWebPictures
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.AppTheme
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.awt.Color as AwtColor
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** The news as a front page: the lead and the cards beside it, the rows after them, and the controls over them. */
@OptIn(ExperimentalTestApi::class)
class NewsScreenTest {
    private val now = Instant.parse("2026-11-20T12:00:00Z")
    private val sources = NewsSources.all.filter { it.languageCode == "pt" }
    private fun source(name: String) = sources.first { it.name == name }

    private fun item(source: String, slug: String, title: String, ago: Duration, topics: List<String>, known: Int?, picture: Boolean = false, summary: String = "") = NewsItem(
        NewsArticle(source(source), "https://news.example/$slug", title, summary, now - ago, topics = topics, imageUrl = if (picture) "https://pictures.example/$slug/photo.jpg" else null),
        known?.let { CoverageCheck.Checked(TextCoverage(1000, it * 10, wholeWords = 180 + it, newWords = 120 - it)) } ?: CoverageCheck.Checking,
    )

    private val articles = listOf(
        item("BBC News Brasil", "cafe", "Preço do café sobe pela terceira semana seguida e pesa no bolso do brasileiro", 12.minutes, listOf("Economia"), 93, picture = true,
            summary = "Seca nas regiões produtoras reduziu a colheita; especialistas explicam o que esperar para o fim do ano."),
        item("The Conversation", "ervas", "Como plantar ervas aromáticas na varanda", 1.hours, listOf("Lifestyle"), 92),
        item("Agência Brasil", "cinema", "Lisboa recebe festival de cinema ao ar livre em dezembro", 2.hours, listOf("Cultura"), 87),
        item("Global Voices", "app", "Estudantes criam app para reduzir desperdício de comida", 3.hours, listOf("Tecnologia", "Mídia Cidadã"), 85),
        item("Notícias ao Minuto", "ic17", "IP \"recomenda cuidado redobrado\" a condutores após pneus furados no IC17", 4.minutes, listOf("País"), 68),
        item("DW Brasil", "selecao", "Seleção feminina vence amigável contra a Espanha", 5.hours, listOf("Desporto"), 89),
        item("Agência Brasil", "chuva", "Chuva forte deve atingir o Sul do Brasil no fim de semana", 6.hours, listOf("Rio Grande do Sul"), 86),
        item("Notícias ao Minuto", "eleicoes", "Eleições municipais: o que muda no segundo turno", 7.hours, listOf("Política"), null),
        item("BBC News Brasil", "mundo", "Cúpula do clima termina com acordo parcial", 8.hours, listOf("Mundo"), 79),
    ) + (1..8).map { item("Notícias ao Minuto", "extra-$it", "Notícia número $it do dia", (9 + it).hours, listOf("Mundo"), 70 + it) } +
        item("DW Brasil", "antiga", "Uma notícia de três dias atrás", 3.days, listOf("Mundo"), 90)

    private val front = NewsUiState(
        languageName = "Portuguese", sources = sources, articles = articles, loading = false, loadedAt = now - 2.minutes,
        opened = mapOf("https://news.example/ervas" to NewsBook("https://news.example/ervas", 7, now - 1.minutes, false)),
    )

    /** Pictures for the tests, drawn here: an address ending in photo.jpg has one, any other fails, as a dead link does. */
    private val pictures = WebPictures { url ->
        if (!url.endsWith("photo.jpg")) null
        else {
            val image = BufferedImage(800, 450, BufferedImage.TYPE_INT_RGB)
            val g = image.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.paint = GradientPaint(0f, 0f, AwtColor(0x6B, 0x3E, 0x26), 800f, 450f, AwtColor(0xD9, 0xA0, 0x66))
            g.fillRect(0, 0, 800, 450)
            g.color = AwtColor(0xF4, 0xE9, 0xDC)
            g.fillOval(300, 125, 200, 200)
            g.color = AwtColor(0x3B, 0x22, 0x15)
            g.fillOval(325, 150, 150, 150)
            g.dispose()
            ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
        }
    }

    private fun ComposeUiTest.show(
        initial: NewsUiState,
        theme: AppTheme = AppThemes.default,
        filtersOpen: Boolean = false,
        actions: (update: ((NewsUiState) -> NewsUiState) -> Unit) -> NewsActions = { NewsActions() },
    ) = setContent {
        var state by remember { mutableStateOf(initial) }
        IdiomikoTheme(theme) {
            CompositionLocalProvider(LocalWebPictures provides pictures) {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column {
                            AppTopBar(title = "News", onNavigate = {}, section = NavSection.NEWS)
                            NewsContent(state, actions { state = it(state) }, now = now, tabs = { NewsTabs(NewsTab.NEWS) {} }, filtersOpen = filtersOpen)
                        }
                    }
                }
            }
        }
    }

    /** The window as it shows, menus and sheets over the page included, into the folder the screenshots are asked for in. */
    private fun ComposeUiTest.shot(name: String) {
        val dir = System.getenv("NEWS_SHOTS") ?: return
        waitForIdle()
        File(dir).mkdirs()
        val roots = onAllNodes(isRoot())
        val page = roots[0].captureToImage().toAwtImage()
        val canvas = BufferedImage(page.width, page.height, BufferedImage.TYPE_INT_ARGB)
        val g = canvas.createGraphics()
        g.drawImage(page, 0, 0, null)
        for (i in 1 until roots.fetchSemanticsNodes().size) {
            val bounds = roots[i].fetchSemanticsNode().boundsInWindow
            g.drawImage(roots[i].captureToImage().toAwtImage(), bounds.left.toInt(), bounds.top.toInt(), null)
        }
        g.dispose()
        ImageIO.write(canvas, "png", File(dir, "$name.png"))
    }

    private fun ComposeUiTest.count(tag: String) = onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun ComposeUiTest.waitFor(text: String) = waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun theFrontPageLeadsWithTheEasiestOfTheNewestBesideFourCards() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val opened = mutableListOf<String>()
        show(front) { update ->
            NewsActions(onOpen = { opened += it.article.url }, onTopic = { t -> update { it.copy(topic = t) } }, onShowAll = { update { it.copy(rows = Int.MAX_VALUE) } })
        }
        waitFor("Preço do café")
        // The lead's picture comes from the web; the cards without one show their topic's tile.
        waitUntil(timeoutMillis = 5_000) { count("web-picture") > 0 }
        shot("desktop-light")
        // Easiest first: the café story is the best known of the newest, and leads, as the one known better was read.
        onNodeWithTag("lead-https://news.example/cafe").assertExists()
        for (card in listOf("ervas", "selecao", "cinema", "chuva")) onNodeWithTag("card-https://news.example/$card").assertExists()
        onNodeWithText("Updated 2 minutes ago").assertExists()
        onNodeWithText("~5 min read", substring = true).assertExists()
        onNodeWithText("27 new words", substring = true).assertExists()
        onNodeWithText("93% known").assertExists()
        // The feeds' sections in Brazil's words, once each; Portugal's own news (País) is no topic.
        val labels = listOf("Brasil", "Mundo", "Política", "Economia", "Esporte", "Cultura", "Ciência & tech", "Estilo de vida")
        NewsTopic.entries.zip(labels).forEach { (topic, label) -> onNodeWithTag("topic-${topic.name}").assert(hasText(label)) }
        assertEquals(0, onAllNodes(hasText("País")).fetchSemanticsNodes().size)
        // Rows wait behind See all. The one read is dimmed and marked.
        onNodeWithText("More from today").assertExists()
        onNodeWithText("See all 17").performClick()
        waitForIdle()
        onNodeWithTag("row-https://news.example/extra-8").assertExists()
        onNodeWithTag("read-mark-https://news.example/ervas", useUnmergedTree = true).assertExists()
        assertEquals(0, count("row-https://news.example/antiga"), "three days ago is not today")
        onNodeWithTag("lead-https://news.example/cafe").performClick()
        assertEquals(listOf("https://news.example/cafe"), opened)
        onNodeWithTag("topic-SPORT").performClick()
        waitForIdle()
        onNodeWithTag("lead-https://news.example/selecao").assertExists()
        assertEquals(0, count("card-https://news.example/ervas"))
    }

    @Test
    fun theFrontPageInTheDarkTheme() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        show(front, AppThemes.night)
        waitFor("Preço do café")
        waitUntil(timeoutMillis = 5_000) { count("web-picture") > 0 }
        shot("desktop-dark")
        onNodeWithTag("front-page").assertExists()
    }

    @Test
    fun theSourcesMenuTicksEachSourceShown() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val toggled = mutableListOf<String>()
        show(front.copy(hiddenSources = setOf("Global Voices"))) { update ->
            NewsActions(onSource = { s -> toggled += s.name; update { it.copy(hiddenSources = it.hiddenSources - s.name) } })
        }
        waitFor("Preço do café")
        onNodeWithText("Sources: 5 of 6").assertExists()
        onNodeWithTag("news-sources").performClick()
        waitForIdle()
        onNodeWithTag("source-Global Voices").assertExists()
        shot("desktop-sources-menu")
        onNodeWithTag("source-Global Voices").performClick()
        waitForIdle()
        assertEquals(listOf("Global Voices"), toggled)
        onNodeWithText("Sources: all 6").assertExists()
    }

    @Test
    fun aSourceThatFailsIsNamedAboveTheOthersNews() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        var retried = 0
        // Newest first, so the article still being judged is among the rows on screen, its share known "…".
        show(front.copy(sort = NewsSort.NEWEST, failed = listOf(source("DW Brasil")), articles = articles.filter { it.article.source.name != "DW Brasil" })) { NewsActions(onRefresh = { retried++ }) }
        waitFor("Preço do café")
        shot("desktop-source-failing")
        onNodeWithTag("known-https://news.example/eleicoes", useUnmergedTree = true).assert(hasContentDescription("Checking the words…"))
        onNodeWithText("DW Brasil could not be reached; the other sources' news is here.").assertExists()
        onNodeWithTag("retry-failed").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun withEverySourceDownTheNewsSaysSoAndTriesAgain() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        var retried = 0
        show(front.copy(failed = sources, articles = emptyList(), loadedAt = null)) { NewsActions(onRefresh = { retried++ }) }
        waitFor("The news can't be reached right now")
        shot("desktop-offline")
        onNodeWithTag("retry-all").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun noMatchOffersToClearTheFilters() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        var cleared = 0
        show(front) { update -> NewsActions(onQuery = { q -> update { it.copy(query = q) } }, onClear = { cleared++; update { it.copy(query = "") } }) }
        waitFor("Preço do café")
        onNodeWithTag("news-search").performTextInput("girafa")
        waitFor("No articles match these filters.")
        shot("desktop-no-matches")
        onNodeWithTag("no-match-any-time").assertExists()
        onNodeWithTag("no-match-clear").performClick()
        waitFor("Preço do café")
        assertEquals(1, cleared)
    }

    @Test
    fun whileTheNewsLoadsItsShapesShow() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        show(NewsUiState(languageName = "Portuguese", sources = sources, loading = true))
        waitUntil(timeoutMillis = 5_000) { count("news-skeleton") > 0 }
        shot("desktop-loading")
    }

    @Test
    fun onAPhoneTheNewsIsOneColumn() = runDesktopComposeUiTest(width = 400, height = 860) {
        show(front)
        waitFor("Preço do café")
        waitUntil(timeoutMillis = 5_000) { count("web-picture") > 0 }
        shot("phone-light")
        onNodeWithTag("lead-https://news.example/cafe").assertExists()
        onNodeWithTag("card-https://news.example/ervas").assertExists()
        onNodeWithTag("news-filters").assertExists()
        assertEquals(0, count("news-sources"), "the sources wait in the sheet")
    }

    @Test
    fun onAPhoneInTheDarkTheme() = runDesktopComposeUiTest(width = 400, height = 860) {
        show(front, AppThemes.night)
        waitFor("Preço do café")
        waitUntil(timeoutMillis = 5_000) { count("web-picture") > 0 }
        shot("phone-dark")
    }

    @Test
    fun onAPhoneTheFiltersWaitInASheet() = runDesktopComposeUiTest(width = 400, height = 860) {
        val periods = mutableListOf<NewsPeriod>()
        show(front.copy(hideRead = true), filtersOpen = true) { update -> NewsActions(onPeriod = { p -> periods += p; update { it.copy(period = p, periodChosen = true) } }) }
        waitUntil(timeoutMillis = 5_000) { count("news-filters-sheet") > 0 }
        waitForIdle()
        shot("phone-filters-sheet")
        onNodeWithText("Show 16 articles").assertExists()
        onNodeWithTag("period-WEEK").performClick()
        assertEquals(listOf(NewsPeriod.WEEK), periods)
        onNodeWithText("Show 17 articles").assertExists()
    }

    private fun wiki(title: String, size: Int, known: Int) = NewsItem(
        NewsArticle(NewsSource("pt", "Wikipedia", "https://pt.wikipedia.org", "", textInFeed = true), "https://pt.wikipedia.org/wiki/$title", title,
            "Introdução do artigo $title, com as primeiras frases sobre o assunto e o que mais se sabe dele.", null, feedText = "Introdução.", length = size),
        CoverageCheck.Checked(TextCoverage(100, known, wholeWords = 140)),
    )

    private val wikiState = WikiUiState(
        languageName = "Portuguese", loading = false, hasMore = true, wikis = listOf(Wiki.WIKIPEDIA, Wiki.WIKIVOYAGE, Wiki.WIKIBOOKS),
        articles = listOf(wiki("Gato", 170_000, 92), wiki("Rafael_Câmara", 8_000, 97), wiki("Mercúrio", 30_000, 80), wiki("Lisboa", 40_000, 88), wiki("Futebol", 9_000, 84)),
        opened = mapOf("https://pt.wikipedia.org/wiki/Gato" to NewsBook("https://pt.wikipedia.org/wiki/Gato", 3, now, false)),
    )

    @Test
    fun wikiArticlesAreReadByTheirIntroductionOrWhole() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val read = mutableListOf<String>()
        var tab: NewsTab? = null
        var site: Wiki? = null
        setContent {
            var state by remember { mutableStateOf(wikiState) }
            IdiomikoTheme {
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
        waitFor("Rafael_Câmara")
        shot("desktop-wiki")
        onNodeWithText("Read most yesterday").assertExists()
        onNodeWithText("Wikipedia · Long article").assertExists()
        onNodeWithText("Continue").assertExists()
        onNodeWithTag("read-https://pt.wikipedia.org/wiki/Gato").performClick()
        onNodeWithTag("whole-https://pt.wikipedia.org/wiki/Mercúrio").performClick()
        assertEquals(listOf("intro Gato", "whole Mercúrio"), read)
        onNodeWithTag("wiki-site").performClick()
        onNodeWithText("Wikibooks").performClick()
        assertEquals(Wiki.WIKIBOOKS, site)
        onNodeWithTag("tab-NEWS").performClick()
        assertEquals(NewsTab.NEWS, tab)
    }

    @Test
    fun aWikiWithoutCountedViewsSaysItsArticlesArePickedAtRandom() = runDesktopComposeUiTest(width = 400, height = 860) {
        val hund = NewsItem(
            NewsArticle(NewsSource("de", "MiniKlexikon", "https://miniklexikon.zum.de", "", textInFeed = true), "https://miniklexikon.zum.de/wiki/Hund", "Hund",
                "Hunde sind Säuge-Tiere.", null, feedText = "Hunde sind Säuge-Tiere.", length = 1217),
            CoverageCheck.Checked(TextCoverage(100, 96)),
        )
        var hideRead = true
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        WikipediaContent(
                            WikiUiState(languageName = "German", wikis = listOf(Wiki.WIKIPEDIA, Wiki.KLEXIKON, Wiki.MINIKLEXIKON), wiki = Wiki.MINIKLEXIKON, loading = false, articles = listOf(hund), hideRead = hideRead),
                            WikiActions(onHideRead = { hideRead = it }),
                            tabs = { NewsTabs(NewsTab.WIKIPEDIA) {} },
                        )
                    }
                }
            }
        }
        waitFor("Hund")
        shot("phone-wiki")
        onNodeWithText("Picked at random").assertExists()
        onNodeWithText("The Klexikon in easy German, a short sentence to a line, for children starting to read.").assertExists()
        assertEquals("Source: MiniKlexikon, «Hund», CC BY-SA 4.0.", wikiAttribution(hund.article))
        onNodeWithTag("wiki-filters").performClick()
        waitForIdle()
        onNodeWithTag("wiki-sheet-hide-read").performClick()
        assertEquals(false, hideRead)
    }

    @Test
    fun aLanguageWithoutFeedsSaysSo() = runDesktopComposeUiTest(width = 1200, height = 800) {
        show(NewsUiState(languageName = "Cornish", loading = false))
        onNodeWithText("There is no news feed in Cornish yet.").assertExists()
        shot("desktop-no-feed")
    }

    @Test
    fun anArticleEndsWithItsSource() {
        assertEquals("Source: BBC News Brasil.", attribution(articles[0].article))
    }

    /** The state behind the page: what the filters leave, in what order, and which article leads. */
    @Test
    fun theFiltersAndTheOrderPickTheFrontPage() {
        fun slugs(items: List<NewsItem>) = items.map { it.article.url.substringAfterLast('/') }
        // Easiest first: the judged by the share known, then those still to judge, newest first.
        assertEquals(listOf("cafe", "ervas", "selecao"), slugs(front.filtered).take(3))
        assertEquals("eleicoes", slugs(front.filtered).last(), "the one still judged comes last")
        assertEquals(listOf("ic17", "cafe", "ervas", "cinema"), slugs(front.copy(sort = NewsSort.NEWEST).filtered).take(4))
        // The lead is the best of the newest not read yet.
        assertEquals("cafe", front.front.lead?.article?.url?.substringAfterLast('/'))
        assertEquals("ic17", front.copy(sort = NewsSort.NEWEST).front.lead?.article?.url?.substringAfterLast('/'))
        assertEquals("ervas", front.copy(opened = mapOf("https://news.example/cafe" to NewsBook("https://news.example/cafe", 8, now, false))).front.lead?.article?.url?.substringAfterLast('/'))
        assertEquals(listOf("ervas", "selecao", "cinema", "chuva"), slugs(front.front.cards))
        assertEquals(NewsUiState.ROWS, front.front.rows.size)
        assertEquals(17, 1 + front.front.cards.size + front.front.rows.size + front.front.hidden)
        // Today by default; a day with no news shows the week until a time is chosen.
        assertTrue(front.filtered.none { it.article.url.endsWith("antiga") })
        val quiet = front.copy(loadedAt = now + 2.days)
        assertEquals(NewsPeriod.WEEK, quiet.shownPeriod)
        assertEquals(NewsPeriod.TODAY, quiet.copy(periodChosen = true).shownPeriod)
        assertEquals(18, front.copy(period = NewsPeriod.ANY, periodChosen = true).filtered.size)
        // Each filter narrows the articles.
        assertEquals(listOf("cafe"), slugs(front.copy(query = "CAFÉ").filtered))
        assertEquals(listOf("selecao"), slugs(front.copy(topic = NewsTopic.SPORT).filtered))
        assertTrue(front.copy(hiddenSources = setOf("Notícias ao Minuto")).filtered.none { it.article.source.name == "Notícias ao Minuto" })
        assertTrue(front.copy(hideRead = true).filtered.none { it.article.url.endsWith("ervas") })
        // The topics offered are those the articles are filed under, in a fixed order; none left by the filters, none offered.
        assertEquals(NewsTopic.entries.toList(), front.topics)
        assertEquals(listOf(NewsTopic.SPORT), front.copy(query = "seleção").topics)
        assertEquals(2, front.copy(hideRead = true, hiddenSources = setOf("DW Brasil")).activeFilters)
        assertEquals(false, front.filtering)
        assertEquals(true, front.copy(topic = NewsTopic.SPORT).filtering)
    }
}
