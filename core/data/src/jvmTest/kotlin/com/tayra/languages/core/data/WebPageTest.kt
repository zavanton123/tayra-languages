package com.tayra.languages.core.data

import com.tayra.languages.core.data.news.ArticleText
import com.tayra.languages.core.data.web.HttpWebPageReader
import com.tayra.languages.core.data.web.PageMeta
import com.tayra.languages.core.domain.web.WebPageException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebPageTest {

    private val sentences = listOf(
        "O pão caseiro precisa de farinha, água, sal e um pouco de fermento para crescer bem.",
        "Misture tudo numa tigela grande e amasse a massa durante dez minutos até ficar lisa.",
        "Deixe a massa descansar numa tigela coberta durante uma hora, longe das correntes de ar.",
        "Depois, forme o pão, faça um corte por cima e leve ao forno bem quente durante quarenta minutos.",
        "Quando a côdea estiver dourada e o pão soar oco ao bater por baixo, está pronto para sair do forno.",
        "Deixe-o arrefecer em cima de uma grelha antes de o cortar, para o miolo não ficar pegajoso.",
    )

    private fun article(body: String = sentences.joinToString("") { "<p>$it</p>" }) = """
        <!doctype html><html lang="pt-BR"><head>
          <title>Como fazer pão caseiro | Receitas da Avó</title>
          <meta property="og:site_name" content="Receitas da Avó">
          <meta name="author" content="Maria Silva">
        </head><body>
          <header><a href="/">Receitas da Avó</a> <nav><a href="/a">Bolos</a> <a href="/b">Pães</a></nav></header>
          <div class="share-bar"><a href="#">Partilhar no Facebook e em todas as outras redes sociais que existem</a></div>
          <main><article class="post-content"><h1>Como fazer pão caseiro</h1>$body</article></main>
          <div class="related-posts"><p>Veja também: como fazer um bolo de chocolate fofinho em apenas trinta minutos.</p></div>
          <section id="comments"><p>Adorei a receita, fiz ontem e ficou muito bom, obrigada por partilhar connosco!</p></section>
          <footer><p>Todos os direitos reservados a quem escreveu estas receitas tão boas e tão antigas.</p></footer>
        </body></html>
    """.trimIndent()

    @Test
    fun aPageGivesItsArticleWithoutTheCommentsAndRelatedPosts() {
        val text = ArticleText.page(article())
        assertEquals(sentences, text.lines())
        assertFalse("Adorei" in text, "comments are left out")
        assertFalse("Veja também" in text, "related posts are left out")
        assertFalse("direitos" in text, "the footer is left out")
        assertFalse("Partilhar" in text, "the share bar is left out")
    }

    @Test
    fun hiddenTextAndLineOfLinksAreLeftOut() {
        val body = sentences.joinToString("") { "<p>$it</p>" } +
            """<p style="display: none">Texto escondido que nenhum leitor vê no ecrã, mas que está na página.</p>""" +
            """<div aria-hidden="true"><p>Outro texto escondido de quem lê com um leitor de ecrã, e de todos.</p></div>""" +
            """<p><a href="/1">Pão de forma</a>, <a href="/2">pão de centeio</a>, <a href="/3">pão de milho e outros pães</a></p>"""
        val text = ArticleText.page(article(body))
        assertFalse("escondido" in text)
        assertFalse("centeio" in text, "a line that is all links is a list of other pages")
        sentences.forEach { assertTrue(it in text) }
    }

    @Test
    fun textLooseInBlocksAndListItemsAreParagraphs() {
        val html = """
            <html><body><div class="entry">
              ${sentences[0]}<br>${sentences[1]}
              <ul><li>500 g de farinha</li><li>300 ml de água</li><li>10 g de sal</li></ul>
              <div>${sentences[2]}</div><div>${sentences[3]}</div>
            </div></body></html>
        """.trimIndent()
        assertEquals(listOf(sentences[0], sentences[1], "500 g de farinha", "300 ml de água", "10 g de sal", sentences[2], sentences[3]), ArticleText.page(html).lines())
    }

    @Test
    fun aSiteThatNamesItsArticleASidebarIsReadAgainWithoutTheNames() {
        val html = "<html><body><div class=\"sidebar-layout\">${sentences.joinToString("") { "<p>$it</p>" }}</div></body></html>"
        assertEquals(sentences, ArticleText.page(html).lines())
    }

    @Test
    fun theNewsReaderStillReadsNoNames() {
        // A news site's article in a "teaser" is the news reader's article; only the page reading leaves such names out.
        val html = "<html><body><div class=\"teaser\">${sentences.joinToString("") { "<p>$it</p>" }}</div></body></html>"
        assertEquals(sentences, ArticleText.extract(html).lines())
    }

    @Test
    fun aPageSaysWhatItIsAndWhoWroteIt() {
        val meta = PageMeta.read(article(), "receitas.example.com")
        assertEquals("Como fazer pão caseiro", meta.title)
        assertEquals("pt-BR", meta.languageCode)
        assertEquals("Maria Silva", meta.author)
        assertEquals("Receitas da Avó", meta.siteName)
        val data = """<html><head><title>Notícia</title><script type="application/ld+json">
            {"@context":"https://schema.org","@graph":[{"@type":"WebSite","name":"Jornal"},
             {"@type":"NewsArticle","headline":"A chuva voltou a Lisboa","inLanguage":"pt-PT","author":[{"@type":"Person","name":"Ana Costa"}],"publisher":{"name":"Jornal"}}]}
            </script></head><body></body></html>"""
        val fromData = PageMeta.read(data, "jornal.example")
        assertEquals("A chuva voltou a Lisboa", fromData.title)
        assertEquals("pt-PT", fromData.languageCode)
        assertEquals("Ana Costa", fromData.author)
        assertEquals("Jornal", fromData.siteName)
    }

    @Test
    fun aTitleLosesTheSiteItEndsIn() {
        assertEquals("Como fazer pão caseiro", PageMeta.cleanTitle("Como fazer pão caseiro | Receitas da Avó", "Receitas da Avó", "example.com"))
        assertEquals("A chuva voltou a Lisboa", PageMeta.cleanTitle("A chuva voltou a Lisboa - Publico", null, "www.publico.pt"))
        assertEquals("Um dia na praia com os meus avós", PageMeta.cleanTitle("Um dia na praia com os meus avós - Blog", null, "example.com"))
        assertEquals("Lisboa: a cidade das sete colinas", PageMeta.cleanTitle("Lisboa: a cidade das sete colinas", null, "example.com"))
    }

    private fun reader(status: HttpStatusCode = HttpStatusCode.OK, type: String = "text/html; charset=utf-8", body: ByteArray, seen: MutableList<String> = mutableListOf()) =
        HttpWebPageReader(HttpClient(MockEngine { request -> seen += request.url.toString(); respond(body, status, headersOf(HttpHeaders.ContentType, type)) }) { install(HttpTimeout) })

    @Test
    fun aPageIsReadWithItsTitleAndAddress() = runTest {
        val seen = mutableListOf<String>()
        val page = reader(body = article().encodeToByteArray(), seen = seen).read("receitas.example.com/pao")
        assertEquals(listOf("https://receitas.example.com/pao"), seen, "an address typed without its scheme is https")
        assertEquals("https://receitas.example.com/pao", page.url)
        assertEquals("Como fazer pão caseiro", page.title)
        assertEquals(sentences, page.text.lines(), "the heading that repeats the title is dropped")
        assertEquals("pt-BR", page.languageCode)
    }

    @Test
    fun aPageInAnOldCodePageIsDecoded() = runTest {
        val russian = "Москва — столица России, крупнейший по численности населения город страны и её политический центр."
        val html = """<html><head><meta charset="windows-1251"></head><body>${"<p>$russian</p>".repeat(4)}</body></html>"""
        val page = reader(type = "text/html", body = html.toByteArray(charset("windows-1251"))).read("https://example.ru/")
        assertEquals(List(4) { russian }, page.text.lines())
    }

    @Test
    fun aLinkToADocumentIsReadAsTheFileItIs() = runTest {
        val page = reader(type = "text/plain; charset=utf-8", body = sentences.joinToString("\n").encodeToByteArray()).read("https://example.com/files/pao")
        assertEquals(sentences, page.text.lines())
        assertEquals("pao", page.title)
    }

    @Test
    fun whyAPageGivesNoArticleIsTold() = runTest {
        assertFailsWith<WebPageException.InvalidAddress> { reader(body = ByteArray(0)).read("not an address") }
        assertFailsWith<WebPageException.InvalidAddress> { reader(body = ByteArray(0)).read("ftp://example.com/file") }
        val missing = assertFailsWith<WebPageException.Unreachable> { reader(HttpStatusCode.NotFound, body = ByteArray(0)).read("https://example.com/gone") }
        assertEquals(404, missing.status)
        assertFailsWith<WebPageException.NoArticle> { reader(body = "<html><body><div id=app></div></body></html>".encodeToByteArray()).read("https://example.com/app") }
        assertNull(HttpWebPageReader.address(""))
    }
}
